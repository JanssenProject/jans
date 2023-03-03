import asyncio

from prompt_toolkit.application import Application
from prompt_toolkit.eventloop import get_event_loop
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.layout.containers import HSplit, VSplit, HorizontalAlign, Window
from prompt_toolkit.widgets import Button

from utils.multi_lang import _
from utils.utils import DialogUtils
from utils.static import cli_style
from wui_components.jans_drop_down import DropDownWidget

class Defaults(DialogUtils):
    def __init__(
        self, 
        app: Application
        ) -> None:

        self.app = self.myparent = app

        self.acr_values_widget = DropDownWidget(
                                            values=[],
                                            select_one_option=False,
                                            )
        self.main_container =  HSplit([
                                        self.app.getTitledWidget(
                                        _("Default ACR"),
                                        name='defaultAcr',
                                        widget=self.acr_values_widget,
                                        jans_help=_("Set default authentication context class reference"),
                                        style=cli_style.drop_down
                                        ),
                                        Window(height=1),
                                        VSplit([Button(_("Save"), handler=self.save_defaults)], padding=5, align=HorizontalAlign.CENTER)
                                    ],
                                    width=D()
                                    )


    def on_cli_object_ready(self) -> None:
        self.populate_acr_values()
        self.app.create_background_task(self.get_default_acr())

    def populate_acr_values(self):
        self.acr_values_widget.values = [(acr, acr) for acr in self.app.cli_object.openid_configuration['acr_values_supported']]

    async def get_default_acr(self) -> None:
        response = self.app.cli_requests({'operation_id': 'get-acrs'})
        if response.ok:
            result = response.json()
            self.default_acr = result.get('defaultAcr', 'basic')
            self.acr_values_widget.value = self.default_acr

    def save_defaults(self) -> None:
        if self.default_acr == self.acr_values_widget.value:
            return

        async def coroutine():
            # save default acr
            cli_args = {'operation_id': 'put-acrs', 'data': {'defaultAcr': self.acr_values_widget.value}}
            self.app.start_progressing(_("Saving default ACR..."))
            await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing()

        asyncio.ensure_future(coroutine())
