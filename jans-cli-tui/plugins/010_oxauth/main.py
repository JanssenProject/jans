import os
import sys
import time

import threading
from asyncio import Future, ensure_future
from functools import partial
from typing import Any, Optional


import prompt_toolkit
from prompt_toolkit.application.current import get_app
from prompt_toolkit.key_binding import KeyBindings
from prompt_toolkit.key_binding.bindings.focus import focus_next, focus_previous
from prompt_toolkit.layout.containers import (
    HSplit,
    VSplit,
    HorizontalAlign,
    DynamicContainer,
    Window,
)
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.widgets import (
    Box,
    Button,
    Label,
    Frame,
    Dialog,
    CheckboxList,
    TextArea
)
from prompt_toolkit.lexers import PygmentsLexer, DynamicLexer

import json
from static import DialogResult
from prompt_toolkit.layout import ScrollablePane
from asyncio import Future

from cli import config_cli
from utils import DialogUtils
from wui_components.jans_nav_bar import JansNavBar
from wui_components.jans_side_nav_bar import JansSideNavBar
from wui_components.jans_vetrical_nav import JansVerticalNav
from wui_components.jans_dialog import JansDialog
from wui_components.jans_dialog_with_nav import JansDialogWithNav
from wui_components.jans_drop_down import DropDownWidget
from wui_components.jans_data_picker import DateSelectWidget
from wui_components.jans_cli_dialog import JansGDialog

from view_property import ViewProperty
from edit_client_dialog import EditClientDialog
from edit_scope_dialog import EditScopeDialog
from prompt_toolkit.buffer import Buffer
from prompt_toolkit.application import Application

from multi_lang import _
import cli_style

