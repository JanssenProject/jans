import copy
import asyncio

from collections import OrderedDict
from typing import Any, Optional
from prompt_toolkit.layout.containers import HSplit, DynamicContainer,\
    VSplit, Window, HorizontalAlign

from prompt_toolkit.layout import ScrollablePane
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.widgets import Button, Label, Box, Dialog
from prompt_toolkit.application import Application
from wui_components.jans_nav_bar import JansNavBar
from wui_components.jans_drop_down import DropDownWidget
from wui_components.jans_vetrical_nav import JansVerticalNav
from wui_components.jans_cli_dialog import JansGDialog
from wui_components.jans_label_widget import JansLabelWidget
from wui_components.widget_collections import get_ldap_config_widgets, get_data_for_ldap_widgets, get_logging_level_widget

from utils.multi_lang import _
from utils.utils import DialogUtils, common_data
from utils.static import cli_style, common_strings


SOURCE_ATTRIBUTE_S = _("Source Attribute")

class Plugin(DialogUtils):
    """This is a general class for plugins 
    """
    def __init__(
        self, 
        app: Application
        ) -> None:
        """init for Plugin class "fido"

        Args:
            app (Generic): The main Application class
        """
        self.app = app
        self.pid = 'kc-link'
        self.name = 'Ja[n]s KC Link'
        self.server_side_plugin = True
        self.page_entered = False
        self.data = {}

        self.prepare_navbar()
        self.prepare_containers()


    def init_plugin(self) -> None:
        """The initialization for this plugin
        """

        self.app.create_background_task(self.get_configuration())


    def delete_mapping(self, **kwargs: Any) -> None:
        """This method for editing mappings
        """

        def do_delete_mapping(result):
            self.mappings_container.remove_item(kwargs['selected'])

        dialog = self.app.get_confirm_dialog(_("Are you sure want to delete mapping {} --> {}?").format(*kwargs['selected']), confirm_handler=do_delete_mapping)
        self.app.show_jans_dialog(dialog)

    def edit_mapping(self, **kwargs: Any) -> None:
        """This method for editing mappings
        """

        if kwargs:
            title = _("Edit Attribute Mapping")
            source_attribute, destination_attribute = kwargs['data']
        else:
            title = _("Add Attribute Mapping")
            source_attribute, destination_attribute = ('','')

        source_widget = self.app.getTitledText(SOURCE_ATTRIBUTE_S, name='source_attribute', value=source_attribute, style=cli_style.titled_text, jans_help=_("Source LDAP Attribute"))
        destination_widget = self.app.getTitledText(_("Destination Attrbiute"), name='destination_attribute', value=destination_attribute, style=cli_style.titled_text, jans_help=_("Destination LDAP Attribute"))

        def add_property(dialog: Dialog) -> None:
            source_attribute_ = source_widget.me.text
            destination_attribute_ = destination_widget.me.text
            cur_data = [source_attribute_, destination_attribute_]

            if not kwargs:
                self.mappings_container.add_item(cur_data)
            else:
                self.mappings_container.replace_item(kwargs['selected'], cur_data)

        body_widgets = [source_widget, destination_widget]

        body = HSplit(body_widgets)
        buttons = [Button(_("Cancel")), Button(_("OK"), handler=add_property)]
        dialog = JansGDialog(self.app, title=title, body=body, buttons=buttons, width=self.app.dialog_width-20)
        self.app.show_jans_dialog(dialog)


    def add_key_attribute(self) -> None:

        key_attribute_widget = self.app.getTitledText(_("Key Attribute"), name='key_attribute', value='', style=cli_style.titled_text, jans_help=_("Key Attribute"))

        def do_add_key_attribute(dialog: Dialog) -> None:
            self.key_attributes_container.add_label(key_attribute_widget.me.text, key_attribute_widget.me.text)

        body = HSplit([key_attribute_widget])
        buttons = [Button(_("Cancel")), Button(_("OK"), handler=do_add_key_attribute)]
        dialog = JansGDialog(self.app, title="Add Key Attribute", body=body, buttons=buttons, width=self.app.dialog_width-20)
        self.app.show_jans_dialog(dialog)

    def delete_key_attribute(self, keya: list) -> None:
        def do_delete_key_attribute(dialog):
            self.key_attributes_container.remove_label(keya[0])

        dialog = self.app.get_confirm_dialog(
            message=_(
                "Are you sure want to delete Key Attrinute:\n {} ?".format(keya[1])),
            confirm_handler=do_delete_key_attribute
        )

        self.app.show_jans_dialog(dialog)


    def add_object_class(self) -> None:

        object_class_widget = self.app.getTitledText(_("Object Class"), name='object_class', value='', style=cli_style.titled_text, jans_help=_("Object Class"))

        def do_add_object_class(dialog: Dialog) -> None:
            self.object_classes_container.add_label(object_class_widget.me.text, object_class_widget.me.text)

        body = HSplit([object_class_widget])
        buttons = [Button(_("Cancel")), Button(_("OK"), handler=do_add_object_class)]
        dialog = JansGDialog(self.app, title="Add Object Class", body=body, buttons=buttons, width=self.app.dialog_width-20)
        self.app.show_jans_dialog(dialog)

    def delete_object_class(self, objectclass: list) -> None:
        def do_delete_object_class(dialog):
            self.object_classes_container.remove_label(objectclass[0])

        dialog = self.app.get_confirm_dialog(
            message=_(
                "Are you sure want to delete Key Object Class:\n {} ?".format(objectclass[1])),
            confirm_handler=do_delete_object_class
        )

        self.app.show_jans_dialog(dialog)


    def add_source_attribute(self) -> None:

        source_attribute_widget = self.app.getTitledText(SOURCE_ATTRIBUTE_S, name='source_attribute', value='', style=cli_style.titled_text, jans_help=SOURCE_ATTRIBUTE_S)

        def do_add_source_attribute(dialog: Dialog) -> None:
            self.source_attribute_container.add_label(source_attribute_widget.me.text, source_attribute_widget.me.text)

        body = HSplit([source_attribute_widget])
        buttons = [Button(_("Cancel")), Button(_("OK"), handler=do_add_source_attribute)]
        dialog = JansGDialog(self.app, title="Add Source Attribute", body=body, buttons=buttons, width=self.app.dialog_width-20)
        self.app.show_jans_dialog(dialog)

    def delete_source_attribute(self, sourcea: list) -> None:
        def do_delete_source_attribute(dialog):
            self.source_attribute_container.remove_label(sourcea[0])

        dialog = self.app.get_confirm_dialog(
            message=_(
                "Are you sure want to delete Source Attribute:\n {} ?".format(sourcea[1])),
            confirm_handler=do_delete_source_attribute
        )

        self.app.show_jans_dialog(dialog)

    def edit_source_config(self, **kwargs: Any) -> None:
        """Method to display the edit source config dialog
        """
        self.edit_source_config_dialog(kwargs.get('data', {}), selected=kwargs['selected'])


    def delete_source_config(self, **kwargs: Any) -> None:
        """This method for the deletion of a Source Backend LDAP Server
        """

        def do_delete_source_attribute(result):
            self.save_config(delete_source_config=kwargs['selected_idx'])

        confirm_dialog = self.app.get_confirm_dialog(
            _("Are you sure you want to delete Source Backend LDAP Server {}?").format(kwargs['selected'][1]), 
            confirm_handler=do_delete_source_attribute
            )
        self.app.show_jans_dialog(confirm_dialog)


    def add_source_config(self):
        self.edit_source_config_dialog()

    def create_widgets(self):
        self.schema = self.app.cli_object.get_schema_from_reference('Keycloak Link', '#/components/schemas/AppConfiguration')

        mappings_title = _("Attribute Mappings:")
        add_mapping_title = _("Add Mapping")

        save_config_buttonc = VSplit([Button(_("Save"), handler=self.save_config)], align=HorizontalAlign.CENTER)
        mappings_data = [(mapping['source'], mapping['destination']) for mapping in self.data.get('attributeMapping', [])]

        self.mappings_container = JansVerticalNav(
                myparent=self.app,
                headers=['Source', 'Destination'],
                preferred_size=[30, 30],
                data=mappings_data,
                on_enter=self.edit_mapping,
                on_delete=self.delete_mapping,
                on_display=self.app.data_display_dialog,
                get_help=self.app.get_help_from_schema(self.schema, 'attributeMapping'),
                selectes=0,
                headerColor=cli_style.navbar_headcolor,
                entriesColor=cli_style.navbar_entriescolor,
                all_data=mappings_data,
                underline_headings=False,
                max_width=44,
                jans_name='attributeMapping',
                max_height=4
                )

        attributes_data = [(attribute['name'], attribute['name']) for attribute in common_data.jans_attributes]

        label_widget_width = common_data.app.output.get_size().columns - 3
        self.key_attributes = JansLabelWidget(
                        title=_("Key Attributes"), 
                        values=self.data.get('keyAttributes', []),
                        data=attributes_data,
                        label_width=label_widget_width
                        )
        self.source_attributes = JansLabelWidget(
                        title=_("Source Attributes"), 
                        values=self.data.get('sourceAttributes', []),
                        data=attributes_data,
                        label_width=label_widget_width
                        )

        self.basic_config_content = HSplit([
            self.app.getTitledCheckBox(_("Enable Jannssen Keycloak Link"), name='keycloakLinkEnabled', checked=self.data.get('keycloakLinkEnabled'), jans_help=self.app.get_help_from_schema(self.schema, 'keycloakLinkEnabled'), style=cli_style.check_box, widget_style=cli_style.black_bg_widget),
            self.app.getTitledText(_("LDAP Search Size Limit"), name='ldapSearchSizeLimit', value=self.data.get('ldapSearchSizeLimit') or '25', jans_help=self.app.get_help_from_schema(self.schema, 'ldapSearchSizeLimit'), style=cli_style.titled_text, widget_style=cli_style.black_bg_widget, text_type='integer'),
            self.app.getTitledText(_("Custom LDAP Filter"), name='customLdapFilter', value=self.data.get('customLdapFilter') or '', jans_help=self.app.get_help_from_schema(self.schema, 'ldapSearchSizeLimit'), style=cli_style.titled_text, widget_style=cli_style.black_bg_widget),
            self.key_attributes,
            self.source_attributes,

            self.app.getTitledText(_("Snapshot Directory"), name='snapshotFolder', value=self.data.get('snapshotFolder',''), jans_help=self.app.get_help_from_schema(self.schema, 'snapshotFolder'), style=cli_style.edit_text_required, widget_style=cli_style.black_bg_widget),

            self.app.getTitledWidget(
                _("Update Method"),
                name='updateMethod',
                widget=DropDownWidget(
                    values=[('vds', 'VDS'), ('copy', 'Copy')],
                    value=self.data.get('updateMethod', 'copy'),
                    select_one_option=False
                    ),
                jans_help=self.app.get_help_from_schema(self.schema, 'updateMethod'),
                style=cli_style.edit_text
                ),
            self.app.getTitledCheckBox(_("Default Inum Server"), name='defaultInumServer', checked=self.data.get('defaultInumServer'), jans_help=self.app.get_help_from_schema(self.schema, 'defaultInumServer'), style=cli_style.check_box, widget_style=cli_style.black_bg_widget),
            self.app.getTitledCheckBox(_("Keep External Person"), name='keepExternalPerson', checked=self.data.get('keepExternalPerson'), jans_help=self.app.get_help_from_schema(self.schema, 'keepExternalPerson'), style=cli_style.check_box, widget_style=cli_style.black_bg_widget),
            self.app.getTitledCheckBox(_("Use Search Limit"), name='useSearchLimit', checked=self.data.get('useSearchLimit'), jans_help=self.app.get_help_from_schema(self.schema, 'useSearchLimit'), style=cli_style.check_box, widget_style=cli_style.black_bg_widget),
            self.app.getTitledCheckBox(_("Allow Person Modification"), name='allowPersonModification', checked=self.data.get('allowPersonModification'), jans_help=self.app.get_help_from_schema(self.schema, 'allowPersonModification'), style=cli_style.check_box, widget_style=cli_style.black_bg_widget),

            VSplit([
                    Label(text=mappings_title, style=cli_style.titled_text, width=len(mappings_title)+1),
                    self.mappings_container,
                    Window(width=2),
                    HSplit([
                        Window(height=1),
                        Button(text=add_mapping_title, width=len(add_mapping_title)+4, handler=self.edit_mapping),
                    ]),
                    ],
                    height=5, width=D(),
                    ),
            #supportedUserStatus
            self.app.getTitledText(_("Metric Reporter Interval"), name='metricReporterInterval', value=self.data.get('metricReporterInterval') or '0', jans_help=self.app.get_help_from_schema(self.schema, 'metricReporterInterval'), style=cli_style.titled_text, widget_style=cli_style.black_bg_widget, text_type='integer'),
            self.app.getTitledText(_("Metric Reporter Keep Data Days"), name='metricReporterKeepDataDays', value=self.data.get('metricReporterKeepDataDays') or '0', jans_help=self.app.get_help_from_schema(self.schema, 'metricReporterKeepDataDays'), style=cli_style.titled_text, widget_style=cli_style.black_bg_widget, text_type='integer'),
            self.app.getTitledText(_("Clean Service Interval"), name='cleanServiceInterval', value=self.data.get('cleanServiceInterval') or '0', jans_help=self.app.get_help_from_schema(self.schema, 'cleanServiceInterval'), style=cli_style.titled_text, widget_style=cli_style.black_bg_widget, text_type='integer'),
            self.app.getTitledCheckBox(_("Disable Jdk Logger"), name='disableJdkLogger', checked=self.data.get('disableJdkLogger'), jans_help=self.app.get_help_from_schema(self.schema, 'disableJdkLogger'), style=cli_style.check_box, widget_style=cli_style.black_bg_widget),
            self.app.getTitledCheckBox(_("useLocalCache"), name='useLocalCache', checked=self.data.get('useLocalCache'), jans_help=self.app.get_help_from_schema(self.schema, 'useLocalCache'), style=cli_style.check_box, widget_style=cli_style.black_bg_widget),

            self.app.getTitledText(_("Keycloak Link Server Ip Address"), name='keycloakLinkServerIpAddress', value=self.data.get('keycloakLinkServerIpAddress') or '255.255.255.255', jans_help=self.app.get_help_from_schema(self.schema, 'keycloakLinkServerIpAddress'), style=cli_style.titled_text, widget_style=cli_style.black_bg_widget),
            self.app.getTitledText(_("Keycloak Link Polling Interval"), name='keycloakLinkPollingInterval', value=self.data.get('keycloakLinkPollingInterval') or '0', jans_help=self.app.get_help_from_schema(self.schema, 'keycloakLinkPollingInterval'), style=cli_style.titled_text, widget_style=cli_style.black_bg_widget, text_type='integer'),
            self.app.getTitledText(_("Keycloak Link Last Update"), name='keycloakLinkLastUpdate', value=self.data.get('keycloakLinkLastUpdate') or '', jans_help=self.app.get_help_from_schema(self.schema, 'keycloakLinkLastUpdate'), style=cli_style.titled_text, widget_style=cli_style.black_bg_widget, read_only=True),
            self.app.getTitledText(_("Keycloak Link Last Update Count"), name='keycloakLinkLastUpdateCount', value=self.data.get('keycloakLinkLastUpdateCount') or '5', jans_help=self.app.get_help_from_schema(self.schema, 'keycloakLinkLastUpdateCount'), style=cli_style.titled_text, widget_style=cli_style.black_bg_widget, read_only=True),
            self.app.getTitledText(_("Keycloak Link Problem Count"), name='keycloakLinkProblemCount', value=self.data.get('keycloakLinkProblemCount') or '0', jans_help=self.app.get_help_from_schema(self.schema, 'keycloakLinkProblemCount'), style=cli_style.titled_text, widget_style=cli_style.black_bg_widget, read_only=True),

            get_logging_level_widget(self.data.get('loggingLevel', 'INFO')),

            Window(height=1),
            save_config_buttonc,
            ]

            )

        self.tabs['basic_config'] = ScrollablePane(content=self.basic_config_content, height=D(), display_arrows=False, show_scrollbar=True)
        self.tabs['inum_config'] = HSplit(get_ldap_config_widgets(self.data.get('inumConfig'), widget_style=cli_style.black_bg_widget) +[Window(height=1), save_config_buttonc], width=D())
        self.tabs['target_config'] = HSplit(get_ldap_config_widgets(self.data.get('targetConfig'), widget_style=cli_style.black_bg_widget) +[Window(height=1), save_config_buttonc], width=D())

        keycloak_config_data = self.data.get('keycloakConfiguration', {})
        keycloak_config_widgets = []
        for label_, var_ in (
                        (_("Server URL"), 'serverUrl'),
                        (_("Rrealm"), 'realm'),
                        (_("Client ID"), 'clientId'),
                        (_("Client Secret"), 'clientSecret'),
                        (_("Grant Type"), 'grantType'),
                        (_("Username"), 'username'),
                        (_("Password"), 'password'),
                        ):

            keycloak_config_widgets.append(
                        self.app.getTitledText(
                            label_, 
                            name=var_, value=keycloak_config_data.get(var_) or '',
                            jans_help=self.app.get_help_from_schema(self.schema, var_),
                            style=cli_style.titled_text, widget_style=cli_style.black_bg_widget
                        )
                )

        self.tabs['keycloak_config'] = HSplit(keycloak_config_widgets +[Window(height=1), save_config_buttonc], width=D())


        self.source_config_container = JansVerticalNav(
                myparent=self.app,
                headers=[_("Name"), _("Enabled")],
                preferred_size= self.app.get_column_sizes(.7, .3),
                data=[],
                on_enter=self.edit_source_config,
                on_display=self.app.data_display_dialog,
                on_delete=self.delete_source_config,
                selectes=0,
                headerColor=cli_style.navbar_headcolor,
                entriesColor=cli_style.navbar_entriescolor,
                all_data=[],
                hide_headers=True
            )

        add_source_config_button_label = _("Add Source LDAP Server")
        self.tabs['source_config'] = HSplit([
                                self.source_config_container,
                                Window(height=1),
                                VSplit([Button(_("Add Source LDAP Server"), handler=self.add_source_config, width=len(add_source_config_button_label)+4)], align=HorizontalAlign.CENTER),
                                ],
                                width=D()
                                )


        self.nav_selection_changed(list(self.tabs)[0])
        self.app.invalidate()

    async def get_configuration(self) -> None:
        'Coroutine for getting Janssen KC Link configuration.'
        try:
            response = self.app.cli_object.process_command_by_id(
                        operation_id='get-kc-link-properties',
                        url_suffix='',
                        endpoint_args='',
                        data_fn=None,
                        data={}
                        )

        except Exception as e:
            self.app.show_message(_("Error getting Janssen KC Link configuration"), str(e), tobefocused=self.app.center_container)
            self.app.disable_plugin(self.pid)
            return

        if response.status_code not in (200, 201):
            self.app.show_message(_("Error getting Janssen KC Link configuration"), str(response.text), tobefocused=self.app.center_container)
            self.app.disable_plugin(self.pid)
            return

        self.data = response.json()
        for i in range(5):
            if not common_data.jans_attributes:
                await asyncio.sleep(1)
        self.create_widgets()
        self.update_source_config_container()

    def prepare_navbar(self) -> None:
        """prepare the navbar for the current Plugin 
        """
        self.nav_bar = JansNavBar(
                    self.app,
                    entries=[
                            ('basic_config', '[B]asic Configuration'),
                            ('inum_config', '[I]num Configuration'),
                            ('source_config', ' [S]ources'),
                            ('target_config', '[T]arget Configuration'),
                            ('keycloak_config', 'K[e]ycloak Configuration'),
                            ],
                    selection_changed=self.nav_selection_changed,
                    select=0,
                    jans_name='kc:nav_bar'
                    )

    def prepare_containers(self) -> None:
        """prepare the main container (tabs) for the current Plugin 
        """

        self.tabs = OrderedDict()
        self.main_area = HSplit([Label("Please waith while retreiving attributes from server")], width=D())

        self.main_container = HSplit([
                                        Box(self.nav_bar.nav_window, style=cli_style.sub_navbar, height=1),
                                        DynamicContainer(lambda: self.main_area),
                                        ],
                                    height=D(),
                                    style=cli_style.navbar_headcolor
                                    )

    def nav_selection_changed(
                self,
                selection: str
            ) -> None:

        """This method for the selection change

        Args:
            selection (str): the current selected tab
        """

        if selection in self.tabs:
            self.main_area = HSplit([self.tabs[selection]],height=D()) 
        else:
            self.main_area = self.app.not_implemented



    def update_source_config_container(self):
        self.source_config_container.clear()
        source_configs = self.data.get('sourceConfigs', [])
        if source_configs:
            self.source_config_container.hide_headers = False
            for sc in source_configs:
                self.source_config_container.add_item((sc.get('configId', ''), str(sc.get('enabled', False))))
            self.source_config_container.all_data = source_configs

    def save_config(self, delete_source_config:Optional[int]=None) -> None:
        """This method for saving the configuration
        """

        #configuration = self.make_data_from_dialog(tabs={'configuration': self.tabs['source_config']})
        #customer_backend = self.make_data_from_dialog(tabs={'customer_backend': self.tabs['customer_backend']})

        new_data = copy.deepcopy(self.data)
        new_data['inumConfig'] = get_data_for_ldap_widgets(self.tabs['inum_config'])
        new_data['targetConfig'] = get_data_for_ldap_widgets(self.tabs['target_config'])
        basic_config = self.make_data_from_dialog(tabs={'basic_config': self.basic_config_content})
        new_data.update(basic_config)

        new_data['keyAttributes'] = self.key_attributes.get_values()
        new_data['sourceAttributes'] = self.source_attributes.get_values()
        new_data['attributeMapping'] = [ {'source': source, 'destination': destination} for source, destination in self.mappings_container.data ]
        new_data['keycloakConfiguration'] = self.make_data_from_dialog(tabs={'keycloak_config': self.tabs['keycloak_config']})


        if 'sourceConfigs' not in new_data:
            new_data['sourceConfigs'] = []

        if delete_source_config is not None:
            try:
                 new_data['sourceConfigs'].pop(delete_source_config)
            except Exception as e:
                self.app.show_message(_('Error Deleting Source Config'), str(e))
                return

        new_data.pop('keycloakLinkLastUpdate')

        async def coroutine():
            cli_args = {'operation_id': 'put-kc-link-properties', 'data': new_data}
            self.app.start_progressing(_("Saving KC Link Configuration..."))
            response = await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            if response.status_code == 200:
                self.app.stop_progressing("")
                self.app.show_message(title=_(common_strings.success), message=_("Jans KC Link configuration was saved."), tobefocused=self.app.center_container)
                self.data = response.json()
                self.update_source_config_container()
            else:
                self.app.show_message(_(common_strings.error), response.text + '\n' + response.reason, tobefocused=self.app.center_container)

        asyncio.ensure_future(coroutine())


    def edit_source_config_dialog(self, data=None, selected=None):
        if data:
            title = _("Edit Source LDAP Config")
        else:
            data = {}
            title = _("Add new Source LDAP Config")

        body = HSplit(get_ldap_config_widgets(data), width=D())

        def save_source_config(dialog):
            sc_data = get_data_for_ldap_widgets(dialog.body)
            if 'sourceConfigs' not in self.data:
                self.data['sourceConfigs'] = []
            if selected is not None:
                self.data['sourceConfigs'][selected] = sc_data
            else:
                self.data['sourceConfigs'].append(sc_data)
            dialog.future.set_result(True)
            self.save_config()

        save_button = Button(_("Save"), handler=save_source_config)
        save_button.keep_dialog = True
        canncel_button = Button(_("Cancel"))
        buttons = [save_button, canncel_button]
        dialog = JansGDialog(self.app, title=title, body=body, buttons=buttons)
        self.app.show_jans_dialog(dialog)


    def set_center_frame(self) -> None:
        """center frame content
        """
        self.app.center_container = self.main_container


