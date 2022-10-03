import os
import sys

import threading
from asyncio import ensure_future
from functools import partial

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
from static import DialogResult

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
                        self.app.getTitledText(_("Search: "), name='oauth:scopes:search', jans_help=_("Press enter to perform search"), accept_handler=self.search_scope),
                        self.app.getButton(text=_("Add Scope"), name='oauth:scopes:add', jans_help=_("To add a new scope press this button"), handler=self.add_scope),
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

    def oauth_update_clients(self,start_index=0, pattern=''):
        """update the current clients data to server

        Args:
            pattern (str, optional): endpoint arguments for the client data. Defaults to ''.
        """

        def get_next(start_index, pattern=''):
            self.oauth_update_clients(start_index, pattern='')

        endpoint_args ='limit:{},startIndex:{}'.format(self.app.entries_per_page, start_index)
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

        for d in result.get('entries', []):
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
                all_data=result['entries']
            )
            buttons = []
            if start_index > 0:
                handler_partial = partial(get_next, start_index-self.app.entries_per_page, pattern)
                prev_button = Button(_("Prev"), handler=handler_partial)
                prev_button.window.jans_help = _("Retreives previous %d entries") % self.app.entries_per_page
                buttons.append(prev_button)
            if  result['start'] + self.app.entries_per_page <  result['totalEntriesCount']:
                handler_partial = partial(get_next, start_index+self.app.entries_per_page, pattern)
                next_button = Button(_("Next"), handler=handler_partial)
                next_button.window.jans_help = _("Retreives next %d entries") % self.app.entries_per_page
                buttons.append(next_button)

            self.app.layout.focus(clients)   # clients.focuse..!? TODO >> DONE
            self.oauth_data_container['clients'] = HSplit([
                clients,
                VSplit(buttons, padding=5, align=HorizontalAlign.CENTER)
            ])

            get_app().invalidate()

        else:
            self.app.show_message(_("Oops"), _("No matching result"),tobefocused = self.oauth_containers['clients'])

    def oauth_get_clients(self):
        """Method to get the clients data from server
        """ 
        self.oauth_data_container['clients'] = HSplit([Label(_("Please wait while getting clients"))], width=D())
        t = threading.Thread(target=self.oauth_update_clients, daemon=True)
        t.start()

    def oauth_update_scopes(self, start_index=0, pattern=''):
        """update the current Scopes data to server

        Args:
            start_index (int, optional): add Button("Prev") to the layout. Defaults to 0.
        """
        def get_next(start_index, pattern=''):
            self.oauth_update_scopes(start_index, pattern='')

        endpoint_args ='limit:{},startIndex:{}'.format(self.app.entries_per_page, start_index)
        if pattern:
            endpoint_args +=',pattern:'+pattern
        try :
            rsponse = self.app.cli_object.process_command_by_id(
                operation_id='get-oauth-scopes',
                url_suffix='',
                endpoint_args=endpoint_args,
                data_fn=None,
                data={}
                        )

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
        
        for d in result.get('entries', []): 
            data.append(
                [
                d['id'],
                d.get('description', ''),
                d.get('scopeType',''),   ## some scopes have no scopetypr
                d['inum']
                ]
            )
        
        if data:

            scopes = JansVerticalNav(
                    myparent=self.app,
                    headers=['id', 'Description', 'Type','inum'],
                    preferred_size= [25,50,30,30],
                    data=data,
                    on_enter=self.edit_scope_dialog,
                    on_display=self.app.data_display_dialog,
                    on_delete=self.delete_scope,
                    # selection_changed=self.data_selection_changed,
                    selectes=0,
                    headerColor='green',
                    entriesColor='white',
                    all_data=result['entries']
                )

            buttons = []
            if start_index > 0:
                handler_partial = partial(get_next, start_index-self.app.entries_per_page, pattern)
                prev_button = Button(_("Prev"), handler=handler_partial)
                prev_button.window.jans_help = _("Retreives previous %d entries") % self.app.entries_per_page
                buttons.append(prev_button)
            if  result['start'] + self.app.entries_per_page <  result['totalEntriesCount']:
                handler_partial = partial(get_next, start_index+self.app.entries_per_page, pattern)
                next_button = Button(_("Next"), handler=handler_partial)
                next_button.window.jans_help = _("Retreives next %d entries") % self.app.entries_per_page
                buttons.append(next_button)

            self.app.layout.focus(scopes)   # clients.focuse..!? TODO >> DONE
            self.oauth_data_container['scopes'] = HSplit([
                scopes,
                VSplit(buttons, padding=5, align=HorizontalAlign.CENTER)
            ])

            get_app().invalidate()

        else:
            self.app.show_message(_("Oops"), _("No matching result"),tobefocused = self.oauth_containers['scopes'])

    def oauth_get_scopes(self):
        """Method to get the Scopes data from server
        """
        self.oauth_data_container['scopes'] = HSplit([Label(_("Please wait while getting Scopes"))], width=D())
        t = threading.Thread(target=self.oauth_update_scopes, daemon=True)
        t.start()

    def display_scope(self):
        pass

  
    def edit_scope_dialog(self, **params): 
        selected_line_data = params['data']  

        dialog = EditScopeDialog(self.app, title=_("Edit Scopes"), data=selected_line_data,save_handler=self.save_scope)
        self.app.show_jans_dialog(dialog)


    def edit_client_dialog(self, **params):
        selected_line_data = params['data']  
        title = _("Edit user Data (Clients)")

        self.EditClientDialog = EditClientDialog(self.app, title=title, data=selected_line_data,save_handler=self.save_client,delete_UMAresource=self.delete_UMAresource)
        self.app.show_jans_dialog(self.EditClientDialog)

    def save_client(self, dialog):
        """This method to save the client data to server

        Args:
            dialog (_type_): the main dialog to save data in

        Returns:
            _type_: bool value to check the status code response
        """

        # self.app.logger.debug('Saved DATA: '+str(dialog.data))

        response = self.app.cli_object.process_command_by_id(
            operation_id='put-oauth-openid-clients' if dialog.data.get('inum') else 'post-oauth-openid-clients',
            url_suffix='',
            endpoint_args='',
            data_fn='',
            data=dialog.data
        )

        # self.app.logger.debug(response.text)

        if response.status_code in (200, 201):
            self.oauth_get_clients()
            return True

        self.app.show_message(_("Error!"), _("An error ocurred while saving client:\n") + str(response.text))

    def save_scope(self, dialog):
        """This method to save the client data to server

        Args:
            dialog (_type_): the main dialog to save data in

        Returns:
            _type_: bool value to check the status code response
        """

        # self.app.logger.debug('Saved scope DATA: '+str(dialog.data))

        response = self.app.cli_object.process_command_by_id(
            operation_id='put-oauth-scopes' if dialog.data.get('inum') else 'post-oauth-scopes',
            url_suffix='',
            endpoint_args='',
            data_fn='',
            data=dialog.data
        )

        # self.app.logger.debug(response.text)

        if response.status_code in (200, 201):
            self.oauth_get_scopes()
            return True

        self.app.show_message(_("Error!"), _("An error ocurred while saving client:\n") + str(response.text))

    def search_scope(self, tbuffer):
        if not len(tbuffer.text) > 2:
            self.app.show_message(_("Error!"), _("Search string should be at least three characters"),tobefocused=self.oauth_containers['scopes'])
            return

        t = threading.Thread(target=self.oauth_update_scopes, args=(0,tbuffer.text), daemon=True)
        t.start()

    def search_clients(self, tbuffer):
        if not len(tbuffer.text) > 2:
            self.app.show_message(_("Error!"), _("Search string should be at least three characters"),tobefocused=self.oauth_containers['clients'])
            return

        t = threading.Thread(target=self.oauth_update_clients, args=(0,tbuffer.text), daemon=True)
        t.start()

    def add_scope(self):
        """Method to display the dialog of clients
        """
        dialog = EditScopeDialog(self.app, title=_("Add New Scope"), data={}, save_handler=self.save_scope)
        result = self.app.show_jans_dialog(dialog)

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
                # TODO Need to do `self.oauth_get_clients()` only if clients list is not empty
                self.oauth_get_clients()
            return result

        ensure_future(coroutine())

    def delete_scope(self, selected, event):
        """This method for the deletion of the clients data

        Args:
            selected (_type_): The selected Client
            event (_type_): _description_

        Returns:
            str: The server response
        """

        dialog = self.app.get_confirm_dialog(_("Are you sure want to delete scope inum:")+"\n {} ?".format(selected[3]))
        async def coroutine():
            focused_before = self.app.layout.current_window
            result = await self.app.show_dialog_as_float(dialog)
            try:
                self.app.layout.focus(focused_before)
            except:
                self.app.layout.focus(self.app.center_frame)

            if result.lower() == 'yes':
                result = self.app.cli_object.process_command_by_id(
                    operation_id='delete-oauth-scopes-by-inum',
                    url_suffix='inum:{}'.format(selected[3]),
                    endpoint_args='',
                    data_fn='',
                    data={}
                )
            
                # TODO Need to do `self.oauth_get_scopes()` only if scopes is not empty

                self.oauth_get_scopes()
            return result

        ensure_future(coroutine())

    def delete_UMAresource(self, selected, event=None):
        

        dialog = self.app.get_confirm_dialog(_("Are you sure want to delete UMA resoucres with id:")+"\n {} ?".format(selected[0]))
        async def coroutine():
            focused_before = self.app.layout.current_window
            result = await self.app.show_dialog_as_float(dialog)
            try:
                self.app.layout.focus(focused_before)
            except:
                self.app.layout.focus(self.EditClientDialog)

            if result.lower() == 'yes':
                result = self.app.cli_object.process_command_by_id(
                    operation_id='delete-oauth-uma-resources-by-id',
                    url_suffix='id:{}'.format(selected[0]),
                    endpoint_args='',
                    data_fn=None,
                    data={}
                    )
                # TODO Need to do `self.oauth_get_uma_resources()` only if UMA-res list is not empty
                self.EditClientDialog.oauth_get_uma_resources()
                
            return result


        ensure_future(coroutine())
        