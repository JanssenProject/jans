import os
import sys
from prompt_toolkit.layout.containers import (
    HSplit,
    VSplit,
    DynamicContainer,
    Window
)

from prompt_toolkit.layout.containers import HSplit
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.widgets import Button, Label, Frame

from wui_components.jans_side_nav_bar import JansSideNavBar

from multi_lang import _


# class Plugin():
#     """This is a general class for plugins 
#     """
#     def __init__(self, app):
#         """init for Plugin class "scripts"

#         Args:
#             app (_type_): _description_
#         """
#         self.app = app
#         self.pid = 'scripts'
#         self.name = 'Scripts'

#     def process(self):
#         pass

#     def set_center_frame(self):
#         """center frame content
#         """
#         self.app.center_container = Frame(
#                             body=HSplit([Label(text="Plugin {} is not imlemented yet".format(self.name)), Button(text="Button-{}".format(self.pid))], width=D()),
#                             height=D())




class Plugin():
    """This is a general class for plugins 
    """
    def __init__(self, app):
        """init for Plugin class "scripts"

        Args:
            app (_type_): _description_
        """
        self.app = app
        self.pid = 'scripts'
        self.name = 'Scripts'

        self.data = {}
        self.script_containers = {}
        self.script_prepare_navbar()
        self.script_prepare_containers()
        self.script_nav_selection_changed(self.script_navbar.navbar_entries[0][0])

    def process(self):
        pass

    def set_center_frame(self):
        """center frame content
        """
        self.app.center_container = self.script_main_container



    def script_prepare_navbar(self):
        """prepare the navbar for the current Plugin 
        """
        self.script_navbar = JansSideNavBar(
                    entries=['Person Authentication', 'Consent Gathering',
                     'Post Authentication', 'id_token', 'Password Grant',
                      'CIBA End User Notification', 'OpenID Configuration', 
                      'Dynamic Scope', 'Spontaneous Scope', 'Application Session',
                       'End Session', 'Client Registration', 'Introspection', 
                       'Update Token'],
                    selection_changed=self.script_nav_selection_changed,
                    select=0,

                    # bgcolor='#66d9ef'
                    )


    def script_nav_selection_changed(self, selection):
        """This method for the selection change

        Args:
            selection (str): the current selected tab
        """
        self.app.logger.debug('OXUATH NAV: %s', selection)
        if selection in self.script_containers:
            self.script_main_area = self.script_containers[selection]
        else:
            self.script_main_area = self.not_implemented


    def oauth_get_scopes(self):
        pass
    def oauth_get_clients(self):
        pass
    def search_clients(self):
        pass
    def add_client(self):
        pass
    def script_prepare_containers(self):
        """prepare the main container (tabs) for the current Plugin 
        """

        self.script_data_container = {
            'clients' :HSplit([],width=D()),
            'scopes' :HSplit([],width=D()),

        } 
        self.script_main_area = HSplit([],width=D())

        self.not_implemented =  Frame(
                            body=HSplit([Label(text=_("Not imlemented yet")), Button(text=_("MyButton"))], width=D()),
                            height=D(),)

        self.script_containers['Person Authentication'] = Frame(
                            body=HSplit([
                    self.app.getTitledCheckBox(_("Enabled"), name='enabled', checked= not self.data.get('enabled'), style='green'),
                    self.app.getTitledText(_("Name"), name='name', value=self.data.get('name',''), style='green'),
                    self.app.getTitledText(_("Description"), name='description', value=self.data.get('description',''), style='green'),
                    VSplit([
                        Label(text=_("Location"),style='green',width=len(_("Location"))),
                        HSplit([
                            self.app.getTitledCheckBox(_("Database"), name='database', checked= not self.data.get('database'), style='green'),
                            VSplit([
                                self.app.getTitledCheckBox(_("File System"), name='filesystem', checked= not self.data.get('filesystem'), style='green'),
                                self.app.getTitledText(_(""), name='filesystem', value=self.data.get('filesystem',''), style='green'),
                                ]),
                            ]),
                        ],
                        padding=3,
                        width=D(),
                    ),
                    Label(text=_("Level"),style='blue',width=len(_("Level"))),
                    Label(text=_("Properties"),style='blue',width=len(_("Properties"))),
                    Label(text=_("Script"),style='blue',width=len(_("Script"))),
                    # DynamicContainer(lambda: self.script_data_container['scopes'])
                    ],height=D(),width=D(),),

                            height=D())
        
        
        HSplit([
                    self.app.getTitledCheckBox(_("Enabled"), name='enabled', checked= not self.data.get('enabled'), style='green'),
                    self.app.getTitledText(_("Name"), name='name', value=self.data.get('name',''), style='green'),
                    self.app.getTitledText(_("Description"), name='description', value=self.data.get('description',''), style='green'),

                    VSplit([
                        Label(text=_("Location"),style='bold',width=len(_("Location"))),
                        HSplit([
                            self.app.getTitledCheckBox(_("Database"), name='database', checked= not self.data.get('database'), style='green'),
                            
                             VSplit([
                            self.app.getTitledCheckBox(_("File System"), name='filesystem', checked= not self.data.get('filesystem'), style='green'),
                            self.app.getTitledText(_(""), name='filesystem', value=self.data.get('filesystem',''), style='green'),


                             ]),
                        ]),

                    Label(text=_("Level"),style='bold',width=len(_("Level"))),
                    Label(text=_("Properties"),style='bold',width=len(_("Properties"))),
                    Label(text=_("Script"),style='bold',width=len(_("Script"))),

                        ],
                        padding=3,
                        width=D(),
                    ),

                    DynamicContainer(lambda: self.script_data_container['scopes'])
                    ])

        self.script_containers['Consent Gathering'] = HSplit([
                    VSplit([
                        self.app.getButton(text=_("Get Clients"), name='oauth:clients:get', jans_help=_("Retreive first 10 OpenID Connect clients"), handler=self.oauth_get_clients),
                        self.app.getTitledText(_("Search"), name='oauth:clients:search', jans_help=_("Press enter to perform search"), accept_handler=self.search_clients),
                        self.app.getButton(text=_("Add Client"), name='oauth:clients:add', jans_help=_("To add a new client press this button"), handler=self.add_client),
                        
                        ],
                        padding=3,
                        width=D(),
                        ),
                        DynamicContainer(lambda: self.script_data_container['clients'])
                    ],
                
                    )

        self.script_main_container = Frame(
                            body=HSplit([
                                    VSplit([
                                        self.script_navbar,
                                        DynamicContainer(lambda: self.script_main_area),
                                        ],
                                    height=D(),
                                    )
                                
                                
                                ], ),
                            height=D())
        
        
    
        
        
                                