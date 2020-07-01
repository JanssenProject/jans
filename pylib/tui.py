#!/usr/bin/python
# -*- coding: utf-8 -*-
import sys
import os
import time
import random
import textwrap
import re
import socket
import curses
import string
import inspect
import threading
import math
from queue import Queue
from .messages import msg

# for putty connections we need the following env
os.environ['NCURSES_NO_UTF8_ACS'] = "1" 


import npyscreen

queue = Queue()

#install types
NONE = 0
LOCAL = '1'
REMOTE = '2'

COMPLETED = -99
ERROR = -101

random_marketing_strings = [
    'Having trouble? Open a ticket: https://support.gluu.org',
    'Cluster your Gluu Server: https://gluu.org/docs/cm',
    'oxd exposes simple, static APIs web application developers https://gluu.org/docs/oxd',
    'Gluu Gateway https://gluu.org/docs/gg',
    'Super Gluu (2FA) https://gluu.org/docs/supergluu',
    'Gluu Casa (self-service web portal) https://gluu.org/docs/casa',
    "Let's discuss your project https://www.gluu.org/booking",
    'Gluu has both a social and a business mission.',
    'Consider Gluu VIP Platform Subscription https://gluu.org/contact',
    "Deploy Gluu using Kubernetes: https://gluu.org/docs/de",
    'Evaluate our commercial offerings: https://gluu.org/pricing',
    ]

marketing_text_period = 20 


def getClassName(c):
    try:
        return getattr(c, '__class__').__name__
    except:
        return ''

class GluuSetupApp(npyscreen.StandardApp):
    installObject = None
    exit_reason = str()
    my_counter = 0
    do_notify = True

    def onStart(self):
        self.addForm("MAIN", MAIN, name=msg.MAIN_label)

        for obj in list(globals().items()):
            if not obj[0]=='GluuSetupForm' and obj[0].endswith('Form') and inspect.isclass(obj[1]):
                print("Adding form", obj[0])
                self.addForm(obj[0], obj[1], name=getattr(msg, obj[0]+'_label'))

    def onCleanExit(self):
        if self.do_notify:
            npyscreen.notify_wait("setup.py will exit in a moment. " + self.exit_reason, title="Warning!")


