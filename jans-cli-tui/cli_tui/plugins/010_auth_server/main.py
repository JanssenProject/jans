import time
import json
import asyncio
from functools import partial
from typing import Any, Optional
from prompt_toolkit.application.current import get_app
from prompt_toolkit.eventloop import get_event_loop
from prompt_toolkit.layout.containers import (
    HSplit,
    VSplit,
    HorizontalAlign,
    DynamicContainer,
    Window,
)
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.widgets import (
    Box,
    Button,
    Label,
    Dialog,
    TextArea
)
from prompt_toolkit.lexers import PygmentsLexer, DynamicLexer
from prompt_toolkit.buffer import Buffer
from prompt_toolkit.application import Application

from utils.static import DialogResult, cli_style, common_strings
from utils.utils import DialogUtils
from utils.utils import common_data
from utils.multi_lang import _

from wui_components.jans_nav_bar import JansNavBar
from wui_components.jans_vetrical_nav import JansVerticalNav
from wui_components.jans_drop_down import DropDownWidget
from wui_components.jans_cli_dialog import JansGDialog
from view_property import ViewProperty
from edit_client_dialog import EditClientDialog
from edit_scope_dialog import EditScopeDialog
from ssa import SSA
from agama import Agama

from prompt_toolkit.widgets import (
    HorizontalLine,
    VerticalLine,
)
QUESTION_TEMP = "\n {} ?"

