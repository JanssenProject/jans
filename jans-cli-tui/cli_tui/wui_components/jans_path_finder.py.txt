#!/usr/bin/env python

from prompt_toolkit.application import Application
from prompt_toolkit.application.current import get_app
from prompt_toolkit.key_binding import KeyBindings
from prompt_toolkit.key_binding.bindings.focus import focus_next, focus_previous
from prompt_toolkit.layout import (
    Float,
    FloatContainer,
    HSplit,
    Layout,
)
from prompt_toolkit.application.current import get_app
from prompt_toolkit.key_binding import KeyBindings
from prompt_toolkit.layout.containers import Float, HSplit, Window
from prompt_toolkit.layout.controls import FormattedTextControl
from prompt_toolkit.formatted_text import HTML, merge_formatted_text
from prompt_toolkit.layout.margins import ScrollbarMargin
from prompt_toolkit.key_binding.bindings.focus import focus_next
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.layout.containers import (
    Float,
    HSplit,
    FloatContainer,
    Window,
    FormattedTextControl
)
from prompt_toolkit.mouse_events import MouseEvent, MouseEventType
from prompt_toolkit.data_structures import Point

from prompt_toolkit.application.current import get_app
from prompt_toolkit.key_binding import KeyBindings
from prompt_toolkit.layout.controls import FormattedTextControl
from prompt_toolkit.formatted_text import HTML, merge_formatted_text
from prompt_toolkit.layout.margins import ScrollbarMargin
from prompt_toolkit.key_binding.bindings.focus import focus_next, focus_previous
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.widgets import Button, Label
from prompt_toolkit.layout.containers import (
    Float,
    HSplit,
    VSplit,
    DynamicContainer,
    Window
)
from typing import Optional
from prompt_toolkit.formatted_text import AnyFormattedText
from prompt_toolkit.widgets import Button, Dialog
from prompt_toolkit.key_binding.key_bindings import KeyBindings, KeyBindingsBase

import glob

#-------------------------------------------------------------------------------#
#-------------------------------------------------------------------------------#
#-------------------------------------------------------------------------------#