class GluuSetupForm(npyscreen.FormBaseNew):

    def beforeEditing(self):

        self.parentApp.my_counter = 0

        self.add_handlers({curses.KEY_F1: self.display_help})
        self.add(npyscreen.MultiLineEdit, value='=' * (self.columns - 4), max_height=1, rely=self.lines-4, editable=False)
        self.marketing_label = self.add(npyscreen.MultiLineEdit, value='', max_height=1, rely=self.lines-3, editable=False)

        form_name = getClassName(self)

        if form_name != 'InstallStepsForm':

            next_x = 20 if  form_name == 'MAIN' else 28
            self.button_next = self.add(npyscreen.ButtonPress, name="Next", when_pressed_function=self.nextButtonPressed, rely=self.lines-5, relx=self.columns - next_x)

            if next_x == 28:
                self.button_back = self.add(npyscreen.ButtonPress, name="Back", when_pressed_function=self.backButtonPressed, rely=self.lines-5, relx=self.columns - 20)

        self.button_quit = self.add(npyscreen.ButtonPress, name="Quit", when_pressed_function=self.quitButtonPressed, rely=self.lines-5, relx=self.columns - 12)

        if hasattr(self, 'do_beforeEditing'):
            self.do_beforeEditing()

    def while_waiting(self):
        if self.parentApp.my_counter % marketing_text_period == 0:
            self.marketing_label.value = random.choice(random_marketing_strings)
            self.marketing_label.update()

        self.parentApp.my_counter += 1

        if hasattr(self, 'do_while_waiting'):
            self.do_while_waiting()

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

        self.os_type = self.add(npyscreen.TitleFixedText, name=msg.os_type_label, begin_entry_at=18, value=msg.os_type + ' ' + msg.os_version, editable=False)
        self.init_type = self.add(npyscreen.TitleFixedText, name=msg.init_type_label, begin_entry_at=18, value=msg.os_initdaemon, editable=False)
        self.httpd_type = self.add(npyscreen.TitleFixedText, name=msg.httpd_type_label, begin_entry_at=18, value=msg.apache_version, field_width=40, editable=False)
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

    myfields_ = ('ip', 'hostname', 'city', 'state', 'orgName', 'admin_email', 'countryCode', 'application_max_ram', 'oxtrust_admin_password')

    def create(self):

        self.add(npyscreen.FixedText, value=make_title(msg.cert_info_label), editable=False)
        self.ip = self.add(npyscreen.TitleText, name=msg.ip_label, begin_entry_at=25)
        self.hostname = self.add(npyscreen.TitleText, name=msg.hostname_label, begin_entry_at=25)        
        self.orgName = self.add(npyscreen.TitleText, name=msg.orgName_label, begin_entry_at=25)
        self.admin_email = self.add(npyscreen.TitleText, name=msg.admin_email_label, begin_entry_at=25)
        self.city = self.add(npyscreen.TitleText, name=msg.city_label, begin_entry_at=25)
        self.state = self.add(npyscreen.TitleText, name=msg.state_label, begin_entry_at=25)
        self.countryCode = self.add(npyscreen.TitleText, name=msg.countryCode_label, begin_entry_at=25)

        self.add(npyscreen.FixedText, value=make_title(msg.sys_info_label), rely=12, editable=False)
        self.application_max_ram = self.add(npyscreen.TitleText, name=msg.application_max_ram_label, begin_entry_at=25)
        self.oxtrust_admin_password = self.add(npyscreen.TitleText, name=msg.oxtrust_admin_password_label, begin_entry_at=25)


    def nextButtonPressed(self):

        if not self.hostname.value:
            npyscreen.notify_confirm(msg.enter_hostname, title="Info")
            return

        if  self.hostname.value.lower() == 'localhost':
            npyscreen.notify_confirm(msg.enter_hostname_local, title="Info")
            return

        if not self.parentApp.installObject.check_email(self.admin_email.value):
            npyscreen.notify_confirm(msg.enter_valid_email, title="Info")
            return

        if not self.parentApp.installObject.isIP(self.ip.value):
            npyscreen.notify_confirm(msg.enter_valid_ip, title="Info")
            return

        if len(self.countryCode.value) < 2:
            npyscreen.notify_confirm(msg.enter_valid_countryCode, title="Info")
            return

        if len(self.oxtrust_admin_password.value) < 6:
            npyscreen.notify_confirm(msg.oxtrust_admin_password_warning, title="Info")
            return

        try:
            int(self.application_max_ram.value)
        except:
            npyscreen.notify_confirm(msg.max_ram_int_warning, title="Info")
            return

        for k in self.myfields_:
            f = getattr(self, k)
            setattr(self.parentApp.installObject, k, f.value)

        self.parentApp.installObject.application_max_ram = int(self.application_max_ram.value)
        self.parentApp.switchForm('ServicesForm')

    def do_beforeEditing(self):
        if not self.parentApp.installObject.hostname:
            self.parentApp.installObject.hostname = self.parentApp.installObject.detect_hostname()

        for k in self.myfields_:
            f = getattr(self, k)
            v = getattr(self.parentApp.installObject, k)
            if v:
                f.value = str(v)
                f.update()

    def backButtonPressed(self):
        self.parentApp.switchForm('MAIN')

