import asyncio
from functools import partial
from typing import Optional, Any

from prompt_toolkit.application import Application
from prompt_toolkit.eventloop import get_event_loop
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.layout.containers import HSplit, VSplit, DynamicContainer, HorizontalAlign, Window
from prompt_toolkit.widgets import TextArea, Button, Label
from prompt_toolkit.buffer import Buffer
from prompt_toolkit.formatted_text import HTML

from utils.multi_lang import _
from utils.utils import common_data
from utils.utils import DialogUtils
from utils.static import cli_style, common_strings
from wui_components.jans_drop_down import DropDownWidget
from wui_components.jans_vetrical_nav import JansVerticalNav
from wui_components.jans_cli_dialog import JansGDialog
from wui_components.jans_date_picker import DateSelectWidget


class Sessions(DialogUtils):

    def __init__(self) -> None:

        self.prev_next_buttons = VSplit([], width=D())
        self.working_container = JansVerticalNav(
                myparent=common_data.app,
                headers=[_("User"), _("Expiration"), _("Deletable"), _("Client ID")],
                preferred_size= common_data.app.get_column_sizes(0, 0, 0, 0),
                on_display=common_data.app.data_display_dialog,
                on_delete=self.delete_session,
                selectes=0,
                headerColor=cli_style.navbar_headcolor,
                entriesColor=cli_style.navbar_entriescolor,
                hide_headers = True
            )

        self.search_user_text_area = TextArea(style=cli_style.white_bg_widget, multiline=False, height=1, width=15)

        self.search_client_area = common_data.app.getTitledText(
                            title=_("Client ID"),
                            name='oauth:sessions:search_client',
                            jans_help=_(common_strings.enter_to_search),
                            style=cli_style.edit_text
                        )

        self.search_date_after_widget = DateSelectWidget(app=common_data.app)
        self.search_date_before_widget = DateSelectWidget(app=common_data.app)

        username_title = _("Username")
        date_after_title = _("Expires After")
        date_before_title = _("Expires Before")
        search_title = _("Search")

        self.main_container = HSplit([
                    VSplit([
                        Label(username_title + ':', width=len(username_title)+1, style=cli_style.edit_text),
                        self.search_user_text_area,
                        Label(date_after_title + ':', width=len(date_after_title)+1, style=cli_style.edit_text),
                        self.search_date_after_widget,
                        Label(date_before_title + ':', width=len(date_before_title)+1, style=cli_style.edit_text),
                        self.search_date_before_widget,
                        Button(text=search_title, width=len(search_title)+4, handler=self.search_sessions),
                        ],
                        padding=1,
                        height=1,
                        width=D()
                    ),
                    self.working_container,
                    DynamicContainer(lambda: self.prev_next_buttons)
                    ], width=D(), style=cli_style.container)


    def update_working_container(self,
            data: Optional[dict] = {}
        ) -> None:
        """This fucntion updates working container

        Args:
            pattern (str, optional): an optional argument for searching sessions. This argument is passed to get_sessions().
            data (dict, optional): the data to be displayed
        """

        self.working_container.clear()
        all_entries = data.get('entries', [])
        for entry in all_entries:
            self.working_container.add_item((
                        entry.get('sessionAttributes', {}).get('auth_user', '--'),
                        entry.get('expirationDate', '---'),
                        str(entry.get('deletable', False)),
                        entry.get('sessionAttributes', {}).get('client_id', '--'),
                    ))
            self.working_container.all_data = all_entries

        buttons = []
        if data.get('start', 0) > 0:
            handler_partial = partial(self.search_sessions, data['start'] - common_data.app.entries_per_page)
            prev_button = Button(_("Prev"), handler=handler_partial)
            prev_button.window.jans_help = _("Retreives previous %d entries") % common_data.app.entries_per_page
            buttons.append(prev_button)
        if  data.get('start', 0) + common_data.app.entries_per_page <  data.get('totalEntriesCount', 0):
            handler_partial = partial(self.search_sessions, data['start'] + common_data.app.entries_per_page)
            next_button = Button(_("Next"), handler=handler_partial)
            next_button.window.jans_help = _("Retreives next %d entries") % common_data.app.entries_per_page
            buttons.append(next_button)

        if self.working_container.data:
            self.working_container.hide_headers = False
        else:
            self.working_container.hide_headers = True
            self.prev_next_buttons = VSplit([], width=D())
            common_data.app.show_message(_("Oops"), _(common_strings.no_matching_result),tobefocused=self.main_container)
            return

        self.prev_next_buttons = VSplit(buttons, padding=5, align=HorizontalAlign.CENTER)
        common_data.app.layout.focus(self.working_container)


    def search_sessions(
        self,
        start_index: Optional[int]= 0
        ) -> None:
        """This method handel the search for sessions
        """

        endpoint_args = f'limit:{common_data.app.entries_per_page},startIndex:{start_index}'
        search_arg_lists = []
        username = self.search_user_text_area.text
        date_after = self.search_date_after_widget.value
        date_before = self.search_date_before_widget.value

        if username:
            search_arg_lists.append(f'auth_user={username}')

        if date_after:
            date_after = date_after.replace(microsecond=0)
            search_arg_lists.append(f'expirationDate>{date_after}')

        if date_before:
            date_before = date_before.replace(microsecond=0)
            search_arg_lists.append(f'expirationDate<{date_before}')

        if search_arg_lists:
            endpoint_args += ',fieldValuePair:' + '\,'.join(search_arg_lists)


        async def search_sessions_coroutine():
            cli_args = {'operation_id': 'search-session'}
            if endpoint_args:
                cli_args['endpoint_args'] = endpoint_args
            common_data.app.start_progressing(_("Searching sessions for user {}").format(username))
            response = await get_event_loop().run_in_executor(common_data.app.executor, common_data.app.cli_requests, cli_args)
            common_data.app.stop_progressing()

            if response.status_code not in (200, 201):
                common_data.app.show_message(_(common_strings.error), str(response) + ':' + response.text, tobefocused=self.main_container)
                return

            data = response.json()
            self.update_working_container(data=data)

        asyncio.ensure_future(search_sessions_coroutine())




    def delete_session(self, **kwargs: Any) -> None:
        """This method is for deleting session.

        Args:
            kwargs (dict): arguments given by on_delete() function of Nav Bar
        """

        selected_idx = kwargs['selected_idx']
        selected_session = self.working_container.all_data[selected_idx]
        sid = selected_session['sessionAttributes']['sid']

        if not selected_session.get('deletable'):
            common_data.app.show_message(_(common_strings.warning), _("This session cannot be deleted."), tobefocused=self.working_container)
            return

        def do_delete_session(dialog):

            async def coroutine():
                cli_args = {'operation_id': 'delete-session', 'url_suffix': f'sid:{sid}'}
                common_data.app.start_progressing(_("Deleting session {}").format(sid))
                response = await get_event_loop().run_in_executor(common_data.app.executor, common_data.app.cli_requests, cli_args)
                common_data.app.stop_progressing()

                if response:
                    common_data.app.show_message(_(common_strings.error), str(response), tobefocused=self.working_container)
                    return

                self.search_sessions()

            asyncio.ensure_future(coroutine())


        confirm_dialog = common_data.app.get_confirm_dialog(
                HTML(_("Are you sure want to delete session <b>{}</b>.").format(sid)),
                confirm_handler=do_delete_session
                )

        common_data.app.show_jans_dialog(confirm_dialog)
