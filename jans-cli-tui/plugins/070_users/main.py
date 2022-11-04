import os
import sys
import threading

from typing import Sequence

from prompt_toolkit.application import Application
from prompt_toolkit.layout.containers import HSplit, VSplit, Window, DynamicContainer
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.widgets import Button, Label, Frame
from prompt_toolkit.formatted_text import HTML
from wui_components.jans_drop_down import DropDownWidget
from wui_components.jans_vetrical_nav import JansVerticalNav

from utils.utils import DialogUtils
from utils.multi_lang import _


class Plugin(DialogUtils):
    """This is a general class for plugins 
    """
    def __init__(
        self, 
        app: Application
        ) -> None:
        """init for Plugin class "users"

        Args:
            app (_type_): _description_
        """
        self.app = app
        self.pid = 'users'
        self.name = '[U]SERS'
        self.users = {}
        self.widgets_ready = False



    def process(self) -> None:
        pass

    def set_center_frame(self) -> None:
        """center frame content
        """

        self.user_list_container = HSplit([],width=D())

        self.app.center_container = HSplit([
                    VSplit([
                        self.app.getButton(text=_("Get Users"), name='oauth:scopes:get', jans_help=_("Retreive first {} users").format(self.app.entries_per_page), handler=self.get_users),
                        self.app.getTitledText(_("Search: "), name='oauth:scopes:search', jans_help=_("Press enter to perform search"), accept_handler=self.search_user, style='class:outh_containers_scopes.text'),
                        self.app.getButton(text=_("Add Users"), name='oauth:scopes:add', jans_help=_("To add a new user press this button"), handler=self.edit_user_dialog),
                        ],
                        padding=3,
                        width=D(),
                    ),
                    DynamicContainer(lambda: self.user_list_container)
                    ],style='class:outh_containers_scopes')


    def get_users(self) -> None:
        """Function to get users.
        """
        t = threading.Thread(target=self.get_users_thread, daemon=True)
        self.app.start_progressing()
        t.start()

    def update_user_list_container(self) -> None:
        """User management list
        """

        data = []

        for user in self.users.get('entries', []):
            data.append((user['displayName'], user['userId'], user['mail']))

        users_list_box = JansVerticalNav(
                myparent=self.app,
                headers=['Name', 'User Name', 'Email'],
                preferred_size= [20, 30 ,30],
                data=data,
                on_enter=self.edit_user_dialog,
                on_display=self.app.data_display_dialog,
                on_delete=self.delete_user,
                #get_help=(self.get_help,'User'),
                selectes=0,
                headerColor='class:outh-verticalnav-headcolor',
                entriesColor='class:outh-verticalnav-entriescolor',
                all_data=self.users['entries']
            )

        self.user_list_container = users_list_box
        self.app.invalidate()

    def get_users_thread(self, start_index: int=0) -> None:
        """Gets Users from server.
        """
        endpoint_args ='limit:{},startIndex:{}'.format(self.app.entries_per_page, start_index)
        try :
            rsponse = self.app.cli_object.process_command_by_id(
                        operation_id='get-user',
                        url_suffix='',
                        endpoint_args='',
                        data_fn=None,
                        data={}
                        )
        except Exception as e:
            self.app.stop_progressing()
            self.app.show_message(_("Error getting Users"), str(e))
            return
        self.app.stop_progressing()

        try:
            self.users = rsponse.json()
        except Exception:
            self.app.show_message(_("Error getting Users"), str(rsponse.text))
            return

        self.app.logger.debug("Users: {}".format(self.users))

        if not self.widgets_ready:
            self.update_user_list_container()


    def edit_user_dialog(self):
        pass

    def delete_user(self):
        pass

    def search_user(self):
        pass

    def edit_user_dialog(self):
        pass
