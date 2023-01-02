from functools import partial
import asyncio
from prompt_toolkit.application.current import get_app
from prompt_toolkit.layout.containers import (
    HSplit,
    VSplit,
    HorizontalAlign,
    DynamicContainer,
    Window,
)
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.widgets import (
    Button,
    Dialog
)
from typing import Any, Optional
from prompt_toolkit.buffer import Buffer
from utils.static import DialogResult
from wui_components.jans_vetrical_nav import JansVerticalNav
from edit_script_dialog import EditScriptDialog
from prompt_toolkit.application import Application
from utils.multi_lang import _

class Plugin():
    """This is a general class for plugins 
    """
    def __init__(
        self, 
        app: Application
        ) -> None:
        """init for Plugin class "scripts"

        Args:
            app (Generic): The main Application class
        """
        self.app = app
        self.pid = 'scripts'
        self.name = 'Sc[r]ipts'

        self.scripts_prepare_containers()

    def process(self) -> None:
        pass

    def set_center_frame(self) -> None:
        """center frame content
        """
        self.app.center_container = self.scripts_main_area

    def scripts_prepare_containers(self) -> None:
        """prepare the main container (tabs) for the current Plugin 
        """
        self.scripts_list_container = HSplit([],width=D(), height=D())

        self.scripts_main_area = HSplit([
                    VSplit([
                        self.app.getButton(text=_("Get Scripts"), name='scripts:get', jans_help=_("Retreive first %d Scripts") % (20), handler=self.get_scripts),
                        self.app.getTitledText(_("Search"), name='scripts:search', jans_help=_("Press enter to perform search"), accept_handler=self.search_scripts, style='class:outh_containers_scopes.text'),
                        self.app.getButton(text=_("Add Sscript"), name='scripts:add', jans_help=_("To add a new scope press this button"), handler=self.add_script_dialog),
                        ],
                        padding=3,
                        width=D(),
                    ),
                    DynamicContainer(lambda: self.scripts_list_container)
                    ],style='class:outh_containers_scopes')

    def get_scripts(
        self, 
        start_index: Optional[int]= 0,
        pattern: Optional[str]= '',
        ) -> None:
        """Get the current Scripts from server

        Args:
            start_index (Optional[int], optional): This is flag for the Scripts pages. Defaults to 0.
            pattern (Optional[str], optional):endpoint arguments for the Scripts. Defaults to ''.
        """

        endpoint_args ='limit:{},startIndex:{}'.format(self.app.entries_per_page, start_index)
        if pattern:
            endpoint_args +=',pattern:'+pattern

        cli_args = {'operation_id': 'get-config-scripts', 'endpoint_args': endpoint_args}

        async def coroutine():
            self.app.start_progressing()
            response = await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing()
            self.users = response.json()

            if not self.users.get('entries'):
                self.app.show_message(_("Not found"), _("No script found for this search."), tobefocused=self.app.center_container)
                return

            self.data = response.json()
            self.scripts_update_list(pattern)
            self.app.layout.focus(self.scripts_list_container)

        asyncio.ensure_future(coroutine())

    def scripts_update_list(
        self, 
        pattern: Optional[str]= '',
        ) -> None:
        """Updates Scripts data from server

        Args:
            pattern (Optional[str], optional):endpoint arguments for the Scripts. Defaults to ''.
        """

        data =[]
        
        for d in self.data.get('entries', []): 
            data.append(
                [
                d['inum'],
                d.get('name', ''),
                d.get('description',''),
                ]
            )

        self.scripts_listbox = JansVerticalNav(
                myparent=self.app,
                headers=['inum', 'Name', 'Description'],
                preferred_size= [15, 25, 0],
                data=data,
                on_enter=self.add_script_dialog,
                on_display=self.app.data_display_dialog,
                get_help=(self.get_help,'Scripts'),
                on_delete=self.delete_script,
                selectes=0,
                headerColor='class:outh-verticalnav-headcolor',
                entriesColor='class:outh-verticalnav-entriescolor',
                all_data=self.data['entries']
            )

        buttons = []

        if self.data['start'] > 1:
            handler_partial = partial(self.get_scripts, self.data['start']-self.app.entries_per_page-1, pattern)
            prev_button = Button(_("Prev"), handler=handler_partial)
            prev_button.window.jans_help = _("Retreives previous %d entries") % self.app.entries_per_page
            buttons.append(prev_button)
        if self.data['totalEntriesCount'] > self.data['start'] + self.data['entriesCount']:
            handler_partial = partial(self.get_scripts, self.data['start']+self.app.entries_per_page+1, pattern)
            next_button = Button(_("Next"), handler=handler_partial)
            next_button.window.jans_help = _("Retreives previous %d entries") % self.app.entries_per_page
            buttons.append(next_button)


        self.scripts_list_container = HSplit([
            Window(height=1),
            self.scripts_listbox,
            VSplit(buttons, padding=5, align=HorizontalAlign.CENTER),
        ], height=D())
        self.app.layout.focus(self.scripts_listbox)
        get_app().invalidate()

    def get_help(self, **kwargs: Any):
        """This method get focused field Description to display on statusbar
        """

        # schema = self.app.cli_object.get_schema_from_reference('#/components/schemas/{}'.format(str(kwargs['scheme'])))
        if kwargs['scheme'] == 'Scripts':
            self.app.status_bar_text= kwargs['data'][2]

    def search_scripts(self, tbuffer:Buffer) -> None:
        """This method handel the search for scripts

        Args:
            tbuffer (Buffer): Buffer returned from the TextArea widget > GetTitleText
        """
 
        self.get_scripts(pattern=tbuffer.text)

    def add_script_dialog(self, **kwargs: Any):
        """Method to display the edit script dialog
        """
        if kwargs:
            data = kwargs.get('data', {})
        else:
            data = {}

        title = _("Edit Script") if data else _("Add Script")

        dialog = EditScriptDialog(self.app, title=title, data=data, save_handler=self.save_script)
        result = self.app.show_jans_dialog(dialog)

    def save_script(self, dialog: Dialog) -> None:
        """This method to save the script data to server

        Args:
            dialog (_type_): the main dialog to save data in

        Returns:
            bool :  value to check the status code response
        """

        async def coroutine():
            import json
            operation_id = 'put-config-scripts' if dialog.new_data.get('baseDn') else 'post-config-scripts'
            cli_args = {'operation_id': operation_id, 'data': dialog.new_data}
            self.app.start_progressing()
            response = await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing()
            if response.status_code == 500:
                self.app.show_message(_('Error'), response.text + '\n' + response.reason)
            else:
                dialog.future.set_result(DialogResult.OK)
                self.get_scripts()

        asyncio.ensure_future(coroutine())

    def delete_script(self, **kwargs: Any) -> None:
        """This method for the deletion of the Scripts
        """

        def do_delete_script():

            async def coroutine():
                cli_args = {'operation_id': 'delete-config-scripts-by-inum', 'url_suffix':'inum:{}'.format(kwargs['selected'][0])}
                self.app.start_progressing()
                response = await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
                self.app.stop_progressing()
                if response:
                    self.app.show_message(_("Error"), _("Deletion was not completed {}".format(response)), tobefocused=self.scripts_listbox)
                else:
                    self.scripts_listbox.remove_item(kwargs['selected'])
            asyncio.ensure_future(coroutine())

        buttons = [Button(_("No")), Button(_("Yes"), handler=do_delete_script)]

        self.app.show_message(
                title=_("Confirm"),
                message=_("Are you sure you want to delete script {}?").format(kwargs['selected'][1]),
                buttons=buttons,
                tobefocused=self.scripts_listbox
                )