class JansPathFinder:
    """_summary_
    """
   
    def __init__(
        self,
        )-> HSplit:


        self.cord_y = 0
        self.cord_x = 0
        self.old_cord_x = 0
        self.selected_cord = (0, 0)

        self.start_dir = 0
        self.start_file = 0
        self.flag_y =0
        self.list_dir = []
        self.select_flag = True

        self.current_dir = '/'
        self.current_file = ''
        self.list_dir.append(self.current_dir)
        # -----------------------------------------------------------------------------------------------# 
        # -----------------------------------------------------------------------------------------------# 
        # -----------------------------------------------------------------------------------------------# 

        self.current_dir_label =  Label(text=self.current_dir,width=len(self.current_dir))
        self.current_file_label =  Label(text=self.current_file,width=len(self.current_file))
        
        self.dire_container = Window(
            content=FormattedTextControl(
                text=self._get_directories,
            ),
            height=5,
            cursorline=False,
            style="fg:#ff0000 bg:#ffffff",  ### days window style
            right_margins=[ScrollbarMargin(display_arrows=True),],
            wrap_lines=True,
        )

        self.file_container = Window(
            content=FormattedTextControl(
                text=self._get_files,
                focusable=True,
            ),
            height=5,
            cursorline=False,
            style="fg:#0000ff bg:#ffffff",  ### time window style
            right_margins=[ScrollbarMargin(display_arrows=True),],
            wrap_lines=True
        )

        self.current_directories = self.get_direct()

        self.current_files = self.get_files()
        
        self.container =HSplit(children=[ 
            VSplit([
                Label(text="Dir: ",width=len("Dir: ")),
                Window(width=2,char=' '),
               DynamicContainer(lambda: self.current_dir_label),
            ],style="fg:#000000 bg:#ffffff",padding=1),  ### Month and year window style
            #DynamicContainer(lambda: self.depug),

        
        # -----------------------------------------------------------------------------------------------# 
        # ----------------------------------------- Directories -----------------------------------------# 
        # -----------------------------------------------------------------------------------------------# 
        DynamicContainer(lambda: self.dire_container),
        
        # -----------------------------------------------------------------------------------------------# 
        # ----------------------------------------- Files -----------------------------------------------# 
        # -----------------------------------------------------------------------------------------------# 
            VSplit([
                Label(text="Files: ",width=len("Files: ")),
                Window(width=2,char=' '),
               DynamicContainer(lambda: self.current_file_label),
            ],style="fg:#000000 bg:#ffffff",padding=1),  ### Month and year window style
        DynamicContainer(lambda: self.file_container),

        ])

    def get_direct (self)-> list:
        dir_list = []

        self.len_directories = len(glob.glob("/{}/*/".format(self.current_dir)))
        
        for dir in (glob.glob("/{}/*/".format(self.current_dir))): 
    
            dir_list.append('{}'.format(dir.replace('/{}/'.format(self.current_dir),'')).replace('//','/'))

        return dir_list


    def get_files(self)-> list:
        files_list = []

        self.len_files = len(glob.glob("/{}/*.*".format(self.current_dir)))
        
        for dir in (glob.glob("/{}/*.*".format(self.current_dir))): 
    
            files_list.append('{}'.format(dir.replace('/{}/'.format(self.current_dir),'')).replace('//','/'))

        return files_list


    def _get_files(self)-> AnyFormattedText: 
        result = []
        files_list = []

        for i, dir in enumerate(self.current_files[0+self.start_file:5+self.start_file]): 
            if i == self.cord_y and self.select_flag==True:
                files_list.append(HTML('<style bg="#777777">{}</style>'.format(dir.replace('/{}/'.format(self.current_dir),''))))
            else:
                files_list.append(HTML('{}'.format(dir.replace('/{}/'.format(self.current_dir),'')))) 

            result= (files_list)

            result.append("\n")

        return merge_formatted_text(result)

    def _get_directories(self)-> AnyFormattedText: 
        result = []
        dir_list = []

        for i, dir in enumerate(self.current_directories[0+self.start_dir:5+self.start_dir]): 
            if i == self.cord_y and self.select_flag == False:
                dir_list.append(HTML('<style bg="#777777">{}</style>'.format(dir.replace('/{}/'.format(self.current_dir),''))))
            else:
                dir_list.append(HTML('{}'.format(dir.replace('/{}/'.format(self.current_dir),'')))) 

            result= (dir_list)

            result.append("\n")

        return merge_formatted_text(result)


    def up(self)-> None: 
        
        if self.select_flag  == False:  ## Dir

            if self.cord_y <= 0  and self.flag_y >  0 :
                self.flag_y -=1
                self.start_dir -=1
                
            elif self.cord_y -1 >=0:
                self.cord_y -=1
                self.flag_y -=1
            self.current_dir = self.current_directories[0+self.start_dir:5+self.start_dir][self.cord_y]

        else: ## file   
            if self.cord_y <= 0  and self.flag_y >  0 :
                self.flag_y -=1
                self.start_file -=1
                
            elif self.cord_y -1 >=0:
                self.cord_y -=1
                self.flag_y -=1
                
            self.current_files = self.current_files[0+self.start_file:5+self.start_file]

    def down(self)-> None:
        if self.select_flag == False :    
            if self.cord_y +1 < 5 :
                self.cord_y +=1
                self.flag_y +=1

            elif self.flag_y  < len(self.current_directories) -1:

                self.flag_y +=1
                self.start_dir +=1

            self.current_dir = self.current_directories[0+self.start_dir:5+self.start_dir][self.cord_y]
        else:
            if self.cord_y +1 < 5 :
                self.cord_y +=1
                self.flag_y +=1

            elif self.flag_y  < len(self.current_files) -1:

                self.flag_y +=1
                self.start_file +=1
   
            self.current_files = self.current_files[0+self.start_file:5+self.start_file]           

    def enter(self)-> None:
        # self.current_dir = self.current_directories[0+self.start_dir:5+self.start_dir][self.cord_y]
        # self.current_dir_label =Label(text=self.current_dir,width=len(self.current_dir))

        ### current files
        if self.current_files:
            self.current_file = self.current_files[0+self.start_file:5+self.start_file][self.cord_y]
            self.current_file_label =  Label(text=self.current_file,width=len(self.current_file))
        else:
            self.current_file = 'No Files'
            self.current_file_label =  Label(text=self.current_file,width=len(self.current_file))


        self.start_dir = 0
        self.cord_y = 0


    def right(self)-> None:
        self.current_dir = self.current_directories[0+self.start_dir:5+self.start_dir][self.cord_y]
        self.current_dir_label =Label(text=self.current_dir,width=len(self.current_dir))

        ### current files
        if self.current_files:
            self.current_file = self.current_files[0+self.start_file:5+self.start_file][self.cord_y]
            self.current_file_label =  Label(text=self.current_file,width=len(self.current_file))
        else:
            self.current_file = 'No Files'
            self.current_file_label =  Label(text=self.current_file,width=len(self.current_file))


        self.start_dir = 0
        self.cord_y = 0
        self.flag_y = 0
        self.current_files = self.get_files()
        

        if len(glob.glob("/{}/*/".format(self.current_dir))) >= 1:
            self.list_dir.append(self.current_dir)
            self.current_directories = self.get_direct()

        else:
            pass


    def left(self)-> None:
        # self.current_dir2 = self.current_directories[0+self.start_dir:5+self.start_dir][self.cord_y]
        
        if len(self.list_dir) != 1:
            self.start_dir = 0
            self.cord_y = 0
            self.flag_y = 0

            
            self.list_dir.remove(self.list_dir[-1])
            self.current_dir = self.list_dir[-1]
            self.current_directories = self.get_direct()
            self.current_files = self.get_files()

            
            self.current_file = 'No Files'
            self.current_file_label =  Label(text=self.current_file,width=len(self.current_file))

            self.current_dir_label =Label(text=self.current_dir,width=len(self.current_dir))


    def next(self)-> None:
        self.select_flag = not self.select_flag 
        self.cord_y = 0
        self.flag_y = 0



    def __pt_container__(self)-> Dialog:
        return self.container