class ServicesForm(GluuSetupForm):

    services = ('installHttpd', 'installSaml', 'installOxAuthRP', 
                'installPassport', 'installGluuRadius', 'installOxd', 
                'installCasa', 'installScimServer', 'installFido2',
                )

    def create(self):
        for service in self.services:
            cb = self.add(npyscreen.Checkbox, scroll_exit=True, name = getattr(msg, 'ask_' + service))
            setattr(self, service, cb)

        self.oxd_url = self.add(npyscreen.TitleText, name=msg.oxd_url_label, rely=12, begin_entry_at=17, hidden=True)

        self.installCasa.value_changed_callback = self.casa_oxd_option_changed
        self.installOxd.value_changed_callback = self.casa_oxd_option_changed

    def do_beforeEditing(self):
        for service in self.services:
            if getattr(self.parentApp.installObject, service):
                cb = getattr(self, service)
                cb.value = True
                cb.update()

    def nextButtonPressed(self):

        service_enable_dict = {
                        'installPassport': 'gluuPassportEnabled',
                        'installGluuRadius': 'gluuRadiusEnabled',
                        'installSaml': 'gluuSamlEnabled',
                        'installScimServer': 'gluuScimEnabled',
                        }

        for service in self.services:
            cb_val = getattr(self, service).value
            setattr(self.parentApp.installObject, service, cb_val)
            if cb_val and service in service_enable_dict:
                setattr(self.parentApp.installObject, service_enable_dict[service], 'true')


        if self.installOxd.value:
            self.parentApp.installObject.oxd_server_https = 'https://{}:8443'.format(self.parentApp.installObject.hostname)

        if self.installCasa.value:
            if not self.installOxd.value and not self.oxd_url.value:
                npyscreen.notify_confirm(msg.install_oxd_or_url_warning, title="Warning")
                return

            if not self.installOxd.value:

                oxd_server_https = self.oxd_url.value
                oxd_connection_result = self.parentApp.installObject.check_oxd_server(oxd_server_https)

                if oxd_connection_result != True:
                    npyscreen.notify_confirm(
                            msg.oxd_connection_error.format(oxd_server_https, oxd_connection_result),
                            title="Warning"
                            )
                    return

                oxd_hostname, oxd_port = self.parentApp.installObject.parse_url(oxd_server_https)
                oxd_ssl_result = self.parentApp.installObject.check_oxd_ssl_cert(oxd_hostname, oxd_port)
                if oxd_ssl_result :

                    npyscreen.notify_confirm(
                            msg.oxd_ssl_cert_error.format(oxd_ssl_result['CN'], oxd_hostname),
                            title="Warning")
                    return

                self.parentApp.installObject.oxd_server_https = oxd_server_https

        oxd_hostname, oxd_port = self.parentApp.installObject.parse_url(self.parentApp.installObject.oxd_server_https)
        if not oxd_port: 
            oxd_port=8443

        self.parentApp.installObject.templateRenderingDict['oxd_hostname'] = oxd_hostname
        self.parentApp.installObject.templateRenderingDict['oxd_port'] = str(oxd_port)

        if self.installOxd.value:
            result = npyscreen.notify_yes_no(msg.ask_use_gluu_storage_oxd, title=msg.ask_use_gluu_storage_oxd_title)
            if result:
                self.parentApp.installObject.oxd_use_gluu_storage = True

        # check if we have enough memory
        if not self.parentApp.installObject.calculate_selected_aplications_memory():
            result = npyscreen.notify_yes_no(msg.memory_warning, title="Warning")
            if not result:
                return

        self.parentApp.switchForm('DBBackendForm')


    def casa_oxd_option_changed(self, widget):

        if self.installOxd.value:
            self.oxd_url.hidden = True

        elif self.installCasa.value and not self.installOxd.value:
            self.oxd_url.hidden = False

        elif not self.installCasa.value:
            self.oxd_url.hidden = True

        self.oxd_url.update()


    def backButtonPressed(self):
        self.parentApp.switchForm('HostForm')


def make_title(text):
    return '-'*10 + ' '+  text +' '+ '-'*10


