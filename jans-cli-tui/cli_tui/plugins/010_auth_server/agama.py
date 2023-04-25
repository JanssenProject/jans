import os
import json
import asyncio
import zipfile

from datetime import datetime
from typing import Any
from types import SimpleNamespace
from prompt_toolkit.application import Application
from prompt_toolkit.eventloop import get_event_loop
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.lexers import PygmentsLexer, DynamicLexer

from prompt_toolkit.layout.containers import HSplit, VSplit, DynamicContainer, Window
from prompt_toolkit.buffer import Buffer
from prompt_toolkit.formatted_text import HTML
from prompt_toolkit.widgets import Button, Label, TextArea, Box

from utils.multi_lang import _
from utils.utils import DialogUtils, fromisoformat
from utils.static import cli_style, common_strings
from wui_components.jans_vetrical_nav import JansVerticalNav
from wui_components.jans_path_browser import jans_file_browser_dialog, BrowseType
from wui_components.jans_cli_dialog import JansGDialog
from wui_components.jans_table import JansTableWidget
from wui_components.jans_path_browser import JansPathBrowserWidget

class Agama(DialogUtils):
    def __init__(
        self, 
        app: Application
        ) -> None:


        self.app = app
        self.data = []
        self.first_enter = False

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
                jans_help=HTML(_("Press <b>d</b> to see deployment details. Press <b>c</b> to view/set projects configurations."))
            )

        self.main_container =  HSplit([
                    VSplit([
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
        self.first_enter = True
        self.get_agama_projects()

    def display_config(self, event):

        project_data = self.working_container.all_data[self.working_container.selectes]
        project_name = project_data['details']['projectMetadata']['projectName']
        fdata = SimpleNamespace()

        export_current_config_button_title = _("Export Current Config")
        export_sample_config_button_title = _("Export Sample Config")
        import_configuration_button_title = _("Import Configuration")


        async def coroutine():

            cli_args = {'operation_id': 'get-agama-dev-studio-prj-by-name', 'url_suffix': 'name:{}'.format(project_name)}
            self.app.start_progressing(_("Retrieving details for project {}".format(project_name)))
            details_response = await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing()

            if details_response.status_code == 204:
                self.app.show_message(_(common_strings.info), _("Project {} is still being deployed. Try again in 1 minute.").format(project_name), tobefocused=self.working_container)
                return
            elif details_response.status_code == 200:
                project_details = details_response.json()

            else:
                self.app.show_message(_(common_strings.error), HTML(_("Can't get details for project <b>{}</b>").format(project_name)), tobefocused=self.working_container)
                return


            async def do_import_config_coroutine(config):
                cli_args = {'operation_id': 'put-agama-dev-studio-prj', 'url_suffix':'name:{}'.format(project_name), 'data':config}
                self.app.start_progressing(_("Saving project configuration..."))
                response = await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)

                self.app.stop_progressing()

                if response.status_code in (200, 202):
                    self.app.show_message(_(common_strings.info), HTML(_("Configuration for project <b>{}</b> was imported successfully.").format(project_name)), tobefocused=fdata.main_dialog)
                else:
                    self.app.show_message(_(common_strings.error), HTML(_("Failed to import configuration for project <b>{}</b>: {} {}").format(project_name, response.status_code, response.reason)), tobefocused=fdata.main_dialog)

            def read_config_file(path):
                try:
                    with open(path) as f:
                        config_s = f.read()
                except Exception as e:
                    self.app.show_message(_(common_strings.error), _("An error ocurred reading file") + ":\n{}".format(str(e)), tobefocused=fdata.main_dialog)
                    return

                try:
                    config = json.loads(config_s)
                except Exception as e:
                    self.app.show_message(_(common_strings.error), _("An error ocurred while parsing json") + ":\n{}".format(str(e)), tobefocused=fdata.main_dialog)
                    return

                asyncio.ensure_future(do_import_config_coroutine(config))



            def import_config():
                file_browser_dialog = jans_file_browser_dialog(self.app, path=self.app.browse_path, browse_type=BrowseType.file, ok_handler=read_config_file)
                self.app.show_jans_dialog(file_browser_dialog)


            def save_data(path):

                try:
                    with open(path, 'w') as w:
                        w.write(fdata.save_data)
                    self.pbar_text = _("File {} was saved".format(path))
                    self.app.show_message(_(common_strings.info), _("File {} was successfully saved").format(path), tobefocused=fdata.main_dialog)
                except Exception as e:
                    self.app.show_message(_(common_strings.error), _("An error ocurred while saving") + ":\n{}".format(str(e)), tobefocused=fdata.main_dialog)



            async def get_current_config_coroutine():

                cli_args = {'operation_id': 'get-agama-dev-prj-configs', 'url_suffix':'name:{}'.format(project_name)}
                self.app.start_progressing(_("Retrieving project configuration..."))
                response = await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
                self.app.stop_progressing()

                result = None
                try:
                    result = response.json()
                    fdata.save_status = export_current_config_button_title
                except Exception:
                    result = response.text

                if not result:
                    self.app.show_message(_(common_strings.info), _("No configurations defined for {}").format(project_name), tobefocused=fdata.main_dialog)
                    return

                fdata.save_data = json.dumps(result, indent=2)
                file_browser_dialog = jans_file_browser_dialog(self.app, path=self.app.browse_path, browse_type=BrowseType.save_as, ok_handler=save_data)
                self.app.show_jans_dialog(file_browser_dialog)

            def export_current_config():
                asyncio.ensure_future(get_current_config_coroutine())


            def export_sample_config():
                if not project_details['details']['projectMetadata'].get('configs'):
                    self.app.show_message(_(common_strings.info), _("No sample configurations defined for project {}").format(project_name), tobefocused=fdata.main_dialog)
                    return

                fdata.save_data = json.dumps(project_details['details']['projectMetadata']['configs'], indent=2)
                file_browser_dialog = jans_file_browser_dialog(self.app, path=self.app.browse_path, browse_type=BrowseType.save_as, ok_handler=save_data)
                self.app.show_jans_dialog(file_browser_dialog)

            export_sample_config_button = Box(Button(export_sample_config_button_title, width=len(export_sample_config_button_title)+4, handler=export_sample_config))
            export_current_config_button = Box(Button(export_current_config_button_title, width=len(export_current_config_button_title)+4, handler=export_current_config))
            import_configuration_button = Box(Button(import_configuration_button_title, width=len(import_configuration_button_title)+4, handler=import_config))

            dialog_title = _("Manage Configuration for Project {}").format(project_name)
            dialog = JansGDialog(
                        self.app,
                        title=dialog_title,
                        body=HSplit([export_sample_config_button, export_current_config_button, import_configuration_button], width=D(), padding=1),
                        buttons=[Button('Close')],
                        width=len(dialog_title)+8
                        )
            fdata.main_dialog = dialog
            self.app.show_jans_dialog(dialog)


        if not project_data.get('finishedAt'):
            asyncio.ensure_future(self.get_projects_coroutine())

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

        if not data_display and self.app.current_page == 'agama' and search_str:
            self.app.show_message(_("Oops"), _(common_strings.no_matching_result), tobefocused = self.main_container)
            return

        self.working_container.hide_headers = False
        for datum in data_display[start_index:start_index+self.app.entries_per_page]:
            self.working_container.add_item(datum)

        if not self.first_enter:
            self.app.layout.focus(self.working_container)

        self.first_enter = False


    async def get_projects_coroutine(self, search_str=''):
        cli_args = {'operation_id': 'get-agama-dev-prj'}
        self.app.start_progressing(_("Retreiving agama projects..."))
        response = await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
        self.app.stop_progressing()

        try:
            self.data = response.json()
        except Exception:
            if self.app.current_page == 'agama':
                self.app.show_message(_(common_strings.error), HTML(_("Server reterned non json data <i>{}</i>").format(response.text)), tobefocused=self.app.center_container)
            return

        if not 'entriesCount' in self.data:
            if self.app.current_page == 'agama':
                self.app.show_message(_(common_strings.error), HTML(_("Server reterned unexpected data <i>{}</i>").format(self.data)), tobefocused=self.app.center_container)
            return

        self.working_container.all_data = self.data.get('entries', [])
        self.update_agama_container(search_str=search_str)


    def get_agama_projects(self, search_str=''):
        asyncio.ensure_future(self.get_projects_coroutine(search_str))

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
                response = await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
                self.app.stop_progressing()

                if response:
                    self.app.show_message(_(common_strings.error), HTML(_("Deleting project <b>{}</b> was failed: {} {}").format(project_name, response.status_code, response.reason)), tobefocused=self.working_container)

                self.get_agama_projects()

            asyncio.ensure_future(coroutine())

        dialog = self.app.get_confirm_dialog(
            message = HTML(_("This will remove flows, assets, code and libraries belonging to project <b>{}</b>. Are you sure want to delete?").format(project_name)),
            confirm_handler=do_delete_agama_project
            )

        self.app.show_jans_dialog(dialog)


    def display_details(self,  **params: Any) -> None:
        project_name = params['data']['details']['projectMetadata'].get('projectName')

        async def coroutine():
            cli_args = {'operation_id': 'get-agama-dev-studio-prj-by-name', 'url_suffix': 'name:{}'.format(project_name)}
            self.app.start_progressing(_("Retrieving details for project {}".format(project_name)))
            response = await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing()

            if response.status_code == 200:

                result = response.json()
                project_metadata = result['details']['projectMetadata']
                body_widgets = [
                        self.app.getTitledText(title=_("Version"), value=project_metadata.get('version') or '-', name='version', read_only=True),
                        self.app.getTitledText(title=_("Description"), value=project_metadata.get('description') or '-', name='description', read_only=True, height=2),
                        self.app.getTitledText(title=_("Deployed started on"), value=result['createdAt'], name='createdAt', read_only=True),
                        self.app.getTitledText(title=_("Deployed finished on"), value=result['finishedAt'], name='finishedAt', read_only=True),
                        self.app.getTitledText(title=_("Errors"), value=result['details'].get('error') or "No", name='error', read_only=True),
                        Window(height=1)
                    ]

                flow_errors = result['details'].get('flowsError', {})

                if flow_errors:
                    jans_table = JansTableWidget(
                        app=self.app,
                        data=list(flow_errors.items()),
                        headers=["Flow", "Error"],
                        )
                    body_widgets.append(jans_table)

                buttons = [Button(_("Close"))]
                dialog = JansGDialog(self.app, body=HSplit(body_widgets), title=_("Details of project {}").format(project_name), buttons=buttons)
                self.app.show_jans_dialog(dialog)


            elif response.status_code == 204:
                self.app.show_message(_(common_strings.info), HTML(_("Project <b>{}</b> is still being deployed. Try again in 1 minute.").format(project_name)), tobefocused=self.working_container)

            else:
                self.app.show_message(_(common_strings.error), HTML(_("Retrieving details for <b>{}</b> was failed: {} {}").format(project_name, response.status_code, response.reason)), tobefocused=self.working_container)

        if project_name:
            if params['selected'][3] == _("Pending"):
                asyncio.ensure_future(self.get_projects_coroutine())
            asyncio.ensure_future(coroutine())
            

