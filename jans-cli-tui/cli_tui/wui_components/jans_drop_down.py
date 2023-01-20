
from prompt_toolkit.application.current import get_app
from prompt_toolkit.key_binding import KeyBindings
from prompt_toolkit.layout.containers import Float, HSplit, Window
from prompt_toolkit.layout.controls import FormattedTextControl
from prompt_toolkit.formatted_text import HTML, merge_formatted_text
from prompt_toolkit.layout.margins import ScrollbarMargin
from prompt_toolkit.key_binding.bindings.focus import focus_next, focus_previous
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.layout.containers import (
    AnyContainer,
)
from prompt_toolkit.formatted_text import AnyFormattedText
from prompt_toolkit.key_binding.key_bindings import KeyBindings, KeyBindingsBase

from prompt_toolkit.layout.dimension import AnyDimension
from typing import Optional, Sequence, Union
from typing import TypeVar, Callable

import cli_style

class JansSelectBox:
    """_summary_
    """

    def __init__(
        self,
        values: Optional[list] = [],
        value: Optional[str] = '',
        height: AnyDimension= 4,
        rotatable_up: Optional[bool] = True,
        rotatable_down: Optional[bool] = True,
        ) -> HSplit:
        """_summary_

        Args:
            values (list, optional): _description_. Defaults to [].
            value (_type_, optional): _description_. Defaults to None.
            height (int, optional): _description_. Defaults to 4.
            rotatable_up (bool, optional): _description_. Defaults to True.
            rotatable_down (bool, optional): _description_. Defaults to True.
        """

        self.values = values
        self.values_flag = (values[0],values[-1])
        self.set_value(value)
        # --------------------------------------------------- #
        self.height = min(len(self.values), height)
        self.rotatable_up = rotatable_up
        self.rotatable_down = rotatable_down
        # --------------------------------------------------- #

        self.container = HSplit(children=[Window(
            content=FormattedTextControl(
                text=self._get_formatted_text,
                focusable=True,
            ),
            height=self.height,
            cursorline=False,
            width=D(),  # 15,
            style='bg:#4D4D4D',
            right_margins=[ScrollbarMargin(display_arrows=True), ],
            wrap_lines=True,
            allow_scroll_beyond_bottom=True,
        )])

    def set_value(
        self, 
        value:str
        )-> None:
        """_summary_

        Args:
            value (_type_): _description_
        """
        self.value = value

        for i, val in enumerate(self.values):
            if val[0] == value:
                self.selected_line = i
                break
        else:
            self.selected_line = 0

    def _get_formatted_text(self)-> AnyFormattedText:
        """_summary_

        Returns:
            _type_: _description_
        """
        result = []
        for i, entry in enumerate(self.values):
            if i == self.selected_line:
                result.append(
                    HTML('<style fg="ansired" bg="{}">{}</style>'.format(cli_style.drop_down_itemSelect, entry[1])))
            else:
                result.append(HTML('<b>{}</b>'.format(entry[1])))
            result.append("\n")

        return merge_formatted_text(result)

    def shift(
        self, 
        seq:str, 
        n:int,
        )-> str:
        """_summary_

        Args:
            seq (_type_): _description_
            n (_type_): _description_

        Returns:
            _type_: _description_
        """
        return seq[n:]+seq[:n]

    def up(self)-> None:
        """_summary_
        """
        if self.selected_line == 0:
            if self.rotatable_up and self.values[self.selected_line] == self.values_flag[0]:
                pass
            else:
                self.values = self.shift(self.values, -1)
        else:
            self.selected_line = (self.selected_line - 1) % (self.height)

        self.set_value(self.values[self.selected_line][0])

    def down(self)-> None:
        """_summary_
        """
        if self.selected_line + 1 == (self.height):
            if self.rotatable_down and self.values[self.selected_line] == self.values_flag[-1]:
                pass
            else:
                self.values = self.shift(self.values, 1)
        else:
            self.selected_line = (self.selected_line + 1) % (self.height)

        self.set_value(self.values[self.selected_line][0])
        
    def __pt_container__(self)-> HSplit:
        return self.container


class DropDownWidget:
    """This is a Combobox widget (drop down) to select single from multi choices
    """

    def __init__(
        self,
        values: Optional[list] = [],
        value: Optional[str] = '',
        on_value_changed: Callable= None, 
        )->Window:
        """init for DropDownWidget
        Args:
            values (list, optional): List of values to select one from them. Defaults to [].
            value (str, optional): The defualt selected value. Defaults to None.

        Examples:
            widget=DropDownWidget(
                values=[('client_secret_basic', 'client_secret_basic'), ('client_secret_post', 'client_secret_post'), ('client_secret_jwt', 'client_secret_jwt'), ('private_key_jwt', 'private_key_jwt')],
                value=self.data.get('tokenEndpointAuthMethodsSupported'))
        """
        self.values = values
        self.on_value_changed = on_value_changed
        values.insert(0, (None, 'Select One'))
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
            ), height=D())  # 5  ## large sized enties get >> (window too small)

        self.select_box = JansSelectBox(
            values=self.values, value=value, rotatable_down=True, rotatable_up=True, height=4)
        self.select_box_float = Float(
            content=self.select_box, xcursor=True, ycursor=True)

    @property
    def value(self)-> str:
        """Getter for the value property

        Returns:
            str: The selected value
        """
        return self.select_box.value

    @value.setter
    def value(
        self, 
        value:str,
        )-> None:
        self.select_box.set_value(value)
        if self.on_value_changed:
            self.on_value_changed(value)

    def _get_text(self)-> AnyFormattedText:
        """To get The selected value

        Returns:
            str: The selected value
        """
        if get_app().layout.current_window is self.window:
            return HTML('&gt; <style fg="ansired" bg="{}">{}</style> &lt;'.format(cli_style.drop_down_hover, self.text))
        return '> {} <'.format(self.text)

    def _get_key_bindings(self)-> KeyBindingsBase:
        """All key binding for the Dialog with Navigation bar

        Returns:
            KeyBindings: The method according to the binding key
        """
        kb = KeyBindings()

        def _focus_next(event):
            focus_next(event)

        def _focus_previous(event):
            focus_previous(event)

        @kb.add("enter")
        def _enter(event) -> None:
            if self.select_box_float not in get_app().layout.container.floats:
                get_app().layout.container.floats.append(self.select_box_float)
            else:
                self.text = self.select_box.values[self.select_box.selected_line][1]
                get_app().layout.container.floats.remove(self.select_box_float)

            self.value = self.select_box.value

        @kb.add('up')
        def _up(event):
            self.select_box.up()

        @kb.add('down')
        def _down(event):
            self.select_box.down()

        @kb.add('escape')
        @kb.add('tab')
        def _(event):
            if self.select_box_float in get_app().layout.container.floats:
                get_app().layout.container.floats.remove(self.select_box_float)

            _focus_next(event)

        @kb.add('s-tab')
        def _(event):
            if self.select_box_float in get_app().layout.container.floats:
                get_app().layout.container.floats.remove(self.select_box_float)

            _focus_previous(event)

        return kb

    def __pt_container__(self)-> Window:
        return self.window
