from enum import Enum
from pathlib import Path
from typing import Callable, Optional
from prompt_toolkit.application import get_app
from functools import partial
from prompt_toolkit.formatted_text import HTML, AnyFormattedText, merge_formatted_text,to_formatted_text
from prompt_toolkit.key_binding import KeyBindings
from prompt_toolkit.layout import FormattedTextControl, Window
from prompt_toolkit.widgets import Label, Frame, Box, Button, TextArea
from prompt_toolkit.layout.containers import HSplit
from prompt_toolkit.layout.containers import HSplit, VSplit, DynamicContainer, ScrollOffsets
from prompt_toolkit.layout.margins import ScrollbarMargin
from prompt_toolkit.layout.dimension import D
from utils.multi_lang import _
from wui_components.jans_cli_dialog import JansGDialog


class BrowseType(Enum):
    directory = 0
    file = 1
    save_as = 2


class JansPathBrowserWidget:

    def __init__(self, path, browse_type, height=10):

        self.path = Path(path)
        self.entries = []
        self.browse_type = browse_type
        self.height = height

        self.selected_line = 0
        browsed_dir = Window(FormattedTextControl(lambda: self.path.as_posix()))
        path_selection_window = Window(
                            content=FormattedTextControl(
                            text=self._get_formatted_text,
                            focusable=True,
                            key_bindings=self._get_key_bindings(),
                            ),
                            scroll_offsets=ScrollOffsets(top=2, bottom=2),
                            right_margins=[ScrollbarMargin(display_arrows=True),],
                            cursorline=False,
                            height=self.height
                        )
        container_content = [VSplit([Label(_("Directory:"), width=11), browsed_dir]), path_selection_window]
        focusable = False if browse_type == BrowseType.file else True
        self.file_name = TextArea(multiline=False, focusable=focusable)

        if browse_type == BrowseType.file:
            self.file_name.read_only = True

        if browse_type in (BrowseType.save_as, BrowseType.file):
            container_content.append(VSplit([Label("File name:", width=11), self.file_name]))

        self.container = HSplit(container_content)


    def path_as_str(self, path, i):
        if path.is_dir():
            s = path.name
            if not i and self.path.as_posix() != '/':
                s = '..'
            return chr(128448) +' ' + s
        return chr(128441) + ' ' + path.name


    def _get_formatted_text(self):
        self.get_path_content()
        result = []

        for i, path in enumerate(self.entries):
            if i == self.selected_line:
                result.append([("[SetCursorPosition]", "")])
                if path.is_dir():
                    result.append(HTML('<b><style fg="ansired" bg="{}">{}</style></b>'.format('#ADD8E6', self.path_as_str(path, i))))
                else:
                    result.append(HTML('<style fg="ansired" bg="{}">{}</style>'.format('#ADD8E6', self.path_as_str(path, i))))
            else:
                result.append(HTML('{}'.format(self.path_as_str(path, i))))
            result.append("\n")

        return merge_formatted_text(result)

    def get_path_content(self):
        self.entries = []
        files = []
        dirs = []

        for path_ in self.path.glob('*'):
            if path_.is_dir():
                dirs.append(path_)
            elif self.browse_type != BrowseType.directory:
                files.append(path_)

        self.entries = sorted(dirs) + sorted(files)

        if self.path.as_posix() != '/':
            self.entries.insert(0, self.path.parent)


    def _set_file_name(self):
        if self.browse_type in (BrowseType.save_as, BrowseType.file):
            self.file_name.text = self.entries[self.selected_line].name if self.entries[self.selected_line].is_file() else ''

    def _get_key_bindings(self):
        kb = KeyBindings()

        @kb.add("up")
        def _go_up(event) -> None:
            if self.selected_line < len(self.entries) and self.selected_line > 0:
                self.selected_line = self.selected_line - 1
                self._set_file_name()

        @kb.add("down")
        def _go_down(event) -> None:
            if self.selected_line < len(self.entries) - 1:
                self.selected_line = self.selected_line + 1
                self._set_file_name()

        @kb.add("enter")
        def _enter(event) -> None:
            selected_path = self.entries[self.selected_line]
            if selected_path.is_dir():
                self.selected_line = 0
                self.path = selected_path

        @kb.add("pageup")
        def _pageup(event) -> None:
            for i in range(self.height - 1):
                _go_up(event)

        @kb.add("pagedown")
        def _pagedown(event) -> None:
            for i in range(self.height - 1):
                _go_down(event)

        return kb

    def __pt_container__(self):
        return self.container


def JansFileBrowserDialog(app, path='/', browse_type=BrowseType.save_as, ok_handler=None):
    browse_widget = JansPathBrowserWidget(path, browse_type)


    def call_ok_handler(dialog):
        dialog.future.set_result(True)
        if ok_handler:
            if browse_type in (BrowseType.file, BrowseType.save_as):
                ok_handler(browse_widget.path.joinpath(browse_widget.file_name.text).as_posix())
            else:
                ok_handler(browse_widget.path.as_posix())

    def confirm_handler(dialog):
        call_ok_handler(dialog.parent_dialog)

    def my_ok_handler(dialog):
        if browse_type == BrowseType.save_as:
            if not browse_widget.file_name.text:
                return
            if browse_widget.path.joinpath(browse_widget.file_name.text).exists():
                confirm_dialog = app.get_confirm_dialog(HTML(_("A file named <b>{}</b> already exists. Do you want to replace it?")).format(browse_widget.file_name.text), confirm_handler=confirm_handler)
                confirm_dialog.parent_dialog = dialog
                app.show_jans_dialog(confirm_dialog)
                return
            call_ok_handler(dialog)
        elif browse_type == BrowseType.file:
            if not browse_widget.file_name.text:
                return
            call_ok_handler(dialog)
        elif BrowseType.directory:
            call_ok_handler(dialog)

    if browse_type == BrowseType.directory:
        title = _("Select Directory")
    elif browse_type == BrowseType.file:
        title = _("Select File")
    elif browse_type == BrowseType.save_as:
        title = _("Save As")

    ok_button = Button(_("OK"), handler=my_ok_handler)
    ok_button.keep_dialog =True
    cancel_button = Button(_("Cancel"))
    dialog = JansGDialog(app, title=title, body=browse_widget, buttons=[ok_button, cancel_button])

    return dialog
