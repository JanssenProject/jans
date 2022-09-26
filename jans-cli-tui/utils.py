import prompt_toolkit


from cli_style import style
from wui_components.jans_drop_down import DropDownWidget
from wui_components.jans_data_picker import DateSelectWidget



class DialogUtils:


    def get_item_data(self, item):
        if hasattr(item, 'me'):
            me = item.me
            key_ = me.window.jans_name
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
            elif isinstance(me,DateSelectWidget):
                value_ = me.value

            return {'key':key_, 'value':value_}


    def make_data_from_dialog(self):
        data = {}
        for tab in self.tabs:

            for item in self.tabs[tab].children:
                item_data = self.get_item_data(item)
                if item_data:
                    data[item_data['key']] = item_data['value']

        return data


    def check_required_fields(self):
        missing_fields = []
        for tab in self.tabs:
            for item in self.tabs[tab].children:
                if hasattr(item, 'children') and len(item.children)>1 and hasattr(item.children[1], 'jans_name'):
                    if 'required-field' in item.children[0].style and not self.data.get(item.children[1].jans_name, None):
                        missing_fields.append(item.children[0].content.text().strip().strip(':'))
        if missing_fields:
            self.myparent.show_message("Please fill required fields", "The following fields are required:\n" + ', '.join(missing_fields))
            return False

        return True
