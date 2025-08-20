import calendar
import datetime

from typing import Optional
from enum import Enum

from prompt_toolkit.application import Application
from prompt_toolkit.key_binding import KeyBindings
from prompt_toolkit.key_binding.bindings.focus import focus_next, focus_previous
from prompt_toolkit.layout.controls import FormattedTextControl
from prompt_toolkit.formatted_text import AnyFormattedText, HTML, merge_formatted_text
from prompt_toolkit.layout.margins import ScrollbarMargin
from prompt_toolkit.widgets import Label
from prompt_toolkit.layout.containers import Float, HSplit, VSplit, DynamicContainer, Window

from utils.static import cli_style, ISOFORMAT
from utils.multi_lang import _


class change_loc(Enum):
    DAY     = 0
    HOURS   = 1
    MINUTES = 2
    SECONDS = 3
    MONTH   = 4
    YEAR    = 5

    def next(self):
        return self.__class__((self.value + 1) % 6)


class JansSelectDate:

    def __init__(
        self,
        value: datetime.datetime,
        min_date: Optional[datetime.datetime]=None,
        date_only: Optional[bool]=False,
        )-> HSplit:
        
        """Date selection float

        Args:
            date (datetime.datetime): datetime object for current date
            min_date (optional): datetime.datetime object or collable
        """
        self.value = value
        self.chstate = change_loc.DAY
        self.cal = calendar.Calendar()
        self.min_date = min_date
        self.date_only = date_only

        child_widgets = [
            Window(
                content=FormattedTextControl(text=self._get_header_text),
                height=1,
                cursorline=False,
                style="class:date-picker-monthandyear",
            ),
            Label(' Mon Tue Wed Thu Fri Sat Sun', style='class:date-picker-weekdays-header'),
            Window(
                content=FormattedTextControl(text=self._get_calender_text),
                height=6,
                cursorline=False,
                style="class:date-picker-day",
                right_margins=[ScrollbarMargin(display_arrows=True)],
                wrap_lines=True,
            ),
            ]

        if not self.date_only:
            child_widgets.append(
                Window(
                    content=FormattedTextControl(text=self._get_time_text, focusable=True),
                    height=1,
                    cursorline=False,
                    style="class:date-picker-time",
                    right_margins=[ScrollbarMargin(display_arrows=True)],
                    wrap_lines=True
                )
            )

        self.container = HSplit(children=child_widgets)

    def _get_header_text(self)-> AnyFormattedText: 
        headers = (self.value.strftime('%B').center(12), str(self.value.year).center(6))
        headers_list = []

        for i, header in enumerate(headers, 4):
            if self.chstate.value == i:
                headers_list.append(f'&lt; <style fg="ansired"><b>{header}</b></style> &gt;')
            else:
                headers_list.append(f'&lt; {header} &gt;')

        return HTML('   '.join(headers_list))

    def _get_time_text(self)-> AnyFormattedText: 
        time_list = (f'{self.value.hour:02}', f'{self.value.minute:02}', f'{self.value.second:02}')
        text_list = []

        for i, timee in enumerate(time_list, 1):
            if self.chstate.value == i:
                text_list.append('<style fg="ansired"><b>' + timee + '</b></style>')
            else:
                text_list.append(timee)

        return HTML(f'Time (HH:MM:SS) <style fg="ansiblue">{text_list[0]}: {text_list[1]}: {text_list[2]}</style>')


    def _get_calender_text(self)-> AnyFormattedText: 
        result = []
        for date in self.cal.itermonthdates(self.value.year, self.value.month):
            day_text = f'{date.day:4d}'
            fg = 'ansiblue'

            if date.month != self.value.month:
                fg = 'ansigray'
            elif date == self.value.date():
                if self.chstate == change_loc.DAY:
                    fg = 'ansired'
                day_text = '<b>' + day_text + '</b>'

            result.append(HTML(f'<style fg="{fg}">{day_text}</style>'))
            if date.weekday() == 6:
                result.append("\n")

        return merge_formatted_text(result)

    def set_value(self, new_value):
        check = self.min_date() if callable(self.min_date) else self.min_date
        if self.min_date and new_value < check:
            return
        self.value = new_value


    def _add_months(self, months):
        month = self.value.month - 1 + months
        year = self.value.year + month // 12
        month = month % 12 + 1
        day = min(self.value.day, calendar.monthrange(year, month)[1])
        new_value = datetime.datetime(year, month, day, self.value.hour, self.value.minute, self.value.second)
        self.set_value(new_value)


    def up(self)-> None:
        if self.chstate == change_loc.DAY:
            self.set_value(self.value - datetime.timedelta(days=7))
        elif self.chstate == change_loc.MONTH:
            self._add_months(1)
        elif self.chstate == change_loc.YEAR:
            self.set_value(self.value.replace(year=self.value.year+1))
        else:
            self.set_value(self.value - datetime.timedelta(**{self.chstate.name.lower(): 1}))


    def down(self)-> None:
        if self.chstate == change_loc.DAY:
            self.value += datetime.timedelta(days=7)
        elif self.chstate == change_loc.MONTH:
            self._add_months(-1)
        elif self.chstate == change_loc.YEAR:
            new_value = self.value.replace(year=self.value.year-1)
            self.set_value(new_value)
        else:
            self.value += datetime.timedelta(**{self.chstate.name.lower(): 1})

    def right(self)-> None:
        if self.chstate == change_loc.DAY:
            self.value += datetime.timedelta(days=1)

    def left(self)-> None:
        if self.chstate == change_loc.DAY:
            new_value = self.value - datetime.timedelta(days=1)
            self.set_value(new_value)

    def tab(self) -> None:
        self.chstate = self.chstate.next()


    def __pt_container__(self)-> HSplit:
        return self.container

