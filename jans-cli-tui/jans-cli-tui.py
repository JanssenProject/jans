#!/usr/bin/env python3
"""
"""
import os
import json
import time
import logging
import importlib
import sys

from pathlib import Path
from asyncio import Future, ensure_future
from pynput.keyboard import Key, Controller

import prompt_toolkit
from prompt_toolkit.application import Application
from prompt_toolkit.application.current import get_app
from prompt_toolkit.key_binding import KeyBindings
from prompt_toolkit.key_binding.bindings.focus import focus_next, focus_previous
from prompt_toolkit.layout.containers import Float, HSplit, VSplit
from prompt_toolkit.formatted_text import HTML, merge_formatted_text

from prompt_toolkit.layout.containers import (
    Float,
    HSplit,
    VSplit,
    HorizontalAlign,
    DynamicContainer,
    FloatContainer,
    Window,
    FormattedTextControl
)
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.layout.layout import Layout
from prompt_toolkit.lexers import PygmentsLexer, DynamicLexer
from prompt_toolkit.widgets import (
    Button,
    Frame,
    Label,
    RadioList,
    TextArea,
    CheckboxList,
    Checkbox,
)

# -------------------------------------------------------------------------- #
from cli import config_cli
from wui_components.jans_cli_dialog import JansGDialog
from wui_components.jans_nav_bar import JansNavBar
from wui_components.jans_message_dialog import JansMessageDialog

from cli_style import style

import cli_style

from multi_lang import _
# -------------------------------------------------------------------------- #

home_dir = Path.home()
config_dir = home_dir.joinpath('.config')
config_dir.mkdir(parents=True, exist_ok=True)
config_ini_fn = config_dir.joinpath('jans-cli.ini')
cur_dir = os.path.dirname(os.path.realpath(__file__))

def accept_yes():
    get_app().exit(result=True)


def accept_no():
    get_app().exit(result=False)

def do_exit(*c):
    get_app().exit(result=False)


