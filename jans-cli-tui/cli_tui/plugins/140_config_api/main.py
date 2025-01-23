import copy
import asyncio
from collections import OrderedDict
from typing import Any, Optional
from functools import partial

import prompt_toolkit
from prompt_toolkit.layout import ScrollablePane
from prompt_toolkit.layout.containers import HSplit, DynamicContainer, VSplit, Window, HorizontalAlign
from prompt_toolkit.formatted_text import HTML
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.widgets import Button, Label, Dialog, TextArea, Box, Frame
from prompt_toolkit.application import Application
from wui_components.jans_nav_bar import JansNavBar
from wui_components.jans_vetrical_nav import JansVerticalNav
from wui_components.jans_cli_dialog import JansGDialog
from wui_components.widget_collections import get_logging_level_widget
from wui_components.jans_label_widget import JansLabelWidget

from utils.multi_lang import _
from utils.utils import DialogUtils, common_data
from utils.static import cli_style, common_strings
from utils.background_tasks import get_asset_services

class Plugin(DialogUtils):
    """This is a general class for plugins 
    """
    def __init__(
        self,
        app: Application
        ) -> None:
        """init for Plugin class "jans saml plugin"

        Args:
            app (Generic): The main Application class
        """
        self.app = app
        
        self.pid = 'config-api'
        self.name = 'Config API'
        self.server_side_plugin = False
        self.page_entered = False
        self.data = {}

        self.prepare_navbar()
        self.prepare_containers()


    def init_plugin(self) -> None:
        """The initialization for this plugin
        """
        self.app.create_background_task(self.get_configuration())
        self.app.create_background_task(get_asset_services())


    def set_center_frame(self) -> None:
        """center frame content
        """
        self.app.center_container = self.main_container

    def create_widgets(self):

        self.schema = self.app.cli_object.get_schema_from_reference('', '#/components/schemas/ApiAppConfiguration')

        label_widget_width = common_data.app.output.get_size().columns - 3


        def add_acr_validation(widget):

            acr_text_area = TextArea()

            def do_add_acr(dialog):
                widget.container.add_label(acr_text_area.text, acr_text_area.text)

            acr_label = _("ACR Name")
            body = VSplit([Label(acr_label + ' :', width=(len(acr_label)+3)), acr_text_area])
            buttons = [Button(_("Cancel")), Button(_("OK"), handler=do_add_acr)]
            dialog = JansGDialog(self.app, title=_("ACR to Exclude Validation"), body=body, buttons=buttons, width=self.app.dialog_width-20)
            self.app.show_jans_dialog(dialog)

        self.user_exclusion_attributes_widget = JansLabelWidget(
                        title = _("User Exclusion Attributes"),
                        values = ['test'],
                        data = [],
                        label_width=label_widget_width
                        )

        self.user_mandatory_attributes_widget = JansLabelWidget(
                        title = _("User Mandatory Attributes"),
                        values = ['test'],
                        data = [],
                        label_width=label_widget_width
                        )

        self.acr_exclusion_widget = JansLabelWidget(
                        title = _("Exclude ACR Validation"),
                        values = self.data.get('acrExclusionList', []),
                        data = [],
                        label_width=label_widget_width,
                        add_handler=add_acr_validation
                        )

        self.tabs['main_'] = ScrollablePane(content=HSplit([

                        self.app.getTitledText(
                            _("Maximum Number of Result Per Page"),
                            name='maxCount',
                            value=self.data.get('maxCount') or '50',
                            jans_help=self.app.get_help_from_schema(self.schema, 'maxCount'),
                            style=cli_style.edit_text,
                            widget_style=cli_style.black_bg_widget,
                            text_type='integer'
                        ),

                        self.app.getTitledCheckBox(
                            _("Enable Configuring OAuth"),
                            name='configOauthEnabled',
                            checked=self.data.get('configOauthEnabled'),
                            jans_help=self.app.get_help_from_schema(self.schema, 'configOauthEnabled'),
                            style=cli_style.check_box,
                            widget_style=cli_style.black_bg_widget
                        ),

                        self.app.getTitledCheckBox(
                            _("Disable Logger Timer"),
                            name='disableLoggerTimer',
                            checked=self.data.get('disableLoggerTimer'),
                            jans_help=self.app.get_help_from_schema(self.schema, 'disableLoggerTimer'),
                            style=cli_style.check_box,
                            widget_style=cli_style.black_bg_widget
                        ),

                        self.app.getTitledCheckBox(
                            _("Disable Audit Logger"),
                            name='disableAuditLogger',
                            checked=self.data.get('disableAuditLogger'),
                            jans_help=self.app.get_help_from_schema(self.schema, 'disableAuditLogger'),
                            style=cli_style.check_box,
                            widget_style=cli_style.black_bg_widget
                        ),

                        self.app.getTitledCheckBox(
                            _("Enable Custom Attribute Validation"),
                            name='customAttributeValidationEnabled',
                            checked=self.data.get('customAttributeValidationEnabled'),
                            jans_help=self.app.get_help_from_schema(self.schema, 'customAttributeValidationEnabled'),
                            style=cli_style.check_box,
                            widget_style=cli_style.black_bg_widget
                        ),

                        self.app.getTitledCheckBox(
                            _("Enable acr Validation"),
                            name='acrValidationEnabled',
                            checked=self.data.get('acrValidationEnabled'),
                            jans_help=self.app.get_help_from_schema(self.schema, 'acrValidationEnabled'),
                            style=cli_style.check_box,
                            widget_style=cli_style.black_bg_widget
                        ),

                        self.acr_exclusion_widget,

                    common_data.app.getTitledText(
                        title=_("Approved Issuer"),
                        name='apiApprovedIssuer',
                        value=' '.join(self.data.get('apiApprovedIssuer', [])),
                        style=cli_style.edit_text,
                        jans_help=common_data.app.get_help_from_schema(self.schema, 'apiApprovedIssuer'),
                        jans_list_type=True,
                        widget_style=cli_style.black_bg_widget
                    ),

                    get_logging_level_widget(self.data.get('loggingLevel', 'INFO')),

                    common_data.app.getTitledText(
                        title=_("Logging Layout"),
                        name='loggingLayout',
                        value=self.data.get('loggingLayout', 'text'),
                        style=cli_style.edit_text,
                        jans_help=self.app.get_help_from_schema(self.schema, 'loggingLayout'),
                        widget_style=cli_style.black_bg_widget
                    ),

                    common_data.app.getTitledText(
                        title=_("External Logger Configuration"),
                        name='externalLoggerConfiguration',
                        value=self.data.get('externalLoggerConfiguration', ''),
                        style=cli_style.edit_text,
                        jans_help=self.app.get_help_from_schema(self.schema, 'externalLoggerConfiguration'),
                        widget_style=cli_style.black_bg_widget
                    ),


                    self.app.getTitledCheckBox(
                        _("Disable JDK Logger"),
                        name='disableJdkLogger',
                        checked=self.data.get('disableJdkLogger'),
                        jans_help=self.app.get_help_from_schema(self.schema, 'disableJdkLogger'),
                        style=cli_style.check_box,
                        widget_style=cli_style.black_bg_widget
                    ),

                    self.user_exclusion_attributes_widget,
                    self.user_mandatory_attributes_widget,
                    Window(height=D()),
                    ],
                    width=D(),
                    )
                , height=D(), display_arrows=False, show_scrollbar=True)

        self.agama_configuration_mandatory_attributes_widget = JansLabelWidget(
                        title = _("Mandatory Attributes"),
                        values = copy.deepcopy(self.data.get('agamaConfiguration', {}).get('mandatoryAttributes', [])),
                        data = [('qname', 'qname'),('source', 'source')],
                        label_width=label_widget_width
                        )

        self.agama_configuration_optional_attributes_widget = JansLabelWidget(
                        title = _("Optional Attributes"),
                        values = copy.deepcopy(self.data.get('agamaConfiguration', {}).get('optionalAttributes', [])),
                        data = [('serialVersionUID', 'serialVersionUID'), ('enabled', 'enabled')],
                        label_width=label_widget_width
                        )

        self.tabs['agamaConfiguration'] = HSplit([
            self.agama_configuration_mandatory_attributes_widget,
            self.agama_configuration_optional_attributes_widget,
            Window(height=D()),
            ],
             width=D()
             )


        self.tabs['dataFormatConversionConf'] = HSplit([
            common_data.app.getTitledCheckBox(
                    _("Enable"),
                    name='enabled',
                    checked=self.data.get('dataFormatConversionConf', {}).get('enabled'),
                    style=cli_style.check_box,
                    widget_style=cli_style.black_bg_widget
                ),
            common_data.app.getTitledText(
                    title=_("Ignore Http Method"),
                    name='ignoreHttpMethod',
                    value=copy.deepcopy(self.data.get('dataFormatConversionConf', {}).get('ignoreHttpMethod', [])),
                    style=cli_style.edit_text,
                    widget_style=cli_style.black_bg_widget,
                    jans_help="Enter values each line",
                    jans_list_type=True,
                    height=3
                    ),
            ],
             width=D()
             )

        self.audit_log_configuration_header_attributes_widget = JansLabelWidget(
                        title = _("Header Attributes"),
                        values = copy.deepcopy(self.data.get('auditLogConf', {}).get('headerAttributes', [])),
                        data = [('User-inum', 'User-inum')],
                        label_width=label_widget_width
                        )

        self.tabs['auditLogConf'] = HSplit([
            common_data.app.getTitledCheckBox(
                    _("Enabled"),
                    name='enabled',
                    checked=self.data.get('auditLogConf', {}).get('enabled'),
                    style=cli_style.check_box,
                    widget_style=cli_style.black_bg_widget
                ),

            common_data.app.getTitledText(
                    title=_("Ignore Http Method"),
                    name='ignoreHttpMethod',
                    value=copy.deepcopy(self.data.get('auditLogConf', {}).get('ignoreHttpMethod', [])),
                    style=cli_style.edit_text,
                    widget_style=cli_style.black_bg_widget,
                    jans_help="Enter values each line",
                    jans_list_type=True,
                    height=3
                    ),

            self.audit_log_configuration_header_attributes_widget,
            Window(height=D()),
            ],
            width=D(),

            )


        def edit_asset_dir_mapping(*args, **kwargs: Any) -> None:

            if kwargs:
                title = _("Edit Mapping Properties")
                mapping_data = (
                    kwargs['data']['directory'],
                    ' '.join(kwargs['data']['type']),
                    kwargs['data']['description'],
                    kwargs['data']['jansServiceModule']
                    )
                services = []
            else:
                title = _("Add Mapping Properties")
                mapping_data = ('','', '', [])

            directory_widget = self.app.getTitledText(
                    _("Directory"),
                    name='directory',
                    value=mapping_data[0],
                    style=cli_style.edit_text,
                    jans_help=_("Directory of asset mapping")
                    )

            type_widget = self.app.getTitledText(
                    _("Type"),
                    name='type',
                    value=mapping_data[1],
                    style=cli_style.edit_text,
                    jans_help=_("File types seperated by space")
                    )

            description_widget = self.app.getTitledText(
                    _("Description"),
                    name='description',
                    value=mapping_data[2],
                    style=cli_style.edit_text,
                    jans_help=_("Description for asset mapping")
                    )

            services_widget = self.app.getTitledCheckBoxList(
                    _("Services"),
                    name='jansServiceModule',
                    values=common_data.asset_services,
                    current_values=mapping_data[3],
                    style=cli_style.edit_text,
                    jans_help=_("Services supported by this mapping")
                    )

            def add_mapping(dialog: Dialog) -> None:

                cur_widegt_data = [
                    directory_widget.me.text,
                    type_widget.me.text,
                    ' '.join(services_widget.me.current_values)
                    ]

                cur_mapping_data = {
                    'directory': directory_widget.me.text,
                    'type': type_widget.me.text.split(),
                    'description': description_widget.me.text,
                    'jansServiceModule': services_widget.me.current_values
                    }

                if not kwargs.get('data'):
                    self.asset_dir_mappings_container.add_item(cur_widegt_data)
                    self.asset_dir_mappings_container.all_data.append(cur_mapping_data)
                else:
                    self.asset_dir_mappings_container.replace_item(kwargs['selected'], cur_widegt_data)
                    self.asset_dir_mappings_container.all_data[kwargs['selected']] = cur_mapping_data

            body = HSplit([directory_widget, type_widget, description_widget, services_widget])
            buttons = [Button(_("Cancel")), Button(_("OK"), handler=add_mapping)]
            dialog = JansGDialog(self.app, title=title, body=body, buttons=buttons, width=self.app.dialog_width-20)
            self.app.show_jans_dialog(dialog)


        def delete_asset_mapping(**kwargs: Any) -> None:
            def do_delete_mapping(result):
                self.asset_dir_mappings_container.remove_item(kwargs['selected'])
                del self.asset_dir_mappings_container.all_data[kwargs['selected_idx']]

            dialog = self.app.get_confirm_dialog(HTML(_("Are you sure want to delete mapping <b>{}</b>?".format(kwargs['selected'][0]))), confirm_handler=do_delete_mapping)
            self.app.show_jans_dialog(dialog)

        add_asset_dir_mapping_properties_title = _("Mapping Properties: ")

        asset_dir_mappings = copy.deepcopy(self.data.get('assetMgtConfiguration', {}).get('assetDirMapping', []))
        asset_dir_mapping_data = []
        for mapping in asset_dir_mappings:
            asset_dir_mapping_data.append((
                        mapping['directory'],
                        ' '.join(mapping['type']),
                        ' '.join(mapping['jansServiceModule'])
                        ))

        self.asset_dir_mappings_container = JansVerticalNav(
                myparent=self.app,
                headers=['Directory', 'Type', 'Services'],
                preferred_size=[35, 30, common_data.app.output.get_size().columns -70],
                data=asset_dir_mapping_data,
                on_enter=edit_asset_dir_mapping,
                on_delete=delete_asset_mapping,
                on_display=self.app.data_display_dialog,
                #get_help=(self.get_help,'Properties'),
                selectes=0,
                all_data=asset_dir_mappings,
                underline_headings=False,
                max_width=110,
                jans_name='assetDirMapping',
                max_height=len(asset_dir_mappings)+1
                )

        self.tabs['assetMgtConfiguration'] = HSplit([
            common_data.app.getTitledCheckBox(
                    _("Enable"),
                    name='assetMgtEnabled',
                    checked=self.data.get('assetMgtConfiguration', {}).get('assetMgtEnabled'),
                    style=cli_style.check_box,
                    widget_style=cli_style.black_bg_widget
                ),
            common_data.app.getTitledCheckBox(
                    _("Enable Server Upload"),
                    name='assetServerUploadEnabled',
                    checked=self.data.get('assetMgtConfiguration', {}).get('assetServerUploadEnabled'),
                    style=cli_style.check_box,
                    widget_style=cli_style.black_bg_widget
                ),
            common_data.app.getTitledCheckBox(
                    _("Enable Extension Validation"),
                    name='fileExtensionValidationEnabled',
                    checked=self.data.get('assetMgtConfiguration', {}).get('fileExtensionValidationEnabled'),
                    style=cli_style.check_box,
                    widget_style=cli_style.black_bg_widget
                ),
            common_data.app.getTitledCheckBox(
                    _("Enable Name Validation"),
                    name='moduleNameValidationEnabled',
                    checked=self.data.get('assetMgtConfiguration', {}).get('moduleNameValidationEnabled'),
                    style=cli_style.check_box,
                    widget_style=cli_style.black_bg_widget
                ),

            Frame(
                title=_("Directory Mappings"),
                body=HSplit([
                    self.asset_dir_mappings_container,
                    common_data.app.getButtonWithHandler(text=_("Add Mapping"), handler=edit_asset_dir_mapping, centered=True)
                ]),
            ),
            Window(height=D()),
            ],
            width=D()
            )

        self.plugins_list_box = JansVerticalNav(
                myparent=self.app,
                headers=['Name', 'Description'],
                preferred_size= [40, 60],
                data=[],
                on_enter=self.edit_plugin_dialog,
                on_display=self.app.data_display_dialog,
                on_delete=self.delete_plugin,
                selectes=0,
                headerColor=cli_style.navbar_headcolor,
                entriesColor=cli_style.navbar_entriescolor,
                all_data=copy.deepcopy(self.data['plugins']),
            )


        self.tabs['plugins'] = HSplit([
                    self.plugins_list_box,
                    Window(height=D()),
                ],
                 width=D()
             )

        self.populate_plugins_listbox()

        self.nav_selection_changed(list(self.tabs)[0])

    def populate_plugins_listbox(self):
        self.plugins_list_box.clear()
        for plugin in self.data['plugins']:
            self.plugins_list_box.add_item((plugin['name'], plugin['description']))
        self.plugins_list_box.all_data=copy.deepcopy(self.data['plugins'])


    def edit_plugin_dialog(self, **kwargs: Any) -> None:
        """Method to display the edit plugin dialog
        """

        if kwargs:
            data = kwargs.get('data', {})
        else:
            data = {}

        title = _("Edit Plugin") if data else _("Add Plugin")

        def save_plugin(dialog):
            new_data = self.make_data_from_dialog(tabs={'plugin': dialog.body})
            nav_data = (new_data['name'], new_data['description'])
            if kwargs.get('data'):
                self.plugins_list_box.replace_item(kwargs['selected'], nav_data)
                self.plugins_list_box.all_data[kwargs['selected']] = new_data
            else:
                self.plugins_list_box.add_item(nav_data)
                self.plugins_list_box.all_data.append(new_data)

            dialog.future.set_result(True)

        body = HSplit([

                self.app.getTitledText(
                    title=_("Name"),
                    name='name',
                    value=data.get('name',''),
                    style=cli_style.edit_text_required
                ),

                self.app.getTitledText(
                    title=_("Description"),
                    name='description',
                    value=data.get('description',''),
                    style=cli_style.edit_text_required
                ),

                self.app.getTitledText(
                    title=_("Class Name"),
                    name='className',
                    value=data.get('className',''),
                    style=cli_style.edit_text_required
                ),

            ])

        save_button = Button(_("OK"), handler=save_plugin)
        save_button.keep_dialog = True
        canncel_button = Button(_("Cancel"))
        buttons = [save_button, canncel_button]
        dialog = JansGDialog(self.app, title=title, body=body, buttons=buttons)
        self.app.show_jans_dialog(dialog)


    def delete_plugin(self, **kwargs: Any) -> None:
        """This method for the deleting plugin
        """
        def do_delete_plugin(result):
            self.plugins_list_box.remove_item(kwargs['selected'])
            del self.plugins_list_box.all_data[kwargs['selected_idx']]

        dialog = self.app.get_confirm_dialog(HTML(_("Are you sure want to delete Plugin <b>{}</b>?").format(kwargs['selected'][0])), confirm_handler=do_delete_plugin)
        self.app.show_jans_dialog(dialog)

    async def get_configuration(self) -> None:
        if self.data:
            return

        'Coroutine for getting Janssen Config API configuration.'
        try:
            response = self.app.cli_object.process_command_by_id(
                        operation_id='get-config-api-properties',
                        url_suffix='',
                        endpoint_args='',
                        data_fn=None,
                        data={}
                        )
        except Exception as e:
            self.app.show_message(_("Error getting Config API configuration"), str(e), tobefocused=self.app.center_container)
            self.app.disable_plugin(self.pid)
            return

        if response.status_code not in (200, 201):
            self.app.show_message(_("Error getting Janssen Config API configuration"), str(response.text), tobefocused=self.app.center_container)
            self.app.disable_plugin(self.pid)
            return

        self.data = response.json()
        self.create_widgets()

    def on_page_enter(self):
        if not self.page_entered:
            self.feed_attribute_widgets()
            self.page_entered = True

    def get_attr_displayname_from_name(self, name):
        for attribute in common_data.jans_attributes:
            if name == attribute['name']:
                return attribute.get('displayName')
        return name


    def feed_attribute_widgets(self) -> None:
        data = []
        for attribute in common_data.jans_attributes:
            aname = attribute['name']
            dname = attribute.get('displayName') or aname
            data.append((aname, dname))

        for data_set, widget in (
                            (self.data.get('userExclusionAttributes', []), self.user_exclusion_attributes_widget),
                            (self.data.get('userMandatoryAttributes', []), self.user_mandatory_attributes_widget),
                            ):

            for name in data_set:
                widget.container.remove_label('test')
                widget.container.add_label(name, self.get_attr_displayname_from_name(name))
            widget.data = copy.deepcopy(data)

    def prepare_navbar(self) -> None:
        """prepare the navbar for the current Plugin
        """
        self.nav_bar = JansNavBar(
                    self.app,
                    entries=[
                            ('main_', '[M]ain'),
                            ('agamaConfiguration', 'A[g]ama'),
                            ('plugins', 'P[l]ugins'),
                            ('assetMgtConfiguration', 'Asse[t] Management'),
                            ('auditLogConf', 'Audit Log Conf'),
                            ('dataFormatConversionConf', 'Data Format Conversion'),
                            ],
                    selection_changed=self.nav_selection_changed,
                    select=0,
                    jans_name='api:nav_bar'
                    )

    def prepare_containers(self) -> None:
        """prepare the main container (tabs) for the current Plugin 
        """

        self.tabs = OrderedDict()
        self.main_area = HSplit([Label("configuration")],width=D())

        self.main_container = HSplit([
                                        Box(self.nav_bar.nav_window, style='class:sub-navbar', height=1),
                                        DynamicContainer(lambda: self.main_area),
                                        VSplit([Button(_("Save"), handler=self.save_config)], align=HorizontalAlign.CENTER),
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

        def get_cur_data_patch_path(items):
            items_ = list(items)
            while None in items_:
                items_.remove(None)

            cur_data = self.data[items_[0]]
            for pprop in items_[1:]:
                cur_data = cur_data[pprop]
            return cur_data, '/'.join(items_)


        changes = []
        new_data = self.make_data_from_dialog(tabs={'main_': self.tabs['main_']})

        replace = 'replace'
        add = 'add'

        for prop in new_data:
            if new_data[prop] != self.data.get(prop):
                how = replace if prop in self.data else add
                changes.append((how, prop, new_data[prop]))


        for ppath, widget in (
                (('userExclusionAttributes',), self.user_exclusion_attributes_widget),
                (('userMandatoryAttributes',), self.user_mandatory_attributes_widget),
                (('acrExclusionList',), self.acr_exclusion_widget),
                (('agamaConfiguration', 'mandatoryAttributes'), self.agama_configuration_mandatory_attributes_widget),
                (('agamaConfiguration', 'optionalAttributes'), self.agama_configuration_optional_attributes_widget),
                (('auditLogConf', 'headerAttributes'), self.audit_log_configuration_header_attributes_widget),
                ):
            attributes = widget.get_values()
            cur_data_, path_ =  get_cur_data_patch_path(ppath)
            if attributes != cur_data_:
                changes.append(('replace', path_, attributes))


        for ctab in (
                'auditLogConf',
                'dataFormatConversionConf',
                'assetMgtConfiguration',
                'plugins',
                ):
            data_ = self.make_data_from_dialog(tabs={ctab: self.tabs[ctab]})
            for prop in data_:
                if data_[prop] != self.data[ctab].get(prop):
                    how = replace if prop in self.data[ctab] else add
                    changes.append((how, '/'.join((ctab, prop)), data_[prop]))


        if self.plugins_list_box.all_data != self.data['plugins']:
            changes.append((replace, 'plugins', self.plugins_list_box.all_data))

        if self.asset_dir_mappings_container.all_data != self.data['assetMgtConfiguration']['assetDirMapping']:
            changes.append((replace, 'assetMgtConfiguration/assetDirMapping', self.asset_dir_mappings_container.all_data))


        patches = [{'op': how, 'path': path, 'value': val} for how, path, val in changes ]

        async def coroutine():
            cli_args = {'operation_id': 'patch-config-api-properties', 'data': patches}
            self.app.start_progressing(_("Saving Config API Configuration changes..."))
            response = await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing()
            if response.status_code == 200:
                self.app.show_message(_(common_strings.info), _("Config API Configuration changes were saved."), tobefocused=self.main_container)
                self.data = response.json()
            else:
                self.app.show_message(_('Error Saving Config API Configuration changes'), response.text + '\n' + response.reason, tobefocused=self.main_container)

        if patches:
            asyncio.ensure_future(coroutine())
        else:
            self.app.show_message(_(common_strings.info), _("No changes were done to save Config API Configuration."), tobefocused=self.main_container)

