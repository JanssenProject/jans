
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

import calendar
import time
import datetime
import cli_style

date_time_temp = '%Y-%m-%dT%H:%M:%S'

class JansSelectDate:
    """_summary_
    """
   
    def __init__(
        self,
        date: Optional[str] = '',
        months: Optional[list] = None,
        mytime: Optional[list] = None,
        )-> HSplit:
        
        """_summary_

        Args:
            date (str, optional): _description_. Defaults to ''.
            months (list, optional): _description_. Defaults to [].
            mytime (list, optional): _description_. Defaults to [].
        """
        self.hours , self.minuts , self.seconds = mytime if mytime else [0,0,0]
        self.change_date = True
        self.date = date  #"11/27/2023"
        self.months = months if months else []
        self.cord_y = 0
        self.cord_x = 0
        self.old_cord_x = 0
        self.selected_cord = (0, 0)
        self.extract_date(self.date)

        #self.depug=Label(text="entries = "+str(self.entries[self.cord_y][self.cord_x])+':',)
        # -----------------------------------------------------------------------------------------------# 
        # ------------------------------------ handle month names ---------------------------------------# 
        # -----------------------------------------------------------------------------------------------# 
        if len(self.months[self.current_month-1] ) < 9 :   ### 9 == the tallest month letters
            mon_text = self.months[self.current_month-1] + ' '* (9-len(self.months[self.current_month-1] ))
        else :
            mon_text = self.months[self.current_month-1]
        # -----------------------------------------------------------------------------------------------# 
        # -----------------------------------------------------------------------------------------------# 
        # -----------------------------------------------------------------------------------------------# 

        self.month_label = Label(text=mon_text,width=len(mon_text))
        self.year_label =  Label(text=str(self.current_year),width=len(str(self.current_year)))


        self.container =HSplit(children=[ 
            VSplit([
                Button(text='<',left_symbol='',right_symbol='',width=1),
                DynamicContainer(lambda: self.month_label),
                Button(text='>',left_symbol='',right_symbol='',width=1),
                Window(width=2,char=' '),
                Button(text='<',left_symbol='',right_symbol='',width=1),
               DynamicContainer(lambda: self.year_label),
                Button(text='>',left_symbol='',right_symbol='',width=1  )
            ],style="class:date-picker-monthandyear",padding=1),  ### Month and year window style
            #DynamicContainer(lambda: self.depug),

        Window(
            content=FormattedTextControl(
                text=self._get_calender_text,
            ),
            height=5,
            cursorline=False,
            style="class:date-picker-day",  ### days window style
            right_margins=[ScrollbarMargin(display_arrows=True),],
            wrap_lines=True,
            
        ),
        Window(
            content=FormattedTextControl(
                text=self._get_time_text,
                focusable=True,
            ),
            height=2,
            cursorline=False,
            style="class:date-picker-time",  ### time window style
            right_margins=[ScrollbarMargin(display_arrows=True),],
            wrap_lines=True
        ),
        ])

    def digitalize (
        self,
        number:int
        )-> str: 
        if len(str(number)) == 1 :
            return '0'+ str(number)
        else :
            return str(number)

    def _get_time_text(self)-> AnyFormattedText: 

        result = []
        time_line = []

        hours = self.digitalize(self.hours)
        minuts = self.digitalize(self.minuts)
        seconds = self.digitalize(self.seconds)

        hours_list = [hours,minuts,seconds]

        time_line.append(HTML('<b><{}>        H : M : S </{}></b>'.format(cli_style.date_picker_TimeTitle,cli_style.date_picker_TimeTitle)))
        time_line.append("\n")

        for i, time_element in enumerate(hours_list):
            if i >= 2:
                space_, colon_ = 0, ''
            elif i == 0 :
                space_, colon_ = 7, ':'
            else:
                space_, colon_ = 0, ':'

            if i == self.cord_x and not self.change_date:
                time_line.append(HTML('{}<style bg="#777777"><{}>{}</{}>{}</style>'.format(' '*space_,cli_style.date_picker_TimeSelected, self.adjust_sizes(time_element),cli_style.date_picker_TimeSelected, colon_)))
            else :
                time_line.append(HTML('{}<b><{}>{}</{}>{}</b>'.format(' '*space_, cli_style.date_picker_Time ,self.adjust_sizes(time_element), cli_style.date_picker_Time, colon_)))

            result= (time_line)


        return merge_formatted_text(result)

    def extract_date(
        self, 
        date:str
        )-> None:  #"11/27/2023"
        ### this function is for the init date >> passed date from `data`
        
        day = int(date.split('/')[1])
        self.current_month = int(date.split('/')[0])
        self.current_year = int(date.split('/')[-1])

        month_week_list = calendar.monthcalendar(int(self.current_year), int(self.current_month))

        self.entries = month_week_list 

        dum_week = []
        for week in self.entries:
            dum_week = week
            try :
                day_index = week.index(day)
                break
            except Exception:
                day_index = 0
        week_index = self.entries.index(dum_week)
        self.cord_y = week_index 
        self.cord_x = day_index 
        self.selected_cord = (self.cord_x, self.cord_y)
 
    def adjust_sizes(
        self,
        day:str
        )-> str:
        if str(day) != '0':
            if len(str(day)) <=1:
                return '  '+str(day)
            else :
                return ' '+str(day)
        else:
            return '   '

    def _get_calender_text(self)-> AnyFormattedText: 
        result = []
        week_line = []
        for i, week in enumerate(self.entries): 
            for day in range(len(week)):
                if i == self.cord_y and day == self.cord_x and self.change_date:
                    week_line.append(HTML('<style bg="#777777"><{}>{}</{}></style>'.format(cli_style.date_picker_calenderSelected,self.adjust_sizes(week[day]),cli_style.date_picker_calenderSelected)))
                elif i == self.selected_cord[1] and day == self.selected_cord[0] and not self.change_date:
                    week_line.append(HTML('<{}><b>{}</b></{}>'.format(cli_style.date_picker_calender_prevSelected, self.adjust_sizes(week[day]), cli_style.date_picker_calender_prevSelected)))
                else:
                    week_line.append(HTML('<{}>{}</{}>'.format(cli_style.date_picker_calenderNSelected, self.adjust_sizes(week[day]), cli_style.date_picker_calenderNSelected)))

            result= (week_line)

            result.append("\n")

        return merge_formatted_text(result)

    def adjust_month(
        self, 
        day:int, 
        i:int,
        )-> None:
        if self.change_date:
            if i == 1 and self.current_month == 12:
                return
            if i == -1 and self.current_month == 1:
                return

            self.current_month += i
            if len(self.months[self.current_month-1] ) < 9 :
                mon_text = self.months[self.current_month-1] + ' '* (9-len(self.months[self.current_month-1] ))
            else :
                mon_text = self.months[self.current_month-1]
            self.month_label = Label(text=mon_text,width=len(mon_text))
            current_date = str(self.current_month)+'/'+str(day) + '/'+str(self.current_year)
            self.extract_date(current_date)

    def inc_month(
        self,
        day:int,
        )-> None:
        self.adjust_month(day, 1)

    def dec_month(
        self,
        day:int,
        )-> None:
        self.adjust_month(day, -1)

    def adjust_year(
        self, 
        day:int, 
        i:int,
        )-> None:
        self.current_year += i
        self.year_label =  Label(text=str(self.current_year),width=len(str(self.current_year)))
        current_date = str(day)+'/'+str(self.current_month) + '/'+str(self.current_year)
        self.extract_date(current_date)# 20/2/1997

    def inc_year(
        self, 
        day:int,
        )-> None:
        self.adjust_year(day, 1)

    def dec_year(
        self, 
        day:int
        )-> None:
        self.adjust_year(day, -1)

    def adjust_time(
        self, 
        i:int,
        )-> None:
        if self.cord_x ==0 :
            self.hours +=i
        elif self.cord_x ==1 :
            self.minuts +=i
        elif self.cord_x ==2 :
            self.seconds +=i

        self.hours = abs(self.hours % 24)
        self.minuts = abs(self.minuts % 60)
        self.seconds = abs(self.seconds % 60)

    def up(self)-> None: 
        if self.change_date:
            if self.cord_y == 0 or int(self.entries[self.cord_y-1][self.cord_x]) == 0:
                self.dec_month(day=1)
            else:
                self.cord_y = (self.cord_y - 1)# % 5
            self.selected_cord = (self.cord_x, self.cord_y)
        else:
            self.adjust_time(1)

    def down(self)-> None:
        if self.change_date:
            if self.cord_y == 4 or int(self.entries[self.cord_y+1][self.cord_x]) == 0:
                self.inc_month(day=28)
            else:
                self.cord_y = (self.cord_y + 1)# % 5
            self.selected_cord = (self.cord_x, self.cord_y)
        else:
            self.adjust_time(-1)

    def right(self)-> None:
        if self.change_date:
            if self.cord_x == 6 or int(self.entries[self.cord_y][self.cord_x+1]) == 0:
                self.inc_year(day=7)
            else :
                self.cord_x = (self.cord_x + 1) #% 7
            self.selected_cord = (self.cord_x, self.cord_y)
        else:
            if not self.cord_x >= 2 :
                self.cord_x = (self.cord_x + 1) #% 7

    def left(self)-> None:
        if self.change_date:
            if self.cord_x == 0 or int(self.entries[self.cord_y][self.cord_x-1]) == 0:
                self.dec_year(day=1)
            else:
                self.cord_x = (self.cord_x - 1)# % 7
                self.depug=Label(text="cord_y = "+str(self.cord_y)+':',)
            self.selected_cord = (self.cord_x, self.cord_y)
            self.date_changed = True
        else:
            if not self.cord_x <=0 :
                self.cord_x = (self.cord_x - 1) #% 7

    def next(self)-> None:
        self.change_date = not self.change_date
         
        if not self.change_date:
            self.old_cord_x = self.cord_x 
            self.cord_x = 0
        else:
            self.cord_x = self.old_cord_x


    def __pt_container__(self)-> Dialog:
        return self.container