class DateSelectWidget:

    def __init__(
        self,
        app: Application,
        value: Optional[datetime.datetime]=None,
        min_date: Optional[datetime.datetime]=None,
        date_only: Optional[bool]=False,
    ) -> Window:

        """Date Pickler widget to select exact time and date

        Args:
            value (datetime): datetime object
            min_date (optional): datetime object
        """
        self.value = value
        self.app = app
        self.date_only = date_only
        self.window = Window(
            content=FormattedTextControl(
                text=self._get_text,
                focusable=True,
                key_bindings=self._get_key_bindings(),
            ),
            height= 10
            )

        self.select_box = JansSelectDate(self.value or datetime.datetime.now(), min_date=min_date, date_only=self.date_only)
        self.select_box_float = Float(content=self.select_box, xcursor=True, ycursor=True)


    def _get_text(self)-> AnyFormattedText:
        if self.value:
            text = self.value.strftime(ISOFORMAT)
            if self.date_only:
                text = text[:10]
        else:
            text = _("Enter to Select")
        return HTML(f'&gt; {text} &lt;')

    def _get_key_bindings(self)-> KeyBindings:
        """All key binding for the Dialog with Navigation bar

        Returns:
            KeyBindings: The method according to the binding key
        """

        kb = KeyBindings()

        def select_box_events(event_funct: str):
            if self.select_box_float in self.app.layout.container.floats:
                getattr(self.select_box, event_funct)()

        @kb.add("enter")
        def _enter(event) -> None:
            if self.select_box_float not in self.app.layout.container.floats:
                self.app.layout.container.floats.append(self.select_box_float)
            else:
                self.value = self.select_box.value
                self.app.layout.container.floats.remove(self.select_box_float)

        @kb.add('up')
        def _up(event):
            select_box_events('up')

        @kb.add('down')
        def _down(event):
            select_box_events('down')

        @kb.add('right')
        def _right(event):
            select_box_events('right')

        @kb.add('left')
        def _left(event):
            select_box_events('left')

        @kb.add('delete')
        def _delete(event):
            self.value = None

        @kb.add('tab')
        def _tab(event):
            if self.select_box_float in self.app.layout.container.floats:
                select_box_events('tab')
            else:
                focus_next(event)

        @kb.add('s-tab')
        def _stab(event):
            _escape(event)
            focus_previous(event)

        @kb.add("escape")
        def _escape(event):
            if self.select_box_float in self.app.layout.container.floats:
                self.app.layout.container.floats.remove(self.select_box_float) 

        return kb

    def __pt_container__(self)-> Window:
        return self.window
