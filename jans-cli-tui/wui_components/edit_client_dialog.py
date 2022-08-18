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
from wui_components.jans_cli_dialog import JansGDialog
import yaml


class EditClientDialog(JansGDialog):
    def __init__(self, parent, title, data, buttons=[], save_handler=None):
        super().__init__(parent, title, buttons)
        self.save_handler = save_handler
        self.data = data
        self.prepare_tabs()

        def save():
            close_me = True
            if save_handler:
                close_me = self.save_handler(self)
            if close_me:
                self.future.set_result(DialogResult.ACCEPT)

        def cancel():
            self.future.set_result(DialogResult.CANCEL)

        self.side_nav_bar = JansSideNavBar(myparent=self.myparent,
            entries=list(self.tabs.keys()),
            selection_changed=(self.client_dialog_nav_selection_changed) ,
            select=0,
            entries_color='#2600ff')

        self.dialog = JansDialogWithNav(
            title=title,
            navbar=DynamicContainer(lambda:self.side_nav_bar),
            content=DynamicContainer(lambda: self.tabs[self.left_nav]),
             button_functions=[
                (save, "Save"),
                (cancel, "Cancel")
            ],
            height=self.myparent.dialog_height,
            width=self.myparent.dialog_width,
                   )

    def prepare_tabs(self):

        self.tabs = OrderedDict()

        self.tabs['Basic'] = HSplit([
                        self.myparent.getTitledText(title ="Client_ID", name='inum', value=self.data.get('inum',''), style='green'),
                        self.myparent.getTitledCheckBox("Active", name='disabled', checked= not self.data.get('disabled'), style='green'),
                        self.myparent.getTitledText("Client Name", name='displayName', value=self.data.get('displayName',''), style='green'),
                        self.myparent.getTitledText("Client Secret", name='clientSecret', value=self.data.get('clientSecret',''), style='green'),
                        self.myparent.getTitledText("Description", name='description', value=self.data.get('description',''), style='green'),
                        Label(text="dropdown1",style='blue'),
                        self.myparent.getTitledRadioButton("Subject Type", name='subjectType', values=[('public', 'Public'),('pairwise', 'Pairwise')], current_value=self.data.get('subjectType'), style='green'),
                        self.myparent.getTitledCheckBoxList("Grant", name='grantTypes', values=[('authorization_code', 'Authorizatuin Code'), ('refresh_token', 'Refresh Token'), ('urn:ietf:params:oauth:grant-type:uma-ticket', 'UMA Ticket'), ('client_credentials', 'Client Credentials'), ('password', 'Password'), ('implicit', 'Implicit')], current_values=self.data.get('grantTypes', []), style='green'),
                        self.myparent.getTitledCheckBoxList("Response Types", name='responseTypes', values=['code', 'token', 'id_token'], current_values=self.data.get('responseTypes', []), style='green'),
                        self.myparent.getTitledCheckBox("Supress Authorization", name='dynamicRegistrationPersistClientAuthorizations', checked=self.data.get('dynamicRegistrationPersistClientAuthorizations'), style='green'),
                        self.myparent.getTitledRadioButton("Application Type", name='applicationType', values=['native','web'], current_value=self.data.get('applicationType'), style='green'),
                        self.myparent.getTitledText("Redirect Uris", name='redirectUris', value='\n'.join(self.data.get('redirectUris', [])), height=3, style='green'),
                        #self.myparent.getTitledText("Redirect Regex", name='redirectregex', value=self.data.get('redirectUris', ''), style='green'), #name not identified
                        self.myparent.getTitledText("Scopes", name='scopes', value='\n'.join(self.data.get('scopes', [])), height=3, style='green'),

                        ],width=D(),
                    )


        self.left_nav = list(self.tabs.keys())[0]


    def client_dialog_nav_selection_changed(self, selection):
        self.left_nav = selection

    def __pt_container__(self):
        return self.dialog




