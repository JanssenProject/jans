import asyncio
from prompt_toolkit.application import Application
from prompt_toolkit.layout.containers import HSplit
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.widgets import Button, Frame
from prompt_toolkit.formatted_text import HTML, merge_formatted_text

from utils.multi_lang import _
from cli import config_cli

class Plugin:
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

    def logout_exit_cli(self) -> None:
        """Removes auth token and exits
        """

        async def coroutine():
            self.app.start_progressing()
            response = await self.app.loop.run_in_executor(self.app.executor, self.app.cli_object.revoke_session)
            self.app.stop_progressing()

        asyncio.ensure_future(coroutine())

        config_cli.config['DEFAULT'].pop('access_token_enc', None)
        config_cli.config['DEFAULT'].pop('user_data', None)
        config_cli.write_config()
        self.exit_cli()

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
