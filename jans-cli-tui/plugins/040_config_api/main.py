import os
import sys
from prompt_toolkit.application import Application

from prompt_toolkit.layout.containers import HSplit
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.widgets import Button, Label, Frame
from wui_components.jans_nav_bar import JansNavBar
from prompt_toolkit.layout.containers import HSplit, DynamicContainer, VSplit, Window
from prompt_toolkit.widgets import Button, Label, Frame, Box, Dialog
from wui_components.jans_cli_dialog import JansGDialog
from collections import OrderedDict
from functools import partial
from typing import Any
from wui_components.jans_vetrical_nav import JansVerticalNav
from utils.multi_lang import _

class Plugin():
    """This is a general class for plugins 
    """
    def __init__(
        self, 
        app: Application
        ) -> None:
        """init for Plugin class "config_api"

        Args:
            app (_type_): _description_
        """
        self.app = app
        self.pid = 'config_api'
        self.name = '[C]onfig-API'
        self.page_entered = False
        self.data = {}
        
        self.prepare_navbar()
        self.prepare_containers()

    def process(self) -> None:
        pass

    def create_widgets(self):

        self.containers['accessroles'] = HSplit([
                                Label(text=_("accessroles")),
                                    ],
                                width=D()
                                )

        self.containers['permissions'] = HSplit([
                                Label(text=_("permissions")),
                                ],
                                width=D()
                                )

        self.containers['mapping'] = HSplit([
                                Label(text=_("mapping")),
                                ],
                                width=D()
                                )
                                
        self.nav_selection_changed(list(self.containers)[0])


    def prepare_navbar(self) -> None:
        """prepare the navbar for the current Plugin 
        """
        self.nav_bar = JansNavBar(
                    self.app,
                    entries=[('accessroles', 'Access r[o]les'), ('permissions', '[P]ermissions'),  ('mapping', '[M]apping')],
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
        self.create_widgets()

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

