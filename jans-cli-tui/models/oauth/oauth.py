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
)

from cli import config_cli
from wui_components.jans_nav_bar import JansNavBar
from wui_components.jans_side_nav_bar import JansSideNavBar
from wui_components.jans_vetrical_nav import JansVerticalNav
from wui_components.jans_dialog import JansDialog
from wui_components.jans_dialog_with_nav import JansDialogWithNav
from wui_components.jans_drop_down import DropDownWidget
from models.oauth.edit_client_dialog import EditClientDialog
from models.oauth.edit_scope_dialog import EditScopeDialog
from wui_components.jans_data_picker import DateSelectWidget


class JansAuthServer:

    def initialize(self):
        self.oauth_containers = {}
        self.oauth_prepare_navbar()
        self.oauth_prepare_containers()
        self.oauth_nav_selection_changed(self.oauth_navbar.navbar_entries[0][0])

    def oauth_prepare_containers(self):

        self.oauth_data_container = {
            'clients' :HSplit([],width=D()),
            'scopes' :HSplit([],width=D()),

        } 
        self.oauth_main_area = HSplit([],width=D())

        self.oauth_containers['scopes'] = HSplit([
                    VSplit([
                        self.getButton(text="Get Scopes", name='oauth:scopes:get', jans_help="Retreive first 10 Scopes", handler=self.oauth_get_scopes),
                        self.getTitledText('Search: ', name='oauth:scopes:search', jans_help='Press enter to perform search'),
                        self.getButton(text="Add Scope", name='oauth:scopes:add', jans_help="To add a new scope press this button"),
                        ],
                        padding=3,
                        width=D(),
                    ),
                    DynamicContainer(lambda: self.oauth_data_container['scopes'])
                    ])

        self.oauth_containers['clients'] = HSplit([
                    VSplit([
                        self.getButton(text="Get Clients", name='oauth:clients:get', jans_help="Retreive first 10 OpenID Connect clients", handler=self.oauth_get_clients),
                        self.getTitledText('Search', name='oauth:clients:search', jans_help='Press enter to perform search', accept_handler=self.search_clients),
                        self.getButton(text="Add Client", name='oauth:clients:add', jans_help="To add a new client press this button", handler=self.add_client),
                        
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
         self.oauth_navbar = JansNavBar(
                    self,
                    entries=[('clients', 'Clients'), ('scopes', 'Scopes'), ('keys', 'Keys'), ('defaults', 'Defaults'), ('properties', 'Properties'), ('logging', 'Logging')],
                    selection_changed=self.oauth_nav_selection_changed,
                    select=0,
                    bgcolor='#66d9ef'
                    )

    def oauth_nav_selection_changed(self, selection):
        if selection in self.oauth_containers:
            self.oauth_main_area = self.oauth_containers[selection]
        else:
            self.oauth_main_area = self.not_implemented

    def oauth_set_center_frame(self):
        self.center_container = self.oauth_main_container

    def oauth_update_clients(self, pattern=''):
        endpoint_args='limit:10'
        if pattern:
            endpoint_args='limit:10,pattern:'+pattern

        try :
            rsponse = self.cli_object.process_command_by_id(
                        operation_id='get-oauth-openid-clients',
                        url_suffix='',
                        endpoint_args=endpoint_args,
                        data_fn=None,
                        data={}
                        )

        except Exception as e:
            self.show_message("Error getting clients", str(e))
            return

        if rsponse.status_code not in (200, 201):
            self.show_message("Error getting clients", str(rsponse.text))
            return

        try:
            result = rsponse.json()
        except Exception:
            self.show_message("Error getting clients", str(rsponse.text))
            press_tab
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
                myparent=self,
                headers=['Client ID', 'Client Name', 'Grant Types', 'Subject Type'],
                preferred_size= [0,0,30,0],
                data=data,
                on_enter=self.edit_client_dialog,
                on_display=self.data_display_dialog,
                on_delete=self.delete_client,
                # selection_changed=self.data_selection_changed,
                selectes=0,
                headerColor='green',
                entriesColor='white',
                all_data=result
            )

            self.layout.focus(clients)   # clients.focuse..!? TODO >> DONE
            self.oauth_data_container['clients'] = HSplit([
                clients
            ])
            get_app().invalidate()

        else:
            self.show_message("Oops", "No matching result")


    def oauth_get_clients(self):
        self.oauth_data_container['clients'] = HSplit([Label("Please wait while getting clients")], width=D())
        t = threading.Thread(target=self.oauth_update_clients, daemon=True)
        t.start()

    def update_oauth_scopes(self, start_index=0):
        try :
            result = self.cli_object.process_command_by_id('get-oauth-scopes', '', 'limit:10', {})
            data =[]
            for d in result: 
                data.append(
                    [
                    d['id'],
                    d['description'],
                    d['scopeType']
                    ]
                )

            clients = JansVerticalNav(
                myparent=self,
                headers=['id', 'Description', 'Type'],
                preferred_size= [0,0,30,0],
                data=data,
                on_enter=self.edit_scope_dialog,
                on_display=self.data_display_dialog,
                # selection_changed=self.data_selection_changed,
                selectes=0,
                headerColor='green',
                entriesColor='white',
                all_data=result
            )

            buttons = []
            if start_index > 0:
                buttons.append(Button("Prev"))
            if len(result) >= 10:
                buttons.append(Button("Next"))

            self.layout.focus(clients)   # clients.focuse..!? TODO >> DONE
            self.oauth_data_container['scopes'] = HSplit([
                clients,
                VSplit(buttons, padding=5, align=HorizontalAlign.CENTER)
            ])

            get_app().invalidate()

        except Exception as e:
            self.oauth_data_container['scopes'] = HSplit([Label("Faild to Fitch client Data.. Reason: " + str(e))], width=D())
            get_app().invalidate()

    def oauth_get_scopes(self):
        self.oauth_data_container['scopes'] = HSplit([Label("Please wait while getting Scopes")], width=D())
        t = threading.Thread(target=self.update_oauth_scopes, daemon=True)
        t.start()

    def display_scope(self):
        pass

    def edit_scope(self, selected,event,size): ## enter 
        self.edit_scope_dialog()

    def edit_client_dialog(self, **params):

        selected_line_data = params['data']  
        title = "Edit user Data (Clients)"
 
        dialog = EditClientDialog(self, title=title, data=selected_line_data, save_handler=self.save_client)
        self.show_jans_dialog(dialog)

    def save_client(self, dialog):

        self.logger.debug(dialog.data)
        return
        response = self.cli_object.process_command_by_id(
            operation_id='put-oauth-openid-clients' if dialog.data.get('inum') else 'post-oauth-openid-clients',
            url_suffix='',
            endpoint_args='',
            data_fn='',
            data=dialog.data
        )

        self.logger.debug(response.text)

        if response.status_code in (200, 201):
            self.oauth_get_clients()
            return True

        self.show_message("Error!", "An error ocurred while saving client:\n" + str(response.text))

    def search_clients(self, tbuffer):
        if not len(tbuffer.text) > 2:
            self.show_message("Error!", "Search string should be at least three characters")
            return

        t = threading.Thread(target=self.oauth_update_clients, args=(tbuffer.text,), daemon=True)
        t.start()



    def add_client(self):
        dialog = EditClientDialog(self, title="Add Client", data={}, save_handler=self.save_client)
        result = self.show_jans_dialog(dialog)

    def delete_client(self, selected, event):

        dialog = self.get_confirm_dialog("Are you sure want to delete client inum:\n {} ?".format(selected[0]))

        async def coroutine():
            app = get_app()
            focused_before = app.layout.current_window
            self.layout.focus(dialog)
            result = await self.show_dialog_as_float(dialog)
            try:
                app.layout.focus(focused_before)
            except:
                app.layout.focus(self.center_frame)

            if result.lower() == 'yes':
                result = self.cli_object.process_command_by_id(
                    operation_id='delete-oauth-openid-clients-by-inum',
                    url_suffix='inum:{}'.format(selected[0]),
                    endpoint_args='',
                    data_fn='',
                    data={}
                )
                self.oauth_get_clients()
            return result

        ensure_future(coroutine())
