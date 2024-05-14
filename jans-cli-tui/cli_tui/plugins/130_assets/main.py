import os
import copy
import asyncio

from typing import Any, Optional
from prompt_toolkit.layout.containers import HSplit, DynamicContainer,\
    VSplit, Window, FormattedTextControl

from prompt_toolkit.eventloop import get_event_loop
from prompt_toolkit.buffer import Buffer
from prompt_toolkit.layout import ScrollablePane
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.widgets import Button, Label, Dialog
from prompt_toolkit.application import Application
from prompt_toolkit.formatted_text import HTML

from wui_components.widget_collections import get_logging_level_widget

from utils.multi_lang import _
from utils.utils import DialogUtils, common_data
from utils.static import DialogResult, cli_style, common_strings
from wui_components.jans_vetrical_nav import JansVerticalNav
from wui_components.jans_cli_dialog import JansGDialog
from wui_components.jans_spinner import Spinner
from wui_components.jans_path_browser import jans_file_browser_dialog, BrowseType



class Plugin(DialogUtils):
    """This is a general class for plugins 
    """
    def __init__(
        self, 
        app: Application
        ) -> None:
        """init for Plugin class "Lock"

        Args:
            app (Generic): The main Application class
        """
        self.app = app
        self.pid = 'assets'
        self.name = 'Assets'
        self.server_side_plugin = False
        self.data = {}
        self.assets_container = HSplit([])
        self.assets_list_box = JansVerticalNav(
                myparent=common_data.app,
                headers=['inum', _("Display Name"), _("Enabled"), _("Creation Time")],
                preferred_size= [40, 40 ,10, 20],
                data=[],
                on_enter=self.edit_asset,
                on_display=common_data.app.data_display_dialog,
                on_delete=self.delete_asset,
                selectes=0,
                headerColor=cli_style.navbar_headcolor,
                entriesColor=cli_style.navbar_entriescolor,
                all_data=[]
            )

    def init_plugin(self) -> None:
        """The initialization for this plugin
        """

        self.app.create_background_task(self.get_assets())

    def set_center_frame(self) -> None:
        """center frame content
        """

        self.main_container = HSplit([
                    VSplit([
                        common_data.app.getTitledText(_("Search"), name='assets:search', jans_help=_("Press enter to perform search"), accept_handler=self.search_asset, style=cli_style.black_bg_widget),
                        common_data.app.getButton(text=_("Add Asset"), name='assets:add', jans_help=_("To add a new asset press this button"), handler=self.edit_asset),
                        ],
                        padding=3,
                        width=D(),
                    ),
                    Window(height=1),
                    DynamicContainer(lambda: self.assets_container),
                    ],
                    style=cli_style.container
                    )


        common_data.app.center_container = self.main_container


    def set_assest_info(self, msg):
        self.assets_container = HSplit([Label(text=msg)])

    def edit_asset(self, **kwargs: Any) -> None:
        """Method to display the edit asset dialog
        """
        if kwargs:
            data = kwargs.get('data', {})
        else:
            data = {}

        title = _("Edit Asset") if data else _("Add Asset")

        self.asset_file_path = ''

        def save_asset(dialog: Dialog) -> None:
            if not self.asset_file_path:
                self.app.show_message(common_strings.error, _("Please select asset"), tobefocused=dialog)
                return

            new_data = self.make_data_from_dialog(tabs={'asset': dialog.body})
            data.update(new_data)

            if not (data.get('description') and data.get('displayName')):
                self.app.show_message(common_strings.error, HTML(_("Please fill <b>Description</b> and <b>Display Name</b>")), tobefocused=dialog)
                return

            for prop in data['jansModuleProperty'][:]:
                if not prop:
                    data['jansModuleProperty'].remove(prop)

            data.pop('document', None)
            form_data = {'assetFile': self.asset_file_path, 'document': data}

            operation_id = 'put-asset' if data else 'post-new-asset'
            cli_args = {'operation_id': operation_id, 'data': form_data}

            async def coroutine():
                common_data.app.start_progressing()
                response = await common_data.app.loop.run_in_executor(common_data.app.executor, common_data.app.cli_requests, cli_args)

                if response.status_code in (200, 201):
                    self.data = response.json()
                    common_data.app.stop_progressing(_("Asset was saved."))
                    dialog.future.set_result(DialogResult.ACCEPT)
                #    await self.myparent.get_trust_relations()

                else:
                    common_data.app.show_message(_(common_strings.error), _("Save failed: Status {} - {}\n").format(response.status_code, response.text), tobefocused=dialog)
                    common_data.app.stop_progressing(_("Failed to save Trust Relationship."))

                #dialog.future.set_result(DialogResult.ACCEPT)
                await self.get_assets()

            asyncio.ensure_future(coroutine())


        def display_file_browser_dialog():
            common_data.app.show_jans_dialog(file_browser_dialog)


        display_name_widget = common_data.app.getTitledText(_("Display Name"), name='displayName', value=data.get('displayName'), style=cli_style.edit_text_required)

        def read_asset(path):
            self.asset_file_path = path
            display_name_widget.me.text = os.path.basename(path)

        asset_browse_button = Button(text="Browse", handler=display_file_browser_dialog)
        inum_widget = common_data.app.getTitledText(_("inum"), name='inum', value=data.get('inum'), read_only=True, style=cli_style.read_only)
        jans_level_widget =  common_data.app.getTitledWidget(
                                _("Level"),
                                name='jansLevel',
                                widget=Spinner(
                                    value=int(data.get('jansLevel', 0))
                                    ),
                                style=cli_style.drop_down
                            )
        enabled_widget = common_data.app.getTitledCheckBox(_("Enabled"), name='jansEnabled', checked=data.get('jansEnabled'), style=cli_style.check_box)
        description_widget = common_data.app.getTitledText(_("Description"), name='description', value=data.get('description', ''), style=cli_style.edit_text_required)
        #selected_widget = common_data.app.getTitledCheckBox(_("Selected"), name='selected', checked=data.get('selected'), style=cli_style.check_box)
        jans_module_widget = common_data.app.getTitledText(_("Jans Module Properties"), name='jansModuleProperty', value='\n'.join(data.get('jansModuleProperty', [])), height=3, jans_list_type=True, style=cli_style.edit_text, jans_help=_("Enter property each line"))
        #document_widget = common_data.app.getTitledText(_("Document"), name='document', height=3, value=data.get('document', ''), style=cli_style.edit_text)

        save_button = Button(_("Save"), handler=save_asset)
        save_button.keep_dialog = True
        buttons = [Button(_("Cancel")), save_button]

        body = HSplit([
            inum_widget,
            common_data.app.getTitledWidget(
                        _("Asset"),
                        name='asset',
                        widget=Button(_('Browse'), handler=display_file_browser_dialog),
                        style=cli_style.edit_text,
                        other_widgets=VSplit([Window(width=2), Window(FormattedTextControl(lambda: self.asset_file_path))])
                    ),
            display_name_widget,
            description_widget,
            jans_level_widget,
            enabled_widget,
            #selected_widget,
            jans_module_widget,
            #document_widget,
            ],
             width=D())

        edit_asset_dialog = JansGDialog(common_data.app, title=title, body=body, buttons=buttons, width=common_data.app.dialog_width)

        file_browser_dialog = jans_file_browser_dialog(common_data.app, path=common_data.app.browse_path, browse_type=BrowseType.file, ok_handler=read_asset)


        common_data.app.show_jans_dialog(edit_asset_dialog)

    def delete_asset(self, **kwargs: Any) -> None:
        """This method for the deletion of asset
        """

        def do_delete():
            async def coroutine():
                cli_args = {'operation_id': 'delete-asset', 'url_suffix': 'inum:{}'.format(kwargs['selected'][0])}
                self.app.start_progressing(_("Deleting asset {}").format(kwargs['selected'][1]))
                response = await self.app.loop.run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
                self.app.stop_progressing()
                if response:
                    self.app.show_message(_("Error"), _("Deletion was not completed {}".format(response)))
                else:
                    await self.get_assets()

            asyncio.ensure_future(coroutine())

        buttons = [Button(_("No")), Button(_("Yes"), handler=do_delete)]

        self.app.show_message(
                title=_("Confirm"),
                message=HTML(_("Are you sure you want to delete asset <b>{}</b>?")).format(kwargs['selected'][1]),
                buttons=buttons,
                tobefocused=self.assets_container
                )


    def search_asset(self, tbuffer:Buffer) -> None:
        
        async def coroutine(pattern):
            await self.get_assets(pattern=pattern)
        asyncio.ensure_future(coroutine(tbuffer.text))

    async def get_assets(self, pattern='') -> None:
        'Coroutine for getting assets.'
        cli_args = {'operation_id': 'get-all-assets'}
        if pattern:
            cli_args['endpoint_args'] = f'pattern:{pattern}'

        self.app.start_progressing(_("Retreiving assets from server..."))
        response = await get_event_loop().run_in_executor(self.app.executor, self.app.cli_requests, cli_args)
        self.app.stop_progressing()
        
        try:
            result = response.json()
        except Exception as e:
            self.app.show_message(_("Error getting Assets"), str(e), tobefocused=self.app.center_container)
            return

        if response.status_code not in (200, 201):
            self.app.show_message(_("Error getting Assets"), str(response.text), tobefocused=self.app.center_container)
            return

        self.data = response.json()

        if not self.data.get('totalEntriesCount'):
            mesg = _("No assets were found on this server")
            if pattern:
                mesg += _(", with search pattern <b>{}</b>".format(pattern))
            self.set_assest_info(HTML(mesg+'.'))
        else:
            self.assets_list_box.clear()
            self.assets_list_box.all_data = self.data['entries']
            for asset_info in self.data['entries']:
                self.assets_list_box.add_item((asset_info['inum'], asset_info['displayName'], asset_info['jansEnabled'], asset_info['creationDate']))

            self.assets_container = self.assets_list_box
