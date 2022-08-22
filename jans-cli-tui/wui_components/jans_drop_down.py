
from prompt_toolkit.application.current import get_app
from prompt_toolkit.key_binding import KeyBindings
from prompt_toolkit.layout.containers import Float, HSplit, Window
from prompt_toolkit.layout.controls import FormattedTextControl
from prompt_toolkit.formatted_text import HTML, merge_formatted_text
from prompt_toolkit.layout.margins import ScrollbarMargin
from prompt_toolkit.key_binding.bindings.focus import focus_next
from prompt_toolkit.layout.dimension import D

class JansSelectBox:
    def __init__(self, values=[], value=None, height=4, rotatable_up=True, rotatable_down=True):
        self.values = values
        self.value = value

        for i, val in enumerate(values):
            if val[0] == value:
                self.selected_line = i
                break
        else:
            self.selected_line = 0

        # --------------------------------------------------- #
        self.height=min(len(self.values), height)
        self.rotatable_up = rotatable_up
        self.rotatable_down = rotatable_down
        # --------------------------------------------------- #

        self.container =HSplit(children=[ Window(
            content=FormattedTextControl(
                text=self._get_formatted_text,
                focusable=True,
            ),
            height=self.height,
            cursorline=False,
            width=D(),  #15,
            style="bg:#4D4D4D",
            right_margins=[ScrollbarMargin(display_arrows=True),],
            wrap_lines=True,
            allow_scroll_beyond_bottom=True,
        )])

    def _get_formatted_text(self):
        result = []
        for i, entry in enumerate(self.values):
            if i == self.selected_line:
                result.append(HTML('<style fg="ansired" bg="{}">{}</style>'.format('#ADD8E6', entry[1])))
            else:
                result.append(HTML('<b>{}</b>'.format(entry[1])))
            result.append("\n")

        return merge_formatted_text(result)


    def shift(self,seq, n):
        return seq[n:]+seq[:n]

    def up(self):
        if self.selected_line == 0 :
            if self.rotatable_up and  self.values[self.selected_line] == self.values[0]:
                pass
            else :
                self.values = self.shift(self.values,-1) 
        else :
            self.selected_line = (self.selected_line - 1) % (self.height)


    def down(self):

        if self.selected_line +1 == (self.height):
            if self.rotatable_down and  self.values[self.selected_line] == self.values[-1]:
                pass
            else:
                self.values = self.shift(self.values, 1)
        else :
            self.selected_line = (self.selected_line + 1) % (self.height)


    def __pt_container__(self):
        return self.container


class DropDownWidget:
    def __init__(self, values=[], value=None):
        self.values = values
        for val in values:
            if val[0] == value:
                self.text = val[1]
                break
        else:
            self.text = self.values[0][1] if self.values else "Enter to Select"

        self.dropdown = True
        self.window = Window(
            content=FormattedTextControl(
                text=self._get_text,
                focusable=True,
                 key_bindings=self._get_key_bindings(),
            ), height=D()) #5  ## large sized enties get >> (window too small)

        self.select_box = JansSelectBox(values=self.values, value=value, rotatable_down=True, rotatable_up=True, height=4)
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
                get_app().layout.container.floats.append(self.select_box_float)
            else:
                self.text = self.select_box.values[self.select_box.selected_line][1]
                get_app().layout.container.floats.remove(self.select_box_float)

            self.value = self.select_box.value

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
