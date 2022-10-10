import os
import sys

import threading
from asyncio import ensure_future
from functools import partial

import prompt_toolkit
from prompt_toolkit.application.current import get_app
from prompt_toolkit.key_binding import KeyBindings
from prompt_toolkit.key_binding.bindings.focus import focus_next, focus_previous
from prompt_toolkit.layout.containers import (
    HSplit,
    VSplit,
    HorizontalAlign,
    DynamicContainer,
)
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.widgets import (
    Box,
    Button,
    Label,
    Frame
)
from static import DialogResult

from cli import config_cli
from wui_components.jans_nav_bar import JansNavBar
from wui_components.jans_side_nav_bar import JansSideNavBar
from wui_components.jans_vetrical_nav import JansVerticalNav
from wui_components.jans_dialog import JansDialog
from wui_components.jans_dialog_with_nav import JansDialogWithNav
from wui_components.jans_drop_down import DropDownWidget
from wui_components.jans_data_picker import DateSelectWidget

from edit_client_dialog import EditClientDialog
from edit_scope_dialog import EditScopeDialog

from multi_lang import _
import cli_style
class Plugin():
    """This is a general class for plugins 
    """
    def __init__(self, app):
        """init for Plugin class "oxauth"

        Args:
            app (_type_): _description_
        """
        self.app = app
        self.pid = 'scripts'
        self.name = 'Scripts'

        self.scripts_prepare_containers()

    def process(self):
        pass

    def set_center_frame(self):
        """center frame content
        """
        self.app.center_container = self.scripts_main_area

    def scripts_prepare_containers(self):
        """prepare the main container (tabs) for the current Plugin 
        """

        self.scripts_list_container = HSplit([],width=D(), height=D())

        self.scripts_main_area = HSplit([
                    VSplit([
                        self.app.getButton(text=_("Get Scripts"), name='scripts:get', jans_help=_("Retreive first %d Scripts") % (20), handler=self.scrips_get_scripts),
                        self.app.getTitledText(_("Search: "), name='scripts:search', jans_help=_("Press enter to perform search"), accept_handler=self.search_scripts, style='class:outh_containers_scopes.text'),
                        self.app.getButton(text=_("Add Sscript"), name='scripts:add', jans_help=_("To add a new scope press this button"), handler=self.add_script),
                        ],
                        padding=3,
                        width=D(),
                    ),
                    DynamicContainer(lambda: self.scripts_list_container)
                    ],style='class:outh_containers_scopes')



    def scrips_get_scripts(self):
        pass

    def search_scripts(self):
        pass

    def add_script(self):
        pass
