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
from utils.utils import DialogUtils, common_data
from utils.static import DialogResult
from utils.multi_lang import _
from wui_components.jans_cli_dialog import JansGDialog
from utils.static import DialogResult, cli_style, common_strings

common_data.users = SimpleNamespace()

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

    def process(self) -> None:
        pass

    def on_page_enter(self) -> None:
        """Function to perform preliminary tasks before this page entered.
        """
        # we need claims everywhere
        self.get_claims()

    def set_center_frame(self) -> None:
        """center frame content
        """

        self.user_list_container = HSplit([],width=D())
        self.nav_buttons = VSplit([],width=D())
        self.app.center_container = HSplit([
                    VSplit([
                        self.app.getButton(text=_("Get Users"), name='oauth:scopes:get', jans_help=_("Retreive first {} users").format(self.app.entries_per_page), handler=self.get_users),
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
                jans_help = "Press p to change password"
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

        edit_user_dialog = EditUserDialog(self.app, title=title, data=data, save_handler=self.save_user)
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
                await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
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
 
    def save_user(self, dialog: Dialog) -> None:
        """This method to save user data to server

        Args:
            dialog (_type_): the main dialog to save data in

        Returns:
            _type_: bool value to check the status code response
        """

        fix_title = _("Please fix!")
        raw_data = self.make_data_from_dialog(tabs={'user': dialog.edit_user_container.content})

        if not (raw_data['userId'].strip() and raw_data['mail'].strip()):
            self.app.show_message(fix_title, _("Username and/or Email is empty"))
            return

        if 'baseDn' not in dialog.data and not raw_data['userPassword'].strip():
            self.app.show_message(fix_title, _("Please enter Password"))
            return

        user_info = {'customObjectClasses':['top', 'jansCustomPerson'], 'customAttributes':[]}
        for key_ in ('mail', 'userId', 'displayName', 'givenName'):
            user_info[key_] = raw_data.pop(key_)

        if 'baseDn' not in dialog.data:
            user_info['userPassword'] = raw_data['userPassword']

        for key_ in ('inum', 'baseDn', 'dn'):
            if key_ in raw_data:
                del raw_data[key_]
            if key_ in dialog.data:
                user_info[key_] = dialog.data[key_]

        status = raw_data.pop('active')
        user_info['jansStatus'] = 'active' if status else 'inactive'

        for key_ in raw_data:
            multi_valued = False
            key_prop = dialog.get_claim_properties(key_)

            if key_prop.get('dataType') == 'json':
                try:
                    json.loads(raw_data[key_])
                except Exception as e:
                    display_name = key_prop.get('displayName') or key_
                    self.app.show_message(
                                fix_title,
                                _(HTML("Can't convert <b>{}</b> to json. Conversion error: <i>{}</i>").format(display_name, e))
                            )
                    return

            user_info['customAttributes'].append({
                    'name': key_, 
                    'multiValued': multi_valued, 
                    'values': [raw_data[key_]],
                    })

        for ca in dialog.data.get('customAttributes', []):
            if ca['name'] == 'memberOf':
                user_info['customAttributes'].append(ca)
                break

        if hasattr(dialog, 'admin_ui_roles_container'):
            admin_ui_roles = [item[0] for item in dialog.admin_ui_roles_container.data]
            if admin_ui_roles:
               user_info['customAttributes'].append({
                        'name': 'jansAdminUIRole', 
                        'multiValued': len(admin_ui_roles) > 1, 
                        'values': admin_ui_roles,
                        })

        async def coroutine():
            operation_id = 'put-user' if dialog.data.get('baseDn') else 'post-user'
            cli_args = {'operation_id': operation_id, 'data': user_info}
            self.app.start_progressing(_("Saving user ..."))
            response = await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing()
            if response.status_code == 500:
                self.app.show_message(_('Error'), response.text + '\n' + response.reason)
            else:
                dialog.future.set_result(DialogResult.OK)
                self.get_users()

        asyncio.ensure_future(coroutine())

    def get_claims(self) -> None:
        """This method for getting claims
        """
        if hasattr(common_data.users, 'claims'):
            return
        async def coroutine():
            cli_args = {'operation_id': 'get-attributes', 'endpoint_args':'limit:200,status:active'}
            self.app.start_progressing(_("Retreiving claims"))
            response = await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing()
            result = response.json()
            common_data.users.claims = result['entries']

        asyncio.ensure_future(coroutine())

    def search_user(self, tbuffer:Buffer) -> None:
        """This method handel the search for Users

        Args:
            tbuffer (Buffer): Buffer returned from the TextArea widget > GetTitleText
        """
        self.get_users(pattern=tbuffer.text)