class DBBackendForm(GluuSetupForm):
    def create(self):
        self.editw = 2
        self.add(npyscreen.FixedText, value=make_title(msg.ask_wrends_install), editable=False)

        self.ask_wrends = self.add(npyscreen.SelectOne, max_height=3, 
                values = msg.wrends_install_options, scroll_exit=True)
        self.ask_wrends.value_changed_callback = self.wrends_option_changed
        self.wrends_password = self.add(npyscreen.TitleText, name=msg.password_label)
        self.wrends_hosts = self.add(npyscreen.TitleText, name=msg.hosts_label)
        self.wrends_option_changed(self.ask_wrends)

        self.add(npyscreen.FixedText, value=make_title(msg.ask_cb_install), rely=10, editable=False)

        self.ask_cb = self.add(npyscreen.SelectOne, max_height=3,
                values = msg.cb_install_options, scroll_exit=True)
        self.ask_cb.value_changed_callback = self.cb_option_changed
        self.cb_admin = self.add(npyscreen.TitleText, name=msg.username_label)
        self.cb_password = self.add(npyscreen.TitleText, name=msg.password_label)
        self.cb_hosts = self.add(npyscreen.TitleText, name=msg.hosts_label)
        self.cb_option_changed(self.ask_cb)

    def do_beforeEditing(self):
        self.ask_wrends.value = [int(self.parentApp.installObject.wrends_install)]

        if self.parentApp.installObject.wrends_install == REMOTE:
            self.wrends_hosts.hidden = False
        else:
            self.wrends_hosts.hidden = True

        if not self.parentApp.installObject.wrends_install:
            self.wrends_password.hidden = True
        else:
            self.wrends_password.hidden = False

        if self.parentApp.installObject.wrends_install == LOCAL:
            if not self.parentApp.installObject.ldapPass:
                self.wrends_password.value = self.parentApp.installObject.oxtrust_admin_password

        self.wrends_hosts.value = self.parentApp.installObject.ldap_hostname        

        self.ask_cb.value = [int(self.parentApp.installObject.cb_install)]

        if not self.parentApp.installObject.cb_install:
            self.cb_admin.hidden = True
        else:
            self.cb_admin.hidden = False

        if self.parentApp.installObject.cb_install == REMOTE:
            self.cb_hosts.hidden = False
        else:
            self.cb_hosts.hidden = True

        if not self.parentApp.installObject.cb_install:
            self.cb_password.hidden = True
        else:
            self.cb_password.hidden = False

        if self.parentApp.installObject.cb_install == LOCAL:
            if not self.parentApp.installObject.cb_password:
                self.cb_password.value = self.parentApp.installObject.oxtrust_admin_password

        self.cb_hosts.value = self.parentApp.installObject.couchbase_hostname
        self.cb_admin.value = self.parentApp.installObject.couchebaseClusterAdmin

        self.wrends_hosts.update()
        self.ask_wrends.update()
        self.wrends_hosts.update()
        self.wrends_password.update()

        self.cb_hosts.update()
        self.ask_cb.update()
        self.cb_hosts.update()
        self.cb_password.update()


    def nextButtonPressed(self):

        msg.backend_types = []

        self.parentApp.installObject.wrends_install = str(self.ask_wrends.value[0]) if self.ask_wrends.value[0] else 0

        if self.parentApp.installObject.wrends_install == LOCAL:
            self.parentApp.installObject.ldap_hostname = 'localhost'
            self.parentApp.installObject.ldapPass = self.wrends_password.value
        elif self.parentApp.installObject.wrends_install == REMOTE:
            self.parentApp.installObject.ldap_hostname = self.wrends_hosts.value
            self.parentApp.installObject.ldapPass = self.wrends_password.value

            result = self.parentApp.installObject.check_remote_ldap(
                        self.wrends_hosts.value, 
                        self.parentApp.installObject.ldap_binddn, 
                        self.wrends_password.value
                        )

            if not result['result']:
                npyscreen.notify_confirm(result['reason'], title="Warning")
                return

        self.parentApp.installObject.cb_install =  str(self.ask_cb.value[0]) if self.ask_cb.value[0] else 0

        if self.parentApp.installObject.cb_install == LOCAL:
            self.parentApp.installObject.couchbase_hostname = 'localhost'
            self.parentApp.installObject.cb_password = self.cb_password.value
        elif self.parentApp.installObject.cb_install == REMOTE:
            self.parentApp.installObject.couchbase_hostname =  self.cb_hosts.value
            self.parentApp.installObject.couchebaseClusterAdmin = self.cb_admin.value
            self.parentApp.installObject.cb_password = self.cb_password.value
            result = self.parentApp.installObject.test_cb_servers(self.cb_hosts.value)
            if not result['result']:
                npyscreen.notify_confirm(result['reason'], title="Warning")
                return

        if self.parentApp.installObject.cb_install:
            self.parentApp.installObject.cache_provider_type = 'NATIVE_PERSISTENCE'
            self.parentApp.installObject.add_couchbase_post_messages()

        if self.parentApp.installObject.wrends_install  == LOCAL and not self.parentApp.installObject.checkPassword(self.parentApp.installObject.ldapPass):
            npyscreen.notify_confirm(msg.weak_password.format('WrenDS'), title="Warning")
            return

        if self.parentApp.installObject.cb_install == LOCAL and not self.parentApp.installObject.checkPassword(self.parentApp.installObject.cb_password):
            npyscreen.notify_confirm(msg.weak_password.format('Couchbase Server'), title="Warning")
            return

        if self.parentApp.installObject.wrends_install or self.parentApp.installObject.cb_install:
            if self.parentApp.installObject.wrends_install and self.parentApp.installObject.cb_install:
                self.parentApp.installObject.persistence_type = 'hybrid'
                self.parentApp.switchForm('StorageSelectionForm')
            else:
                storage_list = list(self.parentApp.installObject.couchbaseBucketDict.keys())
                storage = 'ldap'

                if self.parentApp.installObject.cb_install:
                    storage = 'couchbase'

                for s in storage_list:
                    self.parentApp.installObject.mappingLocations[s] = storage

                self.parentApp.installObject.persistence_type = storage

                self.parentApp.switchForm('DisplaySummaryForm')
        else:
            npyscreen.notify_confirm(msg.notify_select_backend, title="Warning")
            return

    def wrends_option_changed(self, widget):
        if self.ask_wrends.value:
            if not self.ask_wrends.value[0]:
                self.wrends_password.hidden = True
                self.wrends_hosts.hidden = True
            elif str(self.ask_wrends.value[0]) == LOCAL:
                self.wrends_password.hidden = False
                self.wrends_hosts.hidden = True
            elif str(self.ask_wrends.value[0]) == REMOTE:
                self.wrends_password.hidden = False
                self.wrends_hosts.hidden = False

            self.wrends_password.update()
            self.wrends_hosts.update()

    def cb_option_changed(self, widget):
        if self.ask_cb.value:
            if not self.ask_cb.value[0]:
                self.cb_admin.hidden = True
                self.cb_password.hidden = True
                self.cb_hosts.hidden = True
            elif str(self.ask_cb.value[0]) == LOCAL:
                self.cb_admin.hidden = False
                self.cb_hosts.hidden = False
                self.cb_password.hidden = False
                self.cb_hosts.hidden = True
            elif str(self.ask_cb.value[0]) == REMOTE:
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

        self.wrends_storage = self.add(npyscreen.TitleMultiSelect, begin_entry_at=30, max_height=len(msg.storages), 
            values=msg.storages, name=msg.DBBackendForm_label, scroll_exit=True)

        self.add(npyscreen.FixedText, value=msg.unselected_storages, rely=len(msg.storages)+4, editable=False, color='STANDOUT')

    def backButtonPressed(self):
        self.parentApp.switchForm('DBBackendForm')

    def do_beforeEditing(self):
        self.wrends_storage.values = list(self.parentApp.installObject.couchbaseBucketDict.keys())

        value = []
        for i, s in enumerate(self.parentApp.installObject.couchbaseBucketDict.keys()):
            if self.parentApp.installObject.mappingLocations[s] == 'ldap':
                value.append(i)
        self.wrends_storage.value = value

        self.wrends_storage.update()

    def nextButtonPressed(self):
        storage_list = list(self.parentApp.installObject.couchbaseBucketDict.keys())

        for i, s in enumerate(storage_list):
            if i in self.wrends_storage.value:
                self.parentApp.installObject.mappingLocations[s] = 'ldap'
            else:
                self.parentApp.installObject.mappingLocations[s] = 'couchbase'

        self.parentApp.switchForm('DisplaySummaryForm')


