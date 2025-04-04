import asyncio
from functools import partial
from typing import Optional, Any

from prompt_toolkit.application import Application
from prompt_toolkit.eventloop import get_event_loop
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.layout.containers import HSplit, VSplit, DynamicContainer, HorizontalAlign
from prompt_toolkit.widgets import Button
from prompt_toolkit.buffer import Buffer
from prompt_toolkit.formatted_text import HTML, merge_formatted_text

from utils.multi_lang import _
from utils.utils import common_data
from utils.utils import DialogUtils
from utils.static import cli_style, common_strings
from wui_components.jans_drop_down import DropDownWidget
from wui_components.jans_vetrical_nav import JansVerticalNav
from wui_components.jans_cli_dialog import JansGDialog


class Attributes(DialogUtils):
    def __init__(
        self, 
        app: Application
        ) -> None:

        self.app = self.myparent = app
        self.prev_next_buttons = VSplit([], width=D())
        self.working_container = JansVerticalNav(
                myparent=app,
                headers=[_("Name"), _("Data Type"), _("Status")],
                preferred_size= self.app.get_column_sizes(.5, .3 , .2),
                on_enter=self.edit_attribute,
                on_display=self.app.data_display_dialog,
                on_delete=self.delete_attribute,
                selectes=0,
                headerColor=cli_style.navbar_headcolor,
                entriesColor=cli_style.navbar_entriescolor,
                hide_headers = True
            )

        self.main_container =  HSplit([
                    VSplit([
                        self.app.getTitledText(_("Search"), name='oauth:attributes:search', jans_help=_(common_strings.enter_to_search), accept_handler=self.search_attributes, style=cli_style.edit_text),
                        self.app.getButton(text=_("Add Attribute"), name='oauth:attributes:add', jans_help=_("To add a new attribute press this button"), handler=self.add_attributes),
                        ],
                        padding=3,
                        width=D(),
                    ),
                    self.working_container,
                    DynamicContainer(lambda: self.prev_next_buttons)
                    ], style=cli_style.container)


    def edit_attribute(self, **params: Any) -> None:
        """This method displays the attribute editing dialog

        Args:
            params (dict): arguments passed by Nav Bar or add button
        """
        data = params['data']

        def get_custom_validation_container(cb=None):
            if getattr(cb, 'checked', False) or data.get('attributeValidation'):
                self.custom_validation_container = HSplit([
                                self.app.getTitledText(
                                    title=_("    Regular Expression"),
                                    name='regexp',
                                    value=data.get('attributeValidation',{}).get('regexp', ''),
                                    style=cli_style.edit_text
                                ),
                                self.app.getTitledText(
                                    title=_("    Minimum Length"),
                                    name='minLength',
                                    value=data.get('attributeValidation',{}).get('minLength', 8),
                                    text_type='integer',
                                    style=cli_style.edit_text
                                ),
                                self.app.getTitledText(
                                    title=_("    Maximum Length"),
                                    name='maxLength',
                                    value=data.get('attributeValidation',{}).get('maxLength', 10),
                                    text_type='integer',
                                    style=cli_style.edit_text
                                ),
                                ],
                                width=D()
                            )
            else:
                self.custom_validation_container = HSplit([], width=D())

        get_custom_validation_container()

        body = HSplit([

                self.app.getTitledText(
                    title=_("Name"),
                    name='name',
                    value=data.get('name',''),
                    style=cli_style.edit_text_required
                ),

                self.app.getTitledText(
                    title=_("SAML1 URI"),
                    name='saml1Uri',
                    value=data.get('saml1Uri',''),
                    style=cli_style.edit_text
                ),

                self.app.getTitledText(
                    title=_("SAML2 URI"),
                    name='saml2Uri',
                    value=data.get('saml2Uri',''),
                    style=cli_style.edit_text
                ),

                self.app.getTitledText(
                    title=_("Display Name"),
                    name='displayName',
                    value=data.get('displayName',''),
                    style=cli_style.edit_text_required
                ),

                self.app.getTitledWidget(
                                _('Data Type'),
                                name='dataType',
                                widget=DropDownWidget(
                                    values=[(t,t ) for t in ('string', 'numeric', 'boolean', 'binary', 'certificate', 'generalizedTime', 'json')],
                                    value=data.get('dataType', 'string')
                                    ),
                                style=cli_style.drop_down
                                ),

                self.myparent.getTitledCheckBoxList(
                                _("Edit Type"), 
                                name='editType', 
                                values=[('admin', 'admin'), ('user', 'user')],
                                current_values=data.get('editType', []), 
                                style=cli_style.check_box_list
                                ),

                self.myparent.getTitledCheckBoxList(
                                _("View Type"), 
                                name='viewType', 
                                values=[('admin', 'admin'), ('user', 'user')],
                                current_values=data.get('viewType', []), 
                                style=cli_style.check_box_list
                                ),

                self.app.getTitledCheckBox(
                            _("Multivalued"), 
                            name='oxMultiValuedAttribute', 
                            checked=data.get('oxMultiValuedAttribute', False),
                            style=cli_style.check_box
                            ),

                self.app.getTitledText(
                    title=_("Claim Name"),
                    name='claimName',
                    value=data.get('claimName',''),
                    style=cli_style.edit_text
                ),


                self.app.getTitledCheckBox(
                            _("Include in SCIM Extension"), 
                            name='scimCustomAttr', 
                            checked=data.get('scimCustomAttr', False),
                            style=cli_style.check_box
                            ),

                self.app.getTitledText(
                    title=_("Description"),
                    name='description',
                    value=data.get('description',''),
                    style=cli_style.edit_text
                ),

                self.app.getTitledWidget(
                                _('Status'),
                                name='status',
                                widget=DropDownWidget(
                                    values=[(t,t ) for t in ('active', 'inactive', 'expired', 'register')],
                                    value=data.get('status', 'active')
                                    ),
                                style=cli_style.drop_down
                                ),

                self.app.getTitledText(
                    title=_("URN"),
                    name='urn',
                    value=data.get('urn',''),
                    style=cli_style.edit_text
                ),

                self.app.getTitledCheckBox(
                    _("Enable Custom Validation"),
                    name='enableCustomValidation',
                    checked=bool(data.get('attributeValidation')),
                    on_selection_changed=get_custom_validation_container,
                    jans_help=_("Check this to enable custom validation"),
                    style=cli_style.check_box
                ),

                DynamicContainer(lambda: self.custom_validation_container)

            ])

        save_button = Button(_("Save"), handler=self.save_attribute)
        save_button.keep_dialog = True
        canncel_button = Button(_("Cancel"))
        buttons = [save_button, canncel_button]
        dialog = JansGDialog(self.app, title=_("Edit Attribute"), body=body, buttons=buttons)
        dialog.data = data
        self.app.show_jans_dialog(dialog)

    def add_attributes(self) -> None:
        """Calls edit_attribute() with empty data for adding an attribute."""

        self.edit_attribute(data={})

    def save_attribute(self, dialog: JansGDialog) -> None:
        """Saves attribute

        Args:
            dialog (JansGDialog): dialog object
        """

        new_data = self.make_data_from_dialog(tabs={'attributes': dialog.body})

        for key in dialog.data:
            if key not in new_data:
                new_data[key] = dialog.data[key]

        if 'origin' not in new_data:
            new_data['origin'] = 'jansCustomPerson'

        enable_custom_validation = new_data.pop('enableCustomValidation')
        if enable_custom_validation:
            new_data['attributeValidation'] = self.make_data_from_dialog({'attributeValidation': self.custom_validation_container})
        else:
            new_data.pop('attributeValidation', None)

        if self.check_required_fields(dialog.body, data=new_data):
            dialog.future.set_result(True)

            async def coroutine():
                operation_id = 'put-attributes' if 'inum' in dialog.data  else 'post-attributes'
                cli_args = {'operation_id': operation_id, 'data': new_data}
                self.app.start_progressing(_("Saving Attribute ..."))
                response = await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
                self.app.stop_progressing()

                if response.status_code in (200, 201):
                    self.app.start_progressing(_("Attribute was saved"))
                    self.get_attributes()
                    common_data.claims_retreived = False
                else:
                    self.myparent.show_message(_("A server error ocurred while saving attribute"), str(response.text), tobefocused=self.main_container)
            asyncio.ensure_future(coroutine())


    def update_working_container(self,
            pattern: Optional[str] = '',
            data: Optional[dict] = {}
        ) -> None:
        """This fucntion updates working container

        Args:
            pattern (str, optional): an optional argument for searching attribute. This argument is passed to get_attributes().
            data (dict, optional): the data to be displayed
        """

        self.working_container.clear()

        for attribute in data.get('entries', []):
            self.working_container.add_item((
                        attribute['name'],
                        attribute['dataType'],
                        attribute['status'],
                    ))

        buttons = []
        if data.get('start', 0) > 0:
            handler_partial = partial(self.get_attributes, data['start'] - self.app.entries_per_page, pattern)
            prev_button = Button(_("Prev"), handler=handler_partial)
            prev_button.window.jans_help = _("Retreives previous %d entries") % self.app.entries_per_page
            buttons.append(prev_button)
        if  data.get('start', 0) + self.app.entries_per_page <  data.get('totalEntriesCount', 0):
            handler_partial = partial(self.get_attributes, data['start'] + self.app.entries_per_page, pattern)
            next_button = Button(_("Next"), handler=handler_partial)
            next_button.window.jans_help = _("Retreives next %d entries") % self.app.entries_per_page
            buttons.append(next_button)

        if self.working_container.data:
            self.working_container.hide_headers = False
        else:
            self.working_container.hide_headers = True
            self.app.show_message(_("Oops"), _(common_strings.no_matching_result),tobefocused = self.main_container)

        self.prev_next_buttons = VSplit(buttons, padding=5, align=HorizontalAlign.CENTER)
        self.app.layout.focus(self.working_container)

    def get_attributes(self, 
            start_index: Optional[int]= 0,
            pattern: Optional[str]= '',
        ) -> None:
        """Retireives attrbiutes from server according to pattern

        Args:
            start_index (int, optional): an optional argument for start index
            pattern (str, optional): an optional argument for searching attribute.
        """

        asyncio.ensure_future(self.get_attributes_coroutine(start_index, pattern))



    async def get_attributes_coroutine(self,
            start_index: Optional[int]= 0,
            pattern: Optional[str]= '',
            ) -> None:

            endpoint_args ='limit:{},startIndex:{}'.format(self.app.entries_per_page, start_index)
            if pattern:
                endpoint_args +=',pattern:'+pattern

            cli_args = {'operation_id': 'get-attributes', 'endpoint_args':endpoint_args}
            self.app.start_progressing(_("Retreiving attributes..."))
            response = await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing()
            try:
                data = response.json()
            except Exception:
                self.app.show_message(_(common_strings.error), HTML(_("Server reterned non json data <i>{}</i>").format(response.text)), tobefocused=self.app.center_container)
                return

            if not 'entriesCount' in data:
                self.app.show_message(_(common_strings.error), HTML(_("Server reterned unexpected data <i>{}</i>").format(data)), tobefocused=self.app.center_container)
                return
            self.working_container.all_data = data.get('entries', [])
            self.update_working_container(pattern=pattern, data=data)


    def search_attributes(self, tbuffer:Buffer) -> None:
        """This method handel the search for Attributes

        Args:
            tbuffer (Buffer): Buffer returned from the TextArea widget > GetTitleText
        """
        self.get_attributes(pattern=tbuffer.text)


    def delete_attribute(self, **kwargs: Any) -> None:
        """This method is for deleting attribute.

        Args:
            kwargs (dict): arguments given by on_delete() function of Nav Bar
        """

        selected_idx = kwargs['selected_idx']
        selected_attribute = self.working_container.all_data[selected_idx]

        def do_delete_attribute(dialog):
            async def coroutine():
                cli_args = {'operation_id': 'delete-attributes-by-inum', 'url_suffix':'inum:{}'.format(selected_attribute['inum'])}
                self.app.start_progressing(_("Deleting attribute {}").format(selected_attribute['name']))
                await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
                self.app.stop_progressing()
                self.get_attributes()
                common_data.claims_retreived = False

            asyncio.ensure_future(coroutine())

        used_by = []
        for scope in common_data.scopes:
            if selected_attribute['dn'] in scope.get('claims', []):
                used_by.append(scope['id'])

        confirm_msg = [_("You are about to delete attribute <b>{}</b>.").format(selected_attribute['name'])]
        if used_by:
            confirm_msg.append(_("This user attribute is associated with the following OpenID scopes:\n<b>{}</b>.").format(', '.join(used_by)))

        confirm_msg.append(_("Other scripts or customizations may reference this attribute. <b>REMOVE AT YOUR OWN PERIL!</b>"))
        confirm_msg.append(_("Are you sure want to delete ?"))

        confirm_msg = [HTML(msg+'\n') for msg in confirm_msg]

        confirm_dialog = self.app.get_confirm_dialog(
                merge_formatted_text(confirm_msg),
                confirm_handler=do_delete_attribute
                )

        self.app.show_jans_dialog(confirm_dialog)
