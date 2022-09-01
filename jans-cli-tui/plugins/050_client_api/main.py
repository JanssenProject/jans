import os
import sys

from prompt_toolkit.layout.containers import HSplit
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.widgets import Button, Label, Frame

class Plugin():
    def __init__(self, app):
        self.app = app
        self.pid = 'client_api'
        self.name = 'Client-API'

    def process(self):
        pass

    def set_center_frame(self):
        self.app.center_container = Frame(
                            body=HSplit([Label(text="Plugin {} is not imlemented yet".format(self.name)), Button(text="Button-{}".format(self.pid))], width=D()),
                            height=D())

