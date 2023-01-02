from typing import Any, Optional, Sequence, Union, TypeVar, Callable
from asyncio import ensure_future

from prompt_toolkit.layout.dimension import D
from prompt_toolkit.layout.containers import (
    HSplit,
    VSplit,
    DynamicContainer,
    Window,
    AnyContainer
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

from prompt_toolkit.buffer import Buffer
from prompt_toolkit.widgets.base import RadioList
from prompt_toolkit.layout.containers import FloatContainer
from prompt_toolkit.formatted_text import AnyFormattedText
from prompt_toolkit.layout.dimension import AnyDimension

from cli import config_cli
from utils.static import DialogResult, CLI_STYLE
from utils.utils import DialogUtils
from utils.multi_lang import _
from wui_components.jans_dialog_with_nav import JansDialogWithNav
from wui_components.jans_side_nav_bar import JansSideNavBar
from wui_components.jans_cli_dialog import JansGDialog
from wui_components.jans_drop_down import DropDownWidget
from wui_components.jans_vetrical_nav import JansVerticalNav
from view_uma_dialog import ViewUMADialog


class EditScopeDialog(JansGDialog, DialogUtils):
    """The Main Scope Dialog that contain every thing related to The Scope
    """
    def __init__(
        self,
        parent,
        title: AnyFormattedText,
        data: list,
        buttons: Optional[Sequence[Button]]= [],
        save_handler: Callable= None,
        )-> Dialog:
        """init for `EditScopeDialog`, inherits from two diffrent classes `JansGDialog` and `DialogUtils`
            
        DialogUtils (methods): Responsable for all `make data from dialog` and `check required fields` in the form for any Edit or Add New
        JansGDialog (dialog): This is the main dialog Class Widget for all Jans-cli-tui dialogs except custom dialogs like dialogs with navbar
        
                Args:
            parent (widget): This is the parent widget for the dialog, to access `Pageup` and `Pagedown`
            title (str): The Main dialog title
            data (list): selected line data 
            button_functions (list, optional): Dialog main buttons with their handlers. Defaults to [].
            save_handler (method, optional): handler invoked when closing the dialog. Defaults to None.
        """
        super().__init__(parent, title, buttons)
        self.save_handler = save_handler
        self.data = data
        self.title = title
        self.claims_container = None
        self.showInConfigurationEndpoint = self.data.get('attributes', {}).get('showInConfigurationEndpoint', '')
        self.defaultScope = self.data.get('defaultScope', '')
        self.schema = self.myparent.cli_object.get_schema_from_reference('', '#/components/schemas/Scope')
        self.tbuffer = None
        self.prepare_tabs()
        self.create_window()
        self.sope_type = self.data.get('scopeType') or 'oauth'

    def save(self) -> None:
        """method to invoked when saving the dialog (Save button is pressed)
        """

        self.myparent.logger.debug('SAVE SCOPE')

        data = {}

        for item in self.dialog.content.children + self.alt_tabs[self.sope_type].children:
            item_data = self.get_item_data(item)
            if item_data:
                data[item_data['key']] = item_data['value']

        if data['scopeType'] in ('openid', 'dynamic') and hasattr(self, 'claims_container') and self.claims_container:
            claims = [claim[0] for claim in self.claims_container.data]
            data['claims'] = claims

        self.myparent.logger.debug('DATA: ' + str(data))
        self.data = data
        if 'attributes' in self.data.keys():
            self.data['attributes'] = {'showInConfigurationEndpoint':self.data['attributes']}

        cfr = self.check_required_fields(self.dialog.content)
        self.myparent.logger.debug('CFR: '+str(cfr))
        if not cfr:
            return

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
        scope_types = [('oauth', 'OAuth'), ('openid', 'OpenID'), ('dynamic', 'Dynamic'), ('uma', 'UMA')]
        buttons = [(self.save, _("Save")), (self.cancel, _("Cancel"))]
        if self.data:
            if self.data.get('scopeType') == 'spontaneous':
                scope_types.insert(3, ('spontaneous', 'Spontaneous'))
                buttons.pop(0)

            if self.data.get('scopeType') == 'uma':
                buttons.pop(0)
            else:
                for stype in scope_types[:]:
                    if stype[0] == 'uma':
                        scope_types.remove(stype)

        self.dialog = JansDialogWithNav(
            title=self.title,
            content= HSplit([
                self.myparent.getTitledRadioButton(
                                _("Scope Type"),
                                name='scopeType',
                                current_value=self.data.get('scopeType'),
                                values=scope_types,
                                on_selection_changed=self.scope_selection_changed,
                                jans_help=self.myparent.get_help_from_schema(self.schema, 'scopeType'),
                                style=CLI_STYLE.radio_button),

                self.myparent.getTitledText(
                    _("id"), 
                    name='id', 
                    value=self.data.get('id',''), 
                    jans_help=self.myparent.get_help_from_schema(self.schema, 'id'),
                    style=CLI_STYLE.edit_text_required),

                self.myparent.getTitledText(
                    _("inum"), 
                    name='inum', 
                    value=self.data.get('inum',''), 
                    jans_help=self.myparent.get_help_from_schema(self.schema, 'inum'),
                    style=CLI_STYLE.edit_text,
                    read_only=True,),

                self.myparent.getTitledText(
                    _("Display Name"), 
                    name='displayName', 
                    value=self.data.get('displayName',''),
                    jans_help=self.myparent.get_help_from_schema(self.schema, 'displayName'),
                    style=CLI_STYLE.edit_text),

                self.myparent.getTitledText(
                    _("Description"), 
                    name='description', 
                    value=self.data.get('description',''), 
                    jans_help=self.myparent.get_help_from_schema(self.schema, 'description'),
                    style=CLI_STYLE.edit_text),

                DynamicContainer(lambda: self.alt_tabs[self.sope_type]),
            ], style='class:outh-scope-tabs'),
             button_functions=buttons,
            height=self.myparent.dialog_height,
            width=self.myparent.dialog_width,
                   )

    def scope_selection_changed(
        self, 
        cb: RadioList,
        ) -> None:
        """This method for scope type selection set

        Args:
            cb (RadioList): the New Value from the nav-bar
        """

        self.sope_type = cb.current_value

    def get_named_claims(
        self,
        claims_list:list
        ) -> list:
        """This method for getting claim name

        Args:
            claims_list (list): List for Claims

        Returns:
            list: List with Names retlated to that claims
        """

        calims_names = []

        try :
            response = self.myparent.cli_object.process_command_by_id(
                        operation_id='get-attributes',
                        url_suffix='',
                        endpoint_args='',
                        data_fn=None,
                        data={}
                        )
        except Exception as e:
                self.myparent.show_message(_("Error getting claims"), str(e))
                return calims_names

        if response.status_code not in (200, 201):
            self.myparent.show_message(_("Error getting claims"), str(response.text))
            return calims_names

        result = response.json()

        for entry in result["entries"] :
            for claim in claims_list:
                if claim == entry['dn']:
                    calims_names.append([entry['dn'], entry['displayName']])

        return calims_names

    def delete_claim(self, **kwargs: Any) -> None:
        """This method for the deletion of claim

        Args:
            selected (_type_): The selected claim
            event (_type_): _description_

        """

        dialog = self.myparent.get_confirm_dialog(_("Are you sure want to delete claim dn:")+"\n {} ?".format(kwargs['selected'][0]))
        async def coroutine():
            focused_before = self.myparent.layout.current_window
            result = await self.myparent.show_dialog_as_float(dialog)
            try:
                self.myparent.layout.focus(focused_before)
            except Exception:
                self.myparent.layout.focus(self.myparent.center_frame) ##

            if result.lower() == 'yes':
                self.data['claims'].remove(kwargs['selected'][0])
                self.claims_container.data.remove(kwargs['selected'])


        ensure_future(coroutine())

    def prepare_tabs(self) -> None:
        """Prepare the tabs for Edil Scope Dialogs
        """

        self.alt_tabs = {}


        self.alt_tabs['oauth'] = HSplit([
                            self.myparent.getTitledCheckBox(
                                    _("Default Scope"),
                                    name='defaultScope',
                                    checked=self.data.get('defaultScope'),
                                    jans_help=self.myparent.get_help_from_schema(self.schema, 'defaultScope'),
                                    style=CLI_STYLE.check_box,
                            ),

                            self.myparent.getTitledCheckBox(
                                    _("Show in configuration endpoint"),
                                    name='showInConfigurationEndpoint',
                                    checked=self.data.get('attributes',{}).get('showInConfigurationEndpoint',''),
                                    jans_help='Configuration Endpoint',
                                    style=CLI_STYLE.check_box,
                            ),

                        ],width=D(),)


        open_id_widgets = [
                            self.myparent.getTitledCheckBox(
                                    _("Default Scope"),
                                    name='defaultScope',
                                    checked=self.data.get('defaultScope'),
                                    jans_help=self.myparent.get_help_from_schema(self.schema, 'defaultScope'),
                                    style=CLI_STYLE.check_box,
                            ),

                            self.myparent.getTitledCheckBox(
                                    _("Show in configuration endpoint"),
                                    name='showInConfigurationEndpoint',
                                    checked=self.data.get('attributes',{}).get('showInConfigurationEndpoint',''),
                                    jans_help='Configuration Endpoint',
                                    style=CLI_STYLE.check_box,
                            ),

                            # Window(char='-', height=1),

                            # HorizontalLine(),
                            self.myparent.getTitledText(
                                    _("Search"), 
                                    name='__search_claims__',
                                    style='class:outh-scope-textsearch',width=10,
                                    jans_help=_("Press enter to perform search"),
                                    accept_handler=self.search_claims,
                                    ),
                            ]
        calims_data = self.get_named_claims(self.data.get('claims', []))


        self.claims_container = JansVerticalNav(
                myparent=self.myparent,
                headers=['dn', 'Display Name'],
                preferred_size= [0,0],
                data=calims_data,
                on_display=self.myparent.data_display_dialog,
                on_delete=self.delete_claim,
                selectes=0,
                headerColor='class:outh-client-navbar-headcolor',
                entriesColor='class:outh-client-navbar-entriescolor',
                all_data=calims_data
                )

        open_id_widgets.append(self.claims_container)

        self.alt_tabs['openid'] = HSplit(open_id_widgets, width=D())

        self.alt_tabs['dynamic'] = HSplit([
                        
                        self.myparent.getTitledText(_("Dynamic Scope Script"),
                            name='dynamicScopeScripts',
                            value='\n'.join(self.data.get('dynamicScopeScripts', [])),
                            jans_help=self.myparent.get_help_from_schema(self.schema, 'dynamicScopeScripts'),
                            height=3, 
                            style=CLI_STYLE.edit_text),
                        
                        self.myparent.getTitledText(
                                _("Search"), 
                                name='__search_claims__',
                                style='class:outh-scope-textsearch',width=10,
                                jans_help=_("Press enter to perform search"), ),#accept_handler=self.search_scopes

                        self.myparent.getTitledText(
                                _("Claims"),
                                name='claims',
                                value='\n'.join(self.data.get('claims', [])),
                                height=3, 
                                jans_help=self.myparent.get_help_from_schema(self.schema, 'claims'),
                                style=CLI_STYLE.edit_text),

                        ],width=D(),
                    )

        self.alt_tabs['spontaneous'] = HSplit([
                    self.myparent.getTitledText(
                        _("Associated Client"), 
                        name='none', 
                        value=self.data.get('none',''), 
                        style=CLI_STYLE.edit_text,
                        read_only=True,
                        jans_help=self.myparent.get_help_from_schema(self.schema, 'none'),
                        height=3,),## Not fount

                    self.myparent.getTitledText(
                        _("Creationg time"), 
                        name='creationDate', 
                        value=self.data.get('creationDate',''), 
                        jans_help=self.myparent.get_help_from_schema(self.schema, 'creationDate'),
                        style=CLI_STYLE.edit_text,
                        read_only=True,),

                                                ],width=D(),
                    )

        uma_creator = self.data.get('creatorId','') or self.myparent.cli_object.get_user_info().get('inum','')


        self.alt_tabs['uma'] = HSplit([
                    self.myparent.getTitledText(
                        _("IconURL"), 
                        name='iconUrl', 
                        value=self.data.get('iconUrl',''), 
                        jans_help=self.myparent.get_help_from_schema(self.schema, 'iconUrl'),
                        style=CLI_STYLE.edit_text),
                    

                    self.myparent.getTitledText(_("Authorization Policies"),
                            name='umaAuthorizationPolicies',
                            value='\n'.join(self.data.get('umaAuthorizationPolicies', [])),
                            height=3, 
                            jans_help=self.myparent.get_help_from_schema(self.schema, 'umaAuthorizationPolicies'),
                            style=CLI_STYLE.edit_text),

                    self.myparent.getTitledText(
                        _("Associated Client"), 
                        name='none', 
                        value=self.data.get('none',''), 
                        jans_help=self.myparent.get_help_from_schema(self.schema, 'none'),
                        style=CLI_STYLE.edit_text,
                        read_only=True,
                        height=3,), ## Not fount

                    self.myparent.getTitledText(
                        _("Creationg time"), 
                        name='description', 
                        value=self.data.get('description',''), 
                        jans_help=self.myparent.get_help_from_schema(self.schema, 'description'),
                        style=CLI_STYLE.edit_text,
                        read_only=True,),

                    self.myparent.getTitledText(
                                    _("Creator"), 
                                    name='Creator',
                                    style=CLI_STYLE.edit_text,
                                    jans_help=self.myparent.get_help_from_schema(self.schema, 'Creator'),
                                    read_only=True,
                                    value=uma_creator
                                    ),
                    ],
                    width=D(),
                    )

    def search_claims(
        self, 
        textbuffer: Buffer,
        ) -> None:
        """This method handel the search for claims and adding new claims

        Args:
            tbuffer (Buffer): Buffer returned from the TextArea widget > GetTitleText
        """

        try :
            response = self.myparent.cli_object.process_command_by_id(
                        operation_id='get-attributes',
                        url_suffix='',
                        endpoint_args='pattern:{}'.format(textbuffer.text),
                        data_fn=None,
                        data={}
                        )
        except Exception as e:
                    self.myparent.show_message(_("Error searching claims"), str(e))
                    return

        result = response.json()

        if not result.get('entries'):
            self.myparent.show_message(_("Ooops"), _("Can't find any claim for ʹ%sʹ.") % textbuffer.text)
            return


        def add_selected_claims(dialog):
            if 'claims' not in self.data:
                self.data['claims'] = []

            for item in dialog.body.current_values:
                self.claims_container.add_item(item)
                self.data['claims'].append(item[0])

        current_data = self.get_named_claims(self.data.get('claims', []))

        for i in range(len(current_data)):
            current_data[i] = current_data[i][0]

        values = [([claim['dn'], claim['displayName']], claim['displayName']) for claim in result['entries']]
        values_uniqe = []

        for k in values:
            if k[0][0] not in current_data:
                values_uniqe.append(k)

        if not values_uniqe:
            self.myparent.show_message(_("Ooops"), _("Can't find any New claim for ʹ%sʹ.") % textbuffer.text)
            return

        check_box_list = CheckboxList(
            values=values_uniqe,
            )
        buttons = [Button(_("Cancel")), Button(_("OK"), handler=add_selected_claims)]
        dialog = JansGDialog(self.myparent, title=_("Select claims to add"), body=check_box_list, buttons=buttons)
        self.myparent.show_jans_dialog(dialog)

    def __pt_container__(self) -> Dialog:
        """The container for the dialog itself

        Returns:
            Dialog: The Edit Scope Dialog
        """

        return self.dialog

