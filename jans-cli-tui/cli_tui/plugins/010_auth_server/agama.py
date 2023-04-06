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
from prompt_toolkit.widgets import Button, Label

from utils.multi_lang import _
from utils.utils import DialogUtils, fromisoformat
from utils.static import cli_style, common_strings
from wui_components.jans_vetrical_nav import JansVerticalNav
from wui_components.jans_path_browser import jans_file_browser_dialog, BrowseType
from wui_components.jans_cli_dialog import JansGDialog
from wui_components.jans_table import JansTableWidget

class Agama(DialogUtils):
    def __init__(
        self, 
        app: Application
        ) -> None:

        

        self.app = app
        self.data = []

        self.working_container = JansVerticalNav(
                myparent=app,
                headers=[_("Project Name"), _("Type"), _("Author"), _("Status"), _("Deployed on"), _("Errors")],
                preferred_size=self.app.get_column_sizes(.2, .2, .2, .15, .15, .1),
                on_display=self.display_details,
                on_delete=self.delete_agama_project,
                selectes=0,
                headerColor=cli_style.navbar_headcolor,
                entriesColor=cli_style.navbar_entriescolor,
                hide_headers = True,
                custom_key_bindings=([('c', self.display_config)]),
                jans_help=_("Press c to display configuration for project")
            )

        self.main_container =  HSplit([
                    VSplit([
                        self.app.getButton(text=_("Get Projects"), name='oauth:agama:get', jans_help=_("Retrieve all Agama Projects"), handler=self.get_agama_projects),
                        self.app.getTitledText(_("Search"), name='oauth:agama:search', jans_help=_(common_strings.enter_to_search), accept_handler=self.search_agama_project, style=cli_style.edit_text),
                        self.app.getButton(text=_("Upload Project"), name='oauth:agama:add', jans_help=_("To add a new Agama project press this button"), handler=self.upload_project),
                        ],
                        padding=3,
                        width=D(),
                    ),
                    DynamicContainer(lambda: self.working_container)
                    ], style=cli_style.container)

        self.main_container.on_page_enter = self.on_page_enter

    def on_page_enter(self) -> None:
        self.get_agama_projects()

    def display_config(self, event):

        project_data = self.working_container.all_data[self.working_container.selectes]
        project_name = project_data['details']['projectMetadata']['projectName']

        async def coroutine():
            cli_args = {'operation_id': 'get-agama-dev-prj-configs', 'url_suffix':'name:{}'.format(project_name)}
            self.app.start_progressing(_("Retrieving project configuration..."))
            response = await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing()

            try:
                result = response.json()
            except Exception:
                result = response.text

            if result:
                self.app.data_display_dialog(title=_("Configuration for") + " " + project_name, data=response.json())
            else:
                self.app.show_message(_(common_strings.error), "Server did not return configuration for {}".format(project_name), tobefocused=self.working_container)


        asyncio.ensure_future(coroutine())


    def update_agama_container(self, start_index=0, search_str=''):

        self.working_container.clear()
        data_display = []

        for agama in self.data.get('entries', []):
            project_details = agama['details']
            project_metadata = project_details['projectMetadata']

            if search_str.lower():
                project_str = ' '.join((
                        project_metadata.get('projectName'),
                        project_metadata.get('author', ''),
                        project_metadata.get('type', ''),
                        project_metadata.get('description', '')
                        )).lower()
                if search_str not in project_str:
                    continue

            dt_object = fromisoformat(agama['createdAt'])
            if agama.get('finishedAt'):
                status = _("Processed")
                deployed_on = '{:02d}/{:02d}/{} {:02d}:{:02d}'.format(dt_object.day, dt_object.month, str(dt_object.year)[2:], dt_object.hour, dt_object.minute)
                error = 'Yes' if project_details.get('error') else 'No'
            else:
                status = _("Pending")
                deployed_on = '-'
                error = ''

            data_display.append((
                        project_metadata.get('projectName'),
                        project_metadata.get('type', '-'),
                        project_metadata.get('author', '-'),
                        status,
                        deployed_on,
                        error
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

        def project_uploader(path, project_name):
            async def coroutine():
                cli_args = {'operation_id': 'post-agama-dev-studio-prj', 'data_fn': path, 'url_suffix':'name:{}'.format(project_name)}
                self.app.start_progressing(_("Uploading agama project..."))
                await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
                self.app.stop_progressing()
                self.get_agama_projects()

            asyncio.ensure_future(coroutine())


        def aks_project_name(path):
            body = self.app.getTitledText(_("Project Name"), name='oauth:agama:projectname', style=cli_style.edit_text)
            ok = _("OK")
            buttons = [Button(_("Cancel")), Button(ok)]
            dialog = JansGDialog(self.app, body=body, title=_("Enter Project Name"), buttons=buttons, width=self.app.dialog_width-20)

            async def coroutine():
                focused_before = self.app.layout.current_window
                result = await self.app.show_dialog_as_float(dialog)

                try:
                    self.app.layout.focus(focused_before)
                except Exception:
                    self.app.layout.focus(self.app.center_frame)

                if result != ok:
                    return

                project_name = body.me.text.strip()

                if not project_name:
                   aks_project_name(path)

                project_uploader(path, project_name)

            return asyncio.ensure_future(coroutine())


        def do_upload_project(path):
            project_name = None

            try:
                project_zip = zipfile.ZipFile(path)
            except Exception:
                self.app.show_message(_(common_strings.error), HTML(_("Can't open <b>{}</b> as zip file.").format(path)), tobefocused=self.app.center_container)
                return

            try:
                project_json = json.loads(project_zip.read('project.json'))
                project_name = project_json['projectName']
                project_uploader(path, project_name)
            except Exception:
                aks_project_name(path)

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


    def display_details(self,  **params: Any) -> None:
        project_name = params['data']['details']['projectMetadata'].get('projectName')

        def display_error(**params):
            body = HSplit([
                    self.app.getTitledText(title=_("Flow"), value=params['data'][0], name='flow', read_only=True),
                    self.app.getTitledText(title=_("Error"), value=params['data'][1], name='flow', read_only=True, focusable=True, scrollbar=True, height=3),
                    ])
            dialog = JansGDialog(self.app, body=body, title=_("Error Details"), buttons=[Button(_("Close"))], width=self.app.dialog_width-6)
            self.app.show_jans_dialog(dialog)


        async def coroutine():
            cli_args = {'operation_id': 'get-agama-dev-studio-prj-by-name', 'url_suffix': 'name:{}'.format(project_name)}
            self.app.start_progressing(_("Retrieving details for project {}".format(project_name)))
            response = await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing()

            if response.status_code == 200:

                result = response.json()
                body_widgets = [
                        self.app.getTitledText(title=_("Description"), value=result['details']['projectMetadata'].get('description','-'), name='description', read_only=True, height=2),
                        self.app.getTitledText(title=_("Deployed started on"), value=result['createdAt'], name='createdAt', read_only=True),
                        self.app.getTitledText(title=_("Deployed finished on"), value=result['finishedAt'], name='finishedAt', read_only=True),
                        self.app.getTitledText(title=_("Error"), value=result['details'].get('error') or "No", name='error', read_only=True),
                    ]

                flow_errors = result['details'].get('flowsError')

                if flow_errors:
                    jans_table = JansTableWidget(
                        app=self.app,
                        data=list(flow_errors.items()),
                        headers=["Flow", "Error"],
                        on_enter=display_error
                        )
                    body_widgets.append(jans_table)

                buttons = [Button(_("Close"))]
                dialog = JansGDialog(self.app, body=HSplit(body_widgets), title=_("Details of project {}").format(project_name), buttons=buttons)
                self.app.show_jans_dialog(dialog)


            elif response.status_code == 204:
                self.app.show_message(_(common_strings.error), "Project {} is still being deployed. Try again in 1 minute.".format(project_name), tobefocused=self.working_container)

        if project_name:

            asyncio.ensure_future(coroutine())


