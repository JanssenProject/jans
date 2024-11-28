from utils.multi_lang import _
from enum import Enum

ISOFORMAT = '%Y-%m-%dT%H:%M:%S'

class DialogResult(Enum):
    CANCEL = 0
    ACCEPT = 1
    OK = 2

class cli_style:
    titled_text = 'class:plugin-titledtext'
    edit_text = 'class:plugin-text'
    edit_text_required = 'class:plugin-textrequired'
    check_box = 'class:plugin-checkbox'
    check_box_list = 'class:plugin-checkboxlist'
    radio_button = 'class:plugin-radiobutton'
    tabs = 'class:plugin-tabs'
    label = 'class:plugin-label'
    container = 'class:plugin-container'
    navbar_headcolor = 'class:plugin-navbar-headcolor'
    navbar_entriescolor = 'class:plugin-navbar-entriescolor'
    tab_selected = 'class:tab-selected'
    black_bg_widget = 'class:black-bg-widget'
    white_bg_widget = 'class:white-bg-widget'
    black_bg = 'class:plugin-black-bg'
    textarea = 'class:textarea'
    drop_down = 'class:plugin-dropdown'
    sub_navbar = 'class:sub-navbar'
    read_only = 'class:textarea-readonly'

class common_strings:
    enter_to_search = "Press enter to perform search"
    no_matching_result = "No matching result"
    error = "Error!"
    info = "Info"
    oops = "Oops"
    success = "Success"
    warning = "Warning!"
    confirm = "Confirmation"

    help_enter      = f'<Enter>          {_("Confirm or Edit current selection")}'
    help_esc        = f'<Esc>            {_("Close the current dialog")}'
    help_alt_letter = f'<Alt + letter>   {_("Navigate to an other tab")}'
    help_v          = f'<v>              {_("View current item in JSON format if possible")}'
    help_delete     = f'<d> <Delete>     {_("Delete current item project if possible")}'
    help_link_str   = f'{_("For More Visit")} https://docs.jans.io/head/admin/config-guide/config-tools/jans-tui/'
