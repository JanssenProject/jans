from prompt_toolkit.layout.containers import Window
from prompt_toolkit.layout.controls import FormattedTextControl
from prompt_toolkit.formatted_text import HTML, merge_formatted_text
from prompt_toolkit.key_binding import KeyBindings


class JansNavBar():
    def __init__(self, myparent, entries, selection_changed, select=0, bgcolor='#00ff44'):
        self.myparent = myparent
        self.navbar_entries = entries
        self.cur_navbar_selection = select
        self.bgcolor = bgcolor
        self.selection_changed = selection_changed
        self.cur_tab = entries[self.cur_navbar_selection][0]
        self.create_window()


    def create_window(self):
        self.nav_window = Window(
                            content=FormattedTextControl(
                                text=self.get_navbar_entries,
                                focusable=True,
                                key_bindings=self.get_nav_bar_key_bindings(),
                            ),
                            height=1,
                            cursorline=False,
                        )

    def get_navbar_entries(self):

        result = []
        for i, entry in enumerate(self.navbar_entries):
            if i == self.cur_navbar_selection:
                result.append(HTML('<style fg="ansired" bg="{}">{}</style>'.format(self.bgcolor, entry[1])))
            else:
                result.append(HTML('<b>{}</b>'.format(entry[1])))
            result.append("   ")

        return merge_formatted_text(result)


    def get_nav_bar_key_bindings(self):
        kb = KeyBindings()

        @kb.add("left")
        def _go_up(event) -> None:
            self.cur_navbar_selection = (self.cur_navbar_selection - 1) % len(self.navbar_entries)

        @kb.add("right")
        def _go_up(event) -> None:
            self.cur_navbar_selection = (self.cur_navbar_selection + 1) % len(self.navbar_entries)

        return kb
