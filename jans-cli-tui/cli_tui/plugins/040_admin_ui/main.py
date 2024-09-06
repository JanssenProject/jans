import asyncio

from collections import OrderedDict
from functools import partial
from typing import Any, Optional

from prompt_toolkit.application import Application
from prompt_toolkit.layout.containers import HSplit, VSplit, HorizontalAlign, DynamicContainer
from prompt_toolkit.buffer import Buffer
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.formatted_text import HTML
from prompt_toolkit.widgets import Button, Label, Box, Dialog, Frame

from wui_components.jans_nav_bar import JansNavBar
from wui_components.jans_cli_dialog import JansGDialog
from wui_components.jans_vetrical_nav import JansVerticalNav
from utils.multi_lang import _
from utils.static import cli_style
from utils.utils import get_help_with

from edit_mapping_dialog import EditMappingDialog


class Plugin():
    """This is a general class for plugins 
    """
    def __init__(
        self, 
        app: Application
        ) -> None:
        """init for Plugin class "config_api"

        Args:
            app (Generic): The main Application class
        """
        self.app = app
        self.pid = 'admin'
        self.name = 'A[d]min-UI'
        self.server_side_plugin = True
        self.page_entered = False
        self.role_type = 'api-viewer'
        self.admin_ui_permissions = []
        self.role_permission_mappings = {}
        self.prepare_navbar()
        self.prepare_containers()
        self.jans_help = get_help_with(
                f'<m>              {_("View/Edit role properties mapping")}'
                )

    def process(self) -> None:
        pass

    def prepare_navbar(self) -> None:
        """prepare the navbar for the current Plugin 
        """
        self.nav_bar = JansNavBar(
                    self.app,
                    entries=[('accessroles', 'Access r[o]les'), ('permissions', '[P]ermissions')],
                    selection_changed=self.nav_selection_changed,
                    select=0,
                    jans_name='admin_ui:nav_bar'
                    )

    def prepare_containers(self) -> None:
        """prepare the main container (tabs) for the current Plugin 
        """

        self.containers = OrderedDict()
        self.main_area = HSplit([Label("configuration")],width=D())

        self.main_container = HSplit([
                                        Box(self.nav_bar.nav_window, style=cli_style.sub_navbar, height=1),
                                        DynamicContainer(lambda: self.main_area),
                                        ],
                                    height=D(),
                                    style=cli_style.container
                                    )
        self.create_widgets()

    def create_widgets(self):

        self.config_data_container = {
            'accessroles': HSplit([], width=D()),
            'permissions': HSplit([], width=D()),
        }

        self.accessroles_container = JansVerticalNav(
                myparent=self.app,
                headers=['Role', 'Description', 'Deletable', '# Permissions'],
                preferred_size= [0, 0, 0, 0],
                on_enter=self.edit_adminui_roles,
                on_display=self.app.data_display_dialog,
                on_delete= self.delete_adminui_roles,
                selectes=0,
                headerColor=cli_style.navbar_headcolor,
                entriesColor=cli_style.navbar_entriescolor,
                custom_key_bindings=([('m', self.display_mappings)]),
            )

        self.containers['accessroles'] = HSplit([
                    VSplit([
                        self.app.getButton(
                            text=_("Get adminui roles"), 
                            name='oauth:clients:get', 
                            jans_help=_("Get all admin ui roles"), 
                            handler=self.get_adminui_roles),

                        self.app.getButton(
                            text=_("Add adminui role"), 
                            name='oauth:scopes:add', 
                            jans_help=_("Add admin ui role"), 
                            handler=self.add_adminui_role),
                        ],
                        padding=3,
                        width=D(),
                        ),
                        self.accessroles_container
                     ],style=cli_style.container)


        self.adminui_permissions_container = JansVerticalNav(
                myparent=self.app,
                headers=['permission', 'defaultPermissionInToken'],
                preferred_size=[0, 0],
                on_enter=self.edit_adminui_permissions,
                on_display=self.app.data_display_dialog,
                on_delete=self.delete_adminui_permissions,
                selectes=0,
                headerColor=cli_style.navbar_headcolor,
                entriesColor=cli_style.navbar_entriescolor,
            )
        self.adminui_permissions_container_buttons = VSplit([])

        self.containers['permissions'] = HSplit([
                    VSplit([
                        self.app.getTitledText(
                            _("Search"), 
                            name='oauth:scopes:search', 
                            jans_help=_("Press enter to perform search"), 
                            accept_handler=self.search_adminui_permissions,
                            style=cli_style.edit_text),

                        self.app.getButton(
                            text=_("Add adminui permission"), 
                            name='oauth:scopes:add', 
                            jans_help=_("Add admin ui role"), 
                            handler=self.add_adminui_permissions),
                        ],
                        padding=3,
                        width=D(),
                        ),
                        self.adminui_permissions_container,
                        DynamicContainer(lambda: self.adminui_permissions_container_buttons)
                     ],style=cli_style.container)

        self.nav_selection_changed(list(self.containers)[0])


    def get_adminui_roles(self) -> None:
        """Method to get the admin ui roles from server
        """

        async def coroutine():
            # retreive roles
            cli_args = {'operation_id': 'get-all-adminui-roles'}
            self.app.start_progressing()
            response = await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing()
            data = response.json()
            if response.status_code not in (200, 201):
                self.app.show_message(_("Error Getting Admin UI Roles!"), str(data), tobefocused=self.app.center_container)
                return

            # retreive mappings
            cli_args = {'operation_id': 'get-all-adminui-role-permissions'}
            self.app.start_progressing()
            response = await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing()

            role_permission_mappings_list = response.json()
            self.role_permission_mappings = {mapping['role']: mapping['permissions'] for mapping in role_permission_mappings_list}


            self.accessroles_container.clear()
            self.accessroles_container.all_data = data

            for d in data:
                role_name = d.get('role')
                self.accessroles_container.add_item([
                    role_name,
                    d.get('description', ''),
                    _("Yes") if d.get('deletable') else _("No"),
                    len(self.role_permission_mappings.get(role_name, []))
                ])

            self.app.layout.focus(self.accessroles_container)

        asyncio.ensure_future(coroutine())


    def add_adminui_role(self) -> None:
        """Method to display the dialog of adminui-roles
        """

        self.adminui_role = self.app.getTitledText(
            _("Role"), 
            name='role',
            style=cli_style.edit_text_required)

        self.adminui_role_description = self.app.getTitledText(
            _("Description"), 
            name='Description', 
            style=cli_style.edit_text_required)

        def save(dialog: Dialog) -> None:
            role = self.adminui_role.me.text.strip()
            desc = self.adminui_role_description.me.text.strip()

            if not (role or desc):
                self.app.show_message(_("Error!"), HTML(_("<b>Role</b> name and/or <b>Description</b> was not entered.")), tobefocused=self.app.center_container)
                return

            response = self.app.cli_object.process_command_by_id(
                    operation_id='add-adminui-role' ,
                    url_suffix='',
                    endpoint_args='',
                    data_fn='',
                    data={'role': '{}'.format(role), 'description': '{}'.format(desc)},
                    )

            if response:
                self.get_adminui_roles()
                # self.future.set_result(DialogResult.ACCEPT)
                return True

            self.app.show_message(_("Error!"), _("An error ocurred while Adding admin-ui role:\n") + str(response.text))


        body = HSplit([self.adminui_role, self.adminui_role_description])
        buttons = [Button(_("Cancel")), Button(_("OK"), handler=save)]
        dialog = JansGDialog(self.app, title=_('Add New Role'), body=body, buttons=buttons, width=self.app.dialog_width-20)
        self.app.show_jans_dialog(dialog)

    def edit_adminui_roles(self, **params: Any) -> None:
        """Method to display the dialog of admin-ui roles for editing
        """

        role_data = params.get('data', {})
        title = role_data.get('role','')

        self.adminui_role_description = self.app.getTitledText(
            _("Description"),
            name='description',
            value=role_data.get('description',''),
            style=cli_style.edit_text_required)

        self.adminui_role_deletable = self.app.getTitledCheckBox(
            "Deletable", 
            name='deletable', 
            checked= role_data.get('deletable', False),
            jans_help= "Default to False",
            style=cli_style.check_box)

        def save(dialog: Dialog) -> None:
            desc = self.adminui_role_description.me.text
            deletable = self.adminui_role_deletable.me.checked

            async def coroutine():
                cli_args = {
                    'operation_id': 'edit-adminui-role',
                    'data': {'role': '{}'.format(title), 'description': '{}'.format(desc), 'deletable':'{}'.format(deletable)}
                    }
                self.app.start_progressing()
                response = await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
                self.app.stop_progressing()
                self.get_adminui_roles()
                if response.status_code != 200:
                    self.app.show_message(_("Error!"), _("An error ocurred while saving role adminui:\n") + str(response.text), tobefocused=self.app.center_container)

            asyncio.ensure_future(coroutine())

        body = HSplit([self.adminui_role_description,self.adminui_role_deletable])
        buttons = [Button(_("Cancel")), Button(_("OK"), handler=save)]
        dialog = JansGDialog(self.app, title=title, body=body, buttons=buttons, width=self.app.dialog_width-20)
        self.app.show_jans_dialog(dialog)

    def delete_adminui_roles(self, **kwargs: Any) -> None:
        """Method to delete admin-ui roles 
        """

        role_to_be_deleted = kwargs['selected'][0]
        delatable = kwargs['selected'][2]

        if delatable == _("No"):
            self.app.show_message(_("Error!"), HTML(_("This role cannot be deleted. To delete set <b>Deletable</b> flag to True.")), tobefocused=self.app.center_container)
            return

        def do_delete_adminui_roles(dialog):
            # first remove all permissions
            cli_args = {'operation_id': 'remove-role-permissions-permission', 'url_suffix':'adminUIRole:{}'.format(role_to_be_deleted)}
            self.app.start_progressing()
            response = self.app.cli_requests(cli_args)
            self.app.stop_progressing()

            # remove role
            cli_args = {'operation_id': 'delete-adminui-role', 'url_suffix':'adminUIRole:{}'.format(role_to_be_deleted)}
            self.app.start_progressing()
            response = self.app.cli_requests(cli_args)
            self.app.stop_progressing()
            if response:
                self.app.show_message(_("Error!"), str(response), tobefocused=self.app.center_container)
            else:
                self.get_adminui_roles()
                self.app.layout.focus(self.app.center_container)


        dialog = self.app.get_confirm_dialog(
            _("Are you sure want to delete adminui_roles :") +
            f'\n<b>{role_to_be_deleted}</b>?\n' +
            _("Note that all mappings for this role will be deleted."),
            confirm_handler=do_delete_adminui_roles
            )

        self.app.show_jans_dialog(dialog)

    def adminui_update_permissions(self,
        start_index: Optional[int]=0,
        pattern: Optional[str]=''
        ) -> None:
        """update the current adminui_permissions data to server

        Args:.
            start_index (Optional[int], optional): This is flag for the adminui-roles pages. Defaults to 0.
            pattern (str, optional): endpoint arguments for the client data. Defaults to ''.
        """

        async def coroutine():
            cli_args = {'operation_id': 'get-all-adminui-permissions'}
            self.app.start_progressing(_("Retreiving admin-ui permissions"))
            response = await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing()
            self.admin_ui_permissions = response.json()

            all_data = self.admin_ui_permissions[:]
            self.adminui_permissions_container.clear()

            if pattern:
                for k in all_data[:]:
                    if pattern.lower() not in k.get('permission').lower():
                        all_data.remove(k)
                        
            
            self.adminui_permissions_container.all_data = all_data[start_index:start_index+self.app.entries_per_page]

            for d in self.adminui_permissions_container.all_data:
                self.adminui_permissions_container.add_item(
                    [
                    d.get('permission'),
                    d.get('defaultPermissionInToken'),
                    ]
                )

            if all_data:
                buttons = []
                if len(all_data) and start_index:
                    handler_partial = partial(self.adminui_update_permissions, start_index-self.app.entries_per_page, pattern)
                    prev_button = Button(_("Prev"), handler=handler_partial)
                    prev_button.window.jans_help = _("Displays previous %d entries") % self.app.entries_per_page
                    buttons.append(prev_button)

                if len(all_data) - (start_index + self.app.entries_per_page) > 0:
                    handler_partial = partial(self.adminui_update_permissions, start_index+self.app.entries_per_page, pattern)
                    next_button = Button(_("Next"), handler=handler_partial)
                    next_button.window.jans_help = _("Displays next %d entries") % self.app.entries_per_page
                    buttons.append(next_button)

                self.adminui_permissions_container_buttons = VSplit(buttons, padding=3, width=D(), align=HorizontalAlign.CENTER)
                self.app.layout.focus(self.adminui_permissions_container)

            else:
                self.app.show_message(_("Oops"), _("No matching result"), tobefocused=self.app.center_container)

        asyncio.ensure_future(coroutine())



    def add_adminui_permissions(self) -> None:
        """Method to display the dialog of adminui-roles
        """

        self.adminui_permission = self.app.getTitledText(
            _("Permission"), 
            name='permission', 
            height=3, 
            style=cli_style.edit_text)

        self.adminui_role_permissions= self.app.getTitledCheckBox(
            'DefaultPermissionInToken', 
            name='defaultpermissionInToken', 
            checked= False, 
            style=cli_style.check_box)

        def save(dialog: Dialog) -> None:

            permission = self.adminui_permission.me.text
            defaultPermissionInToken = self.adminui_role_permissions.me.checked

            # ------------------------------------------------------------#
            # --------------------- Patch to server ----------------------#
            # ------------------------------------------------------------#
            if permission :
                async def coroutine():
                    cli_args = {
                        'operation_id': 'add-adminui-permission', 
                        'data': {'permission': '{}'.format(permission), 'defaultPermissionInToken': defaultPermissionInToken}
                        }
                    self.app.start_progressing()
                    response = await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
                    self.app.stop_progressing()

                    if response.status_code != 200:
                        self.app.show_message(_("Error!"), _("An error ocurred while Addin role adminui permission:\n") + str(response.text), tobefocused=self.app.center_container)
                    else:
                        self.adminui_update_permissions()

                asyncio.ensure_future(coroutine())


        body = HSplit([self.adminui_permission,self.adminui_role_permissions])
        buttons = [Button(_("Cancel")), Button(_("OK"), handler=save)]
        dialog = JansGDialog(self.app, title=_('Add New Role'), body=body, buttons=buttons, width=self.app.dialog_width-20)
        self.app.show_jans_dialog(dialog)
    
    def search_adminui_permissions(self, tbuffer:Buffer) -> None:
        """This method handel the search for adminui_permissions

        Args:
            tbuffer (Buffer): Buffer returned from the TextArea widget > GetTitleText
        """

        self.adminui_update_permissions(0, tbuffer.text)

    def edit_adminui_permissions(self, **params: Any) -> None:
        """Method to display the dialog of adminui_permissions for editing
        """

        role_data = params.get('passed', [])
        permission = role_data[0]

        defaultPermissionInToken = role_data[1]

        self.adminui_role_permissions= self.app.getTitledCheckBox(
            permission[8:78] if len(permission) > 30 else permission, 
            name='permission', 
            checked= defaultPermissionInToken, 
            style=cli_style.check_box)

        def save(dialog: Dialog) -> None:

            defaultPermissionInToken = self.adminui_role_permissions.me.checked

            # ------------------------------------------------------------#
            # --------------------- Patch to server ----------------------#
            # ------------------------------------------------------------#
            response = self.app.cli_object.process_command_by_id(
                    operation_id='edit-adminui-permission' ,
                    url_suffix='',
                    endpoint_args='',
                    data_fn='',
                    data={'permission': '{}'.format(permission), 'defaultPermissionInToken': '{}'.format(defaultPermissionInToken)},
                    )

            # ------------------------------------------------------------#
            # -- get_properties or serach again to see Momentary change --#
            # ------------------------------------------------------------#
            if response:
                self.adminui_update_permissions()
                # self.future.set_result(DialogResult.ACCEPT)
                return True

            self.app.show_message(_("Error!"), _("An error ocurred while saving role adminui:\n") + str(response.text))

        body = HSplit([self.adminui_role_permissions])
        buttons = [Button(_("Cancel")), Button(_("OK"), handler=save)]
        dialog = JansGDialog(self.app, title='admin ui permissions', body=body, buttons=buttons, width=self.app.dialog_width-20)
        self.app.show_jans_dialog(dialog)

    def delete_adminui_permissions(self, **kwargs: Any) -> None:
        """This method for the deletion of the adminui_permissions

        Returns:
            str: The server response
        """

        dialog = self.app.get_confirm_dialog(_("Are you sure want to delete adminui_permissions :")+"\n {} ?".format(kwargs['selected'][0]))

        async def coroutine():
            focused_before = self.app.layout.current_window
            result = await self.app.show_dialog_as_float(dialog)
            try:
                self.app.layout.focus(focused_before)
            except:
                self.app.stop_progressing()
                self.app.layout.focus(self.app.center_frame)

            if result.lower() == 'yes':
                result = self.app.cli_object.process_command_by_id(
                    operation_id='delete-adminui-permission',
                    url_suffix='adminUIPermission:{}'.format(kwargs ['selected'][0]),
                    endpoint_args='',
                    data_fn='',
                    data={}
                )
                self.app.stop_progressing()
                self.adminui_update_permissions()
                
            return result  ### TODO >> Role cannot be deleted. Please set ‘deletable’ property of role to true.

        asyncio.ensure_future(coroutine())

    def display_mappings(self, event):
        role_data = self.accessroles_container.all_data[self.accessroles_container.selectes]
        role = role_data['role']
        data = {'role': role, 'permissions': self.role_permission_mappings.get(role, [])}
        self.edit_adminui_mapping(data=data)


    def edit_adminui_mapping(self, **params: Any) -> None:
        """Method to display the dialog for editing role-permissions maping
        """

        role_data = params['data']
        edit_mapping_dialog = EditMappingDialog(parent=self, data=role_data)

        async def coroutine():
            cli_args = {'operation_id': 'get-all-adminui-permissions'}
            self.app.start_progressing(_("Retreiving admin-ui permissions"))
            response = await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing()
            edit_mapping_dialog.admin_ui_permissions = response.json()
            self.app.show_jans_dialog(edit_mapping_dialog)

        asyncio.ensure_future(coroutine())

    def nav_selection_changed(
                self,
                selection: str
            ) -> None:

        """This method for the selection change

        Args:
            selection (str): the current selected tab
        """

        if selection in self.containers:
            self.main_area = self.containers[selection]
        else:
            self.main_area = self.app.not_implemented

    def set_center_frame(self) -> None:
        """center frame content
        """
        self.app.center_container = self.main_container

