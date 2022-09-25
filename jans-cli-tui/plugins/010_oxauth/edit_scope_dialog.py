import json
from asyncio import Future
from typing import OrderedDict

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
from wui_components.jans_dialog_with_nav import JansDialogWithNav
from wui_components.jans_side_nav_bar import JansSideNavBar
from utils import DialogUtils

from wui_components.jans_cli_dialog import JansGDialog

from wui_components.jans_drop_down import DropDownWidget

from multi_lang import _


class EditScopeDialog(JansGDialog, DialogUtils):
    """The Main scope Dialog that contain every thing related to The scope
    """
    def __init__(self, parent, title, data, buttons=[], save_handler=None):
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
        super().__init__(parent, title, buttons)
        self.save_handler = save_handler
        self.data = data
        self.prepare_tabs()

        def save():

            self.data = self.make_data_from_dialog()
            
            if 'attributes' in self.data.keys():    
                self.data['attributes'] = {'showInConfigurationEndpoint':self.data['attributes']}

            cfr = self.check_required_fields()
            self.myparent.logger.debug('CFR: '+str(cfr))
            if not cfr:
                return

            self.myparent.logger.debug('handler: '+str(save_handler))
            close_me = True
            if save_handler:
                close_me = self.save_handler(self)
            if close_me:
                self.future.set_result(DialogResult.ACCEPT)

        def cancel():
            self.future.set_result(DialogResult.CANCEL)

        self.side_nav_bar = JansSideNavBar(myparent=self.myparent,
            entries=list(self.tabs.keys()),
            selection_changed=(self.scope_dialog_nav_selection_changed) ,
            select=0,
            entries_color='#2600ff')

        self.dialog = JansDialogWithNav(
            title=title,
            navbar=self.side_nav_bar,
            content=DynamicContainer(lambda: self.tabs[self.left_nav]),
             button_functions=[
                (save, _("Save")),
                (cancel, _("Cancel"))
            ],
            height=self.myparent.dialog_height,
            width=self.myparent.dialog_width,
                   )
    

    def prepare_tabs(self):
        """Prepare the tabs for Edil scope Dialogs
        """

        schema = self.myparent.cli_object.get_schema_from_reference('#/components/schemas/Scope')

        self.tabs = OrderedDict()

        self.tabs['OAuth'] = HSplit([
                       self.myparent.getTitledText(_("id"), name='id', value=self.data.get('id',''), style='green'),
                       self.myparent.getTitledText(_("inum"), name='inum', value=self.data.get('inum',''), style='green',read_only=True,),
                       self.myparent.getTitledText(_("Display Name"), name='displayName', value=self.data.get('displayName',''), style='green'),
                       self.myparent.getTitledText(_("Description"), name='description', value=self.data.get('description',''), style='green'),

                        self.myparent.getTitledCheckBox(_("Default Scope"),
                        name='defaultScope',
                        checked=self.data.get('defaultScope'),
                        style='green'),

                        self.myparent.getTitledCheckBox(_("Show in configuration endpoint"),
                        name='attributes',
                        checked=self.data.get('attributes',{}).get('showInConfigurationEndpoint','') ,
                        style='green'),
                        ],width=D(),
                    )

        # self.tabs['OpenID'] = HSplit([
        #                self.myparent.getTitledText(_("id"), name='id', value=self.data.get('id',''), style='green'),
        #                self.myparent.getTitledText(_("inum"), name='inum', value=self.data.get('inum',''), style='green',read_only=True,),
        #                self.myparent.getTitledText(_("Display Name"), name='displayName', value=self.data.get('displayName',''), style='green'),
        #                self.myparent.getTitledText(_("Description"), name='description', value=self.data.get('description',''), style='green'),

        #                 self.myparent.getTitledCheckBox(_("Default Scope"),
        #                 name='defaultScope',
        #                 checked=self.data.get('defaultScope'),
        #                 style='green'),

        #                 self.myparent.getTitledCheckBox(_("Show in configuration endpoint"),
        #                 name='showInConfigurationEndpoint',
        #                 checked=self.data.get('showInConfigurationEndpoint'),
        #                 style='green'),

        #                 VSplit([
                        
        #                 self.myparent.getTitledText(_("Claims"),
        #                     name='claims',
        #                     value='\n'.join(self.data.get('claims', [])),
        #                     height=3, 
        #                     style='green'),
        #                 self.myparent.getTitledText(_("Search"), name='oauth:scopes:openID:claims:search',style='fg:green',width=10,jans_help=_("Press enter to perform search"), ),#accept_handler=self.search_clients

        #                 ]),



        #                 # Label(text=_("Claims"),style='red'),  ## name = claims TODO 

        #                 ],width=D(),
        #             )

        # self.tabs['Dynamic'] = HSplit([
        #                self.myparent.getTitledText(_("id"), name='id', value=self.data.get('id',''), style='green'),
        #                self.myparent.getTitledText(_("inum"), name='inum', value=self.data.get('inum',''), style='green',read_only=True),
        #                self.myparent.getTitledText(_("Display Name"), name='displayName', value=self.data.get('displayName',''), style='green'),
        #                self.myparent.getTitledText(_("Description"), name='description', value=self.data.get('description',''), style='green'),
                        
        #                 # Label(text=_("Dynamic Scope Script"),style='red'),  ## name = dynamicScopeScripts TODO  
                        
        #                 self.myparent.getTitledText(_("Dynamic Scope Script"),
        #                     name='dynamicScopeScripts',
        #                     value='\n'.join(self.data.get('dynamicScopeScripts', [])),
        #                     height=3, 
        #                     style='green'),


        #                 self.myparent.getTitledCheckBox(_("Allow for dynamic registration"),
        #                 name='defaultScope',
        #                 checked=self.data.get('defaultScope'),
        #                 style='green'),

        #                 self.myparent.getTitledCheckBox(_("Show in configuration endpoint"),
        #                 name='showInConfigurationEndpoint',
        #                 checked=self.data.get('showInConfigurationEndpoint'),
        #                 style='green'),

        #                  self.myparent.getTitledText(_("Claims"),
        #                     name='claims',
        #                     value='\n'.join(self.data.get('claims', [])),
        #                     height=3, 
        #                     style='green'),

        #                 # Label(text=_("Claims"),style='red'),  ## name = claims TODO 

        #                 ],width=D(),
        #             )

        # # self.tabs['Spontaneous'] = HSplit([
        # #             self.myparent.getTitledText(_("id"), name='id', value=self.data.get('id',''), style='green',read_only=True,),
        # #             self.myparent.getTitledText(_("inum"), name='inum', value=self.data.get('inum',''), style='green',read_only=True,),
        # #             self.myparent.getTitledText(_("Display Name"), name='displayName', value=self.data.get('displayName',''), style='green',read_only=True,),
        # #             self.myparent.getTitledText(_("Description"), name='description', value=self.data.get('description',''), style='green',read_only=True,),
        # #             self.myparent.getTitledText(_("Associated Client"), name='none', value=self.data.get('none',''), style='green',read_only=True,height=3,),## Not fount
        # #             self.myparent.getTitledText(_("Creationg time"), name='creationDate', value=self.data.get('creationDate',''), style='green',read_only=True,),
                        
                        
        # #                                         ],width=D(),
        # #             )

        # self.tabs['UMA'] = HSplit([
        #             self.myparent.getTitledText(_("id"), name='id', value=self.data.get('id',''), style='green'),
        #             self.myparent.getTitledText(_("inum"), name='inum', value=self.data.get('inum',''), style='green',read_only=True,),
        #             self.myparent.getTitledText(_("Display Name"), name='displayName', value=self.data.get('displayName',''), style='green'),
        #             self.myparent.getTitledText(_("IconURL"), name='description', value=self.data.get('description',''), style='green'),
                    
        #             self.myparent.getTitledWidget(
        #                     _("Authorization Policies"),
        #                     name='umaAuthorizationPolicies',
        #                     widget=DropDownWidget(
        #                         values=   self.data.get('umaAuthorizationPolicies',[]),
        #                        # value=self.data.get('tokenEndpointAuthMethodsSupported')
        #                         ),
        #                     jans_help=self.myparent.get_help_from_schema(schema, 'tokenEndpointAuthMethodsSupported'),
        #                     style='green'
        #                     ),

        #             self.myparent.getTitledText(_("Associated Client"), name='none', value=self.data.get('none',''), style='green',read_only=True,height=3,), ## Not fount
        #             self.myparent.getTitledText(_("Creationg time"), name='description', value=self.data.get('description',''), style='green',read_only=True,),
        #             self.myparent.getTitledText(_("Creator"), name='Creator', value=self.data.get('Creator',''), style='green',read_only=True,),
                        
        #                                         ],width=D(),
        #             )
        
        self.left_nav = list(self.tabs.keys())[0]

    
    def scope_dialog_nav_selection_changed(self, selection):
        self.left_nav = selection

    def __pt_container__(self):
        return self.dialog

