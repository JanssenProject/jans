import prompt_toolkit


from cli_style import style
from wui_components.jans_drop_down import DropDownWidget
from wui_components.jans_data_picker import DateSelectWidget



class DialogUtils:

    def make_data_from_dialog(self):
        data = {}
        for tab in self.tabs:
            for item in self.tabs[tab].children:
                if hasattr(item, 'children') and len(item.children)>1 and hasattr(item.children[1], 'jans_name'):
                    key_ = item.children[1].jans_name
                    if isinstance(item.children[1].me, prompt_toolkit.widgets.base.TextArea):
                        value_ = item.children[1].me.text
                    elif isinstance(item.children[1].me, prompt_toolkit.widgets.base.CheckboxList):
                        value_ = item.children[1].me.current_values
                    elif isinstance(item.children[1].me, prompt_toolkit.widgets.base.RadioList):
                        value_ = item.children[1].me.current_value
                    elif isinstance(item.children[1].me, prompt_toolkit.widgets.base.Checkbox):
                        value_ = item.children[1].me.checked
                    elif isinstance(item.children[1].me, DropDownWidget):
                        value_ = item.children[1].me.value
                    data[key_] = value_

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
