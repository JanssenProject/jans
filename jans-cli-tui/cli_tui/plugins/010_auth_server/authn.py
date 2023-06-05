import asyncio
from typing import Any
from functools import partial

from prompt_toolkit.application import Application
from prompt_toolkit.eventloop import get_event_loop
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.layout.containers import HSplit, VSplit, HorizontalAlign, Window
from prompt_toolkit.widgets import Button, Label, Dialog, Box
from prompt_toolkit.formatted_text import HTML

from utils.multi_lang import _
from utils.utils import DialogUtils
from utils.static import cli_style, common_strings
from wui_components.jans_vetrical_nav import JansVerticalNav
from wui_components.jans_drop_down import DropDownWidget
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

        self.acr_container = JansVerticalNav(
                myparent=app,
                headers=[_("ACR"), _("SAML ACR"), _("Level"), _("Default")],
                preferred_size= self.app.get_column_sizes(.2, .5 , .15, .15),
                selectes=0,
                headerColor=cli_style.navbar_headcolor,
                entriesColor=cli_style.navbar_entriescolor,
                on_display=self.app.data_display_dialog,
                on_enter=self.edit_acr,
                on_delete=self.delete_ldap_server
            )
        add_ldap_server_title = _("Add Source LDAP Server")
        self.main_container = HSplit([
                                self.acr_container,
                                VSplit([Button(add_ldap_server_title, width=len(add_ldap_server_title)+2, handler=self.ldap_server_dialog)], align=HorizontalAlign.CENTER, width=D())
                            ],
                            width=D()
                            )

        self.main_container.on_page_enter = self.on_page_enter

    def on_cli_object_ready(self):
        if not self.app.cli_object.openid_configuration:
            self.app.retreive_openid_configuration()
        self.app.create_background_task(self.get_default_acr())


    def on_page_enter(self, focus_container=False) -> None:


        def populate_acr_list():

            self.acr_container.clear()
            self.acr_container.add_item((BUILTIN_AUTHN, BUILTIN_SAML, ' -1', 'X' if self.default_acr == BUILTIN_AUTHN else ' '))

            # LDAP Servers
            for ldap_server in self.ldap_servers:
                self.acr_container.add_item((
                    ldap_server['configId'],
                    BUILTIN_SAML,
                    str(ldap_server['level']).rjust(3),
                    'X' if self.default_acr == ldap_server['configId'] else ' '
                    ))


            # Custom scripts
            for scr in self.auth_scripts:
                self.acr_container.add_item((
                    scr['name'],
                    'urn:io:jans:acrs:'+scr['name'],
                    str(scr['level']).rjust(3),
                    'X' if self.default_acr == scr['name'] else ' '
                    ))


            self.acr_container.all_data = self.acr_container.data[:]
            if focus_container:
                self.app.layout.focus(self.acr_container)


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
            populate_acr_list()

        asyncio.ensure_future(coroutine())


    def edit_acr(self, **params: Any) -> None:
        """This Method displays editing ACR
        """

        acr = params['passed'][0]

        if acr == BUILTIN_AUTHN:
            self.simple_password_auth_dialog(params['passed'])
        elif acr in [ldap_server['configId'] for ldap_server in self.ldap_servers]:
            self.ldap_server_dialog(acr)
        elif acr in [scr['name'] for scr in self.auth_scripts]:
            self.auth_script_dialog(acr)

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

    def simple_password_auth_dialog(self, acr_item: list) -> None:

        level = Spinner(value=-1, min_value=-1, max_value=-1)
        hashl_alg = DropDownWidget(values=[('bcrypt','bcrypt')], value='bcrypt', select_one_option=False)
        default_acr = self.default_acr == 'simple_password_auth'
        body = HSplit([
                self.app.getTitledText(title="ACR", value='simple_password_auth', name='acr', read_only=True),
                self.app.getTitledWidget(title=_("Level"), widget=level, name='level'),
                self.app.getTitledCheckBox(title=_("Default Authn Method"), checked = default_acr, name='default'),
                self.app.getTitledText(title="SAML ACR", value=acr_item[1], name='urn', read_only=True),
                self.app.getTitledText(title=_("Description"), value="Built-in default password authentication", name='urn', read_only=True),
                self.app.getTitledText(title=_("Primary Key"), value="uid", name='primary_key', read_only=True),
                self.app.getTitledText(title=_("Password Attribute"), value="userPassword", name='password_attribute', read_only=True),
                self.app.getTitledWidget(title=_("Hash Algorithm"), widget=hashl_alg, name='hashl_alg'),
                ],
                width=D()
                )

        def simple_password(dialog):
            data = self.make_data_from_dialog({'acr': dialog.body})
            if data['default'] and data['default'] != default_acr:
                self.save_default_acr(BUILTIN_AUTHN)

        buttons = [Button(_("Save"), handler=simple_password), Button(_("Cancel"))]
        dialog = JansGDialog(self.app, body=body, title=acr_item[0], buttons=buttons, width=self.app.dialog_width)
        self.app.show_jans_dialog(dialog)


    def ldap_server_dialog(self, acr=None):
        for ldap_server in self.ldap_servers:
            if ldap_server['configId'] == acr:
                config = ldap_server
                break
        else:
            config = {}

        level = Spinner(value=config.get('level', len(self.ldap_servers)), min_value=0, max_value=99)
        default_acr = acr == self.default_acr

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
                    self.app.show_message(title=_("Success"), message=_("LDAP configuration test was successfull"), buttons=[ok_button])
                else:
                    self.app.show_message(title=_(common_strings.error), message=_("LDAP configuration test was failed"))

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
                    self.app.show_message(title=_(common_strings.error), message=_("An error ocurred while saving LDAP configuration: {}").format(result.text))
                else:
                    dialog.future.set_result(True)

                    if data['default'] and data['default'] != data['configId']:
                        self.save_default_acr(data['configId'])
                    else:
                        self.on_page_enter(focus_container=True)

            asyncio.ensure_future(coroutine())

        test_button = Button(_("Test"), test_ldap)
        test_button.keep_dialog = True
        save_button = Button(_("Save"), save_ldap)
        save_button.keep_dialog = True
        buttons = [save_button, test_button, Button(_("Cancel"))]
        dialog = JansGDialog(self.app, body=body, title=acr, buttons=buttons, width=self.app.dialog_width)
        self.app.show_jans_dialog(dialog)


    def delete_script_property(self, **kwargs: Any) -> None:
        """This method for deleting the script coniguration property
        """

        def confirm_handler(dialog) -> None:
            self.script_config_properties_container.remove_item(kwargs['selected'])

        confirm_dialog = self.app.get_confirm_dialog(
                    _("Are you sure want to delete property with Key:")+"\n {} ?".format(kwargs['selected'][0]),
                    confirm_handler=confirm_handler
                    )

        self.app.show_jans_dialog(confirm_dialog)



    def edit_script_property(self, **kwargs: Any) -> None:
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


    def auth_script_dialog(self, acr:str) -> None:
        for scr in self.auth_scripts:
            if scr['name'] == acr:
                auth_script = scr.copy()
                break

        level = Spinner(value=scr['level'], min_value=0, max_value=99)
        default_acr = acr == self.default_acr

        config_properties_title = _("Properties: ")
        add_property_title = _("Add Property")
        config_properties_data = []
        for prop in auth_script.get('configurationProperties', []):
            config_properties_data.append([prop['value1'], prop.get('value2', ''), prop.get('hide', False)])

        self.script_config_properties_container = JansVerticalNav(
                myparent=self.app,
                headers=['Key', 'Value', 'Hide'],
                preferred_size=[15, 15, 5],
                data=config_properties_data,
                on_enter=self.edit_script_property,
                on_delete=self.delete_script_property,
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
                self.app.getTitledText(title="ACR", value=acr, name='acr', read_only=True, style=cli_style.edit_text),
                self.app.getTitledWidget(title=_("Level"), widget=level, name='level', style=cli_style.edit_text),
                self.app.getTitledCheckBox(title=_("Default Authn Method"), checked=default_acr, name='default', style=cli_style.check_box),
                self.app.getTitledText(title="SAML ACR", value='urn:io:jans:acrs:'+acr, name='urn', read_only=True, style=cli_style.edit_text),
                self.app.getTitledText(title="Description", value=auth_script.get('description', ''), name='description', style=cli_style.edit_text),
                VSplit([
                        HSplit([Label(text=config_properties_title, style=cli_style.edit_text, width=len(config_properties_title)+1)]),
                        self.script_config_properties_container,
                        Window(width=2),
                        HSplit([
                            Window(height=1),
                            Button(text=add_property_title, width=len(add_property_title)+4, handler=self.edit_script_property),
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
            auth_script['level'] = data['level']
            auth_script['description'] = data['description']
            auth_script['configurationProperties'].clear()
            auth_script['revision'] += 1
            
            for prop_data in self.script_config_properties_container.data:
                auth_script['configurationProperties'].append({
                    "value1": prop_data[0],
                    "value2": prop_data[1],
                    "hide": prop_data[2]
                    })

            async def coroutine():
                cli_args = {'operation_id': 'put-config-scripts', 'data': auth_script}
                self.app.start_progressing("Saving Script ...")
                response = await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
                self.app.stop_progressing()
                if response.status_code == 500:
                    self.app.show_message(_('Error'), response.text + '\n' + response.reason)
                else:
                    dialog.future.set_result(True)
                    if data['default'] and data['default'] != self.default_acr:
                        self.save_default_acr(acr)

            asyncio.ensure_future(coroutine())

        save_button = Button(_("Save"), handler=save_auth_script)
        save_button.keep_dialog = True

        buttons = [save_button, Button(_("Cancel"))]
        dialog = JansGDialog(self.app, body=body, title=acr, buttons=buttons, width=self.app.dialog_width)
        self.app.show_jans_dialog(dialog)

    def delete_ldap_server(self, **params: Any) -> None:
        async def coroutine(config_id):
            cli_args = {'operation_id': 'delete-config-database-ldap-by-name', 'url_suffix':'name:{}'.format(config_id) }
            self.app.start_progressing(_("Deleting Source LDAP..."))
            await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing()
            self.on_page_enter(focus_container=True)

        def do_delete_ldap_server(config_id, dialog):
            asyncio.ensure_future(coroutine(config_id))


        for ldap_server in self.ldap_servers:
            if ldap_server['configId'] == params['selected'][0]:
                confirm_handler = partial(do_delete_ldap_server, ldap_server['configId'])
                confirm_dialog = self.app.get_confirm_dialog(
                            message=HTML(_("Are you sure deleting source LDAP Server <b>{}</b>?").format(ldap_server['configId'])),
                            confirm_handler=confirm_handler
                            )
                self.app.show_jans_dialog(confirm_dialog)
                break


    def save_default_acr(self, acr):

        async def coroutine():
            # save default acr
            cli_args = {'operation_id': 'put-acrs', 'data': {'defaultAcr': acr}}
            self.app.start_progressing(_("Saving default ACR..."))
            await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing()
            await self.get_default_acr()
            self.on_page_enter(focus_container=True)

        asyncio.ensure_future(coroutine())
