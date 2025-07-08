from typing import Any, Optional, Sequence, Callable

from prompt_toolkit.layout.dimension import D
from prompt_toolkit.layout.containers import HSplit, VSplit,\
    DynamicContainer, Window

from prompt_toolkit.widgets import Button, Label, TextArea, Frame
from prompt_toolkit.widgets import Button, Dialog, CheckboxList

from prompt_toolkit.formatted_text import HTML
from prompt_toolkit.buffer import Buffer
from prompt_toolkit.widgets.base import RadioList
from prompt_toolkit.formatted_text import AnyFormattedText

from utils.multi_lang import _
from utils.static import DialogResult, cli_style
from utils.utils import DialogUtils, common_data
from wui_components.jans_dialog_with_nav import JansDialogWithNav
from wui_components.jans_cli_dialog import JansGDialog
from wui_components.jans_vetrical_nav import JansVerticalNav
from wui_components.jans_label_container import JansLabelContainer
from wui_components.jans_label_widget import JansLabelWidget


class EditScopeDialog(JansGDialog, DialogUtils):
    """The Main Scope Dialog that contain every thing related to The Scope
    """
    def __init__(
        self,
        app,
        title: AnyFormattedText,
        data: list,
        buttons: Optional[Sequence[Button]]= [],
        save_handler: Callable= None,
        )-> Dialog:
        """init for `EditScopeDialog`, inherits from two diffrent classes `JansGDialog` and `DialogUtils`
            
        DialogUtils (methods): Responsable for all `make data from dialog` and `check required fields` in the form for any Edit or Add New
        JansGDialog (dialog): This is the main dialog Class Widget for all Jans-cli-tui dialogs except custom dialogs like dialogs with navbar
        
                Args:
            app (widget): This is the parent application
            title (str): The Main dialog title
            data (list): selected line data 
            button_functions (list, optional): Dialog main buttons with their handlers. Defaults to [].
            save_handler (method, optional): handler invoked when closing the dialog. Defaults to None.
        """
        super().__init__(app, title, buttons)
        self.save_handler = save_handler
        self.data = data
        self.title = title
        self.app = app
        self.claims_container = None
        self.showInConfigurationEndpoint = self.data.get('attributes', {}).get('showInConfigurationEndpoint', '')
        self.defaultScope = self.data.get('defaultScope', '')
        self.schema = self.app.cli_object.get_schema_from_reference('', '#/components/schemas/Scope')
        self.add_attribute_checkbox = CheckboxList(values=[('', '')])
        self.tbuffer = None
        self.prepare_attributes_data()
        self.prepare_tabs()
        self.create_window()
        self.sope_type = self.data.get('scopeType') or 'openid'

    def prepare_attributes_data(self):
        self.jans_attributes_data = []
        for attribute in common_data.jans_attributes:
            self.jans_attributes_data.append([attribute['dn'], attribute.get('displayName') or attribute.get('claimName')])


    def get_attrib_by_dn(self, dn):
        for attribute in common_data.jans_attributes:
            if attribute['dn'] == dn:
                return [attribute['dn'], attribute.get('displayName') or attribute.get('claimName')]

    def save(self) -> None:
        """method to invoked when saving the dialog (Save button is pressed)
        """

        self.app.logger.debug('SAVE SCOPE')

        data = {}

        for item in self.dialog.content.children + self.alt_tabs[self.sope_type].children:
            item_data = self.get_item_data(item)
            if item_data:
                data[item_data['key']] = item_data['value']

        if data['scopeType'] in ('openid', 'dynamic') and hasattr(self, 'claims_container') and self.claims_container:
            claims = [claim[0] for claim in self.claims_container.entries]
            data['claims'] = claims

        if data['scopeType'] == 'dynamic':
            data['dynamicScopeScripts'] = self.dynamic_scope_scripts_widget.get_values()

        self.app.logger.debug('DATA: ' + str(data))
        self.data = data
        if 'attributes' in self.data.keys():
            self.data['attributes'] = {'showInConfigurationEndpoint':self.data['attributes']}

        cfr = self.check_required_fields(self.dialog.content)
        self.app.logger.debug('CFR: '+str(cfr))
        if not cfr:
            return

        close_me = True
        if self.save_handler:
            close_me = self.save_handler(self)
        if close_me:
            self.future.set_result(DialogResult.ACCEPT)

    def cancel(self) -> None:
        """method to invoked when canceling changes in the dialog (Cancel button is pressed)
        """

        self.future.set_result(DialogResult.CANCEL)

    def create_window(self) -> None:
        scope_types = [('oauth', 'OAuth'), ('openid', 'OpenID'), ('dynamic', 'Dynamic'), ('uma', 'UMA')]
        buttons = [(self.save, _("Save")), (self.cancel, _("Cancel"))]
        if self.data:
            if self.data.get('scopeType') == 'spontaneous':
                scope_types.insert(3, ('spontaneous', 'Spontaneous'))
                buttons.pop(0)

            if self.data.get('scopeType') == 'uma':
                buttons.pop(0)
            else:
                for stype in scope_types[:]:
                    if stype[0] == 'uma':
                        scope_types.remove(stype)

        self.dialog = JansDialogWithNav(
            title=self.title,
            content= HSplit([
                self.app.getTitledRadioButton(
                                _("Scope Type"),
                                name='scopeType',
                                current_value=self.data.get('scopeType') or 'openid',
                                values=scope_types,
                                on_selection_changed=self.scope_selection_changed,
                                jans_help=self.app.get_help_from_schema(self.schema, 'scopeType'),
                                style=cli_style.radio_button),

                self.app.getTitledText(
                    _("id"), 
                    name='id', 
                    value=self.data.get('id',''), 
                    jans_help=self.app.get_help_from_schema(self.schema, 'id'),
                    style=cli_style.edit_text_required),

                self.app.getTitledText(
                    _("inum"), 
                    name='inum', 
                    value=self.data.get('inum',''), 
                    jans_help=self.app.get_help_from_schema(self.schema, 'inum'),
                    style=cli_style.edit_text,
                    read_only=True,),

                self.app.getTitledText(
                    _("Display Name"), 
                    name='displayName', 
                    value=self.data.get('displayName',''),
                    jans_help=self.app.get_help_from_schema(self.schema, 'displayName'),
                    style=cli_style.edit_text),

                self.app.getTitledText(
                    _("Description"), 
                    name='description', 
                    value=self.data.get('description',''), 
                    jans_help=self.app.get_help_from_schema(self.schema, 'description'),
                    style=cli_style.edit_text),

                DynamicContainer(lambda: self.alt_tabs[self.sope_type]),
            ], style='class:outh-scope-tabs'),
             button_functions=buttons,
            height=self.app.dialog_height,
            width=self.app.dialog_width,
                   )

    def scope_selection_changed(
        self, 
        cb: RadioList,
        ) -> None:
        """This method for scope type selection set

        Args:
            cb (RadioList): the New Value from the nav-bar
        """

        self.sope_type = cb.current_value


    def delete_claim(self, claim: list) -> None:
        """This method for the deletion of claim

        Args:
            selected (_type_): The selected claim
            event (_type_): _description_

        """

        def do_delete_claim(dialog):
            self.claims_container.remove_label(claim[0])

        dialog = self.myparent.get_confirm_dialog(
            message=HTML(_("Are you sure want to delete claim <b>{}</b>?").format(claim[1])),
            confirm_handler=do_delete_claim
        )

        self.app.show_jans_dialog(dialog)


    def prepare_tabs(self) -> None:
        """Prepare the tabs for Edil Scope Dialogs
        """

        self.alt_tabs = {}


        self.alt_tabs['oauth'] = HSplit([
                            self.app.getTitledCheckBox(
                                    _("Default Scope"),
                                    name='defaultScope',
                                    checked=self.data.get('defaultScope'),
                                    jans_help=self.app.get_help_from_schema(self.schema, 'defaultScope'),
                                    style=cli_style.check_box,
                            ),

                            self.app.getTitledCheckBox(
                                    _("Show in configuration endpoint"),
                                    name='showInConfigurationEndpoint',
                                    checked=self.data.get('attributes',{}).get('showInConfigurationEndpoint',''),
                                    jans_help='Configuration Endpoint',
                                    style=cli_style.check_box,
                            ),

                        ],width=D(),)


        open_id_widgets = [
                            self.app.getTitledCheckBox(
                                    _("Default Scope"),
                                    name='defaultScope',
                                    checked=self.data.get('defaultScope'),
                                    jans_help=self.app.get_help_from_schema(self.schema, 'defaultScope'),
                                    style=cli_style.check_box,
                            ),

                            self.app.getTitledCheckBox(
                                    _("Show in configuration endpoint"),
                                    name='showInConfigurationEndpoint',
                                    checked=self.data.get('attributes',{}).get('showInConfigurationEndpoint',''),
                                    jans_help='Configuration Endpoint',
                                    style=cli_style.check_box,
                            ),
                            ]

        add_claim_button = VSplit([Window(), self.app.getButton(
            text=_("Add Claim"),
            name='oauth:scope:claim-button',
            jans_help=_("Add claim"),
            handler=self.add_claim)
        ])


        claims = []
        for claim in self.data.get('claims', []):
            attbibute = self.get_attrib_by_dn(claim)
            if attbibute:
                claims.append(attbibute)

        self.claims_container = JansLabelContainer(
            title=_('Claims'),
            width=int(self.app.dialog_width*1.1) - 26,
            on_display=self.app.data_display_dialog,
            on_delete=self.delete_claim,
            buttonbox=add_claim_button,
            entries=claims,
        )

        open_id_widgets.append(self.claims_container)

        self.alt_tabs['openid'] = HSplit(open_id_widgets, width=D())


        dynamic_scope_scripts_data = []

        for scr in common_data.enabled_scripts:
            script_type = scr.get('scriptType')
            script_dn = scr.get('dn')

            if script_type == 'dynamic_scope':
                dynamic_scope_scripts_data.append((script_dn, scr['name']))

        self.dynamic_scope_scripts_widget = JansLabelWidget(
                        title = _("Dynamic Scope Script"),
                        values = self.data.get('dynamicScopeScripts', []),
                        data = dynamic_scope_scripts_data
                        )

        self.alt_tabs['dynamic'] = HSplit([

                        self.dynamic_scope_scripts_widget,
                        self.app.getTitledText(
                                _("Claims"),
                                name='claims',
                                value='\n'.join(self.data.get('claims', [])),
                                height=3, 
                                jans_help=self.app.get_help_from_schema(self.schema, 'claims'),
                                style=cli_style.edit_text
                                ),

                        ],width=D(),
                    )

        self.alt_tabs['spontaneous'] = HSplit([
                    self.app.getTitledText(
                        _("Associated Client"), 
                        name='none', 
                        value=self.data.get('none',''), 
                        style=cli_style.edit_text,
                        read_only=True,
                        jans_help=self.app.get_help_from_schema(self.schema, 'none'),
                        height=3,),## Not fount

                    self.app.getTitledText(
                        _("Creationg time"), 
                        name='creationDate', 
                        value=self.data.get('creationDate',''), 
                        jans_help=self.app.get_help_from_schema(self.schema, 'creationDate'),
                        style=cli_style.edit_text,
                        read_only=True,),

                                                ],width=D(),
                    )

        uma_creator = self.data.get('creatorId','') or self.app.cli_object.get_user_info().get('inum','')


        self.alt_tabs['uma'] = HSplit([
                    self.app.getTitledText(
                        _("IconURL"), 
                        name='iconUrl', 
                        value=self.data.get('iconUrl',''), 
                        jans_help=self.app.get_help_from_schema(self.schema, 'iconUrl'),
                        style=cli_style.edit_text),
                    

                    self.app.getTitledText(_("Authorization Policies"),
                            name='umaAuthorizationPolicies',
                            value='\n'.join(self.data.get('umaAuthorizationPolicies', [])),
                            height=3, 
                            jans_help=self.app.get_help_from_schema(self.schema, 'umaAuthorizationPolicies'),
                            style=cli_style.edit_text),

                    self.app.getTitledText(
                        _("Associated Client"), 
                        name='none', 
                        value=self.data.get('none',''), 
                        jans_help=self.app.get_help_from_schema(self.schema, 'none'),
                        style=cli_style.edit_text,
                        read_only=True,
                        height=3,), ## Not fount

                    self.app.getTitledText(
                        _("Creationg time"), 
                        name='description', 
                        value=self.data.get('description',''), 
                        jans_help=self.app.get_help_from_schema(self.schema, 'description'),
                        style=cli_style.edit_text,
                        read_only=True,),

                    self.app.getTitledText(
                                    _("Creator"), 
                                    name='Creator',
                                    style=cli_style.edit_text,
                                    jans_help=self.app.get_help_from_schema(self.schema, 'Creator'),
                                    read_only=True,
                                    value=uma_creator
                                    ),
                    ],
                    width=D(),
                    )

    def attribute_exists(self, dn):
        for dn_, name_ in self.claims_container.entries:
            if dn_ == dn:
                return True

    def add_claim(self):

        def add_selected_claims(dialog):
            if 'claims' not in self.data:
                self.data['claims'] = []

            self.data['claims'] += self.add_attribute_checkbox.current_values
            self.fill_claims_container()

        attribute_list = []
        for attribute in common_data.jans_attributes:
            if not self.attribute_exists(attribute['dn']):
                attribute_list.append((
                    attribute['dn'],
                    attribute.get('displayName') or attribute.get('claimName')
                    ))

            attribute_list.sort(key=lambda x: x[1])

        def on_text_changed(event):
            search_text = event.text
            matching_items = []
            for item in attribute_list:
                if search_text.lower() in item[1].lower():
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
            _("OK"), handler=add_selected_claims)]

        self.addAttributeDialog = JansGDialog(
            self.app,
            title=_("Select Attributes to add"),
            body=layout,
            buttons=buttons)

        self.app.show_jans_dialog(self.addAttributeDialog)


    def fill_claims_container(self):
        for attribute_dn in self.data['claims']:
            attribute = self.get_attrib_by_dn(attribute_dn)
            if attribute and not self.attribute_exists(attribute_dn):
                    self.claims_container.add_label(*attribute)


    def __pt_container__(self) -> Dialog:
        """The container for the dialog itself

        Returns:
            Dialog: The Edit Scope Dialog
        """

        return self.dialog

