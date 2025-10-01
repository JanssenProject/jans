import os
import copy
import json
import time
import hashlib
import asyncio

from typing import Any
from datetime import datetime
from functools import partial

from prompt_toolkit.application import Application
from prompt_toolkit.eventloop import get_event_loop
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.layout.containers import HSplit, VSplit
from prompt_toolkit.layout.containers import DynamicContainer, Window
from prompt_toolkit.widgets import Button, Label, Checkbox, RadioList, Dialog, TextArea
from prompt_toolkit.buffer import Buffer
from prompt_toolkit.formatted_text import HTML

from cli import config_cli
from utils.multi_lang import _
from utils.utils import DialogUtils, fromisoformat
from utils.static import cli_style, common_strings
from wui_components.jans_vetrical_nav import JansVerticalNav
from wui_components.jans_cli_dialog import JansGDialog
from wui_components.jans_label_container import JansLabelContainer
from wui_components.jans_date_picker import DateSelectWidget


class SSA(DialogUtils):
    def __init__(
        self, 
        app: Application
        ) -> None:

        self.app = self.myparent = app
        self.data = []
        self.ssa_templates = {}
        self.templates_dir = self.app.data_dir.joinpath('ssa_templates')

        if not self.templates_dir.exists():
            self.templates_dir.mkdir(parents=True, exist_ok=True)

        self.templates_button = Window(width=0)
        self.load_templates()

        self.working_container = JansVerticalNav(
                myparent=app,
                headers=[_("Software ID"), _("Organization"), _("Software Roles"), _("Status"), _("Exp.")],
                preferred_size= self.app.get_column_sizes(.25, .25 , .3, .1, .1),
                on_display=self.app.data_display_dialog,
                on_delete=self.delete_ssa,
                on_enter=self.ssa_details,
                selectes=0,
                headerColor=cli_style.navbar_headcolor,
                entriesColor=cli_style.navbar_entriescolor,
                hide_headers = True,
                max_height=20
            )

        self.main_container =  HSplit([
                    VSplit([
                        self.app.getTitledText(_("Search"), name='oauth:ssa:search', jans_help=_(common_strings.enter_to_search), accept_handler=self.search_ssa, style=cli_style.edit_text),
                        self.app.getButton(text=_("Add SSA"), name='oauth:ssa:add', jans_help=_("To add a new SSA press this button"), handler=self.add_ssa),
                        DynamicContainer(lambda: self.templates_button),
                        ],
                        padding=3,
                        width=D(),
                    ),
                    DynamicContainer(lambda: self.working_container)
                    ],style='class:outh_containers_scopes')


    def init_cli_object(self):
        self.cli_object = config_cli.JCA_CLI(
                host=self.app.cli_object.idp_host,
                client_id=self.app.cli_object.client_id,
                client_secret=self.app.cli_object.client_secret,
                access_token=self.app.cli_object.access_token,
                op_mode = 'auth'
            )

    def load_templates(self):
        self.ssa_templates.clear()
        for tmp_path in self.templates_dir.glob('ssa-*.json'):
            tmp_data = json.loads(tmp_path.read_text())
            tmp_name = tmp_data['_template_name_']
            self.ssa_templates[tmp_name] = tmp_data

        if self.ssa_templates:
            self.templates_button = self.app.getButton(text=_("Templates"), name='oauth:ssa:load', jans_help=_("To SSA from template press this button"), handler=self.templates_button_handler)
        else:
            self.templates_button = Window(width=0)

    def templates_button_handler(self):
        templates_list = []

        for tmp_name in sorted(self.ssa_templates.keys()):
            templates_list.append((tmp_name, tmp_name))

        templates_radio_list = RadioList(values=templates_list)

        def load_ssa_template(dialog):
            selected_tmp = templates_radio_list.current_value
            template_data = self.ssa_templates[selected_tmp]
            self.edit_ssa_dialog(data=template_data)


        def delete_ssa_template(dialog):
            def do_delete_ssa_template(cdialog):
                template_name_path = self.get_template_path(templates_radio_list.current_value)
                template_name_path.unlink()
                self.load_templates()
                dialog.future.set_result(True)
                self.app.show_message(
                        _("Deleted"),
                        HTML(_("SSA template <b>%s</b> was deleted.") % templates_radio_list.current_value),
                        tobefocused=self.working_container
                    )

            confirm_dialog = self.app.get_confirm_dialog(
                    message=_("Are you sure deleting SSA template <b>%s</b>?") % templates_radio_list.current_value,
                    confirm_handler=do_delete_ssa_template
                    )

            self.app.show_jans_dialog(confirm_dialog)

        delete_button = Button(_("Delete"), handler=delete_ssa_template)
        delete_button.keep_dialog = True
        buttons = [Button(_("Cancel")), Button(_("Load"), handler=load_ssa_template), delete_button]

        templatesDialog = JansGDialog(
            self.app,
            title="SSA Templates",
            body=templates_radio_list,
            buttons=buttons,
            )

        self.app.show_jans_dialog(templatesDialog)


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
                        '{:02d}/{:02d}/{}'.format(dt_object.day, dt_object.month, str(dt_object.year))
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

    def get_template_path(self, template_name):
        sha1 = hashlib.sha1()
        sha1.update(template_name.encode())
        sha1_hash = sha1.hexdigest()
        return self.templates_dir.joinpath(f'ssa-{sha1_hash}.json')


    def save_ssa(self, dialog):
        new_data = self.make_data_from_dialog(tabs={'ssa': dialog.body})

        if self.never_expire_cb.checked:
            # set expiration to 50 years
            new_data['expiration'] = int(datetime.now().timestamp()) + 1576800000
        else:
            if self.expire_widget.value:
                new_data['expiration'] = int(self.expire_widget.value.timestamp())

        if new_data['expiration'] < time.time() + 5 * 60:
            self.app.show_message(_(common_strings.error), _("SSA lifetime should be at least 5 minutes.\nSet expiration time at least 5 minutes away from now."), tobefocused = dialog)
            return

        new_data['software_roles'] = new_data['software_roles'].splitlines()

        template_data = copy.deepcopy(new_data)
        template_data['_custom_attributes_'] = {}

        for custom_attrib_name, custom_attrib_value in self.custom_attributes_container.all_data:
            new_data[custom_attrib_name] = custom_attrib_value
            template_data['_custom_attributes_'][custom_attrib_name] = custom_attrib_value

        if self.save_template_cb.checked:
            template_name = self.template_name_widget.me.text
            template_name_path = self.get_template_path(template_name)
            
            template_data['_template_name_'] = template_name
            if self.never_expire_cb.checked:
                template_data.pop('expiration')
            template_name_path.write_text(json.dumps(template_data, indent=2))
            self.load_templates()

        if self.check_required_fields(dialog.body, data=new_data):

            async def coroutine():
                operation_id = 'post-register-ssa'
                cli_args = {'operation_id': operation_id, 'cli_object': self.cli_object, 'data': new_data}
                self.app.start_progressing(_("Saving ssa..."))
                result = await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
                self.app.stop_progressing()
                ssa = result.json()
                self.display_ssa_token(ssa)
                dialog.future.set_result(True)
                if 'ssa' in ssa:
                    
                    self.get_ssa()
                else:
                    self.app.show_message(_(common_strings.error), _("Something not went good while creating SSA:" + "\n" + str(ssa)), tobefocused = self.main_container)

            asyncio.ensure_future(coroutine())

    def display_ssa_token(self, token, tobefocused=None):
        self.app.data_display_dialog(
                data=token,
                title=_("SSA Token"),
                message=_("You can save the token by using < Export > button."),
                tobefocused=tobefocused
                )

    def edit_custom_attribute(self, *args: Any,  **kwargs: Any) -> None:

        if args:
            dialog_title = _("Add Custom Attribute")
            attrib_name = args[0].dialog.body.current_value
            attrib_value = ''

        else:
            dialog_title = _("Edit Custom Attribute")
            attrib_name, attrib_value = kwargs.get('data', ('',''))


        key_widget = self.app.getTitledText(_("Attribute name"), name='attribute_name', value=attrib_name, style=cli_style.read_only, read_only=True)
        val_widget = self.app.getTitledText(_("Attribute Value"), name='attribute_value', value=attrib_value, style=cli_style.edit_text)

        def add_attribute(dialog: Dialog) -> None:
            cur_data = [attrib_name, val_widget.me.text]

            if args:
                self.custom_attributes_container.add_item(cur_data)
                self.custom_attributes_container.all_data.append(cur_data)
            else:
                self.custom_attributes_container.replace_item(kwargs['selected'], cur_data)
                self.custom_attributes_container.all_data[kwargs['selected']] = cur_data

        body_widgets = [key_widget, val_widget]
        body = HSplit(body_widgets)
        buttons = [Button(_("Cancel")), Button(_("OK"), handler=add_attribute)]
        dialog = JansGDialog(self.app, title=dialog_title, body=body, buttons=buttons, width=self.app.dialog_width-20)
        self.app.show_jans_dialog(dialog)


    def add_custom_attribute(self, **kwargs: Any) -> None:

        if not self.ssa_custom_attributes:
            msg = HTML(_("Custom attributes for SSA was not defined. Please go <b>Auth Server</b> / <b>Properties</b> and edit <b>ssaConfiguration</b> for <b>ssaCustomAttributes</b>"))
            self.app.show_message(
                _("No Custom Attributes"),
                msg,
                tobefocused=self.edit_ssa_dialog_container
                )
            return


        buttons = [
                Button(_("Cancel")),
                Button(_("OK"), handler=self.edit_custom_attribute)
                ]

        attributes_list = []
        current_attributes = [data[0] for data in self.custom_attributes_container.all_data]
        for attr in self.ssa_custom_attributes:
            if not attr in current_attributes:
                attributes_list.append((attr, attr))


        if not attributes_list:
            msg = _("All custom attributes were added")
            self.app.show_message(
                _("Nothing Left to Add"),
                msg,
                tobefocused=self.edit_ssa_dialog_container
                )
            return

        dialog_title = _("Select attribute to add")

        self.addAttributeDialog = JansGDialog(
            self.app,
            title=dialog_title,
            body=RadioList(values=attributes_list),
            buttons=buttons,
            width = max(len(dialog_title),self.attributes_max_len) + 12
            )

        self.app.show_jans_dialog(self.addAttributeDialog)


    def delete_custom_attribute(self, **kwargs: Any) -> None:

        def do_delete_custom_attribute(result):
            self.custom_attributes_container.remove_item(kwargs['selected'])
            for data in self.custom_attributes_container.all_data:
                if data == kwargs['selected']:
                    self.custom_attributes_container.all_data.remove(data)
                    break

        dialog = self.app.get_confirm_dialog(
            HTML(_("Are you sure want to delete custom attribute <b>{}</b>").format(kwargs['selected'][0])),
            confirm_handler=do_delete_custom_attribute
            )

        self.app.show_jans_dialog(dialog)

    def edit_ssa_dialog(self, data=None):

        if data:
            title = _("Edit SSA")
        else:
            data = {}
            title = _("Add new SSA")

        custom_attributes = data.pop('_custom_attributes_', {})
        template_name = data.pop('_template_name_', None)

        if not template_name:
            n = len(list(self.templates_dir.glob('ssa-*.json')))
            template_name = _("SSA Template ") + str((n+1))

        self.attributes_max_len = 0
        for attr in self.ssa_custom_attributes:
            if len(attr) > self.attributes_max_len:
                self.attributes_max_len = len(attr)

        expiration_label = _("Expiration")
        never_expire_label = _("Never")
        self.never_expire_cb = Checkbox(never_expire_label)
        never_expire_cb_handler_org = self.never_expire_cb._handle_enter


        exp_date_time = data.get('expiration', None)
        if exp_date_time:
            exp_date_time = datetime.fromtimestamp(exp_date_time)

        def hide_show_expire_widget():
            never_expire_cb_handler_org()

            if self.never_expire_cb.checked:
                self.expire_widget = Window()
            else:
                self.expire_widget = DateSelectWidget(self.app, value=exp_date_time, min_date=datetime.now)

        self.never_expire_cb._handle_enter = hide_show_expire_widget

        if exp_date_time :
            self.never_expire_cb.checked = not exp_date_time.timestamp() - time.time() > 1500000000

        hide_show_expire_widget()

        save_template_label = _("Save as Template")
        template_name_label = _("Template Name")
        self.save_template_cb = Checkbox('')
        save_template_cb_handler_org = self.save_template_cb._handle_enter
        save_template_cb_handler_org()

        def hide_show_template_name_widget():
            save_template_cb_handler_org()
            if not self.save_template_cb.checked:
                self.template_name_widget = Window()
            else:
                self.template_name_widget = self.app.getTitledText(
                            title=template_name_label,
                            name='template_name',
                            value=template_name,
                            style=cli_style.edit_text
                            )

        self.save_template_cb._handle_enter = hide_show_template_name_widget

        hide_show_template_name_widget()

        custom_attribute_len = self.attributes_max_len or 20
        custom_attributes_title = _("Custom Attributes")
        add_custom_attribute_title = _("Add Attribute")

        custom_attributes_data = []
        for attrib_name in custom_attributes:
            custom_attributes_data.append((attrib_name, custom_attributes[attrib_name]))

        self.custom_attributes_container =  JansVerticalNav(
                myparent=self.app,
                headers=[_('Name'), _('Value')],
                preferred_size=[custom_attribute_len+2, 20],
                on_enter=self.edit_custom_attribute,
                on_delete=self.delete_custom_attribute,
                on_display=self.myparent.data_display_dialog,
                selectes=0,
                data=custom_attributes_data,
                all_data=custom_attributes_data,
                headerColor=cli_style.navbar_headcolor,
                entriesColor=cli_style.navbar_entriescolor,
                underline_headings=False,
                max_width=52,
                jans_name='customAttributes',
                max_height=3
                )

        body_widgets = [

                self.app.getTitledText(
                    title=_("Software ID"),
                    name='software_id',
                    value=data.get('software_id',''),
                    style=cli_style.edit_text
                ),

                self.app.getTitledText(
                    title=_("Organization"),
                    name='org_id',
                    value=data.get('org_id',''),
                    style=cli_style.edit_text
                ),

                self.app.getTitledText(
                    title=_("Description"),
                    name='description',
                    value=data.get('description',''),
                    style=cli_style.edit_text
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
                    style=cli_style.edit_text
                ),

                VSplit([
                            HSplit([Label(text=custom_attributes_title, style=cli_style.titled_text, width=len(custom_attributes_title)+1)], height=1),
                            self.custom_attributes_container,
                            Window(width=2),
                            HSplit([Button(text=add_custom_attribute_title, width=len(add_custom_attribute_title)+ 2, handler=self.add_custom_attribute)], height=1),
                            ],
                            height=5, width=D(),
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
                    Label(expiration_label + ': ', width=len(expiration_label)+2, style=cli_style.titled_text),
                    HSplit([self.never_expire_cb], width=len(never_expire_label)+7),
                    DynamicContainer(lambda: self.expire_widget),
                    ], height=1),


                VSplit([
                    Label(save_template_label + ': ', width=len(save_template_label)+2, style=cli_style.titled_text),
                    HSplit([self.save_template_cb], width=4),
                    DynamicContainer(lambda: self.template_name_widget),
                    ], height=1),

            ]
        if data.get('status'):
            body_widgets.insert(1,
                self.app.getTitledText(
                    title=_("Status"),
                    name='__status__',
                    value=data['status'].upper(),
                    style=cli_style.read_only,
                    read_only=True
                ))

        if data.get('created_at'):
            dt_object = datetime.fromtimestamp(data['created_at'])
            body_widgets.append(
                self.app.getTitledText(
                    title=_("Creation Date"),
                    name='__creation_date__',
                    value=dt_object.strftime("%B-%d-%Y"),
                    style=cli_style.read_only,
                    read_only=True
                ))

        body = HSplit(body_widgets)

        jti = data.get('jti')

        if jti:
            show_token_button_label = _("Show BASE64 Token")
            show_token_button = Button(show_token_button_label, handler=self.show_token, width = len(show_token_button_label) + 4)
            show_token_button.keep_dialog = True
            rovoke_token_button = Button(_("Revoke"), handler=self.rovoke_token)
            rovoke_token_button.keep_dialog = True
            recreate_button = Button(_("Recreate"), handler=self.recreate_ssa)
            recreate_button.keep_dialog = True
            buttons = [show_token_button, rovoke_token_button, recreate_button]
        else:
            save_button = Button(_("Create"), handler=self.save_ssa)
            save_button.keep_dialog = True
            buttons = [save_button]

        buttons.append(Button(_("Cancel")))

        self.edit_ssa_dialog_container = JansGDialog(self.app, title=title, body=body, buttons=buttons)
        self.edit_ssa_dialog_container.jti = jti
        self.app.show_jans_dialog(self.edit_ssa_dialog_container)

    def search_ssa(self, tbuffer:Buffer) -> None:
        if self.data:
            self.update_ssa_container(search_str=tbuffer.text)
        else:
            self.get_ssa(search_str=tbuffer.text)

    async def delete_ssa_coroutine(self, jti):
        cli_args = {'operation_id': 'delete-ssa', 'cli_object': self.cli_object, 'url_suffix': 'jti:{}'.format(jti)}
        self.app.start_progressing(_("Deleting ssa {}".format(jti)))
        await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
        self.app.stop_progressing()


    def delete_ssa(self, **kwargs: Any) -> None:
        jti = kwargs.get('jti') or self.data[kwargs['selected_idx']]['ssa']['jti']

        def do_delete_ssa(result):
            async def coroutine():
                await self.delete_ssa_coroutine(jti)
                self.get_ssa()
                close_dialog = kwargs.get('close_dialog')
                if close_dialog:
                    close_dialog.future.set_result(True)

            asyncio.ensure_future(coroutine())

        dialog = self.app.get_confirm_dialog(
            message = _("Are you sure want to delete SSA jti:") + ' ' + jti,
            confirm_handler=do_delete_ssa
            )

        self.app.show_jans_dialog(dialog)

    def ssa_details(self, **params: Any) -> None:
        ssa_data = params['data']
        data = copy.deepcopy(ssa_data)['ssa']
        data['_custom_attributes_'] = {}
        for attr in self.ssa_custom_attributes:
            if attr in data:
                data['_custom_attributes_'][attr] = data.pop(attr)
        data['expiration'] = data.pop('exp')
        data['created_at'] = ssa_data['created_at']
        data['status'] = ssa_data['status']
        self.edit_ssa_dialog(data=data)


    def show_token(self, dialog):
        async def coroutine():
            cli_args = {'operation_id': 'get-jwt-ssa', 'cli_object': self.cli_object, 'endpoint_args': f'jti:{dialog.jti}'}
            self.app.start_progressing(_("Retreiving ssa token..."))
            response = await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing()
            ssa = response.json()
            self.display_ssa_token(ssa, tobefocused=dialog)

        asyncio.ensure_future(coroutine())

    def rovoke_token(self, dialog):
        self.delete_ssa(jti=dialog.jti, close_dialog=dialog)

    def recreate_ssa(self, dialog):

        def do_recreate_ssa(cdialog):
            async def coroutine():
                await self.delete_ssa_coroutine(dialog.jti)
            self.save_ssa(dialog)
            asyncio.ensure_future(coroutine())
            
 
        confirm_dialog = self.app.get_confirm_dialog(
                message=_("Are you sure revoking current token and re-create?"),
                confirm_handler=do_recreate_ssa
                )

        self.app.show_jans_dialog(confirm_dialog)
