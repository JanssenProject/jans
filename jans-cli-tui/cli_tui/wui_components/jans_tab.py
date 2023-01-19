import re
import os 

from typing import Callable, Optional

from prompt_toolkit.formatted_text import AnyFormattedText
from prompt_toolkit.key_binding.key_bindings import KeyBindings, KeyBindingsBase
from prompt_toolkit.layout.containers import Window, HSplit, VSplit, DynamicContainer, AnyContainer
from prompt_toolkit.layout.controls import FormattedTextControl
from prompt_toolkit.formatted_text import HTML, merge_formatted_text

from cli_style import get_color_for_style

selected_tab_style = get_color_for_style('tab-selected')
unselected_tab_style = get_color_for_style('tab-unselected')

shortcut_re = re.compile(r'\[(.*?)\]')

class JansTab():
    """This is a tab widget used for Jan Client TUI
    """
    def __init__(
            self,
            myparent,
            selection_changed: Optional[Callable]=None,
            ) -> None:

        """init for JansTab

        Args:
            myparent (application): This is the parent application for the dialog, to caluclate the size
            selection_changed (_type_): Callable function when tab selection is changed

        Examples:
            self.user_tab = JansTab(self)
            self.user_tab.add_tab(('Users', HSplit([Label("This is user tab")])))
            self.user_tab.add_tab(('Groups', HSplit([Label("This is group tab")])))
        """
        self.myparent = myparent
        self.selection_changed = selection_changed
        self.cur_tab = 0

        self.tabs = []
        self.tab_container = VSplit([])
        self.create_window()


    def _set_nav_width(self):
        """Sets tab navigation with"""
        nav_bar_width = 1
        for tab in self.tabs:
            nav_bar_width += len(tab[0]) + 3
        self.tab_nav.width = nav_bar_width

    def add_tab(
            self,
            tab_name: str,
            container: AnyContainer
            ) -> None:
        """Adds tab

        Args:
            tab_name (String): name of tab
            container (AnyContainer): container of tab
        """
        self.tabs.append((tab_name, container))
        self._set_nav_width()

        if len(self.tabs) == 1:
            self.tab_container = container

    def remove_tab(self, tab_name: str)->None:
        """Removes tab

        Args:
            tab_name (String): name of tab
        """
        for tab in self.tabs[:]:
            if tab[0] == tab_name:
                self.tabs.remove(tab)
                self.tab_container = self.tabs[0][1] if self.tabs else VSplit([])
                self.cur_tab = 0
                self._set_nav_width()

    def set_tab(self, tab_name:str):
        """Sets current tab

        Args:
            tab_name (String): name of tab
        """
        for i, tab in enumerate(self.tabs):
            if tab[0] == tab_name:
                self.tab_container = tab[1]
                self.cur_tab = i

    def create_window(self)-> None:
        """This method creat the tab widget it self
        """

        self.tab_nav = Window(
                            content=FormattedTextControl(
                                text=self._get_navbar_entries,
                                focusable=True,
                                key_bindings=self.get_nav_bar_key_bindings(),
                            ),
                            style='class:tab-nav-background',
                            height=1,
                            cursorline=False,
                            )

        self.tabbed_container = HSplit([
                            VSplit([self.tab_nav]),
                            DynamicContainer(lambda: self.tab_container)
                        ])


    def add_key_binding(
            self, 
            shorcut_key:str,
            )-> None:
        """Key bindings for tab widget"""

        for binding in self.myparent.bindings.bindings:
            if len(binding.keys) == 2 and binding.keys[0].value == 'escape' and binding.keys[1].lower() == shorcut_key:
                return
        self.myparent.bindings.add('escape', shorcut_key.lower())(self._go_tab)


    def _get_navbar_entries(self)-> AnyFormattedText:
        """Get tab navigation entries

        Returns:
            merge_formatted_text: Merge (Concatenate) several pieces of formatted text together. 
        """

        result = [HTML('<style fg="Blue">|</style>')]

        for i, tab in enumerate(self.tabs):

            if i == self.cur_tab:
                result.append(HTML('<style fg="{}" bg="{}"> {} </style>'.format(selected_tab_style.fg, selected_tab_style.bg, tab[0])))
            else:
                result.append(HTML('<style bg="{}"> {} </style>'.format(unselected_tab_style.bg, tab[0])))
            sep_space = HTML('<style fg="Blue">|</style>')

            result.append(sep_space)

        return merge_formatted_text(result)


    def get_nav_bar_key_bindings(self)-> KeyBindingsBase:
        """All key binding for the Dialog with Navigation bar

        Returns:
            KeyBindings: The method according to the binding key
        """
        kb = KeyBindings()

        @kb.add('left')
        def _go_left(event) -> None:
            if self.cur_tab > 0:
                self.cur_tab -= 1
                self.tab_container = self.tabs[self.cur_tab][1]
                if self.selection_changed:
                    self.selection_changed(self.cur_tab)

        @kb.add('right')
        def _go_right(event) -> None:
            if self.cur_tab < len(self.tabs) - 1:
                self.cur_tab += 1
                self.tab_container = self.tabs[self.cur_tab][1]
                if self.selection_changed:
                    self.selection_changed(self.cur_tab)

        @kb.add('c-left')
        def _go_left(event) -> None:
            if self.cur_tab > 0:
                cur = self.tabs.pop(self.cur_tab)
                self.tabs.insert(self.cur_tab-1, cur)
                self.cur_tab -= 1
                self.tab_container = self.tabs[self.cur_tab][1]

        @kb.add('c-right')
        def _go_right(event) -> None:
            if self.cur_tab < len(self.tabs) - 1:
                cur = self.tabs.pop(self.cur_tab)
                self.tabs.insert(self.cur_tab+1, cur)
                self.cur_tab += 1
                self.tab_container = self.tabs[self.cur_tab][1]


        return kb

    def __pt_container__(self)-> HSplit:
        return self.tabbed_container
