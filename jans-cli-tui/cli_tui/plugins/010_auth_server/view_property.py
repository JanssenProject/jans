import asyncio
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.formatted_text import AnyFormattedText
from cli import config_cli
from prompt_toolkit.layout.containers import (
    HSplit,
    DynamicContainer,
)
from prompt_toolkit.widgets import (
    Button,
    RadioList,
    Dialog,
 )
from utils.static import DialogResult
from utils.utils import DialogUtils
from wui_components.jans_cli_dialog import JansGDialog
from typing import Optional, Sequence
from utils.multi_lang import _
import cli_style

class ViewProperty(JansGDialog, DialogUtils):
    """The Main UMA-resources Dialog to view UMA Resource Details
    """
    def __init__(
            self,
            app,
            parent,
            data:tuple,
            title: AnyFormattedText= "",
            buttons: Optional[Sequence[Button]]= []
            )-> None:
        """init for `ViewProperty`, inherits from two diffrent classes `JansGDialog` and `DialogUtils`
            
        JansGDialog (dialog): This is the main dialog Class Widget for all Jans-cli-tui dialogs except custom dialogs like dialogs with navbar
        DialogUtils (methods): Responsable for all `make data from dialog` and `check required fields` in the form for any Edit or Add New
        
        Args:
            app (Generic): The main Application class
            parent (widget): This is the parent widget for the dialog
            data (tuple): selected line data 
            title (AnyFormattedText, optional): The Main dialog title. Defaults to "".
            button_functions (list, optional): Dialog main buttons with their handlers. Defaults to [].
        """
        super().__init__(app, title, buttons)
        self.property, self.value = data[0],data[1]
        self.app = app
        self.myparent = parent
        self.value_content = HSplit([],width=D())
        self.tabs = {}
        self.selected_tab = 'tab0'
        self.schema = self.app.cli_object.get_schema_from_reference('', '#/components/schemas/AppConfiguration')

        self.prepare_properties()
        self.create_window()
        
    def cancel(self) -> None:
        """method to invoked when canceling changes in the dialog (Cancel button is pressed)
        """

        self.future.set_result(DialogResult.CANCEL)

    def save(self) -> None:
        """method to invoked when saving the dialog (Save button is pressed)
        """

        data_dict = {}
        list_data =[]

        if type(self.value) in [str,bool,int] :
            for wid in self.value_content.children:
                prop_type = self.get_item_data(wid)
            data = prop_type['value']

        elif (type(self.value)==list and (type(self.value[0]) not in  [dict,list])):
            
            for wid in self.value_content.children:
                prop_type = self.get_item_data(wid)
            
            if  self.get_type(prop_type['key']) != 'checkboxlist':
                data = prop_type['value'].split('\n')
            else:
                data = prop_type['value']

        elif type(self.value) == dict :
            for wid in self.value_content.children:
                for k in wid.children :
                    prop_type = self.get_item_data(k)
                    data_dict[prop_type['key']]=prop_type['value']
            data = data_dict

        elif type(self.value) == list  and type(self.value[0]) == dict:
            for tab in self.tabs:
                data_dict = {}
                for k in self.tabs[tab].children :
                    prop_type = self.get_item_data(k.children[0])
                    data_dict[prop_type['key']]=prop_type['value']
                list_data.append(data_dict)
            data = list_data
        else :
            self.app.logger.debug("self.value: "+str(self.value))
            self.app.logger.debug("type self.value: "+str(type(self.value)))
            data = []
                
        # ------------------------------------------------------------#
        # --------------------- Patch to server ----------------------#
        # ------------------------------------------------------------#
        if data :

            cli_args = {'operation_id': 'patch-properties', 'data': [ {'op':'replace', 'path': self.property, 'value': data } ]}

            async def coroutine():
                self.app.start_progressing()
                response = await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
                self.app.stop_progressing()
                self.myparent.app_configuration = response
                self.future.set_result(DialogResult.ACCEPT)
                self.myparent.oauth_update_properties(start_index=self.myparent.oauth_update_properties_start_index)
            asyncio.ensure_future(coroutine())

    def get_type(self,prop):
        """This Method get a property and get its type from schema to return the widget type to implement

        Args:
            prop (str): The property name 

        Returns:
            str: the widget type to implement
        """
        try :
            proper = self.schema.get('properties', {})[prop]

            if proper['type'] == 'string':
                prop_type= 'TitledText'

            elif proper['type'] == 'integer':
                prop_type= 'int-TitledText'

            elif proper['type'] == 'boolean':
                prop_type= 'TitledCheckBox'

            elif proper['type'] == 'object':
                prop_type= 'dict'

            elif proper['type'] == 'array':
                if 'enum' in proper or ('enum' in proper['items']):
                   prop_type= 'checkboxlist' 
                else:
                    if type(self.value[0]) == dict:
                        prop_type= 'list-dict'
                    elif type(self.value[0]) == list:
                        prop_type= 'list-list'
                    else:
                        prop_type= 'long-TitledText'
        except:
            prop_type = None

        return prop_type

    def get_listValues(self,prop,type=None):
        """This method get list values for properties own Enum values

        Args:
            prop (str): The property name
            type (_type_, optional): If the Items in Property properties had a nasted Enum. Defaults to None.

        Returns:
            list: List of the properties enum to choose from
        """
        try :
            if type !='nasted':
                list_values= self.schema.get('properties', {})[prop]['items']['enum']
            else:
                list_values= self.schema.get('properties', {})[prop]['items']['items']['enum']

        except:
            list_values = []

        return list_values

    def prepare_properties(self):
        """This method build the main value_content to edit the properties
        """

        prop_type = self.get_type(self.property)

        if prop_type == 'TitledText':
                self.value_content= HSplit([self.app.getTitledText(
                self.property, 
                name=self.property, 
                value=self.value, 
                style='class:outh-scope-text'
                ),
                ],width=D())

        elif prop_type == 'int-TitledText':
                self.value_content= HSplit([self.app.getTitledText(
                self.property, 
                name=self.property, 
                value=self.value, 
                text_type='integer',
                style='class:outh-scope-text'
                ),
                ],width=D())

        elif prop_type == 'long-TitledText':
            self.value_content= HSplit([self.app.getTitledText(
                                self.property, 
                                name=self.property, 
                                height=3,
                                value='\n'.join(self.value), 
                                style='class:outh-scope-text'
                                ),
                                ],width=D())    

        elif prop_type == 'list-list':
            self.value_content= HSplit([
                        self.app.getTitledCheckBoxList(
                                self.property, 
                                name=self.property, 
                                values=self.get_listValues(self.property,'nasted'), 
                                style='class:outh-client-checkboxlist'),
                                ],width=D())  

        elif prop_type == 'checkboxlist':
            self.value_content= HSplit([
                        self.app.getTitledCheckBoxList(
                                self.property, 
                                name=self.property, 
                                values=self.get_listValues(self.property), 
                                style='class:outh-client-checkboxlist'),
                                ],width=D())    

        elif prop_type == 'list-dict':  
            tab_num = len(self.value)
            tabs = []
            for i in range(tab_num) :
                tabs.append(('tab{}'.format(i),'tab{}'.format(i)))
            

            for tab in self.value:  
                tab_list=[]
                for item in tab:
                    if type(tab[item]) == str:
                        tab_list.append(HSplit([self.app.getTitledText(
                            item ,
                            name=item, 
                            value=tab[item], 
                            style='class:outh-scope-text'
                            ),
                            ],width=D()))

                    if type(tab[item]) == int :
                        tab_list.append(HSplit([self.app.getTitledText(
                            item ,
                            name=item, 
                            value=tab[item], 
                            text_type='integer',
                            style='class:outh-scope-text'
                            ),
                            ],width=D()))

                    elif type(tab[item]) == list:
                        tab_list.append(HSplit([self.app.getTitledText(
                            item, 
                            name=item, 
                            height=3,
                            value='\n'.join(tab[item]), 
                            style='class:outh-scope-text'
                            ),
                            ],width=D()))

                    elif type(tab[item]) == bool:
                        tab_list.append(HSplit([
                            self.app.getTitledCheckBox(
                                item, 
                                name=item, 
                                checked= tab[item], 
                                style='class:outh-client-checkbox'),
                        ],width=D()))  
                                    
                    self.tabs['tab{}'.format(self.value.index(tab))] = HSplit(tab_list,width=D())

            self.value_content=HSplit([
                            self.app.getTitledRadioButton(
                                _("Tab Num"),
                                name='tabNum',
                                current_value=self.selected_tab,
                                values=tabs,
                                on_selection_changed=self.tab_selection_changed,
                                style='class:outh-scope-radiobutton'),

                            DynamicContainer(lambda: self.tabs[self.selected_tab]),     

                ],width=D())
                
        elif prop_type == 'TitledCheckBox':
            self.value_content= HSplit([
                self.app.getTitledCheckBox(
                    self.property, 
                    name=self.property, 
                    checked= self.value, 
                    style='class:outh-client-checkbox'),
            ],width=D())

        elif prop_type == 'dict':
            dict_list=[]
            for item in self.value:
                if type(self.value[item]) == str:
                        dict_list.append(HSplit([self.app.getTitledText(
                        item ,
                        name=item, 
                        value=self.value[item], 
                        style='class:outh-scope-text'
                        ),
                        ],width=D()))

                elif type(self.value[item]) == int :
                        dict_list.append(HSplit([self.app.getTitledText(
                        item ,
                        name=item, 
                        value=self.value[item], 
                        text_type='integer',
                        style='class:outh-scope-text'
                        ),
                        ],width=D()))

                elif type(self.value[item]) == list:
                    dict_list.append(HSplit([self.app.getTitledText(
                        item, 
                        name=item, 
                        height=3,
                        value='\n'.join(self.value[item]), 
                        style='class:outh-scope-text'
                        ),
                        ],width=D()))

                elif type(self.value[item]) == bool:
                    dict_list.append(HSplit([
                        self.app.getTitledCheckBox(
                            item, 
                            name=item, 
                            checked= self.value[item], 
                            style='class:outh-client-checkbox'),
                    ],width=D()))

                else :
                    dict_list.append(HSplit([self.app.getTitledText(
                                            item, 
                                            name=item, 
                                            value="No Items Here", 
                                            style='class:outh-scope-text',
                                            read_only=True,
                                            ),
                                            ],width=D()))  
            self.value_content= HSplit(dict_list,width=D())

    def create_window(self):

        self.dialog = Dialog(title=self.property,
            body=     
            HSplit([
            self.value_content,
        ], padding=1,width=100,style='class:outh-uma-tabs'
        ),
        buttons=[
                Button(
                    text=_("Cancel"),
                    handler=self.cancel,
                ) ,
                Button(
                    text=_("Save"),
                    handler=self.save,
                ) ,            ],
            with_background=False,
        )

    def tab_selection_changed(
        self, 
        cb: RadioList,
        ) -> None:
        """This method for properties that implemented in multi tab

        Args:
            cb (RadioList): the New Value from the nasted tab
        """
        self.selected_tab = cb.current_value

    def __pt_container__(self)-> Dialog:
        """The container for the dialog itself

        Returns:
            Dialog: View Property
        """

        return self.dialog

