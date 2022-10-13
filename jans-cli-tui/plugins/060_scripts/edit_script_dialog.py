from typing import OrderedDict
from asyncio import ensure_future

from prompt_toolkit.layout.dimension import D
from prompt_toolkit.layout.containers import (
    HSplit,
    VSplit,
    DynamicContainer,
    Window
)
from prompt_toolkit.widgets import (
    Box,
    Button,
    Label,
)
from prompt_toolkit.application.current import get_app
from prompt_toolkit.widgets import (
    Button,
    Dialog,
    VerticalLine,
    HorizontalLine,
    CheckboxList,
)

from prompt_toolkit.lexers import PygmentsLexer
from pygments.lexers.python import PythonLexer
from pygments.lexers.jvm import JavaLexer

from cli import config_cli
from static import DialogResult
from wui_components.jans_dialog_with_nav import JansDialogWithNav
from wui_components.jans_side_nav_bar import JansSideNavBar
from wui_components.jans_cli_dialog import JansGDialog
from wui_components.jans_drop_down import DropDownWidget
from wui_components.jans_data_picker import DateSelectWidget
from utils import DialogUtils
from wui_components.jans_vetrical_nav import JansVerticalNav
from wui_components.jans_spinner import Spinner

from view_uma_dialog import ViewUMADialog
import threading

from multi_lang import _
import re

LEXERS = {
        'PYTHON': PygmentsLexer(PythonLexer),
        'JAVA': PygmentsLexer(JavaLexer)
        }



