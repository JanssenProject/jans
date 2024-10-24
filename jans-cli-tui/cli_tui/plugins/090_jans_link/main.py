import copy
import asyncio
from collections import OrderedDict
from typing import Any, Optional
from prompt_toolkit.layout.containers import HSplit, DynamicContainer,\
    VSplit, Window, HorizontalAlign, ConditionalContainer
from prompt_toolkit.filters import Condition
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.widgets import Button, Label, Box, Dialog
from prompt_toolkit.application import Application
from wui_components.jans_nav_bar import JansNavBar
from wui_components.jans_drop_down import DropDownWidget
from wui_components.jans_vetrical_nav import JansVerticalNav
from wui_components.jans_cli_dialog import JansGDialog
from wui_components.widget_collections import get_ldap_config_widgets,\
    get_data_for_ldap_widgets

from utils.utils import common_data

from utils.multi_lang import _
from utils.utils import DialogUtils
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
        self.pid = 'jans-link'
        self.name = 'Jans Lin[k]'
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
        self.schema = self.app.cli_object.get_schema_from_reference('Jans Link Plugin', '#/components/schemas/AppConfiguration')

        mappings_title = _("Mappings:")
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

        self.tabs['configuration'] = HSplit([
                                self.app.getTitledCheckBox(_("Enabled"), name='linkEnabled', checked=self.data.get('linkEnabled'), jans_help=self.app.get_help_from_schema(self.schema, 'linkEnabled'), style=cli_style.check_box, widget_style=cli_style.black_bg_widget),
                                self.app.getTitledWidget(
                                    _("Refresh Method"),
                                    name='updateMethod',
                                    widget=DropDownWidget(
                                        values=[('copy', 'Copy'), ('vds', 'VDS')],
                                        value=self.data.get('updateMethod'),
                                        select_one_option=False
                                        ),
                                    jans_help=self.app.get_help_from_schema(self.schema, 'updateMethod'),
                                    style=cli_style.edit_text
                                    ),
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
                                self.app.getTitledText(_("Polling Interval (minutes)"), name='pollingInterval', value=self.data.get('pollingInterval') or '20', jans_help=self.app.get_help_from_schema(self.schema, 'pollingInterval'), style=cli_style.titled_text, widget_style=cli_style.black_bg_widget, text_type='integer'),
                                self.app.getTitledText(_("Snapshot Directory"), name='snapshotFolder', value=self.data.get('snapshotFolder',''), jans_help=self.app.get_help_from_schema(self.schema, 'snapshotFolder'), style=cli_style.edit_text_required, widget_style=cli_style.black_bg_widget),
                                self.app.getTitledText(_("Snapshot Count"), name='snapshotMaxCount', value=self.data.get('snapshotMaxCount', '10'), jans_help=self.app.get_help_from_schema(self.schema, 'snapshotMaxCount'), style=cli_style.edit_text_required, widget_style=cli_style.black_bg_widget, text_type='integer'),
                                self.app.getTitledCheckBox(_("Keep External Persons"), name='keepExternalPerson', checked=self.data.get('keepExternalPerson'), jans_help=self.app.get_help_from_schema(self.schema, 'keepExternalPerson'), style=cli_style.check_box, widget_style=cli_style.black_bg_widget),
                                self.app.getTitledCheckBox(_("Load Source Data withLimited Search"), name='useSearchLimit', checked=self.data.get('useSearchLimit'), jans_help=self.app.get_help_from_schema(self.schema, 'useSearchLimit'), style=cli_style.check_box, widget_style=cli_style.black_bg_widget),
                                self.app.getTitledWidget(
                                    _("Logging Level"),
                                    name='loggingLevel',
                                    widget=DropDownWidget(
                                        values=[('TRACE', 'TRACE'), ('DEBUG', 'DEBUG'), ('INFO', 'INFO'), ('WARN', 'WARN'),('ERROR', 'ERROR'),('FATAL', 'FATAL'),('OFF', 'OFF')],
                                        value=self.data.get('loggingLevel', 'INFO'),
                                        select_one_option=False
                                        ),
                                    jans_help=self.app.get_help_from_schema(self.schema, 'loggingLevel'),
                                    style=cli_style.edit_text
                                    ),

                                Window(height=1),
                                save_config_buttonc,
                                ],
                                width=D()
                                )


        self.tabs['customer_backend'] = HSplit([
                                self.app.getTitledText(
                                    _("Key Attributes"),
                                    name='keyAttributes',
                                    value=' '.join(self.data.get('keyAttributes', [])),
                                    style=cli_style.titled_text,
                                    widget_style=cli_style.black_bg_widget,
                                    jans_help=_("Space seperated key attributes")
                                ),
                                self.app.getTitledText(
                                    _("Key Object Classes"),
                                    name='keyObjectClasses',
                                    value=' '.join(self.data.get('keyObjectClasses', [])),
                                    style=cli_style.titled_text,
                                    widget_style=cli_style.black_bg_widget,
                                    jans_help=_("Space seperated key object classes")
                                ),
                                self.app.getTitledText(
                                    _("Source Attributes"),
                                    name='sourceAttributes',
                                    value=' '.join(self.data.get('sourceAttributes', [])),
                                    style=cli_style.titled_text,
                                    widget_style=cli_style.black_bg_widget,
                                    jans_help=_("Space seperated source attributes")
                                ),
                                self.app.getTitledText(_("Custom LDAP Filter"), name='customLdapFilter', value=self.data.get('customLdapFilter',''), jans_help=self.app.get_help_from_schema(self.schema, 'customLdapFilter'), style=cli_style.titled_text, widget_style=cli_style.black_bg_widget),
                                Window(height=1),
                                save_config_buttonc,
                                ],
                                width=D()
                                )


        self.source_backends_container = JansVerticalNav(
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
        self.tabs['source_backend'] = HSplit([
                                self.source_backends_container,
                                Window(height=1),
                                VSplit([Button(_("Add Source LDAP Server"), handler=self.add_source_config, width=len(add_source_config_button_label)+4)], align=HorizontalAlign.CENTER),
                                ],
                                width=D()
                                )

        self.inum_db_server_container = HSplit(get_ldap_config_widgets(self.data.get('inumConfig'), widget_style=cli_style.black_bg_widget), width=D())

        self.default_inum_db_server_checkbox = self.app.getTitledCheckBox(
                                title=_("Default Inum Server"), 
                                name='defaultInumServer', 
                                checked=self.data.get('defaultInumServer', False),
                                style=cli_style.check_box
                                )


        self.tabs['inum_db_server'] = HSplit([
                                self.default_inum_db_server_checkbox ,
                                ConditionalContainer(
                                    content=self.inum_db_server_container,
                                    filter=Condition(lambda: not self.default_inum_db_server_checkbox.me.checked),
                                ),
                                Window(height=1),
                                save_config_buttonc,
                                ],
                                width=D()
                                )



        self.nav_selection_changed(list(self.tabs)[0])

    async def get_configuration(self) -> None:
        'Coroutine for getting Janssen Link configuration.'
        try:
            response = self.app.cli_object.process_command_by_id(
                        operation_id='get-jans-link-properties',
                        url_suffix='',
                        endpoint_args='',
                        data_fn=None,
                        data={}
                        )

        except Exception as e:
            self.app.show_message(_("Error getting Janssen Link configuration"), str(e), tobefocused=self.app.center_container)
            self.app.disable_plugin(self.pid)
            return

        if response.status_code not in (200, 201):
            self.app.show_message(_("Error getting Janssen Link configuration"), str(response.text), tobefocused=self.app.center_container)
            self.app.disable_plugin(self.pid)
            return

        self.data = response.json()
        self.create_widgets()
        self.update_source_backends_container()

    def prepare_navbar(self) -> None:
        """prepare the navbar for the current Plugin 
        """
        self.nav_bar = JansNavBar(
                    self.app,
                    entries=[
                            ('configuration', 'C[o]nfiguration'),
                            ('customer_backend', 'Customer [B]ackend Key Attributes'),
                            ('source_backend', 'Source Backend [L]DAP Server'),
                            ('inum_db_server', '[I]num DB Server'),
                            ],
                    selection_changed=self.nav_selection_changed,
                    select=0,
                    jans_name='fido:nav_bar'
                    )

    def prepare_containers(self) -> None:
        """prepare the main container (tabs) for the current Plugin 
        """

        self.tabs = OrderedDict()
        self.main_area = HSplit([Label("configuration")],width=D())

        self.main_container = HSplit([
                                        Box(self.nav_bar.nav_window, style='class:sub-navbar', height=1),
                                        DynamicContainer(lambda: self.main_area),
                                        ],
                                    height=D(),
                                    style='class:outh_maincontainer'
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

    def update_source_backends_container(self):
        self.source_backends_container.clear()
        source_configs = self.data.get('sourceConfigs', [])
        if source_configs:
            self.source_backends_container.hide_headers = False
            for sc in source_configs:
                self.source_backends_container.add_item((sc.get('configId', ''), str(sc.get('enabled', False))))
            self.source_backends_container.all_data = source_configs

    def save_config(self, delete_source_config:Optional[int]=None) -> None:
        """This method for saving the configuration
        """

        configuration = self.make_data_from_dialog(tabs={'configuration': self.tabs['configuration']})
        customer_backend = self.make_data_from_dialog(tabs={'customer_backend': self.tabs['customer_backend']})

        new_data = copy.deepcopy(self.data)
        new_data['defaultInumServer'] = self.default_inum_db_server_checkbox.me.checked
        if not self.default_inum_db_server_checkbox.me.checked:
            inum_config = self.make_data_from_dialog(tabs={'inum_config': self.inum_db_server_container})
            inum_config['servers'] = inum_config['servers'].split()
            inum_config['baseDNs'] = inum_config['baseDNs'].split()

            if 'inumConfig' in new_data:
                new_data['inumConfig'].update(inum_config)
            else:
                new_data['inumConfig'] = inum_config

            if not 'useAnonymousBind' in new_data['inumConfig']:
                new_data['inumConfig']['useAnonymousBind'] = False

            if not 'level' in new_data['inumConfig']:
                new_data['inumConfig']['level'] = 0

            if not 'version' in new_data['inumConfig']:
                new_data['inumConfig']['version'] = 0
            else:
                new_data['inumConfig']['version'] += 1

        if 'sourceConfigs' not in new_data:
            new_data['sourceConfigs'] = []

        if delete_source_config is None:
            configuration['attributeMapping'] = [ {'source': source, 'destination': destination} for source, destination in self.mappings_container.data ]
            configuration['keyAttributes'] = customer_backend['keyAttributes'].split()
            configuration['keyObjectClasses'] = customer_backend['keyObjectClasses'].split()
            configuration['sourceAttributes'] = customer_backend['sourceAttributes'].split()
            configuration['customLdapFilter'] = customer_backend['customLdapFilter']
            new_data.update(configuration)
        else:
            try:
                new_data['sourceConfigs'].pop(delete_source_config)
            except Exception as e:
                self.app.show_message(_('Error Deleting Source Config'), str(e))

        async def coroutine():
            cli_args = {'operation_id': 'put-jans-link-properties', 'data': new_data}
            self.app.start_progressing(_("Saving Jans Link Configuration..."))
            response = await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            if response.status_code == 200:
                self.app.stop_progressing()
                self.app.show_message(title=_(common_strings.success), message=_("Jans Link configuration was saved."), tobefocused=self.app.center_container)
                self.data = response.json()
                self.update_source_backends_container()
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


