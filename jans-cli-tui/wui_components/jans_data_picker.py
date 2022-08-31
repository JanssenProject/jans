
from prompt_toolkit.application.current import get_app
from prompt_toolkit.key_binding import KeyBindings
from prompt_toolkit.layout.containers import Float, HSplit, Window
from prompt_toolkit.layout.controls import FormattedTextControl
from prompt_toolkit.formatted_text import HTML, merge_formatted_text
from prompt_toolkit.layout.margins import ScrollbarMargin
from prompt_toolkit.key_binding.bindings.focus import focus_next
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.layout.containers import Float, FloatContainer, HSplit, Window, VSplit
from prompt_toolkit.widgets import Button, Label, TextArea
from prompt_toolkit.layout.containers import (
    ConditionalContainer,
    Float,
    HSplit,
    VSplit,
    VerticalAlign,
    HorizontalAlign,
    DynamicContainer,
    FloatContainer,
    Window
)

import calendar
import time
# ts = time.strptime(x[:19], "%Y-%m-%dT%H:%M:%S")
# time.strftime("%m/%d/%Y", ts)
# '11/27/2023'
#### not finished yet >> data
import datetime


class JansSelectDate:
   
    def __init__(self, date='',months=[],mytime=[]):  
        self.hours , self.minuts , self.seconds = mytime
        self.change_date = True
        self.date = date  #"11/27/2023"
        self.months = months 
        self.cord_y = 0
        self.cord_x = 0
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
            ],style="bg:#1e51fa",padding=1),
            #DynamicContainer(lambda: self.depug),

        Window(
            content=FormattedTextControl(
                text=self._get_calender_text,
            ),
            height=5,
            cursorline=False,
            # width=D(),  #15,
            style="bg:#D3D3D3",
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
            # width=D(),  #15,
            style="bg:#bab1b1",
            right_margins=[ScrollbarMargin(display_arrows=True),],
            wrap_lines=True
        ),
        ])


    def digitalize (self,number) :
        if len(str(number)) == 1 :
            return '0'+ str(number)
        else :
            return str(number)

    def _get_time_text(self):

        result = []
        time_line = []

        hours = self.digitalize(self.hours)
        minuts = self.digitalize(self.minuts)
        seconds = self.digitalize(self.seconds)

        hours_list = [hours,minuts,seconds]

        time_line.append(HTML('<b><blue>        H : M : S </blue></b>'))
        time_line.append("\n")

        for i, time_element in enumerate(hours_list):
            if i >= 2:
                space_, colon_ = 0, ''
            elif i == 0 :
                space_, colon_ = 7, ':'
            else:
                space_, colon_ = 0, ':'

            if i == self.cord_x and not self.change_date:
                time_line.append(HTML('{}<style fg="ansiwhite" bg="#777777"><blue>{}</blue>{}</style>'.format(' '*space_, self.adjust_sizes(time_element), colon_)))
            else :
                time_line.append(HTML('{}<b><blue>{}</blue>{}</b>'.format(' '*space_, self.adjust_sizes(time_element), colon_)))

            result= (time_line)


        return merge_formatted_text(result)

    def extract_date(self, date):  #"11/27/2023"
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
            except:
                day_index = 0
        week_index = self.entries.index(dum_week)
        self.cord_y = week_index 
        self.cord_x = day_index 
        self.selected_cord = (self.cord_y, self.cord_x)
 
    def adjust_sizes(self,day):
        if str(day) != '0':
            if len(str(day)) <=1:
                return '  '+str(day)
            else :
                return ' '+str(day)
        else:
            return '   '

    def _get_calender_text(self):
        result = []
        week_line = []
        for i, week in enumerate(self.entries): 
            for day in range(len(week)):
                if i == self.cord_y and day == self.cord_x and self.change_date:
                    week_line.append(HTML('<style fg="ansiwhite" bg="#777777"><black>{}</black></style>'.format(self.adjust_sizes(week[day]))))
                elif i == self.selected_cord[1] and day == self.selected_cord[0] and not self.change_date:
                    week_line.append(HTML('<black><b>{}</b></black>'.format(self.adjust_sizes(week[day]))))
                else:
                    week_line.append(HTML('<black>{}</black>'.format(self.adjust_sizes(week[day]))))

            result= (week_line)

            result.append("\n")

        return merge_formatted_text(result)


    def adjust_month(self, day, i):
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


    def inc_month(self,day):
        self.adjust_month(day, 1)

    def dec_month(self,day):
        self.adjust_month(day, -1)

    def adjust_year(self, day, i):
        self.current_year += i
        self.year_label =  Label(text=str(self.current_year),width=len(str(self.current_year)))
        current_date = str(day)+'/'+str(self.current_month) + '/'+str(self.current_year)
        self.extract_date(current_date)# 20/2/1997


    def inc_year(self, day):
        self.adjust_year(day, 1)

    def dec_year(self, day):
        self.adjust_year(day, -1)


    def adjust_time(self, i):
        if self.cord_x ==0 :
            self.hours +=i
        elif self.cord_x ==1 :
            self.minuts +=i
        elif self.cord_x ==2 :
            self.seconds +=i

        self.hours = abs(self.hours % 24)
        self.minuts = abs(self.minuts % 60)
        self.seconds = abs(self.seconds % 60)

    def up(self): 
        if self.change_date:
            if self.cord_y == 0 or int(self.entries[self.cord_y-1][self.cord_x]) == 0:
                self.dec_month(day=1)
            else:
                self.cord_y = (self.cord_y - 1)# % 5
                #self.depug=Label(text="entries = "+str(self.entries[self.cord_y][self.cord_x])+':',)
            self.selected_cord = (self.cord_x, self.cord_y)
        else:
            self.adjust_time(1)

    def down(self):
        if self.change_date:
            if self.cord_y == 4 or int(self.entries[self.cord_y+1][self.cord_x]) == 0:
                self.inc_month(day=28)
            else:
                self.cord_y = (self.cord_y + 1)# % 5
            #self.depug=Label(text="entries = "+str(self.entries[self.cord_y][self.cord_x])+':',)
            self.selected_cord = (self.cord_x, self.cord_y)
        else:
            self.adjust_time(-1)


    def right(self):
        if self.change_date:
            if self.cord_x == 6 or int(self.entries[self.cord_y][self.cord_x+1]) == 0:
                self.inc_year(day=7)
            else :
                self.cord_x = (self.cord_x + 1) #% 7
            self.selected_cord = (self.cord_x, self.cord_y)
        else:
            if self.cord_x >= 2 :
                pass
            else :
                self.cord_x = (self.cord_x + 1) #% 7

    def left(self):
        if self.change_date:
            if self.cord_x == 0 or int(self.entries[self.cord_y][self.cord_x-1]) == 0:
                self.dec_year(day=1)
            else:
                self.cord_x = (self.cord_x - 1)# % 7
                self.depug=Label(text="cord_y = "+str(self.cord_y)+':',)
            self.selected_cord = (self.cord_x, self.cord_y)
            self.date_changed = True
        else:
            if self.cord_x <=0 :
                pass
            else :
                self.cord_x = (self.cord_x - 1) #% 7
                #self.depug=Label(text="cord_y = "+str(self.cord_y)+':',)

    def next(self):
        self.change_date = not self.change_date
        if not self.change_date:
            self.cord_x = 0


    def __pt_container__(self):
        return self.container