class Plugin(DialogUtils):
    """This is a general class for plugins 
    """
    def __init__(
        self, 
        app: Application
        ) -> None:
        """init for Plugin class "oxauth"

        Args:
            app (_type_): _description_
        """
        self.app = app
        self.pid = 'oxauth'
        self.name = '[A]uth Server'
        self.search_text= None

        self.oauth_containers = {}
        self.oauth_prepare_navbar()
        self.oauth_prepare_containers()
        self.oauth_nav_selection_changed(self.nav_bar.navbar_entries[0][0])

    def init_plugin(self) -> None:

        self.app.create_background_task(self.get_appconfiguration())
        self.schema = self.app.cli_object.get_schema_from_reference('#/components/schemas/AppConfiguration')


    async def get_appconfiguration(self) -> None:
        """Coroutine for getting application configuration.
        """
        try:
            response = self.app.cli_object.process_command_by_id(
                        operation_id='get-properties',
                        url_suffix='',
                        endpoint_args='',
                        data_fn=None,
                        data={}
                        )

        except Exception as e:
            self.app.show_message(_("Error getting Jans configuration"), str(e))
            return

        if response.status_code not in (200, 201):
            self.app.show_message(_("Error getting Jans configuration"), str(response.text))
            return

        self.app_configuration = response.json()
        self.oauth_logging()

    def process(self):
        pass

    def set_center_frame(self) -> None:
        """center frame content
        """
        self.app.center_container = self.oauth_main_container

    def oauth_prepare_containers(self) -> None:
        """prepare the main container (tabs) for the current Plugin 
        """

        self.oauth_data_container = {
            'clients': HSplit([],width=D()),
            'scopes': HSplit([],width=D()),
            'keys': HSplit([],width=D()),
            'properties': HSplit([],width=D()),
            'logging': HSplit([],width=D()),
        }

        self.oauth_main_area = HSplit([],width=D())

        self.oauth_containers['scopes'] = HSplit([
                    VSplit([
                        self.app.getButton(text=_("Get Scopes"), name='oauth:scopes:get', jans_help=_("Retreive first 10 Scopes"), handler=self.oauth_get_scopes),
                        self.app.getTitledText(_("Search: "), name='oauth:scopes:search', jans_help=_("Press enter to perform search"), accept_handler=self.search_scope,style='class:outh_containers_scopes.text'),
                        self.app.getButton(text=_("Add Scope"), name='oauth:scopes:add', jans_help=_("To add a new scope press this button"), handler=self.add_scope),
                        ],
                        padding=3,
                        width=D(),
                    ),
                    DynamicContainer(lambda: self.oauth_data_container['scopes'])
                    ],style='class:outh_containers_scopes')

        self.oauth_containers['clients'] = HSplit([
                    VSplit([
                        self.app.getButton(text=_("Get Clients"), name='oauth:clients:get', jans_help=_("Retreive first 10 OpenID Connect clients"), handler=self.oauth_get_clients),
                        self.app.getTitledText(_("Search"), name='oauth:clients:search', jans_help=_("Press enter to perform search"), accept_handler=self.search_clients,style='class:outh_containers_clients.text'),
                        self.app.getButton(text=_("Add Client"), name='oauth:clients:add', jans_help=_("To add a new client press this button"), handler=self.add_client),
                        
                        ],
                        padding=3,
                        width=D(),
                        ),
                        DynamicContainer(lambda: self.oauth_data_container['clients'])
                     ],style='class:outh_containers_clients')


        self.oauth_containers['keys'] = HSplit([
                    VSplit([
                        self.app.getButton(text=_("Get Keys"), name='oauth:keys:get', jans_help=_("Retreive Auth Server keys"), handler=self.oauth_get_keys),
                        ],
                        padding=3,
                        width=D(),
                        ),
                        DynamicContainer(lambda: self.oauth_data_container['keys'])
                     ], style='class:outh_containers_clients')

        self.oauth_containers['properties'] = HSplit([
                    VSplit([
                        self.app.getButton(text=_("Get properties"), name='oauth:scopes:get', jans_help=_("Retreive first 10 Scopes"), handler=self.oauth_get_properties),
                        self.app.getTitledText(
                            _("Search: "), 
                            name='oauth:properties:search', 
                            jans_help=_("Press enter to perform search"), 
                            accept_handler=self.search_properties,
                            style='class:outh_containers_scopes.text')                        ],
                        padding=3,
                        width=D(),
                    ),
                    DynamicContainer(lambda: self.oauth_data_container['properties'])
                    ],style='class:outh_containers_scopes')


        self.oauth_containers['logging'] = DynamicContainer(lambda: self.oauth_data_container['logging'])

        self.oauth_main_container = HSplit([
                                        Box(self.nav_bar.nav_window, style='class:sub-navbar', height=1),
                                        DynamicContainer(lambda: self.oauth_main_area),
                                        ],
                                    height=D(),
                                    style='class:outh_maincontainer'
                                    )

    def oauth_prepare_navbar(self) -> None:
        """prepare the navbar for the current Plugin 
        """
        self.nav_bar = JansNavBar(
                    self.app,
                    entries=[('clients', 'C[l]ients'), ('scopes', 'Sc[o]pes'), ('keys', '[K]eys'), ('defaults', '[D]efaults'), ('properties', 'Properti[e]s'), ('logging', 'Lo[g]ging')],
                    selection_changed=self.oauth_nav_selection_changed,
                    select=0,
                    jans_name='oauth:nav_bar'
                    )

    def oauth_nav_selection_changed(
        self, 
        selection
        ) -> None:
        """This method for the selection change

        Args:
            selection (str): the current selected tab
        """
        if selection in self.oauth_containers:
            self.oauth_main_area = self.oauth_containers[selection]
        else:
            self.oauth_main_area = self.app.not_implemented

    def oauth_update_clients(
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
                headerColor='class:outh-verticalnav-headcolor',
                entriesColor='class:outh-verticalnav-entriescolor',
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
            self.app.layout.focus(clients)  ### it fix focuse on the last item deletion >> try on UMA-res >> edit_client_dialog >> oauth_update_uma_resources

        else:
            self.app.show_message(_("Oops"), _("No matching result"),tobefocused = self.oauth_containers['clients'])

    def oauth_get_clients(self) -> None:
        """Method to get the clients data from server
        """ 
        self.oauth_data_container['clients'] = HSplit([Label(_("Please wait while getting clients"),style='class:outh-waitclientdata.label')], width=D(),style='class:outh-waitclientdata')
        t = threading.Thread(target=self.oauth_update_clients, daemon=True)
        t.start()

    def oauth_update_scopes(
        self, 
        start_index: Optional[int]= 0,  
        pattern: Optional[str]= '', 
        ) -> None:
        """update the current Scopes data to server

        Args:
            start_index (int, optional): add Button("Prev") to the layout. Defaults to 0.
        """
        def get_next(
            start_index: int,  
            pattern: Optional[str]= '', 
            ) -> None:
            self.oauth_update_scopes(start_index, pattern='')

        endpoint_args ='withAssociatedClients:true,limit:{},startIndex:{}'.format(self.app.entries_per_page, start_index)
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
                    preferred_size= [30,40,8,12],
                    data=data,
                    on_enter=self.edit_scope_dialog,
                    on_display=self.app.data_display_dialog,
                    on_delete=self.delete_scope,
                    # selection_changed=self.data_selection_changed,
                    selectes=0,
                    headerColor='class:outh-verticalnav-headcolor',
                    entriesColor='class:outh-verticalnav-entriescolor',
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
            self.app.layout.focus(scopes)  ### it fix focuse on the last item deletion >> try on UMA-res >> edit_client_dialog >> oauth_update_uma_resources

        else:
            self.app.show_message(_("Oops"), _("No matching result"),tobefocused = self.oauth_containers['scopes'])

    def oauth_get_scopes(self) -> None:
        """Method to get the Scopes data from server
        """
        self.oauth_data_container['scopes'] = HSplit([Label(_("Please wait while getting Scopes"),style='class:outh-waitscopedata.label')], width=D(),style='class:outh-waitclientdata')
        t = threading.Thread(target=self.oauth_update_scopes, daemon=True)
        t.start()

    # ---------------------------------------------------------------------- #
    # ---------------------------------------------------------------------- #
    # ---------------------------------------------------------------------- #
    def oauth_update_properties(
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
            self.app.logger.debug("start_index="+str(start_index))
            self.oauth_update_properties(start_index, pattern=pattern)

        # ------------------------------------------------------------------------------- #
        # ------------------------------------------------------------------------------- #
        # ------------------------------------------------------------------------------- #

        try :
            rsponse = self.app.cli_object.process_command_by_id(
                        operation_id='get-properties',
                        url_suffix='',
                        endpoint_args='',
                        data_fn=None,
                        data={}
                        )

        except Exception as e:
            self.app.show_message(_("Error getting properties"), str(e))
            return

        if rsponse.status_code not in (200, 201):
            self.app.show_message(_("Error getting properties"), str(rsponse.text))
            return

        try:
            result = rsponse.json()
        except Exception:
            self.app.show_message(_("Error getting properties"), str(rsponse.text))
            return

        # ------------------------------------------------------------------------------- #
        # ----------------------------------- Search ------------------------------------ #
        # ------------------------------------------------------------------------------- #
        porp_schema = self.app.cli_object.get_schema_from_reference('#/components/schemas/AppConfiguration')

        data =[]
        if pattern:
            for k in result:
                if pattern.lower() in k.lower():
                    if k in porp_schema.get('properties', {}):
                        data.append(
                            [
                            k,
                            result[k],
                            ]
                        )
        else:
            for d in result:
                if d in porp_schema.get('properties', {}):
                    data.append(
                        [
                        d,
                        result[d],
                        ]
                    )


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
                headers=['Property Name', 'Property Value'],
                preferred_size= [0,0],
                data=data_now,
                on_enter=self.view_property,
                on_display=self.properties_display_dialog,
                # on_delete=self.delete_client,
                # selection_changed=self.data_selection_changed,
                selectes=0,      
                headerColor='class:outh-verticalnav-headcolor',
                entriesColor='class:outh-verticalnav-entriescolor',
                all_data=list(result.values())
            )
            self.app.layout.focus(clients)   # clients.focuse..!? TODO >> DONE
            self.oauth_data_container['properties'] = HSplit([
                clients,
                VSplit(buttons, padding=5, align=HorizontalAlign.CENTER)
            ])
            get_app().invalidate()
            self.app.layout.focus(clients)  ### it fix focuse on the last item deletion >> try on UMA-res >> edit_client_dialog >> oauth_update_uma_resources
        else:
            self.app.show_message(_("Oops"), _("No matching result"),tobefocused = self.oauth_containers['properties'])

    def properties_display_dialog(self, **params: Any) -> None:
        data_property, data_value = params['selected'][0], params['selected'][1]
        body = HSplit([
                TextArea(
                    lexer=DynamicLexer(lambda: PygmentsLexer.from_filename('.json', sync_from_start=True)),
                    scrollbar=True,
                    line_numbers=True,
                    multiline=True,
                    read_only=True,
                    text=str(json.dumps(data_value, indent=2)),
                    style='class:jans-main-datadisplay.text'
                )
            ],style='class:jans-main-datadisplay')

        dialog = JansGDialog(self.app, title=data_property, body=body)

        self.app.show_jans_dialog(dialog)

    def oauth_get_properties(self) -> None:
        """Method to get the clients data from server
        """ 
        self.oauth_data_container['properties'] = HSplit([Label(_("Please wait while getting properties"),style='class:outh-waitclientdata.label')], width=D(),style='class:outh-waitclientdata')
        t = threading.Thread(target=self.oauth_update_properties, daemon=True)
        t.start()

    def view_property(self, **params: Any) -> None:
        #property, value =params['passed']


        selected_line_data = params['passed']    ##self.uma_result 
        
        title = _("Edit property")

        dialog = ViewProperty(self.app, title=title, data=selected_line_data, get_properties= self.oauth_get_properties, search_properties=self.search_properties, search_text=self.search_text)
        
        self.app.show_jans_dialog(dialog)
 
    def search_properties(self, tbuffer:Buffer,) -> None:
        self.app.logger.debug("tbuffer="+str(tbuffer))
        self.app.logger.debug("type tbuffer="+str(type(tbuffer)))
        self.search_text=tbuffer.text

        if not len(tbuffer.text) > 2:
            self.app.show_message(_("Error!"), _("Search string should be at least three characters"),tobefocused=self.oauth_containers['properties'])
            return
        t = threading.Thread(target=self.oauth_update_properties, args=(0,tbuffer.text), daemon=True)
        t.start()

    # ---------------------------------------------------------------------- #
    # ---------------------------------------------------------------------- #
    # ---------------------------------------------------------------------- #

    def oauth_update_keys(self) -> None:

        """update the current Keys fromserver
        """

        try :
            rsponse = self.app.cli_object.process_command_by_id(
                operation_id='get-config-jwks',
                url_suffix='',
                endpoint_args='',
                data_fn=None,
                data={}
                        )
        except Exception as e:
            self.app.show_message(_("Error getting keys"), str(e))
            return

        if rsponse.status_code not in (200, 201):
            self.app.show_message(_("Error getting keys"), str(rsponse.text))
            return

        try:
            result = rsponse.json()
        except Exception:
            self.app.show_message(_("Error getting keys"), str(rsponse.text))
            return

        data =[]

        for d in result.get('keys', []): 
            try:
                gmt = time.gmtime(int(d['exp'])/1000)
                exps = time.strftime("%d %b %Y %H:%M:%S", gmt)
            except Exception:
                exps = d.get('exp', '')
            data.append(
                [
                d['name'],
                exps,
                ]
            )

        if data:

            keys = JansVerticalNav(
                    myparent=self.app,
                    headers=['Name', 'Expiration'],
                    data=data,
                    preferred_size=[0,0],
                    on_display=self.app.data_display_dialog,
                    selectes=0,
                    headerColor='class:outh-verticalnav-headcolor',
                    entriesColor='class:outh-verticalnav-entriescolor',
                    all_data=result['keys']
                )

            self.oauth_data_container['keys'] = HSplit([keys])
            get_app().invalidate()
            self.app.layout.focus(keys)

        else:
            self.app.show_message(_("Oops"), _("Nothing to display"), tobefocused=self.oauth_containers['keys'])

    def oauth_get_keys(self) -> None:
        """Method to get the Keys from server
        """
        self.oauth_data_container['keys'] = HSplit([Label(_("Please wait while getting Keys"), style='class:outh-waitscopedata.label')], width=D(), style='class:outh-waitclientdata')
        t = threading.Thread(target=self.oauth_update_keys, daemon=True)
        t.start()
  
    def edit_scope_dialog(self, **params: Any) -> None:
        selected_line_data = params['data']  

        dialog = EditScopeDialog(self.app, title=_("Edit Scopes"), data=selected_line_data,save_handler=self.save_scope)
        self.app.show_jans_dialog(dialog)

    def edit_client_dialog(self, **params: Any) -> None:
        selected_line_data = params['data']  
        title = _("Edit user Data (Clients)")

        self.EditClientDialog = EditClientDialog(self.app, title=title, data=selected_line_data,save_handler=self.save_client,delete_UMAresource=self.delete_UMAresource)
        self.app.show_jans_dialog(self.EditClientDialog)

    def save_client(self, dialog: Dialog) -> None:
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

        if response.status_code in (200, 201):
            self.oauth_get_clients()
            return True

        self.app.show_message(_("Error!"), _("An error ocurred while saving client:\n") + str(response.text))

    def save_scope(self, dialog: Dialog) -> None:
        """This method to save the client data to server

        Args:
            dialog (_type_): the main dialog to save data in

        Returns:
            _type_: bool value to check the status code response
        """

        response = self.app.cli_object.process_command_by_id(
            operation_id='put-oauth-scopes' if dialog.data.get('inum') else 'post-oauth-scopes',
            url_suffix='',
            endpoint_args='',
            data_fn='',
            data=dialog.data
        )

        if response.status_code in (200, 201):
            self.oauth_get_scopes()
            return True

        self.app.show_message(_("Error!"), _("An error ocurred while saving client:\n") + str(response.text))

    def search_scope(self, tbuffer:Buffer,) -> None:
        if not len(tbuffer.text) > 2:
            self.app.show_message(_("Error!"), _("Search string should be at least three characters"),tobefocused=self.oauth_containers['scopes'])
            return

        t = threading.Thread(target=self.oauth_update_scopes, args=(0,tbuffer.text), daemon=True)
        t.start()

    def search_clients(self, tbuffer:Buffer,) -> None:
        if not len(tbuffer.text) > 2:
            self.app.show_message(_("Error!"), _("Search string should be at least three characters"),tobefocused=self.oauth_containers['clients'])
            return

        t = threading.Thread(target=self.oauth_update_clients, args=(0,tbuffer.text), daemon=True)
        t.start()

    def add_scope(self) -> None:
        """Method to display the dialog of clients
        """
        dialog = EditScopeDialog(self.app, title=_("Add New Scope"), data={}, save_handler=self.save_scope)
        result = self.app.show_jans_dialog(dialog)

    def add_client(self) -> None:
        """Method to display the dialog of clients
        """
        dialog = EditClientDialog(self.app, title=_("Add Client"), data={}, save_handler=self.save_client)
        result = self.app.show_jans_dialog(dialog)

    def delete_client(self, **kwargs: Any):
        """This method for the deletion of the clients data

        Args:
            selected (_type_): The selected Client
            event (_type_): _description_

        Returns:
            str: The server response
        """

        dialog = self.app.get_confirm_dialog(_("Are you sure want to delete client inum:")+"\n {} ?".format(kwargs ['selected'][0]))

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
                    url_suffix='inum:{}'.format(kwargs ['selected'][0]),
                    endpoint_args='',
                    data_fn='',
                    data={}
                )
                # TODO Need to do `self.oauth_get_clients()` only if clients list is not empty
                self.oauth_get_clients()
            return result

        ensure_future(coroutine())

    def delete_scope(self, **kwargs: Any):
        """This method for the deletion of the clients data

        Args:
            selected (_type_): The selected Client
            event (_type_): _description_

        Returns:
            str: The server response
        """

        dialog = self.app.get_confirm_dialog(_("Are you sure want to delete scope inum:")+"\n {} ?".format(kwargs ['selected'][3]))
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
                    url_suffix='inum:{}'.format(kwargs['selected'][3]),
                    endpoint_args='',
                    data_fn='',
                    data={}
                )
                self.oauth_get_scopes()
            return result

        ensure_future(coroutine())

    def delete_UMAresource(self, **kwargs: Any):
        dialog = self.app.get_confirm_dialog(_("Are you sure want to delete UMA resoucres with id:")+"\n {} ?".format(kwargs ['selected'][0]))
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
                    url_suffix='id:{}'.format(kwargs['selected'][0]),
                    endpoint_args='',
                    data_fn=None,
                    data={}
                    )
                self.EditClientDialog.oauth_get_uma_resources()

            return result

        ensure_future(coroutine())

    def oauth_logging(self) -> None:
        self.oauth_data_container['logging'] = HSplit([
                        self.app.getTitledWidget(
                                _('Log Level'),
                                name='loggingLevel',
                                widget=DropDownWidget(
                                    values=[('TRACE', 'TRACE'), ('DEBUG', 'DEBUG'), ('INFO', 'INFO'), ('WARN', 'WARN'), ('ERROR', 'ERROR'), ('FATAL', 'FATAL'), ('OFF', 'OFF')],
                                    value=self.app_configuration.get('loggingLevel')
                                    ),
                                jans_help=self.app.get_help_from_schema(self.schema, 'loggingLevel'),
                                ),
                        self.app.getTitledWidget(
                                _('Log Layout'),
                                name='loggingLayout',
                                widget=DropDownWidget(
                                    values=[('text', 'text'), ('json', 'json')],
                                    value=self.app_configuration.get('loggingLayout')
                                    ),
                                jans_help=self.app.get_help_from_schema(self.schema, 'loggingLayout'),
                                ),
                        self.app.getTitledCheckBox(
                            _("Enable HTTP Logging"), 
                            name='httpLoggingEnabled',
                            checked=self.app_configuration.get('httpLoggingEnabled'),
                            jans_help=self.app.get_help_from_schema(self.schema, 'httpLoggingEnabled'),
                            style='class:outh-client-checkbox'
                            ),
                        self.app.getTitledCheckBox(
                            _("Disable JDK Logger"), 
                            name='disableJdkLogger',
                            checked=self.app_configuration.get('disableJdkLogger'),
                            jans_help=self.app.get_help_from_schema(self.schema, 'disableJdkLogger'),
                            style='class:outh-client-checkbox'
                            ),
                        self.app.getTitledCheckBox(
                            _("Enable Oauth Audit Logging"), 
                            name='enabledOAuthAuditLogging',
                            checked=self.app_configuration.get('enabledOAuthAuditLogging'),
                            jans_help=self.app.get_help_from_schema(self.schema, 'enabledOAuthAuditLogging'),
                            style='class:outh-client-checkbox'
                            ),
                        Window(height=1),
                        HSplit([
                            self.app.getButton(text=_("Save Logging"), name='oauth:logging:save', jans_help=_("Save Auth Server logging configuration"), handler=self.save_logging),
                            Window(width=100),
                            ])
                     ], style='class:outh_containers_clients', width=D())

    def save_logging(self) -> None:
        mod_data = self.make_data_from_dialog({'logging':self.oauth_data_container['logging']})
        pathches = []
        for key_ in mod_data:
            if self.app_configuration.get(key_) != mod_data[key_]:
                pathches.append({'op':'replace', 'path': key_, 'value': mod_data[key_]})

        if pathches:
            response = self.app.cli_object.process_command_by_id(
                operation_id='patch-properties',
                url_suffix='',
                endpoint_args='',
                data_fn=None,
                data=pathches
                )
            self.schema = response
            body = HSplit([Label(_("Jans authorization server application configuration logging properties were saved."))])

            buttons = [Button(_("Ok"))]
            dialog = JansGDialog(self.app, title=_("Confirmation"), body=body, buttons=buttons)
            async def coroutine():
                focused_before = self.app.layout.current_window
                result = await self.app.show_dialog_as_float(dialog)
                try:
                    self.app.layout.focus(focused_before)
                except:
                    self.app.layout.focus(self.app.center_frame)

            ensure_future(coroutine())