class DateSelectWidget:
    """This is a Dape Picker widget to select exact time and date
    """
    def __init__(
        self,
        parent,
        value:str,
        ) -> Window:  
        # ex: value = "2023-11-27T14:05:35"
        """init for DateSelectWidget

        Args:
            parent (widget): This is the parent widget for the dialog, to access `Pageup` and `Pagedown`
            value (str): string time stamp value like "2023-11-27T14:05:35"
        """
        self.parent = parent
        self.months = [calendar.month_name[i] for i in range(1,13)]

        if  value:
            self.text = value
            ts = time.strptime(value[:19], date_time_temp)     # "2023-11-27"
            self.date = time.strftime("%m/%d/%Y", ts)               # "11/27/2023"
            self.hours = int(time.strftime("%H",ts)) 
            self.minuts =int(time.strftime("%M",ts))
            self.seconds = int(time.strftime("%S",ts)) 
        else:  
            today = datetime.date.today()
            self.date =  str(today.month) +'/' +str(today.day) +'/'+str(today.year)    ## '11/27/2023' ## 
            self.text = "Enter to Select"
            now = datetime.datetime.now()
            self.hours = int(now.strftime("%H"))
            self.minuts =int(now.strftime("%M"))
            self.seconds = int(now.strftime("%S"))

        self.value  = str(value)

        self.dropdown = True
        self.window = Window(
            content=FormattedTextControl(
                text=self._get_text,
                focusable=True,
                 key_bindings=self._get_key_bindings(),
            ), height= 10) #D()) #5  ## large sized enties get >> (window too small)

        self.select_box = JansSelectDate(date=self.date,months=self.months,mytime=[self.hours,self.minuts,self.seconds] )
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

    def make_time(
        self, 
        text:str,
        )-> None:
        """extract time from the text to increase or decrease

        Args:
            text (str): the text that appear on the wigdet
        """
        ts = time.strptime(text[:19], date_time_temp) # "2023-11-27"
        years =int(time.strftime("%Y",ts))
        months = int(time.strftime("%m",ts))
        days =  int(time.strftime("%d",ts))
        self.hours = int(time.strftime("%H",ts)) -7 ## it start from 0 to 23
        self.minuts =int(time.strftime("%M",ts))
        self.seconds = int(time.strftime("%S",ts)) 

        t = (years, months,days,self.hours,self.minuts,self.seconds,0,0,0)  ## the up increment
        t = time.mktime(t)
        self.text= (time.strftime(date_time_temp, time.gmtime(t)))

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
                get_app().layout.container.floats.append(   self.select_box_float)
            else:
                years = int(self.select_box.current_year)
                months =int(self.select_box.current_month)
                days = int(self.select_box.entries[self.select_box.cord_y][self.select_box.cord_x] )

                t = (years, months,days,(self.select_box.hours-8),self.select_box.minuts,self.select_box.seconds,0,0,0)  ## the up increment
                t = time.mktime(t)  
                self.text= (time.strftime(date_time_temp, time.gmtime(t)))
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


        @kb.add("pageup", eager=True)
        def _pageup(event):
            if self.select_box_float in get_app().layout.container.floats:
                _escape(event)
                self.parent.dialog.navbar.go_up()
            else :
                app = get_app()
                self.parent.dialog.navbar.go_up()
                app.layout.focus(self.parent.dialog.navbar)

        @kb.add("pagedown", eager=True)
        def _pagedown(event):
            if self.select_box_float in get_app().layout.container.floats:
                _escape(event)
                self.parent.dialog.navbar.go_down()
            else :
                app = get_app()
                self.parent.dialog.navbar.go_down()
                app.layout.focus(self.parent.dialog.navbar)

        return kb

    def __pt_container__(self)-> Window:
        return self.window
