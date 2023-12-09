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

class EditTRDialog(JansGDialog, DialogUtils):
    """This user editing SAML Trust Relationship dialog
    """
    def __init__(
            self,
            app,
            myparent,
            data:dict,
            )-> Dialog:
        """init for `EditTRDialog`, inherits from two diffrent classes `JansGDialog` and `DialogUtils`
            
        JansGDialog (dialog): This is the main dialog Class Widget for all Jans-cli-tui dialogs except custom dialogs like dialogs with navbar
        DialogUtils (methods): Responsable for all `make data from dialog` and `check required fields` in the form for any Edit or Add New

        Args:
            app (widget): the application object
            data (dict): data for TR
        """

        if data:
            title = _("Edit Trust Relationship")
        else:
            data = {}
            title = _("Add new Trust Relationship")

        super().__init__(app, title)
        self.app = app
        self.data = data
        self.myparent = myparent
        self.metadata_file_path = ''
        self.add_attribute_checkbox = CheckboxList(values=[('', '')])
        self.tr_attribute_entries = []
        self.schema = self.app.cli_object.get_schema_from_reference('SAML', '#/components/schemas/SamlAppConfiguration')
        self.new_tr = not self.data.get('dn')

        if not 'releasedAttributes' in self.data:
            self.data['releasedAttributes'] = []

        self.fill_tr_attributes()
        self.create_window()

    def cancel(self) -> None:
        """method to invoked when canceling changes in the dialog (Cancel button is pressed)
        """

        self.future.set_result(DialogResult.CANCEL)


    def create_window(self) -> None:

        self.matadata_type_container = HSplit([],width=D())
        enabled = self.data['enabled'] if 'enabled' in self.data else True


        def read_metadata_file(path):
            self.metadata_file_path = path

        def upload_file():
            file_browser_dialog = jans_file_browser_dialog(self.app, path=self.app.browse_path, browse_type=BrowseType.file, ok_handler=read_metadata_file)
            self.app.show_jans_dialog(file_browser_dialog)

        def get_metadata_location(value):
            if value == 'url':
                self.matadata_type_container = HSplit([
                    self.app.getTitledText(title=_("Metadata URL"),
                        name='metadataURL',
                        value='',
                        style=cli_style.edit_text_required,
                        jans_help=_("URL of metadata"),
                        widget_style=cli_style.white_bg_widget
                    )
                    ], width=D())
            elif value == 'file':
                self.matadata_type_container = HSplit([
                self.app.getTitledWidget(
                        _("Metadata File"),
                        name='jansEntityId',
                        widget=Button(_('Browse'), handler=upload_file),
                        style=cli_style.edit_text_required,
                        other_widgets=VSplit([Window(width=2), Window(FormattedTextControl(lambda: self.metadata_file_path))])
                )
                ], width=D())


        get_metadata_location(self.data.get('metaDataLocation', 'file'))


        add_released_attribute_button = VSplit([Window(), self.app.getButton(
            text=_("Add Released Attribute"),
            name='oauth:logging:save',
            jans_help=_("Add Released Atribute"),
            handler=self.add_released_attribute)
        ])

        self.released_attributes_container = JansLabelContainer(
            title=_('Released Attributes'),
            width=int(self.app.dialog_width*1.1) - 26,
            on_display=self.app.data_display_dialog,
            on_delete=self.delete_released_attribute,
            buttonbox=add_released_attribute_button,
            entries=self.tr_attribute_entries,
        )


        edit_tr_container_widgets = [
                self.app.getTitledText(
                    title=_("Display Name"),
                    name='displayName',
                    value=self.data.get('displayName', ''),
                    style=cli_style.edit_text_required,
                    jans_help=_("Display Name of TR"),
                    widget_style=cli_style.white_bg_widget
                ),

                self.app.getTitledCheckBox(
                    _("Enable TR"),
                    name='enabled',
                    checked=enabled,
                    jans_help=_("Is this TR enabled?"),
                    style=cli_style.check_box
                ),

                self.app.getTitledText(
                    title=_("Description"),
                    name='description',
                    value=self.data.get('description', ''),
                    height=3,
                    style=cli_style.edit_text_required,
                    jans_help=_("Description for TR"),
                    widget_style=cli_style.white_bg_widget
                ),

                ]

        if self.new_tr:
            edit_tr_container_widgets.append(
                self.app.getTitledWidget(
                        _("Metadata Location"),
                        name='metaDataLocation',
                        widget=DropDownWidget(
                            values=[('file', 'File'), ('url', 'URL')],
                            value=self.data.get('metaDataLocation', 'file'),
                            select_one_option = False,
                            on_value_changed = get_metadata_location
                        )
                )
            )
            edit_tr_container_widgets.append(DynamicContainer(lambda: self.matadata_type_container))

        edit_tr_container_widgets.append(self.released_attributes_container)

        self.edit_tr_container = HSplit(edit_tr_container_widgets)

        self.dialog = JansDialogWithNav(
            title=self.title,
            content=DynamicContainer(lambda: self.edit_tr_container),
            button_functions=[(self.cancel, _("Cancel")), (self.save, _("Save"))],
            height=self.app.dialog_height,
            width=self.app.dialog_width,
            )


    def add_claim(self) -> None:
        """This method for adding new claim
        """
        cur_claims = []
        for w in self.edit_user_content:
            if hasattr(w, 'me'):
                cur_claims.append(w.me.window.jans_name)

        claims_list = []
        for claim in common_data.users.claims:
            if not claim['oxMultiValuedAttribute'] and claim['name'] in cur_claims:
                continue
            if claim['name'] in ('memberOf', 'userPassword', 'uid', 'jansStatus', 'jansActive', 'updatedAt'):
                continue
            claims_list.append((claim['name'], claim['displayName']))

        claims_checkbox = CheckboxList(values=claims_list)

        def add_claim(dialog) -> None:
            for claim_ in claims_checkbox.current_values:
                for claim_prop in common_data.users.claims:
                    if claim_prop['name'] == claim_:
                        break
                display_name = claim_prop['displayName']
                if claim_prop['dataType'] == 'boolean':
                    widget = self.app.getTitledCheckBox(_(display_name), name=claim_, style='class:script-checkbox', jans_help=self.app.get_help_from_schema(self.schema, claim_))
                else:
                    widget = self.app.getTitledText(_(display_name), name=claim_, value='', style='class:script-titledtext', jans_help=self.app.get_help_from_schema(self.schema, claim_))
                self.edit_user_content.insert(-1, widget)
            self.edit_user_container = ScrollablePane(content=HSplit(self.edit_user_content, width=D()),show_scrollbar=False)


        body = HSplit([Label(_("Select claim to be added to current user.")), claims_checkbox])
        buttons = [Button(_("Cancel")), Button(_("OK"), handler=add_claim)]
        dialog = JansGDialog(self.app, title=_("Claims"), body=body, buttons=buttons, width=self.app.dialog_width-20)
        self.app.show_jans_dialog(dialog)


    def get_attribute_by_inum(self, inum: str) -> dict:
        for attribute in common_data.scopes:
            if attribute['inum'] == inum or attribute['dn'] == inum:
                return attribute
        return {}

    def fill_tr_attributes(self):
        for attribute_dn in self.data['releasedAttributes']:
            attribute = self.get_attribute_by_inum(attribute_dn)
            if attribute:
                label = attribute['id']
                if [attribute_dn, label] not in self.tr_attribute_entries:
                    self.tr_attribute_entries.append([attribute_dn, label])
                    if hasattr(self, 'released_attributes_container'):
                        if not [attribute_dn, label] in self.released_attributes_container.entries:
                            self.released_attributes_container.add_label(attribute_dn, label)


    def add_released_attribute(self) -> None:


        def attribute_exists(attribute_dn: str) -> bool:
            for item_id, item_label in self.released_attributes_container.entries:
                if item_id == attribute_dn:
                    return True
            return False

        def add_selected_attributes(dialog):
            self.data['releasedAttributes'] += self.add_attribute_checkbox.current_values
            self.fill_tr_attributes()

        attribute_list = []
        for attribute in common_data.scopes:
            if not attribute_exists(attribute['dn']):
                attribute_list.append(
                    (attribute['dn'], attribute.get('id', '') or attribute['inum']))

            attribute_list.sort(key=lambda x: x[1])

        def on_text_changed(event):
            search_text = event.text
            matching_items = []
            for item in attribute_list:
                if search_text in item[1]:
                    matching_items.append(item)
            if matching_items:
                self.add_attribute_checkbox.values = matching_items
                self.add_attribute_frame.body = HSplit(children=[self.add_attribute_checkbox])
                self.add_attribute_checkbox._selected_index = 0
            else:
                self.add_attribute_frame.body = HSplit(children=[Label(text=_("No Items "), style=cli_style.label,
                                                                   width=len(_("No Items "))),], width=D())

        ta = TextArea(
            height=D(),
            width=D(),
            multiline=False,
        )

        ta.buffer.on_text_changed += on_text_changed

        self.add_attribute_checkbox.values = attribute_list
        self.add_attribute_frame = Frame(
            title="Checkbox list",
            body=HSplit(children=[self.add_attribute_checkbox]),
        )
        layout = HSplit(children=[
            VSplit(
                children=[
                    Label(text=_("Filter "), style=cli_style.label,
                          width=len(_("Filter "))),
                    ta
                ]),
            Window(height=2, char=' '),
            self.add_attribute_frame

        ])

        buttons = [Button(_("Cancel")), Button(
            _("OK"), handler=add_selected_attributes)]

        self.addAttributeDialog = JansGDialog(
            self.app,
            title=_("Select Attributes to add"),
            body=layout,
            buttons=buttons)

        self.app.show_jans_dialog(self.addAttributeDialog)

    def delete_released_attribute(self, attribute: list) -> None:

        def do_delete_attribute(dialog):
            self.released_attributes_container.remove_label(attribute[0])

        dialog = self.app.get_confirm_dialog(
            message=_(
                "Are you sure want to delete attribute:\n {} ?".format(attribute[1])),
            confirm_handler=do_delete_attribute
        )

        self.app.show_jans_dialog(dialog)



    def save(self):

        tr_data = self.make_data_from_dialog({'tr': self.edit_tr_container})

        if self.new_tr:
            metadata_location = tr_data.pop('metaDataLocation', None)
            if metadata_location == 'url':
                metadata_url_data = self.make_data_from_dialog({'metadataURL': self.matadata_type_container})
                metadata_url = metadata_url_data['metadataURL']

                try:
                    metadata_response = requests.get(metadata_url, verify=False)
                except Exception as e:
                    self.app.show_message(_(common_strings.error), _("Retreiving meatada from:\n {}\nfailed. Reason:\n{}").format(metadata_url, e), tobefocused=self.edit_tr_container)
                    return

                if not metadata_response.ok:
                    self.app.show_message(_(common_strings.error), _("Retreiving meatada from:\n {}\nfailed. Reason:\n{}").format(metadata_url, metadata_response.reason), tobefocused=self.edit_tr_container)
                    return

                parsed_url = urlparse(metadata_url)
                metadata_fn = parsed_url.netloc + '_' + parsed_url.path.replace('/', '_')
                if not metadata_fn.endswith('xml'):
                    metadata_fn += '.xml'

                self.metadata_file_path = os.path.join(self.app.cli_object.tmp_dir, metadata_fn)
                with open(self.metadata_file_path, 'w') as w:
                    w.write(metadata_response.text)
            else:
                if not self.metadata_file_path:
                    self.app.show_message(_(common_strings.error), _("Please locate metadata file."), tobefocused=self.edit_tr_container)
                    return

        tr_data['spMetaDataSourceType'] = 'FILE'

        tr_data['releasedAttributes'] = [entry[0] for entry in self.released_attributes_container.entries]



        if self.new_tr:
            tr_data['spMetaDataFN'] = os.path.basename(self.metadata_file_path)
            data = {'trustRelationship': tr_data, 'metaDataFile': self.metadata_file_path}
        else:
            new_data = copy.deepcopy(self.data)
            new_data.update(tr_data)
            data = {'trustRelationship': new_data}

        async def coroutine():
            operation_id = 'post-trust-relationship-metadata-file' if self.new_tr else 'put-trust-relationship'
            cli_args = {'operation_id': operation_id, 'data': data}
            self.app.start_progressing()
            response = await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)

            if response.status_code in (200, 201):
                self.data = response.json()
                self.app.stop_progressing(_("Trust Relationship was saved."))
                self.future.set_result(DialogResult.ACCEPT)
                await self.myparent.get_trust_relations()

            else:
                self.app.show_message(_(common_strings.error), _("Save failed: Status {} - {}\n").format(response.status_code, response.text), tobefocused=self.edit_tr_container)
                self.app.stop_progressing(_("Failed to save Trust Relationship."))


        asyncio.ensure_future(coroutine())


    def __pt_container__(self)-> Dialog:
        """The container for the dialog itself

        Returns:
            Dialog: The Edit User Dialog
        """

        return self.dialog

