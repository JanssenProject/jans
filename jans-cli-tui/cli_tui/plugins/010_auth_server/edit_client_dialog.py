import copy
import json
import asyncio
import threading

from collections import OrderedDict
from functools import partial
from typing import Any, Optional, Sequence, Callable

from prompt_toolkit.layout.dimension import D
from prompt_toolkit.layout.containers import (
    HSplit,
    VSplit,
    DynamicContainer,
    HorizontalAlign,
)
from prompt_toolkit.widgets import (
    Button,
    Label,
    TextArea,
    Dialog,
    CheckboxList,
    Frame
)
from prompt_toolkit.layout import Window
from prompt_toolkit.filters import Condition
from prompt_toolkit.lexers import PygmentsLexer, DynamicLexer
from prompt_toolkit.application.current import get_app
from prompt_toolkit.buffer import Buffer
from prompt_toolkit.formatted_text import AnyFormattedText, HTML
from prompt_toolkit.eventloop import get_event_loop

from utils.static import DialogResult, cli_style, common_strings, ISOFORMAT
from utils.multi_lang import _
from utils.utils import common_data, fromisoformat, DialogUtils
from wui_components.jans_dialog_with_nav import JansDialogWithNav
from wui_components.jans_side_nav_bar import JansSideNavBar
from wui_components.jans_cli_dialog import JansGDialog
from wui_components.jans_drop_down import DropDownWidget
from wui_components.jans_date_picker import DateSelectWidget
from wui_components.jans_vetrical_nav import JansVerticalNav
from wui_components.jans_label_container import JansLabelContainer
from wui_components.jans_label_widget import JansLabelWidget


from view_uma_dialog import ViewUMADialog


ERROR_GETTING_CLIENTS = _("Error getting clients")
ATTRIBUTE_SCHEMA_PATH = '#/components/schemas/ClientAttributes'
URL_SUFFIX_FORMATTER = 'inum:{}'
ATTRIBUTE_ALG_PROPERTIES = (
    'introspectionSignedResponseAlg',
    'introspectionEncryptedResponseAlg',
    'introspectionEncryptedResponseEnc',
    'txTokenSignedResponseAlg',
    'txTokenEncryptedResponseAlg',
    'txTokenEncryptedResponseEnc',
    )
APP = get_app()


def get_scope_by_inum(inum: str) -> dict:
    for scope in common_data.scopes:
        if scope['inum'] == inum or scope['dn'] == inum:
            return scope
    return {}


