from enum import Enum

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
    black_bg = 'class:plugin-black-bg'
    textarea = 'class:textarea'
    drop_down = 'class:plugin-dropdown'

class common_strings:
    enter_to_search = "Press enter to perform search"
    no_matching_result = "No matching result"
    error = "Error!"
    info = "Info"
    oops = "Oops"
