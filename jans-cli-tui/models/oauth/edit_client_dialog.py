from typing import OrderedDict

from prompt_toolkit.layout.dimension import D
from prompt_toolkit.layout.containers import (
    HSplit,
    VSplit,
    DynamicContainer,
    Window
)
from prompt_toolkit.widgets import (
    Box,
    Button,
    Label,
)

from cli import config_cli
from static import DialogResult
from wui_components.jans_dialog_with_nav import JansDialogWithNav
from wui_components.jans_side_nav_bar import JansSideNavBar
from wui_components.jans_cli_dialog import JansGDialog
from wui_components.jans_drop_down import DropDownWidget
from wui_components.jans_data_picker import DateSelectWidget
from utils import DialogUtils

class EditClientDialog(JansGDialog, DialogUtils):
    def __init__(self, parent, title, data, buttons=[], save_handler=None):
        super().__init__(parent, title, buttons)
        self.save_handler = save_handler
        self.data = data
        self.prepare_tabs()

        def save():

            self.data = self.make_data_from_dialog()
            self.data['disabled'] = not self.data['disabled']
            for list_key in ('redirectUris', 'scopes', 'postLogoutRedirectUris'):
                if self.data[list_key]:
                    self.data[list_key] = self.data[list_key].splitlines()

            if 'accessTokenAsJwt' in self.data:
                self.data['accessTokenAsJwt'] = self.data['accessTokenAsJwt'] == 'jwt'

            cfr = self.check_required_fields()
            self.myparent.logger.debug("CFR: "+str(cfr))
            if not cfr:
                return

            self.myparent.logger.debug("handler: "+str(save_handler))
            close_me = True
            if save_handler:
                close_me = self.save_handler(self)
            if close_me:
                self.future.set_result(DialogResult.ACCEPT)

        def cancel():
            self.future.set_result(DialogResult.CANCEL)

        self.side_nav_bar = JansSideNavBar(myparent=self.myparent,
            entries=list(self.tabs.keys()),
            selection_changed=(self.client_dialog_nav_selection_changed) ,
            select=0,
            entries_color='#2600ff')

        self.dialog = JansDialogWithNav(
            title=title,
            navbar=DynamicContainer(lambda:self.side_nav_bar),
            content=DynamicContainer(lambda: self.tabs[self.left_nav]),
             button_functions=[
                (save, "Save"),
                (cancel, "Cancel")
            ],
            height=self.myparent.dialog_height,
            width=self.myparent.dialog_width,
                   )

    def prepare_tabs(self):

        
        schema = self.myparent.cli_object.get_schema_from_reference('#/components/schemas/Client')

        # self.data['expirationDate'] = 1331856000000  ## to test widget

        self.tabs = OrderedDict()

        self.tabs['Basic'] = HSplit([
                        self.myparent.getTitledText(
                            title ="Client_ID",
                             name='inum',
                              value=self.data.get('inum',''),
                               jans_help=self.myparent.get_help_from_schema(schema, 'inum'),
                                read_only=True,
                                 style='green'),
                        self.myparent.getTitledCheckBox("Active", name='disabled', checked= not self.data.get('disabled'), jans_help=self.myparent.get_help_from_schema(schema, 'disabled'), style='green'),
                        self.myparent.getTitledText("Client Name", name='displayName', value=self.data.get('displayName',''), jans_help=self.myparent.get_help_from_schema(schema, 'displayName'), style='green'),
                        self.myparent.getTitledText("Client Secret", name='clientSecret', value=self.data.get('clientSecret',''), jans_help=self.myparent.get_help_from_schema(schema, 'clientSecret'), style='green'),
                        self.myparent.getTitledText("Description", name='description', value=self.data.get('description',''), style='green'),
                        self.myparent.getTitledWidget(
                                "Authn Method token endpoint",
                                name='tokenEndpointAuthMethodsSupported',
                                widget=DropDownWidget(
                                    values=[('client_secret_basic', 'client_secret_basic'), ('client_secret_post', 'client_secret_post'), ('client_secret_jwt', 'client_secret_jwt'), ('private_key_jwt', 'private_key_jwt')],
                                    value=self.data.get('tokenEndpointAuthMethodsSupported')
                                    ),
                                jans_help=self.myparent.get_help_from_schema(schema, 'tokenEndpointAuthMethodsSupported'),
                                style='green'
                                ),
                        self.myparent.getTitledRadioButton(
                                "Subject Type", 
                                name='subjectType', 
                                values=[('public', 'Public'),('pairwise', 'Pairwise')], 
                                current_value=self.data.get('subjectType'), 
                                jans_help=self.myparent.get_help_from_schema(schema, 'subjectType'),
                                style='green'),
                        self.myparent.getTitledCheckBoxList(
                                "Grant", 
                                name='grantTypes', 
                                values=[('authorization_code', 'Authorization Code'), ('refresh_token', 'Refresh Token'), ('urn:ietf:params:oauth:grant-type:uma-ticket', 'UMA Ticket'), ('client_credentials', 'Client Credentials'), ('password', 'Password'), ('implicit', 'Implicit')],
                                current_values=self.data.get('grantTypes', []), 
                                jans_help=self.myparent.get_help_from_schema(schema, 'grantTypes'),
                                style='green'),
                        self.myparent.getTitledCheckBoxList(
                                "Response Types", 
                                name='responseTypes', 
                                values=['code', 'token', 'id_token'], 
                                current_values=self.data.get('responseTypes', []), 
                                jans_help=self.myparent.get_help_from_schema(schema, 'responseTypes'),
                                style='green'),
                        self.myparent.getTitledCheckBox("Supress Authorization",
                        name='dynamicRegistrationPersistClientAuthorizations',
                        checked=self.data.get('dynamicRegistrationPersistClientAuthorizations'),
                        style='green'),
                        self.myparent.getTitledRadioButton("Application Type", name='applicationType', values=['native','web'], current_value=self.data.get('applicationType'), style='green'),
                        self.myparent.getTitledText("Redirect Uris", name='redirectUris', value='\n'.join(self.data.get('redirectUris', [])), height=3, style='class:required-field'),
                        self.myparent.getTitledText("Redirect Regex", name='redirectUrisRegex', value=self.data.get('redirectUrisRegex', ''), style='green'), 
                        self.myparent.getTitledText("Scopes",
                         name='scopes',
                          value='\n'.join(self.data.get('scopes', [])),
                           height=3, 
                           style='green'),

                        ],width=D(),
                    )
        
        self.tabs['Tokens'] = HSplit([
                        self.myparent.getTitledRadioButton(
                            "Access Token Type",
                            name='accessTokenAsJwt',
                            values=[('jwt', 'JWT'), ('reference', 'Reference')],
                            current_value= 'jwt' if self.data.get('accessTokenAsJwt') else 'reference',
                            jans_help=self.myparent.get_help_from_schema(schema, 'accessTokenAsJwt'),
                            style='green'),

                        self.myparent.getTitledCheckBox(
                            "Incliude Claims in id_token",
                            name='includeClaimsInIdToken',
                            checked=self.data.get('includeClaimsInIdToken'),
                            style='green'),
                        self.myparent.getTitledCheckBox(
                            "Run introspection script before JWT access token creation",
                            name='runIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims',
                            checked=self.data.get('runIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims'),
                            style='green'),
                        self.myparent.getTitledText(
                            title="Token binding confirmation  method for id_token",
                            name='idTokenTokenBindingCnf',
                            value=self.data.get('idTokenTokenBindingCnf',''),
                            style='green'),
                        self.myparent.getTitledText(
                            title="Access token additional audiences",
                            name='additionalAudience',
                            value=self.data.get('additionalAudience',''),
                            style='green'),
                        VSplit([
                                        Button("+", handler=self.myparent.show_again,left_symbol='[',right_symbol=']',width=3)
                        ]),
                        self.myparent.getTitledText("Access token lifetime", name='accessTokenLifetime', value=self.data.get('accessTokenLifetime',''),style='green'),
                        self.myparent.getTitledText("Refresh token lifetime", name='refreshTokenLifetime', value=self.data.get('refreshTokenLifetime',''),style='green'),
                        self.myparent.getTitledText("Defult max authn age", name='defaultMaxAge', value=self.data.get('defaultMaxAge',''),style='green'),

                    ],width=D())
        
        self.tabs['Logout'] = HSplit([
            
                        self.myparent.getTitledText("Front channel logout URI", name='frontChannelLogoutUri', value=self.data.get('frontChannelLogoutUri',''), style='green'),
                        self.myparent.getTitledText("Post logout redirect URIs", name='postLogoutRedirectUris', value='\n'.join(self.data.get('postLogoutRedirectUris',[])), height=3, style='green'),
                        self.myparent.getTitledText("Back channel logout URI", name='backchannelLogoutUri', value=self.data.get('backchannelLogoutUri',''),style='green'),
                        self.myparent.getTitledCheckBox("Back channel logout session required", name='backchannelLogoutSessionRequired', checked=self.data.get('backchannelLogoutSessionRequired'),style='green'),
                        self.myparent.getTitledCheckBox("Front channel logout session required", name='frontChannelLogoutSessionRequired', checked=self.data.get('frontChannelLogoutSessionRequired'),style='green'),

                        ],width=D()
                    )
        
        self.tabs['SoftwareInfo'] =  HSplit([
            #self.myparent.getTitledText(title ="Client URI", name='clientUri', value=self.data.get('clientUri',''),style='green'),
            #self.myparent.getTitledText(title ="Policy URI", name='policyUri', value=self.data.get('policyUri',''),style='green'),
            #self.myparent.getTitledText(title ="Logo URI", name='logoUri', value=self.data.get('logoUri',''),style='green'),
            #self.myparent.getTitledText(title ="Term of service URI", name='tosUri', value=self.data.get('tosUri',''),style='green'),
            self.myparent.getTitledText(title ="Contacts", name='contacts', value=self.data.get('contacts',''),style='green'),
            VSplit([
                            Button("+", handler=self.myparent.show_again,left_symbol='[',right_symbol=']',width=3,)
            ]),
            self.myparent.getTitledText(title ="Authorized JS origins", name='authorizedOrigins', value=self.data.get('authorizedOrigins',''),style='green'),
            VSplit([
                            Button("+", handler=self.myparent.show_again,left_symbol='[',right_symbol=']',width=3)
            ]),
            self.myparent.getTitledText(title ="Software id", name='softwareId', value=self.data.get('softwareId',''),style='green'),
            self.myparent.getTitledText(title ="Software version", name='softwareVersion', value=self.data.get('softwareVersion',''), style='green'),
            self.myparent.getTitledText(title ="Software statement", name='softwareStatement', value=self.data.get('softwareStatement',''), style='green'),
            
        ],width=D())

        self.tabs['CIBA/PAR/UMA'] = HSplit([
                        Label(text="CIBA",style='bold'),
                        self.myparent.getTitledRadioButton("Token delivery method", name='backchannelTokenDeliveryMode', current_value=self.data.get('backchannelTokenDeliveryMode'), values=['poll','push', 'ping'],style='green'),
                        self.myparent.getTitledText(title ="Client notification endpoint", name='backchannelClientNotificationEndpoint', value=self.data.get('backchannelClientNotificationEndpoint',''),style='green'),
                        self.myparent.getTitledCheckBox("Require user code param", name='backchannelUserCodeParameterSupported', checked=self.data.get('backchannelUserCodeParameterSupported'),style='green'),
                        
                        Label(text="PAR",style='bold'),
                         
                        self.myparent.getTitledText(title ="Request lifetime", name='parLifetime', value=self.data.get('parLifetime',''),style='green'),
                        self.myparent.getTitledCheckBox("Request PAR", name='sessionIdRequestParameterEnabled', checked=self.data.get('sessionIdRequestParameterEnabled'),style='green'),
                        
                        Label("UMA",style='bold'),
                        self.myparent.getTitledRadioButton("PRT token type", name='applicationType!', values=['JWT', 'Reference'], current_value=self.data.get('applicationType!'),style='green'),
                        self.myparent.getTitledText(title ="Claims redirect URI", name='claimRedirectUris', value=self.data.get('claimRedirectUris',''),style='green'),


                        self.myparent.getTitledText("RPT Mofification Script",
                         name='rptClaimsScripts',
                          value='\n'.join(self.data.get('rptClaimsScripts', [])), 
                          height=3,
                           style='green'),
         
                        self.myparent.getTitledText("Claims Gathering Script",
                         name='claimRedirectUris',
                          value='\n'.join(self.data.get('claimRedirectUris', [])), 
                          height=3,
                           style='green'),

                     
                        Label(text="tabel",style='blue'),  ## TODO with Jans VerticalNav  
              
                        ]
                            )
        
        self.tabs['Encryption/Signing'] = HSplit([
                        self.myparent.getTitledText(title ="Client JWKS URI", name='jwksUri', value=self.data.get('jwksUri',''),style='green'),
                        self.myparent.getTitledText(title ="Client JWKS", name='jwks', value=self.data.get('jwks',''),style='green'),
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
        
        self.tabs['Advanced Client Properties'] = HSplit([

                        self.myparent.getTitledCheckBox("Default Prompt login", name='defaultPromptLogin', checked=self.data.get('defaultPromptLogin'),style='green'),
                        VSplit([
                                self.myparent.getTitledCheckBox("Persist Authorizations", name='persistClientAuthorizations', checked=self.data.get('persistClientAuthorizations'),style='green'),
                                # self.myparent.getTitledCheckBox("Keep expired", name='Supress', checked=self.data.get('Supress'),style='green'),
                            ]) ,                       
                        self.myparent.getTitledCheckBox("Allow spontaneos scopes", name='allowSpontaneousScopes', checked=self.data.get('allowSpontaneousScopes'),style='green'),

                        self.myparent.getTitledText("spontaneos scopes validation regex", name='spontaneousScopes', value=self.data.get('spontaneousScopes',''),style='green'),
                        VSplit([
                                Label(text="Spontaneous Scopes",style='green'),
                                Button("view current", handler=self.myparent.show_again,left_symbol='',right_symbol='',)
                            
                            ]) ,  
                        self.myparent.getTitledText("Initial Login URI", name='initiateLoginUri', value=self.data.get('initiateLoginUri',''),style='green'),

                        VSplit([
                                self.myparent.getTitledText("Request URIs", name='requestUris', value=self.data.get('requestUris',''),style='green'),
                                Button("+", handler=self.myparent.show_again,left_symbol='[',right_symbol=']',width=3,)
                            ]) ,  

                        self.myparent.getTitledText("Default  ACR",
                         name='authorizedAcrValues',
                          value='\n'.join(self.data.get('authorizedAcrValues', [])), 
                          height=3,
                           style='green'),

                        VSplit([
                                # self.myparent.getTitledText("Allowed ACRs", name='clientSecret', value=self.data.get('clientSecret',''),style='green'),
                                Button("+", handler=self.myparent.show_again,left_symbol='[',right_symbol=']',width=3,)
                            ]) , 
                        self.myparent.getTitledText("TLS Subject DN", name='x5c', value=self.data.get('x5c',''),style='green'),

                        self.myparent.getTitledWidget(
                                "Client Experiation Date",
                                name='expirationDate',
                                widget=DateSelectWidget(
                                    value=self.data.get('expirationDate', "2022-11-05T14:45:26")
                                   ),
                                jans_help=self.myparent.get_help_from_schema(schema, 'expirationDate'),
                                style='green'
                                ),

   
                        ],width=D()
        )



        self.tabs['Client Scripts'] = HSplit([


            self.myparent.getTitledText("Spontaneous Scopes",
                name='spontaneousScopes',
                value='\n'.join(self.data.get('spontaneousScopes', [])), 
                height=3,
                style='green'),

            # --------------------------------------------------------------------------------------# 
            self.myparent.getTitledText("Update Token",
                name='updateTokenScriptDns',
                value='\n'.join(self.data.get('updateTokenScriptDns', [])), 
                height=3,
                style='green'),

            # --------------------------------------------------------------------------------------# 
            self.myparent.getTitledText("Post Authn",
                name='postAuthnScripts',
                value='\n'.join(self.data.get('postAuthnScripts', [])), 
                height=3,
                style='green'),

            # --------------------------------------------------------------------------------------# 
            self.myparent.getTitledText("Introspection",
                name='introspectionScripts',
                value='\n'.join(self.data.get('introspectionScripts', [])), 
                height=3,
                style='green'),

            # --------------------------------------------------------------------------------------# 
            self.myparent.getTitledText("Password Grant",
                name='dynamicRegistrationAllowedPasswordGrantScopes',
                value='\n'.join(self.data.get('dynamicRegistrationAllowedPasswordGrantScopes', [])), 
                height=3,
                style='green'),
                
            # --------------------------------------------------------------------------------------# 
            self.myparent.getTitledText("OAuth Consent",
                name='consentGatheringScripts',
                value='\n'.join(self.data.get('consentGatheringScripts', [])), 
                height=3,
                style='green'),


                        ]
                        )

        self.left_nav = list(self.tabs.keys())[0]


    def client_dialog_nav_selection_changed(self, selection):
        self.left_nav = selection

    def __pt_container__(self):
        return self.dialog


    def client_dialog_nav_selection_changed(self, selection):
        self.left_nav = selection

    def __pt_container__(self):
        return self.dialog






