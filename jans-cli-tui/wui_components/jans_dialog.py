from prompt_toolkit.widgets import (
    Button,
    Dialog,
)
from prompt_toolkit.layout.containers import HSplit


class JansDialog():
    """NOt Used
    """
    def __init__(self,only_view=False,height=None,width=None,title=None, button_functions=[], entries_list=[], entries_color='#00ff44'):
        self.entries_list = entries_list
        self.button_functions = button_functions
        self.entries_color = entries_color
        self.title = title
        self.height = height
        self.width =width
        self.only_view=only_view
        self.create_window()

    def create_window(self):
        ### get max title len
        max_title_str = self.entries_list[0][1]   # list is not empty
        for x in self.entries_list:
            if len(x[1]) > len(max_title_str):
                max_title_str = x[1]

        max_data_str = 41 ## TODO TO BE Dynamic
        self.dialog = Dialog(
            title=str(self.title),
            body=HSplit(
                [
                    self.entries_list[i][0]for i in range(len(self.entries_list))
                ],height=self.height,
                width=  self.width
            ),
            buttons=[
                Button(
                    text=str(self.button_functions[k][1]),
                    handler=self.button_functions[k][0],
                ) for k in range(len(self.button_functions))
            ],
            with_background=False,
        ) 
#--------------------------------------------------------------------------------------#
    def __pt_container__(self):
        return self.dialog

