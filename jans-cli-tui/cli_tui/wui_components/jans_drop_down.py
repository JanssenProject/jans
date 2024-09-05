
from prompt_toolkit.application.current import get_app
from prompt_toolkit.key_binding import KeyBindings
from prompt_toolkit.layout.containers import Float, HSplit, Window, ScrollOffsets, AnyContainer
from prompt_toolkit.layout.controls import FormattedTextControl
from prompt_toolkit.formatted_text import HTML, merge_formatted_text
from prompt_toolkit.layout.margins import ScrollbarMargin
from prompt_toolkit.key_binding.bindings.focus import focus_next, focus_previous
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.formatted_text import AnyFormattedText
from prompt_toolkit.key_binding.key_bindings import KeyBindings, KeyBindingsBase

from prompt_toolkit.layout.dimension import AnyDimension
from typing import Optional, Sequence, Union
from typing import TypeVar, Callable

import cli_style
from utils.multi_lang import _

class JansSelectBox:
    """_summary_
    """

    def __init__(
        self,
        values: Optional[list] = None,
        value: Optional[str] = '',
        height: AnyDimension = 4,
        ) -> HSplit:
        """_summary_

        Args:
            values (list, optional): _description_. Defaults to [].
            value (_type_, optional): _description_. Defaults to None.
            height (int, optional): _description_. Defaults to 4.
        """

        self.values = values if values else []
        self.set_value(value)
        self.height = min(len(self.values), height)

        self.window = Window(
            content=FormattedTextControl(
                text=self._get_formatted_text,
                focusable=True,
            ),
            height=self.height,
            cursorline=False,
            width=D(),
            style='bg:#4D4D4D',
            scroll_offsets=ScrollOffsets(top=2, bottom=2),
            right_margins=[ScrollbarMargin(display_arrows=True)],
        )

        self.container = HSplit(children=[self.window])

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
                result.append([("[SetCursorPosition]", "")])
                result.append(
                    HTML('<style fg="ansired" bg="{}">{}</style>'.format(cli_style.drop_down_itemSelect, entry[1])))
            else:
                result.append(HTML('<b>{}</b>'.format(entry[1])))
            result.append("\n")

        return merge_formatted_text(result)



    def _set_value(self):
        self.value = self.values[self.selected_line][0]

    def up(self)-> None:
        """_summary_
        """
        if self.selected_line < len(self.values) and self.selected_line > 0:
            self.selected_line = self.selected_line - 1
            self._set_value()


    def down(self)-> None:
        """_summary_
        """
        if self.selected_line < len(self.values) - 1:
            self.selected_line = self.selected_line + 1
            self._set_value()

    def __pt_container__(self)-> HSplit:
        return self.container


class DropDownWidget:
    """This is a Combobox widget (drop down) to select single from multi choices
    """

    def __init__(
        self,
        values: Optional[list] = None,
        value: Optional[str] = '',
        on_value_changed: Callable= None,
        select_one_option: Optional[bool] = True,
        ) -> Window:
        """init for DropDownWidget
        Args:
            values (list, optional): List of values to select one from them. Defaults to [].
            value (str, optional): The defualt selected value. Defaults to None.
            select_one_option(bool, optional): Add 'Select One' as first option

        Examples:
            widget=DropDownWidget(
                values=[('client_secret_basic', 'client_secret_basic'), ('client_secret_post', 'client_secret_post'), ('client_secret_jwt', 'client_secret_jwt'), ('private_key_jwt', 'private_key_jwt')],
                value=self.data.get('tokenEndpointAuthMethodsSupported'))
        """
        self.value_list = values if values else []
        self._value = value
        self.on_value_changed = on_value_changed
        if select_one_option:
            self.value_list.insert(0, (None, _("Select One")))

        if not self.value_list:
            self.display_value = _("No option was provided")

        for val in self.value_list:
            if val[0] == value:
                self.display_value = val[1]
                break
        else:
            if select_one_option:
                self.display_value = self.value_list[0][1] if self.value_list else _("Enter to Select")
            else:
                self.display_value = self.value_list[0][1]

        self.dropdown = True
        self.window = Window(
            content=FormattedTextControl(
                text=self._get_text,
                focusable=True,
                key_bindings=self._get_key_bindings(),
            ), height=D())

        self.select_box = JansSelectBox(
                                values=self.value_list,
                                value=value,
                                height=4
                                )

        self.select_box_float = Float(
                            content=self.select_box,
                            xcursor=True,
                            ycursor=True
                            )

    @property
    def value(self)-> str:
        """Getter for the value property

        Returns:
            str: The selected value
        """
        return self._value

    @value.setter
    def value(
        self, 
        value:str,
        )-> None:
        self._value = value
        self.select_box.set_value(value)
        for val in self.value_list:
            if val[0] == value:
                self.display_value = val[1]
                break
        else:
            if self.value_list:
                self.display_value = self.value_list[0][1]

        if self.on_value_changed:
            self.on_value_changed(value)

    @property
    def values(self) -> list:
        """Getter for the values property

        Returns:
            list: values of dropdown widget
        """
        return self.value_list

    @values.setter
    def values(
        self, 
        values:list,
        )-> None:
        self.value_list = values
        self.select_box.values = values
        self.select_box.window.height = len(values)
        self.select_box.height = self.select_box.window.height

    def _get_text(self)-> AnyFormattedText:
        """To get The selected value

        Returns:
            str: The selected value
        """
        if get_app().layout.current_window is self.window:
            return HTML('&gt; <style fg="ansired" bg="{}">{}</style> &lt;'.format(cli_style.drop_down_hover, self.display_value))
        return '> {} <'.format(self.display_value)

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
            if self.select_box_float in get_app().layout.container.floats:
                self.display_value = self.select_box.values[self.select_box.selected_line][1]
                self._value = self.select_box.values[self.select_box.selected_line][0]
                get_app().layout.container.floats.remove(self.select_box_float)
                if self.on_value_changed:
                    self.on_value_changed(self._value)
            else:
                get_app().layout.container.floats.append(self.select_box_float)



        @kb.add('up')
        def _up(event):
            if self.select_box_float in get_app().layout.container.floats:
                self.select_box.up()

        @kb.add('down')
        def _down(event):
            if self.select_box_float in get_app().layout.container.floats:
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
