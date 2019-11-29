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
import curses
import string
import inspect

#Todo: check min lines:25 comumns: 80


random_marketing_strings = ['What is something you refuse to share?', "What's the best type of cheese?", 'Is a hotdog a sandwich?', 'Do you fold your pizza when you eat it?', 'Toilet paper, over or under?', 'Is cereal soup?', 'Who was your worst teacher? Why?', 'Who was your favorite teacher? Why?', 'What was your favorite toy growing up?', 'Who is a celebrity you admire and why?', 'What are your 3 favorite movies?', "What's the right age to get married?", "What's your best childhood memory?", "What's your favorite holiday?", "What's one choice you really regret?", "What's your favorite childhood book?", 'Who is the funniest person you know?', 'Which TV family is most like your own?', "What's your favorite time of day?", "What's your favorite season?", 'What is the sound you love the most?', 'What is your favorite movie quote?', "What's your pet peeve(s)?", "What's your dream job?", 'Cake or pie?', 'Who is the kindest person you know?', 'What is your favorite family tradition?', "Who's your celebrity crush?", 'What are you good at?', 'Whose parents do/did you wish you had?', 'Did you ever skip school as a child?', 'Who is your favorite athlete?', 'What do you like to do on a rainy day?', 'What is your favorite animal sound?', 'What is your favorite Disney movie?', 'What is the sickest you have ever been?', 'What is your favorite day of the week?']

marketing_text_period = 10 


def check_email(email):
    return re.match('^[_a-z0-9-]+(\.[_a-z0-9-]+)*@[a-z0-9-]+(\.[a-z0-9-]+)*(\.[a-z]{2,4})$', email)

def isIP(address):
    if re.match(r'^((\d{1,2}|1\d{2}|2[0-4]\d|25[0-5])\.){3}(\d{1,2}|1\d{2}|2[0-4]\d|25[0-5])$', address):  
        return True

def checkPassword(pwd):
    if re.search('^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*\W)[a-zA-Z0-9\S]{6,}$', pwd):
        return True

def getPW(size=12, chars=string.ascii_uppercase + string.digits + string.lowercase, special=''):
        
        if not special:
            random_password = [random.choice(chars) for _ in range(size)]
        else:
            ndigit = random.randint(1, 3)
            nspecial = random.randint(1, 2)


            ncletter = random.randint(2, 5)
            nsletter = size - ndigit - nspecial - ncletter
            
            random_password = []
            
            for n, rc in ((ndigit, string.digits), (nspecial, special),
                        (ncletter, string.ascii_uppercase),
                        (nsletter, string.lowercase)):
            
                random_password += [random.choice(rc) for _ in range(n)]
            
        random.shuffle(random_password)
                
        return ''.join(random_password)
        
def getClassName(c):
    return getattr(c, '__class__').__name__




class GluuSetupApp(npyscreen.StandardApp):

    exit_reason = str()
    my_counter = 0

    def onStart(self):
        
        
        self.addForm("MAIN", MAIN, name=msg.MAIN_label)
        
        for obj in globals().items():
            if not obj[0]=='GluuSetupForm' and obj[0].endswith('Form') and inspect.isclass(obj[1]):
                print "Adding form", obj[0]
                self.addForm(obj[0], obj[1], name=getattr(msg, obj[0]+'_label'))
 
 
    def onCleanExit(self):
        npyscreen.notify_wait("setup.py will exit in a moment. " + self.exit_reason, title="Warning!")


class GluuSetupForm(npyscreen.FormBaseNew):

    def beforeEditing(self):
        self.add_handlers({curses.KEY_F1: self.display_help})
        self.add(npyscreen.MultiLineEdit, value='─' * self.columns, max_height=1, rely=self.lines-4, editable=False)
        self.marketing_label = self.add(npyscreen.MultiLineEdit, value='', max_height=1, rely=self.lines-3, editable=False)

        next_x = 20 if  getClassName(self) == 'MAIN' else 28
        
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

    def display_help(self, code_of_key_pressed):
        
        class_name = self.__class__.__name__
        if hasattr(msg, class_name+'Help'):
            help_text = getattr(msg, class_name+'Help')
        else:
            help_text = msg.no_help
        
        npyscreen.notify_confirm(help_text, title="Help", wide=True)

class MAIN(GluuSetupForm):
    
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
        
        self.parentApp.switchForm("HostForm")
        

    def on_cancel(self):
        self.title.value = "Hello World!"


    def resize(self):
        self.button_quit.rely = self.lines-5
        self.button_quit.relx = self.columns-12
        self.warning_text.rely = self.columns - 8

        self.button_next.rely =  self.lines-5
        self.button_next.relx = self.columns-20