class DateSelectWidget:
    def __init__(self, value):  # ex: value = "2023-11-27T14:05:35"
        self.months = [calendar.month_name[i] for i in range(1,13)]
        # text >> showed in the widget

        if  value:
            self.text = value
            ts = time.strptime(value[:19], "%Y-%m-%dT%H:%M:%S")     # "2023-11-27"
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
        if self.text != "Enter to Select":
            return self.text

    @value.setter
    def value(self, value):
        #passed_value = self.value  
        self._value = self.value  

    def make_time(self, text):
        ts = time.strptime(text[:19], "%Y-%m-%dT%H:%M:%S") # "2023-11-27"
        years =int(time.strftime("%Y",ts))
        months = int(time.strftime("%m",ts))
        days =  int(time.strftime("%d",ts))
        self.hours = int(time.strftime("%H",ts)) -7 ## it start from 0 to 23
        self.minuts =int(time.strftime("%M",ts))
        self.seconds = int(time.strftime("%S",ts)) 

        t = (years, months,days,self.hours,self.minuts,self.seconds,0,0,0)  ## the up increment
        t = time.mktime(t)
        self.text= (time.strftime("%Y-%m-%dT%H:%M:%S", time.gmtime(t)))

    def _get_text(self):
        if get_app().layout.current_window is self.window:
            return HTML('&gt; <style fg="ansired" bg="{}">{}</style> &lt;'.format('#00FF00', self.text))
        return '> {} <'.format(self.text)

    def _get_key_bindings(self):
        kb = KeyBindings()

        def _focus_next(event):
            focus_next(event)

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
                self.text= (time.strftime("%Y-%m-%dT%H:%M:%S", time.gmtime(t)))
                get_app().layout.container.floats.remove(self.select_box_float)

        @kb.add("up")
        def _up(event):
            if self.select_box_float not in get_app().layout.container.floats:
                self.make_time(self.text)
            else :
                self.select_box.up()

        @kb.add("down")
        def _down(event):
            if self.select_box_float not in get_app().layout.container.floats:
                self.make_time(self.text)
            else :
                self.select_box.down()

        @kb.add("right")
        def _right(event):
            if self.select_box_float not in get_app().layout.container.floats:
                self.make_time(self.text)
            else :
                self.select_box.right()
 
        @kb.add("left")
        def _left(event):
            if self.select_box_float not in get_app().layout.container.floats:
                self.make_time(self.text)
            else :
                self.select_box.left()

        @kb.add("+")
        def _plus(event):
            if self.select_box_float not in get_app().layout.container.floats:
                self.make_time(self.text)

        @kb.add("-")
        def _minus(event):
            if self.select_box_float not in get_app().layout.container.floats:
                self.make_time(self.text)

        @kb.add("tab")
        def _tab(event):
            if self.select_box_float in get_app().layout.container.floats:
                self.select_box.next()
            else :
                _focus_next(event)

        @kb.add("escape")
        @kb.add("pageup", eager=True)
        @kb.add("pagedown", eager=True)
        def _escape(event):
            if self.select_box_float in get_app().layout.container.floats:
                get_app().layout.container.floats.remove(self.select_box_float)
                _focus_next(event)

        return kb

    def __pt_container__(self):
        return self.window
