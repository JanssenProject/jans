import copy
import asyncio
from typing import Any
from functools import partial
from collections import OrderedDict

from prompt_toolkit.application import Application
from prompt_toolkit.eventloop import get_event_loop
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.layout.containers import HSplit, VSplit, HorizontalAlign, Window, DynamicContainer
from prompt_toolkit.widgets import Button, Label, Dialog, Frame, VerticalLine, RadioList
from prompt_toolkit.formatted_text import HTML

from utils.multi_lang import _
from utils.utils import DialogUtils, common_data
from utils.static import cli_style, common_strings
from wui_components.jans_vetrical_nav import JansVerticalNav
from wui_components.jans_side_nav_bar import JansSideNavBar
from wui_components.jans_drop_down import DropDownWidget
from wui_components.jans_table import JansTableWidget
from wui_components.jans_cli_dialog import JansGDialog
from wui_components.jans_spinner import Spinner

BUILTIN_AUTHN = 'simple_password_auth'
BUILTIN_SAML = 'urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport'

class Authn(DialogUtils):
    def __init__(
        self, 
        app: Application
        ) -> None:

        self.app = app
        self.default_acr = None
        self.auth_scripts = []
        self.tabs = OrderedDict()
        self.agama_acr_values = []
        self.acr_radio_list_widget = RadioList(values=[(BUILTIN_AUTHN, BUILTIN_AUTHN)])

        self.tabs["Default ACR"] = HSplit([
                        self.acr_radio_list_widget,
                         Window(height=1),
                        self.app.getButtonWithHandler(text="Save", handler=self.save_default_acr, centered=True)
                        ],
                        width=D()
                        )

        level = Spinner(value=-1, min_value=-1, max_value=-1)
        hashl_alg = DropDownWidget(values=[('bcrypt','bcrypt')], value='bcrypt', select_one_option=False)
        default_acr = self.default_acr == 'simple_password_auth'
        self.tabs["Basic"] = HSplit([
                self.app.getTitledText(title="ACR", value='simple_password_auth', name='acr', read_only=True, style=cli_style.titled_text, widget_style=cli_style.black_bg_widget),
                self.app.getTitledWidget(title=_("Level"), widget=level, name='level', style=cli_style.titled_text),
                self.app.getTitledCheckBox(title=_("Default Authn Method"), checked = default_acr, name='default', style=cli_style.check_box, widget_style=cli_style.black_bg_widget),
                self.app.getTitledText(title="SAML ACR", value=BUILTIN_SAML, name='urn', read_only=True, style=cli_style.titled_text, widget_style=cli_style.black_bg_widget),
                self.app.getTitledText(title=_("Description"), value="Built-in default password authentication", name='urn', read_only=True, style=cli_style.titled_text, widget_style=cli_style.black_bg_widget),
                self.app.getTitledText(title=_("Primary Key"), value="uid", name='primary_key', read_only=True, style=cli_style.titled_text, widget_style=cli_style.black_bg_widget),
                self.app.getTitledText(title=_("Password Attribute"), value="userPassword", name='password_attribute', read_only=True, style=cli_style.titled_text, widget_style=cli_style.black_bg_widget),
                self.app.getTitledWidget(title=_("Hash Algorithm"), widget=hashl_alg, name='hashl_alg', style=cli_style.titled_text),
                self.app.getButtonWithHandler(text="Save", handler=self.save_basic_acr, centered=True),
                ],
                width=D(),
                )

        self.ldap_servers_container = JansVerticalNav(
                myparent=app,
                headers=[_("ACR"), _("SAML ACR"), _("Level")],
                preferred_size= self.app.get_column_sizes(.30, .55, .15),
                selectes=0,
                headerColor=cli_style.navbar_headcolor,
                entriesColor=cli_style.navbar_entriescolor,
                on_display=self.app.data_display_dialog,
                on_enter=self.edit_ldap_server_dialog,
                on_delete=self.delete_ldap_server
            )

        self.tabs["LDAP Servers"] = HSplit([
                self.ldap_servers_container,
                self.app.getButtonWithHandler(text="Add Source LDAP Server", name='add_new_ldap_server', handler=self.edit_ldap_server_dialog, centered=True),
                ],
                width=D(),
                )

        self.scripts_container = JansVerticalNav(
                myparent=app,
                headers=[_("ACR"), _("SAML ACR"), _("Level")],
                preferred_size= self.app.get_column_sizes(.30, .55 , .15),
                selectes=0,
                headerColor=cli_style.navbar_headcolor,
                entriesColor=cli_style.navbar_entriescolor,
                on_display=self.app.data_display_dialog,
                on_enter=self.edit_script_dialog,
            )


        self.tabs["Scripts"] = HSplit([
                self.scripts_container,
                ],
                width=D(),
                )

        self.aliases_container = JansVerticalNav(
                myparent=app,
                headers=[_("Mapping"), _("Source")],
                preferred_size= self.app.get_column_sizes(0.5, 0.5),
                selectes=0,
                headerColor=cli_style.navbar_headcolor,
                entriesColor=cli_style.navbar_entriescolor,
                on_display=self.app.data_display_dialog,
                on_enter=self.edit_alias_dialog,
                on_delete=self.delete_alias,
            )
        
        self.tabs["Aliases"] = HSplit([
                self.aliases_container,
                self.app.getButtonWithHandler(text="Add Alias", name='add_new_alias', handler=self.edit_alias_dialog, centered=True),
                ],
                width=D(),
                )

        self.agama_acr_container = HSplit([])
        self.tabs["Agama Flows"] = DynamicContainer(lambda: self.agama_acr_container)


        self.side_nav_bar = JansSideNavBar(myparent=self.app,
                                           entries=list(self.tabs.keys()),
                                           selection_changed=(self.side_nav_selection_changed),
                                           select=0,
                                           entries_color='class:outh-client-navbar')
        self.left_nav = list(self.tabs.keys())[0]

        self.main_container = Frame(body=VSplit([
                                self.side_nav_bar,
                                Window(width=1),
                                VerticalLine(),
                                Window(width=1),
                                DynamicContainer(lambda: self.tabs[self.left_nav])
                            ],
                            width=D(),
                            height=D(),
                            ))

        self.main_container.on_page_enter = self.on_page_enter


    def side_nav_selection_changed(self, selection: str) -> None:
        self.left_nav = selection

    def on_cli_object_ready(self):
        if not self.app.cli_object.openid_configuration:
            self.app.retreive_openid_configuration()
        self.app.create_background_task(self.get_default_acr())


    def on_page_enter(self) -> None:

        def populate_acr_list():

            acr_values = [(BUILTIN_AUTHN, BUILTIN_AUTHN + ' [builtin]')]

            # LDAP Servers
            if 'ldap' in common_data.server_persistence_type.get('driverVersion', '').lower():
                self.ldap_servers_container.clear()
                for i, ldap_server in enumerate(self.ldap_servers):
                    acr_values.append((ldap_server['configId'], ldap_server['configId'] + ' [ldap]'))
                    if self.default_acr == ldap_server['configId']:
                        self.acr_radio_list_widget.current_value = ldap_server['configId']
                        self.ldap_servers_container.italic_line = i

                    self.ldap_servers_container.add_item((
                            ldap_server['configId'],
                            BUILTIN_SAML,
                            str(ldap_server['level']).rjust(3),
                        ))

                self.ldap_servers_container.all_data = self.ldap_servers[:]


            # Custom scripts
            self.scripts_container.clear()
            for i, scr in enumerate(self.auth_scripts):
                if scr['name'] == 'agama':
                    continue
                acr_values.append((scr['name'], scr['name'] + ' [script]'))
                if self.default_acr == scr['name']:
                    self.acr_radio_list_widget.current_value = scr['name']
                    self.scripts_container.italic_line = i

                self.scripts_container.add_item((
                        scr['name'],
                        'urn:io:jans:acrs:'+scr['name'],
                        str(scr['level']).rjust(3)
                    ))

            self.scripts_container.all_data = self.auth_scripts[:]


            # agama flows
            for agama_acr in self.agama_acr_values:
                if self.default_acr == agama_acr:
                    self.acr_radio_list_widget.current_value = agama_acr
                acr_values.append((agama_acr, agama_acr + ' [agama]'))


            self.acr_radio_list_widget.values = acr_values

            # aliases
            aliases = sorted(self.app.app_configuration.get('acrMappings', {}).items())

            self.aliases_container.clear()
            for key_, val_ in aliases:
                self.aliases_container.add_item((key_, val_))

            self.aliases_container.all_data = aliases

        async def coroutine():
            # retreive auth ldap servers
            cli_args = {'operation_id': 'get-config-database-ldap'}
            self.app.start_progressing(_("Retreiving LDAP Configurations..."))
            result = await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing()
            self.ldap_servers = result.json()

            acr_values_supported = self.app.cli_object.openid_configuration.get('acr_values_supported', [])[:]
            if BUILTIN_AUTHN in acr_values_supported:
                acr_values_supported.remove('simple_password_auth')

            self.auth_scripts.clear()

            self.app.start_progressing(_("Retreiving Auth Scripts"))
            cli_args = {'operation_id': 'get-config-scripts', 'endpoint_args': 'fieldValuePair:scriptType=person_authentication,fieldValuePair:enabled=true'}
            response = await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing()
            if response.status_code == 200:
                result = response.json()
                if result.get('entriesCount', 0) > 0:
                    self.auth_scripts = result['entries']

            # agama flows ACR
            await self.app.agama_module.get_projects_coroutine(update_container=False)
            agama_flows_acrs = []
            self.agama_acr_values.clear()

            for agama_flow in self.app.agama_module.data.get('entries',[]):
                project_metadata = agama_flow.get('details', {}).get('projectMetadata', {})
                flows_error = agama_flow.get('details', {}).get('flowsError', {})
                if not flows_error:
                    continue
                flows_error_keys = list(flows_error.keys())
                no_direct_launch = project_metadata.get('noDirectLaunch', [])

                for e in no_direct_launch:
                    if e in flows_error_keys:
                        flows_error_keys.remove(e)

                flow_acr_values = []
                for e in flows_error_keys:
                    acr = f'agama_{e}'
                    self.agama_acr_values.append(acr)
                    if self.default_acr == acr:
                        acr = '*' + acr
                    flow_acr_values.append(acr)

                agama_flows_acrs.append((project_metadata.get('projectName', 'NA'), '\n'.join(flow_acr_values)))

            if agama_flows_acrs:
                self.agama_acr_container = JansTableWidget(
                                app=self.app,
                                data=agama_flows_acrs,
                                headers=['Project Name', 'ACR Values'],
                                bg_style='class:table-black-bg'
                                )
            else:
                self.agama_acr_container = HSplit([Label(_("No agama project deployed on this server"))])

            populate_acr_list()

        asyncio.ensure_future(coroutine())


    async def get_default_acr(self) -> None:
        response = self.app.cli_requests({'operation_id': 'get-acrs'})
        if response.ok:
            result = response.json()
            self.default_acr = result.get('defaultAcr', BUILTIN_AUTHN)

    def get_ldap_config(self, dialog):
        data = self.make_data_from_dialog({'acr': dialog.body})
        data['version'] = 0
        data['useAnonymousBind'] = False
        data.pop('default', False)
        return data


    def edit_ldap_server_dialog(self, *positional: str, **kwargs: Any) -> None:

        config = {} if positional and positional[0] else kwargs['data']

        level = Spinner(value=config.get('level', len(self.ldap_servers)), min_value=0, max_value=99)
        default_acr = config.get('configId', 0) == self.default_acr

        body = HSplit([
                self.app.getTitledText(title="ACR", value=config.get('configId', ''), name='configId', read_only=bool(config.get('configId', None)), style=cli_style.edit_text_required),
                self.app.getTitledWidget(title=_("Level"), widget=level, name='level'),
                self.app.getTitledCheckBox(title=_("Default Authn Method"), checked=default_acr, name='default', style=cli_style.check_box),
                self.app.getTitledText(title="Bind DN", value=config.get('bindDN',''), name='bindDN', style=cli_style.edit_text_required),
                self.app.getTitledText(title=_("Max Connections"), text_type='integer', value=config.get('maxConnections', 1000), name='maxConnections', style=cli_style.edit_text_required),
                self.app.getTitledText(title=_("Remote Primary Key"), value=config.get('primaryKey', 'uid'), name='primaryKey', style=cli_style.edit_text_required),
                self.app.getTitledText(title=_("Local Primary Key"), value=config.get('localPrimaryKey', 'uid'), name='localPrimaryKey', style=cli_style.edit_text_required),
                self.app.getTitledText(title=_("Remote LDAP server:port"), value='\n'.join(config.get('servers','')), height=2, name='servers', jans_list_type=True, style=cli_style.edit_text_required),
                self.app.getTitledText(title=_("Base DNs"), value='\n'.join(config.get('baseDNs','')), height=2, name='baseDNs', jans_list_type=True, style=cli_style.edit_text_required),
                self.app.getTitledText(title=_("Bind Password"), value=config.get('bindPassword',''), name='bindPassword', style=cli_style.edit_text_required),
                self.app.getTitledCheckBox(title=_("Use SSL"), checked=config.get('useSSL', True), name='useSSL'),
                self.app.getTitledCheckBox(title=_("Enabled"), checked=config.get('enabled', False) , name='enabled'),
                ],
                width=D()
                )


        def test_ldap(dialog):
            ldap_config = self.get_ldap_config(dialog)

            async def coroutine():
                cli_args = {'operation_id': 'post-config-database-ldap-test', 'data': ldap_config}
                self.app.start_progressing(_("Testing LDAP Configuration..."))
                result = await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
                self.app.stop_progressing()

                if result.status_code == 200 and result.text == 'true':
                    # if diolog needs to be closed, use handler: lambda: dialog.future.set_result(True)
                    ok_button = Button(_("OK"))
                    self.app.show_message(title=_(common_strings.success), message=_("LDAP configuration test was successfull"), buttons=[ok_button], tobefocused=self.ldap_servers_container)
                else:
                    self.app.show_message(title=_(common_strings.error), message=_("LDAP configuration test was failed"), tobefocused=self.ldap_servers_container)

            asyncio.ensure_future(coroutine())


        def save_ldap(dialog):

            ldap_config = self.get_ldap_config(dialog)
            data = self.make_data_from_dialog({'acr': dialog.body})
            operation_id = 'put-config-database-ldap' if config.get('configId') else 'post-config-database-ldap'

            async def coroutine():
                cli_args = {'operation_id': operation_id, 'data': ldap_config}
                self.app.start_progressing(_("Saving LDAP configuration..."))
                result = await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
                self.app.stop_progressing()

                if result.status_code not in (200, 201):
                    self.app.show_message(title=_(common_strings.error), message=_("An error ocurred while saving LDAP configuration: {}").format(result.text), tobefocused=self.ldap_servers_container)
                else:
                    dialog.future.set_result(True)

                    if data['default'] and data['default'] != data['configId']:
                        self.save_default_acr(data['configId'], acr_type='ldap')
                    else:
                        self.on_page_enter()
                    self.app.show_message(title=_(common_strings.success), message=_("LDAP Server was successfully saved"), tobefocused=self.ldap_servers_container)

            asyncio.ensure_future(coroutine())

        test_button = Button(_("Test"), test_ldap)
        test_button.keep_dialog = True
        save_button = Button(_("Save"), save_ldap)
        save_button.keep_dialog = True
        buttons = [save_button, test_button, Button(_("Cancel"))]
        dialog_title = _("Editing LDAP Server") if config else _("New LDAP Server") 
        dialog = JansGDialog(self.app, body=body, title=dialog_title, buttons=buttons, width=self.app.dialog_width)
        self.app.show_jans_dialog(dialog)



    def edit_script_dialog(self, **kwargs) -> None:

        config = kwargs['data']
        scr_name = config['name']
        level = Spinner(value=config['level'], min_value=0, max_value=99)
        default_acr = scr_name == self.default_acr

        config_properties_title = _("Properties: ")
        add_property_title = _("Add Property")
        config_properties_data = []
        for prop in config.get('configurationProperties', []):
            config_properties_data.append([prop['value1'], prop.get('value2', ''), prop.get('hide', False)])


        def delete_script_property(**kwargs: Any) -> None:
            """This method for deleting the script coniguration property
            """

            def confirm_handler(dialog) -> None:
                self.script_config_properties_container.remove_item(kwargs['selected'])

            confirm_dialog = self.app.get_confirm_dialog(
                        _("Are you sure want to delete property with Key:")+"\n {} ?".format(kwargs['selected'][0]),
                        confirm_handler=confirm_handler
                        )

            self.app.show_jans_dialog(confirm_dialog)


        def edit_script_property(**kwargs: Any) -> None:
            key, val, hide = kwargs.get('data', ('','', False))
            hide_widget = self.app.getTitledCheckBox(_("Hide"), name='property_hide', checked=hide, style=cli_style.check_box, jans_help=_("Hide script property?"))

            key_widget = self.app.getTitledText(_("Key"), name='property_key', value=key, style=cli_style.edit_text, jans_help=_("Script propery Key"))
            val_widget = self.app.getTitledText(_("Value"), name='property_val', value=val, style=cli_style.edit_text, jans_help=_("Script property Value"))

            def add_property(dialog: Dialog) -> None:
                key_ = key_widget.me.text
                val_ = val_widget.me.text
                hide_ = hide_widget.me.checked
                cur_data = [key_, val_, hide_]

                if not kwargs.get('data'):
                    self.script_config_properties_container.add_item(cur_data)
                else:
                    self.script_config_properties_container.replace_item(kwargs['selected'], cur_data)

            body = HSplit([key_widget, val_widget, hide_widget])
            buttons = [Button(_("Cancel")), Button(_("OK"), handler=add_property)]
            dialog = JansGDialog(self.app, title=_("Configuration Property"), body=body, buttons=buttons, width=self.app.dialog_width-20)
            self.app.show_jans_dialog(dialog)


        self.script_config_properties_container = JansVerticalNav(
                myparent=self.app,
                headers=['Key', 'Value', 'Hide'],
                preferred_size=[15, 15, 5],
                data=config_properties_data,
                on_enter=edit_script_property,
                on_delete=delete_script_property,
                on_display=self.app.data_display_dialog,
                selectes=0,
                headerColor='class:outh-client-navbar-headcolor',
                entriesColor='class:outh-client-navbar-entriescolor',
                all_data=config_properties_data,
                underline_headings=False,
                max_width=52,
                jans_name='configurationProperties',
                max_height=4
                )

        body = HSplit([
                self.app.getTitledText(title="ACR", value=scr_name, name='acr', read_only=True, style=cli_style.edit_text),
                self.app.getTitledWidget(title=_("Level"), widget=level, name='level', style=cli_style.edit_text),
                self.app.getTitledCheckBox(title=_("Default Authn Method"), checked=default_acr, name='default', style=cli_style.check_box),
                self.app.getTitledText(title="SAML ACR", value='urn:io:jans:acrs:'+scr_name, name='urn', read_only=True, style=cli_style.edit_text),
                self.app.getTitledText(title="Description", value=config.get('description', ''), name='description', style=cli_style.edit_text),
                VSplit([
                        HSplit([Label(text=config_properties_title, style=cli_style.edit_text, width=len(config_properties_title)+1)]),
                        self.script_config_properties_container,
                        Window(width=2),
                        HSplit([
                            Window(height=1),
                            Button(text=add_property_title, width=len(add_property_title)+4, handler=edit_script_property),
                            ]),
                        ],
                    height=6,
                    width=D(),
                    align=HorizontalAlign.LEFT
                ),
                ],
                width=D()
                )


        def save_auth_script(dialog: Dialog) -> None:
            data = self.make_data_from_dialog({'script': dialog.body})
            config['level'] = data['level']
            config['description'] = data['description']
            config['configurationProperties'] = []
            config['revision'] += 1
            
            for prop_data in self.script_config_properties_container.data:
                config['configurationProperties'].append({
                    "value1": prop_data[0],
                    "value2": prop_data[1],
                    "hide": prop_data[2]
                    })

            async def coroutine():
                cli_args = {'operation_id': 'put-config-scripts', 'data': config}
                self.app.start_progressing("Saving Script ...")
                response = await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
                self.app.stop_progressing()
                if response.status_code == 500:
                    self.app.show_message(_('Error'), response.text + '\n' + response.reason)
                else:
                    dialog.future.set_result(True)
                    if data['default'] and data['default'] != self.default_acr:
                        self.save_default_acr(scr_name, acr_type='script')


            asyncio.ensure_future(coroutine())

        save_button = Button(_("Save"), handler=save_auth_script)
        save_button.keep_dialog = True

        buttons = [save_button, Button(_("Cancel"))]
        dialog = JansGDialog(self.app, body=body, title=scr_name, buttons=buttons, width=self.app.dialog_width)
        self.app.show_jans_dialog(dialog)

    def delete_ldap_server(self, **params: Any) -> None:
        async def coroutine(config_id):
            cli_args = {'operation_id': 'delete-config-database-ldap-by-name', 'url_suffix':'name:{}'.format(config_id) }
            self.app.start_progressing(_("Deleting Source LDAP..."))
            await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing()
            self.on_page_enter()

        def do_delete_ldap_server(config_id, dialog):
            asyncio.ensure_future(coroutine(config_id))


        for ldap_server in self.ldap_servers:
            if ldap_server['configId'] == params['selected'][0]:
                if ldap_server['configId'] == self.default_acr:
                    self.app.show_message(title=_(common_strings.warning), message=_("This source LDAP server is default ACR, it can't be deleted"), tobefocused=self.ldap_servers_container)
                    return
                confirm_handler = partial(do_delete_ldap_server, ldap_server['configId'])
                confirm_dialog = self.app.get_confirm_dialog(
                            message=HTML(_("Are you sure deleting source LDAP Server <b>{}</b>?").format(ldap_server['configId'])),
                            confirm_handler=confirm_handler
                            )
                self.app.show_jans_dialog(confirm_dialog)
                break

    def save_basic_acr(self, button_name):
        self.save_default_acr(BUILTIN_AUTHN)


    def save_default_acr(self, acr=None, acr_type=None):
        if not acr:
            acr = self.acr_radio_list_widget.current_value
            for val in self.acr_radio_list_widget.values:
                if val[0] == acr:
                    for t_ in ('ldap', 'script', 'agama'):
                        if val[1].endswith(f'{t_}'):
                            acr_type = t_
                            break

        async def coroutine():
            # save default acr
            cli_args = {'operation_id': 'put-acrs', 'data': {'defaultAcr': acr}}
            self.app.start_progressing(_("Saving default ACR..."))
            response = await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing()
            if response.status_code == 200:
                self.default_acr = response.json()['defaultAcr']
                self.app.show_message(_(common_strings.info), _(HTML("Default ACR was set to <b>{}</b>")).format(response.json()['defaultAcr']), tobefocused=self.main_container)
            else:
                self.app.show_message(_(common_strings.error), _("Save failed: Status {} - {}\n").format(response.status_code, response.text), tobefocused=self.main_container)

            self.acr_radio_list_widget.current_value = self.default_acr

            if acr_type == 'script':
                for i, scr in enumerate(self.auth_scripts):
                    if self.default_acr == scr['name']:
                        self.scripts_container.italic_line = i
                        break
            elif acr_type == 'ldap':
                for i, ldap_server in enumerate(self.ldap_servers):
                    if self.default_acr == ldap_server['configId']:
                        self.ldap_servers_container.italic_line = i
                        break

        asyncio.ensure_future(coroutine())

    async def save_acr_mappings_coroutine(self, mappings, dialog=None):

        op = 'replace' if 'acrMappings' in self.app.app_configuration else 'add'
        patch_data = [{'op':op, 'path': 'acrMappings', 'value': mappings}]
        cli_args = {'operation_id': 'patch-properties', 'data': patch_data}
        self.app.start_progressing("Saving Mappings ...")
        response = await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
        self.app.stop_progressing()
        if response.status_code == 200:
            self.app.show_message(_(common_strings.success), _("ACR mappings were saved successfully"), tobefocused=self.aliases_container)
            if dialog:
                dialog.future.set_result(True)
        else:
            self.app.show_message(_(common_strings.error), _("An error ocurred while saving ACR mappings:\n {}".format(response.text)), tobefocused=self.aliases_container)



    def edit_alias_dialog(self, *positional: str, **kwargs: Any) -> None:

        config = ['',''] if positional else kwargs['data']

        def save_alias(dialog: Dialog) -> None:
            data = self.make_data_from_dialog({'alias': dialog.body})
            mapping = (data['mapping'], data['source'])

            if positional:
                self.aliases_container.add_item(mapping)
                self.aliases_container.all_data.append(mapping)
            else:
                self.aliases_container.replace_item(kwargs['selected'], mapping)

            mappings = {source: mapping for source, mapping in self.aliases_container.data}
            asyncio.ensure_future(self.save_acr_mappings_coroutine(mappings, dialog))


        body = HSplit([
                self.app.getTitledText(title=_("Mapping"), value=config[0], name='mapping', style=cli_style.edit_text),
                self.app.getTitledText(title=_("Source"), value=config[1], name='source', style=cli_style.edit_text),
                ],
                width=D()
                )

        title = _("New Mapping") if positional else _("Edit Mapping")
        save_button = Button(_("Save"), handler=save_alias)
        save_button.keep_dialog = True
        buttons = [save_button, Button(_("Cancel"))]

        dialog = JansGDialog(self.app, body=body, title=title, buttons=buttons, width=self.app.dialog_width)
        self.app.show_jans_dialog(dialog)

    def delete_alias(self, **kwargs):

        def confirm_handler(dialog) -> None:
            self.aliases_container.remove_item(kwargs['selected'])
            mappings = {source: mapping for source, mapping in self.aliases_container.data}
            asyncio.ensure_future(self.save_acr_mappings_coroutine(mappings))

        confirm_dialog = self.app.get_confirm_dialog(
                        HTML(_("Are you sure want to delete ACR Mapping <b>{}</b>?").format(kwargs['selected'][0])),
                        confirm_handler=confirm_handler
                        )

        self.app.show_jans_dialog(confirm_dialog)

