from types import SimpleNamespace
from typing import Optional

import prompt_toolkit

from cli_style import style
from wui_components.jans_drop_down import DropDownWidget
from cli_tui.wui_components.jans_date_picker import DateSelectWidget
from wui_components.jans_spinner import Spinner


common_data = SimpleNamespace()

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
            else:
                return

            if getattr(me.window, 'text_type', None) == 'integer':
                if value_:
                    value_ = int(value_)

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


    def check_required_fields(self, container=None):
        missing_fields = []
        containers = [container] if container else [self.tabs[tab] for tab in self.tabs]

        for container in containers:
            for item in container.children:
                if hasattr(item, 'children') and len(item.children)>1 and hasattr(item.children[1], 'jans_name'):
                    if 'required' in item.children[0].style and not self.data.get(item.children[1].jans_name, None):
                        missing_fields.append(item.children[1].jans_name)
        if missing_fields:
            self.myparent.show_message("Please fill required fields", "The following fields are required:\n" + ', '.join(missing_fields))
            return False

        return True
