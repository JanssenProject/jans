from prompt_toolkit.eventloop import get_event_loop

from utils.utils import common_data
from utils.static import common_strings


async def get_attributes_coroutine(app) -> None:

    common_data.jans_attributes = []
    start_index = 1
    limit = 100

    while True:

        cli_args = {'operation_id': 'get-attributes', 'endpoint_args': f'limit:{limit},startIndex:{start_index}'}
        response = await app.loop.run_in_executor(app.executor, app.cli_requests, cli_args)

        if response.status_code == 200:
            try:
                data = response.json()
                if data.get('entriesCount'):
                    common_data.jans_attributes += data['entries']
                    app.logger.info("%d attributes retreived", data['entriesCount'])
                else:
                    break

                start_index += limit

            except Exception as e:
                app.logger.error("Failed tor retreive attributes %s", e)
                break
        else:
            break


async def retrieve_enabled_scripts() -> None:
    'Coroutine for retreiving enabled scripts'

    cli_args = {'operation_id': 'get-config-scripts', 'endpoint_args': 'fieldValuePair:enabled=true'}
    response = await common_data.app.loop.run_in_executor(common_data.app.executor, common_data.app.cli_requests, cli_args)

    if response.status_code not in (200, 201):
        common_data.app.show_message(_("Error getting scriots"), str(response.text), tobefocused=common_data.app.center_frame)
        return

    result = response.json()
    common_data.enabled_scripts = result['entries']
