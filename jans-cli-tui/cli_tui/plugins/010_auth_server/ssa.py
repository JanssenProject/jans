import os
import asyncio
from datetime import datetime
from typing import Any

from prompt_toolkit.application import Application
from prompt_toolkit.eventloop import get_event_loop
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.layout.containers import HSplit, VSplit
from prompt_toolkit.layout.containers import DynamicContainer, Window
from prompt_toolkit.widgets import Button, Label, Checkbox, RadioList, Dialog
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

        self.working_container = JansVerticalNav(
                myparent=app,
                headers=[_("Software ID"), _("Organization"), _("Software Roles"), _("Status"), _("Exp.")],
                preferred_size= self.app.get_column_sizes(.25, .25 , .3, .1, .1),
                on_display=self.app.data_display_dialog,
                on_delete=self.delete_ssa,
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
                new_data['expiration'] = int(self.expire_widget.value.timestamp())

        new_data['software_roles'] = new_data['software_roles'].splitlines()

        for custom_attrib_name, custom_attrib_value in self.custom_attributes_container.all_data:
            new_data[custom_attrib_name] = custom_attrib_value

        if self.check_required_fields(dialog.body, data=new_data):

            async def coroutine():
                operation_id = 'post-register-ssa'
                cli_args = {'operation_id': operation_id, 'cli_object': self.cli_object, 'data': new_data}
                self.app.start_progressing(_("Saving ssa..."))
                result = await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
                self.app.stop_progressing()
                ssa = result.json()
                dialog.future.set_result(True)
                if 'ssa' in ssa:
                    self.app.data_display_dialog(data=ssa, title=_("SSA Token"), message=_("Save and store it securely. This is the only time you see this token."))
                    self.get_ssa()
                else:
                    self.app.show_message(_(common_strings.error), _("Something not went good while creating SSA:" + "\n" + str(ssa)), tobefocused = self.main_container)

            asyncio.ensure_future(coroutine())


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

        self.ssa_custom_attributes = self.app.app_configuration.get('ssaConfiguration', {}).get('ssaCustomAttributes', [])

        self.attributes_max_len = 0
        for attr in self.ssa_custom_attributes:
            if len(attr) > self.attributes_max_len:
                self.attributes_max_len = len(attr)

        expiration_label = _("Expiration")
        never_expire_label = _("Never")
        self.never_expire_cb = Checkbox(never_expire_label)
        never_expire_cb_handler_org = self.never_expire_cb._handle_enter

        def hide_show_expire_widget():
            never_expire_cb_handler_org()
            if self.never_expire_cb.checked:
                self.expire_widget = Window()
            else:
                self.expire_widget = DateSelectWidget(self.app, value=data.get('exp', None), min_date=datetime.now)

        self.never_expire_cb._handle_enter = hide_show_expire_widget

        hide_show_expire_widget()

        custom_attribute_len = self.attributes_max_len or 20
        custom_attributes_title = _("Custom Attributes")
        add_custom_attribute_title = _("Add Attribute")
        self.custom_attributes_container =  JansVerticalNav(
                myparent=self.app,
                headers=[_('Name'), _('Value')],
                preferred_size=[custom_attribute_len+2, 20],
                on_enter=self.edit_custom_attribute,
                on_delete=self.delete_custom_attribute,
                on_display=self.myparent.data_display_dialog,
                selectes=0,
                all_data=[],
                headerColor=cli_style.navbar_headcolor,
                entriesColor=cli_style.navbar_entriescolor,
                underline_headings=False,
                max_width=52,
                jans_name='customAttributes',
                max_height=3
                )

        body = HSplit([

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

            ])

        save_button = Button(_("Save"), handler=self.save_ssa)
        save_button.keep_dialog = True
        canncel_button = Button(_("Cancel"))
        buttons = [save_button, canncel_button]
        self.edit_ssa_dialog_container = JansGDialog(self.app, title=title, body=body, buttons=buttons)
        self.app.show_jans_dialog(self.edit_ssa_dialog_container)

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


