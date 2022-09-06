import json
from asyncio import Future
from typing import OrderedDict

from prompt_toolkit.widgets import Button, TextArea
from prompt_toolkit.application.current import get_app
from prompt_toolkit.layout.dimension import D
from static import DialogResult
from wui_components.jans_dialog import JansDialog
from prompt_toolkit.layout.containers import (
    VSplit,
    DynamicContainer,
)
from prompt_toolkit.widgets import (
    Button,
    Label,
    TextArea,

)

from cli import config_cli
from prompt_toolkit.layout.containers import (
    ConditionalContainer,
    Float,
    HSplit,
    VSplit,
    VerticalAlign,
    DynamicContainer,
    FloatContainer,
    Window
)
from prompt_toolkit.widgets import (
    Box,
    Button,
    Frame,
    Label,
    RadioList,
    TextArea,
    CheckboxList,
    Shadow,
)
from wui_components.jans_dialog_with_nav import JansDialogWithNav
from wui_components.jans_side_nav_bar import JansSideNavBar

from multi_lang import _


class EditScopeDialog:
    def __init__(self,myparent, **params):
        self.myparent = myparent
        self.future = Future()

        def accept_text(buf):
            get_app().layout.focus(ok_button)
            buf.complete_state = None
            return True

        def accept():
            self.future.set_result(DialogResult.ACCEPT)
        
        def cancel():
            self.future.set_result(DialogResult.CANCEL)


        self.side_NavBar = JansSideNavBar(myparent=self.myparent,
            entries=list(self.myparent.oauth_tabs['clients'].keys()),
            selection_changed=(self.myparent.client_dialog_nav_selection_changed) ,
            select=0,  
            entries_color='#2600ff')

        self.dialog = JansDialogWithNav(
            title="Edit Scope Data (Scopes)",
            navbar=DynamicContainer(lambda:self.side_NavBar),
            content=DynamicContainer(lambda: self.myparent.oauth_tabs['clients'][self.myparent.oauth_dialog_nav]), ## can be diffrent
             button_functions=[
                (accept, _("Save")),    ## button name is changed to make sure it is another one
                (cancel, _("Cancel"))
            ],
            height=self.myparent.dialog_height,
            width=self.myparent.dialog_width,
                   )


        ok_button = Button(text=_("OK"), handler=accept)
        cancel_button = Button(text=_("Cancel"), handler=cancel)
        buttons = [cancel_button]
        #if params.get('ok_button'):
        buttons.insert(0, ok_button)


    def __pt_container__(self):
        return self.dialog




