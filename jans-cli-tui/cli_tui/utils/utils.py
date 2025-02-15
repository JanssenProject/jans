import re
import sys
import datetime

from types import SimpleNamespace
from typing import Optional, List

import prompt_toolkit

from cli_style import style
from wui_components.jans_drop_down import DropDownWidget
from wui_components.jans_spinner import Spinner
from wui_components.jans_vetrical_nav import JansVerticalNav
from utils.static import cli_style, common_strings
from wui_components.jans_date_picker import DateSelectWidget
from utils.multi_lang import _

common_data = SimpleNamespace()
common_data.background_tasks_feeds = {}

class DialogUtils:


    def get_item_data(self, item):
        if hasattr(item, 'me'):
            me = item.me
            key_ = me.window.jans_name
            if key_.startswith('__') and key_.endswith('__'):
                return
            if isinstance(me, prompt_toolkit.widgets.base.TextArea):
                value_ = me.text
            elif isinstance(me, prompt_toolkit.widgets.base.Checkbox):
                value_ = me.checked
            elif isinstance(me, prompt_toolkit.widgets.base.CheckboxList):
                value_ = me.current_values
            elif isinstance(me, prompt_toolkit.widgets.base.RadioList):
                value_ = me.current_value
            elif isinstance(me, DropDownWidget):
                value_ = me.value
            elif isinstance(me, DateSelectWidget):
                value_ = me.value
            elif isinstance(me, Spinner):
                value_ = me.value
            elif isinstance(me, JansVerticalNav):
                value_ = {lst[0]: lst[1] for lst in me.data}
                
            elif isinstance(me, prompt_toolkit.layout.containers.VSplit):
                for wid in item.children:
                    self.get_item_data(wid)
            else:
                return

            if getattr(me.window, 'text_type', None) == 'integer':
                if value_:
                    value_ = int(value_)

            if getattr(item, 'jans_list_type', False):
                if not value_.strip():
                    value_ = []
                else:
                    value_ = value_.split('\n')

            return {'key':key_, 'value':value_}


    def make_data_from_dialog(
            self, 
            tabs: Optional[dict]={}
            ) -> dict:

        data = {}
        process_tabs = tabs or self.tabs

        for tab in process_tabs:
            for item in process_tabs[tab].children:
                item_data = self.get_item_data(item)
                if item_data:
                    data[item_data['key']] = item_data['value']

        return data


    def check_required_fields(self, container=None, data=None, tobefocused=None):
        missing_fields = []
        if not data:
            data = self.data

        containers = [container] if container else [self.tabs[tab] for tab in self.tabs]

        def check_subitmes(items):
            for item in items:
                if hasattr(item, 'children') and len(item.children)>1 and hasattr(item.children[1], 'jans_name'):
                    if 'required' in item.children[0].style and not data.get(item.children[1].jans_name, None):
                        missing_fields.append(item.children[1].jans_name)

                if isinstance(item, prompt_toolkit.layout.containers.DynamicContainer):
                    sub_children = item.get_children()
                    if sub_children:
                        for sc in sub_children:
                            check_subitmes(sc.children)

        for container in containers:
            check_subitmes(container.children)

        if missing_fields:
            common_data.app.show_message(_("Please fill required fields"), _("The following fields are required:\n") + ', '.join(missing_fields), tobefocused=tobefocused)
            return False

        return True

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
            values = {}
        if not styles:
            styles = {'widget_style':'', 'string': cli_style.edit_text, 'boolean': cli_style.check_box}

        widgets = []
        for item_name in properties:
            item = properties[item_name]
            if item['type'] in ('integer', 'string'):
                if item.get('enum'):
                    widgets.append(
                        common_data.app.getTitledWidget(
                                item_name,
                                name=item_name,
                                widget=DropDownWidget(
                                    values=[(str(enum), str(enum)) for enum in item['enum']],
                                    value=str(values.get(item_name) or '')
                                    ),
                                style=styles['string'],
                                )
                        )
                else:
                    widgets.append(
                        common_data.app.getTitledText(
                            item_name,
                            name=item_name,
                            value=str(values.get(item_name) or ''),
                            text_type=item['type'],
                            style=styles['string'],
                            widget_style=styles['widget_style']
                            )
                        )

            elif item['type'] == 'boolean':
                widgets.append(
                    common_data.app.getTitledCheckBox(
                        item_name,
                        name=item_name,
                        checked=values.get(item_name, False),
                        style=styles['boolean'],
                        widget_style=styles['widget_style']
                        )
                    )

            elif item['type'] == 'array' and item['items'].get('enum'):
                widgets.append(
                    common_data.app.getTitledCheckBoxList(
                        item_name, 
                        name=item_name, 
                        values=item['items']['enum'],
                        current_values=values.get(item_name, []),
                        style=styles['boolean'],
                        widget_style=styles['widget_style']
                        )
                    )

            elif item['type'] == 'array' and item['items'].get('type') in ('string', 'integer'):
                titled_text = common_data.app.getTitledText(
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

def fromisoformat(dt_str=None):
    if not dt_str:
        return
    dt, _, us = dt_str.partition(".")
    dt = datetime.datetime.strptime(dt, "%Y-%m-%dT%H:%M:%S")
    if us:
        us = int(us.rstrip("Z"), 10)
        dt = dt + datetime.timedelta(microseconds=us)
    return dt

def check_email(email):
    return re.match('^[_a-z0-9-]+(\.[_a-z0-9-]+)*@[a-z0-9-]+(\.[a-z0-9-]+)*(\.[a-z]{2,})$', email, re.IGNORECASE)

def get_help_with(helps: str='', without: List[str]=None):
    if not without:
        without = []
    help_list = []
    for key_ in common_strings.__dict__:
        if key_.startswith('help_') and key_[5:] not in without:
            help_list.append(common_strings.__dict__[key_])
    if helps:
        help_list.insert(-1, helps)
    return '\n'.join(help_list)
