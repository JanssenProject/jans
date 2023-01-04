#!/usr/bin/env python3
"""
"""
import importlib  
import sys
sys.path.append("./cli_tui/")
jans_main = importlib.import_module("jans-cli-tui") 
 
from cli_tui.wui_components.jans_drop_down import DropDownWidget 
from cli_tui.wui_components.jans_date_picker import DateSelectWidget 
from prompt_toolkit import prompt


        

#---------------------------------------------------------------------------#
#------------------------  Test Custom Widget ------------------------------#
#---------------------------------------------------------------------------#
    
def _wid_to_text_wid(wid) -> str:
    return wid.text 
#---------------------------------------------------------------------------#
#------------------------  test getTitledText ------------------------------#
#---------------------------------------------------------------------------#
def test_getTitledText():
    title, value, height, read_only, width = prompt('input')
    wid = jans_main.JansCliApp().getTitledText(
                title=title,
                name='spontaneousScopes',
                height=height,
                value=value,
                read_only=read_only,
                width=width
                )
    return wid.get_children()[0].content.text() + wid.get_children()[1].content.buffer.text == title + ': '+value   ## dont remove this space

#---------------------------------------------------------------------------#
#---------------------- test getTitledCheckBoxList -------------------------#
#---------------------------------------------------------------------------#
def test_getTitledCheckBoxList():
    title, current_values, values = prompt('input')              
    wid = jans_main.JansCliApp().getTitledCheckBoxList(
                                title=title, 
                                name=title, 
                                values=values,
                                current_values=current_values, 
                                ),                 
    selected= []
    x = wid[0].get_children()[1].content.text()
    final=[]
    for i in x:
        if 'class:checkbox ' in i[0] or '*' in i[1]:                        ## dont remove this space
            final.append(i)

    for k in range(len(final)):
        if '*' in final[k][1] :
            if 'class:checkbox ' in final[k+1][0]:                          ## dont remove this space
                selected.append(final[k+1][1])

    check_title = (wid[0].get_children()[0].content.text() == title +': ')  ## dont remove this space
    check_values = set(selected).issubset(current_values)

    if check_title and check_values :
        return True
    else:
        return False

#---------------------------------------------------------------------------#
#---------------------- test getTitledCheckBox -----------------------------#
#---------------------------------------------------------------------------#
def test_getTitledCheckBox():
    title, checked = prompt('input')
    wid = jans_main.JansCliApp().getTitledCheckBox(
                                title=title,
                                name=title,
                                checked=checked
                                ),   

    x = wid[0].get_children()[1].content.text()
    wid_checked=False
    for i in x:
        if  '*' in i[1]:
            checked=True
            break

    check_title = (wid[0].get_children()[0].content.text() == title +': ')                                 
    check_values = checked == wid_checked

    if check_title and check_values :
        return True
    else:
        return False

#---------------------------------------------------------------------------#
#---------------------- test getTitledRadioButton --------------------------#
#---------------------------------------------------------------------------#
def test_getTitledRadioButton():
    title, values, current_value = prompt('input')
    wid = jans_main.JansCliApp().getTitledRadioButton(
                                title=title,
                                name=title, 
                                values=values,
                                current_value=current_value,
                                ),   
    selected= []
    x = wid[0].get_children()[1].content.text()
    final=[]
    for i in x:
        if 'class:radio ' in i[0] or '*' in i[1]:                           ## dont remove this space
            final.append(i)

    for k in range(len(final)):
        if '*' in final[k][1] :
            if 'class:radio ' in final[k+1][0]:                             ## dont remove this space
                selected.append(final[k+1][1])

    check_title = (wid[0].get_children()[0].content.text() == title +': ')  ## dont remove this space
    check_values = current_value ==  selected[0]

    if check_title and check_values :
        return True
    else:
        return False

#---------------------------------------------------------------------------#
#------------------------ test drop_down -----------------------------------#
#---------------------------------------------------------------------------#
def test_drop_down():
    value, values = prompt('input')
    wid = DropDownWidget(value=value,values=values)
    if [item for item in values if value in item]:
        return _wid_to_text_wid(wid) == value
    else:
        return _wid_to_text_wid(wid) == 'Select One'

#---------------------------------------------------------------------------#
#------------------------ test date_picker ---------------------------------#
#---------------------------------------------------------------------------#
def test_date_picker():
    value, parent = prompt('input')
    wid = DateSelectWidget(value=value,parent=parent)
    print(_wid_to_text_wid(wid))
    return _wid_to_text_wid(wid) == value

#---------------------------------------------------------------------------#
#---------------------------------------------------------------------------#
#---------------------------------------------------------------------------#
