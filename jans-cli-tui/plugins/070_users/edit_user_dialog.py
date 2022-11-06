import re
import threading

from typing import OrderedDict, Optional, Sequence, Union, TypeVar, Callable
import asyncio
from functools import partial


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
    TextArea,
    RadioList,
    CheckboxList,
    Button,
    Dialog,
)
from prompt_toolkit.eventloop import get_event_loop

from cli import config_cli
from utils.static import DialogResult
from wui_components.jans_dialog_with_nav import JansDialogWithNav
from wui_components.jans_side_nav_bar import JansSideNavBar
from wui_components.jans_cli_dialog import JansGDialog
from wui_components.jans_drop_down import DropDownWidget
from wui_components.jans_data_picker import DateSelectWidget
from utils.utils import DialogUtils
from wui_components.jans_vetrical_nav import JansVerticalNav
from prompt_toolkit.formatted_text import AnyFormattedText

from typing import Any, Optional


from view_uma_dialog import ViewUMADialog

from utils.multi_lang import _


class EditUserDialog(JansGDialog, DialogUtils):
    """This user editing dialog
    """
    def __init__(
            self,
            parent,
            data:list,
            title: AnyFormattedText= "",
            buttons: Optional[Sequence[Button]]= [],
            save_handler: Callable= None,
            )-> Dialog:
        """init for `EditUserDialog`, inherits from two diffrent classes `JansGDialog` and `DialogUtils`
            
        DialogUtils (methods): Responsable for all `make data from dialog` and `check required fields` in the form for any Edit or Add New
        
        Args:
            parent (widget): This is the parent widget for the dialog, to access `Pageup` and `Pagedown`
            title (str): The Main dialog title
            data (list): selected line data 
            button_functions (list, optional): Dialog main buttons with their handlers. Defaults to [].
            save_handler (method, optional): handler invoked when closing the dialog. Defaults to None.
        """
        super().__init__(parent, title, buttons)
        self.app = parent
        self.save_handler = save_handler
        self.data = data
        self.title=title
        self.create_window()
        self.admin_ui_roles = {}

    def save(self) -> None:
        pass

    def cancel(self) -> None:
        self.future.set_result(DialogResult.CANCEL)

    def create_window(self) -> None:

        schema = self.app.cli_object.get_schema_from_reference('User-Mgt', '#/components/schemas/CustomUser')

        def get_custom_attribute(attribute, multi=False):
            for ca in self.data.get('customAttributes', []):
                if ca['name'] == attribute:
                    if multi:
                        return ca['values']
                    return ', '.join(ca['values'])
            return [] if multi else ''

        if self.data:
            active_checked = self.data.get('jansStatus', '').lower() == 'active'
        else:
            active_checked = True


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


        member_of = [[grp] for grp in get_custom_attribute('memberOf', multi=True) ]
        member_of_label = _("Member of")
        add_group_label = _("Add Group")
        self.member_of_container = JansVerticalNav(
                myparent=self.app,
                headers=['Group'],
                preferred_size=[40],
                data=member_of,
                on_delete=self.delete_group,
                selectes=0,
                headerColor='class:outh-client-navbar-headcolor',
                entriesColor='class:outh-client-navbar-entriescolor',
                all_data=member_of,
                underline_headings=False,
                max_width=40,
                jans_name='configurationProperties',
                max_height=False
                )


        self.edit_user_content = [
                    self.app.getTitledText(_("Inum"), name='inum', value=self.data.get('inum',''), style='class:script-titledtext', jans_help=self.app.get_help_from_schema(schema, 'inum'), read_only=True),
                    self.app.getTitledText(_("First Name"), name='givenName', value=self.data.get('givenName',''), style='class:script-titledtext', jans_help=self.app.get_help_from_schema(schema, 'givenName')),
                    self.app.getTitledText(_("Middle Name"), name='middleName', value=get_custom_attribute('middleName'), style='class:script-titledtext', jans_help=self.app.get_help_from_schema(schema, 'middleName')),
                    self.app.getTitledText(_("Last Name"), name='sn', value=get_custom_attribute('sn'), style='class:script-titledtext', jans_help=self.app.get_help_from_schema(schema, 'sn')),
                    self.app.getTitledText(_("Username"), name='userId', value=self.data.get('userId',''), style='class:script-titledtext', jans_help=self.app.get_help_from_schema(schema, 'userId')),
                    self.app.getTitledText(_("Display Name"), name='displayName', value=self.data.get('displayName',''), style='class:script-titledtext', jans_help=self.app.get_help_from_schema(schema, 'displayName')),
                    self.app.getTitledText(_("Email"), name='mail', value=self.data.get('mail',''), style='class:script-titledtext', jans_help=self.app.get_help_from_schema(schema, 'mail')),
                    self.app.getTitledCheckBox(_("Active"), name='active', checked=active_checked, style='class:script-checkbox', jans_help=self.app.get_help_from_schema(schema, 'enabled')),
                    self.app.getTitledText(_("Nickname"), name='nickname', value='\n'.join(get_custom_attribute('nickname', multi=True)), style='class:script-titledtext', height=3, jans_help=self.app.get_help_from_schema(schema, 'nickname')),

                    VSplit([ 
                        Label(text=admin_ui_roles_label, style='class:script-label', width=len(admin_ui_roles_label)+1), 
                        self.admin_ui_roles_container,
                        Window(width=2),
                        HSplit([
                                Window(height=1),
                                Button(text=add_admin_ui_role_label, width=len(add_admin_ui_role_label)+4, handler=self.add_admin_ui_role),
                                ]),
                        ], height=4, width=D()),

                    VSplit([ 
                        Label(text=member_of_label, style='class:script-label', width=len(member_of_label)+1), 
                        self.member_of_container,
                        Window(width=2),
                        HSplit([
                                Window(height=1),
                                Button(text=add_group_label, width=len(add_group_label)+4, handler=self.add_group),
                                ]),
                        ], height=4, width=D()),

                            ]


        self.dialog = JansDialogWithNav(
            title=self.title,
            content= HSplit(
                self.edit_user_content,
                width=D(),
                height=D()
                ),
            button_functions=[(self.cancel, _("Cancel")), (self.save, _("Save"))],
            height=self.app.dialog_height,
            width=self.app.dialog_width,
            )


    def get_admin_ui_roles(self) -> None:
        async def coroutine():
            cli_args = {'operation_id': 'get-all-adminui-roles'}
            self.app.start_progressing()
            response = await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing()
            self.admin_ui_roles = response.json()
            self.add_admin_ui_role()
        asyncio.ensure_future(coroutine())

    def add_admin_ui_role(self) -> None:
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
                self.app.logger.debug('ADDING ROLE: ' + str(role_))
                self.admin_ui_roles_container.add_item([role_])

            self.app.logger.debug('NEW ROLES: ' + str(self.admin_ui_roles_container.data))

        body = HSplit([Label(_("Select Admin-UI role to be added to current user.")), admin_ui_roles_checkbox])
        buttons = [Button(_("Cancel")), Button(_("OK"), handler=add_role)]
        dialog = JansGDialog(self.app, title=_("Select Admin-UI"), body=body, buttons=buttons, width=self.app.dialog_width-20)
        self.app.show_jans_dialog(dialog)


    def delete_admin_ui_role(self, **kwargs: Any) -> None:
        self.admin_ui_roles_container.remove_item(kwargs['selected'])


    def delete_group(self, **kwargs: Any) -> None:
        pass

    def add_group(self):
        pass

    def __pt_container__(self)-> Dialog:
        return self.dialog

