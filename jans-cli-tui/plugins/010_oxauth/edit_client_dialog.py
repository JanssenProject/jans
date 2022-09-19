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
from prompt_toolkit.application.current import get_app

from cli import config_cli
from static import DialogResult
from wui_components.jans_dialog_with_nav import JansDialogWithNav
from wui_components.jans_side_nav_bar import JansSideNavBar
from wui_components.jans_cli_dialog import JansGDialog
from wui_components.jans_drop_down import DropDownWidget
from wui_components.jans_data_picker import DateSelectWidget
from utils import DialogUtils
from wui_components.jans_vetrical_nav import JansVerticalNav
from view_uma_dialog import ViewUMADialog
import threading

from multi_lang import _


class EditClientDialog(JansGDialog, DialogUtils):
    """The Main Client Dialog that contain every thing related to The Client
    """
    def __init__(self, parent, title, data, buttons=[], save_handler=None):
        """init for `EditClientDialog`, inherits from two diffrent classes `JansGDialog` and `DialogUtils`
            
        JansGDialog (dialog): This is the main dialog Class Widget for all Jans-cli-tui dialogs except custom dialogs like dialogs with navbar
        DialogUtils (methods): Responsable for all `make data from dialog` and `check required fields` in the form for any Edit or Add New
        
        Args:
            parent (widget): This is the parent widget for the dialog, to access `Pageup` and `Pagedown`
            title (str): The Main dialog title
            data (list): selected line data 
            button_functions (list, optional): Dialog main buttons with their handlers. Defaults to [].
            save_handler (method, optional): _description_. Defaults to None.
        """
        super().__init__(parent, title, buttons)
        self.save_handler = save_handler
        self.data = data
        self.prepare_tabs()

        def save():

            self.data = self.make_data_from_dialog()
            self.data['disabled'] = not self.data['disabled']
            for list_key in (
                            'redirectUris',
                            'scopes',
                            'postLogoutRedirectUris',
                            'contacts',
                            'authorizedOrigins',
                            'rptClaimsScripts',
                            'claimRedirectUris',
                             ):
                if self.data[list_key]:
                    self.data[list_key] = self.data[list_key].splitlines()

            if 'accessTokenAsJwt' in self.data:
                self.data['accessTokenAsJwt'] = self.data['accessTokenAsJwt'] == 'jwt'

            if 'rptAsJwt' in self.data:
                self.data['rptAsJwt'] = self.data['rptAsJwt'] == 'jwt'

            cfr = self.check_required_fields()
            self.myparent.logger.debug('CFR: '+str(cfr))
            if not cfr:
                return

            for ditem in self.drop_down_select_first:
                if ditem in self.data and self.data[ditem] is None:
                    self.data.pop(ditem)

            self.myparent.logger.debug('DATA: '+str(self.data))

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
            navbar=self.side_nav_bar,
            content=DynamicContainer(lambda: self.tabs[self.left_nav]),
             button_functions=[
                (save, _("Save")),
                (cancel, _("Cancel"))
            ],
            height=self.myparent.dialog_height,
            width=self.myparent.dialog_width,
                   )
    
    def prepare_tabs(self):
        """Prepare the tabs for Edil Client Dialogs
        """

        schema = self.myparent.cli_object.get_schema_from_reference('#/components/schemas/Client')

        self.tabs = OrderedDict()

        self.tabs['Basic'] = HSplit([
                        self.myparent.getTitledText(
                                _("Client_ID"),
                                name='inum',
                                value=self.data.get('inum',''),
                                jans_help=self.myparent.get_help_from_schema(schema, 'inum'),
                                read_only=True,
                                style='green'),

                        self.myparent.getTitledCheckBox(_("Active"), name='disabled', checked= not self.data.get('disabled'), jans_help=self.myparent.get_help_from_schema(schema, 'disabled'), style='green'),
                        self.myparent.getTitledText(_("Client Name"), name='displayName', value=self.data.get('displayName',''), jans_help=self.myparent.get_help_from_schema(schema, 'displayName'), style='green'),
                        self.myparent.getTitledText(_("Client Secret"), name='clientSecret', value=self.data.get('clientSecret',''), jans_help=self.myparent.get_help_from_schema(schema, 'clientSecret'), style='green'),
                        self.myparent.getTitledText(_("Description"), name='description', value=self.data.get('description',''), style='green'),
                        self.myparent.getTitledWidget(
                                _("Authn Method token endpoint"),
                                name='tokenEndpointAuthMethodsSupported',
                                widget=DropDownWidget(
                                    values=[('client_secret_basic', 'client_secret_basic'), ('client_secret_post', 'client_secret_post'), ('client_secret_jwt', 'client_secret_jwt'), ('private_key_jwt', 'private_key_jwt')],
                                    value=self.data.get('tokenEndpointAuthMethodsSupported')
                                    ),
                                jans_help=self.myparent.get_help_from_schema(schema, 'tokenEndpointAuthMethodsSupported'),
                                style='green'
                                ),
                        self.myparent.getTitledRadioButton(
                                _("Subject Type"), 
                                name='subjectType', 
                                values=[('public', 'Public'),('pairwise', 'Pairwise')],
                                current_value=self.data.get('subjectType'),
                                jans_help=self.myparent.get_help_from_schema(schema, 'subjectType'),
                                style='green'),

                        self.myparent.getTitledText(_("Sector Identifier URI"), name='sectorIdentifierUri', value=self.data.get('sectorIdentifierUri',''), jans_help=self.myparent.get_help_from_schema(schema, 'sectorIdentifierUri'), style='green'),
                                
                        self.myparent.getTitledCheckBoxList(
                                _("Grant"), 
                                name='grantTypes', 
                                values=[('authorization_code', 'Authorization Code'), ('refresh_token', 'Refresh Token'), ('urn:ietf:params:oauth:grant-type:uma-ticket', 'UMA Ticket'), ('client_credentials', 'Client Credentials'), ('password', 'Password'), ('implicit', 'Implicit')],
                                current_values=self.data.get('grantTypes', []), 
                                jans_help=self.myparent.get_help_from_schema(schema, 'grantTypes'),
                                style='green'),
                        self.myparent.getTitledCheckBoxList(
                                _("Response Types"), 
                                name='responseTypes', 
                                values=['code', 'token', 'id_token'], 
                                current_values=self.data.get('responseTypes', []), 
                                jans_help=self.myparent.get_help_from_schema(schema, 'responseTypes'),
                                style='green'),
                        self.myparent.getTitledCheckBox(_("Supress Authorization"),
                        name='dynamicRegistrationPersistClientAuthorizations',
                        checked=self.data.get('dynamicRegistrationPersistClientAuthorizations'),
                        style='green'),
                        self.myparent.getTitledRadioButton(_("Application Type"), name='applicationType', values=['native','web'], current_value=self.data.get('applicationType'), style='green'),
                        self.myparent.getTitledText(_("Redirect Uris"), name='redirectUris', value='\n'.join(self.data.get('redirectUris', [])), height=3, style='class:required-field'),
                        self.myparent.getTitledText(_("Redirect Regex"), name='redirectUrisRegex', value=self.data.get('redirectUrisRegex', ''), style='green'), 
                        self.myparent.getTitledText(_("Scopes"),
                            name='scopes',
                            value='\n'.join(self.data.get('scopes', [])),
                            height=3, 
                            style='green'),

                        ],width=D(),
                    )

        self.tabs['Tokens'] = HSplit([
                        self.myparent.getTitledRadioButton(
                            _("Access Token Type"),
                            name='accessTokenAsJwt',
                            values=[('jwt', 'JWT'), ('reference', 'Reference')],
                            current_value= 'jwt' if self.data.get('accessTokenAsJwt') else 'reference',
                            jans_help=self.myparent.get_help_from_schema(schema, 'accessTokenAsJwt'),
                            style='green'),

                        self.myparent.getTitledCheckBox(
                            _("Incliude Claims in id_token"),
                            name='includeClaimsInIdToken',
                            checked=self.data.get('includeClaimsInIdToken'),
                            style='green'),
                        self.myparent.getTitledCheckBox(
                            _("Run introspection script before JWT access token creation"),
                            name='runIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims',
                            checked=self.data.get('runIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims'),
                            style='green'),
                        self.myparent.getTitledText(
                            title=_("Token binding confirmation  method for id_token"),
                            name='idTokenTokenBindingCnf',
                            value=self.data.get('idTokenTokenBindingCnf',''),
                            style='green'),
                        self.myparent.getTitledText(
                            title=_("Access token additional audiences"),
                            name='additionalAudience',
                            value=self.data.get('additionalAudience',''),
                            style='green'),
                        VSplit([
                                Label(text=_("audiences"),style='bold',width=len(_("audiences")*2)), ## TODO
                                Button("+", handler=self.myparent.show_again,left_symbol='[',right_symbol=']',width=3)
                        ]),
                        self.myparent.getTitledText(_("Access token lifetime"), name='accessTokenLifetime', value=self.data.get('accessTokenLifetime',''),style='green'),
                        self.myparent.getTitledText(_("Refresh token lifetime"), name='refreshTokenLifetime', value=self.data.get('refreshTokenLifetime',''),style='green'),
                        self.myparent.getTitledText(_("Defult max authn age"), name='defaultMaxAge', value=self.data.get('defaultMaxAge',''),style='green'),

                    ],width=D())

        self.tabs['Logout'] = HSplit([
            
                        self.myparent.getTitledText(_("Front channel logout URI"), name='frontChannelLogoutUri', value=self.data.get('frontChannelLogoutUri',''), style='green'),
                        self.myparent.getTitledText(_("Post logout redirect URIs"), name='postLogoutRedirectUris', value='\n'.join(self.data.get('postLogoutRedirectUris',[])), height=3, style='green'),
                        self.myparent.getTitledText(_("Back channel logout URI"), name='backchannelLogoutUri', value=self.data.get('backchannelLogoutUri',''),style='green'),
                        self.myparent.getTitledCheckBox(_("Back channel logout session required"), name='backchannelLogoutSessionRequired', checked=self.data.get('backchannelLogoutSessionRequired'),style='green'),
                        self.myparent.getTitledCheckBox(_("Front channel logout session required"), name='frontChannelLogoutSessionRequired', checked=self.data.get('frontChannelLogoutSessionRequired'),style='green'),

                        ],width=D()
                    )

        self.tabs['SoftwareInfo'] =  HSplit([
            #self.myparent.getTitledText(title =_("Client URI"), name='clientUri', value=self.data.get('clientUri',''),style='green'),
            #self.myparent.getTitledText(title =_("Policy URI"), name='policyUri', value=self.data.get('policyUri',''),style='green'),
            #self.myparent.getTitledText(title =_("Logo URI"), name='logoUri', value=self.data.get('logoUri',''),style='green'),
            #self.myparent.getTitledText(title =_("Term of service URI"), name='tosUri', value=self.data.get('tosUri',''),style='green'),

            self.myparent.getTitledText(_("Contacts"),              ### height =3 insted of the <+> button
                            name='contacts',
                            value='\n'.join(self.data.get('contacts', [])), 
                            height=3,
                            style='green'),

            self.myparent.getTitledText(_("Authorized JS origins"),  ### height =3 insted of the <+> button
                            name='authorizedOrigins',
                            value='\n'.join(self.data.get('authorizedOrigins', [])), 
                            height=3,
                            style='green'),

            self.myparent.getTitledText(title =_("Software id"), name='softwareId', value=self.data.get('softwareId',''),style='green'),
            self.myparent.getTitledText(title =_("Software version"), name='softwareVersion', value=self.data.get('softwareVersion',''), style='green'),
            self.myparent.getTitledText(title =_("Software statement"), name='softwareStatement', value=self.data.get('softwareStatement',''), style='green'),
            
        ],width=D())


        self.uma_resources = HSplit([],width=D())
        self.resources = HSplit([
                    VSplit([
                        self.myparent.getButton(text=_("Get Resources"), name='oauth:Resources:get', jans_help=_("Retreive UMA Resources"), handler=self.oauth_get_uma_resources),
                        self.myparent.getTitledText(_("Search"), name='oauth:Resources:search', jans_help=_("Press enter to perform search"), accept_handler=self.search_uma_resources),
                    
                        ],
                        padding=3,
                        width=D(),
                        ),
                    DynamicContainer(lambda: self.uma_resources)
                    ])

        self.tabs['CIBA/PAR/UMA'] = HSplit([
                        Label(text=_("CIBA"),style='bold'),
                        self.myparent.getTitledRadioButton(_("Token delivery method"), name='backchannelTokenDeliveryMode', current_value=self.data.get('backchannelTokenDeliveryMode'), values=['poll','push', 'ping'],style='green'),
                        self.myparent.getTitledText(title =_("Client notification endpoint"), name='backchannelClientNotificationEndpoint', value=self.data.get('backchannelClientNotificationEndpoint',''),style='green'),
                        self.myparent.getTitledCheckBox(_("Require user code param"), name='backchannelUserCodeParameterSupported', checked=self.data.get('backchannelUserCodeParameterSupported'),style='green'),
                        
                        Label(text=_("PAR"),style='bold'),

                        self.myparent.getTitledText(title =_("Request lifetime"), name='parLifetime', value=self.data.get('parLifetime',''),style='green'),
                        self.myparent.getTitledCheckBox(_("Request PAR"), name='sessionIdRequestParameterEnabled',checked=self.data.get('sessionIdRequestParameterEnabled'),style='green'),

                        Label(_("UMA"), style='bold'),

                        self.myparent.getTitledRadioButton(
                            _("PRT token type"),
                            name='rptAsJwt!', 
                            values=[('jwt', 'JWT'), ('reference', 'Reference')], 
                            current_value='jwt' if self.data.get('rptAsJwt') else 'reference',
                            style='green'),

                        self.myparent.getTitledText(title =_("Claims redirect URI"), name='claimRedirectUris', value=self.data.get('claimRedirectUris',''),style='green'),

                        # self.myparent.getTitledText(_("RPT Mofification Script"),
                        #     name='rptClaimsScripts',
                        #     value='\n'.join(self.data.get('rptClaimsScripts', [])), 
                        #     height=3,
                        #     style='green'),

                        # self.myparent.getTitledText(_("Claims Gathering Script"),
                        #     name='claimRedirectUris',
                        #     value='\n'.join(self.data.get('claimRedirectUris', [])), 
                        #     height=3,
                        #     style='green'),


                        self.myparent.getTitledText(_("UMA Authorization Policies"),
                            name='umaAuthorizationPolicies',
                            value='\n'.join(self.data.get('umaAuthorizationPolicies', [])), 
                            height=3,
                            style='green'),
                            
                    self.resources,

                        ]
                            )


        encryption_signing = [
                        self.myparent.getTitledText(title =_("Client JWKS URI"), name='jwksUri', value=self.data.get('jwksUri',''),style='green'),
                        self.myparent.getTitledText(title =_("Client JWKS"), name='jwks', value=self.data.get('jwks',''),style='green'),
                        ]


        self.drop_down_select_first = []


        # keep this line until this issue is closed https://github.com/JanssenProject/jans/issues/2372
        self.myparent.cli_object.openid_configuration['access_token_singing_alg_values_supported'] = ['HS256', 'HS384', 'HS512', 'RS256', 'RS384', 'RS512', 'ES256', 'ES384', 'ES512', 'PS256', 'PS384', 'PS512']


        for title, swagger_key, openid_key  in (

                (_("ID Token Alg for Signing "), 'idTokenSignedResponseAlg', 'id_token_signing_alg_values_supported'),
                (_("ID Token Alg for Encryption"), 'idTokenEncryptedResponseAlg', 'id_token_encryption_alg_values_supported'),
                (_("ID Token Enc for Encryption"), 'idTokenEncryptedResponseEnc', 'id_token_encryption_enc_values_supported'),
                (_("Access Token Alg for Signing "), 'accessTokenSigningAlg', 'access_token_singing_alg_values_supported'), #?? openid key

                (_("User Info for Signing "), 'userInfoSignedResponseAlg', 'userinfo_signing_alg_values_supported'),
                (_("User Info Alg for Encryption"), 'userInfoEncryptedResponseAlg', 'userinfo_encryption_alg_values_supported'),
                (_("User Info Enc for Encryption"), 'userInfoEncryptedResponseEnc', 'userinfo_encryption_enc_values_supported'),

                (_("Request Object Alg for Signing "), 'requestObjectSigningAlg', 'request_object_signing_alg_values_supported'),
                (_("Request Object Alg for Encryption"), 'requestObjectEncryptionAlg', 'request_object_encryption_alg_values_supported'),
                (_("Request Object Enc for Encryption"), 'requestObjectEncryptionEnc', 'request_object_encryption_enc_values_supported'),
                ):

            self.drop_down_select_first.append(swagger_key)

            values = [ (alg, alg) for alg in self.myparent.cli_object.openid_configuration[openid_key] ]

            encryption_signing.append(self.myparent.getTitledWidget(
                                title,
                                name=swagger_key,
                                widget=DropDownWidget(
                                    values=values,
                                    value=self.data.get(swagger_key)
                                    ),
                                jans_help=self.myparent.get_help_from_schema(schema, swagger_key),
                                style='green'))

        self.tabs['Encryption/Signing'] = HSplit(encryption_signing)

        def allow_spontaneous_changed(cb):
            self.spontaneous_scopes.me.window.style = 'underline ' + (self.myparent.styles['textarea'] if cb.checked else self.myparent.styles['textarea-readonly'])
            self.spontaneous_scopes.me.text = ''
            self.spontaneous_scopes.me.read_only = not cb.checked

        self.spontaneous_scopes = self.myparent.getTitledText(
                    _("Spontaneos scopes validation regex"),
                    name='spontaneousScopes',
                    value=self.data.get('spontaneousScopes',''),
                    read_only=False if 'allowSpontaneousScopes' in self.data and self.data['allowSpontaneousScopes'] else True,
                    focusable=True,
                    style='green')


        self.tabs['Advanced Client Properties'] = HSplit([

                        self.myparent.getTitledCheckBox(_("Default Prompt login"), name='defaultPromptLogin', checked=self.data.get('defaultPromptLogin'), style='green'),
                        self.myparent.getTitledCheckBox(_("Persist Authorizations"), name='persistClientAuthorizations', checked=self.data.get('persistClientAuthorizations'), style='green'),
                        self.myparent.getTitledCheckBox(_("Allow spontaneos scopes"), name='allowSpontaneousScopes', checked=self.data.get('allowSpontaneousScopes'), on_selection_changed=allow_spontaneous_changed, style='green'),

                        self.spontaneous_scopes,


                        VSplit([   ## TODO what the functionality would be?
                                Label(text=_("Spontaneous scopes"),style='bold',width=len(_("Spontaneous scopes")*2)), ## TODO
                                Button(_("View current"), handler=self.myparent.show_again,left_symbol='<',right_symbol='>',width=len(_("View current"))+2)
                        ]),

                        self.myparent.getTitledText(_("Initial Login URI"), name='initiateLoginUri', value=self.data.get('initiateLoginUri',''),style='green'),

                        self.myparent.getTitledText(_("Request URIs"), ### height =3 insted of the <+> button
                            name='requestUris',
                            value='\n'.join(self.data.get('requestUris', [])),
                            height=3,
                            style='green'),

                        self.myparent.getTitledText(_("Default  ACR"), ### height =3 >> "the type is array" cant be dropdown
                            name='defaultAcrValues',
                            value='\n'.join(self.data.get('defaultAcrValues', [])), 
                            height=3,
                            style='green'),

                        self.myparent.getTitledText(_("Allowed  ACR"), ### height =3 insted of the <+> button
                            name='authorizedAcrValues',
                            value='\n'.join(self.data.get('authorizedAcrValues', [])), 
                            height=3,
                            style='green'),



                        self.myparent.getTitledText(_("TLS Subject DN"), name='x5c', value=self.data.get('x5c',''),style='green'),

                        self.myparent.getTitledWidget(
                                _("Client Expiration Date"),
                                name='expirationDate',
                                widget=DateSelectWidget(
                                    value=self.data.get('expirationDate', ''),parent=self
                                   ),
                                jans_help=self.myparent.get_help_from_schema(schema, 'expirationDate'),
                                style='green'
                                ),

                        ],width=D()
        )

        self.tabs['Client Scripts'] = HSplit([


            self.myparent.getTitledText(_("Spontaneous Scopes"),
                name='spontaneousScopes',
                value='\n'.join(self.data.get('spontaneousScopes', [])), 
                height=3,
                style='green'),

            # --------------------------------------------------------------------------------------# 
            self.myparent.getTitledText(_("Update Token"),
                name='updateTokenScriptDns',
                value='\n'.join(self.data.get('updateTokenScriptDns', [])), 
                height=3,
                style='green'),

            # --------------------------------------------------------------------------------------# 
            self.myparent.getTitledText(_("Post Authn"),
                name='postAuthnScripts',
                value='\n'.join(self.data.get('postAuthnScripts', [])), 
                height=3,
                style='green'),

            # --------------------------------------------------------------------------------------# 
            self.myparent.getTitledText(_("Introspection"),
                name='introspectionScripts',
                value='\n'.join(self.data.get('introspectionScripts', [])), 
                height=3,
                style='green'),

            # --------------------------------------------------------------------------------------# 
            self.myparent.getTitledText(_("Password Grant"),
                name='dynamicRegistrationAllowedPasswordGrantScopes',
                value='\n'.join(self.data.get('dynamicRegistrationAllowedPasswordGrantScopes', [])), 
                height=3,
                style='green'),
                
            # --------------------------------------------------------------------------------------# 
            self.myparent.getTitledText(_("OAuth Consent"),
                name='consentGatheringScripts',
                value='\n'.join(self.data.get('consentGatheringScripts', [])), 
                height=3,
                style='green'),


                        ]
                        )

        self.left_nav = list(self.tabs.keys())[0]



    def oauth_get_uma_resources(self):
        """Method to get the clients data from server
        """
        t = threading.Thread(target=self.oauth_update_uma_resources, daemon=True)
        t.start()


    def search_uma_resources(self, tbuffer):
        if not len(tbuffer.text) > 2:
            self.myparent.show_message(_("Error!"), _("Search string should be at least three characters"))
            return

        t = threading.Thread(target=self.oauth_update_uma_resources, args=(tbuffer.text,), daemon=True)
        t.start()


    def oauth_update_uma_resources (self, pattern=''): 
        """update the current uma_resources  data to server

        Args:
            pattern (str, optional): endpoint arguments for the uma_resources data. Defaults to ''.
        """
        endpoint_args ='limit:10'
        if pattern:
            endpoint_args +=',pattern:'+pattern

        try :
            rsponse = self.myparent.cli_object.process_command_by_id(
                        operation_id='get-oauth-uma-resources',
                        url_suffix='',
                        endpoint_args=endpoint_args,
                        data_fn=None,
                        data={}
                        )

        except Exception as e:
            self.myparent.show_message(_("Error getting clients"), str(e))
            return

        if rsponse.status_code not in (200, 201):
            self.myparent.show_message(_("Error getting clients"), str(rsponse.text))
            return

        try:
            result = rsponse.json()
        except Exception:
            self.myparent.show_message(_("Error getting clients"), str(rsponse.text))
            #press_tab
            return

        data =[]

        for d in result:
            data.append(
                [
                str(d['id']),
                str(d.get('description', '')),
                str(d.get('scopes', [''])[0] )
                ]
            )
        

        if data :
            self.uma_resources = HSplit([
                            JansVerticalNav(
                                    myparent=self.myparent,
                                    headers=['id', 'Description', 'Scopes'],
                                    preferred_size= [36,0,0],
                                    data=data,
                                    on_enter=self.view_uma_resources,
                                    on_display=self.myparent.data_display_dialog,
                                    # selection_changed=self.data_selection_changed,
                                    selectes=0,
                                    headerColor='green',
                                    entriesColor='blue',
                                    all_data=result,
                            ),
            ])
        
            # self.uma_result = result
            
            # return result
            get_app().invalidate()
        else:
            self.myparent.show_message(_("Oops"), _("No matching result"))  
            

    def client_dialog_nav_selection_changed(self, selection):
        self.left_nav = selection

    def view_uma_resources(self, **params):
        
        selected_line_data = params['data']    ##self.uma_result 
        title = _("Edit user Data (Clients)")

        dialog = ViewUMADialog(self.myparent, title=title, data=selected_line_data, save_handler=self.save_client)
        
        self.myparent.show_jans_dialog(dialog)

    def save_client(self, dialog):
        pass
    def __pt_container__(self):
        return self.dialog

