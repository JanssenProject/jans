from typing import Callable, Optional
from prompt_toolkit.application import get_app
from prompt_toolkit.filters import to_filter

from prompt_toolkit.formatted_text import HTML, AnyFormattedText, merge_formatted_text,to_formatted_text
from prompt_toolkit.key_binding import KeyBindings
from prompt_toolkit.layout import FormattedTextControl, Window
from prompt_toolkit.widgets import Label, Frame, Box, Button
from prompt_toolkit.layout.containers import HSplit, VSplit, DynamicContainer
from prompt_toolkit.layout.dimension import D


class JansLabelContainer:
    def __init__(
        self,
        title: Optional[str]='',
        width: Optional[int]=50,
        label_width: Optional[int]=None,
        on_enter: Optional[Callable]=None,
        on_delete: Optional[Callable]=None,
        on_display: Optional[Callable]=None,
        buttonbox: Optional[Button]=None,
        entries: Optional=None,
        height: Optional[int]=1,
        ) -> None:

        """Label container for Jans

        Args:
            title (str, optional): title of frame
            width (int, optional): sets width of container, default is 50
            on_enter (Callable, optional): When enter this function is called.
            on_delete (Callable, optional): this function is called when an entry is deleted
            on_display (Callable, optional): this function is called when user press d on keyboard
            buttonbox (Button, optional): buntton box to be appended at the end
        """
        if not label_width:
            label_width = width
        self.width = width
        self.on_enter = on_enter
        self.on_delete = on_delete
        self.on_display = on_display
        self.height = height
        self.entries = [] if not entries else entries
        self.invalidate = False
        self.selected_entry = 0
        self.body_content = FormattedTextControl(
                text=self._get_formatted_text,
                focusable=bool(self.entries),
                key_bindings=self._get_key_bindings(),
            )
        self.body = Window(
            content=self.body_content,
            width=self.width-2,
            height=self.height
        )
        self.body.jans_label_values = self.entries

        widgets = [self.body]
        if buttonbox:
            widgets.append(Window(height=1))
            widgets.append(buttonbox)

        self.container = Box(Frame(HSplit(widgets, width=label_width-3), title=title), width=label_width, height=self.body.height+4)

    def _get_formatted_text(self) -> AnyFormattedText:
        """Internal function for formatting entries
            
        Returns:
            merged formatted text.
        """
        
        result = []
        line_width = 0
        line_count = 1
        for i, entry in enumerate(self.entries):
            if line_width + len(entry[1]) + 2 > self.width:
                result.append('\n\n')
                line_width = 0
                line_count += 2

            line_width += len(entry[1]) + 2

            if i == self.selected_entry:
                result.append(HTML('<style fg="{}" bg="{}">{}</style>'.format("white", "darkgrey", entry[1])))
            else:
                result.append(HTML('<style fg="{}" bg="{}">{}</style>'.format("black", "lightgrey", entry[1])))
            result.append('  ')

        if line_count > self.body.height:
            self.body.height = line_count

        if self.invalidate:
            get_app().invalidate()
            self.invalidate = False

        new_height = self.body.height+4
        if new_height != self.container.container.height:
            self.container.container.height = new_height
            get_app().invalidate()

        return merge_formatted_text(result)

    def add_label(
        self, 
        label_id:str, 
        label_title:str
        ) -> None:
        """Adds label to container

        Args:
            label_id (str): ID for label
            label_title (str): Text to be displayed as label
        """
        if not self.body_content.is_focusable():
            self.body_content.focusable = to_filter(True)

        self.invalidate = True
        self.entries.append((label_id, label_title))


    def remove_label(self, label_id: str) -> None:
        """Removes label from container

        Args:
            label_id (str): ID for label
            label_title (str): Text to be displayed as label
        """
        for entry in self.entries[:]:
            if entry[0] == label_id:
                self.entries.remove(entry)
                self.invalidate = True

        if not self.entries and self.body_content.is_focusable():
            self.body_content.focusable = to_filter(False)

    def _get_key_bindings(self) -> None:
        kb = KeyBindings()

        @kb.add("left")
        def _go_up(event) -> None:
            self.selected_entry = (self.selected_entry - 1) % len(self.entries)

        @kb.add("right")
        def _go_up(event) -> None:
            self.selected_entry = (self.selected_entry + 1) % len(self.entries)

        @kb.add("enter")
        def _enter(event) -> None:
            if self.on_enter:
                self.on_enter(self.entries[self.selected_entry])

        @kb.add("delete")
        def _delete(event) -> None:
           if self.on_delete:
               self.on_delete(self.entries[self.selected_entry])

        @kb.add('d')
        def _display(event):
           if self.on_display:
               self.on_display(selected=self.entries[self.selected_entry], data=self.entries[self.selected_entry])

        return kb

    def __pt_container__(self) -> Frame:
        """Returns frame as container
        """
        return self.container
