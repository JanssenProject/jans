import re
import os 

from typing import TypeVar, Callable, Optional, Sequence, Union

from prompt_toolkit.key_binding.key_processor import KeyPressEvent
from prompt_toolkit.formatted_text import AnyFormattedText
from prompt_toolkit.key_binding.key_bindings import KeyBindings, KeyBindingsBase
from prompt_toolkit.layout.containers import Window
from prompt_toolkit.layout.controls import FormattedTextControl
from prompt_toolkit.formatted_text import HTML, merge_formatted_text


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
        jans_name: Optional[str] = '',
        last_to_right: Optional[bool] = False,
        ) -> Window:

        """init for JansNavBar

        Args:
            myparent (widget): This is the parent widget for the dialog, to caluclate the size
            entries (_type_): _description_
            selection_changed (_type_): _description_
            select (int, optional): _description_. Defaults to 0.
            bgcolor (str, optional): _description_. Defaults to '#00ff44'.
            last_to_right (bool, optional): move last item to rightmost.
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
        self.jans_name = jans_name
        self.last_to_right = last_to_right
        self.cur_tab = entries[self.cur_navbar_selection][0]
        self.create_window()

    def create_window(self)-> None:
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


    def _set_tab_for_view(self, view, ev):
        for i, entry in enumerate(view.navbar_entries):
            re_search = shortcut_re.search(entry[1])
            if re_search and re_search.group(1).lower() == ev.data:
                view.cur_navbar_selection = i
                try: 
                    self.myparent.layout.focus(view.nav_window)
                except:
                    pass
                view._set_selection()
                return True



    def _go_tab(self, ev)-> None:

        if self.myparent.layout.container.floats:
            return
        # first set main navbar tab
        if not self._set_tab_for_view(self.myparent.nav_bar, ev):
        # then set sub navbar
            cur_plugin = self.myparent.nav_bar.cur_navbar_selection
            try: ## i couldnt access the plugin content from here
                cur_view = self.myparent._plugins[cur_plugin].nav_bar
                self._set_tab_for_view(cur_view, ev)
            except:
                pass

    def add_key_binding(
        self, 
        shorcut_key:str,
        )-> None:
        r = os.urandom(3).hex()
        for binding in self.myparent.bindings.bindings:
            if len(binding.keys) == 2 and binding.keys[0].value == 'escape' and binding.keys[1].lower() == shorcut_key:
                return
        self.myparent.bindings.add('escape', shorcut_key.lower())(self._go_tab)


    def get_navbar_entries(self)-> AnyFormattedText:
        """Get all selective entries

        Returns:
            merge_formatted_text: Merge (Concatenate) several pieces of formatted text together. 
        """

        result = []
        nitems = len(self.navbar_entries)
        total_text_lenght = 0
        
        for i, entry in enumerate(self.navbar_entries):
            display_text = entry[1]
            re_search = shortcut_re.search(display_text)
            if re_search:
                sc, ec = re_search.span()
                shorcut_key = re_search.group(1)
                display_text = display_text[:sc]+ '<style fg="{}">'.format(cli_style.shorcut_color) + shorcut_key + '</style>' +display_text[ec:]
                self.add_key_binding(shorcut_key.lower())

            total_text_lenght += len(entry[1].replace('[','').replace(']',''))
            if i == self.cur_navbar_selection:
                result.append(HTML('<style fg="{}" bg="{}">{}</style>'.format(cli_style.sub_navbar_selected_bgcolor, cli_style.sub_navbar_selected_fgcolor, display_text)))
            else:
                result.append(HTML('<b>{}</b>'.format(display_text)))
            if self.last_to_right and i+2 == nitems:
                screen_width = self.myparent.output.get_size().columns
                remaining_space = (screen_width - total_text_lenght - len(self.navbar_entries[-1][1].replace('[','').replace(']','')) - 2)
                sep_space = ' ' * remaining_space
            else:
                sep_space = '   '
            total_text_lenght += len(sep_space)
            result.append(sep_space)

        return merge_formatted_text(result)


    def _set_selection(self)-> None:

        if self.selection_changed:
            self.selection_changed(self.navbar_entries[self.cur_navbar_selection][0])

    def get_nav_bar_key_bindings(self)-> KeyBindingsBase:
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
