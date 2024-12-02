import json
import asyncio
from functools import partial
from types import SimpleNamespace
from typing import Any, Optional

from prompt_toolkit import HTML
from prompt_toolkit.buffer import Buffer
from prompt_toolkit.application import Application
from prompt_toolkit.layout.containers import HSplit, VSplit, DynamicContainer, HorizontalAlign
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.widgets import Button, Dialog
from prompt_toolkit.eventloop import get_event_loop

from wui_components.jans_vetrical_nav import JansVerticalNav
from edit_user_dialog import EditUserDialog
from fido_entries import FidoEntries
from utils.utils import DialogUtils, get_help_with
from utils.static import DialogResult
from utils.multi_lang import _
from wui_components.jans_cli_dialog import JansGDialog
from utils.static import DialogResult, cli_style, common_strings
from utils.background_tasks import get_admin_ui_roles

class Plugin(DialogUtils):
    """This is a general class for plugins 
    """
    def __init__(
        self, 
        app: Application
        ) -> None:
        """init for Plugin class "users"

        Args:
            app (Generic): The main Application class
        """
        self.app = app
        self.pid = 'user-management'
        self.server_side_plugin = True
        self.name = '[U]sers'
        self.users = {}
        self.widgets_ready = False
        self.jans_help = get_help_with(f'<p>              {_("Change user password")}\n<f>              {_("User FIDO Devices")}\n')

    def process(self) -> None:
        pass

    def init_plugin(self) -> None:
        """The initialization for this plugin
        """
        if self.app.plugin_enabled('admin'):
            self.app.create_background_task(get_admin_ui_roles())


    def set_center_frame(self) -> None:
        """center frame content
        """

        self.user_list_container = HSplit([],width=D())
        self.nav_buttons = VSplit([],width=D())
        self.app.center_container = HSplit([
                    VSplit([
                        self.app.getTitledText(_("Search"), name='oauth:scopes:search', jans_help=_("Press enter to perform search"), accept_handler=self.search_user, style='class:outh_containers_scopes.text'),
                        self.app.getButton(text=_("Add Users"), name='oauth:scopes:add', jans_help=_("To add a new user press this button"), handler=self.edit_user_dialog),
                        ],
                        padding=3,
                        width=D(),
                    ),
                    DynamicContainer(lambda: self.user_list_container),
                    DynamicContainer(lambda: self.nav_buttons),
                    ],style='class:outh_containers_scopes')

    def update_user_list_container(self, pattern: Optional[str]='') -> None:
        """User management list
        """

        data = []

        for user in self.users.get('entries', []):
            data.append((user.get('displayName', ''), user.get('userId',''), user.get('mail', '')))

        self.users_list_box = JansVerticalNav(
                myparent=self.app,
                headers=['Name', 'User Name', 'Email'],
                preferred_size= [20, 30 ,30],
                data=data,
                on_enter=self.edit_user_dialog,
                on_display=self.app.data_display_dialog,
                on_delete=self.delete_user,
                #get_help=(self.get_help,'User'),
                change_password=self.change_password,
                selectes=0,
                headerColor=cli_style.navbar_headcolor,
                entriesColor=cli_style.navbar_entriescolor,
                all_data=self.users['entries'],
                jans_help = "Press <b>p</b> to change password, <b>f</b> to view fido devices",
                custom_key_bindings = [('f', self.display_fido_devices)]
            )

        self.user_list_container = self.users_list_box

        buttons = []
        if self.users['start'] > 1:
            handler_partial = partial(self.get_users, self.users['start']-self.app.entries_per_page+1, pattern)
            prev_button = Button(_("Prev"), handler=handler_partial)
            prev_button.window.jans_help = _("Retreives previous %d entries") % self.app.entries_per_page
            buttons.append(prev_button)
        if self.users['totalEntriesCount'] > self.users['start'] + self.users['entriesCount']:
            handler_partial = partial(self.get_users, self.users['start']+self.app.entries_per_page+1, pattern)
            next_button = Button(_("Next"), handler=handler_partial)
            next_button.window.jans_help = _("Retreives previous %d entries") % self.app.entries_per_page
            buttons.append(next_button)

        self.nav_buttons = VSplit(buttons, padding=5, align=HorizontalAlign.CENTER)

        self.app.invalidate()

    def get_users(self, start_index: int=0, pattern: Optional[str]='') -> None:
        """Gets Users from server.
        """

        endpoint_args ='limit:{},startIndex:{}'.format(self.app.entries_per_page, start_index)
        if pattern:
            endpoint_args += ',pattern:'+pattern
        cli_args = {'operation_id': 'get-user', 'endpoint_args': endpoint_args}

        async def coroutine():
            self.app.start_progressing(_("Retreiving users from server..."))
            response = await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing()
            self.users = response.json()
            self.app.logger.debug("Users: {}".format(self.users))

            if not self.users.get('entries'):
                self.app.show_message(_("Not found"), _("No user found for this search."), tobefocused=self.app.center_container)
                return

            if not self.widgets_ready:
                self.update_user_list_container(pattern)
            self.app.layout.focus(self.user_list_container)

        asyncio.ensure_future(coroutine())

    def edit_user_dialog(self, **kwargs: Any) -> None:
        """Method to display the edit user dialog
        """
        if kwargs:
            data = kwargs.get('data', {})
        else:
            data = {}

        title = _("Edit User") if data else _("Add User")

        edit_user_dialog = EditUserDialog(parent=self, title=title, data=data)
        self.app.show_jans_dialog(edit_user_dialog)


    def change_password(self, **kwargs: Any) -> None:
        """Method to display the edit user dialog
        """
        if kwargs:
            data = kwargs.get('data', {})
        else:
            data = {}

        def save(dialog) -> None:
            async def coroutine():
                cli_args = {'operation_id': 'patch-user-by-inum', 'endpoint_args': '',
                'url_suffix':'inum:{}'.format(data['inum']),
                    'data':{"jsonPatchString": "",
                        "customAttributes": [
                                {"name": "userPassword",
                                "multiValued": False,
                                "value": "{}".format(self.new_password.me.text)}]}
                    }
                self.app.start_progressing(_("Changing Password ..."))
                response = await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
                self.app.stop_progressing()

            asyncio.ensure_future(coroutine())

        self.new_password = self.app.getTitledText(
                                        _('New Password'), 
                                        name='passwd', 
                                        value='', 
                                        style='class:outh-scope-text',
                                        )
        body = HSplit([
            self.new_password
            ],style='class:jans-main-datadisplay')
        buttons=[
                Button(
                    text=_("Cancel"),
                ) ,
                Button(
                    text=_("Save"),
                    handler=save,
                ) ,            ]

        dialog = JansGDialog(self.app, title="Change Password for {}".format(data['userId']), body=body, buttons=buttons)

        self.app.show_jans_dialog(dialog)

    def delete_user(self, **kwargs: Any) -> None:
        """This method for the deletion of the User 
        """

        def do_delete_user():
            for user in self.users['entries']:
                if user.get('userId') == kwargs['selected'][1]:
                    async def coroutine():
                        cli_args = {'operation_id': 'delete-user', 'url_suffix':'inum:{}'.format(user['inum'])}
                        self.app.start_progressing(_("Deleting user {}").format(user['userId']))
                        response = await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
                        self.app.stop_progressing()
                        if response:
                            self.app.show_message(_("Error"), _("Deletion was not completed {}".format(response)))
                        else:
                            self.users_list_box.remove_item(kwargs['selected'])
                        self.get_users()
                    asyncio.ensure_future(coroutine())
                    break

        buttons = [Button(_("No")), Button(_("Yes"), handler=do_delete_user)]

        self.app.show_message(
                title=_("Confirm"),
                message=_("Are you sure you want to delete user {}?").format(kwargs['selected'][1]),
                buttons=buttons,
                )
 

    def search_user(self, tbuffer:Buffer) -> None:
        """This method handel the search for Users

        Args:
            tbuffer (Buffer): Buffer returned from the TextArea widget > GetTitleText
        """
        self.get_users(pattern=tbuffer.text)

    def display_fido_devices(self, event) -> None:
        selected_user = self.users_list_box.get_selection()
        fido_entries = FidoEntries(selected_user)
        fido_entries.get_user_fido_entries()
