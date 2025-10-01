import os
import asyncio
import requests
import copy

from typing import Optional, Sequence, Callable
from urllib.parse import urlparse
from functools import partial
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.layout.containers import HSplit, VSplit, DynamicContainer,\
    Window, FormattedTextControl

from prompt_toolkit.widgets import Button, Label, CheckboxList, Dialog, TextArea,\
    Frame
from prompt_toolkit.eventloop import get_event_loop
from utils.static import DialogResult
from wui_components.jans_dialog_with_nav import JansDialogWithNav
from wui_components.jans_label_container import JansLabelContainer
from wui_components.jans_drop_down import DropDownWidget
from wui_components.jans_cli_dialog import JansGDialog
from wui_components.jans_path_browser import jans_file_browser_dialog, BrowseType

from utils.utils import DialogUtils, common_data
from wui_components.jans_vetrical_nav import JansVerticalNav
from prompt_toolkit.formatted_text import AnyFormattedText
from typing import Any, Optional
from prompt_toolkit.layout import ScrollablePane
from utils.multi_lang import _
from utils.static import cli_style, common_strings
from utils.utils import common_data

class EditIdentityProvideDialog(JansGDialog, DialogUtils):
    """This dialog if for editing/creating SAML Identity Provider
    """
    def __init__(
            self,
            myparent,
            data:dict,
            )-> Dialog:
        """init for `EditIdentityProvideDialog`, inherits from two diffrent classes `JansGDialog` and `DialogUtils`
            
        JansGDialog (dialog): This is the main dialog Class Widget for all Jans-cli-tui dialogs except custom dialogs like dialogs with navbar
        DialogUtils (methods): Responsable for all `make data from dialog` and `check required fields` in the form for any Edit or Add New

        Args:
            myparent (class): object which calls this dialog
            data (dict): data for TR
        """

        if data:
            title = _("Edit Identity Provider")
        else:
            data = {}
            title = _("Add new Identity Provider")

        super().__init__(parent=common_data.app, title=title)
        self.data = data
        self.myparent = myparent
        self.metadata_file_path = None
        self.schema = common_data.app.cli_object.get_schema_from_reference('SAML', '#/components/schemas/IdentityProvider')
        self.new_provider = not self.data.get('dn')

        self.create_window()

    def cancel(self) -> None:
        """method to invoked when canceling changes in the dialog (Cancel button is pressed)
        """

        self.future.set_result(DialogResult.CANCEL)


    def create_window(self) -> None:

        enabled = self.data['enabled'] if 'enabled' in self.data else True

        def read_metadata_file(path):
            self.metadata_file_path = path

        def upload_file():
            file_browser_dialog = jans_file_browser_dialog(common_data.app, path=common_data.app.browse_path, browse_type=BrowseType.file, ok_handler=read_metadata_file)
            common_data.app.show_jans_dialog(file_browser_dialog)

        def get_metadata_container(value):
            if value == 'file':
                self.matadata_container = HSplit([
                common_data.app.getTitledWidget(
                        _("Metadata File"),
                        name='jansEntityId',
                        widget=Button(_('Browse'), handler=upload_file),
                        style=cli_style.edit_text,
                        other_widgets=VSplit([Window(width=2), Window(FormattedTextControl(lambda: self.metadata_file_path))])
                )
                ], width=D())

            else:
                self.metadata_file_path = None
                self.matadata_container = HSplit([

                    common_data.app.getTitledText(
                        title=_("Entity ID"),
                        name='idpEntityId',
                        value=self.data.get('idpEntityId', ''),
                        style=cli_style.edit_text_required,
                        jans_help=_("Entity ID for Identity Provider"),
                        widget_style=cli_style.white_bg_widget
                    ),

                    common_data.app.getTitledText(
                        title=_("NameID Policy Format "),
                        name='nameIDPolicyFormat',
                        value=self.data.get('nameIDPolicyFormat', ''),
                        style=cli_style.edit_text_required,
                        jans_help=_("Policy Format for Identity Provider"),
                        widget_style=cli_style.white_bg_widget
                    ),

                    common_data.app.getTitledText(
                        title=_("Single Sign on Service Endpoint"),
                        name='singleSignOnServiceUrl',
                        value=self.data.get('singleSignOnServiceUrl', ''),
                        style=cli_style.edit_text_required,
                        jans_help=_("Endpoint for Single Sign on Service"),
                        widget_style=cli_style.white_bg_widget
                    ),

                    common_data.app.getTitledText(
                        title=_("Single Logout Service Endpoint"),
                        name='singleLogoutServiceUrl',
                        value=self.data.get('singleLogoutServiceUrl', ''),
                        style=cli_style.edit_text,
                        jans_help=_("Endpoint for Single Logout Service"),
                        widget_style=cli_style.white_bg_widget
                    ),

                    common_data.app.getTitledText(
                        title=_("Signing Certificate"),
                        name='signingCertificate',
                        value=self.data.get('signingCertificate', ''),
                        height=3,
                        style=cli_style.edit_text,
                        jans_help=_("Signing Certificates"),
                        widget_style=cli_style.white_bg_widget
                    ),

                    common_data.app.getTitledText(
                        title=_("Encryption Certificate"),
                        name='encryptionPublicKey',
                        value=self.data.get('encryptionPublicKey', ''),
                        height=3,
                        style=cli_style.edit_text,
                        jans_help=_("Public key for Encryption"),
                        widget_style=cli_style.white_bg_widget
                    ),

                    common_data.app.getTitledText(
                        title=_("Principal Attribute"),
                        name='principalAttribute',
                        value=self.data.get('principalAttribute', ''),
                        style=cli_style.edit_text,
                        jans_help=_("Principal Attribute"),
                        widget_style=cli_style.white_bg_widget
                    ),

                    common_data.app.getTitledText(
                        title=_("Principal Type"),
                        name='principalType',
                        value=self.data.get('principalType', ''),
                        style=cli_style.edit_text,
                        jans_help=_("Principal Type"),
                        widget_style=cli_style.white_bg_widget
                    )

                    ],
                    width=D()
                    )

        get_metadata_container('manual')

        edit_provider_container_widgets = [
                common_data.app.getTitledText(
                    title=_("Name"),
                    name='name',
                    value=self.data.get('name', ''),
                    style=cli_style.edit_text_required,
                    jans_help=_("Name for Identity Provider"),
                    widget_style=cli_style.white_bg_widget,
                    read_only = not self.new_provider
                ),

                common_data.app.getTitledCheckBox(
                    _("Enable Provider"),
                    name='enabled',
                    checked=enabled,
                    jans_help=_("Is this Provider enabled?"),
                    style=cli_style.check_box
                ),

                common_data.app.getTitledText(
                    title=_("Display Name"),
                    name='displayName',
                    value=self.data.get('displayName', ''),
                    style=cli_style.edit_text_required,
                    jans_help=_("Display Name for Identity Provider"),
                    widget_style=cli_style.white_bg_widget
                ),

                common_data.app.getTitledText(
                    title=_("Description"),
                    name='description',
                    value=self.data.get('description', ''),
                    height=3,
                    style=cli_style.edit_text,
                    jans_help=_("Description for Identity Provider"),
                    widget_style=cli_style.white_bg_widget
                ),

                common_data.app.getTitledWidget(
                    _("Metadata Source Type"),
                    name='idpMetaDataSourceType',
                    widget=DropDownWidget(
                        values=[(dsp, dsp) for dsp in ('file', 'manual')],
                        value='manual',
                        select_one_option = False,
                        on_value_changed = get_metadata_container
                    )
                ),

                DynamicContainer(lambda: self.matadata_container),

                ]

        self.edit_provider_container = HSplit(edit_provider_container_widgets)

        self.dialog = JansDialogWithNav(
            title=self.title,
            content=DynamicContainer(lambda: self.edit_provider_container),
            button_functions=[(self.cancel, _("Cancel")), (self.save, _("Save"))],
            height=common_data.app.dialog_height,
            width=common_data.app.dialog_width,
            )


    def save(self):

        new_data = self.make_data_from_dialog({'provider': self.edit_provider_container})

        provider_data = copy.deepcopy(self.data)
        provider_data.update(new_data)
        provider_data['realm'] = 'jans'
        import_metadata_from_file = provider_data.pop('idpMetaDataSourceType', None) == 'file'

        if import_metadata_from_file and not self.metadata_file_path:
            common_data.app.show_message(_(common_strings.error), _("Please browse metadata file."), tobefocused=self.edit_provider_container)
            return

        if import_metadata_from_file:
            data = {'identityProvider': provider_data, 'metaDataFile': self.metadata_file_path}
        else:
            metadata_data = self.make_data_from_dialog({'metadata_data': self.matadata_container})
            provider_data.update(metadata_data)

            cfr = self.check_required_fields(data=provider_data, container=self.edit_provider_container, tobefocused=self.edit_provider_container)
            if not cfr:
                return

            data = {'identityProvider': provider_data}

        async def coroutine():
            operation_id = 'post-saml-identity-provider' if self.new_provider else 'put-saml-identity-provider'
            cli_args = {'operation_id': operation_id, 'data': data}
            common_data.app.start_progressing()
            response = await common_data.app.loop.run_in_executor(common_data.app.executor, common_data.app.cli_requests, cli_args)

            if response.status_code in (200, 201):
                self.data = response.json()
                common_data.app.stop_progressing(_("IDP Provider was saved."))
                self.future.set_result(DialogResult.ACCEPT)
                await self.myparent.get_identity_providers()

            else:
                common_data.app.show_message(_(common_strings.error), _("Save failed: Status {} - {}\n").format(response.status_code, response.text), tobefocused=self.edit_provider_container)
                common_data.app.stop_progressing(_("Failed to save IDP Provider"))

        asyncio.ensure_future(coroutine())


    def __pt_container__(self)-> Dialog:
        """The container for the dialog itself

        Returns:
            Dialog: The Edit User Dialog
        """

        return self.dialog

