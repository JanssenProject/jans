from functools import partial

from prompt_toolkit.widgets import Button, Dialog, Label
from prompt_toolkit.application.current import get_app
from prompt_toolkit.layout.dimension import D


class JansMessageDialog:
    """This is a Dialog to show Message
    """
    def __init__(self, title, body, buttons=[], focus_on_exit=None):
        """init for JansMessageDialog

        Args:
            title (str): The title of the Dialog message
            body (widget): Widget to be displayed with the dialog (Usually Label)
            buttons (list, optional): Dialog main buttons with their handlers. Defaults to [].
            focus_on_exit (widget, optional): Move the focus on exit. Defaults to None.

        Examples:
            buttons = [Button("OK", handler=my_method]
            dialog = JansMessageDialog(title="my title", body=HSplit([Label(message)]), buttons=buttons)
        """
        self.result = None
        self.me = None
        self.focus_on_exit = focus_on_exit
        if not buttons:
            buttons = ["OK"]

        def exit_me(result, handler):
            if handler:
                handler()
            self.result = result
            app = get_app()
            
            if self.me in app.root_layout.floats:
                app.root_layout.floats.remove(self.me)

            try:
                app.layout.focus(self.focus_on_exit)
            except:
                pass

        blist = []

        for button in buttons:
            if isinstance(button, str):
                button = Button(text=button)
            button.handler = partial(exit_me, button.text, button.handler)
            blist.append(button)

        self.dialog = Dialog(
            title=title,
            body=body,
            buttons=blist,
            width=D(preferred=80),
            modal=True,
        )

        app = get_app()
        app.layout.focus(self.dialog)

    def __pt_container__(self):
        return self.dialog
