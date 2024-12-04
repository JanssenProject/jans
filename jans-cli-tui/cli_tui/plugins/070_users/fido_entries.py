import json
import asyncio
from typing import Any

from prompt_toolkit import HTML
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.layout.containers import HSplit
from prompt_toolkit.eventloop import get_event_loop

from utils.static import cli_style, common_strings
from utils.utils import common_data
from utils.multi_lang import _
from wui_components.jans_cli_dialog import JansGDialog
from wui_components.jans_vetrical_nav import JansVerticalNav

class FidoEntries():
    """Fido entry displaying and deleting dialog
    """
    def __init__(self, user_data):
        self.user_data = user_data
        self.fido_entries_dialog = None
        self.users_devices_box = JansVerticalNav(
            myparent=common_data.app,
            headers=[_("Nickname"), _("Modality"), _("Date Added"), _("Auth Type")],
            preferred_size= [20, 20 ,30, 30],
            data=[],
            on_enter=self.display_device_details,
            on_display=common_data.app.data_display_dialog,
            on_delete=self.delete_device,
            selectes=0,
            headerColor=cli_style.navbar_headcolor,
            entriesColor=cli_style.navbar_entriescolor,
            all_data=[]
        )


    def delete_device(self, **kwargs: Any) -> None:

        selected_idx = kwargs['selected_idx']
        seleted_device = self.fido_entries[selected_idx]
        device_name = seleted_device['deviceData']['name']

        async def delete_fido_devices_coroutine():
            cli_args = {'operation_id': 'delete-fido2-data', 'url_suffix': f'jansId:{seleted_device["id"]}'}
            common_data.app.start_progressing(_("Deleting FIDO devices ..."))
            response = await get_event_loop().run_in_executor(common_data.app.executor, common_data.app.cli_requests, cli_args)
            if response:
                error_string = ''
                if isinstance(response,str):
                    error_string = response
                elif response.status_code != 200:
                    error_string = response.text
                if error_string:
                    common_data.app.show_message(
                        _(common_strings.error),
                        HTML(_("An error ocurred while deleting FDIO device.\nServer response: {}.").format(error_string)),
                        tobefocused=self
                    )
            self.get_user_fido_entries(show_dialog=False)

        def do_delete_device(result):
            asyncio.ensure_future(delete_fido_devices_coroutine())

        confirm_dialog = common_data.app.get_confirm_dialog(
            HTML(_("Are you sure you want to delete FIDO device <b>{}</b>?").format(device_name)),
            confirm_handler=do_delete_device
            )

        common_data.app.show_jans_dialog(confirm_dialog)

    def display_device_details(self, **kwargs: Any) -> None:
            data = kwargs['data']
            reg_data = data['registrationData']
            dev_data = data['deviceData']
            device_details_widgets = []

            for title, value in (
                        (_("Domain"), reg_data.get('domain','')),
                        (_("Type"), reg_data.get('type','')),
                        (_("Status"), reg_data.get('status','')),
                        (_("Created By"), reg_data.get('createdBy','')),
                        (_("Device Name"), dev_data.get('name','')),
                        (_("OS Name"), dev_data.get('os_name','')),
                        (_("Platform"), dev_data.get('platform','')),
                    ):

                device_details_widgets.append(
                        common_data.app.getTitledText(
                            title=title,
                            value=value,
                            read_only=True
                        )
                )

            body = HSplit(device_details_widgets, width=D())
            dialog = JansGDialog(common_data.app, title=_("2FA Details"), body=body)
            common_data.app.show_jans_dialog(dialog)


    def update_users_devices_box(self):

        self.users_devices_box.clear()
        self.users_devices_box.all_data = self.fido_entries[:]

        for entry in self.fido_entries:
            reg_data = entry.get('registrationData', {})
            attenstation_request = json.loads(reg_data.get('attenstationRequest', '{}'))
            self.users_devices_box.add_item((
                reg_data.get('username'),
                entry.get('deviceData', {}).get('platform'),
                reg_data.get('createdDate'),
                'Super Gluu' if attenstation_request.get('super_gluu_request') else 'FIDO2'
                ))


    def show_dialog(self) -> None:

        self.fido_entries_dialog = JansGDialog(
                    common_data.app,
                    title=_("FIDO Devices for user {}").format(self.user_data['userId']),
                    body=HSplit([self.users_devices_box], width=D()),
                    )

        common_data.app.show_jans_dialog(self.fido_entries_dialog)


    def get_user_fido_entries(self, show_dialog=True):
        async def get_fido_devices_coroutine():
            cli_args = {'operation_id': 'get-registration-entries-fido2', 'url_suffix': f'username:{self.user_data["userId"]}'}
            common_data.app.start_progressing(_("Retreiving FIDO devices ..."))
            response = await get_event_loop().run_in_executor(common_data.app.executor, common_data.app.cli_requests, cli_args)
            common_data.app.stop_progressing()
            data = response.json()
            self.fido_entries = data.get('entries', [])
            self.update_users_devices_box()
            if self.fido_entries:
                if show_dialog:
                    self.show_dialog()
            else:
                if self.fido_entries_dialog:
                    self.fido_entries_dialog.close()
                common_data.app.show_message(_("Not found"), HTML(_("No FIDO devices registered for user <b>{}</b>.").format(self.user_data["userId"])), tobefocused=common_data.app.center_container)
        asyncio.ensure_future(get_fido_devices_coroutine())

