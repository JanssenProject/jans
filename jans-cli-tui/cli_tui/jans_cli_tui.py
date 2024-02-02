#!/usr/bin/env python3
"""
"""
import os
import json
import time
import logging
import importlib
import sys
import asyncio
import concurrent.futures

from enum import Enum
from functools import partial
from pathlib import Path
from itertools import cycle
from requests.models import Response
from logging.handlers import RotatingFileHandler

cur_dir = os.path.dirname(os.path.realpath(__file__))
sys.path.append(cur_dir)

pylib_dir = os.path.join(cur_dir, 'cli', 'pylib')
if os.path.exists(pylib_dir):
    sys.path.insert(0, pylib_dir)

no_tui = False
if '--no-tui' in sys.argv:
    sys.argv.remove('--no-tui')
    no_tui = True

from cli import config_cli

if no_tui:
    config_cli.main()
    sys.exit()

import prompt_toolkit
from asyncio import get_event_loop
from prompt_toolkit.application import Application, get_app_session
from prompt_toolkit.application.current import get_app
from prompt_toolkit.key_binding import KeyBindings
from prompt_toolkit.key_binding.bindings.focus import focus_next, focus_previous
from prompt_toolkit.layout.containers import Float, HSplit, VSplit
from prompt_toolkit.formatted_text import HTML, merge_formatted_text
from prompt_toolkit.patch_stdout import patch_stdout

