import copy
import asyncio
import urllib.request
from collections import OrderedDict
from typing import Any, Optional

import prompt_toolkit
from prompt_toolkit.layout.containers import HSplit, DynamicContainer,\
    VSplit, Window, HorizontalAlign, ConditionalContainer, FormattedTextControl
from prompt_toolkit.filters import Condition
from prompt_toolkit.formatted_text import HTML
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.widgets import Button, Label, Box, Dialog
from prompt_toolkit.application import Application
from wui_components.jans_nav_bar import JansNavBar
from wui_components.jans_drop_down import DropDownWidget
from wui_components.jans_vetrical_nav import JansVerticalNav
from wui_components.jans_cli_dialog import JansGDialog
from wui_components.jans_spinner import Spinner
from wui_components.jans_path_browser import jans_file_browser_dialog, BrowseType
from wui_components.jans_label_container import JansLabelContainer

from edit_tr_dialog import EditTRDialog
from edit_identity_provider_dialog import EditIdentityProvideDialog
from utils.multi_lang import _
from utils.utils import DialogUtils
from utils.static import cli_style, common_strings

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
        self.pid = 'saml'
        self.name = 'Ja[n]s SAML'
        self.server_side_plugin = True
        self.page_entered = False

        self.trust_realtionships = []
        self.identity_providers = []
        self.config = {}
        self.prepare_navbar()
        self.prepare_containers()


    def init_plugin(self) -> None:
        """The initialization for this plugin
        """

        self.app.create_background_task(self.get_configuration())
        self.app.create_background_task(self.get_trust_relations())
        self.app.create_background_task(self.get_identity_providers())

    def set_center_frame(self) -> None:
        """center frame content
        """
        self.app.center_container = self.main_container

    def create_widgets(self):
        self.schema = self.app.cli_object.get_schema_from_reference('SAML', '#/components/schemas/SamlAppConfiguration')

        self.tr_container = JansVerticalNav(
                myparent=self.app,
                headers=['inum', _("Display Name"), _("Enabled")],
                preferred_size= self.app.get_column_sizes(.3, .5, .2),
                data=[],
                on_enter=self.edit_tr,
                on_display=self.app.data_display_dialog,
                on_delete=self.delete_tr,
                selectes=0,
                headerColor=cli_style.navbar_headcolor,
                entriesColor=cli_style.navbar_entriescolor,
                all_data=[],
                hide_headers=False
            )

        self.tabs['configuration'] = HSplit([
                        self.app.getTitledCheckBox(
                            _("Enable SAML"),
                            name='enabled',
                            checked=self.config.get('enabled'),
                            jans_help=self.app.get_help_from_schema(self.schema, 'enabled'),
                            style=cli_style.check_box,
                            widget_style=cli_style.black_bg_widget
                        ),

                        self.app.getTitledWidget(
                            _("Selected IDP"),
                            name='selectedIdp',
                            widget=DropDownWidget(
                                values=[('keycloak', 'Keycloak')],
                                value=self.config.get('selectedIdp', 'keycloak'),
                                select_one_option = False
                            ),
                            jans_help=self.app.get_help_from_schema(self.schema, 'selectedIdp'),
                            style=cli_style.titled_text
                        ),

                        self.app.getTitledCheckBox(_("Ignore Validation"), name='ignoreValidation', checked=self.config.get('ignoreValidation'), jans_help=self.app.get_help_from_schema(self.schema, 'ignoreValidation'), style=cli_style.check_box, widget_style=cli_style.black_bg_widget),
                        Window(height=1),
                        VSplit([Button(_("Save"), handler=self.save_config)], align=HorizontalAlign.CENTER),
                        ],
                        width=D()
                    )

        add_tr_button_label = _("Add Service Provider")
        self.tabs['trust_relationships'] = HSplit([
                                self.tr_container,
                                Window(height=1),
                                VSplit([Button(add_tr_button_label, handler=self.edit_tr, width=len(add_tr_button_label)+4)], align=HorizontalAlign.CENTER),
                                ],
                                width=D()
                                )

        self.provider_container = JansVerticalNav(
                myparent=self.app,
                headers=['inum', _("Display Name"), _("Enabled")],
                preferred_size= self.app.get_column_sizes(.3, .5, .2),
                data=[],
                on_enter=self.edit_identity_provider,
                on_display=self.app.data_display_dialog,
                on_delete=self.delete_identity_provider,
                selectes=0,
                headerColor=cli_style.navbar_headcolor,
                entriesColor=cli_style.navbar_entriescolor,
                all_data=[],
                hide_headers=False
            )


        add_provider_button_label = _("Add Identity Provider")
        self.tabs['identity_providers'] = HSplit([
                                self.provider_container,
                                Window(height=1),
                                VSplit([Button(add_provider_button_label, handler=self.edit_identity_provider, width=len(add_provider_button_label)+4)], align=HorizontalAlign.CENTER),
                                ],
                                width=D()
                                )

        self.nav_selection_changed(list(self.tabs)[0])

    async def get_configuration(self) -> None:
        'Coroutine for getting Janssen SAML configuration.'
        try:
            response = self.app.cli_object.process_command_by_id(
                        operation_id='get-saml-properties',
                        url_suffix='',
                        endpoint_args='',
                        data_fn=None,
                        data={}
                        )
        except Exception as e:
            self.app.show_message(_("Error getting Janssen SAML configuration"), str(e), tobefocused=self.app.center_container)
            self.app.disable_plugin(self.pid)
            return

        if response.status_code not in (200, 201):
            self.app.show_message(_("Error getting Janssen SAML configuration"), str(response.text), tobefocused=self.app.center_container)
            self.app.disable_plugin(self.pid)
            return

        self.config = response.json()
        self.create_widgets()
        self.update_configuration_container()

    async def get_trust_relations(self) -> None:
        'Coroutine for getting SAML trust relationships.'
        try:
            response = self.app.cli_object.process_command_by_id(
                        operation_id='get-trust-relationships',
                        url_suffix='',
                        endpoint_args='',
                        data_fn=None,
                        data={}
                        )
            self.trust_realtionships = response.json()
            self.update_trust_relationships_container()
        except Exception as e:
            self.app.show_message(_("Error getting Janssen SAML trust relationships"), str(e), tobefocused=self.app.center_container)
            return

        if response.status_code not in (200, 201):
            self.app.show_message(_("Error getting SAML trust relationships"), str(response.text), tobefocused=self.app.center_container)
            self.app.disable_plugin(self.pid)
            return

    async def get_identity_providers(self) -> None:
        'Coroutine for getting identity_providers.'
        try:
            response = self.app.cli_object.process_command_by_id(
                        operation_id='get-saml-identity-provider',
                        url_suffix='',
                        endpoint_args='',
                        data_fn=None,
                        data={}
                        )
            providers = response.json()
            self.identity_providers = providers.get('entries', [])

        except Exception as e:
            self.app.show_message(_("Error getting Identity Providers-1"), str(e), tobefocused=self.app.center_container)
            return

        if response.status_code not in (200, 201):
            self.app.show_message(_("Error getting Identity Providers-2"), str(response.text), tobefocused=self.app.center_container)
            return

        self.update_provider_container()

    def update_trust_relationships_container(self):
        self.tr_container.clear()
        self.tr_container.all_data = self.trust_realtionships
        for tr in self.trust_realtionships:
            self.tr_container.add_item((
                        tr['inum'],
                        tr['displayName'],
                        tr['enabled']
                    ))

    def update_provider_container(self):
        self.provider_container.clear()
        self.provider_container.all_data = self.identity_providers
        for provider in self.identity_providers:
            self.provider_container.add_item((
                        provider['inum'],
                        provider['displayName'],
                        provider['enabled']
                    ))


    def prepare_navbar(self) -> None:
        """prepare the navbar for the current Plugin 
        """
        self.nav_bar = JansNavBar(
                    self.app,
                    entries=[
                            ('configuration', 'C[o]nfiguration'),
                            ('trust_relationships', 'S[e]rvice Providers'),
                            ('identity_providers', 'I[d]entity Providers'),
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

    def update_configuration_container(self):
        for item in self.tabs['configuration'].children:
            if hasattr(item, 'me'):
                me = item.me
                key_ = me.window.jans_name
                if isinstance(me, prompt_toolkit.widgets.base.TextArea):
                    me.text = self.config.get(key_, '')
                elif isinstance(me, prompt_toolkit.widgets.base.Checkbox):
                    me.checked = self.config.get(key_, False)
                elif isinstance(me, DropDownWidget):
                    me.value = self.config.get(key_, '')

    def save_config(self) -> None:
        """This method for saving the configuration
        """

        configuration = self.make_data_from_dialog(tabs={'configuration': self.tabs['configuration']})
        new_config = copy.deepcopy(self.config)
        new_config.update(configuration)

        async def coroutine():
            cli_args = {'operation_id': 'put-saml-properties', 'data': new_config}
            self.app.start_progressing(_("Saving Jans SAML Configuration..."))
            response = await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            if response.status_code == 200:
                self.app.stop_progressing()
                self.app.show_message(title=_(common_strings.success), message=_("Jans SAML configuration was saved."), tobefocused=self.app.center_container)
                self.config = response.json()
                self.update_configuration_container()
            else:
                self.app.show_message(_('Error Saving Jans SAML Config'), response.text + '\n' + response.reason)

        asyncio.ensure_future(coroutine())

    def edit_tr(self, **kwargs: Any) -> None:
        edit_tr_dialog = EditTRDialog(self.app, self, data=kwargs.get('data', {}))
        self.app.show_jans_dialog(edit_tr_dialog)


    def delete_tr(self, **kwargs: Any) -> None:
        """This is method for the deleting trust relationship 
        """

        def do_delete_tr():
            async def coroutine():
                cli_args = {'operation_id': 'delete-trust-relationship', 'url_suffix':'id:{}'.format(kwargs['selected'][0])}
                self.app.start_progressing(_("Deleting trust relationship {}").format(kwargs['selected'][1]))
                response = await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
                self.app.stop_progressing()
                if response:
                    self.app.show_message(_("Error"), _("Deletion was not completed {}".format(response)), tobefocused=self.tr_container)
                await self.get_trust_relations()
                self.update_configuration_container()
            asyncio.ensure_future(coroutine())

        buttons = [Button(_("No")), Button(_("Yes"), handler=do_delete_tr)]

        self.app.show_message(
                title=_("Confirm"),
                message=HTML(_("Are you sure you want to delete trust relatiohship <b>{}</b>?").format(kwargs['selected'][1])),
                buttons=buttons,
                tobefocused=self.tr_container
                )

    def edit_identity_provider(self, **kwargs: Any) -> None:
        """This is method for editing/creating identity provider 
        """
        edit_identity_provider_dialog = EditIdentityProvideDialog(self, data=kwargs.get('data', {}))
        self.app.show_jans_dialog(edit_identity_provider_dialog)


    def delete_identity_provider(self, **kwargs: Any) -> None:
        """This is method for the deleting identity provider 
        """

        def do_delete_provider():
            async def coroutine():
                cli_args = {'operation_id': 'delete-saml-identity-provider', 'url_suffix':'inum:{}'.format(kwargs['selected'][0])}
                
                self.app.start_progressing(_("Deleting identity provider {}").format(kwargs['selected'][1]))
                response = await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
                self.app.stop_progressing()
                if response:
                    self.app.show_message(_("Error"), _("Deletion was not completed {}".format(response)), tobefocused=self.provider_container)
                await self.get_identity_providers()

            asyncio.ensure_future(coroutine())

        buttons = [Button(_("No")), Button(_("Yes"), handler=do_delete_provider)]

        self.app.show_message(
                title=_("Confirm"),
                message=HTML(_("Are you sure you want to delete identity provider <b>{}</b>?").format(kwargs['selected'][1])),
                buttons=buttons,
                tobefocused=self.provider_container
                )