class HostForm(GluuSetupForm):
    
    def create(self):

        self.add(npyscreen.FixedText, value=make_title(msg.cert_info_label), editable=False)
        self.ip = self.add(npyscreen.TitleText, name="IP Address", begin_entry_at=25, value=msg.ip)
        self.hostname = self.add(npyscreen.TitleText, name="Hostname", begin_entry_at=25, value=msg.hostname)        
        self.organization = self.add(npyscreen.TitleText, name="Organization Name", begin_entry_at=25, value=msg.organization)
        self.admin_email = self.add(npyscreen.TitleText, name="Support Email", begin_entry_at=25, value=msg.admin_email)
        self.city = self.add(npyscreen.TitleText, name="City or Locality", begin_entry_at=25, value=msg.city)
        self.state = self.add(npyscreen.TitleText, name="State or Province", begin_entry_at=25, value=msg.state)
        self.country = self.add(npyscreen.TitleText, name="Country Code", begin_entry_at=25, value=msg.country)

        self.add(npyscreen.FixedText, value=make_title(msg.sys_info_label), rely=12, editable=False)
        self.max_ram = self.add(npyscreen.TitleText, name=msg.max_ram_label, begin_entry_at=25, value=str(msg.max_ram))
        self.oxtrust_admin_password = self.add(npyscreen.TitleText, name=msg.oxtrust_admin_password_label, begin_entry_at=25, value=msg.oxtrust_admin_password)

        
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
        
        if len(self.oxtrust_admin_password.value) < 6:
            npyscreen.notify_confirm(msg.oxtrust_admin_password_warning, title="Info")
            return

        try:
            msg.max_ram = int(self.max_ram.value)
        except:
            npyscreen.notify_confirm(msg.max_ram_int_warning, title="Info")
            return

        msg.oxtrust_admin_password = self.oxtrust_admin_password.value
        msg.country = msg.country[:2].upper()

        self.parentApp.switchForm('ServicesForm')

    def backButtonPressed(self):
        self.parentApp.switchForm('MAIN')

class ServicesForm(GluuSetupForm):

    services = ('installHttpd', 'installSaml', 'installOxAuthRP', 'installPassport', 'installGluuRadius')

    def create(self):
        
        for service in self.services:
            cb = self.add(npyscreen.Checkbox, scroll_exit=True, name = getattr(msg, 'ask_' + service))  
            if getattr(msg, service):
                cb.value = True

            setattr(self, service, cb)

    def nextButtonPressed(self):
        for service in self.services:
            cb_val = getattr(self, service).value
            setattr(msg, service, cb_val)

        self.parentApp.switchForm('DBBackendForm')

    def backButtonPressed(self):
        self.parentApp.switchForm('HostForm')


def make_title(text):
    return '─'*10 + ' '+  text +' '+ '─'*10


