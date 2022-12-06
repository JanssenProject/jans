import asyncio
from collections import OrderedDict
from functools import partial
from typing import Any
from prompt_toolkit.layout.containers import HSplit, DynamicContainer, VSplit, Window
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.widgets import Button, Label, Box, Dialog
from prompt_toolkit.application import Application
from wui_components.jans_nav_bar import JansNavBar
from wui_components.jans_drop_down import DropDownWidget
from wui_components.jans_vetrical_nav import JansVerticalNav
from wui_components.jans_cli_dialog import JansGDialog
from utils.multi_lang import _
from utils.utils import DialogUtils

class Plugin(DialogUtils):
    """This is a general class for plugins 
    """
    def __init__(
        self, 
        app: Application
        ) -> None:
        """init for Plugin class "fido"

        Args:
            app (Generic): The main Application class
        """
        self.app = app
        self.pid = 'fido'
        self.name = '[F]IDO'
        self.page_entered = False
        self.data = {}
        self.prepare_navbar()
        self.prepare_containers()

    def process(self) -> None:
        pass

    def init_plugin(self) -> None:
        """The initialization for this plugin
        """

        self.app.create_background_task(self.get_fido_configuration())

    def edit_requested_party(self, **kwargs: Any) -> None:
        """This method for editing the requested party
        """
        title = _("Enter Request Party Properties")
        schema = self.app.cli_object.get_schema_from_reference('Fido2', '#/components/schemas/RequestedParty')
        cur_data = kwargs.get('passed', ['', ''])
        name_widget = self.app.getTitledText(_("Name"), name='name', value=cur_data[0], jans_help=self.app.get_help_from_schema(self.schema, 'name'), style='class:outh-scope-text')
        domains_widget = self.app.getTitledText(_("Domains"), name='domains', value='\n'.join(cur_data[1].split(', ')),  height=3, jans_help=self.app.get_help_from_schema(self.schema, 'domains'), style='class:dialog-titled-widget')

        def add_request_party(dialog: Dialog) -> None:
            name_ = name_widget.me.text
            domains_ = domains_widget.me.text
            new_data = [name_, ', '.join(domains_.splitlines())]

            if not kwargs.get('data'):
                self.requested_parties_container.add_item(new_data)
            else:
                self.requested_parties_container.replace_item(kwargs['selected'], new_data)

        body = HSplit([name_widget, domains_widget])
        buttons = [Button(_("Cancel")), Button(_("OK"), handler=add_request_party)]
        dialog = JansGDialog(self.app, title=title, body=body, buttons=buttons, width=self.app.dialog_width-20)
        self.app.show_jans_dialog(dialog)

    def delete_requested_party(self, **kwargs: Any) -> None:
        """This method for deleting the requested party
        """
        
        dialog = self.app.get_confirm_dialog(_("Are you sure want to delete requested patry:")+"\n {} ?".format(kwargs['selected'][1]))

        async def coroutine():
            focused_before = self.app.layout.current_window
            result = await self.app.show_dialog_as_float(dialog)
            try:
                self.app.layout.focus(focused_before)
            except:
                self.app.stop_progressing()
                self.app.layout.focus(self.app.center_frame)

            if result.lower() == 'yes':
                self.requested_parties_container.remove_item(kwargs['selected'])
                self.app.stop_progressing()
                
            return result

        asyncio.ensure_future(coroutine())

    def create_widgets(self):
        self.schema = self.app.cli_object.get_schema_from_reference('Fido2', '#/components/schemas/AppConfiguration')

        self.tabs['configuration'] = HSplit([
                                self.app.getTitledText(_("Issuer"), name='issuer', value=self.data.get('issuer',''), jans_help=self.app.get_help_from_schema(self.schema, 'issuer'), style='class:outh-scope-text'),
                                self.app.getTitledText(_("Base Endpoint"), name='baseEndpoint', value=self.data.get('baseEndpoint',''), jans_help=self.app.get_help_from_schema(self.schema, 'baseEndpoint'), style='class:outh-scope-text'),
                                self.app.getTitledText(_("Clean Service Interval"), name='cleanServiceInterval', value=self.data.get('cleanServiceInterval',''), jans_help=self.app.get_help_from_schema(self.schema, 'cleanServiceInterval'), style='class:outh-scope-text', text_type='integer'),
                                self.app.getTitledText(_("Clean Service Batch ChunkSize"), name='cleanServiceBatchChunkSize', value=self.data.get('cleanServiceBatchChunkSize',''), jans_help=self.app.get_help_from_schema(self.schema, 'cleanServiceBatchChunkSize'), style='class:outh-scope-text', text_type='integer'),
                                self.app.getTitledCheckBox(_("Use Local Cache"), name='useLocalCache', checked=self.data.get('useLocalCache'), jans_help=self.app.get_help_from_schema(self.schema, 'useLocalCache'), style='class:outh-client-checkbox'),
                                self.app.getTitledCheckBox(_("Disable Jdk Logger"), name='disableJdkLogger', checked=self.data.get('disableJdkLogger'), jans_help=self.app.get_help_from_schema(self.schema, 'disableJdkLogger'), style='class:outh-client-checkbox'),
                                self.app.getTitledWidget(
                                    _("Logging Level"),
                                    name='loggingLevel',
                                    widget=DropDownWidget(
                                        values=[('TRACE', 'TRACE'), ('DEBUG', 'DEBUG'), ('INFO', 'INFO'), ('WARN', 'WARN'),('ERROR', 'ERROR'),('FATAL', 'FATAL'),('OFF', 'OFF')],
                                        value=self.data.get('loggingLevel')
                                        ),
                                    jans_help=self.app.get_help_from_schema(self.schema, 'loggingLevel'),
                                    style='class:outh-client-dropdown'
                                    ),
                                self.app.getTitledText(_("Logging Layout"), name='loggingLayout', value=self.data.get('loggingLayout',''), jans_help=self.app.get_help_from_schema(self.schema, 'loggingLayout'), style='class:outh-scope-text'),
                                self.app.getTitledText(_("External Logger Configuration"), name='externalLoggerConfiguration', value=self.data.get('externalLoggerConfiguration',''), jans_help=self.app.get_help_from_schema(self.schema, 'externalLoggerConfiguration'), style='class:outh-scope-text'),
                                self.app.getTitledText(_("Metric Reporter Interval"), name='metricReporterInterval', value=self.data.get('metricReporterInterval',''), jans_help=self.app.get_help_from_schema(self.schema, 'metricReporterInterval'), style='class:outh-scope-text', text_type='integer'),
                                self.app.getTitledText(_("Metric Reporter Keep Data Days"), name='metricReporterKeepDataDays', value=self.data.get('metricReporterKeepDataDays',''), jans_help=self.app.get_help_from_schema(self.schema, 'metricReporterKeepDataDays'), style='class:outh-scope-text', text_type='integer'),
                                self.app.getTitledCheckBox(_("Metric Reporter Enabled"), name='metricReporterEnabled', checked=self.data.get('metricReporterEnabled'), jans_help=self.app.get_help_from_schema(self.schema, 'metricReporterEnabled'), style='class:outh-client-checkbox'),
                                self.app.getTitledText(
                                            _("Person Custom Object Classes"),
                                            name='personCustomObjectClassList',
                                            value='\n'.join(self.data.get('personCustomObjectClassList', [])),
                                            height=3,
                                            jans_help=self.app.get_help_from_schema(self.schema, 'personCustomObjectClassList'),
                                            style='class:outh-scope-text'
                                            ),
                                Window(height=1),
                                VSplit([Window(), Button(_("Save"), handler=self.save_config), Window()]),
                                ],
                                width=D()
                                )


        static_schema = self.app.cli_object.get_schema_from_reference('Fido2', '#/components/schemas/Fido2Configuration')
        static_schema = {}

        fido2_static_config = self.data.get('fido2Configuration', {})

        requested_parties_title = _("Requested Parties")
        add_party_title =  _("Add Party")

        requested_parties_data = []
        for rp in fido2_static_config.get('requestedParties', {}):
            requested_parties_data.append([rp.get('name',''), ', '.join(rp.get('domains', []))])

        self.requested_parties_container = JansVerticalNav(
                myparent=self.app,
                headers=['Name', 'Domains'],
                preferred_size=[30, 30],
                data=requested_parties_data,
                on_enter=self.edit_requested_party,
                on_delete=self.delete_requested_party,
                on_display=self.app.data_display_dialog,
                selectes=0,
                headerColor='class:outh-client-navbar-headcolor',
                entriesColor='class:outh-client-navbar-entriescolor',
                all_data=requested_parties_data,
                underline_headings=False,
                max_width=65,
                jans_name='RequestedParties',
                max_height=False
                )

        self.tabs['static'] = HSplit([
                                self.app.getTitledText(_("Authenticator Certificates Folder"), name='authenticatorCertsFolder', value=fido2_static_config.get('authenticatorCertsFolder',''), jans_help=self.app.get_help_from_schema(static_schema, 'authenticatorCertsFolder'), style='class:outh-scope-text'),
                                self.app.getTitledText(_("MDS Access Token"), name='mdsAccessToken', value=fido2_static_config.get('mdsAccessToken',''), jans_help=self.app.get_help_from_schema(static_schema, 'mdsAccessToken'), style='class:outh-scope-text'),
                                self.app.getTitledText(_("MDS TOC Certificates Folder"), name='mdsCertsFolder', value=fido2_static_config.get('mdsCertsFolder',''), jans_help=self.app.get_help_from_schema(static_schema, 'mdsCertsFolder'), style='class:outh-scope-text'),
                                self.app.getTitledText(_("MDS TOC Files Folder"), name='mdsTocsFolder', value=fido2_static_config.get('mdsTocsFolder',''), jans_help=self.app.get_help_from_schema(static_schema, 'mdsTocsFolder'), style='class:outh-scope-text'),
                                self.app.getTitledCheckBox(_("Check U2f Attestations"), name='checkU2fAttestations', checked=fido2_static_config.get('checkU2fAttestations'), jans_help=self.app.get_help_from_schema(static_schema, 'checkU2fAttestations'), style='class:outh-client-checkbox'),
                                self.app.getTitledCheckBox(_("Check U2f Attestations"), name='checkU2fAttestations', checked=fido2_static_config.get('checkU2fAttestations'), jans_help=self.app.get_help_from_schema(static_schema, 'checkU2fAttestations'), style='class:outh-client-checkbox'),
                                self.app.getTitledText(_("Unfinished Request Expiration"), name='unfinishedRequestExpiration', value=fido2_static_config.get('unfinishedRequestExpiration',''), jans_help=self.app.get_help_from_schema(static_schema, 'unfinishedRequestExpiration'), style='class:outh-scope-text', text_type='integer'),
                                self.app.getTitledText(_("Authentication History Expiration"), name='authenticationHistoryExpiration', value=fido2_static_config.get('authenticationHistoryExpiration',''), jans_help=self.app.get_help_from_schema(static_schema, 'authenticationHistoryExpiration'), style='class:outh-scope-text', text_type='integer'),
                                self.app.getTitledText(_("Server Metadata Folder"), name='serverMetadataFolder', value=fido2_static_config.get('serverMetadataFolder',''), jans_help=self.app.get_help_from_schema(static_schema, 'serverMetadataFolder'), style='class:outh-scope-text'),

                                self.app.getTitledCheckBox(_("User Auto Enrollment"), name='userAutoEnrollment', checked=fido2_static_config.get('userAutoEnrollment'), jans_help=self.app.get_help_from_schema(static_schema, 'userAutoEnrollment'), style='class:outh-client-checkbox'),
                                self.app.getTitledText(
                                            _("Requested Credential Types"),
                                            name='requestedCredentialTypes',
                                            value='\n'.join(fido2_static_config.get('requestedCredentialTypes', [])),
                                            height=3, 
                                            jans_help=self.app.get_help_from_schema(static_schema, 'requestedCredentialTypes'), 
                                            style='class:outh-scope-text'
                                            ),

                    VSplit([
                            Label(text=requested_parties_title, style='class:script-label', width=len(requested_parties_title)+1), 
                            self.requested_parties_container,
                            Window(width=2),
                            HSplit([
                                Window(height=1),
                                Button(text=add_party_title, width=len(add_party_title)+4, handler=partial(self.edit_requested_party, jans_name='editRequestedPary')),
                                ]),
                            ],
                            height=5, width=D(),
                            ),

                    VSplit([Window(), Button(_("Save"), handler=self.save_config), Window()]),
                                ],
                                width=D()
                                )

        self.nav_selection_changed(list(self.tabs)[0])

    async def get_fido_configuration(self) -> None:
        'Coroutine for getting fido2 configuration.'
        try:
            response = self.app.cli_object.process_command_by_id(
                        operation_id='get-properties-fido2',
                        url_suffix='',
                        endpoint_args='',
                        data_fn=None,
                        data={}
                        )

        except Exception as e:
            self.app.show_message(_("Error getting Fido2 configuration"), str(e), tobefocused=self.app.center_container)
            self.app.disable_plugin(self.pid)
            return

        if response.status_code not in (200, 201):
            self.app.show_message(_("Error getting Fido2 configuration"), str(response.text), tobefocused=self.app.center_container)
            self.app.disable_plugin(self.pid)
            return

        self.data = response.json()
        self.create_widgets()

    def prepare_navbar(self) -> None:
        """prepare the navbar for the current Plugin 
        """
        self.nav_bar = JansNavBar(
                    self.app,
                    entries=[('configuration', '[D]ynamic Configuration'), ('static', 'S[t]atic Configuration')],
                    selection_changed=self.nav_selection_changed,
                    select=0,
                    jans_name='fido:nav_bar'
                    )

    def prepare_containers(self) -> None:
        """prepare the main container (tabs) for the current Plugin 
        """

        self.tabs = OrderedDict()
        self.main_area = HSplit([Label("configuration")],width=D())

        self.main_container = HSplit([
                                        Box(self.nav_bar.nav_window, style='class:sub-navbar', height=1),
                                        DynamicContainer(lambda: self.main_area),
                                        ],
                                    height=D(),
                                    style='class:outh_maincontainer'
                                    )

    def nav_selection_changed(
                self,
                selection: str
            ) -> None:

        """This method for the selection change

        Args:
            selection (str): the current selected tab
        """

        if selection in self.tabs:
            self.main_area = self.tabs[selection]
        else:
            self.main_area = self.app.not_implemented

    def save_config(self) -> None:
        """This method for saving the configuration
        """

        fido2_config = self.make_data_from_dialog(tabs={'configuration': self.tabs['configuration']})
        fido2_static = self.make_data_from_dialog(tabs={'static': self.tabs['static']})

        fido2_config['personCustomObjectClassList'] = fido2_config['personCustomObjectClassList'].splitlines()
        fido2_static['requestedCredentialTypes'] = fido2_static['requestedCredentialTypes'].splitlines()

        fido2_static['requestedParties'] = []
        for name, domains in self.requested_parties_container.data:
            fido2_static['requestedParties'].append({'name': name, 'domains': domains.splitlines()})

        fido2_config['fido2Configuration'] = fido2_static

        async def coroutine():
            cli_args = {'operation_id': 'put-properties-fido2', 'data': fido2_config}
            self.app.start_progressing()
            response = await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing()

        asyncio.ensure_future(coroutine())

    def set_center_frame(self) -> None:
        """center frame content
        """
        self.app.center_container = self.main_container


