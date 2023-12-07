import re
import sys
import datetime

from types import SimpleNamespace
from typing import Optional

import prompt_toolkit

from cli_style import style
from wui_components.jans_drop_down import DropDownWidget
from wui_components.jans_spinner import Spinner
from wui_components.jans_vetrical_nav import JansVerticalNav

from wui_components.jans_date_picker import DateSelectWidget
from utils.multi_lang import _

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

        for container in containers:
            for item in container.children:
                if hasattr(item, 'children') and len(item.children)>1 and hasattr(item.children[1], 'jans_name'):
                    if 'required' in item.children[0].style and not data.get(item.children[1].jans_name, None):
                        missing_fields.append(item.children[1].jans_name)

        if missing_fields:
            app = self.app if hasattr(self, 'app') else self.myparent
            app.show_message(_("Please fill required fields"), _("The following fields are required:\n") + ', '.join(missing_fields), tobefocused=tobefocused)
            return False

        return True


def fromisoformat(dt_str):
    dt, _, us = dt_str.partition(".")
    dt = datetime.datetime.strptime(dt, "%Y-%m-%dT%H:%M:%S")
    if us:
        us = int(us.rstrip("Z"), 10)
        dt = dt + datetime.timedelta(microseconds=us)
    return dt

def check_email(email):
    return re.match('^[_a-z0-9-]+(\.[_a-z0-9-]+)*@[a-z0-9-]+(\.[a-z0-9-]+)*(\.[a-z]{2,})$', email, re.IGNORECASE)
