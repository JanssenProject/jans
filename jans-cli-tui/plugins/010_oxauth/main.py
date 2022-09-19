import os
import sys

import threading
from asyncio import ensure_future

import prompt_toolkit
from prompt_toolkit.application.current import get_app
from prompt_toolkit.key_binding import KeyBindings
from prompt_toolkit.key_binding.bindings.focus import focus_next, focus_previous
from prompt_toolkit.layout.containers import (
    HSplit,
    VSplit,
    HorizontalAlign,
    DynamicContainer,
)
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.widgets import (
    Box,
    Button,
    Label,
    Frame
)

from cli import config_cli
from wui_components.jans_nav_bar import JansNavBar
from wui_components.jans_side_nav_bar import JansSideNavBar
from wui_components.jans_vetrical_nav import JansVerticalNav
from wui_components.jans_dialog import JansDialog
from wui_components.jans_dialog_with_nav import JansDialogWithNav
from wui_components.jans_drop_down import DropDownWidget
from wui_components.jans_data_picker import DateSelectWidget

from edit_client_dialog import EditClientDialog
from edit_scope_dialog import EditScopeDialog

from multi_lang import _

class Plugin():
    """This is a general class for plugins 
    """
    def __init__(self, app):
        """init for Plugin class "oxauth"

        Args:
            app (_type_): _description_
        """
        self.app = app
        self.pid = 'oxauth'
        self.name = 'Auth Server'

        self.oauth_containers = {}
        self.oauth_prepare_navbar()
        self.oauth_prepare_containers()
        self.oauth_nav_selection_changed(self.oauth_navbar.navbar_entries[0][0])

    def process(self):
        pass

    def set_center_frame(self):
        """center frame content
        """
        self.app.center_container = self.oauth_main_container

    def oauth_prepare_containers(self):
        """prepare the main container (tabs) for the current Plugin 
        """

        self.oauth_data_container = {
            'clients' :HSplit([],width=D()),
            'scopes' :HSplit([],width=D()),

        } 
        self.oauth_main_area = HSplit([],width=D())

        self.oauth_containers['scopes'] = HSplit([
                    VSplit([
                        self.app.getButton(text=_("Get Scopes"), name='oauth:scopes:get', jans_help=_("Retreive first 10 Scopes"), handler=self.oauth_get_scopes),
                        self.app.getTitledText(_("Search: "), name='oauth:scopes:search', jans_help=_("Press enter to perform search")),
                        self.app.getButton(text=_("Add Scope"), name='oauth:scopes:add', jans_help=_("To add a new scope press this button")),
                        ],
                        padding=3,
                        width=D(),
                    ),
                    DynamicContainer(lambda: self.oauth_data_container['scopes'])
                    ])

        self.oauth_containers['clients'] = HSplit([
                    VSplit([
                        self.app.getButton(text=_("Get Clients"), name='oauth:clients:get', jans_help=_("Retreive first 10 OpenID Connect clients"), handler=self.oauth_get_clients),
                        self.app.getTitledText(_("Search"), name='oauth:clients:search', jans_help=_("Press enter to perform search"), accept_handler=self.search_clients),
                        self.app.getButton(text=_("Add Client"), name='oauth:clients:add', jans_help=_("To add a new client press this button"), handler=self.add_client),
                        
                        ],
                        padding=3,
                        width=D(),
                        ),
                        DynamicContainer(lambda: self.oauth_data_container['clients'])
                    ]
                    )

        self.oauth_main_container = HSplit([
                                        Box(self.oauth_navbar.nav_window, style='fg:#f92672 bg:#4D4D4D', height=1),
                                        DynamicContainer(lambda: self.oauth_main_area),
                                        ],
                                    height=D(),
                                    )

    def oauth_prepare_navbar(self):
        """prepare the navbar for the current Plugin 
        """
        self.oauth_navbar = JansNavBar(
                    self,
                    entries=[('clients', 'Clients'), ('scopes', 'Scopes'), ('keys', 'Keys'), ('defaults', 'Defaults'), ('properties', 'Properties'), ('logging', 'Logging')],
                    selection_changed=self.oauth_nav_selection_changed,
                    select=0,
                    bgcolor='#66d9ef'
                    )

    def oauth_nav_selection_changed(self, selection):
        """This method for the selection change

        Args:
            selection (str): the current selected tab
        """
        self.app.logger.debug('OXUATH NAV: %s', selection)
        if selection in self.oauth_containers:
            self.oauth_main_area = self.oauth_containers[selection]
        else:
            self.oauth_main_area = self.app.not_implemented

    def oauth_update_clients(self, pattern=''):
        """update the current clients data to server

        Args:
            pattern (str, optional): endpoint arguments for the client data. Defaults to ''.
        """
        endpoint_args ='limit:10'
        if pattern:
            endpoint_args +=',pattern:'+pattern

        try :
            rsponse = self.app.cli_object.process_command_by_id(
                        operation_id='get-oauth-openid-clients',
                        url_suffix='',
                        endpoint_args=endpoint_args,
                        data_fn=None,
                        data={}
                        )

        except Exception as e:
            self.app.show_message(_("Error getting clients"), str(e))
            return

        if rsponse.status_code not in (200, 201):
            self.app.show_message(_("Error getting clients"), str(rsponse.text))
            return

        try:
            result = rsponse.json()
        except Exception:
            self.app.show_message(_("Error getting clients"), str(rsponse.text))
            #press_tab
            return


        data =[]

        for d in result:
            data.append(
                [
                d['inum'],
                d.get('clientName', {}).get('values', {}).get('', ''),
                ','.join(d.get('grantTypes', [])),
                d.get('subjectType', '') 
                ]
            )

        if data:
            clients = JansVerticalNav(
                myparent=self.app,
                headers=['Client ID', 'Client Name', 'Grant Types', 'Subject Type'],
                preferred_size= [0,0,30,0],
                data=data,
                on_enter=self.edit_client_dialog,
                on_display=self.app.data_display_dialog,
                on_delete=self.delete_client,
                # selection_changed=self.data_selection_changed,
                selectes=0,
                headerColor='green',
                entriesColor='white',
                all_data=result
            )

            self.app.layout.focus(clients)
            self.oauth_data_container['clients'] = HSplit([
                clients
            ])
            get_app().invalidate()

        else:
            self.app.show_message(_("Oops"), _("No matching result"),tobefocused = self.oauth_containers['clients'])
            # self.app.layout.focus(self.oauth_containers['clients'])

    def oauth_get_clients(self):
        """Method to get the clients data from server
        """
        self.oauth_data_container['clients'] = HSplit([Label(_("Please wait while getting clients"))], width=D())
        t = threading.Thread(target=self.oauth_update_clients, daemon=True)
        t.start()

    def oauth_update_scopes(self, start_index=0):
        """update the current Scopes data to server

        Args:
            start_index (int, optional): add Button("Prev") to the layout. Defaults to 0.
        """
        try :
            rsponse = self.app.cli_object.process_command_by_id('get-oauth-scopes', '', 'limit:10', {})
        except Exception as e:
            self.app.show_message(_("Error getting scopes"), str(e))
            return

        if rsponse.status_code not in (200, 201):
            self.app.show_message(_("Error getting scopes"), str(rsponse.text))
            return

        try:
            result = rsponse.json()
        except Exception:
            self.app.show_message(_("Error getting scopes"), str(rsponse.text))
            return


        data =[]
        for d in result: 
            data.append(
                [
                d['id'],
                d['description'],
                d['scopeType']
                ]
            )

        scopes = JansVerticalNav(
                myparent=self.app,
                headers=['id', 'Description', 'Type'],
                preferred_size= [0,0,30,0],
                data=data,
                on_enter=self.edit_scope_dialog,
                on_display=self.app.data_display_dialog,
                # selection_changed=self.data_selection_changed,
                selectes=0,
                headerColor='green',
                entriesColor='white',
                all_data=result
            )

        buttons = []
        if start_index > 0:
            buttons.append(Button(_("Prev")))
        if len(result) >= 10:
            buttons.append(Button(_("Next")))

        self.app.layout.focus(scopes)   # clients.focuse..!? TODO >> DONE
        self.oauth_data_container['scopes'] = HSplit([
            scopes,
            VSplit(buttons, padding=5, align=HorizontalAlign.CENTER)
        ])

        get_app().invalidate()

    def oauth_get_scopes(self):
        """Method to get the Scopes data from server
        """
        self.oauth_data_container['scopes'] = HSplit([Label(_("Please wait while getting Scopes"))], width=D())
        t = threading.Thread(target=self.oauth_update_scopes, daemon=True)
        t.start()

    def display_scope(self):
        pass

    def edit_client_dialog(self, **params):
        
        selected_line_data = params['data']  
        title = _("Edit user Data (Clients)")

        dialog = EditClientDialog(self.app, title=title, data=selected_line_data,save_handler=self.save_client)
        self.app.show_jans_dialog(dialog)

    def save_client(self, dialog):
        """This method to save the client data to server

        Args:
            dialog (_type_): the main dialog to save data in

        Returns:
            _type_: bool value to check the status code response
        """

        self.app.logger.debug(dialog.data)

        response = self.app.cli_object.process_command_by_id(
            operation_id='put-oauth-openid-clients' if dialog.data.get('inum') else 'post-oauth-openid-clients',
            url_suffix='',
            endpoint_args='',
            data_fn='',
            data=dialog.data
        )

        self.app.logger.debug(response.text)

        if response.status_code in (200, 201):
            self.oauth_get_clients()
            return True

        self.app.show_message(_("Error!"), _("An error ocurred while saving client:\n") + str(response.text))

    def search_clients(self, tbuffer):
        if not len(tbuffer.text) > 2:
            self.app.show_message(_("Error!"), _("Search string should be at least three characters"),tobefocused=self.oauth_containers['clients'])
            return

        t = threading.Thread(target=self.oauth_update_clients, args=(tbuffer.text,), daemon=True)
        t.start()

    def add_client(self):
        """Method to display the dialog of clients
        """
        dialog = EditClientDialog(self.app, title=_("Add Client"), data={}, save_handler=self.save_client)
        result = self.app.show_jans_dialog(dialog)

    def delete_client(self, selected, event):
        """This method for the deletion of the clients data

        Args:
            selected (_type_): The selected Client
            event (_type_): _description_

        Returns:
            str: The server response
        """

        dialog = self.app.get_confirm_dialog(_("Are you sure want to delete client inum:")+"\n {} ?".format(selected[0]))

        async def coroutine():
            focused_before = self.app.layout.current_window
            result = await self.app.show_dialog_as_float(dialog)
            try:
                self.app.layout.focus(focused_before)
            except:
                self.app.layout.focus(self.app.center_frame)

            if result.lower() == 'yes':
                result = self.app.cli_object.process_command_by_id(
                    operation_id='delete-oauth-openid-clients-by-inum',
                    url_suffix='inum:{}'.format(selected[0]),
                    endpoint_args='',
                    data_fn='',
                    data={}
                )
                self.oauth_get_clients()
            return result

        ensure_future(coroutine())

    def edit_scope_dialog(self, **params):
        
        selected_line_data = params['data']  
        title = _("Edit Scopes")

        dialog = EditScopeDialog(self.app, title=title, data=selected_line_data, save_handler=self.save_client)
        self.app.show_jans_dialog(dialog)
