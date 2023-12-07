
from typing import Optional, Callable

from prompt_toolkit.application import Application
from prompt_toolkit.styles import Style
from prompt_toolkit.formatted_text import AnyFormattedText
from prompt_toolkit.key_binding.key_bindings import KeyBindings, KeyBindingsBase
from prompt_toolkit.layout.margins import ScrollbarMargin
from prompt_toolkit.widgets import Label
from prompt_toolkit.layout.containers import Window, HSplit, DynamicContainer, AnyContainer
from prompt_toolkit.layout.controls import FormattedTextControl
from prompt_toolkit.formatted_text import HTML, merge_formatted_text


class JansTableWidget():
    """This is a tabled widget used for Jan Client TUI
    """
    def __init__(
            self,
            app: Application,
            data: list,
            headers: Optional[list] = None,
            preferred_size: Optional[list] = None,
            height: Optional[int] = 5,
            padding: int = 3,
            on_display: Optional[Callable] = None,
            style: Optional[Style] = 'class:table-white'
            ) -> None:

        """initfor JansTab

        Args:
            app (application): This is the parent application for the dialog, to caluclate the size
            data (list): Data to be displayed
            headers (optional, list): Table headers
            preferred_size (optional, list): Preferred column sizes
            padding (optional, int): space between columns
        """

        self.app = app
        self.data = data
        self.headers = headers or []
        self.preferred_size = preferred_size
        self.height = height
        self.padding = padding
        self.style = style
        self.on_display = on_display

        self._calculated_column_sizes()
        self.create_window()


    def _calculated_column_sizes(self):
        if not self.preferred_size:
            max_col_sizes = [len(c) for c in self.data[0]]
            for row in self.data[1:]:
                for i, c in enumerate(row):
                    if len(c) > max_col_sizes[i]:
                        max_col_sizes[i] = len(c)
            total_col_size = sum(max_col_sizes)
            max_width = self.app.dialog_width - 4 - len(max_col_sizes) * self.padding
            if total_col_size > max_width:
                max_col_ratio = [c/total_col_size for c in max_col_sizes]
                self.preferred_size = [int(r*max_width) for r in max_col_ratio]
            else:
                self.preferred_size = max_col_sizes

    def create_window(self)-> None:
        """This method creates table widget container
        """
        self.selected_line = 0
        body = [Window(
                content=FormattedTextControl(
                    text=self._get_formatted_text,
                    focusable=True,
                    key_bindings=self._get_key_bindings(),
                ),
                style=self.style,
                height=self.height,
                cursorline=True,
                right_margins=[ScrollbarMargin(display_arrows=True)],
                )
                ]
        if self.headers:
            headers = [h.ljust(self.preferred_size[i]) for i, h in enumerate(self.headers)]
            header_text = HTML('<b><u>' + (' '*self.padding).join(headers) + '</u></b>')
            body.insert(0, Label(header_text, style=self.style))
        self.container = HSplit(body)

    def _get_formatted_text(self):
        result = []
        for i, row in enumerate(self.data):
            if i == self.selected_line:
                result.append([("[SetCursorPosition]", "")])
            row_list = []
            for j, c in enumerate(row):
                if len(c) > self.preferred_size[j]:
                    c = c[:self.preferred_size[j]-3] + '...'
                    
                row_list.append(c.replace("\n", "\\n").ljust(self.preferred_size[j]))
            entry = HTML((' '*self.padding).join(row_list))
            result.append(entry)
            result.append("\n")

        return merge_formatted_text(result)

    def _get_key_bindings(self):
        kb = KeyBindings()

        @kb.add("up")
        def _go_up(event) -> None:
            self.selected_line = (self.selected_line - 1) % len(self.data)

        @kb.add("down")
        def _go_up(event) -> None:
            self.selected_line = (self.selected_line + 1) % len(self.data)

        @kb.add("d")
        def _display(event) -> None:
            if self.on_display:
                self.on_display(selected_line=self.selected_line, data=self.data[self.selected_line])

        return kb

    def __pt_container__(self) -> DynamicContainer:
        return self.container
