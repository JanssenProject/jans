from enum import Enum

class DialogResult(Enum):
    CANCEL = 0
    ACCEPT = 1
    OK = 2

class CLI_STYLE:
    edit_text = 'class:plugin-text'
    edit_text_required = 'class:plugin-textrequired'
    check_box = 'class:plugin-checkbox'
    radio_button = 'class:plugin-radiobutton'
    
