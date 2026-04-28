import os
import json
import asyncio
import ssl
import jwt

from urllib.parse import urlparse

from prompt_toolkit.application import Application
from prompt_toolkit.layout.containers import HSplit
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.widgets import Button, Frame
from prompt_toolkit.formatted_text import HTML, merge_formatted_text

from utils.multi_lang import _
from cli import config_cli
from utils.utils import DialogUtils
from utils.static import cli_style, common_strings
from wui_components.jans_cli_dialog import JansGDialog


class SSAError(Exception):
    """Exception raised if there is an issue with SSA."""
    pass


class Plugin(DialogUtils):
    """This is a general class for plugins 
    """
    def __init__(
        self, 
        app: Application
        ) -> None:
        """init for Plugin class "Jans CLI Menu"

        Args:
            app (Generic): The main Application class
        """
        self.app = app
        self.pid = 'jans-menu'
        self.name = '[J]ans TUI'

        self.menu_container = Frame(
                            body=HSplit([
                                Button(text=_("Exit Jans TUI"), handler=self.exit_cli),
                                Button(text=_("Logout and Exit Jans TUI"), handler=self.logout_exit_cli),
                                Button(text=_("Configure Jans TUI"), handler=self.configure_cli),
                                Button(text=_("Create client with SSA"), handler=self.create_ssa_client_window),
                                Button(text=_("Clear Configuration and Exit"), handler=self.clear_config),
                                Button(text=_("Application Versions"), handler=self.app_versions),
                            ],
                            width=D()
                        ),
                        height=D()
                        )

    def process(self) -> None:
        pass

    def set_center_frame(self) -> None:
        """center frame content
        """

        self.app.center_container = self.menu_container

    def exit_cli(self) -> None:
        """Exits
        """
        self.app.exit(result=False)

    def logout_exit_cli(self, clear_all=False) -> None:
        """Removes auth token and exits
        """

        if clear_all:
            for key in list(config_cli.config['DEFAULT'].keys()):
                config_cli.config.remove_option('DEFAULT', key)
        else:
            config_cli.config['DEFAULT'].pop('access_token_enc', None)
            config_cli.config['DEFAULT'].pop('user_data', None)
        config_cli.write_config()

        async def coroutine():
            self.app.start_progressing()
            try:
                await self.app.loop.run_in_executor(self.app.executor, self.app.cli_object.revoke_session)
            finally:
                self.app.stop_progressing()
                self.exit_cli()
 

        asyncio.ensure_future(coroutine())


    def configure_cli(self) -> None:
        """Configures CLI creds
        """
        self.app.jans_creds_dialog()


    def app_versions(self) -> None:
        """Display Jannssen application versions
        """

        async def coroutine():

            try:
                response = self.app.cli_object.process_command_by_id(
                        operation_id='get-app-version',
                        url_suffix='',
                        endpoint_args='',
                        data_fn=None,
                        data={}
                        )
                app_status = response.json()
            except Exception as e:
                self.app.show_message(_("Error application versions"), str(e), tobefocused=self.app.center_container)
                return

            version_msgs = []
            for app in app_status:
                version_msgs.append(HTML(f"<b>{app['title']}</b>\n"))
                version_msgs.append(HTML(f"<b>Version:</b> {app['version']}\n"))
                version_msgs.append(HTML(f"<b>Build Date:</b> {app['buildDate']}\n"))
                version_msgs.append(HTML(f"<b>Build Version:</b> {app['build']}\n"))
                version_msgs.append(HTML("\n"))

            self.app.show_message(
                _("Jannsen Application Versions"),
                merge_formatted_text(version_msgs),
                tobefocused=self.app.center_container
                )

        asyncio.ensure_future(coroutine())


    def validate_ssa(self, ssa, issuer_url):

        # get openid configuration
        open_id_url = f'{issuer_url}/{config_cli.AUTH_DISCOVERY_ENDPOINT}'
        try:
            response = config_cli.session.get(
                url=open_id_url,
                verify=False if config_cli.args.noverify else True,
                timeout=30,
                )
            response.raise_for_status()
        except Exception as e:
            raise SSAError(_("Error while retrieving openID Configuration from {}").format(open_id_url)) from e

        try:
            open_id_configuration = response.json()
        except json.JSONDecodeError as e:
            raise SSAError(_("Error while retrieving OpenID Configuration from {}").format(open_id_url)) from e

        jwks_uri = open_id_configuration.get('jwks_uri')

        if not jwks_uri:
            raise SSAError(_("jwks_uri is not found in OpenID Configuration"))

        if config_cli.args.noverify:
            ssl_context = ssl._create_unverified_context()
        else:
            ssl_context = ssl.create_default_context()

        jwks_client = jwt.PyJWKClient(jwks_uri, ssl_context=ssl_context)

        try:
            signing_key = jwks_client.get_signing_key_from_jwt(ssa)
            decoded = jwt.decode(
                ssa,
                signing_key.key,
                algorithms=["RS256"],
                options={"verify_aud": False},
                issuer=issuer_url
            )
        except jwt.exceptions.PyJWTError as e:
            raise SSAError(_("SSA validation failed: {}").format(e)) from e

        ssa_grant_types = decoded.get('grant_types') or []
        missing_grant_types = list({'authorization_code', 'refresh_token', 'client_credentials', 'urn:ietf:params:oauth:grant-type:device_code'} - set(ssa_grant_types))

        if missing_grant_types:
            raise SSAError(_("SSA is missing these grant types: {}").format(', '.join(missing_grant_types)))

        return decoded


    def _build_registration_payload(self, issuer_url, ssa):
        payload = {
              "client_name": f"SSA Created TUI Client {os.urandom(3).hex()}",
              "redirect_uris": [
                f"{issuer_url}/admin"
              ],
              "software_statement": ssa,
              "grant_types": [
                "authorization_code",
                "refresh_token",
                "client_credentials",
                "urn:ietf:params:oauth:grant-type:device_code"
              ],
              "userinfo_signed_response_alg": "RS256",
              "access_token_lifetime": 2592000,
              "scope": "jans_stat offline_access profile email openid https://jans.io/auth/ssa.admin",
              "run_introspection_script_before_jwt_creation": False,
              "update_token_script_dns": ["inum=2D3E.5A04,ou=scripts,o=jans"]
            }

        return payload


    async def _create_client_coroutine(self, client_creation_data, issuer_url, issuer_host, ssa):
        self.app.start_progressing(_("Creating client..."))
        error = None
        response = None
        try:
            await self.app.loop.run_in_executor(
                self.app.executor,
                self.validate_ssa,
                ssa,
                issuer_url,
            )
            response = await self.app.loop.run_in_executor(
                self.app.executor,
                lambda: config_cli.session.post(
                    url=f'{issuer_url}/jans-auth/restv1/register',
                    data=json.dumps(client_creation_data),
                    headers={'Content-Type': 'application/json'},
                    verify=False if config_cli.args.noverify else True,
                    timeout=30,
                ),
            )
        except SSAError as e:
            error = str(e)
        except Exception as e:
            error = _("Error while creating client from SSA: {}").format(e)
        finally:
            self.app.stop_progressing()

        if error:
            self.app.show_message(_(common_strings.error), error, tobefocused=self.menu_container)
            return

        if response is None:
            self.app.show_message(_(common_strings.error), _("Error while creating client from SSA: empty server response"), tobefocused=self.menu_container)
            return

        if response.status_code not in (200, 201):
            self.app.show_message(
                _(common_strings.error),
                _("Server {} returned error.\nStatus code: {}.\nResponse text: {}").format(
                    issuer_url, response.status_code, response.text
                ),
                tobefocused=self.menu_container,
            )
            return

        try:
            cli_info = response.json()
        except json.JSONDecodeError as e:
            self.app.show_message(_(common_strings.error), _("Error while creating client from SSA: {}\nResponse was {}").format(e, response.text), tobefocused=self.menu_container)
            return

        jca_client_id = cli_info.get('client_id')
        jca_client_secret = cli_info.get('client_secret')

        if not (jca_client_id and jca_client_secret):
            self.app.show_message(_(common_strings.error), _("client_id or client_secret is not in server response"), tobefocused=self.menu_container)
            return

        creds_info = {
            'jans_host': issuer_host,
            'jca_client_id': jca_client_id,
            'jca_client_secret': jca_client_secret
            }

        self.app.save_creds(creds_info)
        self.app.create_cli()


    def create_ssa_cli(self, dialog):
        dialog_data = self.make_data_from_dialog({'ssa': dialog.body})
        ssa = dialog_data['ssa']
        iss = dialog_data['iss']

        if not iss:
            self.app.show_message(_(common_strings.error), _("Issuer was not entered"), tobefocused=dialog.buttons[0])
            return

        issuer_url = iss.rstrip('/')
        parsed_iss = urlparse(issuer_url)

        if not (parsed_iss.scheme and parsed_iss.netloc):
            self.app.show_message(_(common_strings.error), _("Please enter valid issuer"), tobefocused=dialog.buttons[0])
            return

        if not ssa:
            self.app.show_message(_(common_strings.error), _("No SSA was entered"), tobefocused=dialog.buttons[0])
            return

        dialog.close()
        self.set_center_frame()

        client_creation_data = self._build_registration_payload(issuer_url, ssa)

        asyncio.ensure_future(
            self._create_client_coroutine(client_creation_data, issuer_url, parsed_iss.netloc, ssa)
        )


    def create_ssa_client_window(self, dialog=None):

        body = HSplit([
            self.app.getTitledText(
                title=_("Issuer"),
                name='iss',
                jans_help=_("Issuer of SSA"),
                style=cli_style.edit_text_required
            ),
            self.app.getTitledText(
                title=_("SSA"),
                name='ssa',
                height=8,
                jans_help=_("SSA for creating TUI Client"),
                style=cli_style.edit_text_required
            ),
        ])

        create_client_button_label = _("Create Client")
        create_client_button = Button(create_client_button_label, handler=self.create_ssa_cli, width=len(create_client_button_label)+4)
        create_client_button.keep_dialog = True
        buttons = [
            create_client_button,
            Button(_("Cancel"))
        ]
        mydialog = JansGDialog(self.app, title=_("SSA for Creating Client"), body= body, buttons=buttons)
        self.app.show_jans_dialog(mydialog)

    def clear_config(self):
        def do_clear_config(dialog):
            self.logout_exit_cli(clear_all=True)

        confirm_dialog = self.app.get_confirm_dialog(
            message=_("Are you sure you want to clear the TUI configuration? You will permanently lose session and configurations."),
            confirm_handler=do_clear_config
        )
        self.app.show_jans_dialog(confirm_dialog)
