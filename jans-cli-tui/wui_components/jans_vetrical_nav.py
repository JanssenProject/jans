from prompt_toolkit.layout.containers import HSplit, Window, FloatContainer
from prompt_toolkit.layout.controls import FormattedTextControl
from prompt_toolkit.layout.margins import ScrollbarMargin
from prompt_toolkit.formatted_text import merge_formatted_text
from prompt_toolkit.key_binding import KeyBindings
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.widgets import HorizontalLine

class JansVerticalNav():
    """This is a Vertical Navigation bar Widget with many values used in <Get clients>/<Get scopes>
    """


    def __init__(self, myparent, headers, selectes,  on_display, on_enter=None, on_delete=None,
                all_data=None, preferred_size=[], data=None, headerColor='green', entriesColor='white', underline_headings=True):
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
            underline_headings (str, optional): Put a line under headings
        
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

        self.data = data                    # ListBox Data (Can be renderable ?!!! #TODO )

        self.preferred_size = preferred_size
        self.headerColor = headerColor
        self.entriesColor = entriesColor

        self.on_enter = on_enter
        self.on_delete = on_delete
        self.on_display = on_display
        self.all_data=all_data
        self.underline_headings = underline_headings
        self.handle_header_spaces()
        self.create_window()


    def create_window(self):
        """This method creat the dialog it self
        """
        self.container_content = [
                        Window(
                            content=FormattedTextControl(
                                text=self._get_head_text,
                                focusable=False,
                                key_bindings=self._get_key_bindings(),
                                style=self.headerColor,
                            ),
                            style='class:select-box',
                            height=D(preferred=1, max=1),
                            cursorline=False,
                        ),
                        Window(
                            content=FormattedTextControl(
                                text=self._get_formatted_text,
                                focusable=True,
                                key_bindings=self._get_key_bindings(),
                                style=self.entriesColor,
                            ),
                            style='class:select-box',
                            height=D(preferred=len(self.data), max=len(self.data)),
                            cursorline=True,
                            right_margins=[ScrollbarMargin(display_arrows=True), ],
                        ),
                    ]

        if self.underline_headings:
            self.container_content.insert(1, HorizontalLine())

        self.container = FloatContainer(
            content=HSplit(self.container_content+[Window(height=1)]),
            floats=[
            ],
        )

    def handle_header_spaces(self):
        """Make header evenlly spaced
        """
        self.spaces = []
        data_length_list = []
        for row in self.data:
            column_length_list = []
            for col in row:
                column_length_list.append(len(col))
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

    def get_spaced_data(self):
        """Make entries evenlly spaced
        """
        spaced_data = []
        for d in self.data:
            spaced_line_list = []
            for i, space in enumerate(self.spaces):
                spaced_line_list.append(d[i] + ' ' * (space - len(d[i])))
            spaced_data.append(spaced_line_list)

        return spaced_data

    def _get_head_text(self):
        """Get all headers entries

        Returns:
            merge_formatted_text: Merge (Concatenate) several pieces of formatted text together. 
        """

        result = []
        y = ''
        for k in range(len(self.headers)):
            y += self.headers[k] + ' ' * \
                (self.spaces[k] - len(self.headers[k]) + 5)
        result.append(y)

        return merge_formatted_text(result)

    def _get_formatted_text(self):
        """Get all selective entries

        Returns:
            merge_formatted_text: Merge (Concatenate) several pieces of formatted text together. 
        """

        result = []
        spaced_data = self.get_spaced_data()
        for i, entry in enumerate(spaced_data): ## entry = ['1800.6c5faa', 'Jans Config Api Client', 'authorization_code,refresh_...', 'Reference]
            if i == self.selectes:
                result.append([('[SetCursorPosition]', '')])
            
            result.append('     '.join(entry))
            result.append('\n')

        return merge_formatted_text(result)


    def remove_item(self, item):
        self.data.remove(item)
        self.handle_header_spaces()

    def add_item(self, item):
        self.data.append(item)
        self.handle_header_spaces()
        self.container_content[-1].height = len(self.data)

    def _get_key_bindings(self):
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

        @kb.add('down')
        def _go_up(event) -> None:
            if not self.data:
                return
            self.selectes = (self.selectes + 1) % len(self.data)

        @kb.add('enter')
        def _(event):
            if not self.data:
                return
            passed = [i.strip() for i in self.data[self.selectes]]
            size = self.myparent.output.get_size()
            if self.on_enter :
                self.on_enter(passed=passed,event=event,size=size,data=self.all_data[self.selectes])


        @kb.add('d')
        def _(event):
            if not self.data:
                return
            selected_line = [i.strip() for i in self.data[self.selectes]]
            size = self.myparent.output.get_size()
            self.on_display(
                selected=selected_line, 
                headers=self.headers, 
                event=event,
                size=size, 
                data=self.all_data[self.selectes])


        @kb.add('delete')
        def _(event):
            if self.data and self.on_delete:
                selected_line = [i.strip() for i in self.data[self.selectes]]
                self.on_delete(selected=selected_line, event=event)

        return kb

    def __pt_container__(self):
        return self.container
