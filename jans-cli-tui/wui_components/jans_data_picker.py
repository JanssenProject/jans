
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

#### not finished yet

class JansSelectDate:
    def __init__(self, entries=[]):
        self.entries = entries
        self.selected_line = 0
        self.months = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December']
        self.current_month = 0
        self.current_year = 2022


        self.month_text = Label(text=self.months[self.current_month],width=8)

        self.container =HSplit(children=[ 
            VSplit([
                Button(text='<',left_symbol='',right_symbol='',width=1),
                Label(text=self.months[self.current_month],width=len(self.months[self.current_month])),
                Button(text='>',left_symbol='',right_symbol='',width=1),
                Window(width=2,char=' '),
                Button(text='<',left_symbol='',right_symbol='',width=1),
                Label(text=str(self.current_year),width=len(str(self.current_year))),
                Button(text='>',left_symbol='',right_symbol='',width=1  )
            ],style="bg:#4D4D4D",padding=1),



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


    def _get_formatted_text(self):

        calendar.monthcalendar(2022, self.months.index('February')+1)
        result = []
        cur_month = self.months.index('February')+1

        # for cur_month in range(len(months)):
            # if i == calendar.monthcalendar(2022, months.index(current_month)+1):
        month_week_list = calendar.monthcalendar(2022, self.months.index(self.months[cur_month])+1)

        for k in range(len(month_week_list))  :
            dum_list =[]
            # not_selectrion =month_week_list[k][-1].count(0) 
            for week in month_week_list[k]:
                if len(str(week)) ==1 :
                    dum_list.append(' '+str(week))
                else :
                    dum_list.append(str(week))
            month_week_list[k] = dum_list
            result.append(HTML('<style fg="ansired" bg="{}">{} \n</style>'.format('#ADD8E6','  '.join(month_week_list[k]))))
        # else:
        #     result.append(HTML('<b>{}</b>'.format(entry[1])))
        result.append("\n")



        return merge_formatted_text(result)

    def inc_month(self):
        self.current_month+=1


    def dec_month(self):
        self.current_month-=1

    def inc_year(self):
        self.current_year+=1
        

    def dec_year(self):
        self.current_year-=1


    def up(self):
        self.selected_line = (self.selected_line - 1) % len(self.entries)


    def down(self):
        self.selected_line = (self.selected_line + 1) % len(self.entries)

    def __pt_container__(self):
        return self.container


class DateSelectWidget:
    def __init__(self):
        self.entries = [('Date','Date')]
        if self.entries:  ## should be replaced with the selected from data.
            self.text = self.entries[0][1]
        else:
            self.text = "Enter to Select"


        self.dropdown = True
        self.window = Window(
            content=FormattedTextControl(
                text=self._get_text,
                focusable=True,
                 key_bindings=self._get_key_bindings(),
            ), height= 10) #D()) #5  ## large sized enties get >> (window too small)

        self.select_box = JansSelectDate(self.entries)
        self.select_box_float = Float(content=self.select_box, xcursor=True, ycursor=True)


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
                get_app().layout.container.floats.insert(0, self.select_box_float)
            else:
                self.text = self.select_box.entries[self.select_box.selected_line][1]
                get_app().layout.container.floats.remove(self.select_box_float)

        @kb.add("up")
        def _up(event):
            self.select_box.up()

        @kb.add("down")
        def _up(event):
            self.select_box.down()

        @kb.add("escape")
        @kb.add("tab")
        def _(event):
            if self.select_box_float in get_app().layout.container.floats:
                get_app().layout.container.floats.remove(self.select_box_float)

            _focus_next(event)

        return kb

    def __pt_container__(self):
        return self.window
