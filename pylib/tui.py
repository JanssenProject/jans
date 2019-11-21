#!/usr/bin/python
# -*- coding: utf-8 -*-
import time
import npyscreen
import sys
import random
from messages import msg
import textwrap

random_marketing_strings = ['What is something you refuse to share?', "What's the best type of cheese?", 'Is a hotdog a sandwich?', 'Do you fold your pizza when you eat it?', 'Toilet paper, over or under?', 'Is cereal soup?', 'Who was your worst teacher? Why?', 'Who was your favorite teacher? Why?', 'What was your favorite toy growing up?', 'Who is a celebrity you admire and why?', 'What are your 3 favorite movies?', "What's the right age to get married?", "What's your best childhood memory?", "What's your favorite holiday?", "What's one choice you really regret?", "What's your favorite childhood book?", 'Who is the funniest person you know?', 'Which TV family is most like your own?', "What's your favorite time of day?", "What's your favorite season?", 'What is the sound you love the most?', 'What is your favorite movie quote?', "What's your pet peeve(s)?", "What's your dream job?", 'Cake or pie?', 'Who is the kindest person you know?', 'What is your favorite family tradition?', "Who's your celebrity crush?", 'What are you good at?', 'Whose parents do/did you wish you had?', 'Did you ever skip school as a child?', 'Who is your favorite athlete?', 'What do you like to do on a rainy day?', 'What is your favorite animal sound?', 'What is your favorite Disney movie?', 'What is the sickest you have ever been?', 'What is your favorite day of the week?']

marketing_text_period = 10 


class GluuSetupApp(npyscreen.StandardApp):

    exit_reason = str()
    my_counter = 0

    def onStart(self):

        self.addForm("MAIN", Form1, name="System Information")
        self.addForm("FORM2", Form2, name="Setup Options")
 
    def onCleanExit(self):
        npyscreen.notify_wait("setup.py will exit in a moment. " + self.exit_reason, title="Warning!")


class GluuSetupForm(npyscreen.FormBaseNew):

    def beforeEditing(self):
        self.my, self.mx = self.curses_pad.getmaxyx()
        self.add(npyscreen.MultiLineEdit, value='─' * self.mx, max_height=1, rely=self.my-4, editable=False)
        self.marketing_label = self.add(npyscreen.MultiLineEdit, value='', max_height=1, rely=self.my-3, editable=False)


    def while_waiting(self):
        if self.parentApp.my_counter % marketing_text_period == 0:
            self.marketing_label.value = random.choice(random_marketing_strings)
            self.marketing_label.display()

        self.parentApp.my_counter += 1

class Form1(GluuSetupForm):
    def create(self):
        my, mx = self.curses_pad.getmaxyx()
        desc_wrap = textwrap.wrap(msg.decription, mx - 6)
        
        
        self.description_label = self.add(npyscreen.MultiLineEdit, value='\n'.join(desc_wrap), max_height=6, rely=2, editable=False)
        self.description_label.autowrap = True
        

        self.os_type = self.add(npyscreen.TitleFixedText, name="Detected OS", begin_entry_at=18, value=msg.os_type, editable=False)

        self.init_type = self.add(npyscreen.TitleFixedText, name="Detected init", begin_entry_at=18, value=msg.os_initdaemon, editable=False)
        self.httpd_type = self.add(npyscreen.TitleFixedText, name="Apache Version", begin_entry_at=18, value=msg.apache_version, field_width=40, editable=False)

        self.license_confirm = self.add(npyscreen.Checkbox, scroll_exit=True, name=msg.acknowledge_lisence)  

        self.warning_text = self.add(npyscreen.MultiLineEdit, value=msg.setup_properties_warning, max_height=4, editable=False)

        self.button_next = self.add(npyscreen.ButtonPress, name="Next", when_pressed_function=self.nextButtonPressed)
        self.button_quit = self.add(npyscreen.ButtonPress, name="Quit", when_pressed_function=self.quitButtonPressed)


        for sys_req in ('file_max', 'mem_size', 'number_of_cpu', 'free_disk_space'):
            cur_val = getattr(msg, 'current_' + sys_req)
            req_val = getattr(msg, 'suggested_' + sys_req)
            if cur_val < req_val:
                warning_text = getattr(msg, 'insufficient_' + sys_req).format(cur_val, req_val)
                
                if sys_req == 'file_max':
                    self.parentApp.exit_reason = warning_text
                    self.parentApp.onCleanExit()
                    time.sleep(3.5)
                    sys.exit(False)
                
                warning_text += '. Do you want to continue?'
                result = npyscreen.notify_yes_no(warning_text, title="Warning")
                if not result:
                    self.parentApp.exit_reason = msg.not_to_continue
                    self.parentApp.onCleanExit()
                    sys.exit(False)

    def nextButtonPressed(self):
        self.parentApp.my_counter = 0
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
        self.button_quit.rely = my-5
        self.button_quit.relx = mx-11
        self.warning_text.rely = my - 8

        self.button_next.rely = my-5
        self.button_next.relx = mx-20


class Form2(GluuSetupForm):
    def create(self):
        self.title = self.add(npyscreen.TitleText, name="TitleText2")

GSA = GluuSetupApp()

msg.os_type = 'CentOS 7'
msg.os_initdaemon = 'initd'
msg.test_title = 'Test Title'
msg.apache_version = "Apache 2.4"
msg.current_mem_size = 3.2
msg.current_number_of_cpu = 1
msg.current_free_disk_space = 23
msg.current_file_max = 91200


GSA.run()
