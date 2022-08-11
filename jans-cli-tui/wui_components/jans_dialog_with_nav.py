from prompt_toolkit.layout.containers import (
    HSplit,
    VSplit,
    Window,
)
from prompt_toolkit.widgets import (
    Button,
    Dialog,
)

class JansDialogWithNav():
    def __init__(self,content,height=None,width=None,title=None, button_functions=[], navbar=None):
        self.navbar = navbar
        self.button_functions = button_functions
        self.title = title
        self.height = height
        self.width =width
        self.content = content
        self.create_window()

    def create_window(self):

        max_data_str = 30 ## TODO TO BE Dynamic


        self.dialog = Dialog(
            title=self.title,
            body=VSplit([
                 HSplit(
                            [
                                self.navbar
                            ],

                            width= (max_data_str )  #self.width 
                        ),
                         Window(width=1, char="|",),
                        HSplit([
                            self.content
                        

                        ]),
                        
                    

            ],width=120,height=32),
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

