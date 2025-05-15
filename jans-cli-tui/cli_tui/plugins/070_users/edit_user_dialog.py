import asyncio
import json

from typing import Optional, Sequence, Callable, Any
from functools import partial

from prompt_toolkit import HTML
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.layout import ScrollablePane
from prompt_toolkit.layout.containers import HSplit, VSplit,\
    DynamicContainer, Window
from prompt_toolkit.formatted_text import AnyFormattedText
from prompt_toolkit.widgets import Button, Label, CheckboxList, Dialog, TextArea
from prompt_toolkit.eventloop import get_event_loop

from utils.multi_lang import _
from utils.static import DialogResult
from utils.utils import DialogUtils, common_data, check_email
from wui_components.jans_dialog_with_nav import JansDialogWithNav
from wui_components.jans_cli_dialog import JansGDialog
from wui_components.jans_vetrical_nav import JansVerticalNav
from wui_components.jans_label_widget import JansLabelWidget


class EditUserDialog(JansGDialog, DialogUtils):
    """This user editing dialog
    """
    def __init__(
            self,
            parent: object,
            data:dict,
            title: AnyFormattedText= "",
            buttons: Optional[Sequence[Button]]= [],
            
            )-> Dialog:
        """init for `EditUserDialog`, inherits from two diffrent classes `JansGDialog` and `DialogUtils`
            
        JansGDialog (dialog): This is the main dialog Class Widget for all Jans-cli-tui dialogs except custom dialogs like dialogs with navbar
        DialogUtils (methods): Responsable for all `make data from dialog` and `check required fields` in the form for any Edit or Add New
                
        Args:
            parent (widget): This is the parent widget for the dialog, to access `Pageup` and `Pagedown`
            title (str): The Main dialog title
            data (list): selected line data 
            buttons (list, optional): Dialog main buttons with their handlers. Defaults to [].
        """
        super().__init__(common_data.app, title, buttons)
        self.myparent = parent
        self.data = data
        self.title=title
        self.admin_ui_roles = {}
        self.schema = common_data.app.cli_object.get_schema_from_reference('User-Mgt', '#/components/schemas/CustomUser')
        self.create_window()

    def cancel(self) -> None:
        """method to invoked when canceling changes in the dialog (Cancel button is pressed)
        """

        self.future.set_result(DialogResult.CANCEL)

    def get_claim_properties(self, claim):
        """This method for getting claims properties

        Args:
            claim (str): Claim

        Returns:
            _type_: properties
        """

        ret_val = {}
        for tmp in common_data.jans_attributes:
            if tmp['name'] == claim:
                ret_val = tmp
                break

        return ret_val



    def add_multivalued_value(self, widget):

        claim_text_area = TextArea()

        def do_add_multivalue(dialog):
            widget.container.add_label(claim_text_area.text, claim_text_area.text)

        claim_label = _("Value")
        body = VSplit([Label(claim_label + ' :', width=(len(claim_label)+3)), claim_text_area])
        buttons = [Button(_("Cancel")), Button(_("OK"), handler=do_add_multivalue)]
        dialog = JansGDialog(common_data.app, title=_(f"Add new {widget.title}"), body=body, buttons=buttons, width=common_data.app.dialog_width-20)
        common_data.app.show_jans_dialog(dialog)


    def get_multivalued_widget(self, title, values, jans_name, data=[]):
        add_handler = None if data else self.add_multivalued_value
        widget = JansLabelWidget(
                        title = _(title),
                        values = values,
                        data = [],
                        label_width=100,
                        add_handler=add_handler,
                        jans_name = jans_name
                        )

        return widget

    def create_window(self) -> None:

        custom_attributes = self.data.get('customAttributes', [])

        def get_custom_attribute(attribute, multi=False):
            for ca in custom_attributes:
                if ca['name'] == attribute:
                    values = ca.get('values', [])
                    #check if there is a bool value
                    for val in values:
                        if isinstance(val, bool):
                            return val
                    if multi:
                        return values
                    while None in values:
                        values.remove(None)
                    ret_val = ', '.join(values)
                    return ret_val
            return [] if multi else ''

        if self.data:
            active_checked = self.data.get('status', '').lower() == 'active'
        else:
            active_checked = True

        self.edit_user_content = [
                    common_data.app.getTitledText(_("Inum"), name='inum', value=self.data.get('inum',''), style='class:script-titledtext', jans_help=common_data.app.get_help_from_schema(self.schema, 'inum'), read_only=True),
                    common_data.app.getTitledText(_("Username *"), name='userId', value=self.data.get('userId',''), style='class:script-titledtext', jans_help=common_data.app.get_help_from_schema(self.schema, 'userId')),
                    common_data.app.getTitledText(_("First Name"), name='givenName', value=self.data.get('givenName',''), style='class:script-titledtext', jans_help=common_data.app.get_help_from_schema(self.schema, 'givenName')),
                    common_data.app.getTitledText(_("Middle Name"), name='middleName', value=get_custom_attribute('middleName'), style='class:script-titledtext', jans_help=common_data.app.get_help_from_schema(self.schema, 'middleName')),
                    common_data.app.getTitledText(_("Last Name"), name='sn', value=get_custom_attribute('sn'), style='class:script-titledtext', jans_help=common_data.app.get_help_from_schema(self.schema, 'sn')),
                    common_data.app.getTitledText(_("Display Name"), name='displayName', value=self.data.get('displayName',''), style='class:script-titledtext', jans_help=common_data.app.get_help_from_schema(self.schema, 'displayName')),
                    common_data.app.getTitledText(_("Email *"), name='mail', value=self.data.get('mail',''), style='class:script-titledtext', jans_help=common_data.app.get_help_from_schema(self.schema, 'mail')),
                    common_data.app.getTitledCheckBox(_("Active"), name='active', checked=active_checked, style='class:script-checkbox', jans_help=common_data.app.get_help_from_schema(self.schema, 'enabled')),
                    common_data.app.getTitledText(_("Nickname"), name='nickname', value=get_custom_attribute('nickname', multi=False), style='class:script-titledtext', jans_help=common_data.app.get_help_from_schema(self.schema, 'nickname')),
                    Button(_("Add Claim"), handler=self.add_claim),
                ]

        if common_data.app.plugin_enabled('admin'):
            widget = self.get_multivalued_widget(
                    title=_("Admin UI Roles"),
                    values=get_custom_attribute('jansAdminUIRole', multi=True),
                    data=[ (role['role'], role['role']) for role in common_data.admin_ui_roles ],
                    jans_name = 'jansAdminUIRole'
                    )

            self.edit_user_content.insert(-1, widget)

        if not self.data:
            self.edit_user_content.insert(2,
                    common_data.app.getTitledText(_("Password *"), name='userPassword', value='', style='class:script-titledtext', jans_help=common_data.app.get_help_from_schema(self.schema, 'userPassword'))
                )


        for ca in custom_attributes:

            if ca['name'] in ('middleName', 'sn', 'status', 'nickname', 'jansActive', 'userPassword', 'jansAdminUIRole'):
                continue

            claim_prop = self.get_claim_properties(ca['name'])
            value = get_custom_attribute(ca['name'], multi=claim_prop.get('oxMultiValuedAttribute'))

            if claim_prop.get('oxMultiValuedAttribute'):
                widget = self.get_multivalued_widget(
                    title=claim_prop.get('displayName', ca['name']),
                    values=value,
                    jans_name=ca['name']
                    )
                self.edit_user_content.insert(-1, widget)

            elif claim_prop.get('dataType', 'string') in ('string', 'json'):
                widget = common_data.app.getTitledText(
                    _(claim_prop.get('displayName', ca['name'])),
                    name=ca['name'],
                    value=value,
                    style='class:script-titledtext',
                    jans_help=common_data.app.get_help_from_schema(self.schema, ca['name']),
                    jans_list_type = claim_prop.get('oxMultiValuedAttribute'),
                    height = 2 if claim_prop.get('oxMultiValuedAttribute') else 1
                    )

                self.edit_user_content.insert(-1, widget)

            elif claim_prop.get('dataType') == 'boolean':
                self.edit_user_content.insert(-1, 
                    common_data.app.getTitledCheckBox(_(claim_prop['displayName']), name=ca['name'], checked=ca['value'], style='class:script-checkbox', jans_help=common_data.app.get_help_from_schema(self.schema, ca['name']))
                )

        self.edit_user_container = ScrollablePane(content=HSplit(self.edit_user_content, width=D()),show_scrollbar=False)

        self.dialog = JansDialogWithNav(
            title=self.title,
            content=DynamicContainer(lambda: self.edit_user_container),
            button_functions=[(self.cancel, _("Cancel")), (self.save_user, _("Save"))],
            height=common_data.app.dialog_height,
            width=common_data.app.dialog_width,
            )

    def add_claim(self) -> None:
        """This method for adding new claim
        """
        cur_claims = []
        for w in self.edit_user_content:
            if hasattr(w, 'me'):
                cur_claims.append(w.me.window.jans_name)

        claims_list = []
        for claim in common_data.jans_attributes:
            if not claim['oxMultiValuedAttribute'] and claim['name'] in cur_claims:
                continue
            if claim['name'] in ('memberOf', 'userPassword', 'uid', 'status', 'jansActive', 'updatedAt', 'jansAdminUIRole'):
                continue
            if claim.get('status') == 'active':
                claims_list.append((claim['name'], claim['displayName']))

        claims_checkbox = CheckboxList(values=claims_list)

        def add_claim(dialog) -> None:
            for claim_ in claims_checkbox.current_values:
                for claim_prop in common_data.jans_attributes:
                    if claim_prop['name'] == claim_:
                        break
                display_name = claim_prop['displayName']
                
                if claim_prop.get('oxMultiValuedAttribute'):
                    widget = self.get_multivalued_widget(
                        title=display_name,
                        values=[],
                        jans_name=claim_
                        )

                elif claim_prop['dataType'] == 'boolean':
                    widget = common_data.app.getTitledCheckBox(
                        _(display_name),
                        name=claim_,
                        style='class:script-checkbox',
                        jans_help=common_data.app.get_help_from_schema(self.schema, claim_)
                        )

                else:
                    widget = common_data.app.getTitledText(
                        _(display_name),
                        name=claim_,
                        value='',
                        style='class:script-titledtext',
                        jans_help=common_data.app.get_help_from_schema(self.schema, claim_)
                        )
                self.edit_user_content.insert(-1, widget)
            self.edit_user_container = ScrollablePane(content=HSplit(self.edit_user_content, width=D()),show_scrollbar=False)


        body = HSplit([Label(HTML(_("Select claim to be added to current user.\n<i>Note</i>: Only <b>active</b> claims are displayed."))), claims_checkbox])
        buttons = [Button(_("Cancel")), Button(_("OK"), handler=add_claim)]
        dialog = JansGDialog(common_data.app, title=_("Claims"), body=body, buttons=buttons, width=common_data.app.dialog_width-20)
        common_data.app.show_jans_dialog(dialog)


    def save_user(self) -> None:
        """This method to save user data to server
        """

        fix_title = _("Please fix!")
        raw_data = self.make_data_from_dialog(tabs={'user': self.edit_user_container.content})
        for widget in self.edit_user_content:
            if isinstance(widget, JansLabelWidget):
                raw_data[widget.jans_name] = widget.get_values()

        if not (raw_data['userId'].strip() and raw_data['mail'].strip()):
            common_data.app.show_message(fix_title, _("Username and/or Email is empty"))
            return

        if not check_email(raw_data['mail']):
            common_data.app.show_message(fix_title, _("Please enter a valid email"))
            return

        if 'baseDn' not in self.data and not raw_data['userPassword'].strip():
            common_data.app.show_message(fix_title, _("Please enter Password"))
            return

        user_info = {'customObjectClasses':['top', 'jansPerson', 'jansCustomPerson'], 'customAttributes':[]}
        for key_ in ('mail', 'userId', 'displayName', 'givenName'):
            user_info[key_] = raw_data.pop(key_)

        if 'baseDn' not in self.data:
            user_info['userPassword'] = raw_data.pop('userPassword')

        for key_ in ('inum', 'baseDn', 'dn'):
            if key_ in raw_data:
                del raw_data[key_]
            if key_ in self.data:
                user_info[key_] = self.data[key_]

        status = raw_data.pop('active')
        user_info['status'] = 'active' if status else 'inactive'

        for key_ in raw_data:
            multi_valued = isinstance(raw_data[key_], list)
            key_prop = self.get_claim_properties(key_)

            if key_prop.get('dataType') == 'json' and raw_data[key_]:
                try:
                    json.loads(raw_data[key_])
                except Exception as e:
                    display_name = key_prop.get('displayName') or key_
                    common_data.app.show_message(
                                fix_title,
                                _(HTML("Can't convert <b>{}</b> to json. Conversion error: <i>{}</i>").format(display_name, e))
                            )
                    return

            if multi_valued:
                if raw_data[key_]:
                    claim_data = raw_data[key_]
                else:
                    multi_valued = False
                    claim_data = None
            else:
                claim_data = [raw_data[key_]]

            user_info['customAttributes'].append({
                    'name': key_, 
                    'multiValued': multi_valued, 
                    'values': claim_data,
                    })

        for ca in self.data.get('customAttributes', [])[:]:
            if ca['name'] == 'memberOf':
                user_info['customAttributes'].append(ca)
                break

        async def coroutine():
            operation_id = 'put-user' if self.data.get('baseDn') else 'post-user'
            cli_args = {'operation_id': operation_id, 'data': user_info}
            common_data.app.start_progressing(_("Saving user ..."))
            response = await common_data.app.loop.run_in_executor(common_data.app.executor, common_data.app.cli_requests, cli_args)
            common_data.app.stop_progressing()
            if response.status_code not in (200, 201):
                common_data.app.show_message(_('Error'), response.text + '\n' + response.reason)
            else:
                self.future.set_result(DialogResult.OK)
                self.myparent.get_users()

        asyncio.ensure_future(coroutine())



    def __pt_container__(self)-> Dialog:
        """The container for the dialog itself

        Returns:
            Dialog: The Edit User Dialog
        """

        return self.dialog