class PathFinderWidget:
    """This is a Dape Picker widget to select exact time and date
    """
    def __init__(
        self,
        value:str,
        ) -> Window:  

        if  value:
            self.text = value
        else:  
            self.text = "Enter to Browse Path"
            

        self.value  = str(value)

        self.dropdown = True
        self.window = Window(
            content=FormattedTextControl(
                text=self._get_text,
                focusable=True,
                 key_bindings=self._get_key_bindings(),
            ), height= 5) #D()) #5  ## large sized enties get >> (window too small)

        self.select_box = JansPathFinder()
        self.select_box_float = Float(content=self.select_box, xcursor=True, ycursor=True)

    @property
    def value(self):
        """Getter for the value property
        
        Returns:
            str: The selected value
        """
        if self.text != "Enter to Select":
            return self.text

    @value.setter
    def value(
        self, 
        value:str,
        )-> None:
        self._value = self.value


    def _get_text(self)-> AnyFormattedText:
        """To get The selected value

        Returns:
            str: The selected value
        """

        if get_app().layout.current_window is self.window:
            return HTML('&gt; <style fg="ansired" bg="{}">{}</style> &lt;'.format('#00FF00', self.text))
        return '> {} <'.format(self.text)

    def _get_key_bindings(self)-> KeyBindingsBase:
        """All key binding for the Dialog with Navigation bar

        Returns:
            KeyBindings: The method according to the binding key
        """

        kb = KeyBindings()

        def _focus_next(event):
            focus_next(event)

        def _focus_pre(event):
            focus_previous(event)

        @kb.add("enter")
        def _enter(event) -> None:

            if self.select_box_float not in get_app().layout.container.floats:
                get_app().layout.container.floats.append(self.select_box_float)
                self.select_box.enter()
            else:

                self.text= self.select_box.current_file


                get_app().layout.container.floats.remove(self.select_box_float)

        @kb.add("up")
        def _up(event):
            if self.select_box_float in get_app().layout.container.floats:
                self.select_box.up()

        @kb.add("down")
        def _down(event):
            if self.select_box_float in get_app().layout.container.floats:
                self.select_box.down()

        @kb.add("right")
        def _right(event):
            if self.select_box_float  in get_app().layout.container.floats:
                self.select_box.right()

        @kb.add("left")
        def _left(event):
            if self.select_box_float  in get_app().layout.container.floats:
                self.select_box.left()

        @kb.add("tab")
        def _tab(event):
            if self.select_box_float in get_app().layout.container.floats:
                self.select_box.next()
            else :
                _focus_next(event)

        @kb.add("s-tab")
        def _tab(event):
            if self.select_box_float in get_app().layout.container.floats:
                self.select_box.next()
            else :
                _focus_pre(event)


        @kb.add("escape")
        def _escape(event):
            if self.select_box_float in get_app().layout.container.floats:
                app = get_app()
                app.layout.container.floats.remove(self.select_box_float) 
        return kb

    def __pt_container__(self)-> Window:
        return self.window



