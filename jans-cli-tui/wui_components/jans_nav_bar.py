from prompt_toolkit.layout.containers import Window
from prompt_toolkit.layout.controls import FormattedTextControl
from prompt_toolkit.formatted_text import HTML, merge_formatted_text
from prompt_toolkit.key_binding import KeyBindings


class JansNavBar():
    """This is a horizontal Navigation bar Widget used in Main screen ('clients', 'scopes', 'keys', 'defaults', 'properties', 'logging')
    """
    def __init__(self, myparent, entries, selection_changed, select=0, bgcolor='#00ff44'):
        """init for JansNavBar

        Args:
            myparent (widget): This is the parent widget for the dialog, to caluclate the size
            entries (_type_): _description_
            selection_changed (_type_): _description_
            select (int, optional): _description_. Defaults to 0.
            bgcolor (str, optional): _description_. Defaults to '#00ff44'.
        
        Examples:
            self.oauth_navbar = JansNavBar(
                                self,
                                entries=[('clients', 'Clients'), ('scopes', 'Scopes'), ('keys', 'Keys'), ('defaults', 'Defaults'), ('properties', 'Properties'), ('logging', 'Logging')],
                                selection_changed=self.oauth_nav_selection_changed,
                                select=0,
                                bgcolor='#66d9ef'
                                )  
        """
        self.myparent = myparent
        self.navbar_entries = entries
        self.cur_navbar_selection = select
        self.bgcolor = bgcolor
        self.selection_changed = selection_changed
        self.cur_tab = entries[self.cur_navbar_selection][0]
        self.create_window()

    def create_window(self):
        """This method creat the Navigation Bar it self
        """
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
        """Get all selective entries

        Returns:
            merge_formatted_text: Merge (Concatenate) several pieces of formatted text together. 
        """

        result = []
        for i, entry in enumerate(self.navbar_entries):
            if i == self.cur_navbar_selection:
                result.append(HTML('<style fg="ansired" bg="{}">{}</style>'.format(self.bgcolor, entry[1])))
            else:
                result.append(HTML('<b>{}</b>'.format(entry[1])))
            result.append("   ")

        return merge_formatted_text(result)


    def _set_selection(self):

        if self.selection_changed:
            self.selection_changed(self.navbar_entries[self.cur_navbar_selection][0])

    def get_nav_bar_key_bindings(self):
        """All key binding for the Dialog with Navigation bar

        Returns:
            KeyBindings: The method according to the binding key
        """
        kb = KeyBindings()

        @kb.add('left')
        def _go_up(event) -> None:
            self.cur_navbar_selection = (self.cur_navbar_selection - 1) % len(self.navbar_entries)
            self._set_selection()

        @kb.add('right')
        def _go_up(event) -> None:
            self.cur_navbar_selection = (self.cur_navbar_selection + 1) % len(self.navbar_entries)
            self._set_selection()



        return kb
