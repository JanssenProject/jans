import json
from asyncio import Future
from typing import OrderedDict
import prompt_toolkit
from prompt_toolkit.widgets import Button, TextArea
from prompt_toolkit.application.current import get_app
from prompt_toolkit.layout.dimension import D
from static import DialogResult
from wui_components.jans_dialog import JansDialog

from prompt_toolkit.layout.containers import (
    VSplit,
    DynamicContainer,
)
from prompt_toolkit.widgets import (
    Button,
    Label,
    TextArea,
    Dialog,

)

from cli import config_cli
from prompt_toolkit.layout.containers import (
    ConditionalContainer,
    Float,
    HSplit,
    VSplit,
    VerticalAlign,
    DynamicContainer,
    FloatContainer,
    Window
)
from prompt_toolkit.widgets import (
    Box,
    Button,
    Frame,
    Label,
    RadioList,
    TextArea,
    CheckboxList,
    Shadow,
)

from wui_components.jans_side_nav_bar import JansSideNavBar
from utils import DialogUtils

from wui_components.jans_cli_dialog import JansGDialog

from wui_components.jans_drop_down import DropDownWidget

from multi_lang import _


class EditScopeDialog(DialogUtils):
    """The Main scope Dialog that contain every thing related to The scope
    """
    def __init__(self, parent, title, data, save_handler=None):
        """init for `EditscopeDialog`, inherits from two diffrent classes `JansGDialog` and `DialogUtils`
            
        JansGDialog (dialog): This is the main dialog Class Widget for all Jans-cli-tui dialogs except custom dialogs like dialogs with navbar
        DialogUtils (methods): Responsable for all `make data from dialog` and `check required fields` in the form for any Edit or Add New
        
        Args:
            parent (widget): This is the parent widget for the dialog, to access `Pageup` and `Pagedown`
            title (str): The Main dialog title
            data (list): selected line data 
            button_functions (list, optional): Dialog main buttons with their handlers. Defaults to [].
            save_handler (method, optional): handler after closing the dialog. Defaults to None.
        """
        self.myparent = parent
        self.save_handler = save_handler
        self.data = data
        self.title=title
        self.future = Future()

        self.showInConfigurationEndpoint = self.data.get('attributes',{}).get('showInConfigurationEndpoint','')
        self.defaultScope = self.data.get('defaultScope','')
        self.prepare_tabs()

        self.create_window()
        self.sope_type = 'oauth'

    def scope_selection_changed(self, cb):
        self.sope_type = cb.current_value

    def cancel(self):
        self.future.set_result(DialogResult.CANCEL)

    def save(self):
        self.myparent.logger.debug('SAVE SCOPE')

        data = {}

        for item in self.dialog.body.children + self.alt_tabs[self.sope_type].children:
            item_data = self.get_item_data(item)
            if item_data:
                data[item_data['key']] = item_data['value']

        self.myparent.logger.debug('DATA: ' + str(data))
        self.data = data    
        if 'attributes' in self.data.keys():    
            self.data['attributes'] = {'showInConfigurationEndpoint':self.data['attributes']}


        self.myparent.logger.debug('handler: '+str(self.save_handler))
        close_me = True
        if self.save_handler:
            close_me = self.save_handler(self)
        if close_me:
            self.future.set_result(DialogResult.ACCEPT)

    def create_window(self):
        """This method creat the dialog it self
        Todo:
            * Change `max_data_str` to be dynamic 
        """


        self.dialog = Dialog(
            title=self.title,
            body = HSplit([
                        self.myparent.getTitledRadioButton(
                                _("Scope Type"),  ## TODO need to be handled
                                name='scopeType', 
                                current_value=self.data.get('scopeType'),
                                values=[('oauth', 'Auth'), ('openid', 'OpenID'), ('dynamic', 'Dynamic'), ('spontaneous', 'Spontaneous'), ('uma', 'UMA')], 
                                on_selection_changed=self.scope_selection_changed,
                                style='green'),

                        self.myparent.getTitledText(_("id"), name='id', value=self.data.get('id',''), style='green'),
                        self.myparent.getTitledText(_("inum"), name='inum', value=self.data.get('inum',''), style='green',read_only=True,),
                        self.myparent.getTitledText(_("Display Name"), name='displayName', value=self.data.get('displayName',''), style='green'),
                        self.myparent.getTitledText(_("Description"), name='description', value=self.data.get('description',''), style='green'),
                        DynamicContainer(lambda: self.alt_tabs[self.sope_type]),

                        ], width=self.myparent.dialog_width
                    ),
            buttons = [Button(_("Save"), handler=self.save), Button(_("Cancel"), self.cancel)],
        )

    def set_scope_type(self, scope_type):
        self.sope_type = scope_type

    def prepare_tabs(self):
        """Prepare the tabs for Edil scope Dialogs
        """
        self.myparent.logger.debug('Data: '+str(self.data))


        schema = self.myparent.cli_object.get_schema_from_reference('#/components/schemas/Scope')

        self.alt_tabs = {}


        self.alt_tabs['oauth'] = HSplit([
                            self.myparent.getTitledCheckBox(
                                    _("Default Scope"),
                                    name='defaultScope',
                                    checked=self.data.get('defaultScope'),
                                    style='green',
                            ),

                            self.myparent.getTitledCheckBox(
                                    _("Show in configuration endpoint"),
                                    name='showInConfigurationEndpoint',
                                    checked=self.data.get('attributes',{}).get('showInConfigurationEndpoint','') ,
                                    style='green',
                            )
                        ])

        self.alt_tabs['openid'] = HSplit([

                            self.myparent.getTitledCheckBox(
                                    _("Default Scope"),
                                    name='defaultScope',
                                    checked=self.data.get('defaultScope'),
                                    style='green',
                            ),

                            self.myparent.getTitledCheckBox(
                                    _("Show in configuration endpoint"),
                                    name='showInConfigurationEndpoint',
                                    checked=self.data.get('attributes',{}).get('showInConfigurationEndpoint','') ,
                                    style='green',
                            ),


                            self.myparent.getTitledText(
                                    _("Claims"),
                                    name='claims',
                                    value='\n'.join(self.data.get('claims', [])),
                                    height=3, 
                                    style='green'),

                            self.myparent.getTitledText(
                                    _("Search"), 
                                    name='oauth:scopes:openID:claims:search',
                                    style='fg:green',width=10,
                                    jans_help=_("Press enter to perform search"), ),#accept_handler=self.search_clients



                            ])

        self.alt_tabs['dynamic'] = HSplit([
                        
                        self.myparent.getTitledText(_("Dynamic Scope Script"),
                            name='dynamicScopeScripts',
                            value='\n'.join(self.data.get('dynamicScopeScripts', [])),
                            height=3, 
                            style='green'),


                         self.myparent.getTitledText(_("Claims"),
                            name='claims',
                            value='\n'.join(self.data.get('claims', [])),
                            height=3, 
                            style='green'),

                        # Label(text=_("Claims"),style='red'),  ## name = claims TODO 

                        ],width=D(),
                    )

        self.alt_tabs['spontaneous'] = HSplit([
                    self.myparent.getTitledText(_("Associated Client"), name='none', value=self.data.get('none',''), style='green',read_only=True,height=3,),## Not fount
                    self.myparent.getTitledText(_("Creationg time"), name='creationDate', value=self.data.get('creationDate',''), style='green',read_only=True,),

                                                ],width=D(),
                    )

        self.alt_tabs['uma'] = HSplit([
                    self.myparent.getTitledText(_("IconURL"), name='iconUrl', value=self.data.get('iconUrl',''), style='green'),
                    

                    self.myparent.getTitledText(_("Authorization Policies"),
                            name='umaAuthorizationPolicies',
                            value='\n'.join(self.data.get('umaAuthorizationPolicies', [])),
                            height=3, 
                            style='green'),

                    self.myparent.getTitledText(_("Associated Client"), name='none', value=self.data.get('none',''), style='green',read_only=True,height=3,), ## Not fount
                    self.myparent.getTitledText(_("Creationg time"), name='description', value=self.data.get('description',''), style='green',read_only=True,),
                    self.myparent.getTitledText(_("Creator"), name='Creator', value=self.data.get('Creator',''), style='green',read_only=True,),
                        
                                                ],width=D(),
                    )
        

    def __pt_container__(self):
        return self.dialog
