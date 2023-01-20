from functools import partial
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.layout.containers import (
    HSplit,
    VSplit,
    Window
)
from prompt_toolkit.widgets import (
    Button,
    Label,
    TextArea,
    RadioList,
    Button,
    Dialog,
)
import asyncio
from prompt_toolkit.lexers import PygmentsLexer
from pygments.lexers.python import PythonLexer
from pygments.lexers.jvm import JavaLexer
from utils.static import DialogResult
from wui_components.jans_dialog_with_nav import JansDialogWithNav
from wui_components.jans_cli_dialog import JansGDialog
from wui_components.jans_drop_down import DropDownWidget
from utils.utils import DialogUtils
from wui_components.jans_vetrical_nav import JansVerticalNav
from wui_components.jans_spinner import Spinner
from prompt_toolkit.formatted_text import AnyFormattedText
from typing import Optional, Sequence
from typing import Callable
from typing import Any, Optional
from utils.multi_lang import _

class EditScriptDialog(JansGDialog, DialogUtils):
    """This Script editing dialog
    """
    def __init__(
        self,
        parent,
        data:list,
        title: AnyFormattedText= "",
        buttons: Optional[Sequence[Button]]= [],
        save_handler: Callable= None,
        )-> Dialog:
        """init for `EditScriptDialog`, inherits from two diffrent classes `JansGDialog` and `DialogUtils`
            
        JansGDialog (dialog): This is the main dialog Class Widget for all Jans-cli-tui dialogs except custom dialogs like dialogs with navbar
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
        self.cur_lang = self.data.get('programmingLanguage', 'python')
        self.create_window()
        self.script = self.data.get('script','')

    def save(self) -> None:
        """method to invoked when saving the dialog (Save button is pressed)
        """

        data = {}

        for item in self.edit_dialog_content:
            item_data = self.get_item_data(item)
            if item_data:
                data[item_data['key']] = item_data['value']

        for prop_container in (self.config_properties_container, self.module_properties_container):

            if prop_container.data:
                data[prop_container.jans_name] = []
                for prop_ in prop_container.data:
                    key_ = prop_[0]
                    val_ = prop_[1]

                    if key_:
                        prop = {'value1': key_}
                        if val_:
                            prop['value2'] = val_
                        if len(prop_) > 2:
                            prop['hide'] = prop_[2]
                        data[prop_container.jans_name].append(prop)

        data['locationType'] = data.get('locationType')
        data['internal'] = self.data.get('internal', False)
        data['modified'] = self.data.get('modified', False)
        data['revision'] = self.data.get('revision', 0) + 1
        data['script'] = self.script

        if data['locationType'] != 'file':
            data.pop('locationPath', None)

        if self.data.get('baseDn'):
            data['baseDn'] = self.data['baseDn']

        self.new_data = data

        close_me = True
        if self.save_handler:
            close_me = self.save_handler(self)
        if close_me:
            self.future.set_result(DialogResult.ACCEPT)

    def cancel(self) -> None:
        """method to invoked when canceling changes in the dialog (Cancel button is pressed)
        """

        self.future.set_result(DialogResult.CANCEL)

    def create_window(self) -> None:

        schema = self.myparent.cli_object.get_schema_from_reference('', '#/components/schemas/CustomScript')

        script_types = [
                        ['person_authentication', 'Person Authentication'],
                        ['consent_gathering', 'Consent Gathering'],
                        ['post_authn', 'Post Authentication'],
                        ['id_token', 'id_token'],
                        ['password_grant', 'Password Grant'],
                        ['ciba_end_user_notification', 'CIBA End User Notification'],
                        #['OpenID Configuration', 'OpenID Configuration'],
                        ['dynamic_scope', 'Dynamic Scope', ],
                        ['spontaneous_scope', 'Spontaneous Scope',],
                        ['application_session', 'Application Session'],
                        ['end_session', 'End Session'],
                        ['client_registration', 'Client Registration'],
                        ['introspection', 'Introspection'],
                        ['update_token', 'Update Token'],
                        ['config_api', 'Config API'],
                        ['idp', 'IDP'],
                        ['resource_owner_password_credentials', 'Resource Owner Password Credentials'],
                        ['cache_refresh', 'Cache Refresh'],
                        ['id_generator', 'Id Generator'],
                        ['uma_rpt_policy', 'Uma Rpt Policy'],
                        ['uma_rpt_claims', 'Uma Rpt Claims'],
                        ['uma_claims_gathering', 'Uma Claims Gathering'],
                        ['scim', 'SCIM'],
                        ['revoke_token', 'Revoke Token'],
                        ['persistence_extension', 'Persistence Extension'],
                        ['discovery', 'Discovery'],
                        ]

        self.location_widget = self.myparent.getTitledText(
            _("          Path"), 
            name='locationPath', 
            value=self.data.get('locationPath',''),
            style='class:script-titledtext', 
            jans_help="locationPath"
            )

        self.set_location_widget_state(self.data.get('locationType') == 'file')

        config_properties_title = _("Conf. Properties: ")
        add_property_title = _("Add Property")
        module_properties_title = _("Module Properties: ")

        config_properties_data = []
        for prop in self.data.get('configurationProperties', []):
            config_properties_data.append([prop['value1'], prop.get('value2', ''), prop.get('hide', False)])

        self.config_properties_container = JansVerticalNav(
                myparent=self.myparent,
                headers=['Key', 'Value', 'Hide'],
                preferred_size=[15, 15, 5],
                data=config_properties_data,
                on_enter=self.edit_property,
                on_delete=self.delete_config_property,
                on_display=self.myparent.data_display_dialog,
                get_help=(self.get_help,'Properties'),
                selectes=0,
                headerColor='class:outh-client-navbar-headcolor',
                entriesColor='class:outh-client-navbar-entriescolor',
                all_data=config_properties_data,
                underline_headings=False,
                max_width=52,
                jans_name='configurationProperties',
                max_height=False
                )

        module_properties_data = []
        for prop in self.data.get('moduleProperties', []):
            module_properties_data.append([prop['value1'], prop.get('value2', '')])

        self.module_properties_container = JansVerticalNav(
                myparent=self.myparent,
                headers=['Key', 'Value'],
                preferred_size=[20, 20],
                data=module_properties_data,
                on_enter=self.edit_property,
                on_delete=self.delete_config_property,
                on_display=self.myparent.data_display_dialog,
                get_help=(self.get_help,'Properties'),
                selectes=0,
                headerColor='class:outh-client-navbar-headcolor',
                entriesColor='class:outh-client-navbar-entriescolor',
                all_data=module_properties_data,
                underline_headings=False,
                max_width=44,
                jans_name='moduleProperties',
                max_height=3
                )

        open_editor_button_title = _("Edit Script")
        open_editor_button = Button(text=open_editor_button_title, width=len(open_editor_button_title)+2, handler=self.edit_script_dialog)
        open_editor_button.window.jans_help="Enter to open editing window"

        self.edit_dialog_content = [
                    self.myparent.getTitledText(_("Inum"), name='inum', value=self.data.get('inum',''), style='class:script-titledtext', jans_help=self.myparent.get_help_from_schema(schema, 'inum'), read_only=True),
                    self.myparent.getTitledWidget(
                                _("Script Type"),
                                name='scriptType',
                                widget=DropDownWidget(
                                    values=script_types,
                                    value=self.data.get('scriptType', '')
                                    ),
                                jans_help=self.myparent.get_help_from_schema(schema, 'scriptType'),
                                style='class:outh-client-dropdown'),

                    self.myparent.getTitledCheckBox(_("Enabled"), name='enabled', checked=self.data.get('enabled'), style='class:script-checkbox', jans_help=self.myparent.get_help_from_schema(schema, 'enabled')),
                    self.myparent.getTitledText(_("Name"), name='name', value=self.data.get('name',''), style='class:script-titledtext', jans_help=self.myparent.get_help_from_schema(schema, 'name')),
                    self.myparent.getTitledText(_("Description"), name='description', value=self.data.get('description',''), style='class:script-titledtext', jans_help=self.myparent.get_help_from_schema(schema, 'description')),

                    self.myparent.getTitledRadioButton(
                            _("Location"),
                            name='locationType',
                            values=[('ldap', _("Database")), ('file', _("File System"))],
                            current_value= 'file' if self.data.get('locationType') == 'file' else 'ldap',
                            jans_help=_("Where to save script"),
                            style='class:outh-client-radiobutton',
                            on_selection_changed=self.script_location_changed,
                            ),

                     self.location_widget,

                    self.myparent.getTitledWidget(
                                _("Programming Language"),
                                name='programmingLanguage',
                                widget=DropDownWidget(
                                    values=[['python', 'Python'], ['java', 'Java']],
                                    value=self.cur_lang,
                                    on_value_changed=self.script_lang_changed,
                                    ),
                                jans_help=self.myparent.get_help_from_schema(schema, 'programmingLanguage'),
                                style='class:outh-client-dropdown'),

                    self.myparent.getTitledWidget(
                                _("Level"),
                                name='level',
                                widget=Spinner(
                                    value=self.data.get('level', 0)
                                    ),
                                jans_help=self.myparent.get_help_from_schema(schema, 'level'),
                                style='class:outh-client-dropdown'),

                    VSplit([
                            Label(text=config_properties_title, style='class:script-label', width=len(config_properties_title)+1), 
                            self.config_properties_container,
                            Window(width=2),
                            HSplit([
                                Window(height=1),
                                Button(text=add_property_title, width=len(add_property_title)+4, handler=partial(self.edit_property, jans_name='configurationProperties')),
                                ]),
                            ],
                            height=5, width=D(),
                            ),

                    VSplit([
                            Label(text=module_properties_title, style='class:script-label', width=len(module_properties_title)+1), 
                            self.module_properties_container,
                            Window(width=2),
                            HSplit([
                                Window(height=1),
                                Button(text=add_property_title, width=len(add_property_title)+4, handler=partial(self.edit_property, jans_name='moduleProperties')),
                                ]),
                            ],
                             height=5
                            ),
                    VSplit([open_editor_button, Window(width=D())]),
                    ]


        self.dialog = JansDialogWithNav(
            title=self.title,
            content= HSplit(
                self.edit_dialog_content,
                width=D(),
                height=D()
                ),
            button_functions=[(self.cancel, _("Cancel")), (self.save, _("Save"))],
            height=self.myparent.dialog_height,
            width=self.myparent.dialog_width,
            )
    
    def get_help(self, **kwargs: Any):
        """This method get focused field Description to display on statusbar
        """

        # schema = self.app.cli_object.get_schema_from_reference('#/components/schemas/{}'.format(str(kwargs['scheme'])))
    
        if kwargs['scheme'] == 'Properties':
            self.myparent.status_bar_text= kwargs['data'][0]

    def script_lang_changed(
        self, 
        value: str,
        ) -> None:
        """Change the script lang

        Args:
            value (str): lang to change to (python, java)
        """
        self.cur_lang = value

    def set_location_widget_state(
        self, 
        state: bool,
        ) -> None:
        """This method check the state of the location to save script

        Args:
            state (bool): state is changed or not
        """
        self.location_widget.me.read_only = not state
        self.location_widget.me.focusable = state
        if not state:
            self.location_widget.me.text = ''

    def script_location_changed(
        self, 
        redio_button: RadioList,
        ) -> None:
        """Location to save Script

        Args:
            redio_button (RadioList): Where to save the scripts (Database, Filesystem)
        """
        state = redio_button.current_value == 'file'
        self.set_location_widget_state(state)

    def edit_property(self, **kwargs: Any) -> None:
        """This method for editing the properties 
        """

        if kwargs['jans_name'] == 'moduleProperties':
            key, val = kwargs.get('data', ('',''))
            title = _("Enter Module Properties")
        else:
            key, val, hide = kwargs.get('data', ('','', False))
            hide_widget = self.myparent.getTitledCheckBox(_("Hide"), name='property_hide', checked=hide, style='class:script-titledtext', jans_help=_("Hide script property?"))
            title = _("Enter Configuration Properties")

        key_widget = self.myparent.getTitledText(_("Key"), name='property_key', value=key, style='class:script-titledtext', jans_help=_("Script propery Key"))
        val_widget = self.myparent.getTitledText(_("Value"), name='property_val', value=val, style='class:script-titledtext', jans_help=_("Script property Value"))

        def add_property(dialog: Dialog) -> None:
            key_ = key_widget.me.text
            val_ = val_widget.me.text
            cur_data = [key_, val_]

            if kwargs['jans_name'] == 'configurationProperties':
                hide_ = hide_widget.me.checked
                cur_data.append(hide_)
                container = self.config_properties_container
            else:
                container = self.module_properties_container
            if not kwargs.get('data'):
                container.add_item(cur_data)
            else:
                container.replace_item(kwargs['selected'], cur_data)

        body_widgets = [key_widget, val_widget]
        if kwargs['jans_name'] == 'configurationProperties':
            body_widgets.append(hide_widget)

        body = HSplit(body_widgets)
        buttons = [Button(_("Cancel")), Button(_("OK"), handler=add_property)]
        dialog = JansGDialog(self.myparent, title=title, body=body, buttons=buttons, width=self.myparent.dialog_width-20)
        self.myparent.show_jans_dialog(dialog)

    def delete_config_property(self, **kwargs: Any) -> None:
        """This method for deleting the coniguration of properties
        """
        dialog = self.myparent.get_confirm_dialog(_("Are you sure want to delete property with Key:")+"\n {} ?".format(kwargs['selected'][0]))

        async def coroutine():
            focused_before = self.myparent.layout.current_window
            result = await self.myparent.show_dialog_as_float(dialog)
            try:
                self.myparent.layout.focus(focused_before)
            except:
                self.myparent.stop_progressing()
                self.myparent.layout.focus(self.myparent.center_frame)

            if result.lower() == 'yes':
                if kwargs['jans_name'] == 'configurationProperties':
                    self.config_properties_container.remove_item(kwargs['selected'])
                else:
                    self.module_properties_container.remove_item(kwargs['selected'])
                self.myparent.stop_progressing()
                
            return result

        asyncio.ensure_future(coroutine())



    def edit_script_dialog(self) -> None:
        """This method shows the script itself and let the user view or edit it
        """

        text_editor = TextArea(
                text=self.script,
                multiline=True,
                height=self.myparent.dialog_height-10,
                width=D(),
                focusable=True,
                scrollbar=True,
                line_numbers=True,
                wrap_lines=False,
                lexer=PygmentsLexer(PythonLexer if self.cur_lang == 'PYTHON' else JavaLexer),
            )

        def modify_script(arg) -> None:
            self.script = text_editor.text

        buttons = [Button(_("Cancel")), Button(_("OK"), handler=modify_script)]
        dialog = JansGDialog(self.myparent, title=_("Edit Script"), body=HSplit([text_editor]), buttons=buttons, width=self.myparent.dialog_width-10)
        self.myparent.show_jans_dialog(dialog)

    def __pt_container__(self)-> Dialog:
        """The container for the dialog itself

        Returns:
            Dialog: The Edit Script Dialog
        """  
        return self.dialog