class JansCliApp(Application):
    
    def __init__(self):
        self.init_logger()
        self.status_bar_text = ''
        self.styles = dict(style.style_rules)
        self._plugins = []
        self._load_plugins()
        self.set_keybindings()
        self.entries_per_page = 20 # we can make this configurable
        # -------------------------------------------------------------------------------- #

        self.not_implemented = Frame(
                            body=HSplit([Label(text=_("Not imlemented yet")), Button(text=_("MyButton"))], width=D()),
                            height=D())


        self.keyboard = Controller()

        self.yes_button = Button(text=_("Yes"), handler=accept_yes)
        self.no_button = Button(text=_("No"), handler=accept_no)
        self.status_bar = Window(
                        FormattedTextControl(self.update_status_bar), style='class:status', height=1
                    )

        self.center_container = self.not_implemented

        self.nav_bar = JansNavBar(
                    self,
                    entries=[(plugin.pid, plugin.name) for plugin in self._plugins], 
                    selection_changed=self.main_nav_selection_changed,
                    select=0,
                    bgcolor=cli_style.main_navbar_bgcolor
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
                                self.status_bar,
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

        self.main_nav_selection_changed(self.nav_bar.navbar_entries[0][0])

        # Since first module is oauth, set center frame to my oauth main container.
        #self.oauth_set_center_frame()


        # ----------------------------------------------------------------------------- #
        self.check_jans_cli_ini()

    def _load_plugins(self):

        plugin_dir = os.path.join(cur_dir, 'plugins')
        for plugin_file in sorted(Path(plugin_dir).glob('*/main.py')):
            sys.path.append(plugin_file.parent.as_posix())
            spec = importlib.util.spec_from_file_location(plugin_file.stem, plugin_file.as_posix())
            plugin = importlib.util.module_from_spec(spec)
            spec.loader.exec_module(plugin)
            self._plugins.append(plugin.Plugin(self))

    @property
    def dialog_width(self):
        return int(self.output.get_size().columns*0.8)

    @property
    def dialog_height(self):
        return int(self.output.get_size().rows*0.9)

    def init_logger(self):
        self.logger = logging.getLogger('JansCli')
        self.logger.setLevel(logging.DEBUG)
        formatter = logging.Formatter('%(asctime)s - %(levelname)s - %(message)s')
        file_handler = logging.FileHandler(os.path.join(cur_dir, 'dev.log'))
        file_handler.setLevel(logging.DEBUG)
        file_handler.setFormatter(formatter)
        self.logger.addHandler(file_handler)
        self.logger.debug('JANS CLI Started')

    def press_tab(self):
        self.keyboard.press(Key.tab)
        self.keyboard.release(Key.tab)

    def create_cli(self):
        conn_ok = False
        test_client = config_cli.client_id if config_cli.test_client else None
        self.cli_object = config_cli.JCA_CLI(
                host=config_cli.host, 
                client_id=config_cli.client_id,
                client_secret=config_cli.client_secret, 
                access_token=config_cli.access_token, 
                test_client=test_client
            )

        status = self.cli_object.check_connection()

        self.logger.info("OpenID Configuration: %s", self.cli_object.openid_configuration)

        self.press_tab()

        if status not in (True, 'ID Token is expired'):
            buttons = [Button(_("OK"), handler=self.jans_creds_dialog)]
            self.show_message(_("Error getting Connection Config Api"), status, buttons=buttons)

        else:
            if not test_client and not self.cli_object.access_token:

                    response = self.cli_object.get_device_verification_code()
                    result = response.json()

                    msg = _("Please visit verification url %s and enter user code %s in %d seconds.")
                    body = HSplit([Label(msg % (result['verification_uri'], result['user_code'], result['expires_in']),style='class:jans-main-verificationuri.text')],style='class:jans-main-verificationuri')
                    dialog = JansGDialog(self, title=_("Waiting Response"), body=body)

                    async def coroutine():
                        app = get_app()
                        focused_before = app.layout.current_window
                        await self.show_dialog_as_float(dialog)
                        try:
                            app.layout.focus(focused_before)
                        except:
                            app.layout.focus(self.center_frame)
                        try:
                            self.cli_object.get_jwt_access_token(result)
                        except Exception as e:
                            err_dialog = JansGDialog(self, title=_("Error!"), body=HSplit([Label(str(e))]))
                            await self.show_dialog_as_float(err_dialog)
                            self.create_cli()

                    ensure_future(coroutine())

    def check_jans_cli_ini(self):
        if not(config_cli.host and (config_cli.client_id and config_cli.client_secret or config_cli.access_token)):
            self.jans_creds_dialog()
        else :
            self.create_cli()

    def jans_creds_dialog(self, *params):

        body=HSplit([
            self.getTitledText(_("Hostname"), name='jans_host', value=config_cli.host or '', jans_help=_("FQN name of Jannsen Config Api Server"),style='class:jans-main-usercredintial.titletext'),
            self.getTitledText(_("Client ID"), name='jca_client_id', value=config_cli.client_id or '', jans_help=_("Jannsen Config Api Client ID"),style='class:jans-main-usercredintial.titletext'),
            self.getTitledText(_("Client Secret"), name='jca_client_secret', value=config_cli.client_secret or '', jans_help=_("Jannsen Config Api Client Secret"),style='class:jans-main-usercredintial.titletext'),
            ],style='class:jans-main-usercredintial')

        buttons = [Button(_("Save"), handler=self.save_creds)]
        dialog = JansGDialog(self, title=_("Janssen Config Api Client Credidentials"), body=body, buttons=buttons)

        async def coroutine():
            app = get_app()
            focused_before = app.layout.current_window
            result = await self.show_dialog_as_float(dialog)
            try:
                app.layout.focus(focused_before)
            except:
                app.layout.focus(self.center_frame)
            
            self.create_cli()

        ensure_future(coroutine())

    def focus_next(self, ev):
        focus_next(ev)

    def focus_previous(self, ev):
        focus_previous(ev)

    def set_keybindings(self):
        # Global key bindings.
        self.bindings = KeyBindings()
        self.bindings.add('tab')(self.focus_next)
        self.bindings.add('s-tab')(self.focus_previous)
        self.bindings.add('c-c')(do_exit)
        self.bindings.add('f1')(self.help)
        self.bindings.add('escape')(self.escape)


    def help(self,ev):
        self.show_message(_("Help"),'''<Enter> {} \n<j> {}\n<d> {}'''.format(_("Edit current selection"),_("Display current item in JSON format"),_("Delete current selection")))

    # def save_dialog(self,ev):
    #     try:
    #         if get_app().layout.container.floats[0]:  ### if there is a dialog
    #             self.logger.debug(self.root_layout.floats)
    #             self.logger.debug(self.root_layout.floats[-1].content)
    #             self.logger.debug(self.root_layout.floats[-1].content.get_children()[0].get_children())
    #             self.logger.debug(self.root_layout.floats[-1].content.get_children()[-1].get_children()[-1].get_children())
        
    #     except Exception as e:
    #         self.logger.debug('ERROR'+str(e))

    def escape(self,ev):
        try:
            if get_app().layout.container.floats[0]:
                if len(get_app().layout.container.floats) >=2 :
                    get_app().layout.container.floats.remove(get_app().layout.container.floats[-1])
                    get_app().layout.focus(get_app().layout.container.floats[-1].content)
                else:
                    get_app().layout.container.floats.remove(get_app().layout.container.floats[0])
                    get_app().layout.focus(self.center_frame)
        except Exception as e:
            pass


    def get_help_from_schema(self, schema, jans_name):
        for prop in schema.get('properties', {}):
            if prop == jans_name:
                return schema['properties'][jans_name].get('description', '')

    def getTitledText(
            self,
            title,
            name,
            value='',
            height=1,
            jans_help='',
            accept_handler=None,
            read_only=False,
            focusable=None,
            width=None,
            style='',
            scrollbar=False,
            line_numbers=False,
            lexer=None
            ):
        title += ': '
        ta = TextArea(
                text=str(value),
                multiline=height > 1,
                height=height,
                width=width,
                read_only=read_only,
                style=self.styles['textarea-readonly'] if read_only else self.styles['textarea'],
                accept_handler=accept_handler,
                focusable=not read_only if focusable is None else focusable,
                scrollbar=scrollbar,
                line_numbers=line_numbers,
                lexer=lexer,
            )
        ta.window.jans_name = name
        ta.window.jans_help = jans_help

        #li, cd, width = self.handle_long_string(title,[1]*num_lines,ta)

        v = VSplit([Label(text=title, width=len(title), style=style), ta], padding=1)
        v.me = ta

        return v
 
    def getTitledCheckBoxList(self, title, name, values, current_values=[], jans_help='', style=''):
        title += ': '
        if values and not (isinstance(values[0], tuple) or isinstance(values[0], list)):
            values = [(o,o) for o in values]
        cbl = CheckboxList(values=values)
        cbl.current_values = current_values
        cbl.window.jans_name = name
        cbl.window.jans_help = jans_help
        #li, cd, width = self.handle_long_string(title, values, cbl)

        v = VSplit([Label(text=title, width=len(title), style=style, wrap_lines=False), cbl])
        v.me = cbl

        return v

    def getTitledCheckBox(self, title, name, text='', checked=False, on_selection_changed=None, jans_help='', style=''):
        title += ': '
        cb = Checkbox(text)
        cb.checked = checked
        cb.window.jans_name = name
        cb.window.jans_help = jans_help

        handler_org = cb._handle_enter
        def custom_handler():
            handler_org()
            on_selection_changed(cb)

        if on_selection_changed:
            cb._handle_enter = custom_handler

        #li, cd, width = self.handle_long_string(title, text, cb)

        v = VSplit([Label(text=title, width=len(title), style=style, wrap_lines=False), cb])
        v.me = cb

        return v

    def getTitledRadioButton(self, title, name, values, current_value=None, on_selection_changed=None, jans_help='', style=''):
        title += ': '
        if values and not (isinstance(values[0], tuple) or isinstance(values[0], list)):
            values = [(o,o) for o in values]
        rl = RadioList(values=values)
        if current_value:
            rl.current_value = current_value
        rl.window.jans_name = name
        rl.window.jans_help = jans_help
        #li, rl2, width = self.handle_long_string(title, values, rl)

        handler_org = rl._handle_enter
        def custom_handler():
            handler_org()
            on_selection_changed(rl)

        if on_selection_changed:
            rl._handle_enter = custom_handler

        v = VSplit([Label(text=title, width=len(title), style=style), rl])
        v.me = rl

        return v

    def getTitledWidget(self, title, name, widget, jans_help='', style=''):
        title += ': '
        widget.window.jans_name = name
        widget.window.jans_help = jans_help
        #li, w2, width = self.handle_long_string(title, widget.values, widget)

        v = VSplit([Label(text=title, width=len(title), style=style), widget])
        v.me = widget

        return v

    def getButton(self, text, name, jans_help, handler=None):
        b = Button(text=text, width=len(text)+2)
        b.window.jans_name = name
        b.window.jans_help = jans_help
        if handler:
            b.handler = handler
        return b

    def update_status_bar(self):
        text = ''
        if self.status_bar_text:
            text = self.status_bar_text
            self.status_bar_text = ''
        else:
            if hasattr(self.layout.current_window, 'jans_help') and self.layout.current_window.jans_help:
                text = self.layout.current_window.jans_help

        return text

    def get_plugin_by_id(self, pid):
        for plugin in self._plugins:
            if plugin.pid == pid:
                return plugin

    def main_nav_selection_changed(self, selection):
        self.logger.debug('Main navbar selection changed %s', str(selection))
        plugin = self.get_plugin_by_id(selection)
        plugin.set_center_frame()

    async def show_dialog_as_float(self, dialog):
        'Coroutine.'
        float_ = Float(content=dialog)
        self.root_layout.floats.append(float_)
        self.layout.focus(dialog)
        result = await dialog.future

        if float_ in self.root_layout.floats:
            self.root_layout.floats.remove(float_)

        if self.root_layout.floats:
            self.layout.focus(self.root_layout.floats[-1].content)
        else:
            self.layout.focus(self.center_frame)

        return result

    def show_jans_dialog(self, dialog):

        async def coroutine():
            focused_before = self.layout.current_window
            result = await self.show_dialog_as_float(dialog)
            try:
                self.layout.focus(focused_before)
            except:
                self.layout.focus(self.center_frame)

            return result

        ensure_future(coroutine())

    def data_display_dialog(self, **params):

        body = HSplit([
                TextArea(
                    lexer=DynamicLexer(lambda: PygmentsLexer.from_filename('.json', sync_from_start=True)),
                    scrollbar=True,
                    line_numbers=True,
                    multiline=True,
                    read_only=True,
                    text=str(json.dumps(params['data'], indent=2)),
                    style='class:jans-main-datadisplay.text'
                )
            ],style='class:jans-main-datadisplay')

        dialog = JansGDialog(self, title=params['selected'][0], body=body)

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

    def show_message(self, title, message, buttons=[],tobefocused=None):
        body = HSplit([Label(message)])
        dialog = JansMessageDialog(title=title, body=body, buttons=buttons)

        if not tobefocused:
            focused_before = self.root_layout.floats[-1].content if self.root_layout.floats else self.layout.current_window #show_message
        else :
            focused_before = self.root_layout.floats[-1].content if self.root_layout.floats else tobefocused 
        float_ = Float(content=dialog)
        self.root_layout.floats.append(float_)
        dialog.me = float_
        dialog.focus_on_exit = focused_before
        self.layout.focus(dialog)
        self.press_tab()

    def show_again(self): ## nasted dialog Button
        self.show_message(_("Again"), _("Nasted Dialogs"),)

    def get_confirm_dialog(self, message):
        body = VSplit([Label(message)], align=HorizontalAlign.CENTER)
        buttons = [Button(_("No")), Button(_("Yes"))]
        dialog = JansGDialog(self, title=_("Confirmation"), body=body, buttons=buttons)
        return dialog




application = JansCliApp()

def run():
    result = application.run()
    print("You said: %r" % result)


if __name__ == "__main__":
    run()