from prompt_toolkit.layout.containers import (
    Float,
    HSplit,
    VSplit,
    HorizontalAlign,
    DynamicContainer,
    FloatContainer,
    Window,
    FormattedTextControl,
    AnyContainer
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
from collections import OrderedDict
from typing import Any, Optional, Sequence, Union
from prompt_toolkit.key_binding.key_processor import KeyPressEvent
from prompt_toolkit.layout.dimension import AnyDimension
from prompt_toolkit.formatted_text import AnyFormattedText
from typing import TypeVar, Callable
from prompt_toolkit.widgets import Button, Dialog, Label
from prompt_toolkit.mouse_events import MouseEvent, MouseEventType
from prompt_toolkit.keys import Keys

from cli_style import style
from utils.multi_lang import _
from utils.static import cli_style, common_strings
from utils.utils import common_data
from utils.validators import IntegerValidator
from utils import background_tasks
from wui_components.jans_cli_dialog import JansGDialog
from wui_components.jans_nav_bar import JansNavBar
from wui_components.jans_message_dialog import JansMessageDialog
from wui_components.jans_path_browser import jans_file_browser_dialog, BrowseType
home_dir = Path.home()
config_dir = home_dir.joinpath('.config')
config_dir.mkdir(parents=True, exist_ok=True)
config_ini_fn = config_dir.joinpath('jans-cli.ini')

def accept_yes() -> None:
    get_app().exit(result=True)

def accept_no() -> None:
    get_app().exit(result=False)

def do_exit(*c) -> None:
    get_app().exit(result=False)

class JansCliApp(Application):

    entries_per_page = 20 # we can make this configurable

    def __init__(self):
        common_data.app = self
        self.executor = concurrent.futures.ThreadPoolExecutor(max_workers=5)
        self.set_keybindings()
        self.init_logger()
        self.disabled_plugins = []
        self.status_bar_text = ''
        self.progress_char = ' '
        self.progress_iterator = cycle(['⣾', '⣷', '⣯', '⣟', '⡿', '⢿', '⣻', '⣽'])
        self.styles = dict(style.style_rules)
        self._plugins = []
        self._load_plugins()
        self.available_plugins = []
        self.cli_object_ok = False
        self.pbar_text = ""
        self.progressing_text = ""
        self.mouse_float=True
        self.browse_path = '/'
        self.app_configuration = {}
        self.current_page = None
        self.jans_help = ("<Enter>          {} \n"
                "<Esc>            {}\n"
                "<Alt + letter>   {}\n"
                "<d>              {}\n"
                "<Delete>         {}\n"
                "For More Visit  {}").format(
                    _("Confirm or Edit current selection"),
                    _("Close the current dialog"),
                    _("Navigate to an other tab"),
                    _("Display current item in JSON format if possible"),
                    _("Delete current selection if possible"),
                    "https://docs.jans.io/v1.0.6/admin/config-guide/tui/")

        self.not_implemented = Frame(
                            body=HSplit([Label(text=_("Not imlemented yet")), Button(text=_("MyButton"))], width=D()),
                            height=D())

        self.yes_button = Button(text=_("Yes"), handler=accept_yes)
        self.no_button = Button(text=_("No"), handler=accept_no)
        self.pbar_window = Window(char=lambda: self.progress_char, style='class:progress', width=1)
        self.status_bar = VSplit([
                                Window(FormattedTextControl(lambda: self.pbar_text), style='class:status', height=1),
                                Window(FormattedTextControl(self.update_status_bar), style='class:status', width=1),
                                self.pbar_window,
                                ], height=1
                                )

        self.center_container = self.not_implemented

        self.nav_bar = JansNavBar(
                    self,
                    entries=[(plugin.pid, plugin.name) for plugin in self._plugins],
                    selection_changed=self.main_nav_selection_changed,
                    select=0,
                    jans_name='main:nav_bar',
                    last_to_right=True,
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
        self.plugins_initialised = False


    async def progress_coroutine(self) -> None:
        """asyncio corotune for progress bar
        """
        self.progress_active = True
        while self.progress_active:
            self.progress_char = next(self.progress_iterator)
            self.pbar_text="Progressing"
            self.invalidate()
            await asyncio.sleep(0.15)
        self.progress_char = ' '
        self.invalidate()

    def cli_requests(self, args: dict) -> Response:
        cli_object = args['cli_object'] if 'cli_object' in args else self.cli_object
        response = cli_object.process_command_by_id(
                        operation_id=args['operation_id'],
                        url_suffix=args.get('url_suffix', ''),
                        endpoint_args=args.get('endpoint_args', ''),
                        data_fn=args.get('data_fn'),
                        data=args.get('data', {})
                        )
        return response


    def retreive_openid_configuration(
        self,
        call_after: Optional[Callable] = None
        ) -> None:
        """Retreives OpenID configuration via CLI Object"""

        self.logger.debug('Retreiving OpenID configuration')

        async def coroutine():
            self.start_progressing(_("Retireiving OpenID configuration..."))
            await get_event_loop().run_in_executor(self.executor, self.cli_object.get_openid_configuration)
            self.stop_progressing()
            self.logger.debug('OpenID Configuration: {}'.format(self.cli_object.openid_configuration))
            if call_after:
                call_after()

        asyncio.ensure_future(coroutine())


    def start_progressing(self, message: Optional[str]="Progressing") -> None:
        self.progressing_text = message
        self.create_background_task(self.progress_coroutine())

    def stop_progressing(self, message: Optional[str]="") -> None:
        self.progressing_text = message
        self.progress_active = False

    def _load_plugins(self) -> None:
        # check if admin-ui plugin is available:

        plugin_dir = os.path.join(cur_dir, 'plugins')
        for plugin_file in sorted(Path(plugin_dir).glob('*/main.py')):
            if plugin_file.parent.joinpath('.enabled').exists():
                sys.path.append(plugin_file.parent.as_posix())
                spec = importlib.util.spec_from_file_location(plugin_file.stem, plugin_file.as_posix())
                plugin = importlib.util.module_from_spec(spec)
                spec.loader.exec_module(plugin)
                plugin_object = plugin.Plugin(self)
                self._plugins.append(plugin_object)

    def init_plugins(self) -> None:
        """Initilizse plugins
        """
        for plugin in self._plugins:
            if hasattr(plugin, 'init_plugin'):
                if getattr(plugin, 'server_side_plugin', False) and plugin.pid not in self.available_plugins:
                    continue
                self.logger.debug('Initializing plugin {}'.format(plugin.pid))
                plugin.init_plugin()

        self.plugins_initialised = True

    def plugin_enabled(self, pid: str) -> bool:
        """Checks whether plugin is enabled or not
        Args:
            pid (str): PID of plugin
        """
        for plugin_object in self._plugins:
            if plugin_object.pid == pid:
                return True
        return False


    def remove_plugin(self, pid: str) -> None:
        """Removes plugin object
        Args:
            pid (str): PID of plugin
        """
        for plugin_object in self._plugins:
            if plugin_object.pid == pid:
                self._plugins.remove(plugin_object)
                return

    @property
    def dialog_width(self) -> int:
        return int(self.output.get_size().columns*0.8)

    @property
    def dialog_height(self) -> int:
        return int(self.output.get_size().rows*0.9)

    def get_column_sizes(self, *args: tuple) -> list:
        col_size_list = []
        w = get_app_session().output.get_size().columns - 3
        for col_ratio in args:
            col_size_list.append(int(w*col_ratio))

        return col_size_list

    def init_logger(self) -> None:
        self.logger = logging.getLogger('JansCli')
        self.logger.setLevel(logging.DEBUG)
        if not os.path.exists(config_cli.log_dir):
            os.makedirs(config_cli.log_dir, exist_ok=True)
        formatter = logging.Formatter('%(asctime)s - %(levelname)s - %(message)s')
        file_handler = RotatingFileHandler(os.path.join(config_cli.log_dir, 'dev-tui.log'), maxBytes=10*1024*1024, backupCount=10)

        file_handler.setLevel(logging.DEBUG)
        file_handler.setFormatter(formatter)
        self.logger.addHandler(file_handler)
        self.logger.debug('JANS CLI Started')


    def create_cli(self) -> None:
        test_client = config_cli.client_id if config_cli.test_client else None
        self.cli_object = config_cli.JCA_CLI(
                host=config_cli.host,
                client_id=config_cli.client_id,
                client_secret=config_cli.client_secret,
                access_token=config_cli.access_token,
                test_client=test_client
            )

        print(_("Checking health of Jans Config Api Server"))
        response = self.cli_requests({'operation_id': 'get-config-health'})

        if response.status_code != 200:
            print(_("Jans Config Api Server is not running propery"))
            print(response.text)
            sys.exit()
        else:
            result = response.json()
            for healt_status in result:
                if healt_status['status'] != 'UP':
                    print(_("Jans Config Api Server is not running propery"))
                    print(healt_status)
                    sys.exit()

        print(_("Health of Jans Config Api Server seems good"))

        status = self.cli_object.check_connection()

        self.invalidate()

        if status not in (True, 'ID Token is expired'):
            buttons = [Button(_("OK"), handler=self.jans_creds_dialog)]
            self.show_message(_("Error connecting to Auth Server"), status, buttons=buttons)

        else:
            if not test_client and not self.cli_object.access_token:

                result = {}
                try:
                    response = self.cli_object.get_device_verification_code()
                    result = response.json()
                except Exception as e:
                    self.cli_object_ok = False
                    self.show_message(
                            _(common_strings.error), 
                            _("Can't get device verification code: \n{}").format(str(e)), 
                            buttons=[Button(_("OK"), handler=self.jans_creds_dialog)],
                            tobefocused=self.center_container)
                    return


                if not 'verification_uri_complete' in result:
                    self.cli_object_ok = False
                    self.show_message(
                            _(common_strings.error),
                            _("Can't find verification code in server response: \n{}").format(response.text),
                            buttons=[Button(_("OK"), handler=self.jans_creds_dialog)],
                            tobefocused=self.center_container)
                    return

                msg = _("Please visit verification url {} and authorize this device within {} seconds.")
                body = HSplit([Label(msg.format(result['verification_uri_complete'], result['expires_in']), style='class:jans-main-verificationuri.text')], style='class:jans-main-verificationuri')
                dialog = JansGDialog(self, title=_("Waiting Response"), body=body)

                async def coroutine():
                    app = get_app()
                    focused_before = app.layout.current_window
                    await self.show_dialog_as_float(dialog)
                    try:
                        app.layout.focus(focused_before)
                    except Exception:
                        app.layout.focus(self.center_frame)

                    self.start_progressing()
                    try:
                        response = await self.loop.run_in_executor(self.executor, self.cli_object.get_jwt_access_token, result)
                    except Exception as e:
                        self.stop_progressing()
                        err_dialog = JansGDialog(self, title=_("Error!"), body=HSplit([Label(str(e))]))
                        await self.show_dialog_as_float(err_dialog)
                        self.cli_object_ok = False
                        self.create_cli()
                        return

                    self.stop_progressing()

                    self.cli_object_ok = True
                    self.check_available_plugins()

                asyncio.ensure_future(coroutine())

            else:
                self.cli_object_ok = True
                self.check_available_plugins()


    def check_available_plugins(self) -> None:
        """Disables plugins when cli object is ready"""

        if self.cli_object_ok:
            self.create_background_task(background_tasks.get_attributes_coroutine(self))
            response = self.cli_requests({'operation_id': 'get-plugins'})
            if response.ok:
                plugins = response.json()
                for plugin in plugins:
                    self.available_plugins.append(plugin['name'])

                for pp in self._plugins:
                    if getattr(pp, 'server_side_plugin', False) and pp.pid not in self.available_plugins:
                        self.disable_plugin(pp.pid)
                    if hasattr(pp, 'on_cli_object_ready'):
                        pp.on_cli_object_ready()

                self.init_plugins()

    def disable_plugin(self, pid) -> None:

        for entry in self.nav_bar.navbar_entries:
            if entry[0] == pid:
                self.nav_bar.navbar_entries.remove(entry)
                self.remove_plugin(entry[0])
                self.invalidate()
                break


    async def check_jans_cli_ini(self) -> None:
        if not(config_cli.host and (config_cli.client_id and config_cli.client_secret or config_cli.access_token)):
            self.jans_creds_dialog()
        else :
            self.create_cli()


    def jans_creds_dialog(self, *params: Any) -> None:
        body=HSplit([
                self.getTitledText(_("OP Hostname"), name='jans_host', value=config_cli.host or '', jans_help=_("FQN name of Jannsen Config Api Server"), style=cli_style.edit_text_required),
                self.getTitledText(_("Client ID"), name='jca_client_id', value=config_cli.client_id or '', jans_help=_("Jannsen Config Api Client ID"), style=cli_style.edit_text_required),
                self.getTitledText(_("Client Secret"), name='jca_client_secret', value=config_cli.client_secret or '', password=True, jans_help=_("Jannsen Config Api Client Secret"), style=cli_style.edit_text_required),
                self.getTitledText(_("Logging Directory"), name='log_dir', value=config_cli.log_dir or '', jans_help=_("Logging Directory"), style=cli_style.edit_text),
                ],
                style='class:jans-main-usercredintial'
                )

        buttons = [Button(_("Save"), handler=self.save_creds)]
        dialog = JansGDialog(self, title=_("Janssen Config Api Client Credentials"), body=body, buttons=buttons)
        async def coroutine():
            app = get_app()
            focused_before = app.layout.current_window
            result = await self.show_dialog_as_float(dialog)
            try:
                app.layout.focus(focused_before)
            except Exception:
                app.layout.focus(self.center_frame)

            self.create_cli()

        asyncio.ensure_future(coroutine())

    def set_keybindings(self) -> None:
        # Global key bindings.
        self.bindings = KeyBindings()
        self.bindings.add('tab')(self.focus_next)
        self.bindings.add('s-tab')(self.focus_previous)
        self.bindings.add('c-c')(do_exit)
        self.bindings.add('c-q')(do_exit)
        self.bindings.add('f1')(self.help)
        self.bindings.add('f4')(self.escape)
        self.bindings.add('escape')(self.escape)
        self.bindings.add('s-up')(self.up)
        self.bindings.add(Keys.Vt100MouseEvent)(self.mouse)


    def mouse(self, event):  ### mouse: [<35;108;20M

        pieces = event.data.split(";")  ##['LEFT', 'MOUSE_DOWN', '146', '10']
        mouse_click=int(pieces[0][3:])
        mouse_state=str(pieces[2][-1:])
        x = int(pieces[1])
        y = int(pieces[2][:-1])

        mouse_event, x, y = map(int, [mouse_click,x,y])
        m = mouse_state

        mouse_event = {
            (0, 'M'): MouseEventType.MOUSE_DOWN,
            (0, 'm'): MouseEventType.MOUSE_UP,
            (2, 'M'): MouseEventType.MOUSE_DOWN,
            (2, 'm'): MouseEventType.MOUSE_UP,
            (64, 'M'): MouseEventType.SCROLL_UP,
            (65, 'M'): MouseEventType.SCROLL_DOWN,
        }.get((mouse_event, m))

        mouse_click = {
            0: "LEFT",
            2: "RIGHT"
        }.get(mouse_click)


        # ------------------------------------------------------------------------------------ #
        # ------------------------------------------------------------------------------------ #
        # ------------------------------------------------------------------------------------ #
        style_tmp = '<style >{}</style>'
        style_tmp_red = '<style fg="ansired" bg="#00FF00">{}</style>'

        class mouse_operations(Enum):
            Copy = 1
            Cut = 2
            Paste = 3

        res=[]
        for mouse_op in mouse_operations:
            res.append(HTML(style_tmp.format(mouse_op.name)))
            res.append("\n")

        content = Window(
            content=FormattedTextControl(
                text=merge_formatted_text(res),
                focusable=True,
            ), height=D())
        mouse_float_container = Float(content=content, left=x,top=y)
        mouse_float_container.name = 'mouse'

        # ------------------------------------------------------------------------------------ #
        # ------------------------------------------------------------------------------------ #
        # ------------------------------------------------------------------------------------ #

        if mouse_click == "RIGHT" and mouse_event == MouseEventType.MOUSE_DOWN :
            if self.mouse_float == True :
                self.root_layout.floats.append(mouse_float_container)
                self.mouse_cord=(x,y)
                self.mouse_float = False
            else:
                try:
                    if self.layout.container.floats:
                        if self.layout.container.floats[-1].name =='mouse':
                            self.layout.container.floats.remove(self.layout.container.floats[-1])
                            self.root_layout.floats.append(mouse_float_container)
                            self.mouse_cord=(x,y)
                            self.mouse_float = False
                        else:
                            self.root_layout.floats.append(mouse_float_container)
                            self.mouse_cord=(x,y)
                            self.mouse_float = False
                    else:
                        self.root_layout.floats.append(mouse_float_container)
                        self.mouse_cord=(x,y)
                        self.mouse_float = False
                except Exception:
                    pass

        elif mouse_click == "LEFT" and mouse_event == MouseEventType.MOUSE_DOWN and self.mouse_float == False:
            try:
                if self.layout.container.floats:
                    if self.layout.container.floats[-1].name == 'mouse':
                        self.layout.container.floats.remove(self.layout.container.floats[-1])
                        self.mouse_float = True
                        if self.mouse_select == mouse_operations.Copy.name:
                            data = self.current_buffer.copy_selection(False)
                            self.clipboard.set_data(data) 
                        elif self.mouse_select == mouse_operations.Paste.name:
                            data = self.clipboard.get_data()
                            self.current_buffer.paste_clipboard_data(data)
                        elif self.mouse_select == mouse_operations.Cut.name:
                            data = self.current_buffer.copy_selection(True)
                            self.clipboard.set_data(data) 
            except Exception:
                pass

        if self.layout.container.floats:
            try :
                get_float_name = self.layout.container.floats[-1].name 
            except Exception:
                get_float_name = ''

            if get_float_name == 'mouse':

                if self.mouse_cord[0] <= x and self.mouse_cord[0] >= x-5:
                    res = []
                    if self.mouse_cord[1] in [y - mouse_op.value for mouse_op in mouse_operations]:
                        for mouse_op in mouse_operations:
                            tmp_ = style_tmp
                            if self.mouse_cord[1] == y - mouse_op.value:
                                self.mouse_select = mouse_op.name
                                tmp_ = style_tmp_red
                            res.append(HTML(tmp_.format(mouse_op.name.ljust(5))))
                            res.append("\n")
                    else:
                        self.mouse_select = None

                    if res:
                        self.layout.container.floats[-1].content.content.text=merge_formatted_text(res) 

                else:
                    res = []
                    for mouse_op in mouse_operations:
                        res.append(HTML(style_tmp.format(mouse_op.name)))
                        res.append("\n")
                    self.layout.container.floats[-1].content.content.text=merge_formatted_text(res)
                    self.mouse_select = None


    def up(self, ev: KeyPressEvent) -> None:
        self.layout.focus(Frame(self.nav_bar.nav_window))

    def focus_next(self, ev: KeyPressEvent) -> None:
        focus_next(ev)

    def focus_previous(self, ev: KeyPressEvent) -> None:
        focus_previous(ev)

    def help(self,ev: KeyPressEvent) -> None:
        
        plugin = self._plugins[self.nav_bar.cur_navbar_selection]
        if callable(getattr(plugin, "help", None)):
            plugin.help()
        else:
            self.show_message(_("Help"),
                self.jans_help,tobefocused=self.center_container)

    def escape(self,ev: KeyPressEvent) -> None:
        try:
            if self.layout.container.floats:
                if len(self.layout.container.floats) >=2 :
                    self.layout.container.floats.remove(self.layout.container.floats[-1])
                    self.layout.focus(self.layout.container.floats[-1].content)
                else:
                    self.layout.container.floats.remove(self.layout.container.floats[0])
                    self.layout.focus(self.center_frame)
        except Exception as e:
            pass

    def get_help_from_schema(
        self, 
        schema: OrderedDict, 
        jans_name: str
        ) -> str:
        for prop in schema.get('properties', {}):
            if prop == jans_name:
                return schema['properties'][jans_name].get('description', '')

    def getTitledText(
            self,
            title: AnyFormattedText = "",
            name: AnyFormattedText = "",
            value: AnyFormattedText = "",
            height: Optional[int] = 1,
            jans_help: AnyFormattedText = "",
            accept_handler: Callable = None,
            read_only: Optional[bool] = False,
            focusable: Optional[bool] = None,
            width: AnyDimension = None,
            style: AnyFormattedText = '',
            widget_style: AnyFormattedText = '',
            scrollbar: Optional[bool] = False,
            line_numbers: Optional[bool] = False,
            lexer: PygmentsLexer = None,
            text_type: Optional[str] = 'string',
            jans_list_type: Optional[bool] = False,
            password: Optional[bool] = False,
            next_widget = None
            ) -> AnyContainer:

        if value is None:
            value = ''

        ta = TextArea(
                text=str(value),
                multiline=height > 1,
                height=height,
                width=width,
                read_only=read_only,
                style=widget_style or (self.styles['textarea-readonly'] if read_only else self.styles['textarea']),
                accept_handler=accept_handler,
                focusable=not read_only if focusable is None else focusable,
                scrollbar=scrollbar,
                line_numbers=line_numbers,
                lexer=lexer,
                password=password,
            )


        if text_type == 'integer':
            ta.buffer.on_text_insert=IntegerValidator(ta)

        ta.window.text_type = text_type
        ta.window.jans_name = name
        ta.window.jans_help = jans_help

        titled_text_widgets = [Window(FormattedTextControl(title+': '), width=len(title)+1, style=style, height=height), ta]
        if next_widget:
            titled_text_widgets.append(Window(width=1))
            titled_text_widgets.append(next_widget)

        v = VSplit(titled_text_widgets)
        v.me = ta
        v.title = title

        if jans_list_type:
            v.jans_list_type = True

        return v
 
    def getTitledCheckBoxList(
        self,
        title: AnyFormattedText,
        name: AnyFormattedText,
        values: Optional[list] = [],
        current_values: Optional[list] = [],
        jans_help: AnyFormattedText= "",
        style: AnyFormattedText= "",
        widget_style: AnyFormattedText = '',
        ) -> AnyContainer:

        title += ': '
        if values and not (isinstance(values[0], tuple) or isinstance(values[0], list)):
            values = [(o,o) for o in values]
        cbl = CheckboxList(values=values)
        cbl.current_values = current_values
        cbl.window.jans_name = name
        cbl.window.jans_help = jans_help

        v = VSplit([Window(FormattedTextControl(title), width=len(title)+1, style=style,), cbl], style=widget_style)
        v.me = cbl

        return v

    def getTitledCheckBox(
            self,
            title: AnyFormattedText,
            name: AnyFormattedText,
            text: AnyFormattedText= "",
            checked: Optional[bool] = False,
            on_selection_changed: Callable= None,
            jans_help: AnyFormattedText= "",
            style: AnyFormattedText= "",
            widget_style: AnyFormattedText = '',
            ) -> AnyContainer:

        title += ': '
        cb = Checkbox(text)
        if widget_style:
            cb.default_style = widget_style
            cb.checked_style = widget_style
            cb.selected_style = widget_style
        cb.checked = checked
        cb.window.jans_name = name
        cb.window.jans_help = jans_help

        handler_org = cb._handle_enter
        def custom_handler():
            handler_org()
            on_selection_changed(cb)

        if on_selection_changed:
            cb._handle_enter = custom_handler

        v = VSplit([Window(FormattedTextControl(title), width=len(title)+1, style=style), cb], height=1, style=widget_style)

        v.me = cb

        return v

    def getTitledRadioButton(
            self, 
            title: AnyFormattedText, 
            name: AnyFormattedText,
            values: Optional[list] = [],
            current_value: AnyFormattedText= "",
            on_selection_changed: Callable= None,
            jans_help: AnyFormattedText= "",
            style: AnyFormattedText= "",
            widget_style: AnyFormattedText = '',
            ) -> AnyContainer:

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

        v = VSplit([Window(FormattedTextControl(title), width=len(title)+1, style=style,), rl], height=len(values))

        v.me = rl

        return v

    def getTitledWidget(
        self, 
        title: AnyFormattedText,
        name: AnyFormattedText,
        widget: AnyContainer,
        jans_help: AnyFormattedText="",
        style: AnyFormattedText="",
        other_widgets: Optional[Sequence[AnyContainer]]=None,
        height: int=1
        )-> AnyContainer:
        title += ': '
        widget.window.jans_name = name
        widget.window.jans_help = jans_help

        my_widgets = [Window(FormattedTextControl(title), width=len(title)+1, style=style, height=height), widget]
        if other_widgets:
            my_widgets.append(other_widgets)

        v = VSplit(my_widgets)
        v.me = widget

        return v

    def getButton(
                self, 
                text: AnyFormattedText,
                name: AnyFormattedText,
                jans_help: AnyFormattedText,
                handler: Callable= None, 
                ) -> Button:

        b = Button(text=text, width=len(text)+2)
        b.window.jans_name = name
        b.window.jans_help = jans_help
        if handler:
            b.handler = handler
        return b

    def getButtonWithHandler(
                self, 
                text: AnyFormattedText,
                name: AnyFormattedText,
                jans_help: AnyFormattedText,
                handler: Callable= None, 
                ) -> Button:

        b = Button(text=text, width=len(text)+2)
        b.window.jans_name = name
        b.window.jans_help = jans_help
        if handler:
            b.handler = lambda:handler(name)
        return b

    def update_status_bar(self) -> None:
        cur_text = self.pbar_text
        if self.progressing_text:
            self.pbar_text = self.progressing_text
        elif hasattr(self.layout.current_window, 'jans_help') and self.layout.current_window.jans_help:
            self.pbar_text = self.layout.current_window.jans_help
        else:
            self.pbar_text = ''
        if cur_text != self.pbar_text:
            self.invalidate()

    def get_plugin_by_id(self, pid: str) -> None:
        for plugin in self._plugins:
            if plugin.pid == pid:
                return plugin

    def main_nav_selection_changed(self, selection: str) -> None:
        plugin = self.get_plugin_by_id(selection)
        if hasattr(plugin, 'on_page_enter'):
            plugin.on_page_enter()
        plugin.set_center_frame()

    async def show_dialog_as_float(self, dialog:Dialog, focus=None) -> None:
        'Coroutine.'
        float_ = Float(content=dialog)
        self.root_layout.floats.append(float_)

        if focus:
            self.layout.focus(focus)
        else:
            self.layout.focus(dialog)

        self.invalidate()

        result = await dialog.future

        if float_ in self.root_layout.floats:
            self.root_layout.floats.remove(float_)

        if self.root_layout.floats:
            self.layout.focus(self.root_layout.floats[-1].content)
        else:
            self.layout.focus(self.center_frame)

        return result

    def show_jans_dialog(self, dialog:Dialog, focus=None) -> None:

        async def coroutine():
            focused_before = self.layout.current_window
            result = await self.show_dialog_as_float(dialog, focus)

            if not self.root_layout.floats:
                try:
                    self.layout.focus(focused_before)
                except Exception:
                    self.layout.focus(self.center_frame)

            return result

        asyncio.ensure_future(coroutine())

    def data_display_dialog(self, **params: Any) -> None:

        text_area = TextArea(
                    lexer=DynamicLexer(lambda: PygmentsLexer.from_filename('.json', sync_from_start=True)),
                    scrollbar=True,
                    line_numbers=True,
                    multiline=True,
                    read_only=True,
                    text=str(json.dumps(params['data'], indent=2)),
                    style='class:jans-main-datadisplay.text'
                )

        data_display_widgets = [text_area]
        if 'message' in params:
            data_display_widgets.insert(0, Label(params['message'], style="blink"))

        body = HSplit(data_display_widgets, style='class:jans-main-datadisplay')
        title = params.get('title') or params['selected'][0]

        def do_save(path):
            try:
                with open(path, 'w') as w:
                    w.write(text_area.text)
                self.pbar_text = _("File {} was saved".format(text_area.text))
                self.show_message(_("Info"), _("File {} was successfully saved").format(path), tobefocused=self.center_container)
            except Exception as e:
                self.show_message(_("Error!"), _("An error ocurred while saving") + ":\n{}".format(str(e)), tobefocused=self.center_container)

        def save(dialog):
            file_browser_dialog = jans_file_browser_dialog(self, path=self.browse_path, browse_type=BrowseType.save_as, ok_handler=do_save)
            self.show_jans_dialog(file_browser_dialog)

        save_button = Button(_("Export"), handler=save)
        buttons = [Button('Close'), save_button]
        dialog = JansGDialog(self, title=title, body=body, buttons=buttons)
        self.show_jans_dialog(dialog)

    def save_creds(self, dialog:Dialog) -> None:

        for child in dialog.body.children:
            prop_name = child.children[1].jans_name
            prop_val = child.children[1].content.buffer.text
            if prop_name == 'jca_client_secret':
                config_cli.config['DEFAULT']['jca_client_secret_enc'] = config_cli.obscure(prop_val)
                if 'jca_client_secret' in config_cli.config['DEFAULT']:
                    del config_cli.config['DEFAULT']['jca_client_secret']
            else:
                config_cli.config['DEFAULT'][prop_name] = prop_val
            config_cli.write_config()

        config_cli.config['DEFAULT']['user_data'] = ''
        config_cli.write_config()

        config_cli.host = config_cli.config['DEFAULT']['jans_host']
        config_cli.client_id = config_cli.config['DEFAULT']['jca_client_id']

        if 'jca_client_secret' in config_cli.config['DEFAULT']:
            config_cli.client_secret = config_cli.config['DEFAULT']['jca_client_secret']
        else:
            config_cli.client_secret = config_cli.unobscure(config_cli.config['DEFAULT']['jca_client_secret_enc'])
        config_cli.access_token = None

        log_dir = config_cli.config['DEFAULT'].get('log_dir')
        if log_dir:
            if not os.path.exists(log_dir):
                os.makedirs(log_dir, mode=0o700)
            config_cli.log_dir = log_dir
            if hasattr(self, 'cli_object'):
                self.cli_object.set_logging()

    def show_message(
            self, 
            title: AnyFormattedText,  
            message: AnyFormattedText,  
            buttons:Optional[Sequence[Button]] = [],
            tobefocused: AnyContainer= None
            ) -> None:
        body = HSplit([Label(message)])
        dialog = JansMessageDialog(title=title, body=body, buttons=buttons)

        if not tobefocused:
            focused_before = self.root_layout.floats[-1].content if self.root_layout.floats else self.layout.current_window #show_message
        else :
            focused_before = tobefocused
        float_ = Float(content=dialog)
        self.root_layout.floats.append(float_)
        dialog.me = float_
        dialog.focus_on_exit = focused_before
        self.layout.focus(dialog)
        self.invalidate()


    def get_confirm_dialog(
            self,
            message: AnyFormattedText,
            confirm_handler: Optional[Callable]=None
        ) -> Dialog:
        body = VSplit([Label(message)], align=HorizontalAlign.CENTER)
        buttons = [Button(_("No")), Button(_("Yes"), handler=confirm_handler)]
        dialog = JansGDialog(self, title=_("Confirmation"), body=body, buttons=buttons, width=self.dialog_width-20)
        return dialog


application = JansCliApp()

def run():
    with patch_stdout(application):
        result = application.run(pre_run=lambda: application.create_background_task(application.check_jans_cli_ini()))
    print("See you next time.")


if __name__ == "__main__":
    run()
