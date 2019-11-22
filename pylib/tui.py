#!/usr/bin/python
# -*- coding: utf-8 -*-
import time
import npyscreen
import sys
import random
from messages import msg
import textwrap
import re
import socket

random_marketing_strings = ['What is something you refuse to share?', "What's the best type of cheese?", 'Is a hotdog a sandwich?', 'Do you fold your pizza when you eat it?', 'Toilet paper, over or under?', 'Is cereal soup?', 'Who was your worst teacher? Why?', 'Who was your favorite teacher? Why?', 'What was your favorite toy growing up?', 'Who is a celebrity you admire and why?', 'What are your 3 favorite movies?', "What's the right age to get married?", "What's your best childhood memory?", "What's your favorite holiday?", "What's one choice you really regret?", "What's your favorite childhood book?", 'Who is the funniest person you know?', 'Which TV family is most like your own?', "What's your favorite time of day?", "What's your favorite season?", 'What is the sound you love the most?', 'What is your favorite movie quote?', "What's your pet peeve(s)?", "What's your dream job?", 'Cake or pie?', 'Who is the kindest person you know?', 'What is your favorite family tradition?', "Who's your celebrity crush?", 'What are you good at?', 'Whose parents do/did you wish you had?', 'Did you ever skip school as a child?', 'Who is your favorite athlete?', 'What do you like to do on a rainy day?', 'What is your favorite animal sound?', 'What is your favorite Disney movie?', 'What is the sickest you have ever been?', 'What is your favorite day of the week?']

marketing_text_period = 10 


def check_email(email):
    return re.match('^[_a-z0-9-]+(\.[_a-z0-9-]+)*@[a-z0-9-]+(\.[a-z0-9-]+)*(\.[a-z]{2,4})$', email)

def isIP(address):
    if re.match(r'^((\d{1,2}|1\d{2}|2[0-4]\d|25[0-5])\.){3}(\d{1,2}|1\d{2}|2[0-4]\d|25[0-5])$', address):  
        return True

class GluuSetupApp(npyscreen.StandardApp):

    exit_reason = str()
    my_counter = 0

    def onStart(self):

        self.addForm("MAIN", MainFrom, name="System Information")
        self.addForm("HostFrom", HostFrom, name="Host and Adresses")
        self.addForm("ServicesFrom", ServicesFrom, name="Select Services to Install")
 
    def onCleanExit(self):
        npyscreen.notify_wait("setup.py will exit in a moment. " + self.exit_reason, title="Warning!")


class GluuSetupForm(npyscreen.FormBaseNew):

    def beforeEditing(self):
        self.add(npyscreen.MultiLineEdit, value='─' * self.columns, max_height=1, rely=self.lines-4, editable=False)
        self.marketing_label = self.add(npyscreen.MultiLineEdit, value='', max_height=1, rely=self.lines-3, editable=False)

        next_x = 20 if self.__class__.__name__ == 'MainFrom' else 28
        
        self.button_next = self.add(npyscreen.ButtonPress, name="Next", when_pressed_function=self.nextButtonPressed, rely=self.lines-5, relx=self.columns - next_x)
        
        if next_x == 28:
            self.button_back = self.add(npyscreen.ButtonPress, name="Back", when_pressed_function=self.backButtonPressed, rely=self.lines-5, relx=self.columns - 20)
        
        self.button_quit = self.add(npyscreen.ButtonPress, name="Quit", when_pressed_function=self.quitButtonPressed, rely=self.lines-5, relx=self.columns - 12)


    def while_waiting(self):
        if self.parentApp.my_counter % marketing_text_period == 0:
            self.marketing_label.value = random.choice(random_marketing_strings)
            self.marketing_label.update()

        self.parentApp.my_counter += 1


    def quitButtonPressed(self):
        notify_result = npyscreen.notify_ok_cancel("Are you sure want to quit?", title= 'Warning')
        if notify_result:
            self.parentApp.exit_reason = msg.not_to_continue
            self.parentApp.switchForm(None)

