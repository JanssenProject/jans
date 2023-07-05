import asyncio
from collections import OrderedDict
from functools import partial
from typing import Any
from prompt_toolkit.layout.containers import HSplit, DynamicContainer, VSplit, Window
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.widgets import Button, Label, Box, Dialog
from prompt_toolkit.application import Application
from wui_components.jans_nav_bar import JansNavBar
from wui_components.jans_drop_down import DropDownWidget
from wui_components.jans_vetrical_nav import JansVerticalNav
from wui_components.jans_cli_dialog import JansGDialog
from wui_components.jans_label_container import JansLabelContainer

from utils.multi_lang import _
from utils.utils import DialogUtils
from utils.static import cli_style


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
        self.pid = 'cache-refresh'
        self.name = 'Jans Lin[k]'
        self.server_side_plugin = True
        self.page_entered = False

        self.data = {}

        self.prepare_navbar()
        self.prepare_containers()


    def process(self) -> None:
        pass

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

        source_widget = self.app.getTitledText(_("Source Attribute"), name='source_attribute', value=source_attribute, style=cli_style.titled_text, jans_help=_("Source LDAP Attribute"))
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

        source_attribute_widget = self.app.getTitledText(_("Source Attribute"), name='source_attribute', value='', style=cli_style.titled_text, jans_help=_("Source Attribute"))

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


    def create_widgets(self):
        self.nav_dialog_width = self.app.output.get_size().columns - 2
        self.schema = self.app.cli_object.get_schema_from_reference('cacherefresh', '#/components/schemas/CacheRefreshConfiguration')

        mappings_title = _("Mappings:")
        add_mapping_title = _("Add Mapping")

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
                                self.app.getTitledText(_("Server IP Address"), name='cacheRefreshServerIpAddress', value=self.data.get('cacheRefreshServerIpAddress',''), jans_help=self.app.get_help_from_schema(self.schema, 'cacheRefreshServerIpAddress'), style=cli_style.titled_text, widget_style=cli_style.black_bg_widget),
                                self.app.getTitledText(_("Polling Interval (minutes)"), name='vdsCacheRefreshPollingInterval', value=self.data.get('vdsCacheRefreshPollingInterval') or '20', jans_help=self.app.get_help_from_schema(self.schema, 'vdsCacheRefreshPollingInterval'), style=cli_style.titled_text, widget_style=cli_style.black_bg_widget, text_type='integer'),
                                self.app.getTitledText(_("Snapshot Directory"), name='snapshotFolder', value=self.data.get('snapshotFolder',''), jans_help=self.app.get_help_from_schema(self.schema, 'snapshotFolder'), style=cli_style.edit_text_required, widget_style=cli_style.black_bg_widget),
                                self.app.getTitledText(_("Snapshot Count"), name='snapshotMaxCount', value=self.data.get('snapshotMaxCount', '10'), jans_help=self.app.get_help_from_schema(self.schema, 'snapshotMaxCount'), style=cli_style.edit_text_required, widget_style=cli_style.black_bg_widget, text_type='integer'),
                                self.app.getTitledCheckBox(_("Keep External Persons"), name='keepExternalPerson', checked=self.data.get('keepExternalPerson'), jans_help=self.app.get_help_from_schema(self.schema, 'keepExternalPerson'), style=cli_style.check_box, widget_style=cli_style.black_bg_widget),
                                self.app.getTitledCheckBox(_("Load Source Data withLimited Search"), name='useSearchLimit', checked=self.data.get('useSearchLimit'), jans_help=self.app.get_help_from_schema(self.schema, 'useSearchLimit'), style=cli_style.check_box, widget_style=cli_style.black_bg_widget),

                                Window(height=1),
                                VSplit([Window(),
                                HSplit([Button(_("Save"), handler=self.save_config)]),
                                Window()]),
                                ],
                                width=D()
                                )

        add_key_attribute_button = VSplit([Window(), self.app.getButton(
            text=_("Add Key Attribute"),
            name='add_key_attribute',
            jans_help=_("Add Key Attribute"),
            handler=self.add_key_attribute)
        ])


        self.key_attributes_container = JansLabelContainer(
            title=_('Key Attributes'),
            width=self.nav_dialog_width,
            on_display=self.app.data_display_dialog,
            on_delete=self.delete_key_attribute,
            buttonbox=add_key_attribute_button,
            entries=[(keya, keya) for keya in self.data.get('keyAttributes', [])],
        )

        add_object_class_button = VSplit([Window(), self.app.getButton(
            text=_("Add Key Object Class"),
            name='add_object_class',
            jans_help=_("Add Key Object Class"),
            handler=self.add_object_class)
        ])


        self.object_classes_container = JansLabelContainer(
            title=_('Key Object Classes'),
            width=self.nav_dialog_width,
            on_display=self.app.data_display_dialog,
            on_delete=self.delete_object_class,
            buttonbox=add_object_class_button,
            entries=[(keyoc, keyoc) for keyoc in self.data.get('keyObjectClasses', [])],
        )

        add_source_attribute_button = VSplit([Window(), self.app.getButton(
            text=_("Add Source Attribute"),
            name='add_object_class',
            jans_help=_("Add Source Attribute"),
            handler=self.add_source_attribute)
        ])

        self.source_attribute_container = JansLabelContainer(
            title=_('Source Attributes'),
            width=self.nav_dialog_width,
            on_display=self.app.data_display_dialog,
            on_delete=self.delete_source_attribute,
            buttonbox=add_source_attribute_button,
            entries=[(sourcea, sourcea) for sourcea in self.data.get('sourceAttributes', [])],
        )

        self.tabs['customer_backend'] = HSplit([
                                self.key_attributes_container,
                                self.object_classes_container,
                                self.source_attribute_container,
                                self.app.getTitledText(_("Custom LDAP Filter"), name='customLdapFilter', value=self.data.get('customLdapFilter',''), jans_help=self.app.get_help_from_schema(self.schema, 'customLdapFilter'), style=cli_style.titled_text, widget_style=cli_style.black_bg_widget),
                                ],
                                width=D()
                                )

        self.tabs['source_backend'] = HSplit([
                                self.app.getTitledText(_("Source Backend"), name='authenticatorCertsFolder'),
                                ],
                                width=D()
                                )

        self.nav_selection_changed(list(self.tabs)[0])

    async def get_configuration(self) -> None:
        'Coroutine for getting cache refresh configuration.'
        try:
            response = self.app.cli_object.process_command_by_id(
                        operation_id='get-properties-cache-refresh',
                        url_suffix='',
                        endpoint_args='',
                        data_fn=None,
                        data={}
                        )

        except Exception as e:
            self.app.show_message(_("Error getting Cache Refresh configuration"), str(e), tobefocused=self.app.center_container)
            self.app.disable_plugin(self.pid)
            return

        if response.status_code not in (200, 201):
            self.app.show_message(_("Error getting Cache Refresh configuration"), str(response.text), tobefocused=self.app.center_container)
            self.app.disable_plugin(self.pid)
            return

        self.data = response.json()
        self.create_widgets()

    def prepare_navbar(self) -> None:
        """prepare the navbar for the current Plugin 
        """
        self.nav_bar = JansNavBar(
                    self.app,
                    entries=[('configuration', 'C[o]nfiguration'), ('customer_backend', 'Customer [B]ackend Key Attributes'), ('source_backend', 'Source Backend [L]DAP Server')],
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

    def save_config(self) -> None:
        """This method for saving the configuration
        """
        
        configuration = self.make_data_from_dialog(tabs={'configuration': self.tabs['configuration']})
        customer_backend = self.make_data_from_dialog(tabs={'customer_backend': self.tabs['customer_backend']})
        source_backend = self.make_data_from_dialog(tabs={'source_backend': self.tabs['source_backend']})

        configuration['attributeMapping'] = [ {'source': source, 'destination': destination} for source, destination in self.mappings_container.data ]
        configuration['keyAttributes'] = [ keya[0] for keya in self.key_attributes_container.entries ]
        configuration['keyObjectClasses'] = [ keyoc[0] for keyoc in self.object_classes_container.entries ]
        configuration['sourceAttributes'] = [ sourcea[0] for sourcea in self.source_attribute_container.entries ]
        configuration['customLdapFilter'] = customer_backend['customLdapFilter']

        self.data.update(configuration)
        import json
        open("/tmp/data.txt","w").write(json.dumps(self.data, indent=2))

        async def coroutine():
            cli_args = {'operation_id': 'put-properties-fido2', 'data': fido2_config}
            self.app.start_progressing(_("Saving FIDO Configuration..."))
            response = await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing(_("FIDO Configuration was saved."))

        #asyncio.ensure_future(coroutine())

    def set_center_frame(self) -> None:
        """center frame content
        """
        self.app.center_container = self.main_container


