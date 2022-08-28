
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
   
    def __init__(self, date='',months=[]):  

        if date != '':
            self.date = date  #"11/27/2023"
        else:
            today = datetime.date.today()
            self.date =  str(today.month) +'/' +str(today.day) +'/'+str(today.year)    ## '11/27/2023' ## 
        
        self.months = months 
    
        self.cord_y = 0
        self.cord_x = 0
        
        self.extract_date(self.date)

        #self.depug=Label(text="entries = "+str(self.entries[self.cord_y][self.cord_x])+':',)
        self.month_label = Label(text=self.months[self.current_month-1],width=len(self.months[self.current_month-1]))
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
            ],style="bg:#4D4D4D",padding=1),
            #DynamicContainer(lambda: self.depug),
            
            Window(
            content=FormattedTextControl(
                text=self._get_formatted_text,
                focusable=True,
            ),
            height=5,
            cursorline=False,
            # width=D(),  #15,
            style="bg:#4D4D4D",
            right_margins=[ScrollbarMargin(display_arrows=True),],
            wrap_lines=True
        ),                  
        ])

    # def set_value(self, value):
    #     self.value = self.date

    def extract_date(self,date):  #"11/27/2023"
        ### this function is for the init date >> passed date from `data`
        
        day = int(date.split('/')[1])
        self.current_month = int(date.split('/')[0])
        self.current_year = int(date.split('/')[-1])

        month_week_list = calendar.monthcalendar(int(self.current_year),int(self.current_month))

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
 
    def adjust_sizes(self,day):
        if str(day) != '0':
            if len(str(day)) <=1:
                return '  '+str(day)
            else :
                return ' '+str(day)
        else:
            return '   '

    def _get_formatted_text(self):
        result = []
        week_line = []
        for i, week in enumerate(self.entries): 
            for day in range(len(week))  :
                if i == self.cord_y:
                    if day == self.cord_x:
                        week_line.append(HTML('<style fg="ansired" bg="#ADD8E6">{}</style>'.format(self.adjust_sizes(week[day]))))
                        
                        # result.append(HTML('<style fg="ansired" bg="#ADD8E6">{}</style>'.format( day)))
                    else :
                        week_line.append(HTML('<b>{}</b>'.format(self.adjust_sizes(week[day]))))
                else:
                    week_line.append(HTML('<b>{}</b>'.format(self.adjust_sizes(week[day]))))
            
            result= (week_line)

            result.append("\n")

        return merge_formatted_text(result)

    def inc_month(self,day):
        if self.current_month != 12:
            self.current_month+=1
            self.month_label = Label(text=self.months[self.current_month-1],width=len(self.months[self.current_month-1]))
            current_date = str(self.current_month)+'/'+str(day) + '/'+str(self.current_year)
            self.extract_date(current_date)

    def dec_month(self,day):
        if self.current_month > 1:
            self.current_month-=1
            self.month_label = Label(text=self.months[self.current_month-1],width=len(self.months[self.current_month-1]))
            current_date = str(self.current_month)+'/'+str(day) + '/'+str(self.current_year)
            self.extract_date(current_date)

    def inc_year(self,day):
        
        self.current_year+=1
        self.year_label =  Label(text=str(self.current_year),width=len(str(self.current_year)))
        current_date = str(day)+'/'+str(self.current_month) + '/'+str(self.current_year)
        self.extract_date(current_date)# 20/2/1997        

    def dec_year(self,day):
        
        self.current_year-=1
        self.year_label =  Label(text=str(self.current_year),width=len(str(self.current_year)))
        current_date = str(day)+'/'+str(self.current_month) + '/'+str(self.current_year)
        self.extract_date(current_date)# 20/2/1997

    def up(self): 
        if self.cord_y == 0 or int(self.entries[self.cord_y-1][self.cord_x]) == 0:
            self.dec_month(day=1)
        else:
            self.cord_y = (self.cord_y - 1)# % 5
            #self.depug=Label(text="entries = "+str(self.entries[self.cord_y][self.cord_x])+':',)

    def down(self):
        
        if self.cord_y == 4 or int(self.entries[self.cord_y+1][self.cord_x]) == 0:
            self.inc_month(day=28)
        else:
            self.cord_y = (self.cord_y + 1)# % 5
            #self.depug=Label(text="entries = "+str(self.entries[self.cord_y][self.cord_x])+':',)

    def right(self):
        if self.cord_x == 6 or int(self.entries[self.cord_y][self.cord_x+1]) == 0:
            self.inc_year(day=7)
        else :
            self.cord_x = (self.cord_x + 1) #% 7
            #self.depug=Label(text="entries = "+str(self.entries[self.cord_y][self.cord_x])+':',)

    def left(self):
        if self.cord_x == 0 or int(self.entries[self.cord_y][self.cord_x-1]) == 0:
            self.dec_year(day=1)
        else:
            self.cord_x = (self.cord_x - 1)# % 7
            #self.depug=Label(text="entries = "+str(self.entries[self.cord_y][self.cord_x])+':',)


    def __pt_container__(self):
        return self.container


class DateSelectWidget:  
    def __init__(self, value):  # ex: value = "2023-11-27T14:05:35"
        self.months = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December']
        self.text = value       # text >> showed in the widget           #[:19]+ "T23:59:59"

        if  value:
            ts = time.strptime(value[:19], "%Y-%m-%dT%H:%M:%S") # "2023-11-27"
            self.date = time.strftime("%m/%d/%Y", ts)           # "11/27/2023"
        else:  
            ### maybe here we can select today date!!
            self.date=''
            self.text = "Enter to Select"

        self.value  = str(value)   
    
        self.dropdown = True
        self.window = Window(
            content=FormattedTextControl(
                text=self._get_text,
                focusable=True,
                 key_bindings=self._get_key_bindings(),
            ), height= 10) #D()) #5  ## large sized enties get >> (window too small)

        self.select_box = JansSelectDate(date=self.date,months=self.months )
        self.select_box_float = Float(content=self.select_box, xcursor=True, ycursor=True)

    @property
    def value(self):
        return self.text

    @value.setter
    def value(self, value):
        passed_value = self.value  
    
        self._value = passed_value
    
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
                self.text = str(self.select_box.current_year) +'-'+str(self.select_box.current_month-1) +'-'+str(self.select_box.entries[self.select_box.cord_y][self.select_box.cord_x] )+'T23:59:59'
                get_app().layout.container.floats.remove(self.select_box_float)

        @kb.add("up")
        def _up(event):
            self.select_box.up()

        @kb.add("down")
        def _up(event):
            self.select_box.down()

        @kb.add("right")
        def _up(event):
            self.select_box.right()

        @kb.add("left")
        def _up(event):
            self.select_box.left()

        @kb.add("w")
        def _up(event):
            self.select_box.inc_year()

        @kb.add("s")
        def _up(event):
            self.select_box.dec_year()

        @kb.add("a")
        def _up(event):
            self.select_box.inc_month()

        @kb.add("d")
        def _up(event):
            self.select_box.dec_month()


        @kb.add("escape")
        @kb.add("tab")
        def _(event):
            if self.select_box_float in get_app().layout.container.floats:
                get_app().layout.container.floats.remove(self.select_box_float)

            _focus_next(event)

        return kb

    def __pt_container__(self):
        return self.window