class DBBackendForm(GluuSetupForm):
    def create(self):
        self.editw = 2
        self.add(npyscreen.FixedText, value=make_title(msg.ask_wrends_install), editable=False)

        wrends_val = 0
        if msg.wrends_remote:
            wrends_val = 2
        if msg.wrends_install:
            wrends_val = 1

        self.ask_wrends = self.add(npyscreen.SelectOne, max_height=3, value = [wrends_val,], 
                values = msg.wrends_install_options, scroll_exit=True)
        self.ask_wrends.value_changed_callback = self.wrends_option_changed

        self.wrends_password = self.add(npyscreen.TitleText, name="Password", value=msg.wrends_password)
        self.wrends_hosts = self.add(npyscreen.TitleText, name="Hosts", value=msg.wrends_hosts)
        self.wrends_option_changed(self.ask_wrends)
        
        
        self.add(npyscreen.FixedText, value=make_title(msg.ask_cb_install), rely=10, editable=False)

        cb_val = 0
        if msg.cb_remote:
            cb_val = 2
        if msg.cb_install:
            cb_val = 1

        self.ask_cb = self.add(npyscreen.SelectOne, max_height=3, value = [cb_val,], 
                values = msg.wrends_install_options, scroll_exit=True)
        self.ask_cb.value_changed_callback = self.cb_option_changed

        self.cb_admin = self.add(npyscreen.TitleText, name="Username", value=msg.cb_username)
        self.cb_password = self.add(npyscreen.TitleText, name="Password", value=msg.cb_password)
        self.cb_hosts = self.add(npyscreen.TitleText, name="Hosts", value=msg.cb_hosts)
        self.cb_option_changed(self.ask_cb)

    def nextButtonPressed(self):
        if self.ask_wrends.value[0] == 0:
            msg.wrends_install = False
        elif self.ask_wrends.value[0] == 1:
            msg.wrends_install = True
            msg.wrends_remote = False
            msg.wrends_hosts = 'localhost'
            msg.wrends_password = self.wrends_password.value
        elif self.ask_wrends.value[0] == 2:
            msg.wrends_install = False
            msg.wrends_remote = True
            msg.wrends_hosts = self.wrends_hosts.value
            msg.wrends_password = self.wrends_password.value

        if self.ask_cb.value[0] == 0:
            msg.cb_install = False
        elif self.ask_cb.value[0] == 1:
            msg.cb_install = True
            msg.cb_remote = False
            msg.cb_hosts = 'localhost'
            msg.cb_password = self.cb_password.value
        elif self.ask_cb.value[0] == 2:
            msg.cb_install = False
            msg.cb_remote = True
            msg.cb_hosts = self.cb_hosts.value
            msg.cb_password = self.cb_password.value

        if not checkPassword(msg.wrends_password):
            npyscreen.notify_confirm(msg.weak_password.format('WrenDS'), title="Warning")
            return

        if not checkPassword(msg.cb_password):
            npyscreen.notify_confirm(msg.weak_password.format('Couchbase Server'), title="Warning")
            return

        if (self.ask_wrends.value[0] in (1,2)) or (self.ask_cb.value[0] in (1,2)):
            if (self.ask_wrends.value[0] in (1,2)) and (self.ask_cb.value[0] in (1,2)):
                self.parentApp.switchForm('StorageSelectionForm')
            else:
                self.parentApp.switchForm('DisplaySummaryForm')
        else:
            npyscreen.notify_confirm(msg.notify_select_backend, title="Warning")
            return

    def wrends_option_changed(self, widget):
        if not self.ask_wrends.value[0]:
            self.wrends_password.hidden = True
            self.wrends_hosts.hidden = True
        elif self.ask_wrends.value[0] == 1:
            self.wrends_password.hidden = False
            self.wrends_hosts.hidden = True
        elif self.ask_wrends.value[0] == 2:
            self.wrends_password.hidden = False
            self.wrends_hosts.hidden = False
            
        self.wrends_password.update()
        self.wrends_hosts.update()

    def cb_option_changed(self, widget):
        if not self.ask_cb.value[0]:
            self.cb_admin.hidden = True
            self.cb_password.hidden = True
            self.cb_hosts.hidden = True
        elif self.ask_cb.value[0] == 1:
            self.cb_admin.hidden = False
            self.cb_hosts.hidden = False
            self.cb_password.hidden = False
            self.cb_hosts.hidden = True
        elif self.ask_cb.value[0] == 2:
            self.cb_admin.hidden = False
            self.cb_password.hidden = False
            self.cb_hosts.hidden = False
        
        self.cb_admin.update()
        self.cb_password.update()
        self.cb_hosts.update()

    def backButtonPressed(self):
        self.parentApp.switchForm('ServicesForm')


class StorageSelectionForm(GluuSetupForm):
    def create(self):

        storage_val = []

        for i, s in enumerate(msg.storage_list):
            if s in msg.wrends_storages:
                storage_val.append(i)
        
        self.wrends_storage = self.add(npyscreen.TitleMultiSelect, max_height = len(msg.storage_list), begin_entry_at=25, value = storage_val, name="Store on WrenDS", 
            values = [ s.title() for s in msg.storage_list ], scroll_exit=True)
        
        self.add(npyscreen.FixedText, value=msg.unselected_storages, rely=len(msg.storage_list)+4, editable=False, color='STANDOUT')

    def backButtonPressed(self):
        self.parentApp.switchForm('DBBackendForm')

    def nextButtonPressed(self):
        
        tmp_ = []

        for s in self.wrends_storage.value:
            tmp_.append(msg.storage_list[s])

        msg.wrends_storages = tmp_
        
        self.parentApp.switchForm('DisplaySummaryForm')

class DisplaySummaryForm(GluuSetupForm):
    def create(self):
        pass

    def backButtonPressed(self):
        pass

    def nextButtonPressed(self):
        pass



class InstallStepsForm(GluuSetupForm):
    def create(self):
        pass

    def backButtonPressed(self):
        pass

    def nextButtonPressed(self):
        pass




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

msg.max_ram = 3072
msg.oxtrust_admin_password = getPW(special='.*=!%&+/-')

msg.installHttpd = True
msg.installSaml = False
msg.installOxAuthRP = False
msg.installPassport = False
msg.installGluuRadius = False


msg.wrends_install = False
msg.wrends_remote = True
msg.wrends_hosts = 'localhost'
msg.wrends_password = getPW(special='.*=!%&+/-')

msg.cb_install = True
msg.cb_remote = False
msg.cb_password = getPW(special='.*=!%&+/-')
msg.cb_username = 'admin'

msg.cb_hosts = 'c1.gluu.org,c2.gluu.org'

msg.storage_list = ['default', 'user', 'cache', 'site', 'token']
msg.wrends_storages = ['user', 'token']


GSA.run()
