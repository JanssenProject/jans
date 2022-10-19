import os
import sys

from prompt_toolkit.layout.containers import HSplit
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.widgets import Button, Label, Frame

class Plugin():
    """This is a general class for plugins 
    """
    def __init__(self, app):
        """init for Plugin class "config_api"

        Args:
            app (_type_): _description_
        """
        self.app = app
        self.pid = 'config_api'
        self.name = '[C]onfig-API'

    def process(self) -> None:
        pass

    def set_center_frame(self) -> None:
        """center frame content
        """        
        self.app.center_container = Frame(
                            body=HSplit([Label(text="Plugin {} is not imlemented yet".format(self.name)), Button(text="Button-{}".format(self.pid))], width=D()),
                            height=D())

