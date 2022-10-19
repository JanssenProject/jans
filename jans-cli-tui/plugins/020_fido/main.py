import os
import sys

from prompt_toolkit.layout.containers import HSplit, DynamicContainer
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.widgets import Button, Label, Frame, Box

from wui_components.jans_nav_bar import JansNavBar


from multi_lang import _
import cli_style

class Plugin():
    """This is a general class for plugins 
    """
    def __init__(self, app):
        """init for Plugin class "fido"

        Args:
            app (_type_): _description_
        """
        self.app = app
        self.pid = 'fido'
        self.name = '[F]IDO'
        self.prepare_navbar()
        self.prepare_containers()

    def process(self) -> None:
        pass

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

        self.containers = {
            'configuration': HSplit([Label("config")],width=D()),
            'registration': HSplit([Label("regist")],width=D()),
        }

        self.main_area = HSplit([],width=D())


        self.main_container = HSplit([
                                        Box(self.nav_bar.nav_window, style='class:sub-navbar', height=1),
                                        DynamicContainer(lambda: self.main_area),
                                        ],
                                    height=D(),
                                    style='class:outh_maincontainer'
                                    )

    def nav_selection_changed(
        self, 
        selection: str,
        ) -> None:
        """This method for the selection change

        Args:
            selection (str): the current selected tab
        """

        if selection in self.containers:
            self.oauth_main_area = self.containers[selection]
        else:
            self.oauth_main_area = self.app.not_implemented

    def set_center_frame(self) -> None:
        """center frame content
        """
        self.app.center_container = self.main_container


