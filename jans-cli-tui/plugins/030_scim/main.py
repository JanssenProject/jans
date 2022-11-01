import os
import sys
import threading

from typing import Sequence

from prompt_toolkit.application import Application
from prompt_toolkit.layout.containers import HSplit, VSplit, Window
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.widgets import Button, Label, Frame
from prompt_toolkit.formatted_text import HTML
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
            app (_type_): _description_
        """
        self.app = app
        self.pid = 'scim'
        self.name = '[S]CIM'
        self.app_config = {}
        self.widgets_ready = False
        self.container = Frame(
                            body=HSplit([Label(text=_("Please wait while loading SCIM configuration"))], width=D()),
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
                                    self.app.getTitledText(_("Base DN"), name='baseDN', value=self.app_config.get('baseDN',''), jans_help=self.app.get_help_from_schema(schema, 'baseDN'), read_only=True, style='class:outh-scope-text'),
                                    self.app.getTitledText(_("Application Url"), name='applicationUrl', value=self.app_config.get('applicationUrl',''), jans_help=self.app.get_help_from_schema(schema, 'applicationUrl'), style='class:outh-scope-text'),
                                    self.app.getTitledText(_("Base Endpoint"), name='baseEndpoint', value=self.app_config.get('baseEndpoint',''), jans_help=self.app.get_help_from_schema(schema, 'baseEndpoint'), style='class:outh-scope-text'),
                                    self.app.getTitledText(_("Person Custom Object Class"), name='personCustomObjectClass', value=self.app_config.get('personCustomObjectClass',''), jans_help=self.app.get_help_from_schema(schema, 'personCustomObjectClass'), style='class:outh-scope-text'),
                                    self.app.getTitledText(_("Person Custom Object Class"), name='oxAuthIssuer', value=self.app_config.get('oxAuthIssuer',''), jans_help=self.app.get_help_from_schema(schema, 'oxAuthIssuer'), style='class:outh-scope-text'),
                                    self.app.getTitledRadioButton(_("Protection Mode"), name='protectionMode', values=[('OAUTH', 'OAUTH'),('BYPASS', 'BYPASS')], current_value=self.app_config.get('protectionMode'), jans_help=self.app.get_help_from_schema(schema, 'protectionMode'), style='class:outh-client-radiobutton'),
                                    self.app.getTitledText(_("Max Count"), name='maxCount', value=self.app_config.get('maxCount',''), jans_help=self.app.get_help_from_schema(schema, 'maxCount'), text_type='integer', style='class:outh-scope-text'),
                                    self.app.getTitledText(_("Bulk Max Operations"), name='bulkMaxOperations', value=self.app_config.get('bulkMaxOperations',''), jans_help=self.app.get_help_from_schema(schema, 'bulkMaxOperations'), text_type='integer', style='class:outh-scope-text'),
                                    self.app.getTitledText(_("Bulk Max Payload Size"), name='bulkMaxPayloadSize', value=self.app_config.get('bulkMaxPayloadSize',''), jans_help=self.app.get_help_from_schema(schema, 'bulkMaxPayloadSize'), text_type='integer', style='class:outh-scope-text'),
                                    self.app.getTitledText(_("User Extension Schema URI"), name='userExtensionSchemaURI', value=self.app_config.get('userExtensionSchemaURI',''), jans_help=self.app.get_help_from_schema(schema, 'userExtensionSchemaURI'), style='class:outh-scope-text'),
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
                                    self.app.getTitledText(_("Logging Layout"), name='loggingLayout', value=self.app_config.get('loggingLayout',''), jans_help=self.app.get_help_from_schema(schema, 'loggingLayout'), style='class:outh-scope-text'),
                                    self.app.getTitledText(_("External Logger Configuration"), name='externalLoggerConfiguration', value=self.app_config.get('externalLoggerConfiguration',''), jans_help=self.app.get_help_from_schema(schema, 'externalLoggerConfiguration'), style='class:outh-scope-text'),
                                    self.app.getTitledText(_("Metric Reporter Interval"), name='metricReporterInterval', value=self.app_config.get('metricReporterInterval',''), jans_help=self.app.get_help_from_schema(schema, 'metricReporterInterval'), style='class:outh-scope-text', text_type='integer'),
                                    self.app.getTitledText(_("Metric Reporter Keep Data Days"), name='metricReporterKeepDataDays', value=self.app_config.get('metricReporterKeepDataDays',''), jans_help=self.app.get_help_from_schema(schema, 'metricReporterKeepDataDays'), style='class:outh-scope-text', text_type='integer'),
                                    self.app.getTitledCheckBox(_("Metric Reporter Enabled"), name='metricReporterEnabled', checked=self.app_config.get('metricReporterEnabled'), jans_help=self.app.get_help_from_schema(schema, 'metricReporterEnabled'), style='class:outh-client-checkbox'),
                                    self.app.getTitledCheckBox(_("Disable Jdk Logger"), name='disableJdkLogger', checked=self.app_config.get('disableJdkLogger'), jans_help=self.app.get_help_from_schema(schema, 'disableJdkLogger'), style='class:outh-client-checkbox'),
                                    self.app.getTitledCheckBox(_("Use Local Cache"), name='useLocalCache', checked=self.app_config.get('useLocalCache'), jans_help=self.app.get_help_from_schema(schema, 'useLocalCache'), style='class:outh-client-checkbox'),
                                    VSplit([Window(), self.save_button, Window()])
                                ],
                                width=D()
                            )

        self.app.center_container = self.container


    def get_app_config(self) -> None:
        """Gets SCIM application configurations from server.
        """
        try :
            rsponse = self.app.cli_object.process_command_by_id(
                        operation_id='get-scim-config',
                        url_suffix='',
                        endpoint_args='',
                        data_fn=None,
                        data={}
                        )
        except Exception as e:
            self.app.stop_progressing()
            self.app.show_message(_("Error getting SCIM configuration"), str(e))
            return
        self.app.stop_progressing()

        try:
            self.app_config = rsponse.json()
        except Exception:
            self.app.show_message(_("Error getting SCIM configuration"), str(rsponse.text))
            return

        self.app.logger.debug("SCIM Configuration: {}".format(self.app_config))

        if not self.widgets_ready:
            self.create_widgets()
            self.app.invalidate()

    def on_page_enter(self) -> None:
        """Function to perform preliminary tasks before this page entered.
        """
        if not self.app_config:
            t = threading.Thread(target=self.get_app_config, daemon=True)
            self.app.start_progressing()
            t.start()

        if self.app_config and not self.widgets_ready:
            self.create_widgets()

    def do_save_app_config(self, patche_list: Sequence[dict]) -> None:
        """Saves appication configuration to server
        """
        self.app.cli_object.process_command_by_id(
                    operation_id='patch-scim-config',
                    url_suffix='',
                    endpoint_args='',
                    data_fn=None,
                    data=patche_list
                    )
        self.app.stop_progressing()
        self.status_bar_text = _("Scim appilication configuration was saved.")
        self.app.status_bar_text = HTML('<style bg="ansired">' + _("Scim appilication configuration was saved.") + '</style>')

        for patch in patche_list:
            key = patch['path'].lstrip('/')
            val = patch['value']
            self.app_config[key] = val

    def save_app_config(self) -> None:
        """Save button handler for saving SCIM application configurations.
        Once configuration data was obtained from form, patch operations are prepared and send to `do_save_app_config()` in thread.
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

        self.app.logger.debug("SCIM PATCH {}".format(patche_list))
        if patche_list:
            t = threading.Thread(target=self.do_save_app_config, args=(patche_list,), daemon=True)
            self.app.start_progressing()
            t.start()
        else:
            self.app.status_bar_text = HTML('<style bg="ansired">' + _("No changes was done on Scim appilication configuration. Nothing to save.") + '</style>')

