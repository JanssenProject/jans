import os
import json
import asyncio
import zipfile

from datetime import datetime
from typing import Any

from prompt_toolkit.application import Application
from prompt_toolkit.eventloop import get_event_loop
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.layout.containers import HSplit, VSplit, DynamicContainer
from prompt_toolkit.buffer import Buffer
from prompt_toolkit.formatted_text import HTML

from utils.multi_lang import _
from utils.utils import DialogUtils
from utils.static import cli_style, common_strings
from wui_components.jans_vetrical_nav import JansVerticalNav
from wui_components.jans_path_browser import jans_file_browser_dialog, BrowseType

class Agama(DialogUtils):
    def __init__(
        self, 
        app: Application
        ) -> None:

        self.app = self.myparent = app
        self.data = []
        self.working_container = JansVerticalNav(
                myparent=app,
                headers=[_("Project Name"), _("Type"), _("Author"), _("Updated")],
                preferred_size= self.app.get_column_sizes(.25, .25 , .3, .1, .1),
                on_display=self.app.data_display_dialog,
                on_delete=self.delete_agama_project,
                selectes=0,
                headerColor=cli_style.navbar_headcolor,
                entriesColor=cli_style.navbar_entriescolor,
                hide_headers = True
            )

        self.main_container =  HSplit([
                    VSplit([
                        self.app.getButton(text=_("Get Projects"), name='oauth:agama:get', jans_help=_("Retreive all Agama Projects"), handler=self.get_agama_projects),
                        self.app.getTitledText(_("Search"), name='oauth:agama:search', jans_help=_(common_strings.enter_to_search), accept_handler=self.search_agama_project, style=cli_style.edit_text),
                        self.app.getButton(text=_("Upload Project"), name='oauth:agama:add', jans_help=_("To add a new Agama project press this button"), handler=self.upload_project),
                        ],
                        padding=3,
                        width=D(),
                    ),
                    DynamicContainer(lambda: self.working_container)
                    ], style=cli_style.container)


    def update_agama_container(self, start_index=0, search_str=''):

        self.working_container.clear()
        data_display = []

        for agama in self.data.get('entries', []):
            if search_str.lower():
                project_str = ' '.join((
                        agama['details']['projectMetadata'].get('projectName'),
                        agama['details']['projectMetadata'].get('author', ''),
                        agama['details']['projectMetadata'].get('type', ''),
                        agama['details']['projectMetadata'].get('description', '')
                        )).lower()
                if search_str not in project_str:
                    continue

            dt_object = datetime.fromisoformat(agama['createdAt'])

            data_display.append((
                        agama['details']['projectMetadata'].get('projectName'),
                        agama['details']['projectMetadata'].get('type', '??'),
                        agama['details']['projectMetadata'].get('author', '??'),
                        '{:02d}/{:02d}/{}'.format(dt_object.day, dt_object.month, str(dt_object.year)[2:])
                    ))

        if not data_display:
            self.app.show_message(_("Oops"), _(common_strings.no_matching_result), tobefocused = self.main_container)
            return

        self.working_container.hide_headers = False
        for datum in data_display[start_index:start_index+self.app.entries_per_page]:
            self.working_container.add_item(datum)

        self.app.layout.focus(self.working_container)

    def get_agama_projects(self, search_str=''):
        async def coroutine():
            cli_args = {'operation_id': 'get-agama-dev-prj'}
            self.app.start_progressing(_("Retreiving agama projects..."))
            response = await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing()
            try:
                self.data = response.json()
            except Exception:
                self.app.show_message(_(common_strings.error), HTML(_("Server reterned non json data <i>{}</i>").format(response.text)), tobefocused=self.app.center_container)
                return

            if not 'entriesCount' in self.data:
                self.app.show_message(_(common_strings.error), HTML(_("Server reterned unexpected data <i>{}</i>").format(self.data)), tobefocused=self.app.center_container)
                return

            self.working_container.all_data = self.data.get('entries', [])
            self.update_agama_container(search_str=search_str)

        asyncio.ensure_future(coroutine())

    def upload_project(self):

        def do_upload_project(path):
            try:
                project_zip = zipfile.ZipFile(path)
            except Exception:
                self.app.show_message(_(common_strings.error), HTML(_("Can't open <b>{}</b> as zip file.").format(path)), tobefocused=self.app.center_container)
                return

            try:
                project_json = json.loads(project_zip.read('project.json'))
            except Exception as e:
                self.app.show_message(_(common_strings.error), HTML(_("Can't read <b>project.json</b> from zip file:\n {}").format(str(e))), tobefocused=self.app.center_container)
                return

            if 'projectName' not in project_json:
                self.app.show_message(_(common_strings.error), HTML(_("Property <b>projectName</b> does not exist in <b>project.json</b>.")), tobefocused=self.app.center_container)
                return

            async def coroutine():
                cli_args = {'operation_id': 'post-agama-dev-studio-prj', 'data_fn': path, 'url_suffix':'name:{}'.format(project_json['projectName'])}
                self.app.start_progressing(_("Uploading agama project..."))
                await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
                self.app.stop_progressing()
                self.get_agama_projects()

            asyncio.ensure_future(coroutine())


        file_browser_dialog = jans_file_browser_dialog(self.app, path=self.app.browse_path, browse_type=BrowseType.file, ok_handler=do_upload_project)
        self.app.show_jans_dialog(file_browser_dialog)

    def search_agama_project(self, tbuffer:Buffer) -> None:
        if 'entries' in self.data:
            self.update_agama_container(search_str=tbuffer.text)
        else:
            self.get_agama_projects(search_str=tbuffer.text)

    def delete_agama_project(self, **kwargs: Any) -> None:
        agama = self.data['entries'][kwargs['selected_idx']]
        project_name = agama['details']['projectMetadata']['projectName']

        def do_delete_agama_project(result):
            async def coroutine():
                cli_args = {'operation_id': 'delete-agama-dev-studio-prj', 'url_suffix': 'name:{}'.format(agama['details']['projectMetadata']['projectName'])}
                self.app.start_progressing(_("Deleting agama project {}".format(project_name)))
                await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
                self.app.stop_progressing()
                self.get_agama_projects()

            asyncio.ensure_future(coroutine())

        dialog = self.app.get_confirm_dialog(
            message = HTML(_("Are you sure want to delete Agama Project <b>{}</b>?").format(project_name)),
            confirm_handler=do_delete_agama_project
            )

        self.app.show_jans_dialog(dialog)


