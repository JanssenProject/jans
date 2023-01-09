import json
import asyncio
from functools import partial
from typing import Optional, Sequence

from prompt_toolkit.application import Application
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.formatted_text import AnyFormattedText
from prompt_toolkit.layout.containers import HSplit
from prompt_toolkit.widgets import Button, Dialog

from cli import config_cli
from utils.static import DialogResult, cli_style
from utils.utils import DialogUtils
from wui_components.jans_cli_dialog import JansGDialog
from wui_components.jans_tab import JansTab

from utils.multi_lang import _

class ViewProperty(JansGDialog, DialogUtils):
    """The Main UMA-resources Dialog to view UMA Resource Details
    """
    def __init__(
            self,
            app: Application,
            parent,
            data:tuple,
            title: AnyFormattedText='',
            op_type: Optional[str]='replace'
            )-> None:
        """init for `ViewProperty`, inherits from two diffrent classes `JansGDialog` and `DialogUtils`
            
        JansGDialog (dialog): This is the main dialog Class Widget for all Jans-cli-tui dialogs except custom dialogs like dialogs with navbar
        DialogUtils (methods): Responsable for all `make data from dialog` and `check required fields` in the form for any Edit or Add New
        
        Args:
            app (Generic): The main Application class
            parent (widget): This is the parent widget for the dialog
            data (tuple): selected line data 
            title (AnyFormattedText, optional): The Main dialog title. Defaults to "".
        """
        super().__init__(app)
        self.property_name, self.value = data[0], data[1]
        self.app = app
        self.myparent = parent
        self.op_type = op_type
        self.value_content = HSplit([],width=D())
        self.tab_widget = None
        self.widgets = []
        self.buttons = [Button(text=_("Cancel"), handler=self.cancel), Button(text=_("Save"), handler=self.save)]
        self.prepare_properties()
        self.create_window()

    def cancel(self) -> None:
        """method to invoked when canceling changes in the dialog (Cancel button is pressed)
        """

        self.future.set_result(DialogResult.CANCEL)

    def save(self) -> None:
        """method to invoked when saving the dialog (Save button is pressed)
        """

        if len(self.widgets) == 1:
            item_data = self.get_item_data(self.widgets[0])
            data = item_data['value']

        elif self.tab_widget:
            data = []
            tabn = []
            for tab in self.tab_widget.tabs:
                tabn.append(tab[0])
                if self.tab_widget.tab_content_type == 'object':
                    tab_data = self.make_data_from_dialog({tab[0]: tab[1]})
                else:
                    tab_data_tmp = self.make_data_from_dialog({tab[0]: tab[1]})
                    tab_data = tab_data_tmp[self.property_name]
                data.append(tab_data)
        else:
            data = {}
            for widget in self.widgets:
                item_data = self.get_item_data(widget)
                data[item_data['key']] = item_data['value']

        cli_args = {'operation_id': 'patch-properties', 'data': [ {'op':self.op_type, 'path': self.property_name, 'value': data } ]}

        async def coroutine():
            self.app.start_progressing()
            response = await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
            self.app.stop_progressing()
            self.myparent.app_configuration = response
            self.future.set_result(DialogResult.ACCEPT)
            self.myparent.oauth_update_properties(start_index=self.myparent.oauth_update_properties_start_index)
        asyncio.ensure_future(coroutine())


    def get_widgets(
            self, 
            properties:dict,
            values: dict=None,
            styles: dict=None
            ) -> list:
        """Returns list of widgets for properties

        Args:
            properties (dict): properties to get widget
            values (dict): values of properties
            styes (dict): styles for widgets
        """

        if not values:
            values = {self.property_name: self.value}
        if not styles:
            styles = {'widget_style':'', 'string': cli_style.edit_text, 'boolean': cli_style.check_box}

        widgets = []
        for item_name in properties:
            item = properties[item_name]
            if item['type'] in ('integer', 'string'):
                widgets.append(
                    self.app.getTitledText(
                        item_name,
                        name=item_name,
                        value=str(values.get(item_name,'')),
                        text_type=item['type'],
                        style=styles['string'],
                        widget_style=styles['widget_style']
                    )
                )

            elif item['type'] == 'boolean':
                widgets.append(
                    self.app.getTitledCheckBox(
                        item_name,
                        name=item_name,
                        checked=values.get(item_name, False),
                        style=styles['boolean'],
                        widget_style=styles['widget_style']
                        )
                    )

            elif item['type'] == 'array' and item['items'].get('enum'):
                widgets.append(
                    self.app.getTitledCheckBoxList(
                        item_name, 
                        name=item_name, 
                        values=item['items']['enum'],
                        current_values=values.get(item_name, []),
                        style=styles['boolean'],
                        widget_style=styles['widget_style']
                        )
                    )

            elif item['type'] == 'array' and item['items'].get('type') in ('string', 'integer'):
                titled_text = self.app.getTitledText(
                                    item_name,
                                    name=item_name,
                                    height=4,
                                    text_type = item['items']['type'],
                                    value='\n'.join(values.get(item_name, [])),
                                    style=styles['string'],
                                    widget_style=styles['widget_style']
                                )
                titled_text.jans_list_type = True
                widgets.append(titled_text)

        return widgets


    def add_tab_element(
        self,
        properties:dict,
        values: dict
        ) -> None:
        """Adds element to tab widget
            Args:
            properties (dict): properties of element to add
            values (dict): values of properties
        """

        tab_name = '#{}'.format(len(self.tab_widget.tabs)+1)
        tab_widgets = self.get_widgets(properties, values=values, styles={'widget_style': cli_style.tab_selected, 'string': cli_style.tab_selected, 'boolean': cli_style.tab_selected})
        self.tab_widget.add_tab(tab_name, HSplit(tab_widgets, style=cli_style.tab_selected))
        if not values:
            self.tab_widget.set_tab(tab_name)

    def delete_tab_element(self) -> None:
        """Deletes currenlt oelemnt form tab widget
        """
        cur_tab = self.tab_widget.cur_tab
        cur_tab_name = self.tab_widget.tabs[cur_tab][0]
        self.tab_widget.remove_tab(cur_tab_name)

    def prepare_properties(self):
        """This method build the main value_content to edit the properties
        """

        properties = self.myparent.schema['properties'][self.property_name]

        if properties['type'] in ('string', 'integer', 'boolean'):
            self.widgets = self.get_widgets({self.property_name: properties})

        elif properties['type'] == 'array':
            if properties['items'].get('type') in ('string', 'integer', 'boolean'):
                self.widgets = self.get_widgets({self.property_name: properties})

            elif properties['items'].get('type') == 'array':
                self.tab_widget = JansTab(self)
                self.tab_widget.tab_content_type = 'array'
                item_property = {self.property_name: properties['items']}
                for entry_value in self.value:
                    self.add_tab_element(item_property, {self.property_name: entry_value})

                self.value_content = self.tab_widget
                add_entry_partial = partial(self.add_tab_element, item_property, {})
                self.buttons.append(Button(_("Add"), handler=add_entry_partial))
                self.buttons.append(Button(_("Delete"), handler=self.delete_tab_element))

            elif properties.get('properties'):
                self.tab_widget = JansTab(self)
                self.tab_widget.tab_content_type = 'object'
                for entry_value in self.value:
                    self.add_tab_element(properties['properties'], entry_value)
                self.value_content = self.tab_widget
                add_entry_partial = partial(self.add_tab_element, properties['properties'], {})
                self.buttons.append(Button(_("Add"), handler=add_entry_partial))
                self.buttons.append(Button(_("Delete"), handler=self.delete_tab_element))

        elif properties['type'] == 'object':
            self.widgets = self.get_widgets(properties['properties'], values=self.value)

        if not self.tab_widget:
            self.value_content = HSplit(self.widgets, width=D())

    def create_window(self) -> None:
        """Creates dialog window
        """

        self.dialog = Dialog(
            title=self.property_name,
            body= HSplit([self.value_content], padding=1,width=100,style='class:outh-uma-tabs'),
            buttons=self.buttons,
            with_background=False,
        )


    def __pt_container__(self)-> Dialog:
        """The container for the dialog itself

        Returns:
            Dialog: View Property
        """

        return self.dialog