class MainFrom(GluuSetupForm):
    
    def create(self):
        
        desc_wrap = textwrap.wrap(msg.decription, self.columns - 6)
        
        self.description_label = self.add(npyscreen.MultiLineEdit, value='\n'.join(desc_wrap), max_height=6, rely=2, editable=False)
        self.description_label.autowrap = True

        self.os_type = self.add(npyscreen.TitleFixedText, name="Detected OS", begin_entry_at=18, value=msg.os_type, editable=False)

        self.init_type = self.add(npyscreen.TitleFixedText, name="Detected init", begin_entry_at=18, value=msg.os_initdaemon, editable=False)
        self.httpd_type = self.add(npyscreen.TitleFixedText, name="Apache Version", begin_entry_at=18, value=msg.apache_version, field_width=40, editable=False)

        self.license_confirm = self.add(npyscreen.Checkbox, scroll_exit=True, name=msg.acknowledge_lisence)  

        self.warning_text = self.add(npyscreen.MultiLineEdit, value=msg.setup_properties_warning, max_height=4, editable=False)



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
        
        self.parentApp.switchForm("HostFrom")


    def on_cancel(self):
        self.title.value = "Hello World!"


    def resize(self):
        self.button_quit.rely = self.lines-5
        self.button_quit.relx = self.columns-12
        self.warning_text.rely = self.columns - 8

        self.button_next.rely =  self.lines-5
        self.button_next.relx = self.columns-20

class HostFrom(GluuSetupForm):
    
    def create(self):

        self.ip = self.add(npyscreen.TitleText, name="IP Address", begin_entry_at=25, value=msg.ip)
        self.hostname = self.add(npyscreen.TitleText, name="Hostname", begin_entry_at=25, value=msg.hostname)        
        self.organization = self.add(npyscreen.TitleText, name="Organization Name", begin_entry_at=25, value=msg.organization)
        self.admin_email = self.add(npyscreen.TitleText, name="Support Email", begin_entry_at=25, value=msg.admin_email)
        self.city = self.add(npyscreen.TitleText, name="City or Locality", begin_entry_at=25, value=msg.city)
        self.state = self.add(npyscreen.TitleText, name="State or Province", begin_entry_at=25, value=msg.state)
        self.country = self.add(npyscreen.TitleText, name="Country Code", begin_entry_at=25, value=msg.country)

        """
        zone_fn = '/usr/share/zoneinfo/iso3166.tab'
        zone_info = []
        choices = []

        for l in open(zone_fn):
            ls = l.strip()
            if not ls.startswith('#'):
                lsl = ls.split('\t')
                zone_info.append(lsl)
                choices.append(lsl[1])

        Options = npyscreen.OptionList()
        options = Options.options
        
        options.append(npyscreen.OptionSingleChoice('Select Country', choices=choices))
        
        self.add(npyscreen.OptionListDisplay, name="Country",
            values = options,
            scroll_exit=True,
            max_height=None
            )
        """
        
    def nextButtonPressed(self):
        self.parentApp.my_counter = 0

        for k in ('ip', 'hostname', 'city', 'state', 'organization', 'admin_email', 'country'):
            setattr(msg, k, getattr(self, k).value)

        if not msg.hostname:
            npyscreen.notify_confirm(msg.enter_hostname, title="Info")
            return

        if  msg.hostname.lower() == 'localhost':
            npyscreen.notify_confirm(msg.enter_hostname_local, title="Info")
            return

        if not check_email(msg.admin_email):
            npyscreen.notify_confirm(msg.enter_valid_email, title="Info")
            return
        
        if not isIP(msg.ip):
            npyscreen.notify_confirm(msg.enter_valid_ip, title="Info")
            return

        if len(msg.country) < 2:
            npyscreen.notify_confirm(msg.enter_valid_country_code, title="Info")
            return
    
        msg.country = msg.country[:2].upper()

        self.parentApp.switchForm('ServicesFrom')

    def backButtonPressed(self):
        self.parentApp.switchForm('MAIN')

class ServicesFrom(GluuSetupForm):

    def create(self):
        self.title = self.add(npyscreen.TitleText, name="TitleText2")

    def nextButtonPressed(self):
        pass

    def backButtonPressed(self):
        self.parentApp.switchForm('GluuSetupForm')
        

GSA = GluuSetupApp()

msg.os_type = 'CentOS 7'
msg.os_initdaemon = 'initd'
msg.test_title = 'Test Title'
msg.apache_version = "Apache 2.4"
msg.current_mem_size = 5.2
msg.current_number_of_cpu = 4
msg.current_free_disk_space = 123
msg.current_file_max = 91200

msg.ip = "192.168.56.112"
msg.hostname = 'c1.gluu.org'
msg.city = 'Austin'
msg.state = 'TX'
msg.organization = 'Gluu'
msg.admin_email = 'support@gluu.org'
msg.country = 'US'

GSA.run()
