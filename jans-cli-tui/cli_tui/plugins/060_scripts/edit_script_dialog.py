import os
import asyncio
from functools import partial
from typing import Optional, Sequence, Callable, Any

from pygments.lexers.python import PythonLexer
from pygments.lexers.jvm import JavaLexer

from prompt_toolkit.layout.dimension import D
from prompt_toolkit.layout.containers import HSplit, VSplit, Window
from prompt_toolkit.lexers import PygmentsLexer
from prompt_toolkit.formatted_text import AnyFormattedText
from prompt_toolkit.widgets import Button, Label, TextArea, RadioList,\
    Button, Dialog, Frame

from utils.multi_lang import _
from utils.static import DialogResult
from utils.utils import DialogUtils, common_data
from wui_components.jans_dialog_with_nav import JansDialogWithNav
from wui_components.jans_cli_dialog import JansGDialog
from wui_components.jans_drop_down import DropDownWidget
from wui_components.jans_vetrical_nav import JansVerticalNav
from wui_components.jans_spinner import Spinner
from wui_components.jans_path_browser import jans_file_browser_dialog, BrowseType

class EditScriptDialog(JansGDialog, DialogUtils):
    """This Script editing dialog
    """
    def __init__(
        self,
        parent,
        data:list,
        title: AnyFormattedText= "",
        buttons: Optional[Sequence[Button]]= [],
        save_handler: Callable= None,
        )-> Dialog:
        """init for `EditScriptDialog`, inherits from two diffrent classes `JansGDialog` and `DialogUtils`
            
        JansGDialog (dialog): This is the main dialog Class Widget for all Jans-cli-tui dialogs except custom dialogs like dialogs with navbar
        DialogUtils (methods): Responsable for all `make data from dialog` and `check required fields` in the form for any Edit or Add New
                
        Args:
            parent (widget): This is the parent widget for the dialog, to access `Pageup` and `Pagedown`
            title (str): The Main dialog title
            data (list): selected line data 
            button_functions (list, optional): Dialog main buttons with their handlers. Defaults to [].
            save_handler (method, optional): handler invoked when closing the dialog. Defaults to None.
        """
        super().__init__(parent, title, buttons)
        self.myparent = parent
        self.save_handler = save_handler
        self.data = data
        self.title=title
        self.cur_lang = self.data.get('programmingLanguage', 'python')
        self.create_window()
        self.script = self.data.get('script','')
        self.script_enabled = self.data.get('enabled')

    def save(self) -> None:
        """method to invoked when saving the dialog (Save button is pressed)
        """

        data = {}

        for item in self.edit_dialog_content:
            item_data = self.get_item_data(item)
            if item_data:
                data[item_data['key']] = item_data['value']

        for prop_container in (self.config_properties_container, self.module_properties_container):

            if prop_container.data:
                data[prop_container.jans_name] = []
                for prop_ in prop_container.data:
                    key_ = prop_[0]
                    val_ = prop_[1]

                    if key_:
                        prop = {'value1': key_}
                        if val_:
                            prop['value2'] = val_
                        data[prop_container.jans_name].append(prop)

        data['locationType'] = data.get('locationType')
        data['internal'] = self.data.get('internal', False)
        data['modified'] = self.data.get('modified', False)
        data['revision'] = self.data.get('revision', 0) + 1
        data['script'] = self.script

        if data['locationType'] != 'file':
            data['locationType'] = 'db'

        if not 'moduleProperties' in data:
            data['moduleProperties'] = []

        for prop in data['moduleProperties'][:]:
            if prop['value1'] in ('location_type', 'location_path'):
                data['moduleProperties'].remove(prop)

        location_type = data.pop('locationType')
        data['moduleProperties'].append({'value1': 'location_type', 'value2': location_type})

        if location_type == 'file':
            data['moduleProperties'].append({'value1': 'location_path', 'value2': data.pop('locationPath')})

        if self.data.get('baseDn'):
            data['baseDn'] = self.data['baseDn']

        self.new_data = data

        if self.save_handler:
            self.save_handler(self)


    def cancel(self) -> None:
        """method to invoked when canceling changes in the dialog (Cancel button is pressed)
        """

        self.future.set_result(DialogResult.CANCEL)

    def create_window(self) -> None:

        schema = self.myparent.cli_object.get_schema_from_reference('', '#/components/schemas/CustomScript')


        script_types = []
        for scr_type in common_data.script_types:
            scr_name = ' '.join([w.title() for w in scr_type.split('_')])
            script_types.append((scr_type, scr_name))
        script_types.sort()


        file_name = ''
        if self.data.get('locationType') == 'file':
            for prop in self.data['moduleProperties'][:]:
                if prop['value1'] == 'location_path':
                    location_path = prop.get('value2', '')
                    file_name = os.path.basename(location_path)

        self.location_widget = self.myparent.getTitledText(
            _("          File Name"), 
            name='locationPath', 
            value=file_name,
            style='class:script-titledtext', 
            jans_help="locationPath"
            )

        self.set_location_widget_state(self.data.get('locationType') == 'file')

        config_properties_title = _("Config Properties: ")
        add_property_title = _("Add Property")
        module_properties_title = _("Module Properties: ")

        config_properties_data = []
        for prop in self.data.get('configurationProperties', []):
            config_properties_data.append([prop['value1'], prop.get('value2', '')])

        config_prop_data_lenght = len(config_properties_data)
        config_prop_max_height = config_prop_data_lenght if config_prop_data_lenght > 2 else 3

        self.config_properties_container = JansVerticalNav(
                myparent=self.myparent,
                headers=['Key', 'Value'],
                preferred_size=[15, 15, 5],
                data=config_properties_data,
                on_enter=self.edit_property,
                on_delete=self.delete_config_property,
                on_display=self.myparent.data_display_dialog,
                get_help=(self.get_help,'Properties'),
                selectes=0,
                headerColor='class:outh-client-navbar-headcolor',
                entriesColor='class:outh-client-navbar-entriescolor',
                all_data=config_properties_data,
                underline_headings=False,
                max_width=44,
                jans_name='configurationProperties',
                max_height=config_prop_max_height
                )

        module_properties_data = []
        for prop in self.data.get('moduleProperties', []):
            if prop['value1'] in ('location_type', 'location_path'):
                continue
            module_properties_data.append([prop['value1'], prop.get('value2', '')])

        module_prop_data_lenght = len(module_properties_data)
        module_prop_max_height = module_prop_data_lenght if module_prop_data_lenght > 2 else 3

        self.module_properties_container = JansVerticalNav(
                myparent=self.myparent,
                headers=['Key', 'Value'],
                preferred_size=[20, 20],
                data=module_properties_data,
                on_enter=self.edit_property,
                on_delete=self.delete_config_property,
                on_display=self.myparent.data_display_dialog,
                get_help=(self.get_help,'Properties'),
                selectes=0,
                headerColor='class:outh-client-navbar-headcolor',
                entriesColor='class:outh-client-navbar-entriescolor',
                all_data=module_properties_data,
                underline_headings=False,
                max_width=44,
                jans_name='moduleProperties',
                max_height=module_prop_max_height
                )

        open_editor_button_title = _("Edit Script")
        open_editor_button = Button(text=open_editor_button_title, width=len(open_editor_button_title)+2, handler=self.edit_script_dialog)
        open_editor_button.window.jans_help="Enter to open editing window"

        import_script_button_title = _("Import Script")
        import_script_button = Button(text=import_script_button_title, width=len(import_script_button_title)+2, handler=self.import_script_dialog)
        import_script_button.window.jans_help="Enter to import script for local file"

        self.edit_dialog_content = [
                    self.myparent.getTitledText(_("Inum"), name='inum', value=self.data.get('inum',''), style='class:script-titledtext', jans_help=self.myparent.get_help_from_schema(schema, 'inum'), read_only=True),
                    self.myparent.getTitledWidget(
                                _("Script Type"),
                                name='scriptType',
                                widget=DropDownWidget(
                                    values=script_types,
                                    value=self.data.get('scriptType', '')
                                    ),
                                jans_help=self.myparent.get_help_from_schema(schema, 'scriptType'),
                                style='class:outh-client-dropdown'),

                    self.myparent.getTitledCheckBox(_("Enabled"), name='enabled', checked=self.data.get('enabled'), style='class:script-checkbox', jans_help=self.myparent.get_help_from_schema(schema, 'enabled')),
                    self.myparent.getTitledText(_("Name"), name='name', value=self.data.get('name',''), style='class:script-titledtext', jans_help=self.myparent.get_help_from_schema(schema, 'name')),
                    self.myparent.getTitledText(_("Description"), name='description', value=self.data.get('description',''), style='class:script-titledtext', jans_help=self.myparent.get_help_from_schema(schema, 'description')),


                    self.myparent.getTitledRadioButton(
                            _("Location"),
                            name='locationType',
                            values=[('db', _("Database")), ('file', _("File System"))],
                            current_value= 'file' if self.data.get('locationType') == 'file' else 'db',
                            jans_help=_("Where to save script"),
                            style='class:outh-client-radiobutton',
                            on_selection_changed=self.script_location_changed,
                            ),

                     self.location_widget,

                    self.myparent.getTitledWidget(
                                _("Programming Language"),
                                name='programmingLanguage',
                                widget=DropDownWidget(
                                    values=[['python', 'Python'], ['java', 'Java']],
                                    value=self.cur_lang,
                                    on_value_changed=self.script_lang_changed,
                                    ),
                                jans_help=self.myparent.get_help_from_schema(schema, 'programmingLanguage'),
                                style='class:outh-client-dropdown'),

                    self.myparent.getTitledWidget(
                                _("Level"),
                                name='level',
                                widget=Spinner(
                                    value=self.data.get('level', 0)
                                    ),
                                jans_help=self.myparent.get_help_from_schema(schema, 'level'),
                                style='class:outh-client-dropdown'),

                    VSplit([
                            Label(text=config_properties_title, style='class:script-label', width=len(config_properties_title)+1), 
                            self.config_properties_container,
                            Window(width=2),
                            HSplit([
                                Window(height=1),
                                Button(text=add_property_title, width=len(add_property_title)+4, handler=partial(self.edit_property, jans_name='configurationProperties')),
                                ]),
                            ],
                            height=config_prop_max_height+1, width=D(),
                            ),

                    VSplit([
                            Label(text=module_properties_title, style='class:script-label', width=len(module_properties_title)+1), 
                            self.module_properties_container,
                            Window(width=2),
                            HSplit([
                                Window(height=1),
                                Button(text=add_property_title, width=len(add_property_title)+4, handler=partial(self.edit_property, jans_name='moduleProperties')),
                                ]),
                            ],
                             height=module_prop_max_height+1
                            ),
                    VSplit([open_editor_button, import_script_button, Window(width=D())], padding=2),
                    ]


        self.dialog = JansDialogWithNav(
            title=self.title,
            content= HSplit(
                self.edit_dialog_content,
                width=D()
                ),
            button_functions=[(self.cancel, _("Cancel")), (self.save, _("Save"))],
            height=self.myparent.dialog_height,
            width=self.myparent.dialog_width,
            )

    def get_help(self, **kwargs: Any):
        """This method get focused field Description to display on statusbar
        """

        # schema = self.app.cli_object.get_schema_from_reference('#/components/schemas/{}'.format(str(kwargs['scheme'])))
    
        if kwargs['scheme'] == 'Properties':
            self.myparent.status_bar_text= kwargs['data'][0]

    def script_lang_changed(
        self, 
        value: str,
        ) -> None:
        """Change the script lang

        Args:
            value (str): lang to change to (python, java)
        """
        self.cur_lang = value

    def set_location_widget_state(
        self, 
        state: bool,
        ) -> None:
        """This method check the state of the location to save script

        Args:
            state (bool): state is changed or not
        """
        self.location_widget.me.read_only = not state
        self.location_widget.me.focusable = state
        if not state:
            self.location_widget.me.text = ''

    def script_location_changed(
        self, 
        redio_button: RadioList,
        ) -> None:
        """Location to save Script

        Args:
            redio_button (RadioList): Where to save the scripts (Database, Filesystem)
        """
        state = redio_button.current_value == 'file'
        self.set_location_widget_state(state)

    def edit_property(self, **kwargs: Any) -> None:
        """This method for editing the properties 
        """

        if kwargs['jans_name'] == 'moduleProperties':
            title = _("Enter Module Properties")
        else:
            title = _("Enter Configuration Properties")

        prop_data = kwargs.get('data', ('',''))
        key_widget = self.myparent.getTitledText(_("Key"), name='property_key', value=prop_data[0], style='class:script-titledtext', jans_help=_("Script propery Key"))
        val_widget = self.myparent.getTitledText(_("Value"), name='property_val', value=prop_data[1], style='class:script-titledtext', jans_help=_("Script property Value"))

        def add_property(dialog: Dialog) -> None:
            key_ = key_widget.me.text
            val_ = val_widget.me.text
            cur_data = [key_, val_]

            container = self.config_properties_container if kwargs['jans_name'] == 'configurationProperties' else self.module_properties_container

            if not kwargs.get('data'):
                container.add_item(cur_data)
            else:
                container.replace_item(kwargs['selected'], cur_data)

        body_widgets = [key_widget, val_widget]

        body = HSplit(body_widgets)
        buttons = [Button(_("Cancel")), Button(_("OK"), handler=add_property)]
        dialog = JansGDialog(self.myparent, title=title, body=body, buttons=buttons, width=self.myparent.dialog_width-20)
        self.myparent.show_jans_dialog(dialog)

    def delete_config_property(self, **kwargs: Any) -> None:
        """This method for deleting the coniguration of properties
        """
        dialog = self.myparent.get_confirm_dialog(_("Are you sure want to delete property with Key:")+"\n {} ?".format(kwargs['selected'][0]))

        async def coroutine():
            focused_before = self.myparent.layout.current_window
            result = await self.myparent.show_dialog_as_float(dialog)
            try:
                self.myparent.layout.focus(focused_before)
            except Exception:
                self.myparent.stop_progressing()
                self.myparent.layout.focus(self.myparent.center_frame)

            if result.lower() == 'yes':
                if kwargs['jans_name'] == 'configurationProperties':
                    self.config_properties_container.remove_item(kwargs['selected'])
                else:
                    self.module_properties_container.remove_item(kwargs['selected'])
                self.myparent.stop_progressing()

            return result

        asyncio.ensure_future(coroutine())



    def import_script_dialog(self) -> None:

        def set_script(path):
            with open(path, 'rb') as f:
                script_content = f.read()
            self.script = script_content.decode()

        def open_file_browser_dialog(dialog=None):
            file_browser_dialog = jans_file_browser_dialog(self.myparent, path=self.myparent.browse_path, browse_type=BrowseType.file, ok_handler=set_script)
            self.myparent.show_jans_dialog(file_browser_dialog)

        if self.script:
            dialog = self.myparent.get_confirm_dialog(_("Are you sure want to replace script?"), confirm_handler=open_file_browser_dialog)
            self.myparent.show_jans_dialog(dialog)
        else:
            open_file_browser_dialog()


    def edit_script_dialog(self) -> None:
        """This method shows the script itself and let the user view or edit it
        """

        text_editor = TextArea(
                text=self.script,
                multiline=True,
                height=self.myparent.dialog_height-10,
                width=D(),
                focusable=True,
                scrollbar=True,
                line_numbers=True,
                wrap_lines=False,
                lexer=PygmentsLexer(PythonLexer if self.cur_lang == 'PYTHON' else JavaLexer),
            )

        def modify_script(arg) -> None:
            self.script = text_editor.text

        buttons = [Button(_("Cancel")), Button(_("OK"), handler=modify_script)]
        dialog = JansGDialog(self.myparent, title=_("Edit Script"), body=HSplit([text_editor]), buttons=buttons, width=self.myparent.dialog_width-10)
        self.myparent.show_jans_dialog(dialog)

    def __pt_container__(self)-> Dialog:
        """The container for the dialog itself

        Returns:
            Dialog: The Edit Script Dialog
        """  
        return self.dialog

