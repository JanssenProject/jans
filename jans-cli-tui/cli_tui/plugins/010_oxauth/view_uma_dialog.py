import json
from asyncio import Future
from typing import OrderedDict

from prompt_toolkit.widgets import Button, TextArea
from prompt_toolkit.application.current import get_app
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.layout.containers import (
    VSplit,
    DynamicContainer,
)
from prompt_toolkit.key_binding import KeyBindings

from prompt_toolkit.widgets import (
    Button,
    Label,
    TextArea,

)
from asyncio import ensure_future

from prompt_toolkit.widgets import (
    Button,
    Dialog,
    VerticalLine,
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
    Window,
    AnyContainer
)
from prompt_toolkit.widgets import (
    Box,
    Button,
    Frame,
    Label,
    RadioList,
    TextArea,
 )

import cli_style
from utils.multi_lang import _
from utils.utils import DialogUtils
from utils.static import DialogResult
from wui_components.jans_dialog_with_nav import JansDialogWithNav
from wui_components.jans_nav_bar import JansNavBar
from wui_components.jans_side_nav_bar import JansSideNavBar
from wui_components.jans_dialog import JansDialog
from wui_components.jans_cli_dialog import JansGDialog
from wui_components.jans_drop_down import DropDownWidget

from prompt_toolkit.formatted_text import AnyFormattedText
from prompt_toolkit.layout.dimension import AnyDimension
from typing import Optional, Sequence, Union
from typing import TypeVar, Callable

class ViewUMADialog(JansGDialog, DialogUtils):
    """The Main UMA-resources Dialog to view UMA Resource Details
    """
    def __init__(
        self,
        parent,
        data:list,
        title: AnyFormattedText= "",
        buttons: Optional[Sequence[Button]]= [],
        deleted_uma: Callable= None,
        )-> Dialog:
        """init for `ViewUMADialog`, inherits from two diffrent classes `JansGDialog` and `DialogUtils`
            
        JansGDialog (dialog): This is the main dialog Class Widget for all Jans-cli-tui dialogs except custom dialogs like dialogs with navbar
        DialogUtils (methods): Responsable for all `make data from dialog` and `check required fields` in the form for any Edit or Add New
        
        Args:
            parent (widget): This is the parent widget for the dialog
            title (str): The Main dialog title
            data (list): selected line data 
            button_functions (list, optional): Dialog main buttons with their handlers. Defaults to [].
            save_handler (method, optional): handler invoked when closing the dialog. Defaults to None.
        """
        super().__init__(parent, title, buttons)
        self.data = data
        self.myparent= parent
        self.deleted_uma = deleted_uma
        self.UMA_containers = {}
        self.UMA_prepare_containers()

        def delete() -> None:
            selected = [data.get('id'),data.get('description', ''),data.get('scopes', [''])[0]]
            self.deleted_uma(selected,self.future.set_result(DialogResult.CANCEL))
            
        def cancel() -> None:
            self.future.set_result(DialogResult.CANCEL)

        self.side_nav_bar =  JansNavBar(
                    self,
                    entries=[('scope', 'scope'), ('expression', 'scope expression'), ],
                    selection_changed=self.oauth_nav_selection_changed,
                    select=0,
                    bgcolor=cli_style.outh_navbar_bgcolor  ### it is not a style > only color
                    )

        self.dialog = Dialog(title='UMA-resources',

        body=     
        HSplit([

            self.myparent.getTitledText(
                                _("Resource id"),
                                name='id',
                                value=self.data.get('id',''),
                                read_only=True,
                                style='class:outh-uma-text',
                            ),

            self.myparent.getTitledText(
                                _("Display Name"),
                                name='name',
                                value=self.data.get('name',''),
                                read_only=True,
                                style='class:outh-uma-text'),

            self.myparent.getTitledText(
                                _("IconURL"),
                                name='iconUri',
                                value=self.data.get('iconUri',''),
                                read_only=True,
                                style='class:outh-uma-text'),    


            VSplit([
            Label(text=_("Scope Selection"),style='class:outh-uma-label',width=len(_("Scope Selection"))), ## TODO dont know what is that

            Box(self.side_nav_bar.nav_window, style='class:outh-uma-navbar', height=1),

            ]),
            
            DynamicContainer(lambda: self.oauth_main_area),
            
            self.myparent.getTitledText(
                                _("Associated Client"),
                                name='clients',
                                value=self.data.get('clients',''),
                                read_only=True,
                                style='class:outh-uma-text'), 

            self.myparent.getTitledText(
                                _("Creation time"),
                                name='creationDate',
                                value=self.data.get('creationDate',''),
                                read_only=True,
                                style='class:outh-uma-text'), 

        ], padding=1,width=100,style='class:outh-uma-tabs'
        # key_bindings=self.get_uma_dialog_key_bindings()
        ),
        buttons=[
                Button(
                    text=_("Cancel"),
                    handler=cancel,
                ) ,
                Button(
                    text=_("Delete"),
                    handler=delete,
                ) 
            ],
            with_background=False,
            # width=140,
        )

    def UMA_prepare_containers(self) -> None:
        """Prepare the containers for UMA Dialog
        """
        self.oauth_main_area =  self.UMA_containers['scope'] = HSplit([
        self.myparent.getTitledText(
                                _("Scopes"),
                                name='scopes',
                                value='\n'.join(self.data.get('scopes',[])),
                                read_only=True,
                                style='class:outh-uma-text',
                                height=3,
                            )
        ],width=D())

        self.UMA_containers['expression'] = HSplit([
        self.myparent.getTitledText(
                                _("Expression"),
                                name='scopeExpression',
                                value='\n'.join(self.data.get('scopeExpression',[])),
                                read_only=True,
                                style='class:outh-uma-text',
                                 height=3,
                            ),
        ],width=D())
        
    def oauth_nav_selection_changed(
        self, 
        selection: str
        ) -> None:
        """This method for the selection change for tabs

        Args:
            selection (str): the current selected tab
        """
        if selection in self.UMA_containers:
            self.oauth_main_area = self.UMA_containers[selection]
        
    def __pt_container__(self)-> Dialog:
        return self.dialog

