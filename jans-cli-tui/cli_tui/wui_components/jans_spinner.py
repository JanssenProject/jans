from prompt_toolkit.formatted_text import merge_formatted_text
from prompt_toolkit.key_binding import KeyBindings
from prompt_toolkit.layout import FormattedTextControl, Window
from typing import Optional, Sequence, Union
from prompt_toolkit.formatted_text import AnyFormattedText
from prompt_toolkit.key_binding.key_bindings import KeyBindings, KeyBindingsBase
class Spinner:
    def __init__(self, 
            value: Optional[int]= 1, 
            min_value: Optional[int]= 1, 
            max_value: Optional[int]= 100,  
            style: Optional[str]= 'bg:#cccccc fg:blue',  
            ) -> Window:

        self.value = value
        self.min_value = min_value
        self.max_value = max_value
        self.window = Window(
            content=FormattedTextControl(
                text=self._get_formatted_text,
                focusable=True,
                key_bindings=self._get_key_bindings(),
            ),
            style=style,
            height=1,
            width=len(str(self.max_value))+3,
            cursorline=False,
        )

    def _get_formatted_text(self) -> AnyFormattedText:
        spacing = len(str(self.max_value))+1
        result = [str(self.value).rjust(spacing) + ' ↕']
        return merge_formatted_text(result)

    def _get_key_bindings(self) -> KeyBindingsBase:
        kb = KeyBindings()

        @kb.add("up")
        def _go_left(event) -> None:
            if self.value > self.min_value:
                self.value -= 1

        @kb.add("down")
        def _go_right(event) -> None:
            if self.value < self.max_value:
                self.value += 1

        return kb

    def __pt_container__(self) -> Window:
        return self.window
