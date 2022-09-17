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


class ViewUMADialog(JansGDialog, DialogUtils):

    def __init__(self, parent, title, data, buttons=[], save_handler=None):

        super().__init__(parent, title, buttons)
        self.save_handler = save_handler
        self.data = data

        def delete():
            self.myparent.show_again()
            # self.future.set_result(DialogResult.CANCEL)

        def cancel():
            self.future.set_result(DialogResult.CANCEL)

#    {"dn":"jansId=1caf7fbe-349f-468a-ac48-8cbf24a638bd,
#         ou=resources,
#         ou=uma,
#         o=jans",
#         "id":"1caf7fbe-349f-468a-ac48-8cbf24a638bd",
#         "name":"test-uma-resource",
#         "description":"This is a test UMA Resource",
#         "deletable":false

        self.dialog = Dialog(title='title',
        
        body=     
        HSplit([

            self.myparent.getTitledText(
                                "Resource id",
                                name='id',
                                value=self.data.get('id',''),
                                read_only=True,
                                style='green',
                            ),

            self.myparent.getTitledText(
                                "Display Name",
                                name='name',
                                value=self.data.get('name',''),
                                read_only=True,
                                style='green'),

            self.myparent.getTitledText(
                                "IconURL",
                                name='iconUri',
                                value=self.data.get('iconUri',''),
                                read_only=True,
                                style='green'),    

            Label(text=_("Scope Selection"),style='bold',width=len(_("Scope Selection"))), ## TODO dont know what is that

            self.myparent.getTitledText(
                                "Scope  or Expression",
                                name='scopeExpression',
                                value=self.data.get('scopeExpression',''),
                                read_only=True,
                                style='green',
                                # height=3   ### enmpty spaces eccures here
                                ), 

            self.myparent.getTitledText(
                                "Associated Client",
                                name='clients',
                                value=self.data.get('clients',''),
                                read_only=True,
                                style='green'), 

            self.myparent.getTitledText(
                                "Creation time",
                                name='creationDate',
                                value=self.data.get('creationDate',''),
                                read_only=True,
                                style='green'), 

        ], padding=1,width=self.myparent.dialog_width,),
        buttons=[
                Button(
                    text="Cancel",
                    handler=cancel,
                ) ,
                Button(
                    text="Delete",
                    handler=delete,
                ) 
            ],
            with_background=False,
            # width=140,
            
            
  
        )
   

    def __pt_container__(self):
        return self.dialog