class EditClientDialog(JansGDialog, DialogUtils):
    """The Main Client Dialog that contain every thing related to The Client
    """

    def __init__(
        self,
        parent,
        data: list,
        title: AnyFormattedText = "",
        buttons: Optional[Sequence[Button]] = [],
        save_handler: Callable = None,
        delete_uma_resource: Callable = None,
    ) -> Dialog:
        """init for `EditClientDialog`, inherits from two diffrent classes `JansGDialog` and `DialogUtils`

        JansGDialog (dialog): This is the main dialog Class Widget for all Jans-cli-tui dialogs except custom dialogs like dialogs with navbar
        DialogUtils (methods): Responsable for all `make data from dialog` and `check required fields` in the form for any Edit or Add New

        Args:
            parent (widget): This is the parent widget for the dialog, to access `Pageup` and `Pagedown`
            title (str): The Main dialog title
            data (list): selected line data 
            button_functions (list, optional): Dialog main buttons with their handlers. Defaults to [].
            save_handler (method, optional): handler invoked when closing the dialog. Defaults to None.
            delete_uma_resource (method, optional): handler invoked when deleting UMA-resources
        """
        super().__init__(parent, title, buttons)

        self.save_handler = save_handler
        self.delete_uma_resource = delete_uma_resource
        self.data = data
        self.title = title
        self.nav_dialog_width = int(self.myparent.dialog_width*1.1)
        self.client_scopes_entries = []
        self.active_tokes_buttons = VSplit([])
        self.add_scope_checkbox = CheckboxList(values=[('', '')])
        self.activate_tokens_per_page = self.myparent.dialog_height-12
        self.fill_client_scopes()
        self.prepare_tabs()
        self.create_window()


    def save(self) -> None:
        """method to invoked when saving the dialog (Save button is pressed)
        """

        current_data = copy.deepcopy(self.data)
        self.data = self.make_data_from_dialog()
        self.data['disabled'] = not self.data['disabled']

        for list_key in (
            'redirectUris',
            'postLogoutRedirectUris',
            'contacts',
            'authorizedOrigins',
            'requestUris',
            'claimRedirectUris',
        ):
            if list_key in self.data:
                self.data[list_key] = self.data[list_key].splitlines()

        self.data['scopes'] = [item[0] for item in self.client_scopes.entries]

        if 'accessTokenAsJwt' in self.data:
            self.data['accessTokenAsJwt'] = self.data['accessTokenAsJwt'] == 'jwt'

        if 'rptAsJwt' in self.data:
            self.data['rptAsJwt'] = self.data['rptAsJwt'] == 'jwt'

        self.data['attributes'] = {}
        self.data['attributes']['redirectUrisRegex'] = self.data.pop('redirectUrisRegex')
        self.data['attributes']['parLifetime'] = self.data.pop('parLifetime')
        self.data['attributes']['requirePar'] = self.data.pop('requirePar')
        #self.data['attributes']['requirePkce'] = self.data.pop('requirePkce', False)

        for list_key in (
            'backchannelLogoutUri',
            'additionalAudience',
            'rptClaimsScripts',
            'spontaneousScopeScriptDns',
        ):
            if list_key in self.data:
                self.data['attributes'][list_key] = self.data.pop(list_key,'').splitlines()

        for key in (
            'runIntrospectionScriptBeforeJwtCreation',
            'backchannelLogoutSessionRequired',
            'jansDefaultPromptLogin',
            'allowSpontaneousScopes',
            'tlsClientAuthSubjectDn',
        ):
            if key in self.data:
                self.data['attributes'][key] = self.data.pop(key)

        for scr_var in self.scripts_widget_dict:
            values = self.scripts_widget_dict[scr_var].get_values()
            if values:
                self.data['attributes'][scr_var] = values

        self.data['displayName'] = self.data['clientName']
        self.data['attributes']['jansAuthorizedAcr'] = self.data.pop('jansAuthorizedAcr')

        for intro_attr in ATTRIBUTE_ALG_PROPERTIES:
            if intro_attr in self.data:
                self.data['attributes'][intro_attr] = self.data.pop(intro_attr)

        cfr = self.check_required_fields()

        if not cfr:
            return

        for ditem in self.drop_down_select_first:
            if ditem in self.data and self.data[ditem] is None:
                self.data.pop(ditem)

        for prop in current_data:
            if prop not in self.data:
                self.data[prop] = current_data[prop]

        # remove authenticationMethod, it is read only
        self.data.pop('authenticationMethod', None)

        exp_date = self.data.pop('expirationDate', None)
        if exp_date:
            self.data['expirationDate'] = exp_date.strftime(ISOFORMAT)

        if self.save_handler:
            self.save_handler(self)

    def cancel(self) -> None:
        """method to invoked when canceling changes in the dialog (Cancel button is pressed)
        """

        self.future.set_result(DialogResult.CANCEL)

    def create_window(self) -> None:
        self.side_nav_bar = JansSideNavBar(myparent=self.myparent,
                                           entries=list(self.tabs.keys()),
                                           selection_changed=(
                                               self.client_dialog_nav_selection_changed),
                                           select=0,
                                           entries_color='class:outh-client-navbar',
                                           save_handler=self.save)

        self.dialog = JansDialogWithNav(
            title=self.title,
            navbar=self.side_nav_bar,
            content=DynamicContainer(lambda: self.tabs[self.left_nav]),
            button_functions=[
                (self.save, _("Save")),
                (self.cancel, _("Cancel"))
            ],
            height=self.myparent.dialog_height,
            width=self.nav_dialog_width,
        )

    def fill_client_scopes(self):
        for scope_dn in self.data.get('scopes', []):
            scope = get_scope_by_inum(scope_dn)
            if scope:
                label = scope['id']
                if [scope_dn, label] not in self.client_scopes_entries:
                    self.client_scopes_entries.append([scope_dn, label])
                    if hasattr(self, 'client_scopes'):
                        if not [scope_dn, label] in self.client_scopes.entries:
                            self.client_scopes.add_label(scope_dn, label)

    def prepare_tabs(self) -> None:
        """Prepare the tabs for Edil Client Dialogs
        """

        acr_values_supported = self.myparent.cli_object.openid_configuration.get('acr_values_supported', [])[:]

        acr_values_supported_list = [ (acr, acr) for acr in acr_values_supported ]


        schema = self.myparent.cli_object.get_schema_from_reference('', '#/components/schemas/Client')

        self.tabs = OrderedDict()

        self.tf = True
        client_secret_next_widget_texts = (_("View"), _("Hide"))

        client_secret_next_widget = Button(client_secret_next_widget_texts[0])

        def change_view_hide(me):
            self.tf = not self.tf  
            client_secret_next_widget.text = client_secret_next_widget_texts[not self.tf]

        client_secret_widget = self.myparent.getTitledText(
                _("Client Secret"),
                name='clientSecret',
                value=self.data.get('clientSecret', ''),
                jans_help=self.myparent.get_help_from_schema(
                    schema, 'clientSecret'),
                style=cli_style.check_box,
                password=Condition(lambda: self.tf),
                next_widget=client_secret_next_widget
                )

        client_secret_next_widget.handler = partial(change_view_hide, client_secret_widget)

        #require_pkce = self.data.get('attributes', {}).get('redirectUrisRegex')
        #if require_pkce is None:
        #    require_pkce = self.myparent.app_configuration.get('requirePkce', False)

        token_endpoint_authmethods = [('none', 'none')]
        for method in schema['properties']['tokenEndpointAuthMethod']['enum']:
            if method == 'none':
                continue
            token_endpoint_authmethods.append((method, method))


        basic_tab_widgets = [
            self.myparent.getTitledText(
                _("Client_ID"),
                name='inum',
                value=self.data.get('inum', ''),
                jans_help=self.myparent.get_help_from_schema(schema, 'inum'),
                read_only=True,
                style=cli_style.edit_text),

            self.myparent.getTitledCheckBox(
                _("Active"),
                name='disabled',
                checked=not self.data.get('disabled'),
                jans_help=self.myparent.get_help_from_schema(
                    schema, 'disabled'),
                style=cli_style.check_box),

            self.myparent.getTitledText(
                _("Client Name"),
                name='clientName',
                value=self.data.get('clientName', ''),
                jans_help=self.myparent.get_help_from_schema(
                    schema, 'clientName'),
                style=cli_style.edit_text),

            client_secret_widget,

            self.myparent.getTitledText(
                _("Description"),
                name='description',
                value=self.data.get('description', ''),
                jans_help=self.myparent.get_help_from_schema(
                    schema, 'description'),
                style=cli_style.check_box),


            self.myparent.getTitledWidget(
                _("Authn Method token endpoint"),
                name='tokenEndpointAuthMethod',
                widget=DropDownWidget(
                    values=token_endpoint_authmethods,
                    value=self.data.get('tokenEndpointAuthMethod')
                ),
                jans_help=self.myparent.get_help_from_schema(
                    schema, 'tokenEndpointAuthMethod'),
                style=cli_style.drop_down),


            self.myparent.getTitledRadioButton(
                _("Subject Type"),
                name='subjectType',
                values=[('public', 'Public'), ('pairwise', 'Pairwise')],
                current_value=self.data.get('subjectType'),
                jans_help=self.myparent.get_help_from_schema(
                    schema, 'subjectType'),
                style=cli_style.radio_button),

            self.myparent.getTitledText(
                _("Sector Identifier URI"),
                name='sectorIdentifierUri',
                value=self.data.get('sectorIdentifierUri', ''),
                jans_help=self.myparent.get_help_from_schema(
                    schema, 'sectorIdentifierUri'),
                style=cli_style.check_box),

            self.myparent.getTitledCheckBoxList(
                _("Grant"),
                name='grantTypes',
                values=[('authorization_code', 'Authorization Code'), ('refresh_token', 'Refresh Token'), ('urn:ietf:params:oauth:grant-type:uma-ticket',
                                                                                                           'UMA Ticket'), ('client_credentials', 'Client Credentials'), ('password', 'Password'), ('implicit', 'Implicit')],
                current_values=self.data.get('grantTypes', []),
                jans_help=self.myparent.get_help_from_schema(
                    schema, 'grantTypes'),
                style='class:outh-client-checkboxlist'),

            self.myparent.getTitledCheckBoxList(
                _("Response Types"),
                name='responseTypes',
                values=['code', 'token', 'id_token'],
                current_values=self.data.get('responseTypes', []),
                jans_help=self.myparent.get_help_from_schema(
                    schema, 'responseTypes'),
                style='class:outh-client-checkboxlist'),

            self.myparent.getTitledCheckBox(
                _("Suppress Authorization"),
                name='trustedClient',
                checked=self.data.get('trustedClient'),
                jans_help=self.myparent.get_help_from_schema(schema, 'trustedClient'),
                style=cli_style.check_box),

            #self.myparent.getTitledCheckBox(
            #    _("Require PKCE"),
            #    name='requirePkce',
            #    checked=require_pkce,
            #    jans_help=self.myparent.get_help_from_schema(schema, 'requirePkce'),
            #    style=cli_style.check_box),

            self.myparent.getTitledRadioButton(
                _("Application Type"),
                name='applicationType',
                values=['native', 'web'],
                current_value=self.data.get('applicationType'),
                jans_help=self.myparent.get_help_from_schema(
                    schema, 'applicationType'),
                style=cli_style.radio_button),

            self.myparent.getTitledText(
                _("Redirect Uris"),
                name='redirectUris',
                value='\n'.join(self.data.get('redirectUris', [])),
                height=3,
                jans_help=self.myparent.get_help_from_schema(
                    schema, 'redirectUris'),
                style='class:outh-client-textrequired'),

            self.myparent.getTitledText(
                _("Redirect Regex"),
                name='redirectUrisRegex',
                value=self.data.get('attributes', {}).get(
                    'redirectUrisRegex', ''),
                jans_help=self.myparent.get_help_from_schema(
                    self.myparent.cli_object.get_schema_from_reference(
                        '', ATTRIBUTE_SCHEMA_PATH),
                    'redirectUrisRegex'),
                style=cli_style.check_box)
        ]

        add_scope_button = VSplit([Window(), self.myparent.getButton(
            text=_("Add Scope"),
            name='oauth:logging:save',
            jans_help=_("Add Scopes"),
            handler=self.add_scopes)
        ])

        self.client_scopes = JansLabelContainer(
            title=_('Scopes'),
            width=self.nav_dialog_width - 26,
            on_display=self.myparent.data_display_dialog,
            on_delete=self.delete_scope,
            buttonbox=add_scope_button,
            entries=self.client_scopes_entries,
        )

        basic_tab_widgets.append(self.client_scopes)

        self.tabs['Basic'] = HSplit(
            basic_tab_widgets, width=D(), style=cli_style.tabs)

        self.tabs['Tokens'] = HSplit([
            self.myparent.getTitledRadioButton(
                _("Access Token Type"),
                name='accessTokenAsJwt',
                values=[('jwt', 'JWT'), ('reference', 'Reference')],
                current_value='jwt' if self.data.get(
                    'accessTokenAsJwt') else 'reference',
                jans_help=self.myparent.get_help_from_schema(
                    schema, 'accessTokenAsJwt'),
                style=cli_style.radio_button),

            self.myparent.getTitledCheckBox(
                _("Include Claims in id_token"),
                name='includeClaimsInIdToken',
                checked=self.data.get('includeClaimsInIdToken'),
                jans_help=self.myparent.get_help_from_schema(
                    schema, 'includeClaimsInIdToken'),
                style=cli_style.check_box),

            self.myparent.getTitledCheckBox(
                _("Run introspection script before JWT access token creation"),
                name='runIntrospectionScriptBeforeJwtCreation',
                checked=self.data.get('attributes', {}).get(
                    'runIntrospectionScriptBeforeJwtCreation'),
                jans_help=self.myparent.get_help_from_schema(
                    self.myparent.cli_object.get_schema_from_reference(
                        '', ATTRIBUTE_SCHEMA_PATH),
                    'runIntrospectionScriptBeforeJwtCreation'),
                style=cli_style.check_box),

            self.myparent.getTitledText(
                title=_("Token binding confirmation  method for id_token"),
                name='idTokenTokenBindingCnf',
                value=self.data.get('idTokenTokenBindingCnf', ''),
                jans_help=self.myparent.get_help_from_schema(
                    schema, 'idTokenTokenBindingCnf'),
                style=cli_style.check_box),

            self.myparent.getTitledText(
                title=_("Access token additional audiences"),
                name='additionalAudience',
                value='\n'.join(self.data.get('attributes', {}).get(
                    'additionalAudience', [])),
                jans_help=self.myparent.get_help_from_schema(
                    self.myparent.cli_object.get_schema_from_reference(
                        '', ATTRIBUTE_SCHEMA_PATH),
                    'additionalAudience'),
                style=cli_style.check_box,
                height=3),

            self.myparent.getTitledText(
                _("Access token lifetime"),
                name='accessTokenLifetime',
                value=self.data.get('accessTokenLifetime', ''),
                jans_help=self.myparent.get_help_from_schema(
                    schema, 'accessTokenLifetime'),
                text_type='integer',
                style=cli_style.check_box),

            self.myparent.getTitledText(
                _("Refresh token lifetime"),
                name='refreshTokenLifetime',
                value=self.data.get('refreshTokenLifetime', ''),
                jans_help=self.myparent.get_help_from_schema(
                    schema, 'refreshTokenLifetime'),
                text_type='integer',
                style=cli_style.check_box),

            self.myparent.getTitledText(
                _("Default max authn age"),
                name='defaultMaxAge',
                value=self.data.get('defaultMaxAge', ''),
                jans_help=self.myparent.get_help_from_schema(
                    schema, 'defaultMaxAge'),
                text_type='integer',
                style=cli_style.check_box),

        ], width=D(), style=cli_style.tabs)

        self.tabs['Logout'] = HSplit([

            self.myparent.getTitledText(
                _("Front channel logout URI"),
                name='frontChannelLogoutUri',
                value=self.data.get('frontChannelLogoutUri', ''),
                jans_help=self.myparent.get_help_from_schema(
                    schema, 'frontChannelLogoutUri'),  # No Descritption
                style=cli_style.check_box),

            self.myparent.getTitledText(
                _("Post logout redirect URIs"),
                name='postLogoutRedirectUris',
                value='\n'.join(self.data.get('postLogoutRedirectUris', [])),
                jans_help=self.myparent.get_help_from_schema(
                    schema, 'postLogoutRedirectUris'),
                height=3, style=cli_style.check_box),

            self.myparent.getTitledText(
                _("Back channel logout URI"),
                name='backchannelLogoutUri',
                value='\n'.join(self.data.get('attributes', {}).get(
                    'backchannelLogoutUri', [])),
                jans_help=self.myparent.get_help_from_schema(
                    self.myparent.cli_object.get_schema_from_reference(
                        '', ATTRIBUTE_SCHEMA_PATH),
                    'backchannelLogoutUri'),
                height=3, style=cli_style.check_box
            ),

            self.myparent.getTitledCheckBox(
                _("Back channel logout session required"),
                name='backchannelLogoutSessionRequired',
                checked=self.data.get('attributes', {}).get(
                    'backchannelLogoutSessionRequired'),
                jans_help=self.myparent.get_help_from_schema(
                    self.myparent.cli_object.get_schema_from_reference(
                        '', ATTRIBUTE_SCHEMA_PATH),
                    'backchannelLogoutSessionRequired'),
                style=cli_style.check_box
            ),

            self.myparent.getTitledCheckBox(
                _("Front channel logout session required"),
                name='frontChannelLogoutSessionRequired',
                checked=self.data.get('frontChannelLogoutSessionRequired'),
                jans_help=self.myparent.get_help_from_schema(
                    schema, 'frontChannelLogoutSessionRequired'),  # No Descritption
                style=cli_style.check_box),

        ], width=D(), style=cli_style.tabs
        )

        self.tabs['Software Info'] = HSplit([


            self.myparent.getTitledText(
                _("Client URI"),
                name='clientUri',
                value=self.data.get('clientUri', ''),
                jans_help=self.myparent.get_help_from_schema(
                    schema, 'redirectUris'),
                style=cli_style.titled_text),

            self.myparent.getTitledText(
                _("Policiy URI"),
                name='policyUri',
                value=self.data.get('policyUri', ''),
                jans_help=self.myparent.get_help_from_schema(
                    schema, 'policyUri'),
                style=cli_style.titled_text),

            self.myparent.getTitledText(
                _("Logo URI"),
                name='logoUri',
                value=self.data.get('logoUri', ''),
                jans_help=self.myparent.get_help_from_schema(
                    schema, 'logoUri'),
                style=cli_style.titled_text),

            self.myparent.getTitledText(_("Contacts"),  # height =3 insted of the <+> button
                                        name='contacts',
                                        value='\n'.join(
                                            self.data.get('contacts', [])),
                                        height=3,
                                        jans_help=self.myparent.get_help_from_schema(
                                            schema, 'contacts'),
                                        style=cli_style.check_box),

            self.myparent.getTitledText(_("Authorized JS origins"),  # height =3 insted of the <+> button
                                        name='authorizedOrigins',
                                        value='\n'.join(self.data.get(
                                            'authorizedOrigins', [])),
                                        height=3,
                                        jans_help=self.myparent.get_help_from_schema(
                                            schema, 'authorizedOrigins'),
                                        style=cli_style.check_box),

            self.myparent.getTitledText(
                title=_("Software id"),
                name='softwareId',
                value=self.data.get('softwareId', ''),
                jans_help=self.myparent.get_help_from_schema(
                    schema, 'softwareId'),
                style=cli_style.check_box),

            self.myparent.getTitledText(
                title=_("Software version"),
                name='softwareVersion',
                value=self.data.get('softwareVersion', ''),
                jans_help=self.myparent.get_help_from_schema(
                    schema, 'softwareVersion'),
                style=cli_style.check_box),

            self.myparent.getTitledText(
                title=_("Software statement"),
                name='softwareStatement',
                value=self.data.get('softwareStatement', ''),
                jans_help=self.myparent.get_help_from_schema(
                    schema, 'softwareStatement'),
                style=cli_style.check_box),

        ], width=D(), style=cli_style.tabs)

        self.uma_resources = HSplit([], width=D())
        self.resources = HSplit([
            VSplit([
                self.myparent.getButton(text=_("Get Resources"), name='oauth:Resources:get', jans_help=_(
                    "Retreive UMA Resources"), handler=self.oauth_get_uma_resources),
                self.myparent.getTitledText(_("Search"), name='oauth:Resources:search', jans_help=_(
                    "Press enter to perform search"), accept_handler=self.search_uma_resources, style='class:outh-client-textsearch'),

            ],
                padding=3,
                width=D(),
            ),
            DynamicContainer(lambda: self.uma_resources)
        ], style=cli_style.tabs)

        self.tabs['CIBA/PAR/UMA'] = HSplit([
            Label(text=_("CIBA"), style=cli_style.label),
            self.myparent.getTitledRadioButton(
                _("Token delivery method"),
                name='backchannelTokenDeliveryMode',
                current_value=self.data.get(
                    'backchannelTokenDeliveryMode'),
                values=['poll', 'push', 'ping'],
                jans_help=self.myparent.get_help_from_schema(
                    schema, 'backchannelTokenDeliveryMode'),
                style=cli_style.radio_button),

            self.myparent.getTitledText(
                title=_("Client notification endpoint"),
                name='backchannelClientNotificationEndpoint',
                value=self.data.get(
                    'backchannelClientNotificationEndpoint', ''),
                jans_help=self.myparent.get_help_from_schema(
                    schema, 'backchannelClientNotificationEndpoint'),
                style=cli_style.check_box),

            self.myparent.getTitledCheckBox(
                _("Require user code param"),
                name='backchannelUserCodeParameter',
                checked=self.data.get(
                    'backchannelUserCodeParameter', ''),
                style=cli_style.check_box,
                jans_help=self.myparent.get_help_from_schema(
                    schema, 'backchannelUserCodeParameter'),

            ),

            Label(text=_("PAR"), style=cli_style.label),

            self.myparent.getTitledText(
                title=_("Request lifetime"),
                name='parLifetime',
                value=self.data.get('attributes', {}).get(
                    'parLifetime', 0),
                jans_help=self.myparent.get_help_from_schema(
                    self.myparent.cli_object.get_schema_from_reference(
                        '', ATTRIBUTE_SCHEMA_PATH),
                    'parLifetime'),
                text_type='integer',
                style=cli_style.check_box),

            self.myparent.getTitledCheckBox(
                _("Request PAR"),
                name='requirePar',
                checked=self.data.get(
                    'attributes', {}).get('requirePar', ''),
                style=cli_style.check_box,
                jans_help=self.myparent.get_help_from_schema(
                    schema, 'requirePar'),
            ),

            Label(_("UMA"), style=cli_style.label),

            self.myparent.getTitledRadioButton(
                _("PRT token type"),
                name='rptAsJwt',
                values=[('jwt', 'JWT'),
                        ('reference', 'Reference')],
                current_value='jwt' if self.data.get(
                    'rptAsJwt') else 'reference',
                jans_help=self.myparent.get_help_from_schema(
                    schema, 'rptAsJwt'),
                style=cli_style.radio_button),

            self.myparent.getTitledText(
                title=_("Claims redirect URI"),
                name='claimRedirectUris',
                value='\n'.join(self.data.get('claimRedirectUris', '')),
                jans_help=self.myparent.get_help_from_schema(
                    schema, 'claimRedirectUris'),
                height=3,
                style=cli_style.check_box),

            self.myparent.getTitledText(_("RPT Modification Script"),
                                        name='rptClaimsScripts',
                                        value='\n'.join(self.data.get(
                                            'attributes', {}).get('rptClaimsScripts', [])),
                                        height=3,
                                        style=cli_style.check_box,
                                        jans_help=self.myparent.get_help_from_schema(
                self.myparent.cli_object.get_schema_from_reference(
                    '', '#/components/schemas/Scope'),
                'rptClaimsScripts'),
            ),

            self.resources if self.data.get(
                'inum', '') else HSplit([], width=D())

        ], width=D(), style=cli_style.tabs
        )

        encryption_signing = [
            self.myparent.getTitledText(
                title=_("Client JWKS URI"),
                name='jwksUri',
                value=self.data.get('jwksUri', ''),
                jans_help=self.myparent.get_help_from_schema(
                    schema, 'jwksUri'),
                style=cli_style.check_box),

            self.myparent.getTitledText(
                title=_("Client JWKS"),
                name='jwks',
                value=self.data.get('jwks', ''),
                jans_help=self.myparent.get_help_from_schema(schema, 'jwks'),
                style=cli_style.check_box),
        ]

        self.drop_down_select_first = []

        for title, swagger_key, openid_key in (

                (_("ID Token Alg for Signing "), 'idTokenSignedResponseAlg',
                 'id_token_signing_alg_values_supported'),
                (_("ID Token Alg for Encryption"), 'idTokenEncryptedResponseAlg',
                 'id_token_encryption_alg_values_supported'),
                (_("ID Token Enc for Encryption"), 'idTokenEncryptedResponseEnc',
                 'id_token_encryption_enc_values_supported'),
                (_("Access Token Alg for Signing "), 'accessTokenSigningAlg',
                 'access_token_signing_alg_values_supported'),

                (_("User Info for Signing "), 'userInfoSignedResponseAlg',
                 'userinfo_signing_alg_values_supported'),
                (_("User Info Alg for Encryption"), 'userInfoEncryptedResponseAlg',
                 'userinfo_encryption_alg_values_supported'),
                (_("User Info Enc for Encryption"), 'userInfoEncryptedResponseEnc',
                 'userinfo_encryption_enc_values_supported'),

                (_("Request Object Alg for Signing "), 'requestObjectSigningAlg',
                 'request_object_signing_alg_values_supported'),
                (_("Request Object Alg for Encryption"), 'requestObjectEncryptionAlg',
                 'request_object_encryption_alg_values_supported'),
                (_("Request Object Enc for Encryption"), 'requestObjectEncryptionEnc',
                 'request_object_encryption_enc_values_supported'),

                 (_("Introspection Signed Response Alg "), 'introspectionSignedResponseAlg',
                 'id_token_signing_alg_values_supported'),
                (_("Introspection Encrypted Response Alg"), 'introspectionEncryptedResponseAlg',
                 'id_token_encryption_alg_values_supported'),
                (_("Introspection Encrypted Response Enc"), 'introspectionEncryptedResponseEnc',
                 'id_token_encryption_enc_values_supported'),

                 (_("Transaction Token Alg for Signing"), 'txTokenSignedResponseAlg',
                 'tx_token_signing_alg_values_supported'),
                (_("Transaction Token Alg for Encryption"), 'txTokenEncryptedResponseAlg',
                 'tx_token_encryption_alg_values_supported'),
                (_("Transaction Token Enc for Encryption"), 'txTokenEncryptedResponseEnc',
                 'tx_token_encryption_enc_values_supported'),


        ):

            self.drop_down_select_first.append(swagger_key)

            values = [(alg, alg) for alg in self.myparent.cli_object.openid_configuration.get(
                openid_key, [])]

            value = self.data.get('attributes', {}).get(swagger_key) if swagger_key in ATTRIBUTE_ALG_PROPERTIES else self.data.get(swagger_key)

            encryption_signing.append(self.myparent.getTitledWidget(
                title,
                name=swagger_key,
                widget=DropDownWidget(
                    values=values,
                    value=value
                ),
                jans_help=self.myparent.get_help_from_schema(
                    schema, swagger_key),
                style='class:outh-client-dropdown'))

        self.tabs['Encryption/Signing'] = HSplit(encryption_signing)

        def allow_spontaneous_changed(cb):
            self.spontaneous_scopes.me.window.style = 'underline ' + \
                (self.myparent.styles['textarea']
                 if cb.checked else self.myparent.styles['textarea-readonly'])
            self.spontaneous_scopes.me.text = ''
            self.spontaneous_scopes.me.read_only = not cb.checked

        self.spontaneous_scopes = self.myparent.getTitledText(
            _("Spontaneos scopes validation regex"),
            name='spontaneousScopeScriptDns',
            value='\n'.join(self.data.get('attributes', {}).get(
                'spontaneousScopeScriptDns', [])),
            read_only=False if 'allowSpontaneousScopes' in self.data and self.data.get(
                'attributes', {}).get('allowSpontaneousScopes') else True,
            focusable=True,
            jans_help=self.myparent.get_help_from_schema(
                self.myparent.cli_object.get_schema_from_reference(
                    '', ATTRIBUTE_SCHEMA_PATH),
                'spontaneousScopeScriptDns'),
            height=3,
            style=cli_style.check_box)

        self.tabs['Advanced Client Prop.'] = HSplit([

            self.myparent.getTitledCheckBox(
                _("Default Prompt login"),
                name='jansDefaultPromptLogin',
                checked=self.data.get('attributes', {}).get(
                    'jansDefaultPromptLogin'),
                jans_help=self.myparent.get_help_from_schema(
                    self.myparent.cli_object.get_schema_from_reference(
                        '', ATTRIBUTE_SCHEMA_PATH),
                    'jansDefaultPromptLogin'),

                style=cli_style.check_box
            ),

            self.myparent.getTitledCheckBox(
                _("Persist Authorizations"),
                name='persistClientAuthorizations',
                checked=self.data.get('persistClientAuthorizations'),
                jans_help=self.myparent.get_help_from_schema(
                    schema, 'persistClientAuthorizations'),
                style=cli_style.check_box),

            self.myparent.getTitledCheckBox(
                _("Allow spontaneos scopes"),
                name='allowSpontaneousScopes',
                checked=self.data.get('attributes', {}).get(
                    'allowSpontaneousScopes'),
                on_selection_changed=allow_spontaneous_changed,
                jans_help=self.myparent.get_help_from_schema(
                    self.myparent.cli_object.get_schema_from_reference(
                        '', ATTRIBUTE_SCHEMA_PATH),
                    'allowSpontaneousScopes'),
                style=cli_style.check_box
            ),

            self.spontaneous_scopes,


            VSplit([
                Label(text=_("Spontaneous scopes"), style=cli_style.label,
                      width=len(_("Spontaneous scopes")*2)),  # TODO
                Button(
                    _("View current"),
                    handler=self.show_client_scopes,
                    left_symbol='<',
                    right_symbol='>',
                    width=len(_("View current"))+2)

            ]) if self.data.get('inum', '') else HSplit([], width=D()),

            self.myparent.getTitledText(
                _("Initial Login URI"),
                name='initiateLoginUri',
                value=self.data.get('initiateLoginUri', ''),
                jans_help=self.myparent.get_help_from_schema(
                    schema, 'initiateLoginUri'),
                style=cli_style.check_box),

            self.myparent.getTitledText(_("Request URIs"),  # height =3 insted of the <+> button
                                        name='requestUris',
                                        value='\n'.join(
                self.data.get('requestUris', [])),
                height=3,
                jans_help=self.myparent.get_help_from_schema(
                                            schema, 'requestUris'),
                style=cli_style.check_box),

            self.myparent.getTitledCheckBoxList(_("Default  ACRs"),  # height =3 >> "the type is array" cant be dropdown
                                        name='defaultAcrValues',
                                        values=acr_values_supported_list,
                                        current_values=self.data.get('defaultAcrValues', []),
                                        jans_help=self.myparent.get_help_from_schema(
                                                schema, 'defaultAcrValues'),
                                        style=cli_style.check_box),

            self.myparent.getTitledCheckBoxList(_("Allowed  ACRs"),  # height =3 insted of the <+> button
                                        name='jansAuthorizedAcr',
                                        values=acr_values_supported_list,
                                        current_values=self.data.get('attributes', {}).get('jansAuthorizedAcr', []),
                                        jans_help=self.myparent.get_help_from_schema(
                                            self.myparent.cli_object.get_schema_from_reference(
                                            '', ATTRIBUTE_SCHEMA_PATH), 'jansAuthorizedAcr'),
                                        style=cli_style.check_box),

            self.myparent.getTitledText(
                _("TLS Subject DN"),
                name='tlsClientAuthSubjectDn',
                value='\n'.join(self.data.get('attributes', {}).get(
                                'tlsClientAuthSubjectDn') or []),
                height=3, style=cli_style.check_box,
                jans_help=self.myparent.get_help_from_schema(
                    self.myparent.cli_object.get_schema_from_reference(
                        '', ATTRIBUTE_SCHEMA_PATH),
                    'tlsClientAuthSubjectDn'),
            ),

            self.myparent.getTitledWidget(
                _("Client Expiration Date"),
                name='expirationDate',
                widget=DateSelectWidget(app=common_data.app, value=fromisoformat(self.data.get('expirationDate', ''))),
                jans_help=self.myparent.get_help_from_schema(
                    schema, 'expirationDate'),
                style='class:outh-client-widget'
            ),

        ], width=D(), style=cli_style.tabs
        )


        self.scripts_widget_dict = OrderedDict()


        list_of_scripts = (
                ("Spontaneous Scopes", 'spontaneousScopes', ['spontaneous_scope', 'uma_claims_gathering', 'uma_rpt_policy']),
                ("Update Token", 'updateTokenScriptDns', ['update_token']),
                ("Post Authn", 'postAuthnScripts', ['post_authn']),
                ("Introspection", 'introspectionScripts', ['introspection', 'persistence_extension', 'person_authentication']),
                ("Password Grant", 'ropcScripts', ['resource_owner_password_credentials', 'scim']),
                ("OAuth Consent", 'consentGatheringScripts', ['application_session', 'authorization_challenge',  'cache_refresh', 'ciba_end_user_notification', 'client_registration', 'config_api_auth',  'consent_gathering', 'discovery', 'dynamic_scope', 'end_session', 'id_generator', 'idp'])
                )


        for title, script_var, script_types in list_of_scripts:
            scripts_data = []
            for scr in common_data.enabled_scripts:
                if scr['scriptType'] in script_types:
                    scripts_data.append((scr['dn'], scr['name']))

            self.scripts_widget_dict[script_var] = JansLabelWidget(
                        title = _(title), 
                        values = self.data.get('attributes', {}).get(script_var, []),
                        data = scripts_data
                        )


        self.tabs['Client Scripts'] = HSplit(list(self.scripts_widget_dict.values()), width=D(), style=cli_style.tabs)


        self.active_tokens_list = JansVerticalNav(
                myparent=common_data.app,
                headers=[_("Deletable"), _("Expiration"), _("Token Type")],
                preferred_size=[0, 0, 0],
                data={},
                on_display=common_data.app.data_display_dialog,
                on_delete=self.delete_active_token,
                selectes=0,
                headerColor=cli_style.navbar_headcolor,
                entriesColor=cli_style.navbar_entriescolor,
                all_data=[],
                hide_headers=True
            )


        self.active_tokens_list.start_index = 0
        date_after_title = _("Expires After")
        date_before_title = _("Expires Before")
        search_title = _("Search Tokens")

        self.search_date_after_widget = DateSelectWidget(app=common_data.app)
        self.search_date_before_widget = DateSelectWidget(app=common_data.app)

        if self.data.get('inum'):
            self.tabs['Active Tokens'] = HSplit([
                            VSplit([
                                Label(date_after_title + ':', width=len(date_after_title)+1, style=cli_style.edit_text),
                                self.search_date_after_widget,
                                Label(date_before_title + ':', width=len(date_before_title)+1, style=cli_style.edit_text),
                                self.search_date_before_widget,
                                Button(text=search_title, width=len(search_title)+4, handler=self.search_active_tokens),
                            ],
                            padding=1,
                            height=1,
                            width=D()
                            ),
                            self.active_tokens_list,
                            DynamicContainer(lambda: self.active_tokes_buttons)
                        ],
                        width=D())

        self.left_nav = list(self.tabs.keys())[0]

    def delete_active_token(self, **kwargs: Any) -> None:
        """This method for the deletion of the User 
        """

        entry = self.active_tokens_list.all_data[kwargs['selected_idx']]

        if not entry.get('deletable'):
            common_data.app.show_message(_(common_strings.warning), _("This token cannot be deleted."), tobefocused=self.active_tokens_list)
            return

        def do_delete_token():
            cli_args = {'operation_id': 'revoke-token', 'url_suffix': f"tknCde:{entry['tokenCode']}"}

            async def coroutine():
                common_data.app.start_progressing(_("Deleting token {}...").format(entry['tokenCode']))
                response = await common_data.app.loop.run_in_executor(common_data.app.executor, common_data.app.cli_requests, cli_args)
                common_data.app.stop_progressing()

                if response is not None:
                    common_data.app.show_message(_(common_strings.error), _("Token was not delated"), tobefocused=self.active_tokens_list)
                    return

                self.get_client_active_tokens(start_index=self.active_tokens_list.start_index)
            asyncio.ensure_future(coroutine())


        buttons = [Button(_("No")), Button(_("Yes"), handler=do_delete_token)]

        common_data.app.show_message(
                title=_(common_strings.confirm),
                message=HTML(_("Are you sure you want to delete token <b>{}</b>?").format(entry['tokenCode'])),
                buttons=buttons,
                )


    def search_active_tokens(self, start_index: Optional[int]= 0) -> None:
        """This method handle the search for active tokens

        Args:
            tbuffer (Buffer): Buffer returned from the TextArea widget > GetTitleText
        """

        #self.oauth_get_scopes(pattern=tbuffer.text)

        if not self.data.get('inum'):
            return

        self.get_client_active_tokens(start_index)

    def get_client_active_tokens(self, start_index=0, pattern=''):
        self.active_tokens_list.start_index = start_index

        endpoint_args = f'limit:{self.activate_tokens_per_page},startIndex:{start_index},fieldValuePair:clientId={self.data["inum"]}'
        search_arg_lists = []
        date_after = self.search_date_after_widget.value
        date_before = self.search_date_before_widget.value

        if date_after:
            date_after = date_after.replace(microsecond=0)
            search_arg_lists.append(f'expirationDate>{date_after}')

        if date_before:
            date_before = date_before.replace(microsecond=0)
            search_arg_lists.append(f'expirationDate<{date_before}')

        if search_arg_lists:
            endpoint_args += '\\,' + '\\,'.join(search_arg_lists)

        if pattern:
            endpoint_args += f',pattern:{pattern}'

        cli_args = {'operation_id': 'search-token', 'endpoint_args': endpoint_args}


        def get_next():
            self.get_client_active_tokens(start_index=start_index+self.activate_tokens_per_page, pattern=pattern)

        def get_previous():
            self.get_client_active_tokens(start_index=start_index-self.activate_tokens_per_page, pattern=pattern)


        async def coroutine():
            common_data.app.start_progressing(_("Retreiving tokens from server..."))
            response = await common_data.app.loop.run_in_executor(common_data.app.executor, common_data.app.cli_requests, cli_args)
            common_data.app.stop_progressing()
            result = response.json()
            common_data.app.logger.debug("Tokens: {}".format(result))

            if not result.get('entries'):
                self.active_tokes_buttons = VSplit([])
                common_data.app.show_message(_(common_strings.no_matching_result), _("No token found for this search."), tobefocused=self.tabs['Active Tokens'])
                return

            self.active_tokens_list.clear()
            all_data = result['entries']
            self.active_tokens_list.all_data = all_data
            for entry in all_data:
                #entry.pop('tokenCode', None)
                self.active_tokens_list.add_item(
                    [str(entry.get('deletable', 'False')),
                    str(entry.get('expirationDate', '---')),
                    entry.get('tokenType', '---')]
                )

            buttons = []
            if start_index:
                buttons.append(Button("Previous", handler=get_previous))
            if result['totalEntriesCount'] > start_index + self.activate_tokens_per_page:
                buttons.append(Button("Next", handler=get_next))

            self.active_tokes_buttons = VSplit(buttons, padding=1, align=HorizontalAlign.CENTER)
            self.active_tokens_list.hide_headers = False
            common_data.app.layout.focus(self.active_tokens_list)

        asyncio.ensure_future(coroutine())



    def scope_exists(self, scope_dn: str) -> bool:
        for item_id, item_label in self.client_scopes.entries:
            if item_id == scope_dn:
                return True
        return False


    def add_scopes(self) -> None:

        def add_selected_claims(dialog):
            if 'scopes' not in self.data:
                self.data['scopes'] = []

            self.data['scopes'] += self.add_scope_checkbox.current_values
            self.fill_client_scopes()

        scopes_list = []
        for scope in common_data.scopes:
            if not self.scope_exists(scope['dn']):
                scopes_list.append(
                    (scope['dn'], scope.get('id', '') or scope['inum']))

            scopes_list.sort(key=lambda x: x[1])

        def on_text_changed(event):
            search_text = event.text
            matching_items = []
            search_text = event.text
            for item in scopes_list:
                if search_text in item[1]:
                    matching_items.append(item)
            if matching_items:
                self.add_scope_checkbox.values = matching_items
                self.add_scope_frame.body = HSplit(children=[self.add_scope_checkbox])
                self.add_scope_checkbox._selected_index = 0
            else:
                self.add_scope_frame.body = HSplit(children=[Label(text=_("No Items "), style=cli_style.label,
                                                                   width=len(_("No Items "))),], width=D())

            

        ta = TextArea(
            height=D(),
            width=D(),
            multiline=False,
        )

        ta.buffer.on_text_changed += on_text_changed

        self.add_scope_checkbox.values = scopes_list
        self.add_scope_frame = Frame(
            title="Checkbox list",
            body=HSplit(children=[self.add_scope_checkbox]),
        )
        layout = HSplit(children=[
            VSplit(
                children=[
                    Label(text=_("Filter "), style=cli_style.label,
                          width=len(_("Filter "))),
                    ta
                ]),
            Window(height=2, char=' '),
            self.add_scope_frame

        ])

        buttons = [Button(_("Cancel")), Button(
            _("OK"), handler=add_selected_claims)]

        self.addScopeDialog = JansGDialog(
            self.myparent,
            title=_("Select scopes to add"),
            body=layout,
            buttons=buttons)

        self.myparent.show_jans_dialog(self.addScopeDialog)

    def delete_scope(self, scope: list) -> None:

        def do_delete_scope(dialog):
            self.data['scopes'].remove(scope[0])
            self.client_scopes_entries.remove([scope[0], scope[1]])

        dialog = self.myparent.get_confirm_dialog(
            message=_(
                "Are you sure want to delete Scope:\n {} ?".format(scope[1])),
            confirm_handler=do_delete_scope
        )

        self.myparent.show_jans_dialog(dialog)

    def show_client_scopes(self) -> None:
        client_scopes = self.data.get('scopes')  # [0]
        data = []
        for i in client_scopes:
            try:
                inum = i.split(',')[0][5:]
                rsponse = self.myparent.cli_object.process_command_by_id(
                    operation_id='get-oauth-scopes-by-inum',
                    url_suffix=URL_SUFFIX_FORMATTER.format(inum),
                    endpoint_args="",
                    data_fn=None,
                    data={}
                )

            except Exception:
                # self.myparent.show_message(ERROR_GETTING_CLIENTS, str(e))
                pass

            if rsponse.status_code not in (200, 201):
                # self.myparent.show_message(ERROR_GETTING_CLIENTS, str(rsponse.text))
                pass
            if rsponse.json().get('scopeType', '') == 'spontaneous':
                data.append(rsponse.json())

        if not data:
            data = "No Scope of type: Spontaneous"

        body = HSplit([
            TextArea(
                lexer=DynamicLexer(lambda: PygmentsLexer.from_filename(
                    '.json', sync_from_start=True)),
                scrollbar=True,
                line_numbers=True,
                multiline=True,
                read_only=True,
                text=str(json.dumps(data, indent=2)),
                style='class:jans-main-datadisplay.text'
            )
        ], style='class:jans-main-datadisplay')

        dialog = JansGDialog(self.myparent, title='View Scopes', body=body)

        self.myparent.show_jans_dialog(dialog)

    def oauth_get_uma_resources(self) -> None:
        """Method to get the clients data from server
        """
        t = threading.Thread(
            target=self.oauth_update_uma_resources, daemon=True)
        t.start()

    def search_uma_resources(
        self,
        tbuffer: Buffer,
    ) -> None:
        """This method handel the search for UMA resources

        Args:
            tbuffer (Buffer): Buffer returned from the TextArea widget > GetTitleText
        """

        if not len(tbuffer.text) > 2:
            self.myparent.show_message(_("Error!"), _(
                "Search string should be at least three characters"))
            return

        t = threading.Thread(target=self.oauth_update_uma_resources, args=(
            tbuffer.text,), daemon=True)
        t.start()

    def oauth_update_uma_resources(
        self,
        pattern: Optional[str] = '',
    ) -> None:
        """update the current uma_resources  data to server

        Args:
            pattern (str, optional): endpoint arguments for the uma_resources data. Defaults to ''.
        """
        endpoint_args = 'limit:10'
        if pattern:
            endpoint_args += ',pattern:'+pattern

        try:
            rsponse = self.myparent.cli_object.process_command_by_id(
                operation_id='get-oauth-uma-resources-by-clientid',
                url_suffix='clientId:{}'.format(self.data['inum']),
                endpoint_args=endpoint_args,
                data_fn=None,
                data={}
            )

        except Exception as e:
            self.myparent.show_message(ERROR_GETTING_CLIENTS, str(e))
            return

        if rsponse.status_code not in (200, 201):
            self.myparent.show_message(
                ERROR_GETTING_CLIENTS, str(rsponse.text))
            return

        result = {}
        try:
            result = rsponse.json()
        except Exception:
            self.myparent.show_message(
                ERROR_GETTING_CLIENTS, str(rsponse.text))
            return
        data = []

        for d in result:
            scopes_of_resource = []
            for scope_dn in d.get('scopes', []):

                inum = scope_dn.split(',')[0].split('=')[1]
                scope_result = {}
                try:
                    scope_response = self.myparent.cli_object.process_command_by_id(
                        operation_id='get-oauth-scopes-by-inum',
                        url_suffix=URL_SUFFIX_FORMATTER.format(inum),
                        endpoint_args='',
                        data_fn=None,
                        data={}
                    )
                    scope_result = scope_response.json()
                except Exception:
                    display_name = 'None'

                display_name = scope_result.get(
                    'displayName') or scope_result.get('inum')

                if display_name:
                    scopes_of_resource.append(display_name)
                else:
                    scopes_of_resource.append(str(d.get('scopes', [''])[0]))
            data.append(
                [
                    d.get('id'),
                    str(d.get('description', '')),
                    ','.join(scopes_of_resource)
                ]
            )

        if data:
            self.uma_resources = HSplit([
                JansVerticalNav(
                    myparent=self.myparent,
                    headers=['id', 'Description', 'Scopes'],
                    preferred_size=[36, 0, 0],
                    data=data,
                    on_enter=self.view_uma_resources,
                    on_display=self.myparent.data_display_dialog,
                    on_delete=self.delete_uma_resource,
                    selectes=0,
                    headerColor='class:outh-client-navbar-headcolor',
                    entriesColor='class:outh-client-navbar-entriescolor',
                    all_data=result,
                    underline_headings=False,
                ),
            ])

            APP.invalidate()
            self.myparent.layout.focus(self.uma_resources)

        else:
            self.uma_resources = HSplit([], width=D())
            self.myparent.show_message(_("Oops"), _(
                "No matching result"), tobefocused=self.resources.children[0].children[0])

    def client_dialog_nav_selection_changed(
        self,
        selection: str
    ) -> None:
        """This method for client navigation bar when value is changed

        Args:
            selection (str): the New Value from the nav-bar
        """

        self.left_nav = selection

    def view_uma_resources(self, **params: Any) -> None:
        """This method view the UMA resources in a dialog
        """

        selected_line_data = params['data']  # self.uma_result
        title = _("Edit user Data (Clients)")

        dialog = ViewUMADialog(self.myparent, title=title,
                               data=selected_line_data, deleted_uma=self.delete_uma_resource)

        self.myparent.show_jans_dialog(dialog)

    def __pt_container__(self) -> Dialog:
        """The container for the dialog itself

        Returns:
            Dialog: The Edit Client Dialog
        """

        return self.dialog
