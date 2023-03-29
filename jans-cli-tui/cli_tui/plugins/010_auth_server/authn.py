import asyncio
from typing import Any

from prompt_toolkit.application import Application
from prompt_toolkit.eventloop import get_event_loop
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.layout.containers import HSplit, VSplit, HorizontalAlign, Window
from prompt_toolkit.widgets import Button, Label

from utils.multi_lang import _
from utils.utils import DialogUtils
from utils.static import cli_style, common_strings
from wui_components.jans_vetrical_nav import JansVerticalNav
from wui_components.jans_drop_down import DropDownWidget
from wui_components.jans_cli_dialog import JansGDialog
from wui_components.jans_spinner import Spinner


BUILTIN_AUTHN = 'simple_password_auth'
BUILTIN_SAML = 'urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport'

class Authn(DialogUtils):
    def __init__(
        self, 
        app: Application
        ) -> None:

        self.app = app
        self.default_acr = None

        self.main_container = JansVerticalNav(
                myparent=app,
                headers=[_("ACR"), _("SAML ACR"), _("Level"), _("Default")],
                preferred_size= self.app.get_column_sizes(.2, .5 , .15, .15),
                selectes=0,
                headerColor=cli_style.navbar_headcolor,
                entriesColor=cli_style.navbar_entriescolor,
                on_enter=self.edit_acr,
            )

        self.main_container.on_page_enter = self.on_page_enter

    def on_cli_object_ready(self):
        if not self.app.cli_object.openid_configuration:
            self.app.retreive_openid_configuration()
        self.app.create_background_task(self.get_default_acr())


    def on_page_enter(self) -> None:


        def populate_acr_list():

            self.main_container.clear()
            self.main_container.add_item((BUILTIN_AUTHN, BUILTIN_SAML, ' -1', 'X' if self.default_acr == BUILTIN_AUTHN else ' '))

            # LDAP Servers
            for ldap_server in self.ldap_servers:
                self.main_container.add_item((
                    ldap_server['configId'],
                    BUILTIN_SAML,
                    str(ldap_server['level']).rjust(3),
                    'X' if self.default_acr == ldap_server['configId'] else ' '
                    ))

            self.main_container.all_data = self.main_container.data[:]
            self.app.layout.focus(self.main_container)


        async def coroutine():
            # retreive auth ldap servers
            cli_args = {'operation_id': 'get-config-database-ldap'}
            self.app.start_progressing(_("Retreiving LDAP Configurations..."))
            result = await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing()
            self.ldap_servers = result.json()
            populate_acr_list()

        asyncio.ensure_future(coroutine())


    def edit_acr(self, **params: Any) -> None:
        """This Method displays editing ACR
        """

        acr = params['passed'][0]

        if acr == BUILTIN_AUTHN:
            self.simple_password_auth_dialog(params['passed'])
        elif acr in [ldap_server['configId'] for ldap_server in self.ldap_servers]:
            self.ldap_server_dialog(acr)

    def update_acr_list(self):

        if self.app.cli_object.openid_configuration:
            acr_values = [(acr, acr) for acr in self.app.cli_object.openid_configuration['acr_values_supported']]
            if not 'simple_password_auth' in self.app.cli_object.openid_configuration['acr_values_supported']:
                acr_values.imsert((BUILTIN_AUTHN, BUILTIN_AUTHN))
            self.acr_values_widget.values = acr_values
            if hasattr(self, 'default_acr'):
                self.acr_values_widget.value = self.default_acr

        else:
            self.app.retreive_openid_configuration(self.populate_acr_values)

    async def get_default_acr(self) -> None:
        response = self.app.cli_requests({'operation_id': 'get-acrs'})
        if response.ok:
            result = response.json()
            self.default_acr = result.get('defaultAcr', BUILTIN_AUTHN)

    def simple_password_auth_dialog(self, acr_item: list) -> None:


        level = Spinner(value=-1, min_value=-1, max_value=-1)
        hashl_alg = DropDownWidget(values=[('bcrypt','bcrypt')], value='bcrypt', select_one_option=False)
        default_acr = self.default_acr == 'simple_password_auth'
        body = HSplit([
                self.app.getTitledText(title="ACR", value='simple_password_auth', name='acr', read_only=True),
                self.app.getTitledWidget(title=_("Level"), widget=level, name='level'),
                self.app.getTitledCheckBox(title=_("Default Authn Method"), checked = default_acr, name='default'),
                self.app.getTitledText(title="SAML ACR", value=acr_item[1], name='urn', read_only=True),
                self.app.getTitledText(title=_("Description"), value="Built-in default password authentication", name='urn', read_only=True),
                self.app.getTitledText(title=_("Primary Key"), value="uid", name='primary_key', read_only=True),
                self.app.getTitledText(title=_("Password Attribute"), value="userPassword", name='password_attribute', read_only=True),
                self.app.getTitledWidget(title=_("Hash Algorithm"), widget=hashl_alg, name='hashl_alg'),
                ],
                width=D()
                )


        def save_default(dialog):
            data = self.make_data_from_dialog({'acr': dialog.body})
            if data['default'] and data['default'] != default_acr:
                self.save_default_acr(BUILTIN_AUTHN)

        buttons = [Button(_("Save"), handler=save_default), Button(_("Cancel"))]
        dialog = JansGDialog(self.app, body=body, title=acr_item[0], buttons=buttons, width=self.app.dialog_width)
        self.app.show_jans_dialog(dialog)


    def ldap_server_dialog(self, acr):
        for ldap_server in self.ldap_servers:
            if ldap_server['configId'] == acr:
                config = ldap_server
                break

        level = Spinner(value=config['level'], min_value=0, max_value=99)
        default_acr = acr == self.default_acr
        body = HSplit([
                self.app.getTitledText(title="ACR", value=config['configId'], name='configId'),
                self.app.getTitledWidget(title=_("Level"), widget=level, name='level'),
                self.app.getTitledCheckBox(title=_("Default Authn Method"), checked=default_acr, name='default'),
                self.app.getTitledText(title="Bind DN", value=config['bindDN'], name='bindDN'),
                self.app.getTitledText(title=_("Max Connections"), text_type='integer', value=config['maxConnections'], name='maxConnections'),
                self.app.getTitledText(title="Remote Primary Key", value=config['primaryKey'], name='primaryKey'),
                self.app.getTitledText(title="Local Primary Key", value=config['localPrimaryKey'], name='localPrimaryKey'),
                self.app.getTitledText(title="Remote LDAP server:port", value='\n'.join(config['servers']), height=2, name='servers', jans_list_type=True),
                self.app.getTitledText(title="Base DNs", value='\n'.join(config['baseDNs']), height=2, name='baseDNs', jans_list_type=True),
                self.app.getTitledText(title="Bind Password", value=config['bindPassword'], name='bindPassword'),
                self.app.getTitledCheckBox(title=_("Use SSL"), checked=config['useSSL'] , name='useSSL'),
                self.app.getTitledCheckBox(title=_("Enabled"), checked=config['enabled'] , name='enabled'),
                ],
                width=D()
                )


        def test_ldap(dialog):
            data = self.make_data_from_dialog({'acr': dialog.body})
            data['version'] = 0
            data['useAnonymousBind'] = False
            data.pop('default', False)

            async def coroutine():
                # save default acr
                cli_args = {'operation_id': 'post-config-database-ldap-test', 'data': data}
                self.app.start_progressing(_("Testing LDAP Configuration..."))
                result = await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
                self.app.stop_progressing()

                
                if result.status_code == 200 and result.text == 'true':
                    ok_button = Button(_("OK"), lambda: dialog.future.set_result(True))
                    self.app.show_message(title=_("Success"), message=_("LDAP Configuration test was successfull"), buttons=[ok_button])
                else:
                    self.app.show_message(title=_(common_strings.error), message=_("LDAP Configuration test was failed"))

            asyncio.ensure_future(coroutine())

            


        def save_default(dialog):
            data = self.make_data_from_dialog({'acr': dialog.body})
            if data['default'] and data['default'] != default_acr:
                self.save_default_acr(BUILTIN_AUTHN)

        test_button = Button(_("Test"), test_ldap)
        test_button.keep_dialog = True
        buttons = [Button(_("Save"), handler=save_default), test_button, Button(_("Cancel"))]
        dialog = JansGDialog(self.app, body=body, title=acr, buttons=buttons, width=self.app.dialog_width)
        self.app.show_jans_dialog(dialog)



    def save_default_acr(self, acr):

        async def coroutine():
            # save default acr
            cli_args = {'operation_id': 'put-acrs', 'data': {'defaultAcr': acr}}
            self.app.start_progressing(_("Saving default ACR..."))
            await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing()

        asyncio.ensure_future(coroutine())
