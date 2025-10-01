from functools import partial
from asyncio import Future
from prompt_toolkit.widgets import Button, Dialog
from typing import Optional, Sequence, Union
from prompt_toolkit.layout.containers import AnyContainer
from prompt_toolkit.layout.dimension import AnyDimension
from prompt_toolkit.formatted_text import AnyFormattedText
from utils.multi_lang import _

class JansGDialog:
    """This is the main dialog Class Widget for all Jans-cli-tui dialogs except custom dialogs like dialogs with navbar
    """
    def __init__(
        self, 
        parent, 
        body: Optional[AnyContainer] = None,
        title: Optional[str] = '',
        buttons: Optional[Sequence[Button]] = None,
        width: AnyDimension = None
        )-> Dialog:
        
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
        self.title = title
        self.buttons = buttons
        self.width = width

        if not self.width:
            self.width = self.myparent.dialog_width

        if not self.buttons:
            self.buttons = [Button(text=_("OK"))]

        def do_handler(button_text, handler, keep_dialog):
            if handler:
                self.current_button_text = button_text
                handler(self)

            if not (keep_dialog or self.future.done()):
                self.future.set_result(button_text)

        for button in self.buttons:
            button.handler = partial(do_handler, button.text, button.handler, getattr(button, 'keep_dialog', False))

        self.dialog = Dialog(
            title=title,
            body=self.body,
            buttons=self.buttons,
            width=self.width,
            modal=True,
            with_background=True
        )

    def close(self):
        self.future.set_result(False)

    def __pt_container__(self)-> Dialog:
        return self.dialog
