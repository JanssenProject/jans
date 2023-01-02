from prompt_toolkit.layout.dimension import D
from prompt_toolkit.layout.containers import (
    VSplit,
    DynamicContainer,
)
from prompt_toolkit.layout.containers import (
    HSplit,
    VSplit,
    DynamicContainer,
)
from prompt_toolkit.widgets import (
    Box,
    Button,
    Label,
    Dialog,
 )
import cli_style
from utils.multi_lang import _
from utils.utils import DialogUtils
from utils.static import DialogResult, cli_style
from wui_components.jans_nav_bar import JansNavBar
from wui_components.jans_cli_dialog import JansGDialog

from prompt_toolkit.formatted_text import AnyFormattedText
from typing import Optional, Sequence
from typing import Callable

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
            buttons (list, optional): Dialog main buttons with their handlers. Defaults to [].
            deleted_uma (method, optional): handler invoked when Deleting UMA-res Defaults to None.
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
                                style=cli_style.edit_text,
                            ),

            self.myparent.getTitledText(
                                _("Display Name"),
                                name='name',
                                value=self.data.get('name',''),
                                read_only=True,
                                style=cli_style.edit_text),

            self.myparent.getTitledText(
                                _("IconURL"),
                                name='iconUri',
                                value=self.data.get('iconUri',''),
                                read_only=True,
                                style=cli_style.edit_text),    


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
                                style=cli_style.edit_text), 

            self.myparent.getTitledText(
                                _("Creation time"),
                                name='creationDate',
                                value=self.data.get('creationDate',''),
                                read_only=True,
                                style=cli_style.edit_text), 

        ], padding=1,width=100,style='class:outh-uma-tabs'

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
                                style=cli_style.edit_text,
                                height=3,
                            )
        ],width=D())

        self.UMA_containers['expression'] = HSplit([
        self.myparent.getTitledText(
                                _("Expression"),
                                name='scopeExpression',
                                value='\n'.join(self.data.get('scopeExpression',[])),
                                read_only=True,
                                style=cli_style.edit_text,
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
        """The container for the dialog itself

        Returns:
            Dialog: View Property
        """

        return self.dialog

