#!/usr/bin/python
import time
import npyscreen
import sys

from messages import msg


class GluuSetupApp(npyscreen.NPSAppManaged):

    exit_reason = str()

    def onStart(self):

        self.addForm("MAIN", Form1, name="System Information")
        self.addForm("FORM2", Form2, name="Setup Options")
 
    def onCleanExit(self):
        npyscreen.notify_wait("setup.py will exit in a moment. " + self.exit_reason)


class Form1(npyscreen.FormBaseNew):
    def create(self):

        self.description_label = self.add(npyscreen.MultiLineEdit, value=msg.decription, max_height=6, rely=2, editable=False)

        self.os_type = self.add(npyscreen.TitleFixedText, name="Detected OS", value=msg.os_type, editable=False)

        self.init_type = self.add(npyscreen.TitleFixedText, name="Detected init", value=msg.os_initdaemon, editable=False)
        self.httpd_type = self.add(npyscreen.TitleFixedText, name="Apache Version", value=msg.apache_version, editable=False)
        
        
        self.license_confirm = self.add(npyscreen.Checkbox, scroll_exit=True, name=msg.acknowledge_lisence)  

        self.warning_text = self.add(npyscreen.MultiLineEdit, value=msg.setup_properties_warning, max_height=4, editable=False)

        self.button_next = self.add(npyscreen.ButtonPress, name="Next", when_pressed_function=self.nextButtonPressed)
        self.button_quit = self.add(npyscreen.ButtonPress, name="Quit", when_pressed_function=self.quitButtonPressed)

        if not msg.current_mem_size < msg.suggested_number_of_cpu:
            warning_text = msg.insufficient_mem.format(msg.current_mem_size, msg.suggested_mem_size)
            warning_text += '. Do you want to continue?'
            result = npyscreen.notify_yes_no(warning_text, title="Warning")
            if not result:
                self.parentApp.exit_reason = msg.not_to_continue
                self.parentApp.onCleanExit()
                sys.exit(False)

    def nextButtonPressed(self):
        
        if not self.license_confirm.value:
            npyscreen.notify_confirm(msg.acknowledge_lisence_ask, title="Info")
            return
        
        self.parentApp.switchForm("FORM2")
        
    
    def quitButtonPressed(self):
        notify_result = npyscreen.notify_ok_cancel("Are you sure want to quit?", title= 'Warning')
        if notify_result:
            self.parentApp.exit_reason = msg.not_to_continue
            self.parentApp.switchForm(None)

    def on_cancel(self):
        self.title.value = "Hello World!"


    def resize(self):
        my, mx = self.curses_pad.getmaxyx()
        self.button_quit.rely = my-4
        self.button_quit.relx = mx-15
        self.warning_text.rely = my - 8


        self.button_next.rely = my-4
        self.button_next.relx = mx-25


class Form2(npyscreen.FormBaseNew):
    def create(self):
        self.title = self.add(npyscreen.TitleText, name="TitleText2")

    def beforeEditing(self):
        self.title.set_value("Setup Options")

        
GSA = GluuSetupApp()

msg.os_type = 'CentOS 7'
msg.os_initdaemon = 'initd'
msg.test_title = 'Test Title'
msg.apache_version = "Apache 2.4"
msg.current_mem_size = 2.2

GSA.run()
