#!/usr/bin/env python3
"""
"""
import json
import os
from shutil import get_terminal_size
import time
from asyncio import Future, ensure_future

import prompt_toolkit
from prompt_toolkit.application import Application
from prompt_toolkit.application.current import get_app
from prompt_toolkit.key_binding import KeyBindings
from prompt_toolkit.key_binding.bindings.focus import focus_next, focus_previous
from prompt_toolkit.layout.containers import Float, HSplit, VSplit
from prompt_toolkit.layout.containers import (
    ConditionalContainer,
    Float,
    HSplit,
    VSplit,
    VerticalAlign,
    DynamicContainer,
    FloatContainer,
)
from prompt_toolkit.layout.containers import VerticalAlign
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.layout.layout import Layout
from prompt_toolkit.lexers import PygmentsLexer ,DynamicLexer
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
from prompt_toolkit.filters import Condition

# -------------------------------------------------------------------------- #
from cli import config_cli
from wui_components.edit_client_dialog import EditClientDialog
from wui_components.edit_scope_dialog import EditScopeDialog
from wui_components.jans_cli_dialog import JansGDialog
from wui_components.jans_nav_bar import JansNavBar
from wui_components.jans_side_nav_bar import JansSideNavBar
from wui_components.jans_vetrical_nav import JansVerticalNav
from wui_components.jans_dialog import JansDialog
from wui_components.jans_dialog_with_nav import JansDialogWithNav

from cli_style import style

from models.oauth import JansAuthServer
from pathlib import Path

# -------------------------------------------------------------------------- #


help_text_dict = {
    'displayName': ("Name of the user suitable for display to end-users"),
    'clientSecret': ("The client secret. The client MAY omit the parameter if the client secret is an empty string"),
    'redirectUris': ("Redirection URI values used by the Client. One of these registered Redirection URI values must exactly match the redirect_uri parameter value used in each Authorization Request"),
    'responseTypes': ("A list of the OAuth 2.0 response_type values that the Client is declaring that it will restrict itself to using. If omitted, the default is that the Client will use only the code Response Type. Allowed values are code, token, id_token"),
    'applicationType': ("Kind of the application. The default, if omitted, is web. The defined values are native or web. Web Clients using the OAuth Implicit Grant Type must only register URLs using the HTTPS scheme as redirect_uris, they must not use localhost as the hostname. Native Clients must only register redirect_uris using custom URI schemes or URLs using the http scheme with localhost as the hostname"),
    'helper': ("To guide you through the fields"),
}

home_dir = Path.home()
config_dir = home_dir.joinpath('.config')
config_dir.mkdir(parents=True, exist_ok=True)
config_ini_fn = config_dir.joinpath('jans-cli.ini')

def accept_yes():
    get_app().exit(result=True)


def accept_no():
    get_app().exit(result=False)

def do_exit(*c):
    get_app().exit(result=False)