class Plugin(DialogUtils):
    """This is a general class for plugins 
    """
    def __init__(
        self, 
        app: Application
        ) -> None:
        """init for Plugin class "oauth"

        Args:
            app (Generic): The main Application class
        """
        self.app = app
        self.pid = 'oauth'
        self.name = '[A]uth Server'
        self.search_text= None
        self.oauth_update_properties_start_index = 0
        self.ssa = SSA(app)
        self.agama = Agama(app)
        self.app_configuration = {}
        self.oauth_containers = {}

        self.oauth_prepare_navbar()
        self.oauth_prepare_containers()
        self.oauth_nav_selection_changed(self.nav_bar.navbar_entries[0][0])
        

    def init_plugin(self) -> None:
        """The initialization for this plugin
        """

        self.app.create_background_task(self.get_appconfiguration())
        self.schema = self.app.cli_object.get_schema_from_reference('', '#/components/schemas/AppConfiguration')

        if not hasattr(common_data, 'scopes'):
            self.app.create_background_task(self.retrieve_sopes())

    async def get_appconfiguration(self) -> None:
        'Coroutine for getting application configuration.'

        cli_args = {'operation_id': 'get-properties'}
        response = await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)

        if response.status_code not in (200, 201):
            self.app.show_message(_("Error getting Jans configuration"), str(response.text), tobefocused=self.app.center_frame)
            return

        self.app_configuration = response.json()
        self.oauth_logging()


    async def retrieve_sopes(self) -> None:
        """asyncio corotune for retreiving scopes
        """
        self.app.logger.debug("retreiving scopes")
        cli_args = {'operation_id': 'get-oauth-scopes', 'endpoint_args': 'limit:200,startIndex:0'}
        response = await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
        common_data.scopes = response.json()['entries']
        self.app.logger.debug("scopes retreived")

    def process(self):
        """No pre-processing for this plugin.
        """

    def set_center_frame(self) -> None:
        """center frame content
        """
        self.app.center_container = self.oauth_main_container

    def oauth_prepare_containers(self) -> None:
        """prepare the main container (tabs) for the current Plugin 
        """

        self.oauth_data_container = {
            'clients': HSplit([],width=D()),
            'scopes': HSplit([],width=D()),
            'keys': HSplit([],width=D()),
            'properties': HSplit([],width=D()),
            'logging': HSplit([],width=D()),
        }

        self.oauth_main_area = HSplit([],width=D())

        self.oauth_containers['scopes'] = HSplit([
                    VSplit([
                        self.app.getButton(text=_("Get Scopes"), name='oauth:scopes:get', jans_help=_("Retreive first {} Scopes").format(self.app.entries_per_page), handler=self.oauth_get_scopes),
                        self.app.getTitledText(_("Search"), name='oauth:scopes:search', jans_help=_(common_strings.enter_to_search), accept_handler=self.search_scope,style='class:outh_containers_scopes.text'),
                        self.app.getButton(text=_("Add Scope"), name='oauth:scopes:add', jans_help=_("To add a new scope press this button"), handler=self.add_scope),
                        ],
                        padding=3,
                        width=D(),
                    ),
                    DynamicContainer(lambda: self.oauth_data_container['scopes'])
                    ],style='class:outh_containers_scopes')

        self.oauth_containers['clients'] = HSplit([
                    VSplit([
                        self.app.getButton(text=_("Get Clients"), name='oauth:clients:get', jans_help=_("Retreive first {} OpenID Connect clients").format(self.app.entries_per_page), handler=self.oauth_update_clients),
                        self.app.getTitledText(_("Search"), name='oauth:clients:search', jans_help=_(common_strings.enter_to_search), accept_handler=self.search_clients,style='class:outh_containers_clients.text'),
                        self.app.getButton(text=_("Add Client"), name='oauth:clients:add', jans_help=_("To add a new client press this button"), handler=self.add_client),

                        ],
                        padding=3,
                        width=D(),
                        ),
                        DynamicContainer(lambda: self.oauth_data_container['clients'])
                     ],style=cli_style.container)


        self.oauth_containers['keys'] = HSplit([
                    VSplit([
                        self.app.getButton(text=_("Get Keys"), name='oauth:keys:get', jans_help=_("Retreive Auth Server keys"), handler=self.oauth_get_keys),
                        ],
                        padding=3,
                        width=D(),
                        ),
                        DynamicContainer(lambda: self.oauth_data_container['keys'])
                     ], style=cli_style.container)

        self.oauth_containers['properties'] = HSplit([
                    VSplit([
                            self.app.getTitledText(
                                _("Search"), 
                                name='oauth:properties:search', 
                                jans_help=_(common_strings.enter_to_search), 
                                accept_handler=self.search_properties,
                                style='class:outh_containers_scopes.text'
                            ),
                            self.app.getButton(
                                _("Add Property"),
                                name='oauth:properties:add',
                                jans_help=_("Press this button to add a missing preperty"),
                                handler=self.add_property
                            ),
                        ],
                    padding=3,
                    width=D(),
                    ),
                    DynamicContainer(lambda: self.oauth_data_container['properties'])
                    ],style='class:outh_containers_scopes')

        self.oauth_containers['ssa'] = self.ssa.main_container
        self.oauth_containers['agama'] = self.agama.main_container
        self.oauth_containers['logging'] = DynamicContainer(lambda: self.oauth_data_container['logging'])

        self.oauth_main_container = HSplit([
                                        Box(self.nav_bar.nav_window, style='class:sub-navbar', height=1),
                                        DynamicContainer(lambda: self.oauth_main_area),
                                        ],
                                    height=D(),
                                    style=cli_style.container
                                    )

    def oauth_prepare_navbar(self) -> None:
        """prepare the navbar for the current Plugin 
        """
        self.nav_bar = JansNavBar(
                    self.app,
                    entries=[('clients', 'C[l]ients'), ('scopes', 'Sc[o]pes'), ('keys', '[K]eys'), ('defaults', '[D]efaults'), ('properties', 'Properti[e]s'), ('logging', 'Lo[g]ging'), ('ssa', '[S]SA'), ('agama', 'Aga[m]a')],
                    selection_changed=self.oauth_nav_selection_changed,
                    select=0,
                    jans_name='oauth:nav_bar'
                    )

    def oauth_nav_selection_changed(
        self, 
        selection
        ) -> None:
        """This method for the selection change

        Args:
            selection (str): the current selected tab
        """
        if selection in self.oauth_containers:
            if selection == 'properties':
                self.oauth_update_properties(tofocus=False)
            self.oauth_main_area = self.oauth_containers[selection]
        else:
            self.oauth_main_area = self.app.not_implemented

    def oauth_update_clients(
        self,
        start_index: Optional[int]= 0, 
        pattern: Optional[str]= '',
        ) -> None:
        """update the current clients data to server

        Args:
            start_index (Optional[int], optional): This is flag for the clients page. Defaults to 0.
            pattern (Optional[str], optional):endpoint arguments for the client data. Defaults to ''.
        """

        async def coroutine():
            endpoint_args ='limit:{},startIndex:{}'.format(self.app.entries_per_page, start_index)
            if pattern:
                endpoint_args +=',pattern:'+pattern
            cli_args = {'operation_id': 'get-oauth-openid-clients', 'endpoint_args': endpoint_args}
            self.app.start_progressing(_("Retreiving clients from server..."))
            response = await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing()

            if response.status_code not in (200, 201):
                self.app.show_message(_("Error getting clients"), str(response.text),tobefocused=self.oauth_containers['clients'])
                return

            try:
                result = response.json()
            except Exception:
                self.app.show_message(_("Error getting clients"), str(response.text),tobefocused=self.oauth_containers['clients'])
                return

            data =[]

            for d in result.get('entries', []):
                data.append(
                    [
                    d['inum'],
                    d.get('clientName', ''),
                    ','.join(d.get('grantTypes', [])),
                    d.get('subjectType', '') 
                    ]
                )

            if data:
                clients = VSplit([
                    Label(text=" ",width=1),
                    JansVerticalNav(
                        myparent=self.app,
                        headers=['Client ID', 'Client Name', 'Grant Types', 'Subject Type'],
                        preferred_size= [0,0,30,0],
                        data=data,
                        on_enter=self.edit_client,
                        on_display=self.app.data_display_dialog,
                        on_delete=self.delete_client,
                        get_help=(self.get_help,'Client'),
                        selectes=0,
                        headerColor=cli_style.navbar_headcolor,
                        entriesColor=cli_style.navbar_entriescolor,
                        all_data=result['entries']
                    )
                ])
                buttons = []
                if start_index > 0:
                    handler_partial = partial(self.oauth_update_clients, start_index-self.app.entries_per_page, pattern)
                    prev_button = Button(_("Prev"), handler=handler_partial)
                    prev_button.window.jans_help = _("Retreives previous %d entries") % self.app.entries_per_page
                    buttons.append(prev_button)
                if  result['start'] + self.app.entries_per_page <  result['totalEntriesCount']:
                    handler_partial = partial(self.oauth_update_clients, start_index+self.app.entries_per_page, pattern)
                    next_button = Button(_("Next"), handler=handler_partial)
                    next_button.window.jans_help = _("Retreives next %d entries") % self.app.entries_per_page
                    buttons.append(next_button)

                self.app.layout.focus(clients)
                self.oauth_data_container['clients'] = HSplit([
                    clients,
                    VSplit(buttons, padding=5, align=HorizontalAlign.CENTER)
                ])

                get_app().invalidate()
                self.app.layout.focus(clients)  ### it fix focuse on the last item deletion >> try on UMA-res >> edit_client_dialog >> oauth_update_uma_resources

            else:
                self.app.show_message(_("Oops"), _(common_strings.no_matching_result),tobefocused = self.oauth_containers['clients'])

        asyncio.ensure_future(coroutine())


    def get_scopes(self, client_data) -> None:

        async def coroutine():
            cli_args = {'operation_id': 'get-oauth-scopes', 'endpoint_args':'limit:200,startIndex:0'}
            self.app.start_progressing(_("Retreiving client Scopes..."))
            response = await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing()

            if response.status_code not in (200, 201):
                self.app.show_message(_("Error getting client Scopes"), str(response.text),tobefocused=self.oauth_containers['clients'])
                return

            try:
                result = response.json()
            except Exception:
                self.app.show_message(_("Error getting client Scopes"), str(response.text),tobefocused=self.oauth_containers['clients'])
                return

            data_display_name =[]
            data_base_dn =[]

            for d in result.get('entries', []):
                data_display_name.append(d.get('displayName',d.get('baseDn')))
                data_base_dn.append(d.get('baseDn'))



            for client_num in range(len(client_data)):

                for scope_dn_num in range(len(client_data[client_num]['scopes'])):
                    if client_data[client_num]['scopes'][scope_dn_num] in data_base_dn:

                        index = data_base_dn.index(client_data[client_num]['scopes'][scope_dn_num])

                        client_data[client_num]['scopes'][scope_dn_num] = [data_display_name[index],data_base_dn[index].replace(',ou=scopes,o=jans','')]


        asyncio.ensure_future(coroutine())
        return client_data

    def delete_client(self, **kwargs: Any) -> None:
        """This method for the deletion of the clients data

        Args:
            selected (_type_): The selected Client
            event (_type_): _description_

        Returns:
            str: The server response
        """

        dialog = self.app.get_confirm_dialog(_("Are you sure want to delete client inum:")+QUESTION_TEMP.format(kwargs ['selected'][0]))

        async def coroutine():
            focused_before = self.app.layout.current_window
            result = await self.app.show_dialog_as_float(dialog)
            try:
                self.app.layout.focus(focused_before)
            except Exception:
                self.app.stop_progressing()
                self.app.layout.focus(self.app.center_frame)

            if result.lower() == 'yes':
                result = self.app.cli_object.process_command_by_id(
                    operation_id='delete-oauth-openid-client-by-inum',
                    url_suffix='inum:{}'.format(kwargs ['selected'][0]),
                    endpoint_args='',
                    data_fn='',
                    data={}
                )
                # TODO Need to do `self.oauth_get_clients()` only if clients list is not empty
                self.app.stop_progressing()
                self.oauth_update_clients()
                
            return result

        asyncio.ensure_future(coroutine())

    def oauth_get_scopes(
            self, 
            start_index: Optional[int]= 0,
            pattern: Optional[str]= '',
            ) -> None:
        """update the current Scopes data to server

        Args:
            start_index (int, optional): add Button("Prev") and Button("Next")to the layout (which page am I in). Defaults to 0.
            pattern (Optional[str], optional):endpoint arguments for the Scopes data. Defaults to ''.
        """

        async def coroutine():

            endpoint_args ='withAssociatedClients:true,limit:{},startIndex:{}'.format(self.app.entries_per_page, start_index)
            if pattern:
                endpoint_args +=',pattern:'+pattern

            cli_args = {'operation_id': 'get-oauth-scopes', 'endpoint_args':endpoint_args}
            self.app.start_progressing(_("Retreiving scopes from server..."))
            response = await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing(_("Retreived"))

            try:
                result = response.json()
            except Exception:
                self.app.show_message(_("Error getting response"), str(response))
                return

            data =[]

            for d in result.get('entries', []): 
                data.append(
                    [
                    d['id'],
                    d.get('description', ''),
                    d.get('scopeType',''),   ## some scopes have no scopetypr
                    d['inum']
                    ]
                )

            if data:

                scopes =VSplit([
                    Label(text=" ",width=1),
                    JansVerticalNav(
                            myparent=self.app,
                            headers=['id', 'Description', 'Type','inum'],
                            preferred_size= [30,40,8,12],
                            data=data,
                            on_enter=self.edit_scope_dialog,
                            on_display=self.app.data_display_dialog,
                            on_delete=self.delete_scope,
                            get_help=(self.get_help,'Scope'),
                            selectes=0,
                            headerColor=cli_style.navbar_headcolor,
                            entriesColor=cli_style.navbar_entriescolor,
                            all_data=result['entries']
                        )
                ])

                buttons = []
                if start_index > 0:
                    handler_partial = partial(self.oauth_get_scopes, start_index-self.app.entries_per_page, pattern)
                    prev_button = Button(_("Prev"), handler=handler_partial)
                    prev_button.window.jans_help = _("Retreives previous %d entries") % self.app.entries_per_page
                    buttons.append(prev_button)
                if  result['start'] + self.app.entries_per_page <  result['totalEntriesCount']:
                    handler_partial = partial(self.oauth_get_scopes, start_index+self.app.entries_per_page, pattern)
                    next_button = Button(_("Next"), handler=handler_partial)
                    next_button.window.jans_help = _("Retreives next %d entries") % self.app.entries_per_page
                    buttons.append(next_button)

                self.app.layout.focus(scopes)   # clients.focuse..!? TODO >> DONE
                self.oauth_data_container['scopes'] = HSplit([
                    scopes,
                    VSplit(buttons, padding=5, align=HorizontalAlign.CENTER)
                ])

                get_app().invalidate()
                self.app.layout.focus(scopes)  ### it fix focuse on the last item deletion >> try on UMA-res >> edit_client_dialog >> oauth_update_uma_resources

            else:
                self.app.show_message(_("Oops"), _(common_strings.no_matching_result),tobefocused = self.oauth_containers['scopes'])

        asyncio.ensure_future(coroutine())


    def add_property(self):
        missing_properties = []

        for prop in self.schema['properties']:
            if prop not in self.app_configuration:
                missing_properties.append(prop)
        missing_properties.sort()
        missing_properties_data = [ [prop] for prop in missing_properties ]

        def add_property(**params: Any) -> None:
            self.add_property_dialog.future.set_result('add_property')
            prop_name = params['passed'][0]
            prop_val = ''
            prop_type = self.schema['properties'][prop_name]['type']

            if prop_type == 'string':
                prop_val = ''
            elif prop_type == 'array':
                prop_val = []

            passed = [prop_name, prop_val]

            self.view_property(passed=passed, op_type='add')

        properties = VSplit([
                Label(text=" ",width=1),
                JansVerticalNav(
                        myparent=self.app,
                        headers=['Property Name'],
                        preferred_size=[0],
                        data=missing_properties_data,
                        on_enter=add_property,
                        get_help=(self.get_help,'AppConfiguration'),
                        selectes=0,
                        headerColor=cli_style.navbar_headcolor,
                        entriesColor=cli_style.navbar_entriescolor,
                        all_data=missing_properties
                    )
        ])

        body = HSplit([properties])
        buttons = [Button(_("Cancel"))]
        self.add_property_dialog = JansGDialog(self.app, title=_("Select Property"), body=body, buttons=buttons)
        self.app.show_jans_dialog(self.add_property_dialog)


    def oauth_update_properties(
        self,
        start_index: Optional[int]= 0,
        pattern: Optional[str]= '',
        tofocus:Optional[bool]=True,
        ) -> None:

        """update the current clients data to server

        Args:
            start_index (int, optional): add Button("Prev") and Button("Next")to the layout (which page am I in). Defaults to 0.
            pattern (str, optional): endpoint arguments for the client data. Defaults to ''.
            tofocus (Optional[bool], optional): To focus the properties or not (used to not focus on navigation). Defaults to True.
        """

        self.oauth_update_properties_start_index = start_index
        # ------------------------------------------------------------------------------- #
        # ----------------------------------- Search ------------------------------------ #
        # ------------------------------------------------------------------------------- #

        data =[]
        
        if pattern:
            for k in self.app_configuration:
                if pattern.lower() in k.lower():
                    data.append([k, self.app_configuration[k]])
        else:
            for d in self.app_configuration:
                data.append([d, self.app_configuration[d]])

        # ------------------------------------------------------------------------------- #
        # --------------------------------- View Data ----------------------------------- #
        # ------------------------------------------------------------------------------- #


        if data:
            data.sort()
            buttons = []

            if len(data) > 20:

                if start_index!=0:
                    handler_partial = partial(self.oauth_update_properties, start_index-1, pattern)
                    prev_button = Button(_("Prev"), handler=handler_partial)
                    prev_button.window.jans_help = _("Displays previous %d entries") % self.app.entries_per_page
                    buttons.append(prev_button)

                if start_index< int(len(data)/ 20) :
                    handler_partial = partial(self.oauth_update_properties, start_index+1, pattern)
                    next_button = Button(_("Next"), handler=handler_partial)
                    next_button.window.jans_help = _("Displays next %d entries") % self.app.entries_per_page
                    buttons.append(next_button)

            data_now = data[start_index*20:start_index*20+20]

            properties =VSplit([
                Label(text=" ",width=1),
                JansVerticalNav(
                    myparent=self.app,
                    headers=['Property Name', 'Property Value'],
                    preferred_size= [0,0],
                    data=data_now,
                    on_enter=self.view_property,
                    on_display=self.properties_display_dialog,
                    get_help=(self.get_help,'AppConfiguration'),
                    selectes=0,
                    headerColor=cli_style.navbar_headcolor,
                    entriesColor=cli_style.navbar_entriescolor,
                    all_data=list(self.app_configuration.values())
            )
            ])


            self.oauth_data_container['properties'] = HSplit([
                properties,
                VSplit(buttons, padding=5, align=HorizontalAlign.CENTER)
            ])
            self.app.invalidate()
            if tofocus:
                self.app.layout.focus(properties)
        else:
            self.app.show_message(_("Oops"), _(common_strings.no_matching_result), tobefocused= self.oauth_containers['properties'])

    def properties_display_dialog(self, **params: Any) -> None:
        """Display the properties as Text
        """
        data_property, data_value = params['selected'][0], params['selected'][1]
        body = HSplit([
                TextArea(
                    lexer=DynamicLexer(lambda: PygmentsLexer.from_filename('.json', sync_from_start=True)),
                    scrollbar=True,
                    line_numbers=True,
                    multiline=True,
                    read_only=True,
                    text=str(json.dumps(data_value, indent=2)),
                    style='class:jans-main-datadisplay.text'
                )
            ],style='class:jans-main-datadisplay')

        dialog = JansGDialog(self.app, title=data_property, body=body)

        self.app.show_jans_dialog(dialog)

    def view_property(self, **params: Any) -> None:
        """This method view the properties in Dialog to edit
        """

        selected_line_data = params['passed']    ##self.uma_result 

        title = _("Edit property")

        dialog = ViewProperty(app=self.app, parent=self, title=title, data=selected_line_data, op_type=params.get('op_type', 'replace'))

        self.app.show_jans_dialog(dialog)
 
    def search_properties(self, tbuffer:Buffer) -> None:
        """This method handel the search for Properties

        Args:
            tbuffer (Buffer): Buffer returned from the TextArea widget > GetTitleText
        """
        self.app.logger.debug("tbuffer="+str(tbuffer))
        self.app.logger.debug("type tbuffer="+str(type(tbuffer)))
        self.search_text=tbuffer.text

        self.oauth_update_properties(0, tbuffer.text)

    def oauth_update_keys(self) -> None:
        """update the current Keys fromserver
        """

        data =[]

        for d in self.jwks_keys.get('keys', []): 
            try:
                gmt = time.gmtime(int(d['exp'])/1000)
                exps = time.strftime("%d %b %Y %H:%M:%S", gmt)
            except Exception:
                exps = d.get('exp', '')
            data.append(
                [
                d['name'],
                exps,
                d['kid']
                ]
            )

        if data:

            keys = VSplit([
                Label(text=" ",width=1),
                JansVerticalNav(
                    myparent=self.app,
                    headers=['Name', 'Expiration','Kid'],
                    data=data,
                    preferred_size=[0,0,0],
                    on_display=self.app.data_display_dialog,
                    selectes=0,
                    headerColor=cli_style.navbar_headcolor,
                    entriesColor=cli_style.navbar_entriescolor,
                    all_data=self.jwks_keys['keys']
                )
            ])

            self.oauth_data_container['keys'] = HSplit([keys])
            get_app().invalidate()
            self.app.layout.focus(keys)

        else:
            self.app.show_message(_("Oops"), _("No JWKS keys were found"), tobefocused=self.app.center_frame)

    def oauth_get_keys(self) -> None:
        """Method to get the Keys from server
        """

        async def coroutine():
            cli_args = {'operation_id': 'get-config-jwks'}
            self.app.start_progressing("Retreiving JWKS keys...")
            response = await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing()
            self.jwks_keys = response.json()
            self.oauth_update_keys()

        asyncio.ensure_future(coroutine())

    def edit_scope_dialog(self, **params: Any) -> None:
        """This Method show the scopes dialog for edit
        """
        selected_line_data = params['data']

        dialog = EditScopeDialog(self.app, title=_("Edit Scopes"), data=selected_line_data, save_handler=self.save_scope)
        self.app.show_jans_dialog(dialog)

    def edit_client(self, **params: Any) -> None:
        """This Method show the scopes dialog for edit
        """
        selected_line_data = params['data']
        title = _("Edit Clients")

        self.edit_client_dialog = EditClientDialog(
                                parent=self.app,
                                title=title,
                                data=selected_line_data,
                                save_handler=self.save_client,
                                delete_uma_resource=self.delete_uma_resource
                                )

        self.app.show_jans_dialog(self.edit_client_dialog)

    def save_client(self, dialog: Dialog) -> None:
        """This method to save the client data to server

        Args:
            dialog (_type_): the main dialog to save data in

        Returns:
            _type_: bool value to check the status code response
        """

        async def coroutine():
            self.app.start_progressing(_("Saving clinet ..."))
            operation_id='put-oauth-openid-client' if dialog.data.get('inum') else 'post-oauth-openid-client'
            cli_args = {'operation_id': operation_id, 'data': dialog.data}
            response = await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)

            dialog.future.set_result(DialogResult.ACCEPT)
            self.app.stop_progressing()

            if response.status_code in (200, 201):
                self.oauth_update_clients()
            else:
                self.app.show_message(_("Error!"), _("An error ocurred while saving client:\n") + str(response.text), tobefocused=self.app.center_frame)

        asyncio.ensure_future(coroutine())


    def save_scope(self, dialog: Dialog) -> None:
        """This method to save the client data to server

        Args:
            dialog (_type_): the main dialog to save data in

        Returns:
            _type_: bool value to check the status code response
        """

        async def coroutine():
            operation_id='put-oauth-scopes' if dialog.data.get('inum') else 'post-oauth-scopes'
            cli_args = {'operation_id': operation_id, 'data': dialog.data}
            self.app.start_progressing()
            response = await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing()
            dialog.future.set_result(DialogResult.ACCEPT)
            if response.status_code == 500:
                self.app.show_message(_('Error'), response.text + '\n' + response.reason)
            else:
                self.oauth_get_scopes()

        asyncio.ensure_future(coroutine())

    def search_scope(self, tbuffer:Buffer) -> None:
        """This method handel the search for Scopes

        Args:
            tbuffer (Buffer): Buffer returned from the TextArea widget > GetTitleText
        """

        self.oauth_get_scopes(pattern=tbuffer.text)

    def search_clients(self, tbuffer:Buffer) -> None:
        """This method handel the search for Clients

        Args:
            tbuffer (Buffer): Buffer returned from the TextArea widget > GetTitleText
        """

        self.oauth_update_clients(pattern=tbuffer.text)

    def add_scope(self) -> None:
        """Method to display the dialog of Scopes (Add New)
        """
        dialog = EditScopeDialog(self.app, title=_("Add New Scope"), data={}, save_handler=self.save_scope)
        self.app.show_jans_dialog(dialog)

    def add_client(self) -> None:
        """Method to display the dialog of clients (Add New)
        """
        dialog = EditClientDialog(self.app, title=_("Add Client"), data={}, save_handler=self.save_client)
        self.app.show_jans_dialog(dialog)

    def get_help(self, **kwargs: Any):
        """This method get focused field Description to display on statusbar
        """

        self.app.logger.debug("get_help: "+str(kwargs['data']))
        self.app.logger.debug("get_help: "+str(kwargs['scheme']))
        schema = self.app.cli_object.get_schema_from_reference('', '#/components/schemas/{}'.format(str(kwargs['scheme'])))

        self.app.logger.debug("schema: "+str(schema))
        if kwargs['scheme'] == 'AppConfiguration':
            self.app.status_bar_text= self.app.get_help_from_schema(schema, kwargs['data'][0])
        elif kwargs['scheme'] == 'Client':
            self.app.status_bar_text= "Client Name: "+kwargs['data'][1]
        elif kwargs['scheme'] == 'Scope':
            self.app.status_bar_text= kwargs['data'][1]
        elif kwargs['scheme'] == 'Keys':
            self.app.status_bar_text= kwargs['data'][1]
            self.app.logger.debug("kwargs['data']: "+str(kwargs['data']))

    def delete_scope(self, **kwargs: Any):
        """This method for the deletion of the clients data

        Returns:
            str: The server response
        """

        dialog = self.app.get_confirm_dialog(_("Are you sure want to delete scope inum:")+QUESTION_TEMP.format(kwargs ['selected'][3]))
        async def coroutine():
            focused_before = self.app.layout.current_window
            result = await self.app.show_dialog_as_float(dialog)
            try:
                self.app.layout.focus(focused_before)
            except Exception:
                self.app.layout.focus(self.app.center_frame)

            if result.lower() == 'yes':
                result = self.app.cli_object.process_command_by_id(
                    operation_id='delete-oauth-scopes-by-inum',
                    url_suffix='inum:{}'.format(kwargs['selected'][3]),
                    endpoint_args='',
                    data_fn='',
                    data={}
                )
                self.oauth_get_scopes()
            return result

        asyncio.ensure_future(coroutine())

    def delete_uma_resource(self, **kwargs: Any):
        """This method for the deletion of the UMAresource

        Returns:
            str: The server response
        """
        dialog = self.app.get_confirm_dialog(_("Are you sure want to delete UMA resoucres with id:")+QUESTION_TEMP.format(kwargs ['selected'][0]))
        async def coroutine():
            focused_before = self.app.layout.current_window
            result = await self.app.show_dialog_as_float(dialog)
            try:
                self.app.layout.focus(focused_before)
            except Exception:
                self.app.layout.focus(self.edit_client_dialog)

            if result.lower() == 'yes':
                result = self.app.cli_object.process_command_by_id(
                    operation_id='delete-oauth-uma-resources-by-id',
                    url_suffix='id:{}'.format(kwargs['selected'][0]),
                    endpoint_args='',
                    data_fn=None,
                    data={}
                    )
                self.edit_client_dialog.oauth_get_uma_resources()

            return result

        asyncio.ensure_future(coroutine())

    def oauth_logging(self) -> None:
        """This method for the Auth Login
        """
        self.oauth_data_container['logging'] = HSplit([
                        self.app.getTitledWidget(
                                _('Log Level'),
                                name='loggingLevel',
                                widget=DropDownWidget(
                                    values=[('TRACE', 'TRACE'), ('DEBUG', 'DEBUG'), ('INFO', 'INFO'), ('WARN', 'WARN'), ('ERROR', 'ERROR'), ('FATAL', 'FATAL'), ('OFF', 'OFF')],
                                    value=self.app_configuration.get('loggingLevel')
                                    ),
                                jans_help=self.app.get_help_from_schema(self.schema, 'loggingLevel'),
                                ),
                        self.app.getTitledWidget(
                                _('Log Layout'),
                                name='loggingLayout',
                                widget=DropDownWidget(
                                    values=[('text', 'text'), ('json', 'json')],
                                    value=self.app_configuration.get('loggingLayout')
                                    ),
                                jans_help=self.app.get_help_from_schema(self.schema, 'loggingLayout'),
                                ),
                        self.app.getTitledCheckBox(
                            _("Enable HTTP Logging"), 
                            name='httpLoggingEnabled',
                            checked=self.app_configuration.get('httpLoggingEnabled'),
                            jans_help=self.app.get_help_from_schema(self.schema, 'httpLoggingEnabled'),
                            style=cli_style.check_box
                            ),
                        self.app.getTitledCheckBox(
                            _("Disable JDK Logger"), 
                            name='disableJdkLogger',
                            checked=self.app_configuration.get('disableJdkLogger'),
                            jans_help=self.app.get_help_from_schema(self.schema, 'disableJdkLogger'),
                            style=cli_style.check_box
                            ),
                        self.app.getTitledCheckBox(
                            _("Enable Oauth Audit Logging"), 
                            name='enabledOAuthAuditLogging',
                            checked=self.app_configuration.get('enabledOAuthAuditLogging'),
                            jans_help=self.app.get_help_from_schema(self.schema, 'enabledOAuthAuditLogging'),
                            style=cli_style.check_box
                            ),
                        Window(height=1),
                        HSplit([
                            self.app.getButton(text=_("Save Logging"), name='oauth:logging:save', jans_help=_("Save Auth Server logging configuration"), handler=self.save_logging),
                            Window(width=100),
                            ])
                     ], style=cli_style.container, width=D())

    def save_logging(self) -> None:
        """This method to Save the Auth Login to server
        """
        mod_data = self.make_data_from_dialog({'logging':self.oauth_data_container['logging']})
        pathches = []
        for key_ in mod_data:
            if self.app_configuration.get(key_) != mod_data[key_]:
                pathches.append({'op':'replace', 'path': key_, 'value': mod_data[key_]})

        if pathches:
            response = self.app.cli_object.process_command_by_id(
                operation_id='patch-properties',
                url_suffix='',
                endpoint_args='',
                data_fn=None,
                data=pathches
                )
            self.schema = response
            body = HSplit([Label(_("Jans authorization server application configuration logging properties were saved."))])

            buttons = [Button(_("Ok"))]
            dialog = JansGDialog(self.app, title=_("Confirmation"), body=body, buttons=buttons)
            async def coroutine():
                focused_before = self.app.layout.current_window
                await self.app.show_dialog_as_float(dialog)
                try:
                    self.app.layout.focus(focused_before)
                except Exception:
                    self.app.layout.focus(self.app.center_frame)

            asyncio.ensure_future(coroutine())