class EditScriptDialog(JansGDialog, DialogUtils):
    """This Script editing dialog
    """
    def __init__(self, parent, title, data, buttons=[], save_handler=None):
        """init for `EditScriptDialog`, inherits from two diffrent classes `JansGDialog` and `DialogUtils`
            
        DialogUtils (methods): Responsable for all `make data from dialog` and `check required fields` in the form for any Edit or Add New
        
        Args:
            parent (widget): This is the parent widget for the dialog, to access `Pageup` and `Pagedown`
            title (str): The Main dialog title
            data (list): selected line data 
            button_functions (list, optional): Dialog main buttons with their handlers. Defaults to [].
            save_handler (method, optional): handler invoked when closing the dialog. Defaults to None.
        """
        super().__init__(parent, title, buttons)
        self.myparent = parent
        self.save_handler = save_handler
        self.data = data
        self.title=title
        self.create_window()


    def save(self):

        data = {}

        for item in self.dialog.content.children + self.alt_tabs[self.sope_type].children:
            item_data = self.get_item_data(item)
            if item_data:
                data[item_data['key']] = item_data['value']

        if data['scopeType'] in ('openid', 'dynamic') and hasattr(self, 'claims_container'):
            claims = [claim[0] for claim in self.claims_container.data]
            data['claims'] = claims

        self.myparent.logger.debug('DATA: ' + str(data))
        self.data = data
        if 'attributes' in self.data.keys():
            self.data['attributes'] = {'showInConfigurationEndpoint':self.data['attributes']}

        close_me = True
        if self.save_handler:
            close_me = self.save_handler(self)
        if close_me:
            self.future.set_result(DialogResult.ACCEPT)
    
    def cancel(self):
        self.future.set_result(DialogResult.CANCEL)



    def create_window(self):

        schema = self.myparent.cli_object.get_schema_from_reference('#/components/schemas/CustomScript')

        script_types = [
                        ['PERSON_AUTHENTICATION', 'Person Authentication'],
                        ['CONSENT_GATHERING', 'Consent Gathering'],
                        ['POST_AUTHN', 'Post Authentication'],
                        ['ID_TOKEN', 'id_token'],
                        ['PASSWORD_GRANT', 'Password Grant'],
                        ['CIBA_END_USER_NOTIFICATION', 'CIBA End User Notification'],
                        ['OpenID Configuration', 'OpenID Configuration'],
                        ['DYNAMIC_SCOPE', 'Dynamic Scope', ],
                        ['SPONTANEOUS_SCOPE', 'Spontaneous Scope',],
                        ['APPLICATION_SESSION', 'Application Session'],
                        ['END_SESSION', 'End Session'],
                        ['CLIENT_REGISTRATION', 'Client Registration'],
                        ['INTROSPECTION', 'Introspection'],
                        ['UPDATE_TOKEN', 'Update Token'],
                        ['CONFIG_API', 'Config API'],
                        ['IDP', 'IDP'],
                        ['RESOURCE_OWNER_PASSWORD_CREDENTIALS', 'Resource Owner Password Credentials'],
                        ['CACHE_REFRESH', 'Cache Refresh'],
                        ['ID_GENERATOR', 'Id Generator'],
                        ['UMA_RPT_POLICY', 'Uma Rpt Policy'],
                        ['UMA_RPT_CLAIMS', 'Uma Rpt Claims'],
                        ['UMA_CLAIMS_GATHERING', 'Uma Claims Gathering'],
                        ['SCIM', 'SCIM'],
                        ['REVOKE_TOKEN', 'Revoke Token'],
                        ['PERSISTENCE_EXTENSION', 'Persistence Extension'],
                        ['DISCOVERY', 'Discovery'],
                        ]

        self.script_widget = self.myparent.getTitledText(
                                    _("Script"), 
                                    name='script', 
                                    value=self.data.get('script',''),
                                    style='class:script-titledtext', 
                                    jans_help=self.myparent.get_help_from_schema(schema, 'script'),
                                    height=12,
                                    scrollbar=True,
                                    line_numbers=True,
                                    lexer=LEXERS[self.data.get('programmingLanguage', 'PYTHON')],
                                    )

        self.location_widget = self.myparent.getTitledText(_("          Path"), name='locationPath', value=self.data.get('locationPath',''), style='class:script-titledtext', jans_help=self.myparent.get_help_from_schema(schema, 'locationPath'))
        self.set_location_widget_state(self.data.get('locationPath') == 'file')

        properties_title = _("Properties: ")
        add_property_title = _("Add Property")

        properties_data = []
        for prop in self.data.get('configurationProperties', []):
            properties_data.append([prop['value1'], prop.get('value2', ''), prop.get('hide', False)])

        self.properties_container = JansVerticalNav(
                myparent=self.myparent,
                headers=['Key', 'Value', 'Hide'],
                preferred_size=[20, 20, 5],
                data=properties_data,
                on_enter=self.edit_property,
                on_delete=self.delete_property,
                on_display=self.display_property,
                selectes=0,
                headerColor='class:outh-client-navbar-headcolor',
                entriesColor='class:outh-client-navbar-entriescolor',
                all_data=properties_data,
                underline_headings=False,
                max_width=49
                )


        self.dialog = JansDialogWithNav(
            title=self.title,
            content= HSplit([
                    self.myparent.getTitledWidget(
                                _("Script Type"),
                                name='scriptType',
                                widget=DropDownWidget(
                                    values=script_types,
                                    value=self.data.get('scriptType', '')
                                    ),
                                jans_help=self.myparent.get_help_from_schema(schema, 'scriptType'),
                                style='class:outh-client-dropdown'),

                    self.myparent.getTitledCheckBox(_("Enabled"), name='enabled', checked= not self.data.get('enabled'), style='class:script-checkbox', jans_help=self.myparent.get_help_from_schema(schema, 'enabled')),
                    self.myparent.getTitledText(_("Name"), name='name', value=self.data.get('name',''), style='class:script-titledtext', jans_help=self.myparent.get_help_from_schema(schema, 'name')),
                    self.myparent.getTitledText(_("Description"), name='description', value=self.data.get('description',''), style='class:script-titledtext', jans_help=self.myparent.get_help_from_schema(schema, 'description')),

                    self.myparent.getTitledRadioButton(
                            _("Location"),
                            name='accessTokenAsJwt',
                            values=[('db', _("Database")), ('file', _("File System"))],
                            current_value= 'file' if self.data.get('locationPath') else 'db',
                            jans_help=_("Where to save script"),
                            style='class:outh-client-radiobutton',
                            on_selection_changed=self.script_location_changed,
                            ),

                     self.location_widget,

                    self.myparent.getTitledWidget(
                                _("Programming Language"),
                                name='programmingLanguage',
                                widget=DropDownWidget(
                                    values=[['PYTHON', 'Python'], ['JAVA', 'Java']],
                                    value=self.data.get('programmingLanguage', 'PYTHON'),
                                    on_value_changed=self.script_lang_changed,
                                    ),
                                jans_help=self.myparent.get_help_from_schema(schema, 'programmingLanguage'),
                                style='class:outh-client-dropdown'),

                    self.myparent.getTitledWidget(
                                _("Level"),
                                name='scriptType',
                                widget=Spinner(
                                    value=self.data.get('level', 0)
                                    ),
                                jans_help=self.myparent.get_help_from_schema(schema, 'level'),
                                style='class:outh-client-dropdown'),

                    VSplit([
                            Label(text=properties_title, style='class:script-label', width=len(properties_title)+1), 
                            self.properties_container,
                            Window(width=2),
                            Button(text=add_property_title, width=len(add_property_title)+4, handler=self.edit_property),
                            ]),

                    self.script_widget,
                    ],
                width=D(),
                height=D()
                ),
            button_functions=[(self.cancel, _("Cancel")), (self.save, _("Save"))],
            height=self.myparent.dialog_height,
            width=self.myparent.dialog_width,
            )


    def script_lang_changed(self, value):
        self.script_widget.me.lexer = LEXERS[value]

    def set_location_widget_state(self, state):
        self.location_widget.me.read_only = not state
        self.location_widget.me.focusable = state
        if not state:
            self.location_widget.me.text = ''

    def script_location_changed(self, redio_button):
        state = redio_button.current_value == 'file'
        self.set_location_widget_state(state)

    def edit_property(self, **kwargs):

        key, val, hide = kwargs.get('data', ('','', False))
        key_widget = self.myparent.getTitledText(_("Key"), name='property_key', value=key, style='class:script-titledtext', jans_help=_("Script propery Key"))
        val_widget = self.myparent.getTitledText(_("Value"), name='property_val', value=val, style='class:script-titledtext', jans_help=_("Script property Value"))
        hide_widget = self.myparent.getTitledCheckBox(_("Hide"), name='property_hide', checked=hide, style='class:script-titledtext', jans_help=_("Hide script property?"))

        def add_property(dialog):
            key_ = key_widget.me.text
            val_ = val_widget.me.text
            hide_ = hide_widget.me.checked
            if not kwargs.get('data'):
                self.properties_container.add_item([key_, val_, hide_])
            else:
                self.properties_container.replace_item(kwargs['selected'], [key_, val_, hide_])


        body = HSplit([key_widget, val_widget, hide_widget])
        buttons = [Button(_("Cancel")), Button(_("OK"), handler=add_property)]
        dialog = JansGDialog(self.myparent, title=_("Enter script properties"), body=body, buttons=buttons, width=self.myparent.dialog_width-20)
        self.myparent.show_jans_dialog(dialog)


    def delete_property(self, **data):
        pass

    def display_property(self, **data):
        pass

    def __pt_container__(self):
        return self.dialog

