from functools import partial

from prompt_toolkit.layout import Window
from prompt_toolkit.widgets import Frame, Button
from prompt_toolkit.layout.containers import VSplit, HSplit
from prompt_toolkit.widgets import Button, Label, TextArea, Frame, CheckboxList
from prompt_toolkit.layout.dimension import D

from utils.multi_lang import _
from utils.utils import common_data
from wui_components.jans_label_container import JansLabelContainer
from wui_components.jans_cli_dialog import JansGDialog
from utils.static import cli_style


class JansLabelWidget:
    def __init__(self, title, values, data, label_width=None, add_handler=None, jans_name=''):

        if not label_width:
            label_width = int(common_data.app.dialog_width*1.1) - 26

        self.title = title
        self.jans_name = jans_name
        self.data = data
        self.values = values
        if add_handler:
            add_handler_func = partial(add_handler, self)
        else:
            add_handler_func = self.add_value
        add_button_container = VSplit([Window(), Button(_("Add"), handler=add_handler_func)])

        initial_labels = []
        for value_id in self.values:
            entry = self.get_label_entry(value_id)
            initial_labels.append(entry)


        self.container = JansLabelContainer(
            title=title,
            width=int(common_data.app.dialog_width*1.1) - 26,
            label_width=label_width,
            on_display=common_data.app.data_display_dialog,
            on_delete=self.delete_value,
            buttonbox=add_button_container,
            entries=initial_labels
        )

    def get_label_entry(self, value_id) -> tuple:
        for item_id, item_label in self.data:
                if item_id == value_id:
                    return (item_id, item_label)
        return (value_id, value_id)

    def add_value(self) -> None:

        add_checkbox = CheckboxList(values=[('', '')])

        def value_exists(value_id: str) -> bool:
            for item_id, item_label in self.container.entries:
                if item_id == value_id:
                    return True
            return False

        def add_selected_values(dialog):
            for value_id in add_checkbox.current_values:
                self.container.add_label(*self.get_label_entry(value_id))

        value_list = []
        for val in self.data:
            if not value_exists(val[0]):
                value_list.append(val)

        if not value_list:
            common_data.app.show_message(("No Data"), _("Currenlty you have no option to add"), tobefocused=self.container)
            return

        def on_text_changed(event):
            matching_items = []
            search_text = event.text
            for item in value_list:
                if search_text.lower() in item[1].lower():
                    matching_items.append(item)
            if matching_items:
                add_checkbox.values = matching_items
                self.add_frame.body = HSplit(children=[add_checkbox])
                add_checkbox._selected_index = 0
            else:
                self.add_frame.body = HSplit(children=[Label(text=_("No Items "), style=cli_style.label,
                                                                   width=len(_("No Items "))),], width=D())

        ta = TextArea(
            height=D(),
            width=D(),
            multiline=False,
        )

        ta.buffer.on_text_changed += on_text_changed

        add_checkbox.values = value_list
        self.add_frame = Frame(
            title="Checkbox list",
            body=HSplit(children=[add_checkbox]),
        )
        layout = HSplit(children=[
            VSplit(
                children=[
                    Label(text=_("Filter "), style=cli_style.label,
                          width=len(_("Filter "))),
                    ta
                ]),
            Window(height=1, char=' '),
            self.add_frame

        ])

        buttons = [Button(_("Cancel")), Button(
            _("OK"), handler=add_selected_values)]

        add_dialog = JansGDialog(
            common_data.app,
            title=_("Select to add"),
            body=layout,
            buttons=buttons)

        common_data.app.show_jans_dialog(add_dialog)

    def delete_value(self, value: list) -> None:

        def do_delete_value(dialog):
            self.container.remove_label(value[0])

        dialog = common_data.app.get_confirm_dialog(
            message=_(
                _("Are you sure want to delete \n<b>{}</b> ?").format(value[1])),
            confirm_handler=do_delete_value
        )

        common_data.app.show_jans_dialog(dialog)

    def get_values(self) -> list:
        values = [item[0] for item in self.container.entries]
        return values

    def __pt_container__(self) -> Frame:
        """Returns frame as container
        """
        return self.container
