import asyncio
from prompt_toolkit.application import Application
from prompt_toolkit.layout.containers import HSplit, VSplit, Window
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.widgets import Button,  Frame
from wui_components.jans_drop_down import DropDownWidget
from utils.utils import DialogUtils
from utils.multi_lang import _

class Plugin(DialogUtils):
    """This is a general class for plugins 
    """
    def __init__(
        self, 
        app: Application
        ) -> None:
        """init for Plugin class "scim"

        Args:
            app (Generic): The main Application class
        """
        self.app = app
        self.pid = 'scim'
        self.name = '[S]CIM'
        self.app_config = {}
        self.bg_color='black'
        self.widgets_ready = False
        self.container = Frame(
                            body=HSplit([Button(text=_("Get Scim Configuration"), handler=self.get_app_config)], width=D()),
                            height=D())

    def process(self) -> None:
        pass

    def set_center_frame(self) -> None:
        """center frame content
        """
        self.app.center_container = self.container

    def create_widgets(self) -> None:
        """SCIM Application configuration widgets are created in this fonction
        """
        self.save_button = Button(_("Save"), handler=self.save_app_config)
        schema = self.app.cli_object.get_schema_from_reference('SCIM', '#/components/schemas/AppConfiguration')
        self.container = HSplit([
                                    self.app.getTitledText(_("Base DN"), name='baseDN', value=self.app_config.get('baseDN',''), jans_help=self.app.get_help_from_schema(schema, 'baseDN'), read_only=True, style='class:outh-scope-text',widget_style='bg:{} fg:white'.format(self.bg_color)),
                                    self.app.getTitledText(_("Application Url"), name='applicationUrl', value=self.app_config.get('applicationUrl',''), jans_help=self.app.get_help_from_schema(schema, 'applicationUrl'), style='class:outh-scope-text',widget_style='bg:{} fg:white'.format(self.bg_color)),
                                    self.app.getTitledText(_("Base Endpoint"), name='baseEndpoint', value=self.app_config.get('baseEndpoint',''), jans_help=self.app.get_help_from_schema(schema, 'baseEndpoint'), style='class:outh-scope-text',widget_style='bg:{} fg:white'.format(self.bg_color)),
                                    self.app.getTitledText(_("Person Custom Object Class"), name='personCustomObjectClass', value=self.app_config.get('personCustomObjectClass',''), jans_help=self.app.get_help_from_schema(schema, 'personCustomObjectClass'), style='class:outh-scope-text',widget_style='bg:{} fg:white'.format(self.bg_color)),
                                    self.app.getTitledText(_("Auth Issuer"), name='oxAuthIssuer', value=self.app_config.get('oxAuthIssuer',''), jans_help=self.app.get_help_from_schema(schema, 'oxAuthIssuer'), style='class:outh-scope-text',widget_style='bg:{} fg:white'.format(self.bg_color)),
                                    self.app.getTitledRadioButton(_("Protection Mode"), name='protectionMode', values=[('OAUTH', 'OAUTH'),('BYPASS', 'BYPASS')], current_value=self.app_config.get('protectionMode'), jans_help=self.app.get_help_from_schema(schema, 'protectionMode'), style='class:outh-client-radiobutton',widget_style='bg:{} fg:white'.format(self.bg_color)),
                                    self.app.getTitledText(_("Max Count"), name='maxCount', value=self.app_config.get('maxCount',''), jans_help=self.app.get_help_from_schema(schema, 'maxCount'), text_type='integer', style='class:outh-scope-text',widget_style='bg:{} fg:white'.format(self.bg_color)),
                                    self.app.getTitledText(_("Bulk Max Operations"), name='bulkMaxOperations', value=self.app_config.get('bulkMaxOperations',''), jans_help=self.app.get_help_from_schema(schema, 'bulkMaxOperations'), text_type='integer', style='class:outh-scope-text',widget_style='bg:{} fg:white'.format(self.bg_color)),
                                    self.app.getTitledText(_("Bulk Max Payload Size"), name='bulkMaxPayloadSize', value=self.app_config.get('bulkMaxPayloadSize',''), jans_help=self.app.get_help_from_schema(schema, 'bulkMaxPayloadSize'), text_type='integer', style='class:outh-scope-text',widget_style='bg:{} fg:white'.format(self.bg_color)),
                                    self.app.getTitledText(_("User Extension Schema URI"), name='userExtensionSchemaURI', value=self.app_config.get('userExtensionSchemaURI',''), jans_help=self.app.get_help_from_schema(schema, 'userExtensionSchemaURI'), style='class:outh-scope-text',widget_style='bg:{} fg:white'.format(self.bg_color)),
                                    self.app.getTitledWidget(
                                        _("Logging Level"),
                                        name='loggingLevel',
                                        widget=DropDownWidget(
                                            values=[('TRACE', 'TRACE'), ('DEBUG', 'DEBUG'), ('INFO', 'INFO'), ('WARN', 'WARN'),('ERROR', 'ERROR'),('FATAL', 'FATAL'),('OFF', 'OFF')],
                                            value=self.app_config.get('loggingLevel')
                                            ),
                                        jans_help=self.app.get_help_from_schema(schema, 'loggingLevel'),
                                        style='class:outh-client-dropdown'
                                        ),
                                    self.app.getTitledText(_("Logging Layout"), name='loggingLayout', value=self.app_config.get('loggingLayout',''), jans_help=self.app.get_help_from_schema(schema, 'loggingLayout'), style='class:outh-scope-text',widget_style='bg:{} fg:white'.format(self.bg_color)),
                                    self.app.getTitledText(_("External Logger Configuration"), name='externalLoggerConfiguration', value=self.app_config.get('externalLoggerConfiguration',''), jans_help=self.app.get_help_from_schema(schema, 'externalLoggerConfiguration'), style='class:outh-scope-text',widget_style='bg:{} fg:white'.format(self.bg_color)),
                                    self.app.getTitledText(_("Metric Reporter Interval"), name='metricReporterInterval', value=self.app_config.get('metricReporterInterval',''), jans_help=self.app.get_help_from_schema(schema, 'metricReporterInterval'), style='class:outh-scope-text', text_type='integer',widget_style='bg:{} fg:white'.format(self.bg_color)),
                                    self.app.getTitledText(_("Metric Reporter Keep Data Days"), name='metricReporterKeepDataDays', value=self.app_config.get('metricReporterKeepDataDays',''), jans_help=self.app.get_help_from_schema(schema, 'metricReporterKeepDataDays'), style='class:outh-scope-text', text_type='integer',widget_style='bg:{} fg:white'.format(self.bg_color)),
                                    self.app.getTitledCheckBox(_("Metric Reporter Enabled"), name='metricReporterEnabled', checked=self.app_config.get('metricReporterEnabled'), jans_help=self.app.get_help_from_schema(schema, 'metricReporterEnabled'), style='class:outh-client-checkbox',widget_style='bg:{} fg:white'.format(self.bg_color)),
                                    self.app.getTitledCheckBox(_("Disable Jdk Logger"), name='disableJdkLogger', checked=self.app_config.get('disableJdkLogger'), jans_help=self.app.get_help_from_schema(schema, 'disableJdkLogger'), style='class:outh-client-checkbox',widget_style='bg:{} fg:white'.format(self.bg_color)),
                                    self.app.getTitledCheckBox(_("Use Local Cache"), name='useLocalCache', checked=self.app_config.get('useLocalCache'), jans_help=self.app.get_help_from_schema(schema, 'useLocalCache'), style='class:outh-client-checkbox',widget_style='bg:{} fg:white'.format(self.bg_color)),
                                    VSplit([Window(), self.save_button, Window()])
                                ],
                                width=D()
                            )

        self.app.center_container = HSplit([ self.container],style='bg:{}'.format(self.bg_color),height=D()) 

    def get_app_config(self) -> None:
        """Gets SCIM application configurations from server.
        """

        async def coroutine():
            cli_args = {'operation_id': 'get-scim-config'}
            self.app.start_progressing()
            response = await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing()
            self.app_config = response.json()
            self.create_widgets()
            self.app.invalidate()
            self.app.layout.focus(self.app.center_container)

        asyncio.ensure_future(coroutine())

    def save_app_config(self) -> None:
        """Save button handler for saving SCIM application configurations.
        Once configuration data was obtained from form, patch operations are prepared and saved to server.
        """
        data = self.make_data_from_dialog({'scim': self.container})
        self.app.logger.debug("SCIM APP CONFIG {}".format(data))
        patche_list = []
        for key in self.app_config:
            if self.app_config[key] != data[key]:
                patche_list.append({'op':'replace', 'path': key, 'value': data[key]})
        for key in data:
            if data[key] and key not in self.app_config:
                patche_list.append({'op':'add', 'path': key, 'value': data[key]})

        if not patche_list:
            self.app.show_message(_("Warning"), _("No changes was done on Scim appilication configuration. Nothing to save."), tobefocused=self.app.center_container)
            return

        async def coroutine():
            cli_args = {'operation_id': 'patch-scim-config', 'data': patche_list}
            self.app.start_progressing()
            response = await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing()

        asyncio.ensure_future(coroutine())

