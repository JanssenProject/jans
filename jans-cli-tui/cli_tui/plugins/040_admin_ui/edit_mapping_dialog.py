import asyncio

from typing import Any

from prompt_toolkit.layout.containers import HSplit, VSplit, ConditionalContainer
from prompt_toolkit.application.current import get_app
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.formatted_text import HTML
from prompt_toolkit.widgets import Button, Label, Dialog, TextArea, CheckboxList, Frame
from prompt_toolkit.filters import Condition

from utils.utils import DialogUtils, common_data
from wui_components.jans_cli_dialog import JansGDialog
from wui_components.jans_vetrical_nav import JansVerticalNav
from wui_components.jans_dialog_with_nav import JansDialogWithNav

from utils.multi_lang import _
from utils.static import cli_style, DialogResult

class EditMappingDialog(JansGDialog, DialogUtils):

    """A dialog for editing/creating role-permissions mapping.
    """
    def __init__(self, parent: object, data: dict)-> None:
        """init for Plugin class "config_api"

        Args:
            mapping (dict): role-permission mapping dictionary
        """
        super().__init__(common_data.app, '', [])
        self.myparent = parent
        self.data = data
        self.admin_ui_permissions = []
        self.permissions = self.data['permissions']
        self.initial_permissions = len(self.permissions)
        self.role = self.data['role']
        self.create_window()

    def create_window(self) -> None:

        self.adminui_role_permissions_container = JansVerticalNav(
                        myparent=common_data.app,
                        headers=['Permissions'],
                        hide_headers=True,
                        preferred_size=[0],
                        on_delete=self.delete_role,
                        on_display=common_data.app.data_display_dialog,
                        selectes=0,
                        headerColor='red',
                        entriesColor='green',
                        max_height=common_data.app.dialog_height-10,
                    )

        body = HSplit([self.adminui_role_permissions_container])
        buttons = [(self.cancel, _("Cancel")), (self.add_permission, _("Add Permission")), (self.save, _("Save"))]

        self.dialog = JansDialogWithNav(
                            title=_('admin-ui Permissions for Role {}').format(self.role),
                            content=body,
                            button_functions=buttons,
                            height=common_data.app.dialog_height,
                            width=common_data.app.dialog_width,
                            show_scrollbar=False,
                            )

        self.update_adminui_role_permissions_container()


    def cancel(self) -> None:
        """method to invoked when canceling changes in the dialog (Cancel button is pressed)
        """

        self.future.set_result(DialogResult.CANCEL)

    def add_role_mapping(self) -> None:
        """Method to add the admin-ui role mapping
        """
        pass


    def delete_role(self, **params: Any) -> None:
        to_be_deleted = params['selected'][0]

        def do_delete_permission(dialog):
            self.permissions.remove(to_be_deleted)
            self.update_adminui_role_permissions_container()

        confirm_dialog = common_data.app.get_confirm_dialog(
                    HTML(_("Are you sure you want to remove permission <b>{}</b>?").format(to_be_deleted)),
                    do_delete_permission
                    )

        common_data.app.show_jans_dialog(confirm_dialog)


    def add_permission(self):

        search_warning_label = Label(text="")
        permission_list = []

        for permission in self.admin_ui_permissions:
            perm_ = permission['permission']
            if perm_ not in self.permissions:
                permission_list.append((perm_, perm_))
            permission_list.sort()

        def add_selected_permission(dialog):
            for permt in permissions_cb_list.current_values:
                self.permissions.append(permt)
            self.update_adminui_role_permissions_container()


        def search_permission_text_changed(event):
            search_text = event.text
            matching_permissions = []
            for permission in permission_list:
                if search_text.lower() in permission[1].lower():
                    matching_permissions.append(permission)

            if matching_permissions:
                permissions_cb_list.values = matching_permissions
                search_warning_label.text = ''
            else:
                permissions_cb_list.values = [(None,None)]
                search_warning_label.text = _("No matching permissions")


        permissions_cb_list = CheckboxList(values=permission_list)
        ta = TextArea(width=D(), multiline=False)
        ta.buffer.on_text_changed += search_permission_text_changed
        permission_list_frame = Frame(title="Checkbox list", body=HSplit([permissions_cb_list]))

        add_permission_layout = HSplit([
            VSplit([
                    Label(text=_("Filter "),
                    style=cli_style.label, width=len(_("Filter "))),
                    ta
                ]),
            search_warning_label,
            ConditionalContainer(content=permissions_cb_list, filter=Condition(lambda: not search_warning_label.text))
        ])

        add_permission_buttons = [Button(_("Cancel")), Button(_("OK"), handler=add_selected_permission)]

        add_permission_dialog = JansGDialog(
            common_data.app,
            title=_("Select permission to add"),
            body=add_permission_layout,
            buttons=add_permission_buttons)

        common_data.app.show_jans_dialog(add_permission_dialog)

    def save(self):
        common_data.app.start_progressing(_("Saving admin-ui role mapping"))
        if self.permissions:
            data = {'permissions': self.permissions, 'role': self.role}
            url_suffix = ''
            operation_id = 'map-permissions-to-role' if self.initial_permissions else 'add-role-permissions-mapping'
        else:
            data = None
            url_suffix = f'adminUIRole:{self.role}'
            operation_id = 'remove-role-permissions-permission'

        response = common_data.app.cli_object.process_command_by_id(
                operation_id=operation_id,
                url_suffix=url_suffix,
                endpoint_args='',
                data_fn='',
                data=data,
                )
        common_data.app.stop_progressing()

        if not response:
            self.myparent.get_adminui_roles()
            self.future.set_result(DialogResult.ACCEPT)
            return

        if isinstance(response, str) and response.strip():
            common_data.app.show_message(_("Info"), response, tobefocused=common_data.app.center_container)
            self.future.set_result(DialogResult.ACCEPT)
            return

        if response.status_code == 200:
            self.myparent.get_adminui_roles()
            self.future.set_result(DialogResult.ACCEPT)
        else:
            common_data.app.show_message(_("Error!"), _("An error ocurred while saving role adminui:\n") + str(response.text))


    def update_adminui_role_permissions_container(self):
        self.adminui_role_permissions_container.clear()
        self.adminui_role_permissions_container.all_data = self.permissions
        for permission in self.permissions:
            self.adminui_role_permissions_container.add_item([permission])


    def __pt_container__(self) -> Dialog:
        """The container for the dialog itself

        Returns:
            Dialog: The Edit Scope Dialog
        """

        return self.dialog
