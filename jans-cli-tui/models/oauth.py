from curses import window
import threading

from collections import OrderedDict
import json
from asyncio import Future, ensure_future

from prompt_toolkit.application.current import get_app
from prompt_toolkit.key_binding import KeyBindings
from prompt_toolkit.key_binding.bindings.focus import focus_next, focus_previous
from prompt_toolkit.layout.containers import (
    ConditionalContainer,
    Float,
    HSplit,
    VSplit,
    VerticalAlign,
    HorizontalAlign,
    DynamicContainer,
    FloatContainer,
    Window
)
from prompt_toolkit.layout.containers import VerticalAlign
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.layout.layout import Layout
from prompt_toolkit.lexers import PygmentsLexer ,DynamicLexer
from prompt_toolkit.widgets import (
    Box,
    Button,
    Frame,
    Label,
    RadioList,
    TextArea,
    CheckboxList,
    Shadow,
)
from prompt_toolkit.filters import Condition

from cli import config_cli
from wui_components.jans_nav_bar import JansNavBar
from wui_components.jans_side_nav_bar import JansSideNavBar
from wui_components.jans_vetrical_nav import JansVerticalNav
from wui_components.jans_dialog import JansDialog
from wui_components.jans_dialog_with_nav import JansDialogWithNav


class JansAuthServer:


    def initialize(self):
        self.oauth_containers = {}
        self.oauth_prepare_navbar()
        self.oauth_prepare_containers()
        self.oauth_prepare_tabs()
        self.oauth_nav_selection_changed(self.oauth_navbar.navbar_entries[0][0])


    def oauth_prepare_tabs(self):
        self.oauth_tabs = {}
        self.oauth_tabs['clients'] = OrderedDict()

        self.oauth_tabs['clients']['Basic'] = HSplit([
                        VSplit([
                            self.getTitledText("Client_ID", name='Client_ID',style='green'),
                            Window(width=1, char=" ",),
                            self.getTitledCheckBox("Active:", name='active', values=['active'],style='green'),
                            ]) ,
                        self.getTitledText("Client Name", name='displayName',style='green'),
                        self.getTitledText("Client Secret", name='clientSecret',style='green'),
                        self.getTitledText("Description", name='Description',style='green'),
                        self.getTitledRadioButton("Id_token Subject Type:", name='Id_token', values=['Pairwise','Public'],style='green'),
                        self.getTitledCheckBox("Grant:", name='Grant', values=['Authorizatuin Code', 'Refresh Token', 'UMA Ticket','Client Credential','Password','Implicit'],style='green'),
                        self.getTitledCheckBox("Response Types:", name='responseTypes', values=['code', 'token', 'id_token'],style='green'),
                        self.getTitledCheckBox("Supress Authorization:", name='Supress', values=['True'],style='green'),
                        self.getTitledRadioButton("Application Type:", name='applicationType', values=['web', 'native'],style='green'),
                        self.getTitledText("Redirect Uris", name='redirectUris', height=3,style='green'),
                        self.getTitledText("Redirect Regex", name='redirectregex',style='green'),
                        self.getTitledText("Scopes", name='scopes', height=3,style='green'),
                        # HSplit([Box(body=VSplit([self.yes_button, self.no_button], align="CENTER", padding=3), style="class:button-bar", height=1)],
                        #     height=D(),
                        #     align = VerticalAlign.BOTTOM,
                        #     )
                        ],width=D(),
                    )
        
        self.oauth_tabs['clients']['Tokens'] = HSplit([
            self.getTitledRadioButton("Access Token Type", name='access_token_type', values=['JWT','Reference'],style='green'),
            self.getTitledCheckBox("Incliude Claims in id_token", name='id_token_claims', values=['True'],style='green'),
            self.getTitledCheckBox("Run introspection script before JWT access token creation", name='Supress_JWT', values=['True'],style='green'),
            self.getTitledText("Token binding confirmation  method for id_token", name='id_token_binding_confirmation',style='green'),
            self.getTitledText("Access token additional audiences", name='access_token_audiences',style='green'),
            VSplit([
                            Button("+", handler=self.show_again,left_symbol='[',right_symbol=']',width=3)
            ]),
            self.getTitledText("Access token lifetime", name='access_token_lifetime',style='green'),
            self.getTitledText("Refresh token lifetime", name='refresh_token_lifetime',style='green'),
            self.getTitledText("Defult max authn age", name='max_authn_age',style='green'),

        ],width=D())

        self.oauth_tabs['clients']['Logout'] = HSplit([

                        self.getTitledText("Front channel logout URI", name='f_channel_logout_URI',style='green'),
                        self.getTitledText("Post logout redirect URI", name='p_channel_logout_redirect_URI',style='green'),
                        self.getTitledText("Back channel logout URI", name='b_channel_logout_URI',style='green'),
                        self.getTitledCheckBox("Back channel logout session required", name='b_channel_session_required', values=['True'],style='green'),
                        self.getTitledCheckBox("Front channel logout session required", name='f_channel_session_required', values=['True'],style='green'),

                        ],width=D()
                    )
        
        self.oauth_tabs['clients']['Software Info'] =  HSplit([
            self.getTitledText("Client URI", name='client_URI',style='green'),
            self.getTitledText("Policy URI", name='policy_URI',style='green'),
            self.getTitledText("Logo URI", name='logo_URI',style='green'),
            self.getTitledText("Term of service URI", name='term_of_service_URI',style='green'),
            self.getTitledText("Contacts", name='contacts',style='green'),
            VSplit([
                            Button("+", handler=self.show_again,left_symbol='[',right_symbol=']',width=3,)
            ]),
            self.getTitledText("Authorized JS origins", name='authorized_JS_origins',style='green'),
            VSplit([
                            Button("+", handler=self.show_again,left_symbol='[',right_symbol=']',width=3)
            ]),
            self.getTitledText("Software id", name='software_id',style='green'),
            self.getTitledText("Software version", name='software_version',style='green'),
            self.getTitledText("Software statement", name='software_statement',style='green'),
            
        ],width=D())

        self.oauth_tabs['clients']['CIBA/PAR/UMA'] = HSplit([
                        Label(text="CIBA",style='bold'),
                        self.getTitledRadioButton("Token delivery method", name='applicationType', values=['poll','push', 'ping'],style='green'),
                        self.getTitledText("Client notification endpoint", name='displayName',style='green'),
                        self.getTitledCheckBox("Require user code param", name='Supress', values=['True'],style='green'),
                        
                        Label(text="PAR",style='bold'),
                        self.getTitledText("Request lifetime", name='displayName',style='green'),
                        self.getTitledCheckBox("Request PAR", name='Supress', values=['True'],style='green'),
                        
                        Label("UMA",style='bold'),
                        self.getTitledRadioButton("PRT token type", name='applicationType', values=['JWT', 'Reference'],style='green'),
                        self.getTitledText("Claims redirect URI", name='displayName',style='green'),
                        
                        # self.getButton(text="dropdown1", name='oauth:scopes:dropdown1', jans_help="dropdown1",handler=self.testdropdown),
                        # self.getButton(text="dropdown2", name='oauth:scopes:dropdown2', jans_help="dropdown2",handler=self.testdropdown),

                        Label(text="dropdown1",style='blue'),  ## TODO with Jans VerticalNav  
                        Label(text="dropdown2",style='blue'),  ## TODO with Jans VerticalNav  
                        Label(text="tabel",style='blue'),  ## TODO with Jans VerticalNav  

                        # JansVerticalNav(
                        #     myparent=self,
                        #     headers=['Client ID', 'Client Name', 'Grant Types', 'Subject Type'],
                        #     preferred_size= [0,0,30,0],
                        #     data=[['1','2','3','4'],['1','2','3','4'],['1','2','3','4'],['1','2','3','4'],],
                        #     on_enter=self.edit_client_dialog,
                        #     on_display=self.data_display_dialog,
                        #     # selection_changed=self.data_selection_changed,
                        #     selectes=0,
                        #     headerColor='green',
                        #     entriesColor='white',
                        #     all_data=[['1','2','3','4'],['1','2','3','4'],['1','2','3','4'],['1','2','3','4'],]
                        # )                
                        ]
                            )
        
        self.oauth_tabs['clients']['Encryption/Signing'] = HSplit([
                        self.getTitledText("Client JWKS URI", name='displayName',style='green'),
                        self.getTitledText("Client JWKS", name='displayName',style='green'),
                        VSplit([
                            Label(text="id_token"), 
                            Label(text="a, b, c",style='red'),
                        ]),
                        VSplit([
                            Label(text="Access token"), 
                            Label(text="a",style='red'),
                        ]),
                        VSplit([
                            Label(text="Userinfo"), 
                            Label(text="a, b, c",style='red'),
                        ]),
                        VSplit([
                            Label(text="JARM"), 
                            Label(text="a, b, c",style='red'),
                        ]),
                        VSplit([
                            Label(text="Request Object"), 
                            Label(text="a, b, c",style='red'),
                        ]),
                
                        ]
                            )
        
        self.oauth_tabs['clients']['Advanced Client Properties'] = HSplit([

                        self.getTitledCheckBox("Default Prompt=login", name='Supress', values=['True'],style='green'),
                        VSplit([
                                self.getTitledCheckBox("Persist Authorizations", name='Supress', values=['True'],style='green'),
                                self.getTitledCheckBox("Keep expired?", name='Supress', values=['True'],style='green'),
                            ]) ,                       
                        self.getTitledCheckBox("Allow spontaneos scopes", name='Supress', values=['True'],style='green'),

                        self.getTitledText("spontaneos scopes validation regex", name='displayName',style='green'),
                        VSplit([
                                Label(text="Spontaneous Scopes",style='green'),
                                Button("view current", handler=self.show_again,left_symbol='',right_symbol='',)
                            
                            ]) ,  
                        self.getTitledText("Initial Login URI", name='displayName',style='green'),

                        VSplit([
                                self.getTitledText("Request URIs", name='clientSecret',style='green'),
                                Button("+", handler=self.show_again,left_symbol='[',right_symbol=']',width=3,)
                            ]) ,  

                            Label(text="Dropdown 3",style='blue'),

                        VSplit([
                                self.getTitledText("Allowed ACRs", name='clientSecret',style='green'),
                                Button("+", handler=self.show_again,left_symbol='[',right_symbol=']',width=3,)
                            ]) , 
                        self.getTitledText("TLS Subject DN", name='clientSecret',style='green'),

                        VSplit([
                        self.getTitledCheckBox("Client Experiation Date", name='id_token_claims', values=['True'],style='green'),
                            Label(text="Pick Date",style='blue'),
                            ]) , 
                        

                        
                        ],width=D()
                    )
        
        self.oauth_tabs['clients']['Client Scripts'] = HSplit([
                Label(text="Dropdown 4",style='blue'),
                Label(text="Dropdown 5",style='blue'),
                Label(text="Dropdown 6",style='blue'),
                Label(text="Dropdown 7",style='blue'),
                Label(text="Dropdown 8",style='blue'),
                        ]
                        )
        
        
        self.oauth_tabs['clients']['Save'] = HSplit([
                        Button("Save", handler=self.show_again,left_symbol='(',right_symbol=')',)

                        ],width=D()
                    )
        
        self.oauth_dialog_nav = list(self.oauth_tabs['clients'].keys())[0]


    def client_dialog_nav_selection_changed(self, selection):
        self.oauth_dialog_nav = selection

    def edit_client(self, selected,event,size): ## enter 
        self.edit_client_dialog()
        # self.active_dialog_select = 'enter'
        # self.show_dialog = True

        # event.app.layout.focus(self.my_dialogs())

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
                        self.getButton(text="Add Scope", name='oauth:scopes:add', jans_help="To add a new scope press this button")
                        ],
                        padding=3,
                        width=D(),
                    ),
                    DynamicContainer(lambda: self.oauth_data_container['scopes'])
                    ])

        self.oauth_containers['clients'] = HSplit([
                    VSplit([
                        self.getButton(text="Get Clients", name='oauth:clients:get', jans_help="Retreive first 10 OpenID Connect clients", handler=self.oauth_get_clients),
                        self.getTitledText('Search: ', name='oauth:clients:search', jans_help='Press enter to perform search'),
                        self.getButton(text="Add Client", name='oauth:clients:add', jans_help="To add a new client press this button")
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

        self.dialogs['oauth:clients:d'] = JansDialog(
            entries_list=[[DynamicContainer(lambda:self.data_show_client_dialog) ,'']],
            title='Show client Dialog',
            button_functions=[[self.dialog_back_but,"Back"]],
            height=self.dialog_height,
            width=self.dialog_width,
            only_view=True  
            ) 

        self.dialogs['oauth:scopes:d'] = JansDialog(
            entries_list=[[DynamicContainer(lambda:self.data_show_client_dialog) ,'']],
            title='Show scopes Dialog',
            button_functions=[[self.dialog_back_but,"Back"]],
            height=self.dialog_height,
            width=self.dialog_width,
            only_view=True 
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

    def oauth_update_clients(self):
        try :
            result = self.cli_object.process_command_by_id('get-oauth-openid-clients', '', 'limit:10', {})

            data =[]

            for d in result: 
                data.append(
                    [
                    d['inum'],
                    d['clientName']['values'][''],
                    ','.join(d['grantTypes']),
                    d['subjectType'] 
                    ]
                )

            clients = JansVerticalNav(
                myparent=self,
                headers=['Client ID', 'Client Name', 'Grant Types', 'Subject Type'],
                preferred_size= [0,0,30,0],
                data=data,
                on_enter=self.edit_client,
                on_display=self.data_display_dialog,
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

        except Exception as e:
            self.oauth_data_container['clients'] = HSplit([Label("Faild to Fitch client Data.. Reason: " + str(e))], width=D())
            get_app().invalidate()

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
                on_enter=self.edit_scope,
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
