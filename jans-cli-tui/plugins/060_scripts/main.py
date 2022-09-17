import os
import sys

from prompt_toolkit.layout.containers import HSplit
from prompt_toolkit.layout.dimension import D
from prompt_toolkit.widgets import Button, Label, Frame

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

    def process(self):
        pass

    def set_center_frame(self):
        """center frame content
        """
        self.app.center_container = Frame(
                            body=HSplit([Label(text="Plugin {} is not imlemented yet".format(self.name)), Button(text="Button-{}".format(self.pid))], width=D()),
                            height=D())




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
#         self.script_containers = {}
#         self.script_prepare_navbar()
#         self.script_prepare_containers()
#         self.script_nav_selection_changed(self.script_navbar.navbar_entries[0][0])

#     def process(self):
#         pass

#     def set_center_frame(self):
#         """center frame content
#         """
#         self.app.center_container = self.script_main_container



#     def script_prepare_navbar(self):
#         """prepare the navbar for the current Plugin 
#         """
#         self.script_navbar = JansSideNavBar(
#                     entries=[('clients', 'Clients'), ('scopes', 'Scopes'), ('keys', 'Keys'), ('defaults', 'Defaults'), ('properties', 'Properties'), ('logging', 'Logging')],
#                     selection_changed=self.script_nav_selection_changed,
#                     select=0,
#                     # bgcolor='#66d9ef'
#                     )


#     def script_nav_selection_changed(self, selection):
#         """This method for the selection change

#         Args:
#             selection (str): the current selected tab
#         """
#         self.app.logger.debug('OXUATH NAV: %s', selection)
#         if selection in self.script_containers:
#             self.script_main_area = self.script_containers[selection]
#         else:
#             self.script_main_area = self.app.not_implemented


#     def oauth_get_scopes(self):
#         pass
#     def oauth_get_clients(self):
#         pass
#     def search_clients(self):
#         pass
#     def add_client(self):
#         pass
#     def script_prepare_containers(self):
#         """prepare the main container (tabs) for the current Plugin 
#         """

#         self.script_data_container = {
#             'clients' :HSplit([],width=D()),
#             'scopes' :HSplit([],width=D()),

#         } 
#         self.script_main_area = HSplit([],width=D())

#         self.script_containers['scopes'] = HSplit([
#                     VSplit([
#                         self.app.getButton(text=_("Get Scopes"), name='oauth:scopes:get', jans_help=_("Retreive first 10 Scopes"), handler=self.oauth_get_scopes),
#                         self.app.getTitledText(_("Search: "), name='oauth:scopes:search', jans_help=_("Press enter to perform search")),
#                         self.app.getButton(text=_("Add Scope"), name='oauth:scopes:add', jans_help=_("To add a new scope press this button")),
#                         ],
#                         padding=3,
#                         width=D(),
#                     ),
#                     DynamicContainer(lambda: self.script_data_container['scopes'])
#                     ])

#         self.script_containers['clients'] = HSplit([
#                     VSplit([
#                         self.app.getButton(text=_("Get Clients"), name='oauth:clients:get', jans_help=_("Retreive first 10 OpenID Connect clients"), handler=self.oauth_get_clients),
#                         self.app.getTitledText(_("Search"), name='oauth:clients:search', jans_help=_("Press enter to perform search"), accept_handler=self.search_clients),
#                         self.app.getButton(text=_("Add Client"), name='oauth:clients:add', jans_help=_("To add a new client press this button"), handler=self.add_client),
                        
#                         ],
#                         padding=3,
#                         width=D(),
#                         ),
#                         DynamicContainer(lambda: self.script_data_container['clients'])
#                     ]
#                     )

#         self.script_main_container = self.script_navbar
        
#         # VSplit([
#         #                                 self.script_navbar,
#         #                                 DynamicContainer(lambda: self.script_main_area),
#         #                                 ],
#         #                             height=D(),
#         #                             )
