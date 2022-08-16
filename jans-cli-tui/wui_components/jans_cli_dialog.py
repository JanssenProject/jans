import json
from asyncio import Future

from prompt_toolkit.widgets import Button, Dialog, TextArea
from prompt_toolkit.layout.containers import HSplit
from prompt_toolkit.application.current import get_app
from prompt_toolkit.layout.dimension import D
from static import DialogResult
from functools import partial
class JansGDialog:
    def __init__(self, parent, title, body, buttons=[]):

        self.result = None
        self.future = Future()
        self.body = body

        def do_handler(button_text, handler):
            if handler:
                handler(self)
            self.future.set_result(button_text)

        def exit_me(button):
            if not getattr(button, 'keep_me', False):
                parent.root_layout.floats.pop()
            if parent.root_layout.floats:
                parent.layout.focus(parent.root_layout.floats[-1].content)
            else:
                parent.layout.focus(parent.center_frame)

        if not buttons:
            buttons = [Button(text="OK")]

        for button in buttons:
            button.handler = partial(do_handler, button, button.handler)


        self.dialog = Dialog(
            title=title,
            body=self.body,
            buttons=buttons,
            width=D(preferred=80),
            modal=True,
        )

    def __pt_container__(self):
        return self.dialog
