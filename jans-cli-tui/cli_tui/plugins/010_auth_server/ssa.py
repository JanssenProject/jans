import os
import asyncio
from datetime import datetime
from typing import Any

from prompt_toolkit.application import Application
from prompt_toolkit.eventloop import get_event_loop
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.layout.containers import HSplit, VSplit
from prompt_toolkit.layout.containers import DynamicContainer, Window
from prompt_toolkit.widgets import Button, Label, Checkbox
from prompt_toolkit.buffer import Buffer

from utils.utils import DialogUtils
from utils.static import cli_style, common_strings
from wui_components.jans_vetrical_nav import JansVerticalNav
from wui_components.jans_cli_dialog import JansGDialog
from wui_components.jans_label_container import JansLabelContainer
from wui_components.jans_date_picker import DateSelectWidget

from cli import config_cli


from utils.multi_lang import _

class SSA(DialogUtils):
    def __init__(
        self, 
        app: Application
        ) -> None:

        self.app = self.myparent = app
        self.data = []
        self.working_container = JansVerticalNav(
                myparent=app,
                headers=[_("Software ID"), _("Organisation"), _("Software Roles"), _("Status"), _("Exp.")],
                preferred_size= self.app.get_column_sizes(.25, .25 , .3, .1, .1),
                on_display=self.app.data_display_dialog,
                on_delete=self.delete_ssa,
                selectes=0,
                headerColor=cli_style.navbar_headcolor,
                entriesColor=cli_style.navbar_entriescolor,
                hide_headers = True
            )

        self.main_container =  HSplit([
                    VSplit([
                        self.app.getButton(text=_("Get SSA"), name='oauth:ssa:get', jans_help=_("Retreive all SSA"), handler=self.get_ssa),
                        self.app.getTitledText(_("Search"), name='oauth:ssa:search', jans_help=_(common_strings.enter_to_search), accept_handler=self.search_ssa, style=cli_style.edit_text),
                        self.app.getButton(text=_("Add SSA"), name='oauth:ssa:add', jans_help=_("To add a new SSA press this button"), handler=self.add_ssa),
                        ],
                        padding=3,
                        width=D(),
                    ),
                    DynamicContainer(lambda: self.working_container)
                    ],style='class:outh_containers_scopes')


        self.cli_object = config_cli.JCA_CLI(
                host=config_cli.host,
                client_id=config_cli.client_id,
                client_secret=config_cli.client_secret,
                access_token=config_cli.access_token,
                op_mode = 'auth'
            )


    def update_ssa_container(self, start_index=0, search_str=''):

        self.working_container.clear()
        data_display = []

        for ssa in self.data:
            if search_str and (search_str not in ssa['ssa']['software_id'] and search_str not in str(ssa['ssa']['org_id']) and search_str not in ssa['ssa']['software_roles']):
                continue
            try:
                dt_object = datetime.fromtimestamp(ssa['ssa']['exp'])
            except Exception:
                continue
            data_display.append((
                        str(ssa['ssa']['software_id']),
                        str(ssa['ssa']['org_id']),
                        ','.join(ssa['ssa']['software_roles']),
                        ssa['status'],
                        '{:02d}/{:02d}/{}'.format(dt_object.day, dt_object.month, str(dt_object.year)[2:])
                    ))

        if not data_display:
            self.app.show_message(_("Oops"), _(common_strings.no_matching_result), tobefocused = self.main_container)
            return

        self.working_container.hide_headers = False
        for datum in data_display[start_index:start_index+self.app.entries_per_page]:
            self.working_container.add_item(datum)

        self.app.layout.focus(self.working_container)

    def get_ssa(self, search_str=''):
        async def coroutine():
            cli_args = {'operation_id': 'get-ssa', 'cli_object': self.cli_object}
            self.app.start_progressing(_("Retreiving ssa..."))
            response = await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing()
            self.data = response.json()
            self.working_container.all_data = self.data
            self.update_ssa_container(search_str=search_str)

        asyncio.ensure_future(coroutine())

    def add_ssa(self):
        self.edit_ssa_dialog()


    def edit_ssa(self, **params: Any) -> None:
        data = self.data[params['selected']]['ssa']
        self.edit_ssa_dialog(data=data)

    def save_ssa(self, dialog):
        new_data = self.make_data_from_dialog(tabs={'ssa': dialog.body})

        if self.never_expire_cb.checked:
            # set expiration to 50 years
            new_data['expiration'] = int(datetime.now().timestamp()) + 1576800000
        else:
            if self.expire_widget.value:
                new_data['expiration'] = int(datetime.fromisoformat(self.expire_widget.value).timestamp())

        new_data['software_roles'] = new_data['software_roles'].splitlines()

        new_data['software_roles'] = new_data['software_roles'].splitlines()

        if self.check_required_fields(dialog.body, data=new_data):

            async def coroutine():
                operation_id = 'post-register-ssa'
                cli_args = {'operation_id': operation_id, 'cli_object': self.cli_object, 'data': new_data}
                self.app.start_progressing(_("Saving ssa..."))
                result = await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
                self.app.stop_progressing()
                ssa = result.json()
                self.app.data_display_dialog(data=ssa, title=_("SSA Token"), message=_("Save and store it securely. This is the only time you see this token."))

                self.get_ssa()
                dialog.future.set_result(True)

            asyncio.ensure_future(coroutine())

    def edit_ssa_dialog(self, data=None):
        if data:
            title = _("Edit SSA")
        else:
            data = {}
            title = _("Add new SSA")

        expiration_label = _("Expiration")
        never_expire_label = _("Never")
        self.never_expire_cb = Checkbox(never_expire_label)
        expiration_iso = datetime.fromtimestamp(data['exp']).isoformat() if 'exp' in data else ''
        self.expire_widget = DateSelectWidget(value=expiration_iso, parent=self)

        body = HSplit([

                self.app.getTitledText(
                    title=_("Software ID"),
                    name='software_id',
                    value=data.get('software_id',''),
                    style=cli_style.edit_text_required
                ),

                self.app.getTitledText(
                    title=_("Organisation"),
                    name='org_id',
                    value=data.get('org_id',''),
                    style=cli_style.edit_text_required
                ),

                self.app.getTitledText(
                    title=_("Description"),
                    name='description',
                    value=data.get('description',''),
                    style=cli_style.edit_text_required
                ),

                self.app.getTitledCheckBoxList(
                    title=_("Grant Types"),
                    name='grant_types',
                    values=[('authorization_code', 'Authorization Code'), ('refresh_token', 'Refresh Token'), ('urn:ietf:params:oauth:grant-type:uma-ticket', 'UMA Ticket'), ('client_credentials', 'Client Credentials'), ('password', 'Password'), ('implicit', 'Implicit')],
                    current_values=data.get('grant_types', []),
                    style=cli_style.check_box,
                ),

                self.app.getTitledText(
                    title=_("Software Roles"),
                    name='software_roles',
                    value='\n'.join(data.get('software_roles', [])),
                    height=3,
                    style=cli_style.edit_text_required
                ),

                self.app.getTitledCheckBox(
                            _("One Time Use"), 
                            name='one_time_use', 
                            checked=data.get('one_time_use', False),
                            style=cli_style.check_box
                            ),

                self.app.getTitledCheckBox(
                            _("Rotate SSA"), 
                            name='rotate_ssa', 
                            checked=data.get('rotate_ssa', False),
                            style=cli_style.check_box
                            ),

                VSplit([
                    Label(expiration_label + ': ', width=len(expiration_label)+2, style=cli_style.edit_text),
                    HSplit([self.never_expire_cb], width=len(never_expire_label)+7),
                    self.expire_widget,
                    ], height=1),

            ])

        save_button = Button(_("Save"), handler=self.save_ssa)
        save_button.keep_dialog = True
        canncel_button = Button(_("Cancel"))
        buttons = [save_button, canncel_button]
        dialog = JansGDialog(self.app, title=title, body=body, buttons=buttons)
        self.app.show_jans_dialog(dialog)

    def search_ssa(self, tbuffer:Buffer) -> None:
        if self.data:
            self.update_ssa_container(search_str=tbuffer.text)
        else:
            self.get_ssa(search_str=tbuffer.text)

    def delete_ssa(self, **kwargs: Any) -> None:
        jti = self.data[kwargs['selected_idx']]['ssa']['jti']

        def do_delete_ssa(result):

            async def coroutine():
                cli_args = {'operation_id': 'delete-ssa', 'cli_object': self.cli_object, 'url_suffix': 'jti:{}'.format(jti)}
                self.app.start_progressing(_("Deleting ssa {}".format(jti)))
                await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
                self.app.stop_progressing()
                self.get_ssa()

            asyncio.ensure_future(coroutine())

        dialog = self.app.get_confirm_dialog(
            message = _("Are you sure want to delete SSA jti:") + ' ' + jti,
            confirm_handler=do_delete_ssa
            )

        self.app.show_jans_dialog(dialog)


