import os
import sys
from prompt_toolkit.application import Application
import threading

from prompt_toolkit.layout.containers import (
    HSplit,
    VSplit,
    HorizontalAlign,
    DynamicContainer,
    Window,
)
from prompt_toolkit.application.current import get_app
from prompt_toolkit.buffer import Buffer

from prompt_toolkit.layout.dimension import D
from prompt_toolkit.widgets import Button, Label, Frame
from wui_components.jans_nav_bar import JansNavBar
from prompt_toolkit.layout.containers import HSplit, DynamicContainer, VSplit, Window
from prompt_toolkit.widgets import Button, Label, Frame, Box, Dialog
from wui_components.jans_cli_dialog import JansGDialog
from collections import OrderedDict
from functools import partial
from typing import Any
from wui_components.jans_vetrical_nav import JansVerticalNav
from utils.multi_lang import _
from typing import Any, Optional

class Plugin():
    """This is a general class for plugins 
    """
    def __init__(
        self, 
        app: Application
        ) -> None:
        """init for Plugin class "config_api"

        Args:
            app (_type_): _description_
        """
        self.app = app
        self.pid = 'config_api'
        self.name = '[C]onfig-API'
        self.page_entered = False
        self.data = {}
        
        self.prepare_navbar()
        self.prepare_containers()

    def process(self) -> None:
        pass

    def prepare_navbar(self) -> None:
        """prepare the navbar for the current Plugin 
        """
        self.nav_bar = JansNavBar(
                    self.app,
                    entries=[('accessroles', 'Access r[o]les'), ('permissions', '[P]ermissions'),  ('mapping', '[M]apping')],
                    selection_changed=self.nav_selection_changed,
                    select=0,
                    jans_name='fido:nav_bar'
                    )

    def prepare_containers(self) -> None:
        """prepare the main container (tabs) for the current Plugin 
        """

        self.containers = OrderedDict()
        self.main_area = HSplit([Label("configuration")],width=D())

        self.main_container = HSplit([
                                        Box(self.nav_bar.nav_window, style='class:sub-navbar', height=1),
                                        DynamicContainer(lambda: self.main_area),
                                        ],
                                    height=D(),
                                    style='class:outh_maincontainer'
                                    )
        self.create_widgets()

    def create_widgets(self):

        self.config_data_container = {
            'accessroles': HSplit([],width=D()),
            'permissions': HSplit([],width=D()),
        }

        self.containers['accessroles'] = HSplit([
                    VSplit([
                        self.app.getButton(
                            text=_("Get adminui roles"), 
                            name='oauth:clients:get', 
                            jans_help=_("Get all admin ui roles"), 
                            handler=self.get_adminui_roles),
                        
                        self.app.getTitledText(
                            _("Search: "), 
                            name='oauth:scopes:search', 
                            jans_help=_("Press enter to perform search"), 
                            accept_handler=self.search_adminui_roles,
                            style='class:outh_containers_scopes.text'),

                        self.app.getButton(
                            text=_("Add Scope"), 
                            name='oauth:scopes:add', 
                            jans_help=_("Add admin ui role"), 
                            handler=self.add_adminui_roles),
                        ],
                        padding=3,
                        width=D(),
                        ),
                        DynamicContainer(lambda: self.config_data_container['accessroles'])
                     ],style='class:outh_containers_clients')


        self.containers['permissions'] = HSplit([
                    VSplit([
                        self.app.getButton(
                            text=_("Get adminui permissions"), 
                            name='oauth:clients:get', 
                            jans_help=_("Get all admin ui permissions"), 
                            handler=self.get_adminui_permissions),
                        
                        self.app.getTitledText(
                            _("Search: "), 
                            name='oauth:scopes:search', 
                            jans_help=_("Press enter to perform search"), 
                            accept_handler=self.search_adminui_permissions,
                            style='class:outh_containers_scopes.text'),

                        self.app.getButton(
                            text=_("Add Scope"), 
                            name='oauth:scopes:add', 
                            jans_help=_("Add admin ui role"), 
                            handler=self.add_adminui_permissions),
                        ],
                        padding=3,
                        width=D(),
                        ),
                        DynamicContainer(lambda: self.config_data_container['permissions'])
                     ],style='class:outh_containers_clients')

        self.containers['mapping'] = HSplit([
                                Label(text=_("mapping")),
                                ],
                                width=D()
                                )
                                
        self.nav_selection_changed(list(self.containers)[0])

    def get_adminui_roles(self) -> None:
        """Method to get the clients data from server
        """ 
        self.config_data_container['accessroles'] = HSplit([Label(_("Please wait while getting clients"),style='class:outh-waitclientdata.label')], width=D(),style='class:outh-waitclientdata')
        t = threading.Thread(target=self.adminui_update_roles, daemon=True)
        self.app.start_progressing()
        t.start()

    def adminui_update_roles(
        self,
        start_index: Optional[int]= 0, 
        pattern: Optional[str]= '',
        ) -> None:
        """update the current clients data to server

        Args:
            pattern (str, optional): endpoint arguments for the client data. Defaults to ''.
        """

        def get_next(
            start_index: int,  
            pattern: Optional[str]= '', 
            ) -> None:
            self.adminui_update_roles(start_index, pattern='')

        endpoint_args ='limit:{},startIndex:{}'.format(self.app.entries_per_page, start_index)
        if pattern:
            endpoint_args +=',pattern:'+pattern
        try :
            rsponse = self.app.cli_object.process_command_by_id(
                        operation_id='get-all-adminui-roles',
                        url_suffix='',
                        endpoint_args='',
                        data_fn=None,
                        data={}
                        )

        except Exception as e:
            self.app.stop_progressing()
            self.app.show_message(_("Error getting clients"), str(e))
            return

        self.app.stop_progressing()

        if rsponse.status_code not in (200, 201):
            self.app.show_message(_("Error getting clients"), str(rsponse.text))
            return

        try:
            result = rsponse.json()
        except Exception:
            self.app.show_message(_("Error getting clients"), str(rsponse.text))
            return

        data =[]

        for d in result:
            data.append(
                [
                d.get('role'),
                d.get('description'),
                ]
            )

        # if data:
        #     clients = JansVerticalNav(
        #         myparent=self.app,
        #         headers=['Role', 'Description',],
        #         preferred_size= [0,0],
        #         data=data,
        #         on_enter=self.edit_adminui_roles,
        #         on_display=self.app.data_display_dialog,
        #         # get_help=(self.get_help,'AdminRole'),
        #         selectes=0,
        #         headerColor='class:outh-verticalnav-headcolor',
        #         entriesColor='class:outh-verticalnav-entriescolor',
        #         all_data=result
        #     )

        #     self.app.layout.focus(clients)  
        #     self.config_data_container['accessroles'] = HSplit([
        #             clients,
                
        #     ])

        #     get_app().invalidate()
        #     self.app.layout.focus(clients)  

        # else:
        #     self.app.show_message(_("Oops"), _("No matching result"),tobefocused = self.containers['accessroles'])

        # ------------------------------------------------------------------------------- #
        # --------------------------------- View Data ----------------------------------- #
        # ------------------------------------------------------------------------------- #               

        if data:
            buttons = []
            if int(len(data)/ 20) >=1  :

                if start_index< int(len(data)/ 20) :
                    handler_partial = partial(get_next, start_index+1, pattern)
                    next_button = Button(_("Next"), handler=handler_partial)
                    next_button.window.jans_help = _("Retreives next %d entries") % self.app.entries_per_page
                    buttons.append(next_button)

                if start_index!=0:
                    handler_partial = partial(get_next, start_index-1, pattern)
                    prev_button = Button(_("Prev"), handler=handler_partial)
                    prev_button.window.jans_help = _("Retreives previous %d entries") % self.app.entries_per_page
                    buttons.append(prev_button)
            data_now = data[start_index*20:start_index*20+20]
            
            clients = JansVerticalNav(
                myparent=self.app,
                headers=['Role', 'Description',],
                preferred_size= [0,0],
                data=data_now,
                on_enter=self.edit_adminui_roles,
                on_display=self.app.data_display_dialog,
                # get_help=(self.get_help,'AdminRole'),
                selectes=0,
                headerColor='class:outh-verticalnav-headcolor',
                entriesColor='class:outh-verticalnav-entriescolor',
                all_data=result
            )
            self.app.layout.focus(clients)   # clients.focuse..!? TODO >> DONE
            self.config_data_container['accessroles'] = HSplit([
                clients,
                VSplit(buttons, padding=5, align=HorizontalAlign.CENTER)
            ])
            get_app().invalidate()
            self.app.layout.focus(clients)  ### it fix focuse on the last item deletion >> try on UMA-res >> edit_client_dialog >> oauth_update_uma_resources
        
        else:
            self.app.show_message(_("Oops"), _("No matching result"),tobefocused = self.config_data_container['accessroles'])



    def add_adminui_roles(self) -> None:
        """Method to display the dialog of clients
        """
        pass
        # dialog = EditScopeDialog(self.app, title=_("Add New Scope"), data={}, save_handler=self.save_scope)
        # result = self.app.show_jans_dialog(dialog)

    def search_adminui_roles(self, tbuffer:Buffer,) -> None:
        if not len(tbuffer.text) > 2:
            self.app.show_message(_("Error!"), _("Search string should be at least three characters"),tobefocused=self.containers['accessroles'])
            return

        t = threading.Thread(target=self.adminui_update_roles, args=(0,tbuffer.text), daemon=True)
        self.app.start_progressing()
        t.start()

    #--------------------------------------------------------------------------------#
    #--------------------------------------------------------------------------------#
    #--------------------------------------------------------------------------------#

    def get_adminui_permissions(self) -> None:
        """Method to get the adminui_permissions data from server
        """ 
        self.config_data_container['permissions'] = HSplit([Label(_("Please wait while getting adminui_permissions"),style='class:outh-waitclientdata.label')], width=D(),style='class:outh-waitclientdata')
        t = threading.Thread(target=self.adminui_update_permissions, daemon=True)
        self.app.start_progressing()
        t.start()

    def adminui_update_permissions(
        self,
        start_index: Optional[int]= 0, 
        pattern: Optional[str]= '',
        ) -> None:
        """update the current adminui_permissions data to server

        Args:
            pattern (str, optional): endpoint arguments for the client data. Defaults to ''.
        """

        def get_next(
            start_index: int,  
            pattern: Optional[str]= '', 
            ) -> None:
            self.adminui_update_permissions(start_index, pattern='')

        endpoint_args ='limit:{},startIndex:{}'.format(self.app.entries_per_page, start_index)
        if pattern:
            endpoint_args +=',pattern:'+pattern
        try :
            rsponse = self.app.cli_object.process_command_by_id(
                        operation_id='get-all-adminui-permissions',
                        url_suffix='',
                        endpoint_args='',
                        data_fn=None,
                        data={}
                        )

        except Exception as e:
            self.app.stop_progressing()
            self.app.show_message(_("Error getting adminui_permissions"), str(e))
            return

        self.app.stop_progressing()

        if rsponse.status_code not in (200, 201):
            self.app.show_message(_("Error getting adminui_permissions"), str(rsponse.text))
            return

        try:
            result = rsponse.json()
        except Exception:
            self.app.show_message(_("Error getting adminui_permissions"), str(rsponse.text))
            return

        data =[]

        for d in result:
            data.append(
                [
                d.get('permission'),
                d.get('defaultPermissionInToken'),
                ]
            )

        # if data:
        #     adminui_permissions = JansVerticalNav(
        #         myparent=self.app,
        #         headers=['permission', 'defaultPermissionInToken',],
        #         preferred_size= [0,0],
        #         data=data,
        #         on_enter=self.edit_adminui_roles,
        #         on_display=self.app.data_display_dialog,
        #         # get_help=(self.get_help,'AdminRole'),
        #         selectes=0,
        #         headerColor='class:outh-verticalnav-headcolor',
        #         entriesColor='class:outh-verticalnav-entriescolor',
        #         all_data=result
        #     )

        #     self.app.layout.focus(adminui_permissions)  
        #     self.config_data_container['permissions'] = HSplit([
        #             adminui_permissions,
                
        #     ])

        #     get_app().invalidate()
        #     self.app.layout.focus(adminui_permissions)  

        # else:
        #     self.app.show_message(_("Oops"), _("No matching result"),tobefocused = self.containers['permissions'])

        if data:
            buttons = []
            if int(len(data)/ 20) >=1  :

                if start_index< int(len(data)/ 20) :
                    handler_partial = partial(get_next, start_index+1, pattern)
                    next_button = Button(_("Next"), handler=handler_partial)
                    next_button.window.jans_help = _("Retreives next %d entries") % self.app.entries_per_page
                    buttons.append(next_button)

                if start_index!=0:
                    handler_partial = partial(get_next, start_index-1, pattern)
                    prev_button = Button(_("Prev"), handler=handler_partial)
                    prev_button.window.jans_help = _("Retreives previous %d entries") % self.app.entries_per_page
                    buttons.append(prev_button)
            data_now = data[start_index*20:start_index*20+20]
            
            adminui_permissions = JansVerticalNav(
                myparent=self.app,
                headers=['permission', 'defaultPermissionInToken',],
                preferred_size= [0,0],
                data=data_now,
                on_enter=self.edit_adminui_roles,
                on_display=self.app.data_display_dialog,
                # get_help=(self.get_help,'AdminRole'),
                selectes=0,
                headerColor='class:outh-verticalnav-headcolor',
                entriesColor='class:outh-verticalnav-entriescolor',
                all_data=result
            )
            self.app.layout.focus(adminui_permissions)   # clients.focuse..!? TODO >> DONE
            self.config_data_container['permissions'] = HSplit([
                adminui_permissions,
                VSplit(buttons, padding=5, align=HorizontalAlign.CENTER)
            ])
            get_app().invalidate()
            self.app.layout.focus(adminui_permissions)  ### it fix focuse on the last item deletion >> try on UMA-res >> edit_client_dialog >> oauth_update_uma_resources
        
        else:
            self.app.show_message(_("Oops"), _("No matching result"),tobefocused = self.config_data_container['permissions'])

    def add_adminui_permissions(self) -> None:
        """Method to display the dialog of adminui_permissions
        """
        pass
        # dialog = EditScopeDialog(self.app, title=_("Add New Scope"), data={}, save_handler=self.save_scope)
        # result = self.app.show_jans_dialog(dialog)

    def search_adminui_permissions(self, tbuffer:Buffer,) -> None:
        if not len(tbuffer.text) > 2:
            self.app.show_message(_("Error!"), _("Search string should be at least three characters"),tobefocused=self.containers['permissions'])
            return

        t = threading.Thread(target=self.adminui_update_permissions, args=(0,tbuffer.text), daemon=True)
        self.app.start_progressing()
        t.start()

    #--------------------------------------------------------------------------------#
    #--------------------------------------------------------------------------------#
    #--------------------------------------------------------------------------------#

    def edit_adminui_roles(self, **params: Any) -> None:
        pass

    def nav_selection_changed(
                self,
                selection: str
            ) -> None:

        """This method for the selection change

        Args:
            selection (str): the current selected tab
        """

        if selection in self.containers:
            self.main_area = self.containers[selection]
        else:
            self.main_area = self.app.not_implemented

    def set_center_frame(self) -> None:
        """center frame content
        """
        self.app.center_container = self.main_container

