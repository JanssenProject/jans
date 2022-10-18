import os
import sys

from prompt_toolkit.layout.containers import HSplit
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

    def process(self):
        pass


    def prepare_navbar(self):
        """prepare the navbar for the current Plugin 
        """
        self.navbar = JansNavBar(
                    self.app,
                    entries=[('configuration', 'C[o]nfiguration'), ('registration', 'Re[g]istration')],
                    selection_changed=self.nav_selection_changed,
                    select=0,
                    )

    def nav_selection_changed(self, entry):
        pass

    def set_center_frame(self):
        """center frame content
        """
        self.app.center_container = HSplit([
                                Box(self.navbar.nav_window, style='class:sub-navbar', height=1),
                                Label(text="Plugin {} is not imlemented yet".format(self.name))
                                ],
                                height=D(),
                                style='class:outh_maincontainer'
                            )