class DisplaySummaryForm(GluuSetupForm):

    myfields_1 = ("hostname", "orgName", "os_type", "city", "state", "countryCode",
                   "application_max_ram")

    myfields_2 = ( "installOxAuth", "installOxTrust", 
                    "installHttpd", "installSaml", "installOxAuthRP",
                    "installPassport", "installGluuRadius", 
                    "installOxd", "installCasa",
                    'installScimServer', 'installFido2',
                    "java_type",
                    "backend_types", 'wrends_storages')

    def create(self):

        for i, wn in enumerate(self.myfields_1):
            setattr(self, 
                    wn, 
                    self.add(
                            npyscreen.TitleFixedText,
                            name=getattr(msg, wn+'_label'),
                            value="",
                            begin_entry_at=24,
                            editable=False,
                            )
                    )

        sec_col_n = math.ceil(len(self.myfields_2)/2.0)
        for j, wn in enumerate(self.myfields_2):
            if j < sec_col_n:
                relx=2
                rely = i+4+j
            else:
                relx=39
                rely = i+4+j-sec_col_n
            setattr(self, 
                    wn, 
                    self.add(
                            npyscreen.TitleFixedText,
                            name=getattr(msg, wn+'_label'),
                            value="",
                            begin_entry_at=20,
                            editable=False,
                            rely=rely,
                            relx=relx,
                            )
                    )



    def do_beforeEditing(self):
        wrends_storages_widget = getattr(self, 'wrends_storages')

        for wn in self.myfields_1+self.myfields_2:
            w = getattr(self, wn)
            if getClassName(w) == 'TitleFixedText':
                if wn == 'backend_types':
                    bt_ = []
                    if self.parentApp.installObject.wrends_install == LOCAL:
                        bt_.append('wrends')
                    elif self.parentApp.installObject.wrends_install == REMOTE:
                        bt_.append('wrends[R]')

                    if self.parentApp.installObject.cb_install == LOCAL:
                        bt_.append('couchbase')
                    elif self.parentApp.installObject.cb_install == REMOTE:
                        bt_.append('couchbase[R]')
                    w.value = ', '.join(bt_)
                elif wn == 'wrends_storages':
                    if self.parentApp.installObject.wrends_install and self.parentApp.installObject.cb_install:
                        wds_ = []
                        for k in self.parentApp.installObject.mappingLocations:
                            if self.parentApp.installObject.mappingLocations[k] == 'ldap':
                                wds_.append(k)
                        w.hidden = False
                        w.value = ', '.join(wds_)
                    else:
                        w.hidden = True
                else:
                    val = getattr(self.parentApp.installObject, wn)
                    w.value = str(val)

            w.update()

    def backButtonPressed(self):
        if self.parentApp.installObject.wrends_install and self.parentApp.installObject.cb_install:
            self.parentApp.switchForm('StorageSelectionForm')
        else:
            self.parentApp.switchForm('DBBackendForm')


    def nextButtonPressed(self):
        # Validate Properties
        self.parentApp.installObject.check_properties()

        self.parentApp.switchForm('InstallStepsForm')

