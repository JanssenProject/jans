import asyncio

from prompt_toolkit.layout.dimension import D
from prompt_toolkit.eventloop import get_event_loop
from prompt_toolkit.layout.containers import HSplit, VSplit, DynamicContainer, HorizontalAlign
from prompt_toolkit.widgets import Button, Frame, RadioList

from utils.multi_lang import _
from utils.utils import DialogUtils
from utils.static import cli_style, common_strings
from utils.utils import common_data

class Message(DialogUtils):
    def __init__(self) -> None:

        self.first_enter = False
        self.data = {}
        self.message_provider_container = VSplit([])
        self.working_container = VSplit([])
        self.main_container = DynamicContainer(lambda: self.working_container)
        self.main_container.on_page_enter = self.on_page_enter

    def on_page_enter(self) -> None:

        if not self.first_enter:
            self.get_message_configuration()
            self.schema = common_data.app.cli_object.get_schema_from_reference('', '#/components/schemas/MessageConfiguration')

            self.message_provider_type_widget = common_data.app.getTitledRadioButton(
                    _("Message Provider Type"),
                    name='messageProviderType',
                    values=[(mtype, mtype) for mtype in self.schema['properties']['messageProviderType']['enum']],
                    current_value='DISABLED',
                    jans_help="Message provider Type",
                    on_selection_changed=self.display_message_provider_type,
                    style=cli_style.radio_button)
            self.working_container = HSplit([
                            Frame(self.message_provider_type_widget),
                            DynamicContainer(lambda: self.message_provider_container)
                            ])

        self.first_enter = True

    def display_message_provider_type(self, rlwidget: RadioList):

        provider_type = rlwidget.current_value

        save_button = Button(text=_("Save"), handler=self.save)

        if provider_type == 'DISABLED':
            widgets = []
            self.provider_widgets = []
        else:
            styles = {'widget_style':cli_style.black_bg_widget, 'string': cli_style.edit_text, 'boolean': cli_style.check_box}
            schema_prop_name = f'{provider_type.lower()}Configuration'
            properties = self.schema['properties'][schema_prop_name]['properties']
            self.provider_widgets = self.get_widgets(
                properties=properties,
                values=self.data.get(schema_prop_name),
                styles=styles
                )
            widgets = [Frame(body=HSplit(self.provider_widgets),title=f"{provider_type} Configuration")]

        widgets.append(VSplit([save_button], align=HorizontalAlign.CENTER))
        self.message_provider_container = HSplit(widgets, width=D())

    def save(self):

        provider_type = self.message_provider_type_widget.me.current_value

        # save provider type

        async def provider_type_coroutine():
            cli_args = {'operation_id': 'patch-config-message', 'data':[{'op':'replace', 'path': 'messageProviderType', 'value': provider_type}]}
            common_data.app.start_progressing(_("Saving provider type"))
            response = await get_event_loop().run_in_executor(common_data.app.executor, common_data.app.cli_requests, cli_args)
            common_data.app.stop_progressing()
            if response.status_code not in (200, 201):
                common_data.app.show_message(common_strings.error, str(response.text), tobefocused=self.main_container)
            else:
                self.data = response.json()

        asyncio.ensure_future(provider_type_coroutine())

        # save prpvider data

        async def provider_coroutine(pcli_args):
            common_data.app.start_progressing(_("Saving message configuration"))
            response = await get_event_loop().run_in_executor(common_data.app.executor, common_data.app.cli_requests, pcli_args)
            common_data.app.stop_progressing()
            if response.status_code not in (200, 201):
                common_data.app.show_message(common_strings.error, str(response.text), tobefocused=self.main_container)
            else:
                self.data = response.json()

        if provider_type != 'DISABLED':
            data = {}
            for widget in self.provider_widgets:
                item_data = self.get_item_data(widget)
                data[item_data['key']] = item_data['value']

            cli_args = {'operation_id': 'put-config-message-' + provider_type.lower(), 'data': data}

            asyncio.ensure_future(provider_coroutine(cli_args))



    def get_message_configuration(self):
        async def coroutine():

            cli_args = {'operation_id': 'get-config-message'}
            common_data.app.start_progressing(_(f"Retreiving data for Messages"))
            response = await get_event_loop().run_in_executor(common_data.app.executor, common_data.app.cli_requests, cli_args)
            common_data.app.stop_progressing()

            if response.status_code not in (200, 201):
                common_data.app.show_message(common_strings.error, str(response.text), tobefocused=self.main_container)

            try:
                self.data = response.json()
            except Exception as e:
                common_data.app.show_message(common_strings.error, str(e), tobefocused=self.main_container)
                return

            self.message_provider_type_widget.me.current_value = self.data.get('messageProviderType', 'DISABLED')
            self.display_message_provider_type(self.message_provider_type_widget.me)

        asyncio.ensure_future(coroutine())

