from typing import Tuple, TypeVar, Callable, Optional, Sequence, Union


from prompt_toolkit.layout.containers import HSplit, Window, FloatContainer, ScrollOffsets
from prompt_toolkit.layout.controls import FormattedTextControl
from prompt_toolkit.layout.margins import ScrollbarMargin
from prompt_toolkit.formatted_text import merge_formatted_text
from prompt_toolkit.key_binding import KeyBindings
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.widgets import HorizontalLine
from prompt_toolkit.widgets.base import Border
from prompt_toolkit.layout.dimension import AnyDimension
from prompt_toolkit.formatted_text import AnyFormattedText
from prompt_toolkit.key_binding.key_bindings import KeyBindings, KeyBindingsBase

class JansVerticalNav():
    """This is a Vertical Navigation bar Widget with many values used in <Get clients>/<Get scopes>
    """
    def __init__(
        self,
        myparent,
        headers: list,
        on_display: Callable= None, 
        selectes: Optional[int]= 0, 
        on_enter: Callable= None,
        get_help: Tuple= None,
        on_delete: Callable= None,
        change_password: Callable= None,
        all_data: Optional[list]= [], 
        preferred_size: Optional[list]= [], 
        data: Optional[list]= [], 
        headerColor: Optional[str]= "green",
        entriesColor: Optional[str]= "white",
        underline_headings: Optional[bool]= True, 
        max_width: AnyDimension = None,
        jans_name: Optional[str]= '', 
        max_height: AnyDimension = None,
        jans_help: Optional[str]= '',
        hide_headers: Optional[bool]= False,
        )->FloatContainer :
        """init for JansVerticalNav

        Args:
            parent (widget): This is the parent widget for the dialog, to caluclate the size
            headers (List): List of all navigation headers names
            selectes (int): The first value to be selected.
            on_enter (Method): Method to be called when '<Enter>' key is pressed
            on_display (Method): Method to be called when '<d>' key is pressed
            on_delete (Method, optional): Method to be called when '<Delete>' key is pressed. Defaults to None.
            all_data (List, optional): All Data to be used with `on_enter` and `on_display`. Defaults to None.
            preferred_size (list, optional): List of the max desired width for the columns contents. Defaults to [].
            data (List, optional): Data to be displayed. Defaults to None.
            headerColor (str, optional): Color for the Headers. Defaults to 'green'.
            entriesColor (str, optional): Color for the Entries. Defaults to 'white'.
            underline_headings (str, optional): Put a line under headings.
            max_width (int, optional): Maximum width of container.
            jans_name (str, optional): Widget name
            max_height (int, optional): Maximum hegight of container
            jans_help (str, optional): Status bar help message
            hide_headers (bool, optional): Hide or display headers
        Examples:
            clients = JansVerticalNav(
                myparent=self,
                headers=['Client ID', 'Client Name', 'Grant Types', 'Subject Type'],
                preferred_size= [0,0,30,0],
                data=data,
                on_enter=self.edit_client_dialog,
                on_display=self.data_display_dialog,
                on_delete=self.delete_client,
                # selection_changed=self.data_selection_changed,
                selectes=0,
                headerColor='green',
                entriesColor='white',
                all_data=result
            )
        Todo:
            * Needs refactor
        """
        self.myparent = myparent            # ListBox parent class
        self.headers = headers              # ListBox headers
        self.selectes = selectes            # ListBox initial selection
        self.max_width = max_width
        self.data = data                    # ListBox Data (Can be renderable ?!!! #TODO )
        self.jans_name = jans_name
        self.preferred_size = preferred_size
        self.headerColor = headerColor
        self.entriesColor = entriesColor
        self.max_height = max_height
        self.jans_help = jans_help
        self.on_enter = on_enter
        self.on_delete = on_delete
        self.on_display = on_display
        self.change_password = change_password
        self.hide_headers = hide_headers
        self.spaces = [len(header)+1 for header in self.headers]

        if get_help:
            self.get_help, self.scheme = get_help
            if self.data :
                self.get_help(data=self.data[self.selectes], scheme=self.scheme)
        else:
            self.get_help= None

        self.all_data=all_data
        self.underline_headings = underline_headings

        self.handle_header_spaces()
        self.create_window()


    def view_data(
        self,
        data:list
        ) -> list:
        result = []
        for i, entry in enumerate(data):
            mod_entry = []
            for col in range(len(entry)) :
                if self.preferred_size[col] == 0:
                    mod_entry.append(entry[col])
                else :
                    if self.preferred_size[col] >= len(str(entry[col])):
                        mod_entry.append(entry[col])
                    else :
                        mod_entry.append(entry[col][:self.preferred_size[col]]+'...')

            result.append(mod_entry)
 
        return result

    def create_window(self) -> None:
        """This method creat the dialog it self
        """

        self.list_box = Window(
                            content=FormattedTextControl(
                                text=self._get_formatted_text,
                                focusable=True,
                                key_bindings=self._get_key_bindings(),
                                style=self.entriesColor,
                            ),
                            style='class:select-box',
                            height=D(preferred=len(self.data), max=len(self.data)),
                            cursorline=True,
                            always_hide_cursor=True,
                            scroll_offsets=ScrollOffsets(top=1, bottom=1),
                            right_margins=[ScrollbarMargin(display_arrows=True)],
                        )
        if self.jans_help:
            self.list_box.jans_help = self.jans_help

        headers_height = 2 if self.underline_headings else 1

        self.container_content = [
                        Window(
                            content=FormattedTextControl(
                                text=self._get_head_text,
                                focusable=False,
                                key_bindings=self._get_key_bindings(),
                                style=self.headerColor,
                            ),
                            style='class:select-box',
                            height=D(preferred=headers_height, max=headers_height),
                            cursorline=False,
                        ),
                        self.list_box,
                    ]

        self.container = FloatContainer(
            content=HSplit(self.container_content+[Window(height=1)], width=D(max=self.max_width)),
            floats=[],
        )

    def handle_header_spaces(self) -> None:
        """Make header evenlly spaced
        """
        if not self.data:
            return
        data = self.view_data(self.data)
        self.spaces = []
        data_length_list = []
        for row in data:
            column_length_list = []
            for col in row:
                column_length_list.append(len(str(col)))
            data_length_list.append(column_length_list)


        if data_length_list:
            tmp_dict = {}
            for num in range(len(data_length_list[0])):
                tmp_dict[num] = []

            for k in range(len(data_length_list)):
                for i in range(len(data_length_list[k])):
                    tmp_dict[i].append(data_length_list[k][i])

            for i in tmp_dict:
                self.spaces.append(max(tmp_dict[i]))

            for i, space in enumerate(self.spaces):
                if space < len(self.headers[i]):
                    self.spaces[i] = space + (len(self.headers[i]) - space)
        else:
            self.spaces = [len(header) + 2 for header in self.headers]
            
        self.spaces[-1] =  self.myparent.output.get_size()[1] - sum(self.spaces) + sum(len(s) for s in self.headers)    ## handle last head spaces (add space to the end of ter. width to remove the white line)

    def get_spaced_data(self) -> list:
        """Make entries evenlly spaced
        """
        data = self.view_data(self.data)

        spaced_data = []
        for d in data:
            spaced_line_list = []
            for i, space in enumerate(self.spaces):
                spaced_line_list.append(str(d[i]) + ' ' * (space - len(str(d[i]))))
            spaced_data.append(spaced_line_list)

        return spaced_data

    def _get_head_text(self) -> AnyFormattedText:
        if self.hide_headers:
            return ''

        """Get all headers entries

        Returns:
            merge_formatted_text: Merge (Concatenate) several pieces of formatted text together. 
        """

        result = []
        y = ''
        for k in range(len(self.headers)):
            y += self.headers[k] + ' ' * \
                (self.spaces[k] - len(self.headers[k]) + 3)

        result.append(y)

        if self.underline_headings:
            result.append('\n' + Border.HORIZONTAL*len(y))

        return merge_formatted_text(result)

    def _get_formatted_text(self) -> AnyFormattedText:
        """Get all selective entries

        Returns:
            merge_formatted_text: Merge (Concatenate) several pieces of formatted text together. 
        """

        result = []
        spaced_data = self.get_spaced_data()
        for i, entry in enumerate(spaced_data):
            if i == self.selectes:
                result.append([('[SetCursorPosition]', '')])

            result.append('   '.join(entry))
            result.append('\n')

        return merge_formatted_text(result)


    def remove_item(
        self, 
        item: list,
        ) -> None:
        self.data.remove(item)
        self.handle_header_spaces()
        if self.max_height:
            self.container_content[-1].height = self.max_height if self.max_height else len(self.data)

    def add_item(
        self, 
        item: list,
        ) -> None:
        self.data.append(item)
        self.handle_header_spaces()
        self.container_content[-1].height = self.max_height if self.max_height else len(self.data)

    def replace_item(
        self, 
        item_index: int,
        item: list,
        ) -> None:
        self.data[item_index] = item
        self.handle_header_spaces()

    def clear(self) -> None:
        self.data = []
        self.container_content[-1].height = self.max_height


    def _get_key_bindings(self) -> KeyBindingsBase:
        """All key binding for the Dialog with Navigation bar

        Returns:
            KeyBindings: The method according to the binding key
        """
        kb = KeyBindings()

        @kb.add('up')
        def _go_up(event) -> None:
            if not self.data:
                return
            self.selectes = (self.selectes - 1) % len(self.data)

            if self.get_help :
                self.get_help(data=self.data[self.selectes],scheme=self.scheme)

        @kb.add('down')
        def _go_up(event) -> None:
            if not self.data:
                return
            self.selectes = (self.selectes + 1) % len(self.data)
            if self.get_help :
                self.get_help(data=self.data[self.selectes],scheme=self.scheme)

        @kb.add('enter')
        def _(event):
            if not self.data:
                return
            size = self.myparent.output.get_size()
            if self.on_enter :
                self.on_enter(passed=self.data[self.selectes], event=event, size=size, data=self.all_data[self.selectes], selected=self.selectes, jans_name=self.jans_name)

        @kb.add('p')
        def _(event):
            if not self.data:
                return
            if self.change_password:
                self.change_password(data=self.all_data[self.selectes])


        @kb.add('d')
        def _(event):
            if not self.data:
                return

            size = self.myparent.output.get_size()
            self.on_display(
                selected=self.data[self.selectes], 
                headers=self.headers, 
                event=event,
                size=size, 
                data=self.all_data[self.selectes])


        @kb.add('delete')
        def _(event):
            if self.data and self.on_delete:
                selected_line = self.data[self.selectes]
                self.on_delete(selected=selected_line, selected_idx=self.selectes, event=event, jans_name=self.jans_name)

        return kb

    def __pt_container__(self) ->FloatContainer:
        return self.container
