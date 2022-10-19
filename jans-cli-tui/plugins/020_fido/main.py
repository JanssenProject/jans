import os
import sys
import asyncio
import time

from collections import OrderedDict

from prompt_toolkit.application.current import get_app
from prompt_toolkit.layout.containers import HSplit, DynamicContainer
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.widgets import Button, Label, Frame, Box
from prompt_toolkit.application import Application

from wui_components.jans_nav_bar import JansNavBar


from multi_lang import _
import cli_style

class Plugin():
    """This is a general class for plugins 
    """
    def __init__(
        self, 
        app: Application
        ) -> None:
        """init for Plugin class "fido"

        Args:
            app (_type_): _description_
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

        self.app.create_background_task(self.get_fido_configuration())
        self.schema = self.app.cli_object.get_schema_from_reference('#/components/schemas/AppConfiguration')

        schema = self.app.cli_object.get_schema_from_reference('#/components/schemas/JansFido2DynConfiguration')

        self.containers['configuration'] = HSplit([
                                self.app.getTitledText(_("Issuer"), name='issuer', value=self.data.get('issuer',''), jans_help=self.app.get_help_from_schema(schema, 'issuer'), style='class:outh-scope-text'),
                                self.app.getTitledText(_("Base Endpoint"), name='baseEndpoint', value=self.data.get('baseEndpoint',''), jans_help=self.app.get_help_from_schema(schema, 'baseEndpoint'), style='class:outh-scope-text'),
                                self.app.getTitledText(_("Clean Service Interval"), name='cleanServiceInterval', value=self.data.get('cleanServiceInterval',''), jans_help=self.app.get_help_from_schema(schema, 'cleanServiceInterval'), style='class:outh-scope-text'),
                                self.app.getTitledText(_("Clean Service Batch ChunkSize"), name='cleanServiceBatchChunkSize', value=self.data.get('cleanServiceBatchChunkSize',''), jans_help=self.app.get_help_from_schema(schema, 'cleanServiceBatchChunkSize'), style='class:outh-scope-text'),
                                    ],
                                width=D()
                                )

        self.nav_selection_changed(list(self.containers)[0])


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
            self.app.show_message(_("Error getting Fido2 configuration"), str(e))
            return

        if response.status_code not in (200, 201):
            self.app.show_message(_("Error getting Fido2 configuration"), str(response.text))
            return

        self.data = response.json()

        for item in self.containers['configuration'].children:
            if hasattr(item, 'me'):
                key_ = item.me.window.jans_name
                item.me.text = str(self.data.get(key_, ''))

    def prepare_navbar(self) -> None:
        """prepare the navbar for the current Plugin 
        """
        self.nav_bar = JansNavBar(
                    self.app,
                    entries=[('configuration', 'C[o]nfiguration'), ('registration', 'Re[g]istration')],
                    selection_changed=self.nav_selection_changed,
                    select=0,
                    jans_name='fido:nav_bar'
                    )

    def prepare_containers(self) -> None:
        """prepare the main container (tabs) for the current Plugin 
        """

        self.containers = OrderedDict()
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

        if selection in self.containers:
            self.main_area = self.containers[selection]
        else:
            self.main_area = self.app.not_implemented

    def set_center_frame(self) -> None:
        """center frame content
        """
        self.app.center_container = self.main_container


