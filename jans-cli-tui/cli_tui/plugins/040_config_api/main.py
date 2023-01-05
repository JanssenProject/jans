from prompt_toolkit.application import Application
from prompt_toolkit.layout.containers import (
    HSplit,
    VSplit,
    HorizontalAlign,
    DynamicContainer,
)
from prompt_toolkit.application.current import get_app
from prompt_toolkit.buffer import Buffer
from prompt_toolkit.layout.dimension import D
from wui_components.jans_nav_bar import JansNavBar
from prompt_toolkit.widgets import Button, Label, Box, Dialog
from wui_components.jans_cli_dialog import JansGDialog
from collections import OrderedDict
from functools import partial
from wui_components.jans_vetrical_nav import JansVerticalNav
from utils.multi_lang import _
from typing import Any, Optional
import asyncio

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
        self.pid = 'config_api'
        self.name = '[C]onfig-API'
        self.page_entered = False
        self.role_type = 'api-viewer'
        self.admin_ui_roles_data = {}

        self.prepare_navbar()
        self.prepare_containers()

    def process(self) -> None:
        pass

    def prepare_navbar(self) -> None:
        """prepare the navbar for the current Plugin 
        """
        self.nav_bar = JansNavBar(
                    self.app,
                    entries=[('accessroles', 'Access r[o]les'), ('permissions', '[P]ermissions'),  ('mapping', '[M]apping')],
                    selection_changed=self.nav_selection_changed,
                    select=0,
                    jans_name='fido:nav_bar'
                    )

    def prepare_containers(self) -> None:
        """prepare the main container (tabs) for the current Plugin 
        """

        self.containers = OrderedDict()
        self.main_area = HSplit([Label("configuration")],width=D())

        self.main_container = HSplit([
                                        Box(self.nav_bar.nav_window, style='class:sub-navbar', height=1),
                                        DynamicContainer(lambda: self.main_area),
                                        ],
                                    height=D(),
                                    style='class:outh_maincontainer'
                                    )
        self.create_widgets()

    def create_widgets(self):

        self.config_data_container = {
            'accessroles': HSplit([],width=D()),
            'permissions': HSplit([],width=D()),
            'mapping': HSplit([],width=D()),
        }

        self.containers['accessroles'] = HSplit([
                    VSplit([
                        self.app.getButton(
                            text=_("Get adminui roles"), 
                            name='oauth:clients:get', 
                            jans_help=_("Get all admin ui roles"), 
                            handler=self.get_adminui_roles),

                        self.app.getButton(
                            text=_("Add adminui roles"), 
                            name='oauth:scopes:add', 
                            jans_help=_("Add admin ui role"), 
                            handler=self.add_adminui_roles),
                        ],
                        padding=3,
                        width=D(),
                        ),
                        DynamicContainer(lambda: self.config_data_container['accessroles'])
                     ],style='class:outh_containers_clients')

        self.containers['permissions'] = HSplit([
                    VSplit([
                        self.app.getButton(
                            text=_("Get adminui permissions"), 
                            name='oauth:clients:get', 
                            jans_help=_("Get all admin ui permissions"), 
                            handler=self.get_adminui_permissions),
                        
                        self.app.getTitledText(
                            _("Search"), 
                            name='oauth:scopes:search', 
                            jans_help=_("Press enter to perform search"), 
                            accept_handler=self.search_adminui_permissions,
                            style='class:outh_containers_scopes.text'),

                        self.app.getButton(
                            text=_("Add adminui permission"), 
                            name='oauth:scopes:add', 
                            jans_help=_("Add admin ui role"), 
                            handler=self.add_adminui_permissions),
                        ],
                        padding=3,
                        width=D(),
                        ),
                        DynamicContainer(lambda: self.config_data_container['permissions'])
                     ],style='class:outh_containers_clients')

        self.containers['mapping'] = HSplit([
                    VSplit([
                        self.app.getButton(
                            text=_("Get adminui mapping"), 
                            name='oauth:clients:get', 
                            jans_help=_("Get all admin ui mapping"), 
                            handler=self.get_adminui_mapping),
                        
                        self.app.getTitledText(
                            _("Search"), 
                            name='oauth:scopes:search', 
                            jans_help=_("Press enter to perform search"), 
                            accept_handler=self.search_adminui_mapping,
                            style='class:outh_containers_scopes.text'),

                        # self.app.getButton(
                        #     text=_("Add adminui mapping"), 
                        #     name='oauth:scopes:add', 
                        #     jans_help=_("Add admin ui mapping"), 
                        #     handler=self.add_adminui_mapping),
                        ],
                        padding=3,
                        width=D(),
                        ),
                        DynamicContainer(lambda: self.config_data_container['mapping'])
                     ],style='class:outh_containers_clients')
                                
        self.nav_selection_changed(list(self.containers)[0])

    def get_adminui_roles(self) -> None:
        """Method to get the admin ui roles from server
        """
        cli_args = {'operation_id': 'get-all-adminui-roles'}

        async def coroutine():
            self.app.start_progressing()
            response = await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing()
            data = response.json()
            if response.status_code not in (200, 201):
                self.app.show_message(_("Error Getting Admin UI Roles!"), str(data), tobefocused=self.app.center_container)
                return

            self.admin_ui_roles_data = data
            self.adminui_update_roles()
            self.app.layout.focus(self.app.center_container)

        asyncio.ensure_future(coroutine())

    def adminui_update_roles(self,
        ) -> None:
        """update the current adminui-roles to server
        """

        data =[]

        for d in self.admin_ui_roles_data:
            data.append(
                [
                d.get('role'),
                d.get('description'),
                ]
            )

        if data:
            clients = JansVerticalNav(
                myparent=self.app,
                headers=['Role', 'Description',],
                preferred_size= [0,0],
                data=data,
                on_enter=self.edit_adminui_roles,
                on_display=self.app.data_display_dialog,
                on_delete= self.delete_adminui_roles,
                # get_help=(self.get_help,'AdminRole'),
                selectes=0,
                headerColor='class:outh-verticalnav-headcolor',
                entriesColor='class:outh-verticalnav-entriescolor',
                all_data=self.admin_ui_roles_data
            )
            self.app.layout.focus(clients)   # clients.focuse..!? TODO >> DONE
            self.config_data_container['accessroles'] = HSplit([
                clients,
            ])
            get_app().invalidate()
            self.app.layout.focus(clients)  ### it fix focuse on the last item deletion >> try on UMA-res >> edit_client_dialog >> oauth_update_uma_resources
        else:
            self.app.show_message(_("Oops"), _("No matching result"), tobefocused=self.app.center_container)

    def add_adminui_roles(self) -> None:
        """Method to display the dialog of adminui-roles
        """

        self.adminui_role = self.app.getTitledText(
            _("Role"), 
            name='role', 
            height=3, 
            style='class:dialog-titled-widget')

        self.adminui_role_description = self.app.getTitledText(
            _("Description"), 
            name='Description', 
            height=3, 
            style='class:dialog-titled-widget')

        def save(dialog: Dialog) -> None:
            role = self.adminui_role.me.text
            desc = self.adminui_role_description.me.text

            if desc :
                response = self.app.cli_object.process_command_by_id(
                        operation_id='add-adminui-role' ,
                        url_suffix='',
                        endpoint_args='',
                        data_fn='',
                        data={'role': '{}'.format(role), 'description': '{}'.format(desc)},
                        )
            else:
                return

            if response:
                self.get_adminui_roles()
                # self.future.set_result(DialogResult.ACCEPT)
                return True

            self.app.show_message(_("Error!"), _("An error ocurred while Addin role adminui:\n") + str(response.text))


        body = HSplit([self.adminui_role,self.adminui_role_description])
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
            style='class:dialog-titled-widget')

        self.adminui_role_deletable = self.app.getTitledCheckBox(
            "Deletable", 
            name='deletable', 
            checked= role_data.get('deletable', False),
            jans_help= "Default to False",
            style='class:outh-client-checkbox')

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

        dialog = self.app.get_confirm_dialog(_("Are you sure want to delete adminui_roles :")+"\n {} ?".format(kwargs['selected'][0]))

        async def coroutine(): ## Need to add editable
            focused_before = self.app.layout.current_window
            result = await self.app.show_dialog_as_float(dialog)
            try:
                self.app.layout.focus(focused_before)
            except:
                self.app.layout.focus(self.app.center_frame)

            if result.lower() == 'yes': ## should we delete the main roles?!
                cli_args = {'operation_id': 'delete-adminui-role', 'url_suffix':'adminUIRole:{}'.format(kwargs ['selected'][0])}
                self.app.start_progressing()
                response = await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
                self.app.stop_progressing()
                if response:
                    self.app.show_message(_("Error!"), str(response), tobefocused=self.app.center_container)
                else:
                    self.get_adminui_roles()

        asyncio.ensure_future(coroutine())
    
    def get_adminui_permissions(self,
        start_index: Optional[int]=0, 
        pattern: Optional[str]= ''
        ) -> None:
        """Method to get the adminui_permissions data from server

        Args:
        start_index (Optional[int], optional): This is flag for the adminui-roles pages. Defaults to 0.
        pattern (str, optional): endpoint arguments for the client data. Defaults to ''.
        """

        async def coroutine():
            cli_args = {'operation_id': 'get-all-adminui-permissions'}
            self.app.start_progressing()
            response = await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing()
            self.adminui_permissions_data = response.json()
            self.adminui_update_permissions(start_index, pattern)

        asyncio.ensure_future(coroutine())
        
    def adminui_update_permissions(self,
        start_index: Optional[int]=0, 
        pattern: Optional[str]= ''
        ) -> None:
        """update the current adminui_permissions data to server

        Args:.
            start_index (Optional[int], optional): This is flag for the adminui-roles pages. Defaults to 0.
            pattern (str, optional): endpoint arguments for the client data. Defaults to ''.
        """

        if not hasattr(self, 'adminui_permissions_data'):
            self.get_adminui_permissions(start_index, pattern)
            return

        data =[]
        if pattern:
            for k in self.adminui_permissions_data:
                if pattern.lower() in k.get('permission').lower():
                    data.append(
                        [
                        k.get('permission'),
                        k.get('defaultPermissionInToken'),
                        ]
                    )
        else:
            for d in self.adminui_permissions_data:
                data.append(
                    [
                    d.get('permission'),
                    d.get('defaultPermissionInToken'),
                    ]
                )

        if data:
            buttons = []
            if int(len(data)/self.app.entries_per_page) >=1:
                if start_index!=0:
                    handler_partial = partial(self.adminui_update_permissions, start_index-1, pattern)
                    prev_button = Button(_("Prev"), handler=handler_partial)
                    prev_button.window.jans_help = _("Displays previous %d entries") % self.app.entries_per_page
                    buttons.append(prev_button)

                if start_index< int(len(data)/ 20) :
                    handler_partial = partial(self.adminui_update_permissions, start_index+1, pattern)
                    next_button = Button(_("Next"), handler=handler_partial)
                    next_button.window.jans_help = _("Displays next %d entries") % self.app.entries_per_page
                    buttons.append(next_button)

            data_now = data[start_index*20:start_index*20+20]

            adminui_permissions = JansVerticalNav(
                myparent=self.app,
                headers=['permission', 'defaultPermissionInToken',],
                preferred_size= [0,0],
                data=data_now,
                on_enter=self.edit_adminui_permissions,
                on_display=self.app.data_display_dialog,
                on_delete=self.delete_adminui_permissions,
                # get_help=(self.get_help,'AdminRole'),
                selectes=0,
                headerColor='class:outh-verticalnav-headcolor',
                entriesColor='class:outh-verticalnav-entriescolor',
                all_data=self.adminui_permissions_data
            )

            self.app.layout.focus(adminui_permissions)
            self.config_data_container['permissions'] = HSplit([
                adminui_permissions,
                VSplit(buttons, padding=5, align=HorizontalAlign.CENTER)
            ])
            self.app.invalidate()
            self.app.layout.focus(adminui_permissions)
        else:
            self.app.show_message(_("Oops"), _("No matching result"), tobefocused=self.app.center_container)

    def add_adminui_permissions(self) -> None:
        """Method to display the dialog of adminui-roles
        """

        self.adminui_permission = self.app.getTitledText(
            _("Permission"), 
            name='permission', 
            height=3, 
            style='class:dialog-titled-widget')

        self.adminui_role_permissions= self.app.getTitledCheckBox(
            'DefaultPermissionInToken', 
            name='defaultpermissionInToken', 
            checked= False, 
            style='class:outh-client-checkbox')

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
                        'data': {'permission': '{}'.format(permission), 'defaultPermissionInToken': '{}'.format(defaultPermissionInToken)}
                        }
                    self.app.start_progressing()
                    response = await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
                    self.app.stop_progressing()

                    if response:
                        self.app.show_message(_("Error!"), _("An error ocurred while Addin role adminui permission:\n") + str(response.text))
                    else:
                        self.get_adminui_permissions()

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
            style='class:outh-client-checkbox')

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
                self.get_adminui_permissions()
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
                self.get_adminui_permissions()
                
            return result  ### TODO >> Role cannot be deleted. Please set ‘deletable’ property of role to true.

        asyncio.ensure_future(coroutine())
  
    def get_adminui_mapping(self, pattern: Optional[str]= '') -> None:
        """Method to get the adminui_permissions data from server
        """
        async def coroutine():
            cli_args = {'operation_id': 'get-all-adminui-role-permissions'}
            self.app.start_progressing()
            response = await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing()
            self.adminui_role_permissions_data = response.json()
            self.adminui_update_mapping(pattern)

        asyncio.ensure_future(coroutine())

    def adminui_update_mapping(self, pattern: Optional[str]= '') -> None:
        """update the current adminui_permissions data to server

        Args:
            pattern (str, optional): endpoint arguments for the client data. Defaults to ''.
        """

        if not hasattr(self, 'adminui_role_permissions_data'):
            self.get_adminui_mapping(pattern)
            return

        data =[]

        if pattern:
            for k in self.adminui_role_permissions_data:
                if pattern.lower() in k.get('role').lower():
                    data.append(
                        [
                        k.get('role'),
                        len(k.get('permissions')),
                        ]
                    )
        else:
            for d in self.adminui_role_permissions_data:
                data.append(
                    [
                    d.get('role'),
                    len(d.get('permissions')),
                    ]
                )

        if data:
            adminui_permissions = JansVerticalNav(
                myparent=self.app,
                headers=['role', 'permissions',],
                preferred_size= [0,0],
                data=data,
                on_enter=self.edit_adminui_mapping,
                on_display=self.app.data_display_dialog,
                # get_help=(self.get_help,'AdminRole'),
                selectes=0,
                headerColor='class:outh-verticalnav-headcolor',
                entriesColor='class:outh-verticalnav-entriescolor',
                all_data=self.adminui_role_permissions_data
            )
            self.app.layout.focus(adminui_permissions)   # clients.focuse..!? TODO >> DONE
            self.config_data_container['mapping'] = adminui_permissions
            self.app.invalidate()
            self.app.layout.focus(adminui_permissions)

        else:
            self.app.show_message(_("Oops"), _("No matching result"), tobefocused=self.app.center_container)

    def search_adminui_mapping(self, tbuffer:Buffer,) -> None:
        """This method handel the search for adminui_mapping

        Args:
            tbuffer (Buffer): Buffer returned from the TextArea widget > GetTitleText
        """

        self.adminui_update_mapping(tbuffer.text)

    def edit_adminui_mapping(self, **params: Any) -> None:
        """Method to display the dialog of adminui_mapping
        """
        role_data = params.get('data', [])
        permission = role_data.get('role')
        defaultPermissionInToken = []

        for i in role_data.get('permissions'):
            defaultPermissionInToken.append([i])


        self.adminui_role_permissions= JansVerticalNav(
                        myparent=self.app,
                        headers=['permission'],
                        preferred_size= [0],
                        data= defaultPermissionInToken,#'defaultPermissionInToken',
                        on_enter=self.edit_adminui_mapping,
                        on_display=self.app.data_display_dialog,
                        # get_help=(self.get_help,'AdminRole'),
                        selectes=0,
                        headerColor='red',
                        entriesColor='green',
                        all_data=defaultPermissionInToken
                    )   

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
                self.get_adminui_permissions()
                # self.future.set_result(DialogResult.ACCEPT)
                return True

            self.app.show_message(_("Error!"), _("An error ocurred while saving role adminui:\n") + str(response.text))

        body = HSplit([self.adminui_role_permissions])
        buttons = [Button(_("Cancel"))]
        dialog = JansGDialog(self.app, title='admin ui permissions', body=body, buttons=buttons, width=self.app.dialog_width-20)
        self.app.show_jans_dialog(dialog)

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

