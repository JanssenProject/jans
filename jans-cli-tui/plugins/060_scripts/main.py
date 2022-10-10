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
    Window,
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
        """Method to get the Scripts data from server
        """
        self.scripts_list_container = HSplit([Label(_("Please wait while getting Scripts"),style='class:outh-waitscopedata.label')], width=D(), height=D(), style='class:outh-waitclientdata')
        t = threading.Thread(target=self.scripts_update_list, daemon=True)
        t.start()


    def scripts_update_list(self, start_index=1, pattern=''):
        """Updates Scripts data from server

        Args:
            start_index (int, optional): add Button("Prev") to the layout. Defaults to 0.
        """
        def get_next(start_index, pattern=''):
            self.scripts_update_list(start_index, pattern='')

        endpoint_args ='limit:{},startIndex:{}'.format(self.app.entries_per_page, start_index)
        #endpoint_args=''
        if pattern:
            endpoint_args +=',pattern:'+pattern
        try :
            rsponse = self.app.cli_object.process_command_by_id(
                operation_id='get-config-scripts',
                url_suffix='',
                endpoint_args=endpoint_args,
                data_fn=None,
                data={}
                        )

        except Exception as e:
            self.app.show_message(_("Error getting scripts"), str(e))
            return

        if rsponse.status_code not in (200, 201):
            self.app.show_message(_("Error getting scripts"), str(rsponse.text))
            return

        try:
            result = rsponse.json()
        except Exception:
            self.app.show_message(_("Error getting scripts"), str(rsponse.text))
            return
        
        data =[]
        
        for d in result.get('entries', []): 
            data.append(
                [
                d['inum'],
                d.get('name', ''),
                d.get('description',''),   ## some scopes have no scopetypr
                ]
            )

        if data:

            scripts = JansVerticalNav(
                    myparent=self.app,
                    headers=['inum', 'Name', 'Description'],
                    preferred_size= [12, 25, 0],
                    data=data,
                    #on_enter=self.edit_scope_dialog,
                    on_display=self.app.data_display_dialog,
                    #on_delete=self.delete_scope,
                    # selection_changed=self.data_selection_changed,
                    selectes=0,
                    headerColor='class:outh-verticalnav-headcolor',
                    entriesColor='class:outh-verticalnav-entriescolor',
                    all_data=result['entries']
                )

            buttons = []
            if start_index > 1:
                handler_partial = partial(get_next, start_index-self.app.entries_per_page, pattern)
                prev_button = Button(_("Prev"), handler=handler_partial)
                prev_button.window.jans_help = _("Retreives previous %d entries") % self.app.entries_per_page
                buttons.append(prev_button)
            if  result['start'] + self.app.entries_per_page <  result['totalEntriesCount']:
                handler_partial = partial(get_next, start_index+self.app.entries_per_page, pattern)
                next_button = Button(_("Next"), handler=handler_partial)
                next_button.window.jans_help = _("Retreives next %d entries") % self.app.entries_per_page
                buttons.append(next_button)

            self.scripts_list_container = HSplit([
                Window(height=1),
                scripts,
                VSplit(buttons, padding=5, align=HorizontalAlign.CENTER),
            ], height=D())
            self.app.layout.focus(scripts)
            get_app().invalidate()

        else:
            self.app.show_message(_("Oops"), _("No matching result"),tobefocused = self.scripts_main_area)


    def search_scripts(self, tbuffer):
        if not len(tbuffer.text) > 2:
            self.app.show_message(_("Error!"), _("Search string should be at least three characters"), tobefocused=self.scripts_main_area)
            return

        t = threading.Thread(target=self.scripts_update_list, args=(1, tbuffer.text), daemon=True)
        t.start()


    def add_script(self):
        pass
