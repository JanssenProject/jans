import json
from functools import partial
from asyncio import Future
from prompt_toolkit.widgets import Button, Dialog
from prompt_toolkit.layout.dimension import D

from multi_lang import _

class JansGDialog:
    """This is the main dialog Class Widget for all Jans-cli-tui dialogs except custom dialogs like dialogs with navbar
    """
    def __init__(self, parent, title, body, buttons=[], width=None):
        """init for JansGDialog

        Args:
            parent (widget): this is the parent widget for the dialog, to caluclate the size
            title (String): the title for the dialog
            body (Widget): The content of the dialog
            buttons (list, optional): Dialog main buttons with their handlers. Defaults to [].
            width (int, optional): If needed custom width. Defaults to None.
        
        Examples:
            dialog = JansGDialog(self, title="Waiting Response", body=body)
        """
        self.future = Future()
        self.body = body
        self.myparent = parent

        if not width:
            width = parent.dialog_width

        if not buttons:
            buttons = [Button(text=_("OK"))]

        def do_handler(button_text, handler):
            if handler:
                handler(self)
            self.future.set_result(button_text)

        for button in buttons:
            button.handler = partial(do_handler, button.text, button.handler)

        self.dialog = Dialog(
            title=title,
            body=body,
            buttons=buttons,
            width=width,
            modal=True,
        )

    def __pt_container__(self):
        return self.dialog
