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
    Window
)
from prompt_toolkit.widgets import (
    Box,
    Button,
    Frame,
    Label,
    RadioList,
    TextArea,
 )
from wui_components.jans_dialog_with_nav import JansDialogWithNav
from wui_components.jans_nav_bar import JansNavBar
from wui_components.jans_side_nav_bar import JansSideNavBar
from utils import DialogUtils

from wui_components.jans_cli_dialog import JansGDialog

from wui_components.jans_drop_down import DropDownWidget
from prompt_toolkit.layout.containers import (
    AnyContainer,
)
from prompt_toolkit.buffer import Buffer

from prompt_toolkit.formatted_text import AnyFormattedText
from prompt_toolkit.layout.dimension import AnyDimension
from typing import Optional, Sequence, Union
from typing import TypeVar, Callable
from multi_lang import _
import cli_style

class ViewProperty(JansGDialog, DialogUtils):
    """The Main UMA-resources Dialog to view UMA Resource Details
    """
    def __init__(
        self,
        parent,
        data:tuple,
        title: AnyFormattedText= "",
        search_text: AnyFormattedText= "",
        buttons: Optional[Sequence[Button]]= [],
        get_properties: Callable= None,
        search_properties: Callable= None,
        )-> Dialog:

        super().__init__(parent, title, buttons)
        self.property, self.value = data[0],data[1]
        self.myparent= parent
        self.get_properties = get_properties
        self.search_properties= search_properties
        self.search_text=search_text
        self.UMA_containers = {}
        # self.property_prepare_containers()


        def cancel() -> None:
            self.future.set_result(DialogResult.CANCEL)

        def save() -> None:
            
            for wid in self.dialog.body.children:
                for item in wid.children:
                    if type(self.value) == str or type(self.value) == int :
                        item_data = self.get_item_data(item)
                        if item_data:
                            data = item_data['value']

                    elif type(self.value) == bool:
                        item_data = self.get_item_data(item)
                        if item_data:
                            data = item_data['value']

                    elif type(self.value) == list:
                        if type(self.value[0]) == str:
                            item_data = self.get_item_data(item)
                            if item_data:
                                data[item_data['key']] = item_data['value'].split('\n')
                        else:
                            ## TODO dict in list
                            pass

                    elif type(self.value) == dict:
                        data = {}
                        for i,field in enumerate(item.children) :
                            if type(self.value[list(self.value)[i]])  in [str,int,bool]:
                                item_data = self.get_item_data(field)
                                if item_data:
                                    data[item_data['key']] = item_data['value']

                    else:
                        item_data = self.get_item_data(item)
                        self.myparent.logger.debug("item_data,Else: "+str(item_data))

            response = self.myparent.cli_object.process_command_by_id(
                    operation_id='patch-properties' ,
                    url_suffix='',
                    endpoint_args='',
                    data_fn='',
                    data=[ {'op':'replace', 'path': self.property, 'value': data } ]
                    )

            if response:
                tbuff = Buffer(name='', )
                tbuff.text=self.search_text
                if self.search_text:
                    self.search_properties(tbuff)
                else:
                    self.get_properties()
                self.future.set_result(DialogResult.ACCEPT)
                return True

            self.myparent.show_message(_("Error!"), _("An error ocurred while saving property:\n") + str(response.text))

        if type(self.value) == str or type(self.value) == int :
                value_content= HSplit([self.myparent.getTitledText(
                self.property, 
                name=self.property, 
                value=self.value, 
                style='class:outh-scope-text'
                ),
                ],width=D())

        elif type(self.value) == list:
            try:
                value_content= HSplit([self.myparent.getTitledText(
                    self.property+'\n'*(len(self.value)-1), 
                    name=self.property, 
                    height=3,
                    value='\n'.join(self.value), 
                    style='class:outh-scope-text'
                    ),
                    ],width=D())
            except:
                value_content=HSplit([],width=D())
                pass

        elif type(self.value) == bool:
            value_content= HSplit([
                self.myparent.getTitledCheckBox(
                    self.property, 
                    name=self.property, 
                    checked= self.value, 
                    style='class:outh-client-checkbox'),
            ],width=D())

        elif type(self.value) == dict:
            dict_list=[]
            for item in self.value:
                if type(self.value[item]) == str or type(self.value[item]) == int :
                        dict_list.append(HSplit([self.myparent.getTitledText(
                        item ,
                        name=item, 
                        value=self.value[item], 
                        style='class:outh-scope-text'
                        ),
                        ],width=D()))

                elif type(self.value[item]) == list:
                    dict_list.append(HSplit([self.myparent.getTitledText(
                        item, 
                        name=item, 
                        height=3,
                        value='\n'.join(self.value[item]), 
                        style='class:outh-scope-text'
                        ),
                        ],width=D()))

                elif type(self.value[item]) == bool:
                    dict_list.append(HSplit([
                        self.myparent.getTitledCheckBox(
                            item, 
                            name=item, 
                            checked= self.value[item], 
                            style='class:outh-client-checkbox'),
                    ],width=D()))

            value_content= HSplit(dict_list,width=D())


        self.dialog = Dialog(title=self.property,
            body=     
            HSplit([
            value_content,
        ], padding=1,width=100,style='class:outh-uma-tabs'
        ),
        buttons=[
                Button(
                    text=_("Cancel"),
                    handler=cancel,
                ) ,
                Button(
                    text=_("Save"),
                    handler=save,
                ) ,            ],
            with_background=False,
        )

    def __pt_container__(self)-> Dialog:
        return self.dialog

