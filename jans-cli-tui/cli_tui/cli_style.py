from prompt_toolkit.styles import Style
from types import SimpleNamespace

style = Style.from_dict(
    {
        "window.border": "#888888",
        "shadow": "bg:#222222",
        "menu-bar": "bg:#aaaaaa #888888",
        "menu-bar.selected-item": "bg:#ffffff #000000",
        "menu": "bg:#888888 #ffffff",
        "menu.border": "#aaaaaa",
        "window.border shadow": "#444444",
        "focused  button": "bg:#880000 #ffffff noinherit",
        # Styling for Dialog widgets.
        "button-bar": "bg:#4D4D4D",
        "textarea-readonly": "bg:#ffffff fg:#4D4D4D",
        "required-field": "#8b000a",
        "textarea":"bg:#ffffff fg:#0000ff",
        "status": "bg:ansigray fg:black",
        "progress": "bg:ansigray fg:ansired",
        "select-box cursor-line": "nounderline bg:ansired fg:ansiwhite",
        "checkbox":"nounderline bg:#ffffff fg:#d1c0c0 #ff0000",
        ### Jans_cli_tui
        ### main
        "jans-main-navbar":"fg:ansired bg:green",
        "jans-main-verificationuri":"",
        "jans-main-verificationuri.text":"",
        "jans-main-usercredintial":"",
        "jans-main-usercredintial.titletext":"",
        "jans-main-datadisplay":"",
        "jans-main-datadisplay.text":"",

        ### Styling for Scripts plugin
        ### main
        "script_maincontainer":"bg:#908F90",
        "script-sidenav":"red",  
        "script-mainarea":"yellow",  
        "script-navbar-bgcolor":"#2600ff",
        "script-checkbox":"green",
        "script-titledtext":"green",
        "script-label":"blue",
        ### Styling for oauth plugin
        ## main
        "outh_maincontainer":"",  
        "outh_containers_scopes":"",  
        "outh_containers_scopes.text":"green",
        "outh_containers_clients":"",  
        "outh_containers_clients.text":"green",

        "sub-navbar": "fg:Silver bg:MidnightBlue",

        "outh-navbar":"fg:#f92672 bg:#4D4D4D",
        "outh-verticalnav-headcolor":'green',
        "outh-verticalnav-entriescolor":'white',

        "outh-waitclientdata":'',
        "outh-waitclientdata.label":'',
        "outh-waitscopedata":'' , 
        "outh-waitscopedata.label":'',

        "outh-titledtext":"green",
        "outh-label":"blue",
        
        # PLUGINS
        "plugin-navbar":"#2600ff",
        "plugin-navbar-headcolor":"green",
        "plugin-navbar-entriescolor":"blue",
        "plugin-tabs":"",
        "plugin-text":"green",
        "plugin-textsearch":"",
        "plugin-label":"bold",
        "plugin-textrequired":"#8b000a",
        "plugin-checkbox":"green",
        "plugin-checkboxlist":"green",
        "plugin-radiobutton":"green",
        "plugin-dropdown":"green",
        "plugin-widget":"green",
        "plugin-container":"",
        "plugin-container.text":"green",
        "plugin-black-bg": "bg: black",

        ## edit_client_dialog
        "outh-client-navbar":"#2600ff",
        "outh-client-navbar-headcolor":"green",
        "outh-client-navbar-entriescolor":"blue",
        "outh-client-tabs":"",
        "outh-client-text":"green",
        "outh-client-textsearch":"",
        "outh-client-label":"bold",
        "outh-client-textrequired":"#8b000a",
        "outh-client-checkbox":"green",
        "outh-client-checkboxlist":"green",
        "outh-client-radiobutton":"green",
        "outh-client-dropdown":"green",
        "outh-client-widget":"green",

        ## edit_scope_dialog
        "outh-scope-navbar":"#2600ff",
        "outh-scope-navbar-headcolor":"green",
        "outh-scope-navbar-entriescolor":"blue",
        "outh-scope-tabs":"",
        "outh-scope-text":"green",
        "outh-scope-textsearch":"fg:green",
        "outh-scope-label":"bold",
        "outh-scope-textrequired":"#8b000a",
        "outh-scope-checkbox":"green",
        "outh-scope-checkboxlist":"green",
        "outh-scope-radiobutton":"green",
        "outh-scope-dropdown":"green",
        "outh-scope-widget":"green",

        ## view-uma_dialog
        "outh-uma-navbar":"fg:#4D4D4D bg:#ffffff",
        "outh-uma-tabs":"",
        "outh-uma-text":"green",
        "outh-uma-textsearch":"fg:green",
        "outh-uma-label":"green bold",
        "outh-uma-textrequired":"#8b000a",
        "outh-uma-checkbox":"green",
        "outh-uma-checkboxlist":"green",
        "outh-uma-radiobutton":"green",
        "outh-uma-dropdown":"green",
        "outh-uma-widget":"green",

        "script-label":"fg:green",


        ### WUI Componenets
        ## jans_data_picker
        "date-picker-monthandyear":"bg:#1e51fa",
        "date-picker-day":"bg:#D3D3D3",
        "date-picker-time":"bg:#bab1b1",
        "dialog-titled-widget":"bg:#ffffff fg:green",

        ####tab
        "tab-nav-background": "fg:#b0e0e6 bg:#a9a9a9",
        "tab-unselected": "fg:#b0e0e6 bg:#a9a9a9 underline",
        "tab-selected": "fg:#000080 bg:#d3d3d3",
        
        ##scim
        "scim-widget": "bg:black fg:white",

    }
)


def get_color_for_style(style_name:str)->SimpleNamespace:
    ret_val = SimpleNamespace()
    ret_val.fg = '#000000'
    ret_val.bg = '#ffffff'

    for pstyle in style.class_names_and_attrs:
        if pstyle[0].__contains__(style_name):
            if pstyle[1].color:
                ret_val.fg = '#'+pstyle[1].color
            if pstyle[1].bgcolor:
                ret_val.bg = '#'+pstyle[1].bgcolor

    return ret_val

## jans nav bar
main_navbar_bgcolor = "DimGray"
outh_navbar_bgcolor = "#ADD8E6"
sub_navbar_selected_bgcolor = "Navy"
sub_navbar_selected_fgcolor = "OldLace"
shorcut_color= 'OrangeRed'


### WUI Componenets
## jans_data_picker
date_picker_TimeTitle = "yellow"  ## only color >> HTML '<blue></blue>'
date_picker_Time = "green"       ## only color
date_picker_TimeSelected = "black"

date_picker_calender_prevSelected = "red" #>black >> defult bold
date_picker_calenderNSelected = "blue"#>black
date_picker_calenderSelected = "red"

## jans_drop_down
drop_down_hover = '#00FF00'
drop_down_itemSelect = '#ADD8E6'
