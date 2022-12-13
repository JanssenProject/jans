from typing import Optional, Sequence, Callable
import asyncio
from functools import partial
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.layout.containers import (
    HSplit,
    VSplit,
    DynamicContainer,
    Window,
)
from prompt_toolkit.widgets import (
    Button,
    Label,
    CheckboxList,
    Dialog,
)
from prompt_toolkit.eventloop import get_event_loop
from utils.static import DialogResult
from wui_components.jans_dialog_with_nav import JansDialogWithNav
from wui_components.jans_cli_dialog import JansGDialog
from utils.utils import DialogUtils, common_data
from wui_components.jans_vetrical_nav import JansVerticalNav
from prompt_toolkit.formatted_text import AnyFormattedText
from typing import Any, Optional
from prompt_toolkit.layout import ScrollablePane
from utils.multi_lang import _

class EditUserDialog(JansGDialog, DialogUtils):
    """This user editing dialog
    """
    def __init__(
            self,
            parent,
            data:dict,
            title: AnyFormattedText= "",
            buttons: Optional[Sequence[Button]]= [],
            save_handler: Callable= None,
            )-> Dialog:
        """init for `EditUserDialog`, inherits from two diffrent classes `JansGDialog` and `DialogUtils`
            
        JansGDialog (dialog): This is the main dialog Class Widget for all Jans-cli-tui dialogs except custom dialogs like dialogs with navbar
        DialogUtils (methods): Responsable for all `make data from dialog` and `check required fields` in the form for any Edit or Add New
                
        Args:
            parent (widget): This is the parent widget for the dialog, to access `Pageup` and `Pagedown`
            title (str): The Main dialog title
            data (list): selected line data 
            buttons (list, optional): Dialog main buttons with their handlers. Defaults to [].
            save_handler (method, optional): handler invoked when closing the dialog. Defaults to None.
        """
        super().__init__(parent, title, buttons)
        self.app = parent
        self.save_handler = save_handler
        self.data = data
        self.title=title
        self.admin_ui_roles = {}
        self.schema = self.app.cli_object.get_schema_from_reference('User-Mgt', '#/components/schemas/CustomUser')
        self.create_window()

    def cancel(self) -> None:
        """method to invoked when canceling changes in the dialog (Cancel button is pressed)
        """

        self.future.set_result(DialogResult.CANCEL)

    def get_claim_properties(self, claim):
        """This method for getting claims properties

        Args:
            claim (str): Claim

        Returns:
            _type_: properties
        """

        ret_val = {}
        for tmp in common_data.users.claims:
            if tmp['name'] == claim:
                ret_val = tmp
                break

        return ret_val

    def create_window(self) -> None:

        def get_custom_attribute(attribute, multi=False):
            for ca in self.data.get('customAttributes', []):
                if ca['name'] == attribute:
                    values = ca.get('values', [])
                    #check if there is a bool value
                    for val in values:
                        if isinstance(val, bool):
                            return val
                    if multi:
                        return values
                    while None in values:
                        values.remove(None)
                    ret_val = ', '.join(values)
                    return ret_val
            return [] if multi else ''

        if self.data:
            active_checked = self.data.get('jansStatus', '').lower() == 'active'
        else:
            active_checked = True

        self.edit_user_content = [
                    self.app.getTitledText(_("Inum"), name='inum', value=self.data.get('inum',''), style='class:script-titledtext', jans_help=self.app.get_help_from_schema(self.schema, 'inum'), read_only=True),
                    self.app.getTitledText(_("Username *"), name='userId', value=self.data.get('userId',''), style='class:script-titledtext', jans_help=self.app.get_help_from_schema(self.schema, 'userId')),
                    self.app.getTitledText(_("First Name"), name='givenName', value=self.data.get('givenName',''), style='class:script-titledtext', jans_help=self.app.get_help_from_schema(self.schema, 'givenName')),
                    self.app.getTitledText(_("Middle Name"), name='middleName', value=get_custom_attribute('middleName'), style='class:script-titledtext', jans_help=self.app.get_help_from_schema(self.schema, 'middleName')),
                    self.app.getTitledText(_("Last Name"), name='sn', value=get_custom_attribute('sn'), style='class:script-titledtext', jans_help=self.app.get_help_from_schema(self.schema, 'sn')),
                    self.app.getTitledText(_("Display Name"), name='displayName', value=self.data.get('displayName',''), style='class:script-titledtext', jans_help=self.app.get_help_from_schema(self.schema, 'displayName')),
                    self.app.getTitledText(_("Email *"), name='mail', value=self.data.get('mail',''), style='class:script-titledtext', jans_help=self.app.get_help_from_schema(self.schema, 'mail')),
                    self.app.getTitledCheckBox(_("Active"), name='active', checked=active_checked, style='class:script-checkbox', jans_help=self.app.get_help_from_schema(self.schema, 'enabled')),
                    self.app.getTitledText(_("Nickname"), name='nickname', value='\n'.join(get_custom_attribute('nickname', multi=True)), style='class:script-titledtext', height=3, jans_help=self.app.get_help_from_schema(self.schema, 'nickname')),

                    Button(_("Add Claim"), handler=self.add_claim),
                ]


        if self.app.plugin_enabled('config_api'):
            admin_ui_roles = [[role] for role in get_custom_attribute('jansAdminUIRole', multi=True) ]
            admin_ui_roles_label = _("jansAdminUIRole")
            add_admin_ui_role_label = _("Add Admin UI Role")
            self.admin_ui_roles_container = JansVerticalNav(
                    myparent=self.app,
                    headers=['Role'],
                    preferred_size=[20],
                    data=admin_ui_roles,
                    on_delete=self.delete_admin_ui_role,
                    selectes=0,
                    headerColor='class:outh-client-navbar-headcolor',
                    entriesColor='class:outh-client-navbar-entriescolor',
                    all_data=admin_ui_roles,
                    underline_headings=False,
                    max_width=25,
                    jans_name='configurationProperties',
                    max_height=False
                    )

            self.edit_user_content.insert(-1,
                    VSplit([ 
                        Label(text=admin_ui_roles_label, style='class:script-label', width=len(admin_ui_roles_label)+1), 
                        self.admin_ui_roles_container,
                        Window(width=2),
                        HSplit([
                                Window(height=1),
                                Button(text=add_admin_ui_role_label, width=len(add_admin_ui_role_label)+4, handler=self.add_admin_ui_role),
                                ]),
                        ], height=4, width=D())
                    )


        if not self.data:
            self.edit_user_content.insert(2,
                    self.app.getTitledText(_("Password *"), name='userPassword', value='', style='class:script-titledtext', jans_help=self.app.get_help_from_schema(self.schema, 'userPassword'))
                )

        for ca in self.data.get('customAttributes', []):

            if ca['name'] in ('middleName', 'sn', 'jansStatus', 'nickname', 'jansActive'):
                continue

            claim_prop = self.get_claim_properties(ca['name'])

            if claim_prop.get('dataType', 'string') in ('string', 'json'):
                value = get_custom_attribute(ca['name'])
                self.edit_user_content.insert(-1, 
                    self.app.getTitledText(_(claim_prop.get('displayName', ca['name'])), name=ca['name'], value=value, style='class:script-titledtext', jans_help=self.app.get_help_from_schema(self.schema, ca['name']))
                )

            elif claim_prop.get('dataType') == 'boolean':
                checked = get_custom_attribute(ca['value']).lower() == 'true'
                self.edit_user_content.insert(-1, 
                    self.app.getTitledCheckBox(_(claim_prop['displayName']), name=ca['name'], checked=checked, style='class:script-checkbox', jans_help=self.app.get_help_from_schema(self.schema, ca['name']))
                )

        self.edit_user_container = ScrollablePane(content=HSplit(self.edit_user_content, width=D()),show_scrollbar=False)




        self.dialog = JansDialogWithNav(
            title=self.title,
            content=DynamicContainer(lambda: self.edit_user_container),
            button_functions=[(self.cancel, _("Cancel")), (partial(self.save_handler, self), _("Save"))],
            height=self.app.dialog_height,
            width=self.app.dialog_width,
            )

    def get_admin_ui_roles(self) -> None:
        """This method for getting admin ui roles
        """
        async def coroutine():
            cli_args = {'operation_id': 'get-all-adminui-roles'}
            self.app.start_progressing(_("Retreiving admin UI roles from server..."))
            response = await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing()
            self.admin_ui_roles = response.json()
            self.add_admin_ui_role()
        asyncio.ensure_future(coroutine())

    def add_admin_ui_role(self) -> None:
        """This method for adding new admin ui roles
        """
        if not self.admin_ui_roles:
            self.get_admin_ui_roles()
            return

        ui_roles_to_be_added = []
        for role in self.admin_ui_roles:
            for cur_role in self.admin_ui_roles_container.data:
                if cur_role[0] == role['role']:
                    break
            else:
                ui_roles_to_be_added.append([role['role'], role['role']])

        admin_ui_roles_checkbox = CheckboxList(values=ui_roles_to_be_added)

        def add_role(dialog) -> None:
            for role_ in admin_ui_roles_checkbox.current_values:
                self.admin_ui_roles_container.add_item([role_])

        body = HSplit([Label(_("Select Admin-UI role to be added to current user.")), admin_ui_roles_checkbox])
        buttons = [Button(_("Cancel")), Button(_("OK"), handler=add_role)]
        dialog = JansGDialog(self.app, title=_("Select Admin-UI"), body=body, buttons=buttons, width=self.app.dialog_width-20)
        self.app.show_jans_dialog(dialog)

    def delete_admin_ui_role(self, **kwargs: Any) -> None:
        """This method for deleting admin ui roles
        """
        self.admin_ui_roles_container.remove_item(kwargs['selected'])

    def add_claim(self) -> None:
        """This method for adding new claim
        """
        cur_claims = []
        for w in self.edit_user_content:
            if hasattr(w, 'me'):
                cur_claims.append(w.me.window.jans_name)

        claims_list = []
        for claim in common_data.users.claims:
            if not claim['oxMultiValuedAttribute'] and claim['name'] in cur_claims:
                continue
            if claim['name'] in ('memberOf', 'userPassword', 'uid', 'jansStatus', 'jansActive', 'updatedAt'):
                continue
            claims_list.append((claim['name'], claim['displayName']))

        claims_checkbox = CheckboxList(values=claims_list)

        def add_claim(dialog) -> None:
            for claim_ in claims_checkbox.current_values:
                for claim_prop in common_data.users.claims:
                    if claim_prop['name'] == claim_:
                        break
                display_name = claim_prop['displayName']
                if claim_prop['dataType'] == 'boolean':
                    widget = self.app.getTitledCheckBox(_(display_name), name=claim_, style='class:script-checkbox', jans_help=self.app.get_help_from_schema(self.schema, claim_))
                else:
                    widget = self.app.getTitledText(_(display_name), name=claim_, value='', style='class:script-titledtext', jans_help=self.app.get_help_from_schema(self.schema, claim_))
                self.edit_user_content.insert(-1, widget)
            self.edit_user_container = ScrollablePane(content=HSplit(self.edit_user_content, width=D()),show_scrollbar=False)


        body = HSplit([Label(_("Select claim to be added to current user.")), claims_checkbox])
        buttons = [Button(_("Cancel")), Button(_("OK"), handler=add_claim)]
        dialog = JansGDialog(self.app, title=_("Claims"), body=body, buttons=buttons, width=self.app.dialog_width-20)
        self.app.show_jans_dialog(dialog)

    def __pt_container__(self)-> Dialog:
        """The container for the dialog itself

        Returns:
            Dialog: The Edit User Dialog
        """

        return self.dialog

