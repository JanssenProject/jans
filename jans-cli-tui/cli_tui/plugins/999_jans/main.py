import os
import sys
import asyncio

from typing import Sequence


from prompt_toolkit.application import Application
from prompt_toolkit.layout.containers import HSplit, VSplit, Window, Float
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.widgets import Button, Label, Frame
from prompt_toolkit.formatted_text import HTML
from prompt_toolkit.widgets import Shadow
from prompt_toolkit.layout.controls import FormattedTextControl


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
            app (_type_): _description_
        """
        self.app = app
        self.pid = 'jans-menu'
        self.name = '[J]ans Cli'

        self.menu_container = Frame(
                            body=HSplit([
                                Button(text=_("Exit Jans CLI"), handler=self.exit_cli),
                                Button(text=_("Logout and Exit Jans CLI"), handler=self.logout_exit_cli),
                                Button(text=_("Configure Jans CLI"), handler=self.configure_cli),
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

        del config_cli.config['DEFAULT']['access_token_enc']
        del config_cli.config['DEFAULT']['user_data']
        config_cli.write_config()
        self.exit_cli()

    def configure_cli(self) -> None:
        """Configures CLI creds
        """
        self.app.jans_creds_dialog()