class InputBox(npyscreen.BoxTitle):
    _contained_widget = npyscreen.MultiLineEdit

class InstallStepsForm(GluuSetupForm):

    desc_value = None

    def create(self):
        self.prgress_percantage = self.add(npyscreen.TitleSliderPercent, accuracy=0, out_of=msg.installation_step_number+1, rely=4, editable=False, name="Progress")
        self.installing = self.add(npyscreen.TitleFixedText, name=msg.installing_label, value="", editable=False)
        self.description = self.add(InputBox, name="", max_height=6, rely=8)


    def do_beforeEditing(self):
        t=threading.Thread(target=self.parentApp.installObject.do_installation, args=(queue,))
        t.daemon = True
        t.start()


    def do_while_waiting(self):

        if not queue.empty():
            data = queue.get()
            if data[0] == COMPLETED:
                if self.parentApp.installObject.post_messages:
                    npyscreen.notify_confirm('\n'.join(self.parentApp.installObject.post_messages), title="Post Install Messages", wide=True)
                npyscreen.notify_confirm(msg.installation_completed.format(self.parentApp.installObject.hostname), title="Completed")
                self.parentApp.do_notify = False
                self.parentApp.switchForm(None)
            elif data[0] == ERROR:
                npyscreen.notify_confirm(msg.installation_error +"\n"+data[2], title="ERROR")
                self.parentApp.do_notify = False
                self.parentApp.switchForm(None)

            self.prgress_percantage.value = data[0]
            self.prgress_percantage.update()
            self.installing.value = data[2]
            self.installing.update()

            if self.desc_value != data[1]:

                if hasattr(msg, 'installation_description_' + data[1]):
                    desc = getattr(msg, 'installation_description_' + data[1])

                else:
                    desc = msg.installation_description_gluu
                self.description.value = '\n'.join(textwrap.wrap(desc, self.columns - 10))
                self.description.update()
                self.desc_value = data[1]


    def backButtonPressed(self):
        pass

    def nextButtonPressed(self):
        pass
