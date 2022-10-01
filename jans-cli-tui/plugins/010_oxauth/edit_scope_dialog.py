from typing import OrderedDict

from prompt_toolkit.layout.dimension import D
from prompt_toolkit.layout.containers import (
    HSplit,
    VSplit,
    DynamicContainer,
    Window
)
from prompt_toolkit.widgets import (
    Box,
    Button,
    Label,
)
from prompt_toolkit.application.current import get_app
from prompt_toolkit.widgets import (
    Button,
    Dialog,
    VerticalLine,
    HorizontalLine
)
from cli import config_cli
from static import DialogResult
from wui_components.jans_dialog_with_nav import JansDialogWithNav
from wui_components.jans_side_nav_bar import JansSideNavBar
from wui_components.jans_cli_dialog import JansGDialog
from wui_components.jans_drop_down import DropDownWidget
from wui_components.jans_data_picker import DateSelectWidget
from utils import DialogUtils
from wui_components.jans_vetrical_nav import JansVerticalNav
from view_uma_dialog import ViewUMADialog
import threading

from multi_lang import _

class EditScopeDialog(JansGDialog, DialogUtils):
    """The Main Scope Dialog that contain every thing related to The Scope
    """
    def __init__(self, parent, title, data, buttons=[], save_handler=None):
        """init for `EditScopeDialog`, inherits from two diffrent classes `JansGDialog` and `DialogUtils`
            
        DialogUtils (methods): Responsable for all `make data from dialog` and `check required fields` in the form for any Edit or Add New
        
        Args:
            parent (widget): This is the parent widget for the dialog, to access `Pageup` and `Pagedown`
            title (str): The Main dialog title
            data (list): selected line data 
            button_functions (list, optional): Dialog main buttons with their handlers. Defaults to [].
            save_handler (method, optional): handler invoked when closing the dialog. Defaults to None.
        """
        super().__init__(parent, title, buttons)
        self.save_handler = save_handler
        self.data = data
        self.title=title
        self.showInConfigurationEndpoint = self.data.get('attributes',{}).get('showInConfigurationEndpoint','')
        self.defaultScope = self.data.get('defaultScope','')
        self.prepare_tabs()
        self.create_window()
        self.sope_type = self.data.get('scopeType') or 'oauth'


    def save(self):
        self.myparent.logger.debug('SAVE SCOPE')

        data = {}

        for item in self.dialog.content.children + self.alt_tabs[self.sope_type].children:
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
    
    def cancel(self):
        self.future.set_result(DialogResult.CANCEL)


    def create_window(self):

        self.body = HSplit([
                        self.myparent.getTitledRadioButton(
                                _("Scope Type"),  
                                name='scopeType', 
                                current_value=self.data.get('scopeType'),
                                values=[('oauth', 'OAuth'), ('openid', 'OpenID'), ('dynamic', 'Dynamic'), ('spontaneous', 'Spontaneous'), ('uma', 'UMA')], 
                                on_selection_changed=self.scope_selection_changed,
                                style='green'),

                        self.myparent.getTitledText(_("id"), name='id', value=self.data.get('id',''), style='green'),
                        self.myparent.getTitledText(_("inum"), name='inum', value=self.data.get('inum',''), style='green',read_only=True,),
                        self.myparent.getTitledText(_("Display Name"), name='displayName', value=self.data.get('displayName',''), style='green'),
                        self.myparent.getTitledText(_("Description"), name='description', value=self.data.get('description',''), style='green'),
                        DynamicContainer(lambda: self.alt_tabs[self.sope_type]),

                        ], width=self.myparent.dialog_width
                    ),
        
        self.dialog = JansDialogWithNav(
            title=self.title,
            content= HSplit([
                self.myparent.getTitledRadioButton(
                                _("Scope Type"),  
                                name='scopeType', 
                                current_value=self.data.get('scopeType'),
                                values=[('oauth', 'OAuth'), ('openid', 'OpenID'), ('dynamic', 'Dynamic'), ('spontaneous', 'Spontaneous'), ('uma', 'UMA')], 
                                on_selection_changed=self.scope_selection_changed,
                                style='green'),
        
                self.myparent.getTitledText(_("id"), name='id', value=self.data.get('id',''), style='green'),
                self.myparent.getTitledText(_("inum"), name='inum', value=self.data.get('inum',''), style='green',read_only=True,),
                self.myparent.getTitledText(_("Display Name"), name='displayName', value=self.data.get('displayName',''), style='green'),
                self.myparent.getTitledText(_("Description"), name='description', value=self.data.get('description',''), style='green'),
                DynamicContainer(lambda: self.alt_tabs[self.sope_type]),    
            ]),  #DynamicContainer(lambda: self.alt_tabs[self.sope_type]),
             button_functions=[
                (self.save, _("Save")),
                (self.cancel, _("Cancel"))
            ],
            height=self.myparent.dialog_height,
            width=self.myparent.dialog_width,
                   )

    def scope_selection_changed(self, cb):
        self.sope_type = cb.current_value

    def set_scope_type(self, scope_type):
        self.sope_type = scope_type

    def prepare_tabs(self):
        """Prepare the tabs for Edil Scope Dialogs
        """
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
                        ],width=D(),)

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

                            # HorizontalLine(),
                            self.myparent.getTitledText(
                                    _("Search"), 
                                    name='oauth:scopes:openID:claims:search',
                                    style='fg:green',width=10,
                                    jans_help=_("Press enter to perform search"), ),#accept_handler=self.search_scopes

                            self.myparent.getTitledText(
                                    _("Claims"),
                                    name='claims',
                                    value='\n'.join(self.data.get('claims', [])),
                                    height=3, 
                                    style='green'),

                            ],width=D(),)

        self.alt_tabs['dynamic'] = HSplit([
                        
                        self.myparent.getTitledText(_("Dynamic Scope Script"),
                            name='dynamicScopeScripts',
                            value='\n'.join(self.data.get('dynamicScopeScripts', [])),
                            height=3, 
                            style='green'),
                        
                        # Window(char='-', height=1),
                        self.myparent.getTitledText(
                                _("Search"), 
                                name='oauth:scopes:openID:claims:search',
                                style='fg:green',width=10,
                                jans_help=_("Press enter to perform search"), ),#accept_handler=self.search_scopes

                        self.myparent.getTitledText(
                                _("Claims"),
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

