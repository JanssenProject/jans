from shutil import get_terminal_size


from prompt_toolkit.layout.containers import (
    HSplit,
    VSplit,
    Window,
)
from prompt_toolkit.widgets import (
    Button,
    Dialog,
    VerticalLine,
)
from prompt_toolkit.key_binding import KeyBindings
from prompt_toolkit.layout import ScrollablePane
from prompt_toolkit.application.current import get_app
from typing import Optional, Sequence, Union
from prompt_toolkit.layout.containers import (
    AnyContainer,
)
from prompt_toolkit.key_binding.key_bindings import KeyBindings, KeyBindingsBase
from prompt_toolkit.layout.dimension import AnyDimension
from prompt_toolkit.formatted_text import AnyFormattedText

class JansDialogWithNav():
    """This is a custom dialog Widget with side Navigation Bar (Used for Client/Scope dialogs)
    """
    def __init__(
        self,
        content: AnyContainer,
        width: AnyDimension,
        navbar: AnyContainer=None,
        height: AnyDimension= None,
        title: Optional[str]= '',
        button_functions: Optional[list] = [],
        )-> Dialog:

        """init for JansDialogWithNav

        Args:
            content (OrderedDict): All tabs content orderd in Dict
            height (int, optional): Only if custom hieght is needed. Defaults to None.
            width (int, optional): Only if custom width is needed. Defaults to None.
            title (str, optional): The main title of the dialog. Defaults to None.
            button_functions (list, optional): Dialog main buttons with their handlers. Defaults to [].
            navbar (widget, optional): The Navigation bar widget can be Vertical (Side) Navigation bar or horizontal Navigation bar  . Defaults to None.
        
        Examples:
            self.dialog = JansDialogWithNav(
                title=title,                ## Dialog Title
                navbar=self.side_nav_bar,   ## Nav Bar widget
                content=DynamicContainer(lambda: self.tabs[self.left_nav]),
                button_functions=[
                    (save, "Save"),         ## Button Handler , Button Name
                    (cancel, "Cancel")      ## Button Handler , Button Name
                ],
                height=10,                  ## Fixed Height
                width=30,                   ## Fixed Width
                    ) 
        """
        self.navbar = navbar
        self.button_functions = button_functions
        self.title = title
        self.height = height
        self.width = width
        self.content = content
        self.create_window()

    def create_window(self)-> None:
        """This method creat the dialog it self
        Todo:
            * Change `max_data_str` to be dynamic 
        """
        max_data_str = 22 ## TODO TO BE Dynamic
        wwidth, wheight = get_terminal_size()

        height = 19 if wheight <= 30 else wheight - 11

        self.dialog = Dialog(
            title=self.title,
            body=VSplit([
                VSplit([
                    HSplit([
                        self.navbar 
                        ], width= (max_data_str )),
                    VerticalLine(),

                ]) if self.navbar else VSplit([]),
                    VSplit([
                        ScrollablePane(content=self.content, height=height,display_arrows=False),
                    ],key_bindings=self.get_nav_bar_key_bindings()) 
                ], width=self.width, padding=1),

            buttons=[
                Button(
                    text=str(self.button_functions[k][1]),
                    handler=self.button_functions[k][0],
                ) for k in range(len(self.button_functions))
            ],
            with_background=False,
        )

    def get_nav_bar_key_bindings(self)-> KeyBindingsBase:
        """All key binding for the Dialog with Navigation bar

        Returns:
            KeyBindings: The method according to the binding key
        """
        kb = KeyBindings()


        @kb.add('pageup', eager=True)  ###  eager neglect any other keybinding 
        def _go_pageup(event) -> None:
            if self.navbar : 
                app = get_app()
                self.navbar.go_up()
                app.layout.focus(self.navbar)

        @kb.add('pagedown', eager=True)
        def _go_pagedown(event) -> None:
            if self.navbar:
                app = get_app()
                self.navbar.go_down()
                app.layout.focus(self.navbar)

        @kb.add('f2', eager=True)
        def _go_up(event) -> None:
            if self.button_functions:
                for k in range(len(self.button_functions)):
                    if str(self.button_functions[k][1]).lower() == 'save':
                        self.button_functions[k][0]()

        return kb

    def __pt_container__(self)-> Dialog:
        return self.dialog