class JansCliApp(Application, JansAuthServer):

    def __init__(self):
        self.app_started = False
        self.width, self.height = get_terminal_size()
        self.app = get_app()
        self.show_dialog = False   ## ## ## ##
        self.set_keybindings()
        self.containers = {}
        # -------------------------------------------------------------------------------- #
        self.dialogs = {}
        self.tabs = {}
        self.active_dialog_select = ''
        self.Auth_clients_tabs = {}

        # ----------------------------------------------------------------------------- #
        self.check_jans_cli_ini()
        # ----------------------------------------------------------------------------- #


        self.yes_button = Button(text="Yes", handler=accept_yes)
        self.no_button = Button(text="No", handler=accept_no)
        self.status_bar = TextArea(style="class:status", height=1, focusable=False)

        self.prapare_dialogs()

        JansAuthServer.initialize(self)


        self.not_implemented = Frame(
                            body=HSplit([Label(text="Not imlemented yet"), Button(text="MyButton")], width=D()),
                            height=D())

        self.center_container = self.not_implemented

        self.nav_bar = JansNavBar(
                    self,
                    entries=[('oauth', 'Auth Server'), ('fido', 'FDIO'), ('scim', 'SCIM'), ('config_api', 'Config-API'), ('client_api', 'Client-API'), ('scripts', 'Scripts')],
                    selection_changed=self.main_nav_selection_changed,
                    select=0,
                    )

        self.center_frame = FloatContainer(content=
                    Frame(
                        body=DynamicContainer(lambda: self.center_container),
                        height=D()
                        ),
                        floats=[],
                )


        self.root_layout = FloatContainer(
                        HSplit([
                                Frame(self.nav_bar.nav_window),
                                self.center_frame,
                                self.status_bar
                                    ],
                                ),
                        floats=[]
                )

        super(JansCliApp, self).__init__(
                layout=Layout(self.root_layout),
                key_bindings=self.bindings, 
                style=style, 
                full_screen=True,
                mouse_support=True, ## added
            )



        self.app_started = True
        self.main_nav_selection_changed(self.nav_bar.navbar_entries[0][0])

        # Since first module is oauth, set center frame to my oauth main container.
        self.oauth_set_center_frame()

    def create_cli(self):
        test_client = config_cli.client_id if config_cli.test_client else None
        self.cli_object = config_cli.JCA_CLI(
                host=config_cli.host, 
                client_id=config_cli.client_id,
                client_secret=config_cli.client_secret, 
                access_token=config_cli.access_token, 
                test_client=test_client
            )

        status = self.cli_object.check_connection()

        if status is not True:
            buttons = [Button("OK", handler=self.jans_creds_dialog)]
            self.show_message("Error getting Connection Config Api", status, buttons=buttons)
        else:
            if not test_client and not self.cli_object.access_token:
                try:
                    response = self.cli_object.get_device_verification_code()
                    result = response.json()

                    msg = "Please visit verification url {} and enter user code {} in {} secods".format(
                        result['verification_uri'], result['user_code'], result['expires_in']
                        )

                    self.show_message("Waiting Response", msg)
                    self.cli_object.get_jwt_access_token(result)

                except Exception as e:
                    self.show_message("ERROR", "An Error ocurred while getting device authorization code: " + str(e))


    def check_jans_cli_ini(self):
        if not(config_cli.host and (config_cli.client_id and config_cli.client_secret or config_cli.access_token)):
            self.jans_creds_dialog()
        else :
            self.create_cli()


    def dialog_back_but(self): ## BACK
        self.active_dialog_select = ''
        self.show_dialog = False
        self.layout.focus(self.center_frame) 


    def prapare_dialogs(self):
        self.data_show_client_dialog = Label(text='Selected Line Data as Json') 
        self.dialog_width = int(self.width*0.9) # 120 ## to be dynamic
        self.dialog_height = int(self.height*0.8) ## to be dynamic


    def focus_next(self, ev):
        focus_next(ev)
        self.update_status_bar()

    def focus_previous(self, ev):
        focus_previous(ev)
        self.update_status_bar()

    def set_keybindings(self):
        # Global key bindings.
        self.bindings = KeyBindings()
        self.bindings.add("tab")(self.focus_next)
        self.bindings.add("s-tab")(self.focus_previous)
        self.bindings.add("c-c")(do_exit)

    def getTitledText(self, title, name, value='', height=1, jans_help='', width=None):
        multiline = height > 1
        ta = TextArea(text=value, multiline=multiline, style='class:textarea')
        ta.window.jans_name = name
        ta.window.jans_help = jans_help
        if width:
            ta.window.width = width
        li = title
        for i in range(height-1):
            li +='\n'

        return VSplit([Label(text=li + ':', width=len(title)+1), ta], height=height, padding=1)

    def getTitledCheckBox(self, title, name, values):
        cb = CheckboxList(values=[(o,o) for o in values],)
        cb.window.jans_name = name
        li = title
        for i in range(len(values)-1):
            li +='\n'
        return VSplit([Label(text=li, width=len(title)+1), cb] )

    def getTitledRadioButton(self, title, name, values):
        rl = RadioList(values=[(option, option) for option in values])
        rl.window.jans_name = name
        li = title
        for i in range(len(values)-1):
            li +='\n'

        return VSplit([Label(text=li, width=len(title)+1), rl],
                height=len(values)
            )

    def getButton(self, text, name, jans_help, handler=None):
        b = Button(text=text, width=len(text)+2)
        b.window.jans_name = name
        b.window.jans_help = jans_help
        if handler:
            b.handler = handler
        return b

    def get_statusbar_text(self):
        wname = getattr(self.layout.current_window, 'jans_name', 'NA')
        return help_text_dict.get(wname, '')

    def update_status_bar(self, text=None):
        if text:
            self.status_bar.text = text
        else:
            if hasattr(self.layout.current_window, 'jans_help') and self.layout.current_window.jans_help:
                text = self.layout.current_window.jans_help
            else:
                wname = getattr(self.layout.current_window, 'jans_name', 'NA')
                text = help_text_dict.get(wname, '')

        self.status_bar.text = text

    def main_nav_selection_changed(self, selection):
        if hasattr(self, selection+'_set_center_frame'):
            center_frame_setter = getattr(self, selection+'_set_center_frame')
            center_frame_setter()
        else:
            self.center_container = self.not_implemented

    async def show_dialog_as_float(self, dialog):
        "Coroutine."
        float_ = Float(content=dialog)
        self.root_layout.floats.insert(0, float_)

        result = await dialog.future

        if float_ in self.root_layout.floats:
            self.root_layout.floats.remove(float_)

        return result

    def show_jans_dialog(self, dialog):
        async def coroutine():
            app = get_app()
            focused_before = app.layout.current_window
            self.layout.focus(dialog)
            result = await self.show_dialog_as_float(dialog)
            try:
                app.layout.focus(focused_before)
            except:
                app.layout.focus(self.center_frame)
            return result
        ensure_future(coroutine())

    def data_display_dialog(self, **params):

        body = HSplit([
                TextArea(
                    lexer=DynamicLexer(lambda: PygmentsLexer.from_filename(".json", sync_from_start=True)),
                    scrollbar=True,
                    line_numbers=True,
                    multiline=True,
                    read_only=True,
                    text=str(json.dumps(params['data'], indent=2)),
                )
            ])

        dialog = JansGDialog(title=params['selected'][0], body=body)

        self.show_jans_dialog(dialog)

    def save_creds(self, dialog):

        for child in dialog.body.children:
            prop_name = child.children[1].jans_name
            prop_val = child.children[1].content.buffer.text
            config_cli.config['DEFAULT'][prop_name] = prop_val
            config_cli.write_config()

        config_cli.host = config_cli.config['DEFAULT']['jans_host']
        config_cli.client_id = config_cli.config['DEFAULT']['jca_client_id']
        config_cli.client_secret = config_cli.config['DEFAULT']['jca_client_secret']

        self.create_cli()


    def jans_creds_dialog(self, *params):

        body=HSplit([
            self.getTitledText("Hostname", name='jans_host', value=config_cli.host or '', jans_help="FQN name of Jannsen Config Api Server"),
            self.getTitledText("Client ID", name='jca_client_id', value=config_cli.client_id or '', jans_help="Jannsen Config Api Client ID"),
            self.getTitledText("Client Secret", name='jca_client_secret', value=config_cli.client_secret or '', jans_help="Jannsen Config Api Client Secret")
            ])

        buttons = [Button("Save", handler=self.save_creds)]
        dialog = JansGDialog(title="Janssen Config Api Client Credidentials", body=body, buttons=buttons)

        self.show_jans_dialog(dialog)
  

    def edit_client_dialog(self, **params):
        dialog = EditClientDialog(self,**params)
        self.show_jans_dialog(dialog)

    def edit_scope_dialog(self, **params):
        dialog = EditScopeDialog(self,**params)
        self.show_jans_dialog(dialog)

    def show_message(self, title, message, buttons=[]):
        body = HSplit([Label(message)])
        dialog = JansGDialog(title=title, body=body, buttons=buttons)
        self.show_jans_dialog(dialog)

  
application = JansCliApp()

def run():
    result = application.run()
    print("You said: %r" % result)


if __name__ == "__main__":
    run()
