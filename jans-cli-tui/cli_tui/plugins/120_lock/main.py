import copy
import asyncio

from prompt_toolkit.layout.containers import HSplit, DynamicContainer,\
    VSplit, Window, HorizontalAlign

from prompt_toolkit.layout import ScrollablePane
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.widgets import Button, Frame, Label, Dialog
from prompt_toolkit.application import Application
from wui_components.widget_collections import get_logging_level_widget
from wui_components.jans_drop_down import DropDownWidget
from wui_components.jans_vetrical_nav import JansVerticalNav
from wui_components.jans_cli_dialog import JansGDialog


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



    def edit_policy_source(self, *args, **kwargs):
        if kwargs:
            title = _("Edit Policy Source")
            source_data = kwargs['data']
        else:
            title = _("Add Policy Source")
            source_data = (False, '', '')

        enabled_widget = common_data.app.getTitledCheckBox(
                    title=_("Enabled"),
                    name='enabled',
                    checked=source_data[0],
                    style=cli_style.check_box,
                    jans_help=common_data.app.get_help_from_schema(self.schema_cedarling_policy_sources, 'enabled'),
                    widget_style=cli_style.titled_text
                )

        authorization_token_widget = common_data.app.getTitledText(
                    title=_("Authorization Token"),
                    name='authorizationToken',
                    value=source_data[1],
                    style=cli_style.edit_text,
                    widget_style=cli_style.titled_text,
                    jans_help=common_data.app.get_help_from_schema(self.schema_cedarling_policy_sources, 'authorizationToken'),
                )

        policy_store_uri_widget = common_data.app.getTitledText(
                    title=_("Policy Store Uri"),
                    name='policyStoreUri',
                    value=source_data[2],
                    style=cli_style.edit_text,
                    widget_style=cli_style.titled_text,
                    jans_help=common_data.app.get_help_from_schema(self.schema_cedarling_policy_sources, 'policyStoreUri'),
                )

        def add_policy_source(dialog: Dialog) -> None:

            cur_widegt_data = (
                enabled_widget.me.checked,
                authorization_token_widget.me.text,
                policy_store_uri_widget.me.text
                )

            if not kwargs.get('data'):
                self.policy_sources_container.add_item(cur_widegt_data)
                self.policy_sources_container.all_data.append(cur_widegt_data)
            else:
                self.policy_sources_container.replace_item(kwargs['selected'], cur_widegt_data)
                self.policy_sources_container.all_data[kwargs['selected']] = cur_widegt_data

        body = HSplit([enabled_widget, authorization_token_widget, policy_store_uri_widget])
        buttons = [Button(_("Cancel")), Button(_("OK"), handler=add_policy_source)]
        dialog = JansGDialog(self.app, title=title, body=body, buttons=buttons, width=common_data.app.dialog_width-20)
        common_data.app.show_jans_dialog(dialog)


    def delete_policy_source(self, **kwargs):

        dialog = common_data.app.get_confirm_dialog(_("Are you sure want to delete policy source?")+"\n {} ?".format(kwargs['selected'][0]))

        async def coroutine():
            result = await common_data.app.show_dialog_as_float(dialog)
            common_data.app.layout.focus(self.policy_sources_container)

            if result.lower() == 'yes':
                self.policy_sources_container.remove_item(kwargs['selected'])

            return result

        asyncio.ensure_future(coroutine())

    def create_widgets(self):
        self.schema = self.app.cli_object.get_schema_from_reference('Lock', '#/components/schemas/AppConfiguration')
        self.schema_grpc = self.app.cli_object.get_schema_from_reference('Lock', '#/components/schemas/GrpcConfiguration')
        self.schema_cedarling = self.app.cli_object.get_schema_from_reference('Lock', '#/components/schemas/CedarlingConfiguration')
        self.schema_cedarling_policy_sources = self.app.cli_object.get_schema_from_reference('Lock', '#/components/schemas/PolicySource')

        self.grpc_configuration_widgets = HSplit(
            children=[
                common_data.app.getTitledWidget(
                    _("Server Mode"),
                    name='serverMode',
                    widget=DropDownWidget(
                        values=[(mode, mode) for mode in ('disabled', 'bridge', 'plain_server', 'tls_server')],
                        value=self.data.get('grpcConfiguration', {}).get('serverMode', 'bridge'),
                        select_one_option=False
                        ),
                    jans_help=common_data.app.get_help_from_schema(self.schema_grpc, 'serverMode'),
                    style=cli_style.edit_text,
                ),

                common_data.app.getTitledText(
                    title=_("Grpc Port"),
                    name='grpcPort',
                    value=self.data.get('grpcConfiguration', {}).get('grpcPort', '50051'),
                    style=cli_style.edit_text,
                    widget_style=cli_style.black_bg_widget,
                    jans_help=common_data.app.get_help_from_schema(self.schema_grpc, 'grpcPort'),
                    text_type='integer'
                ),

                common_data.app.getTitledCheckBox(
                    title=_("Use Tls"),
                    name='useTls', 
                    checked=self.data.get('grpcConfiguration', {}).get('useTls', False),
                    style=cli_style.check_box,
                    jans_help=common_data.app.get_help_from_schema(self.schema_grpc, 'useTls'),
                    widget_style=cli_style.black_bg_widget
                )
            ]
        )

        cedarling_configuration_data = self.data.get('cedarlingConfiguration', {})
        cedarling_configuration_policy_sources_data = []
        for ccps in cedarling_configuration_data.get('policySources', []):
            cedarling_configuration_policy_sources_data.append((
                    ccps['enabled'],
                    ccps['authorizationToken'],
                    ccps['policyStoreUri']
                ))

        self.policy_sources_container = JansVerticalNav(
                myparent=self.app,
                headers=['Enabled', 'Auth Token', 'Store Uri'],
                preferred_size=[10, 30, common_data.app.output.get_size().columns -60],
                data=cedarling_configuration_policy_sources_data,
                on_enter=self.edit_policy_source,
                on_delete=self.delete_policy_source,
                on_display=common_data.app.data_display_dialog,
                selectes=0,
                all_data=cedarling_configuration_policy_sources_data,
                underline_headings=False,
                max_width=common_data.app.output.get_size().columns - 5,
                jans_name='policySources',
                max_height=len(cedarling_configuration_policy_sources_data)+2
                )

        self.cedarling_configuration_widgets = HSplit(
            children=[
                common_data.app.getTitledCheckBox(
                    title=_("Enabled"),
                    name='enabled', 
                    checked=cedarling_configuration_data.get('enabled', False),
                    style=cli_style.check_box,
                    jans_help=common_data.app.get_help_from_schema(self.schema_cedarling, 'enabled'),
                    widget_style=cli_style.black_bg_widget
                ),

                common_data.app.getTitledWidget(
                    _("Log Type"),
                    name='logType',
                    widget=DropDownWidget(
                        values=[(ltype, ltype) for ltype in ('OFF', 'MEMORY', 'STD_OUT')],
                        value=cedarling_configuration_data.get('logType', 'STD_OUT'),
                        select_one_option=False
                        ),
                    jans_help=common_data.app.get_help_from_schema(self.schema_cedarling, 'logType'),
                    style=cli_style.edit_text,
                ),

                get_logging_level_widget(cedarling_configuration_data.get('logLevel', 'INFO')),


                Frame(
                    title=_("Policy Sources"),
                    body=HSplit([
                        self.policy_sources_container,
                        common_data.app.getButtonWithHandler(text=_("Add Source"), handler=self.edit_policy_source, centered=True)
                    ]),
                )
            ]
        )

        self.main_widgets = HSplit([

            common_data.app.getTitledText(
                title=_("Base Endpoint"),
                name='baseEndpoint',
                value=self.data.get('baseEndpoint'),
                style=cli_style.edit_text,
                jans_help=common_data.app.get_help_from_schema(self.schema, 'baseEndpoint'),
                widget_style=cli_style.black_bg_widget
            ),

            common_data.app.getTitledText(
                title=_("Open ID Issuer"),
                name='openIdIssuer',
                value=self.data.get('openIdIssuer'),
                style=cli_style.edit_text,
                jans_help=common_data.app.get_help_from_schema(self.schema, 'openIdIssuer'),
                widget_style=cli_style.black_bg_widget
            ),

            common_data.app.getTitledWidget(
                _("Protection Mode"),
                name='protectionMode',
                widget=DropDownWidget(
                    values=[('cedarling', 'cedarling'), ('oauth', 'oauth')],
                    value=self.data.get('protectionMode'),
                    select_one_option=False
                    ),
                jans_help=common_data.app.get_help_from_schema(self.schema, 'protectionMode'),
                style=cli_style.edit_text,
            ),

            common_data.app.getTitledWidget(
                _("Audit Persistence Mode"),
                name='auditPersistenceMode',
                widget=DropDownWidget(
                    values=[('internal', 'internal'), ('config-api', 'config-api')],
                    value=self.data.get('auditPersistenceMode'),
                    select_one_option=False
                    ),
                jans_help=common_data.app.get_help_from_schema(self.schema, 'auditPersistenceMode'),
                style=cli_style.edit_text,
            ),

            common_data.app.getTitledCheckBox(
                title=_("Stat Enabled"),
                name='statEnabled', 
                checked=self.data.get('statEnabled', True),
                style=cli_style.check_box,
                jans_help=common_data.app.get_help_from_schema(self.schema, 'statEnabled'),
                widget_style=cli_style.black_bg_widget
            ),

            common_data.app.getTitledText(
                title=_("Stat Timer Interval in Seconds"),
                name='statTimerIntervalInSeconds',
                value=self.data.get('statTimerIntervalInSeconds', '0'),
                style=cli_style.edit_text,
                widget_style=cli_style.black_bg_widget,
                jans_help=common_data.app.get_help_from_schema(self.schema, 'statTimerIntervalInSeconds'),
                text_type='integer'
            ),

            common_data.app.getTitledCheckBox(
                title=_("Disable JDK Logger"),
                name='disableJdkLogger',
                checked=self.data.get('disableJdkLogger', True),
                style=cli_style.check_box,
                jans_help=common_data.app.get_help_from_schema(self.schema, 'disableJdkLogger'),
                widget_style=cli_style.black_bg_widget
            ),

            common_data.app.getTitledCheckBox(
                title=_("Disable External Logger Configuration"),
                name='disableExternalLoggerConfiguration',
                checked=self.data.get('disableExternalLoggerConfiguration', True),
                style=cli_style.check_box,
                jans_help=common_data.app.get_help_from_schema(self.schema, 'disableExternalLoggerConfiguration'),
                widget_style=cli_style.black_bg_widget
            ),

            get_logging_level_widget(self.data.get('loggingLevel', 'INFO')),

            common_data.app.getTitledText(
                title=_("Logging Layout"),
                name='loggingLayout',
                value=self.data.get('loggingLayout', 'text'),
                style=cli_style.edit_text,
                jans_help=common_data.app.get_help_from_schema(self.schema, 'loggingLayout'),
                widget_style=cli_style.black_bg_widget
            ),

            common_data.app.getTitledText(
                title=_("External Logger Configuration"),
                name='externalLoggerConfiguration',
                value=self.data.get('externalLoggerConfiguration'),
                style=cli_style.edit_text,
                jans_help=common_data.app.get_help_from_schema(self.schema, 'externalLoggerConfiguration'),
                widget_style=cli_style.black_bg_widget
            ),

            common_data.app.getTitledCheckBox(
                title=_("Enable Metric Reporter"),
                name='metricReporterEnabled', 
                checked=self.data.get('metricReporterEnabled', False),
                style=cli_style.check_box,
                jans_help=common_data.app.get_help_from_schema(self.schema, 'metricReporterEnabled'),
                widget_style=cli_style.black_bg_widget
            ),

            common_data.app.getTitledText(
                title=_("Metric Reporter Interval"),
                name='metricReporterInterval',
                value=self.data.get('metricReporterInterval', '300'),
                style=cli_style.edit_text,
                widget_style=cli_style.black_bg_widget,
                jans_help=common_data.app.get_help_from_schema(self.schema, 'metricReporterInterval'),
                text_type='integer'
            ),

            common_data.app.getTitledText(
                title=_("Metric Reporter Keep Data Days"),
                name='metricReporterKeepDataDays',
                value=self.data.get('metricReporterKeepDataDays', '15'),
                style=cli_style.edit_text,
                widget_style=cli_style.black_bg_widget,
                jans_help=common_data.app.get_help_from_schema(self.schema, 'metricReporterKeepDataDays'),
                text_type='integer'
            ),

            common_data.app.getTitledText(
                title=_("Clean Service Interval"),
                name='cleanServiceInterval',
                value=self.data.get('cleanServiceInterval', '60'),
                style=cli_style.edit_text,
                widget_style=cli_style.black_bg_widget,
                jans_help=common_data.app.get_help_from_schema(self.schema, 'cleanServiceInterval'),
                text_type='integer'
            ),

            common_data.app.getTitledText(
                title=_("Clean Service Batch ChunkSize"),
                name='cleanServiceBatchChunkSize',
                value=self.data.get('cleanServiceBatchChunkSize', '10000'),
                style=cli_style.edit_text,
                widget_style=cli_style.black_bg_widget,
                jans_help=common_data.app.get_help_from_schema(self.schema, 'cleanServiceBatchChunkSize'),
                text_type='integer'
            ),

            common_data.app.getTitledText(
                title=_("Message Consumer Type"),
                name='messageConsumerType',
                value=self.data.get('messageConsumerType', 'DISABLED'),
                style=cli_style.edit_text,
                jans_help=common_data.app.get_help_from_schema(self.schema, 'messageConsumerType'),
                widget_style=cli_style.black_bg_widget
            ),

            common_data.app.getTitledCheckBox(
                title=_("Error Reason Enabled"),
                name='errorReasonEnabled', 
                checked=self.data.get('errorReasonEnabled', False),
                style=cli_style.check_box,
                jans_help=common_data.app.get_help_from_schema(self.schema, 'errorReasonEnabled'),
                widget_style=cli_style.black_bg_widget
            ),

         Frame(
            title=_("Grpc Configuration"),
            body=self.grpc_configuration_widgets,
        ),

         Frame(
            title=_("Cedarling Configuration"),
            body=self.cedarling_configuration_widgets,
        ),

        Window(height=1),
        VSplit([Button(text=_("Save"), handler=self.save)], align=HorizontalAlign.CENTER)

        ])


        self.working_container = ScrollablePane(
            content = self.main_widgets
        )


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

        async def lock_config_coroutine() -> None:

            lock_config = self.make_data_from_dialog(tabs={'lock_config': self.main_widgets})
            grpc_configuration = self.make_data_from_dialog(tabs={'grpc_configuration': self.grpc_configuration_widgets})
            cedarling_configuration = self.make_data_from_dialog(tabs={'cedarling_configuration': self.cedarling_configuration_widgets})
            lock_config['grpcConfiguration'] = grpc_configuration
            cedarling_configuration['policySources'] = []

            for ps in self.policy_sources_container.data:
                ps_dict = {
                    'enabled': ps[0],
                    'authorizationToken': ps[1],
                    'policyStoreUri': ps[2]
                }
                cedarling_configuration['policySources'].append(ps_dict)

            lock_config['cedarlingConfiguration'] = cedarling_configuration
            new_data = copy.deepcopy(self.data)

            new_data.update(lock_config)

            cli_args = {'operation_id': 'put-lock-properties', 'data': new_data}
            common_data.app.start_progressing(_("Saving Lock configuration"))
            response = await common_data.app.loop.run_in_executor(common_data.app.executor, common_data.app.cli_requests, cli_args)
            common_data.app.stop_progressing()
            if response.status_code not in (200, 201):
                common_data.app.show_message(
                    title=_(common_strings.error),
                    message=str(response.text),
                    tobefocused=self.main_container
                    )
            else:
                self.data = response.json()
                common_data.app.show_message(
                    title=_(common_strings.success),
                    message=_("Jans Lock Server configuration was saved."),
                    tobefocused=self.main_container
                    )

        asyncio.ensure_future(lock_config_coroutine())

    def set_center_frame(self) -> None:
        """center frame content
        """
        self.app.center_container = self.main_container


