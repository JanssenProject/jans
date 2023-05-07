import asyncio
from collections import OrderedDict
from functools import partial
from typing import Any

import prompt_toolkit
from prompt_toolkit.eventloop import get_event_loop
from prompt_toolkit.layout.containers import HSplit, DynamicContainer,\
    VSplit, Window, HorizontalAlign, Window
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.widgets import Frame, Button, Label, Box, Dialog
from prompt_toolkit.application import Application
from wui_components.jans_nav_bar import JansNavBar
from wui_components.jans_drop_down import DropDownWidget
from wui_components.jans_vetrical_nav import JansVerticalNav
from wui_components.jans_cli_dialog import JansGDialog
from utils.multi_lang import _
from utils.utils import DialogUtils
from utils.static import cli_style, common_strings


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


    def init_plugin(self) -> None:
        """The initialization for this plugin
        """
        pass

    def prepare_container(self):

        self.host_widget = self.app.getTitledText(_("SMTP Host"), name='host', style=cli_style.edit_text, widget_style=cli_style.black_bg_widget)
        self.port_widget = self.app.getTitledText(_("SMTP Port"), name='port', text_type='integer', style=cli_style.edit_text, widget_style=cli_style.black_bg_widget)
        self.connect_protection_widget = self.app.getTitledRadioButton(
                    _("Connect Protection"),
                    name='connect_protection',
                    values=[(v,v) for v in ('None', 'StartTls', 'SslTls')],
                    style=cli_style.edit_text,
                    widget_style=cli_style.black_bg_widget
                    )
        self.from_name_widget = self.app.getTitledText(_("From Name"), name='from_name', style=cli_style.edit_text, widget_style=cli_style.black_bg_widget)
        self.from_email_address_widget = self.app.getTitledText(_("From Email Address"), name='from_email_address', style=cli_style.edit_text, widget_style=cli_style.black_bg_widget)
        self.requires_authentication_widget = self.app.getTitledCheckBox(_("Requires Authentication"), name='requires_authentication', style=cli_style.check_box, widget_style=cli_style.black_bg_widget)
        self.smtp_authentication_account_username_widget = self.app.getTitledText(_("SMTP User Name"), name='smtp_authentication_account_username', style=cli_style.edit_text, widget_style=cli_style.black_bg_widget)
        self.smtp_authentication_account_password_widget = self.app.getTitledText(_("SMTP Password"), name='smtp_authentication_account_password', style=cli_style.edit_text, widget_style=cli_style.black_bg_widget)
        self.trust_host_widget = self.app.getTitledCheckBox(_("Trust Server"), name='trust_host', style=cli_style.edit_text, widget_style=cli_style.black_bg_widget)

        self.key_store_widget = self.app.getTitledText(_("Keystore"), name='key_store', style=cli_style.edit_text, widget_style=cli_style.black_bg_widget)
        self.key_store_password_widget = self.app.getTitledText(_("Keystore Password"), name='key_store_password', style=cli_style.edit_text, widget_style=cli_style.black_bg_widget)
        self.key_store_alias_widget = self.app.getTitledText(_("Keystore Alias"), name='key_store_alias', style=cli_style.edit_text, widget_style=cli_style.black_bg_widget)
        self.signing_algorithm_widget = self.app.getTitledText(_("Keystore Signing Alg"), name='signing_algorithm', style=cli_style.edit_text, widget_style=cli_style.black_bg_widget)


        self.main_container = HSplit([
                        self.host_widget,
                        self.port_widget,
                        self.connect_protection_widget,
                        self.from_name_widget,
                        self.from_email_address_widget,
                        self.requires_authentication_widget,
                        self.smtp_authentication_account_username_widget,
                        self.smtp_authentication_account_password_widget,
                        self.trust_host_widget,
                        Window(height=1),
                        self.key_store_widget,
                        self.key_store_password_widget,
                        self.key_store_alias_widget,
                        self.signing_algorithm_widget,
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

        self.host_widget.me.text = self.data.get('host', '')
        self.port_widget.me.text = str(self.data.get('port', ''))
        self.connect_protection_widget.me.current_value = self.data.get('connect_protection', '')
        self.from_name_widget.me.text = self.data.get('from_name', '')
        self.from_email_address_widget.me.text = self.data.get('from_email_address', '')
        self.requires_authentication_widget.me.checked = self.data.get('requires_authentication', False)
        self.smtp_authentication_account_username_widget.me.text = self.data.get('smtp_authentication_account_username', '')
        self.smtp_authentication_account_password_widget.me.text = self.data.get('smtp_authentication_account_password', '')
        self.trust_host_widget.me.checked = self.data.get('trust_host', False)

        self.key_store_widget.me.text = self.data.get('key_store', '')
        self.key_store_password_widget.me.text = self.data.get('key_store_password', '')
        self.key_store_alias_widget.me.text = self.data.get('key_store_alias', '')
        self.signing_algorithm_widget.me.text = self.data.get('signing_algorithm', '')

    def save_config(self) -> None:
        """This method saves STMP configuration
        """
        self.data = self.make_data_from_dialog(tabs={'smtp': self.main_container})

        operation_id = 'put-config-smtp'

        async def coroutine():
            cli_args = {'operation_id': operation_id, 'data': self.data}
            self.app.start_progressing(_("Saving SMTP Configuration..."))
            await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing(_("SMTP Configuration was saved."))
            await self.get_smtp_config()

        asyncio.ensure_future(coroutine())


    def test_config(self) -> None:
        """This method tests SMTP configuration
        """

        async def coroutine():
            cli_args = {'operation_id': 'test-config-smtp', 'data': {'sign': True, 'subject': "SMTP Configuration verification", 'message': "Mail to test SMTP configuration"}}
            self.app.start_progressing(_("Testing SMTP Configuration..."))
            response = await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing(_("SMTP Configuration test was completed."))

            if response.status_code == 200 and response.json() == True:
                self.app.show_message(_(common_strings.info), _("SMTP configuration test was successfull."), tobefocused=self.main_container)
            else:
                self.app.show_message(_(common_strings.info), _("SMTP configuration test was failed:\nStatus code: {}\nServer response: {}").format(response.status_code, response.json()), tobefocused=self.main_container)

        asyncio.ensure_future(coroutine())



    def set_center_frame(self) -> None:
        """center frame content
        """
        self.app.center_container = self.main_container


