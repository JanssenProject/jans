import json
from asyncio import Future

from prompt_toolkit.widgets import Button, Dialog, TextArea
from prompt_toolkit.layout.containers import HSplit
from prompt_toolkit.application.current import get_app
from prompt_toolkit.layout.dimension import D
from static import DialogResult
from functools import partial
class JansGDialog:
    def __init__(self, title, body, buttons=[]):
        self.future = Future()
        self.body = body

        if not buttons:
            buttons = [Button(text="OK")]

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
            width=D(preferred=80),
            modal=True,
        )

    def __pt_container__(self):
        return self.dialog
