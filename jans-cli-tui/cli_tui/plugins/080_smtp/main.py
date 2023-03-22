import asyncio
from collections import OrderedDict
from functools import partial
from typing import Any

import prompt_toolkit
from prompt_toolkit.eventloop import get_event_loop
from prompt_toolkit.layout.containers import HSplit, DynamicContainer, VSplit, Window, HorizontalAlign
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.widgets import Frame, Button, Label, Box, Dialog
from prompt_toolkit.application import Application
from wui_components.jans_nav_bar import JansNavBar
from wui_components.jans_drop_down import DropDownWidget
from wui_components.jans_vetrical_nav import JansVerticalNav
from wui_components.jans_cli_dialog import JansGDialog
from utils.multi_lang import _
from utils.utils import DialogUtils
from utils.static import cli_style


class Plugin(DialogUtils):
    """This is a general class for plugins 
    """
    def __init__(
        self, 
        app: Application
        ) -> None:
        """init for Plugin class "SMTP"

        Args:
            app (Generic): The main Application class
        """
        self.app = app
        self.pid = 'smtp'
        self.name = 'SMT[P]'
        self.server_side_plugin = False
        self.page_entered = False
        self.data = {}
        self.edit_container = HSplit([])
        self.prepare_container()

    def process(self) -> None:
        pass

    def init_plugin(self) -> None:
        """The initialization for this plugin
        """
        pass

    def prepare_container(self):

        self.main_container = HSplit([

                        self.app.getTitledText(_("SMTP Host"), name='host', style=cli_style.edit_text),
                        self.app.getTitledText(_("SMTP Port"), name='port', text_type='integer', style=cli_style.edit_text),
                        self.app.getTitledCheckBox(_("Reqire SSL"), name='requires_ssl', style=cli_style.edit_text),
                        self.app.getTitledText(_("From Name"), name='from_name', style=cli_style.edit_text),
                        self.app.getTitledText(_("From Email Address"), name='from_email_address', style=cli_style.edit_text),
                        self.app.getTitledCheckBox(_("Requires Authentication"), name='requires_authentication', style=cli_style.check_box),
                        self.app.getTitledText(_("SMTP User Name"), name='user_name', style=cli_style.edit_text),
                        self.app.getTitledText(_("SMTP Password"), name='password', style=cli_style.edit_text),
                        self.app.getTitledCheckBox(_("Trust Server"), name='trust_host', style=cli_style.edit_text),
                        VSplit([
                                Button(_("Save"), handler=self.save_config),
                                Button(_("Test"), handler=self.test_config),
                                ],
                                padding=5, align=HorizontalAlign.CENTER, width=D()),
                        ])

    def on_page_enter(self):
        if not self.data:
            self.app.create_background_task(self.get_smtp_config())

    async def get_smtp_config(self) -> None:
        self.app.start_progressing(_("Retreiving smtp configuration..."))
        response = await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, {'operation_id': 'get-config-smtp'})
        self.app.stop_progressing()
        self.data = response.json()

        for item in self.main_container.children:
            if hasattr(item, 'me'):
                if isinstance(item.me, prompt_toolkit.widgets.base.TextArea):
                    item.me.text = str(self.data.get(item.me.window.jans_name, ''))
                elif isinstance(item.me, prompt_toolkit.widgets.base.Checkbox):
                    item.me.checked = self.data.get(item.me.window.jans_name, '')


    def save_config(self) -> None:
        """This method saves STMP configuration
        """
        new_data = self.make_data_from_dialog(tabs={'smtp': self.main_container})
        self.data.update(new_data)

        operation_id = 'put-config-smtp'

        async def coroutine():
            cli_args = {'operation_id': operation_id, 'data': self.data}
            self.app.start_progressing(_("Saving SMTP Configuration..."))
            await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing(_("SMTP Configuration was saved."))

        asyncio.ensure_future(coroutine())


    def test_config(self) -> None:
        """This method tests SMTP configuration
        """
        new_data = self.make_data_from_dialog(tabs={'smtp': self.main_container})

        open("/tmp/a.txt", "w").write(str(new_data)+'\n')

        async def coroutine():
            cli_args = {'operation_id': 'test-config-smtp', 'data': new_data}
            self.app.start_progressing(_("Testing SMTP Configuration..."))
            response = await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing(_("SMTP Configuration test was completed."))
            open("/tmp/a.txt", "a").write(str(response.status_code)+'\n')
            open("/tmp/a.txt", "a").write(str(response.text)+'\n')


        asyncio.ensure_future(coroutine())



    def set_center_frame(self) -> None:
        """center frame content
        """
        self.app.center_container = self.main_container


