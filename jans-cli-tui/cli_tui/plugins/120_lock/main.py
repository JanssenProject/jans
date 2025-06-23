import copy
import asyncio

from typing import Any, Optional
from prompt_toolkit.layout.containers import HSplit, DynamicContainer,\
    VSplit, Window, HorizontalAlign

from prompt_toolkit.layout import ScrollablePane
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.widgets import Button, Frame
from prompt_toolkit.application import Application
from wui_components.widget_collections import get_logging_level_widget

from utils.multi_lang import _
from utils.utils import DialogUtils, common_data
from utils.static import cli_style, common_strings


class Plugin(DialogUtils):
    """This is a general class for plugins 
    """
    def __init__(
        self, 
        app: Application
        ) -> None:
        """init for Plugin class "Lock"

        Args:
            app (Generic): The main Application class
        """
        self.app = app
        self.pid = 'lock'
        self.name = 'Jans Lock'
        self.server_side_plugin = True
        self.data = {}
        self.main_container = DynamicContainer(lambda: self.working_container)


    def init_plugin(self) -> None:
        """The initialization for this plugin
        """

        self.app.create_background_task(self.get_configuration())


    def create_widgets(self):
        self.schema = self.app.cli_object.get_schema_from_reference('Lock', '#/components/schemas/AppConfiguration')

        self.working_container = HSplit([

                common_data.app.getTitledText(
                    title=_("Base DN"),
                    name='baseDN',
                    value=self.data.get('baseDN'),
                    style=cli_style.edit_text,
                    jans_help=_("Base DN"),
                    widget_style=cli_style.black_bg_widget
                ),

                common_data.app.getTitledText(
                    title=_("Token Channels"),
                    name='tokenChannels',
                    value=' '.join(self.data.get('tokenChannels', [])),
                    style=cli_style.edit_text,
                    jans_help=_("Space seperated token channels"),
                    jans_list_type=True,
                    widget_style=cli_style.black_bg_widget
                ),

                common_data.app.getTitledCheckBox(
                    title=_("Disable JDK Logger"),
                    name='disableJdkLogger', 
                    checked=self.data.get('disableJdkLogger', False),
                    style=cli_style.check_box,
                    widget_style=cli_style.black_bg_widget
                ),

                get_logging_level_widget(self.data.get('loggingLevel', 'INFO')),

                common_data.app.getTitledText(
                    title=_("Logging Layout"),
                    name='loggingLayout',
                    value=self.data.get('loggingLayout', 'text'),
                    style=cli_style.edit_text,
                    jans_help=_("Logging layout"),
                    widget_style=cli_style.black_bg_widget
                ),

                common_data.app.getTitledText(
                    title=_("External Logger Configuration"),
                    name='externalLoggerConfiguration',
                    value=self.data.get('externalLoggerConfiguration'),
                    style=cli_style.edit_text,
                    jans_help=_("Configuration for External Logger"),
                    widget_style=cli_style.black_bg_widget
                ),

                common_data.app.getTitledCheckBox(
                    title=_("Enable Metric Reporter"),
                    name='metricReporterEnabled', 
                    checked=self.data.get('metricReporterEnabled', False),
                    style=cli_style.check_box,
                    widget_style=cli_style.black_bg_widget
                ),

                common_data.app.getTitledText(
                    title=_("Metric Reporter Interval"),
                    name='metricReporterInterval',
                    value=self.data.get('metricReporterInterval', '300'),
                    style=cli_style.edit_text,
                    widget_style=cli_style.black_bg_widget,
                    text_type='integer'
                ),

                common_data.app.getTitledText(
                    title=_("Metric Reporter Keep Data Days"),
                    name='metricReporterKeepDataDays',
                    value=self.data.get('metricReporterKeepDataDays', '15'),
                    style=cli_style.edit_text,
                    widget_style=cli_style.black_bg_widget,
                    text_type='integer'
                ),

                common_data.app.getTitledText(
                    title=_("Clean Service Interval"),
                    name='cleanServiceInterval',
                    value=self.data.get('cleanServiceInterval', '60'),
                    style=cli_style.edit_text,
                    widget_style=cli_style.black_bg_widget,
                    text_type='integer'
                ),

                common_data.app.getTitledText(
                    title=_("Metric Channel"),
                    name='metricChannel',
                    value=self.data.get('metricChannel', ''),
                    style=cli_style.edit_text,
                    widget_style=cli_style.black_bg_widget
                ),

                common_data.app.getTitledText(
                    title=_("PDP Type"),
                    name='pdpType',
                    value=self.data.get('pdpType', ''),
                    style=cli_style.edit_text,
                    widget_style=cli_style.black_bg_widget
                ),

                common_data.app.getTitledText(
                    title=_("Policies JSON URIs Authorization Token"),
                    name='policiesJsonUrisAuthorizationToken',
                    value=self.data.get('policiesJsonUrisAuthorizationToken', ''),
                    style=cli_style.edit_text,
                    widget_style=cli_style.black_bg_widget
                ),

                common_data.app.getTitledText(
                    title=_("Policies JSON URIs"),
                    name='policiesJsonUris',
                    value=' '.join(self.data.get('policiesJsonUris', [])),
                    style=cli_style.edit_text,
                    jans_help=_("Space seperated policies JSON URIs"),
                    jans_list_type=True,
                    widget_style=cli_style.black_bg_widget
                ),

                common_data.app.getTitledText(
                    title=_("Policies Zip URIs Authorization Token"),
                    name='policiesZipUrisAuthorizationToken',
                    value=self.data.get('policiesZipUrisAuthorizationToken', ''),
                    style=cli_style.edit_text,
                    widget_style=cli_style.black_bg_widget
                ),

                common_data.app.getTitledText(
                    title=_("Policies Zip URIs"),
                    name='policiesZipUris',
                    value=' '.join(self.data.get('policiesZipUris', [])),
                    style=cli_style.edit_text,
                    jans_help=_("Space seperated policies Zip URIs"),
                    jans_list_type=True,
                    widget_style=cli_style.black_bg_widget
                ),

            Window(height=1),
            VSplit([Button(text=_("Save"), handler=self.save)], align=HorizontalAlign.CENTER)

            ])


    async def get_configuration(self) -> None:
        'Coroutine for getting Janssen Lock configuration.'
        try:
            response = self.app.cli_object.process_command_by_id(
                        operation_id='get-lock-properties',
                        url_suffix='',
                        endpoint_args='',
                        data_fn=None,
                        data={}
                        )

        except Exception as e:
            self.app.show_message(_("Error getting Janssen Lock configuration"), str(e), tobefocused=self.app.center_container)
            self.app.disable_plugin(self.pid)
            return

        if response.status_code not in (200, 201):
            self.app.show_message(_("Error getting Janssen Lock configuration"), str(response.text), tobefocused=self.app.center_container)
            self.app.disable_plugin(self.pid)
            return

        self.data = response.json()
        self.create_widgets()


    def save(self):
        # save lock configuration

        async def lock_config_coroutine():
            lock_config = self.make_data_from_dialog(tabs={'lock_config': self.working_container})

            cli_args = {'operation_id': 'put-lock-properties', 'data': lock_config}
            common_data.app.start_progressing(_("Saving Lock configuration"))
            response = await common_data.app.loop.run_in_executor(common_data.app.executor, common_data.app.cli_requests, cli_args)
            common_data.app.stop_progressing()
            if response.status_code not in (200, 201):
                common_data.app.show_message(common_strings.error, str(response.text), tobefocused=self.main_container)
            else:
                self.data = response.json()

        asyncio.ensure_future(lock_config_coroutine())

    def set_center_frame(self) -> None:
        """center frame content
        """
        self.app.center_container = self.main_container


