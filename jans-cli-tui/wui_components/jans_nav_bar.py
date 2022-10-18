import re
import os 
from prompt_toolkit.layout.containers import Window
from prompt_toolkit.layout.controls import FormattedTextControl
from prompt_toolkit.formatted_text import HTML, merge_formatted_text
from prompt_toolkit.key_binding import KeyBindings
from prompt_toolkit.application.current import get_app
from typing import TypeVar, Callable

import cli_style


shortcut_re = re.compile(r'\[(.*?)\]')

class JansNavBar():
    """This is a horizontal Navigation bar Widget used in Main screen ('clients', 'scopes', 'keys', 'defaults', 'properties', 'logging')
    """
    def __init__(
        self, 
        myparent, 
        entries: list, 
        selection_changed: Callable, 
        select: int= 0, 
        ) -> Window:

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

    def _go_tab(self, ev):

        if get_app().layout.container.floats:
            return

        for i, entry in enumerate(self.navbar_entries):
            re_search = shortcut_re.search(entry[1])
            if re_search and re_search.group(1).lower() == ev.data:
                self.cur_navbar_selection = i
                try: 
                    self.myparent.layout.focus(self.nav_window)
                except:
                    pass
                self._set_selection()
                break

    def add_key_binding(self, shorcut_key):
        r = os.urandom(3).hex()
        for binding in self.myparent.bindings.bindings:
            if len(binding.keys) == 2 and binding.keys[0].value == 'escape' and binding.keys[1].lower() == shorcut_key:
                return
        self.myparent.bindings.add('escape', shorcut_key.lower())(self._go_tab)


    def get_navbar_entries(self):
        """Get all selective entries

        Returns:    
            merge_formatted_text: Merge (Concatenate) several pieces of formatted text together. 
        """
        
        result = []
        for i, entry in enumerate(self.navbar_entries):
            display_text = entry[1]
            re_search = shortcut_re.search(display_text)
            if re_search:
                sc, ec = re_search.span()
                shorcut_key = re_search.group(1)
                display_text = display_text[:sc]+ '<style fg="{}">'.format(cli_style.shorcut_color) + shorcut_key + '</style>' +display_text[ec:]
                self.add_key_binding(shorcut_key.lower())

            if i == self.cur_navbar_selection:
                result.append(HTML('<style fg="{}" bg="{}">{}</style>'.format(cli_style.sub_navbar_selected_bgcolor, cli_style.sub_navbar_selected_fgcolor, display_text)))
            else:
                result.append(HTML('<b>{}</b>'.format(display_text)))
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
