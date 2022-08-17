import json
from asyncio import Future
from typing import OrderedDict

from prompt_toolkit.widgets import Button, TextArea
from prompt_toolkit.application.current import get_app
from prompt_toolkit.layout.dimension import D
from static import DialogResult
from wui_components.jans_dialog import JansDialog
from prompt_toolkit.layout.containers import (
    VSplit,
    DynamicContainer,
)
from prompt_toolkit.widgets import (
    Button,
    Label,
    TextArea,

)

from cli import config_cli
from prompt_toolkit.layout.containers import (
    ConditionalContainer,
    Float,
    HSplit,
    VSplit,
    VerticalAlign,
    DynamicContainer,
    FloatContainer,
    Window
)
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
from wui_components.jans_dialog_with_nav import JansDialogWithNav
from wui_components.jans_side_nav_bar import JansSideNavBar
from wui_components.jans_cli_dialog import JansGDialog
import yaml


class EditClientDialog(JansGDialog):
    def __init__(self, parent, title, data ,buttons=...):
        super().__init__(parent, title, buttons)
        self.data = data
        self.prepare_tabs()

        
        def accept():
            self.future.set_result(DialogResult.ACCEPT)
        
        def cancel():
            self.future.set_result(DialogResult.CANCEL)

        self.side_NavBar = JansSideNavBar(myparent=self.myparent,
            entries=list(self.oauth_tabs['clients'].keys()),
            selection_changed=(self.client_dialog_nav_selection_changed) ,
            select=0,
            entries_color='#2600ff')

        self.dialog = JansDialogWithNav(
            title=title,
            navbar=DynamicContainer(lambda:self.side_NavBar),
            content=DynamicContainer(lambda: self.oauth_tabs['clients'][self.oauth_dialog_nav]),
             button_functions=[
                (accept, "Save"),
                (cancel, "Cancel")
            ],
            height=self.myparent.dialog_height,
            width=self.myparent.dialog_width,
                   )

    def prepare_tabs(self):
        self.oauth_tabs = {}
        self.oauth_tabs['clients'] = OrderedDict()

        self.oauth_tabs['clients']['Basic'] = HSplit([
                        VSplit([
                            self.getTitledText(title ="Client_ID", name='inum',style='green',),
                            Window(width=1, char=" ",),
                            self.getTitledCheckBox("Active:", name='disabled', values=['active'],style='green'),
                            ]) ,
                        self.getTitledText("Client Name", name='displayName',style='green'),
                        self.getTitledText("Client Secret", name='clientSecret',style='green'),
                        self.getTitledText("Description", name='description',style='green'),
                        Label(text="dropdown1",style='blue'),
                        self.getTitledRadioButton("Id_token Subject Type:", name='subjectTypesSupported', values=['Public','Pairwise'],style='green'),
                        self.getTitledCheckBox("Grant:", name='grantTypes', values=['Authorizatuin Code', 'Refresh Token', 'UMA Ticket','Client Credential','Password','Implicit'],style='green'),
                        self.getTitledCheckBox("Response Types:", name='responseTypes', values=['code', 'token', 'id_token'],style='green'),
                        self.getTitledCheckBox("Supress Authorization:", name='dynamicRegistrationPersistClientAuthorizations', values=['True'],style='green'),
                        self.getTitledRadioButton("Application Type:", name='applicationType', values=[ 'native','web'],style='green'),
                        self.getTitledText("Redirect Uris", name='redirectUris', height=3,style='green'),
                        self.getTitledText("Redirect Regex", name='redirectregex',style='green'),
                        self.getTitledText("Scopes", name='scopes', height=3,style='green'),

                        ],width=D(),
                    )

        self.oauth_tabs['clients']['Tokens'] = HSplit([
            self.getTitledRadioButton("Access Token Type", name='accessTokenAsJwt', values=['JWT','Reference'],style='green'),
            self.getTitledCheckBox("Incliude Claims in id_token", name='includeClaimsInIdToken', values=['True'],style='green'),
            self.getTitledCheckBox("Run introspection script before JWT access token creation", name='runIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims', values=['True'],style='green'),
            self.getTitledText("Token binding confirmation  method for id_token", name='idTokenTokenBindingCnf',style='green'),
            self.getTitledText("Access token additional audiences", name='additionalAudience',style='green'),
            VSplit([
                            Button("+", handler=self.myparent.show_again,left_symbol='[',right_symbol=']',width=3)
            ]),
            self.getTitledText("Access token lifetime", name='accessTokenLifetime',style='green'),
            self.getTitledText("Refresh token lifetime", name='refreshTokenLifetime',style='green'),
            self.getTitledText("Defult max authn age", name='defaultMaxAge',style='green'),

        ],width=D())

        self.oauth_tabs['clients']['Logout'] = HSplit([

                        self.getTitledText("Front channel logout URI", name='frontChannelLogoutUri',style='green'),
                        self.getTitledText("Post logout redirect URI", name='postLogoutRedirectUris',style='green'),
                        self.getTitledText("Back channel logout URI", name='backchannelLogoutUri',style='green'),
                        self.getTitledCheckBox("Back channel logout session required", name='backchannelLogoutSessionRequired', values=['True'],style='green'),
                        self.getTitledCheckBox("Front channel logout session required", name='frontChannelLogoutSessionRequired', values=['True'],style='green'),

                        ],width=D()
                    )
        
        self.oauth_tabs['clients']['Software Info'] =  HSplit([
            self.getTitledText("Client URI", name='clientUri',style='green'),
            self.getTitledText("Policy URI", name='policyUri',style='green'),
            self.getTitledText("Logo URI", name='logoUri',style='green'),
            self.getTitledText("Term of service URI", name='term_of_service_URI',style='green'),
            self.getTitledText("Contacts", name='contacts',style='green'),
            VSplit([
                            Button("+", handler=self.myparent.show_again,left_symbol='[',right_symbol=']',width=3,)
            ]),
            self.getTitledText("Authorized JS origins", name='authorizedOrigins',style='green'),
            VSplit([
                            Button("+", handler=self.myparent.show_again,left_symbol='[',right_symbol=']',width=3)
            ]),
            self.getTitledText("Software id", name='softwareId',style='green'),
            self.getTitledText("Software version", name='softwareVersion',style='green'),
            self.getTitledText("Software statement", name='softwareStatement',style='green'),
            
        ],width=D())

        self.oauth_tabs['clients']['CIBA/PAR/UMA'] = HSplit([
                        Label(text="CIBA",style='bold'),
                        self.getTitledRadioButton("Token delivery method", name='backchannelTokenDeliveryMode', values=['poll','push', 'ping'],style='green'),
                        self.getTitledText("Client notification endpoint", name='backchannelClientNotificationEndpoint',style='green'),
                        self.getTitledCheckBox("Require user code param", name='backchannelUserCodeParameterSupported', values=['True'],style='green'),
                        
                        Label(text="PAR",style='bold'),
                        self.getTitledText("Request lifetime", name='request!',style='green'),
                        self.getTitledCheckBox("Request PAR", name='sessionIdRequestParameterEnabled', values=['True'],style='green'),
                        
                        Label("UMA",style='bold'),
                        self.getTitledRadioButton("PRT token type", name='applicationType!', values=['JWT', 'Reference'],style='green'),
                        self.getTitledText("Claims redirect URI", name='claims!',style='green'),
                        
                        # self.myparent.getButton(text="dropdown1", name='oauth:scopes:dropdown1', jans_help="dropdown1",handler=self.myparent.testdropdown),
                        # self.myparent.getButton(text="dropdown2", name='oauth:scopes:dropdown2', jans_help="dropdown2",handler=self.myparent.testdropdown),

                        Label(text="dropdown1",style='blue'),  ## TODO with Jans VerticalNav  
                        Label(text="dropdown2",style='blue'),  ## TODO with Jans VerticalNav  
                        Label(text="tabel",style='blue'),  ## TODO with Jans VerticalNav  
              
                        ]
                            )
        
        self.oauth_tabs['clients']['Encryption/Signing'] = HSplit([
                        self.getTitledText("Client JWKS URI", name='jwksUri',style='green'),
                        self.getTitledText("Client JWKS", name='jwks',style='green'),
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

                        self.getTitledCheckBox("Default Prompt=login", name='defaultPromptLogin', values=['True'],style='green'),
                        VSplit([
                                self.getTitledCheckBox("Persist Authorizations", name='persistClientAuthorizations', values=['True'],style='green'),
                                self.getTitledCheckBox("Keep expired?", name='Supress', values=['True'],style='green'),
                            ]) ,                       
                        self.getTitledCheckBox("Allow spontaneos scopes", name='allowSpontaneousScopes', values=['True'],style='green'),

                        self.getTitledText("spontaneos scopes validation regex", name='spontaneousScopes',style='green'),
                        VSplit([
                                Label(text="Spontaneous Scopes",style='green'),
                                Button("view current", handler=self.myparent.show_again,left_symbol='',right_symbol='',)
                            
                            ]) ,  
                        self.getTitledText("Initial Login URI", name='initiateLoginUri',style='green'),

                        VSplit([
                                self.getTitledText("Request URIs", name='requestUris',style='green'),
                                Button("+", handler=self.myparent.show_again,left_symbol='[',right_symbol=']',width=3,)
                            ]) ,  

                            Label(text="Dropdown 3",style='blue'),

                        VSplit([
                                self.getTitledText("Allowed ACRs", name='clientSecret',style='green'),
                                Button("+", handler=self.myparent.show_again,left_symbol='[',right_symbol=']',width=3,)
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
                        Button("Save", handler=self.myparent.show_again,left_symbol='(',right_symbol=')',)

                        ],width=D()
                    )
    
        self.oauth_dialog_nav = list(self.oauth_tabs['clients'].keys())[0]

    def getTitledText(self, title, name, value='', height=1, jans_help='', width=None,style='',data=[]):
        ### Custom getTitledText (i didnt want to mess with the old one)
        ### search for getTitledText widget name in `data`
        ### data is Dict

        if name in self.data.keys() :
            if type(self.data[name]) is not list :
                value= str(self.data[name])
            else : 
                value= str('\n'.join(self.data[name]))      
# 
        multiline = height > 1
        ta = TextArea(text=value, multiline=multiline,style="class:titledtext")
        ta.window.jans_name = name
        ta.window.jans_help = jans_help
        li,cd,width = self.myparent.handle_long_string(title,[1]*height,ta)

        return VSplit([Label(text=li, width=width,style=style), cd],  padding=1)


    def getTitledCheckBox(self, title, name, values,style=''):  

        value = ''
        if name in self.data.keys() :
            if type(self.data[name]) is not list :
                value= str(self.data[name])
    
        with open('./checkbox.txt', 'a') as f:
            f.write("Name : {} , value = {}".format(name,value))
            f.write("\n**********\n")


        cb = CheckboxList(values=[(o,o) for o in values],default_values=value)
        cb.window.jans_name = name
        li,cd,width = self.myparent.handle_long_string(title,values,cb)

        return VSplit([Label(text=li, width=width,style=style,wrap_lines=False), cd])

    def getTitledRadioButton(self, title, name, values,style=''):
        value = ''
        if name in self.data.keys() :
            if type(self.data[name]) is not list :
                value= str(self.data[name])

        # if value == 'False' :
        #     value = ''
        # elif  value == 'True':

        with open('./radio.txt', 'a') as f:
            f.write("Name : {} , value = {}".format(name,value))
            f.write("\n**********\n")

        rl = RadioList(values=[(option, option) for option in values],default=value)
        rl.window.jans_name = name
        li,rl2,width = self.myparent.handle_long_string(title,values,rl)
        
        return VSplit([Label(text=li, width=width,style=style), rl2],)



    def client_dialog_nav_selection_changed(self, selection):
        self.oauth_dialog_nav = selection

    def __pt_container__(self):
        return self.dialog




