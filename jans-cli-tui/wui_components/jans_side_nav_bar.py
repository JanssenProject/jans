from prompt_toolkit.layout.containers import (
    HSplit,
    Window,
    FloatContainer,
    Dimension
)
from prompt_toolkit.formatted_text import merge_formatted_text
from prompt_toolkit.layout.controls import FormattedTextControl
from prompt_toolkit.key_binding import KeyBindings


class JansSideNavBar():
    def __init__(self, myparent, entries, selection_changed, select=0, entries_color='#00ff44'):
        self.myparent = myparent  # ListBox parent class
        self.navbar_entries = entries  # ListBox entries
        self.cur_navbar_selection = select  # ListBox initial selection
        self.entries_color = entries_color
        self.cur_tab = entries[self.cur_navbar_selection][0]
        self.selection_changed = selection_changed
        self.create_window()

    def create_window(self):
        self.side_Nav = FloatContainer(
            content=HSplit([
                Window(
                    content=FormattedTextControl(
                        text=self.get_navbar_entries,
                        focusable=True,
                        key_bindings=self.get_nav_bar_key_bindings(),
                        style=self.entries_color,

                    ),
                    style="class:select-box",
                    height=Dimension(preferred=len(
                        self.navbar_entries)*2, max=len(self.navbar_entries)*2+1),
                    cursorline=True,
                    width= 10 #self.get_data_width()
                ),
            ]
            ), floats=[]
        )
#--------------------------------------------------------------------------------------#
    def get_data_width(self):
        return len(max(self.navbar_entries, key=len))
#--------------------------------------------------------------------------------------#
    def get_navbar_entries(self):

        result = []
        for i, entry in enumerate(self.navbar_entries):
            if i == self.cur_navbar_selection:
                result.append([("[SetCursorPosition]", "")])
            result.append(entry)
            result.append("\n")
            result.append("\n")
        return merge_formatted_text(result)


    def update_selection(self):
        self.cur_tab = self.navbar_entries[self.cur_navbar_selection]
        self.selection_changed(self.cur_tab)


    def get_nav_bar_key_bindings(self):
        kb = KeyBindings()

        @kb.add("up")
        def _go_up(event) -> None:
            self.cur_navbar_selection = (
                self.cur_navbar_selection - 1) % len(self.navbar_entries)
            self.update_selection()

        @kb.add("down")
        def _go_up(event) -> None:
            self.cur_navbar_selection = (
                self.cur_navbar_selection + 1) % len(self.navbar_entries)
            self.update_selection()

        @kb.add("enter")
        def _(event):
            pass

        return kb

    def __pt_container__(self):
        return self.side_Nav
