import math
import textwrap

from typing import Optional

from prompt_toolkit.application import Application
from prompt_toolkit.styles import Style
from prompt_toolkit.formatted_text import AnyFormattedText
from prompt_toolkit.key_binding.key_bindings import KeyBindings, KeyBindingsBase
from prompt_toolkit.layout.margins import ScrollbarMargin
from prompt_toolkit.widgets import Label, TextArea
from prompt_toolkit.layout.containers import Window, VSplit, HSplit, DynamicContainer, AnyContainer
from prompt_toolkit.layout.controls import FormattedTextControl
from prompt_toolkit.formatted_text import HTML, merge_formatted_text
from prompt_toolkit.layout import ScrollablePane
from prompt_toolkit.layout.dimension import D


class JansTableWidget():
    """This is a tabled widget used for Jan Client TUI
    """
    def __init__(
            self,
            app: Application,
            data: list,
            headers: Optional[list] = None,
            preferred_size: Optional[list] = None,
            padding: int = 1,
            max_height = 10,
            header_style: Optional[Style] = 'class:table-white-header',
            data_style: Optional[Style] = 'class:table-white-data',
            bg_style = "class:table-white-bg"
            ) -> None:

        """initfor JansTab

        Args:
            myparent (application): This is the parent application for the dialog, to caluclate the size
            data (list): Data to be displayed
            headers (optional, list): Table headers
            preferred_size (optional, list): Preferred column sizes
            padding (optional, int): space between columns
        """

        self.app = app
        self.data = data
        self.headers = headers or []
        self.preferred_size = preferred_size
        self.padding = padding
        self.header_style = header_style
        self.data_style = data_style
        self.bg_style = bg_style
        self.max_height = max_height
        self._calculated_column_sizes()
        self.create_window()


    def _calculated_column_sizes(self):
        if not self.preferred_size:
            if self.headers:
                max_col_sizes = [len(h) for h in self.headers]
            else:
                max_col_sizes = [2, 2, 2, 2, 2, 2, 2]

            for row in self.data:
                for i, c in enumerate(row):
                    if c:
                        for cl in c.splitlines():
                            if len(cl) > max_col_sizes[i]:
                                max_col_sizes[i] = len(cl)+1

            total_col_size = sum(max_col_sizes)

            max_width = self.app.dialog_width - 5 - len(max_col_sizes) * self.padding
            if total_col_size > max_width:
                max_col_ratio = [c/total_col_size for c in max_col_sizes]
                self.preferred_size = [math.ceil(r*max_width) for r in max_col_ratio]
            else:
                self.preferred_size = max_col_sizes

    def create_window(self)-> None:
        """This method creates table widget container
        """
        self.selected_line = 1
        table_height = 1
        table_widgets = []
        if self.headers:
            header_row = []
            for i, h in enumerate(self.headers):
                header_row.append(Label(h, style=self.header_style, width=self.preferred_size[i]))
            table_widgets.append(VSplit(header_row, width=D(), padding=self.padding))

        for row in self.data:
            row_widgets = []
            for i, c in enumerate(row):
                if not c:
                    c = '-'
                if len(c) > self.preferred_size[i]:
                    c = textwrap.fill(c, self.preferred_size[i]-1)

                m = c.splitlines()
                row_widgets.append(TextArea(c, width=self.preferred_size[i], read_only=True, height=len(m), style=self.data_style))
                table_height += len(m) +1
            table_widgets.append(VSplit(row_widgets, width=D(), padding=self.padding))

        table_container = HSplit(table_widgets, height=table_height, width=D(), padding=1, style=self.bg_style)

        self.container = VSplit([
                        ScrollablePane(content=table_container, height=self.max_height, display_arrows=True),
                    ])


    def __pt_container__(self) -> DynamicContainer:
        return self.container
