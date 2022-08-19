
from prompt_toolkit.application.current import get_app
from prompt_toolkit.key_binding import KeyBindings
from prompt_toolkit.layout.containers import Float, HSplit, Window
from prompt_toolkit.layout.controls import FormattedTextControl
from prompt_toolkit.formatted_text import HTML, merge_formatted_text
from prompt_toolkit.layout.margins import ScrollbarMargin
from prompt_toolkit.key_binding.bindings.focus import focus_next

class JansSelectBox:
    def __init__(self, entries=[]):
        self.entries = entries
        self.selected_line = 0
        self.container =HSplit(children=[Window(
            content=FormattedTextControl(
                text=self._get_formatted_text,
                focusable=True,
            ),
            height=len(entries),
            cursorline=False,
            width=15,
            style="bg:#4D4D4D",
            right_margins=[ScrollbarMargin(display_arrows=True),],
        )])

    def _get_formatted_text(self):
        result = []
        for i, entry in enumerate(self.entries):
            if i == self.selected_line:
                result.append(HTML('<style fg="ansired" bg="{}">{}</style>'.format('#ADD8E6', entry[1])))
            else:
                result.append(HTML('<b>{}</b>'.format(entry[1])))
            result.append("\n")

        return merge_formatted_text(result)


    def up(self):
        self.selected_line = (self.selected_line - 1) % len(self.entries)


    def down(self):
        self.selected_line = (self.selected_line + 1) % len(self.entries)

    def __pt_container__(self):
        return self.container


class DropDownWidget:
    def __init__(self, entries=[]):
        self.entries = entries
        self.text = "Enter to Select"
        self.dropdown = True
        self.window = Window(
            content=FormattedTextControl(
                text=self._get_text,
                focusable=True,
                 key_bindings=self._get_key_bindings(),
            ), height=5)

        self.select_box = JansSelectBox(self.entries)
        self.select_box_float = Float(content=self.select_box, xcursor=True, ycursor=True)


    def _get_text(self):
        if get_app().layout.current_window is self.window:
            return HTML('&gt; <style fg="ansired" bg="{}">{}</style> &lt;'.format('#00FF00', self.text))
        return '> {} <'.format(self.text)


    def _get_key_bindings(self):
        kb = KeyBindings()


        def _focus_next(event):
            focus_next(event)

        @kb.add("enter")
        def _enter(event) -> None:

            if self.select_box_float not in get_app().layout.container.floats:
                get_app().layout.container.floats.insert(0, self.select_box_float)
            else:
                self.text = self.select_box.entries[self.select_box.selected_line][1]
                get_app().layout.container.floats.remove(self.select_box_float)

        @kb.add("up")
        def _up(event):
            self.select_box.up()

        @kb.add("down")
        def _up(event):
            self.select_box.down()

        @kb.add("escape")
        @kb.add("tab")
        def _(event):
            if self.select_box_float in get_app().layout.container.floats:
                get_app().layout.container.floats.remove(self.select_box_float)

            _focus_next(event)

        return kb

    def __pt_container__(self):
        return self.window
