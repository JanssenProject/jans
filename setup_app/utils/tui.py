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

# for putty connections we need the following env
os.environ['NCURSES_NO_UTF8_ACS'] = "1" 

from setup_app.messages import msg
from setup_app.config import Config
from setup_app import static
from setup_app.utils import base
from setup_app.utils.properties_utils import propertiesUtils
from setup_app.utils.progress import gluuProgress

import npyscreen

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
    do_installation = None
    exit_reason = str()
    my_counter = 0
    do_notify = True
    gluuInstaller = None
    installed_instance = None
    jettyInstaller = None

    def onStart(self):

        if Config.installed_instance: 
            self.addForm('MAIN', ServicesForm, name=msg.MAIN_label)
        else:
            self.addForm('MAIN', MAIN, name=msg.ServicesForm_label)
            self.addForm('ServicesForm', ServicesForm, name=msg.ServicesForm_label)

        for obj in list(globals().items()):
            if not obj[0] in ('MAIN', 'GluuSetupForm', 'ServicesForm') and obj[0].endswith('Form') and inspect.isclass(obj[1]):
                self.addForm(obj[0], obj[1], name=getattr(msg, obj[0]+'_label'))

    def onCleanExit(self):
        if self.do_notify:
            npyscreen.notify_wait("setup.py will exit in a moment. " + self.exit_reason, title="Warning!")

class GluuSetupForm(npyscreen.FormBaseNew):

    def beforeEditing(self):
        
        self.parentApp.my_counter = 0
        
        self.add_handlers({curses.KEY_F1: self.display_help})
        self.marketing_label = self.add(npyscreen.MultiLineEdit, value='', max_height=1, rely=self.lines-3, editable=False)

        form_name = getClassName(self)

        self.add(npyscreen.TitleFixedText, name=msg.version_label + ' ' + Config.oxVersion, rely=self.lines-5,  editable=False, labelColor='CONTROL')
        self.add(npyscreen.MultiLineEdit, value='=' * (self.columns - 4), max_height=1, rely=self.lines-4, editable=False)

        if form_name != 'InstallStepsForm':

            next_x = 20 if  form_name == 'MAIN' or (Config.installed_instance and form_name == 'ServicesForm') else 28
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

        self.os_type = self.add(npyscreen.TitleFixedText, name=msg.os_type_label, begin_entry_at=18, value=base.os_name, editable=False)
        self.init_type = self.add(npyscreen.TitleFixedText, name=msg.init_type_label, begin_entry_at=18, value=base.os_initdaemon, editable=False)
        self.httpd_type = self.add(npyscreen.TitleFixedText, name=msg.httpd_type_label, begin_entry_at=18, value=base.httpd_name, field_width=40, editable=False)
        self.license_confirm = self.add(npyscreen.Checkbox, scroll_exit=True, name=msg.acknowledge_lisence)  
        self.warning_text = self.add(npyscreen.MultiLineEdit, value=msg.setup_properties_warning, max_height=4, editable=False)


        for sys_req in ('file_max', 'mem_size', 'number_of_cpu', 'free_disk_space'):
            cur_val = getattr(base, 'current_' + sys_req)
            req_val =  static.suggested_mem_size if sys_req == 'mem_size' else getattr(msg, 'suggested_' + sys_req)
            
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

        if not propertiesUtils.check_email(self.admin_email.value):
            npyscreen.notify_confirm(msg.enter_valid_email, title="Info")
            return
        
        if not propertiesUtils.isIP(self.ip.value):
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
            setattr(Config, k, f.value)

        Config.application_max_ram = int(self.application_max_ram.value)
        self.parentApp.switchForm('ServicesForm')

    def do_beforeEditing(self):
        if not Config.hostname:
            Config.hostname = self.parentApp.gluuInstaller.detect_hostname()

        for k in self.myfields_:
            f = getattr(self, k)
            v = Config.get(k,'')
            if v:
                f.value = str(v)
                f.update()

    def backButtonPressed(self):
        self.parentApp.switchForm('MAIN')

class ServicesForm(GluuSetupForm):
    services_before_this_form = []
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
            if Config.get(service):
                cb = getattr(self, service)
                cb.value = True
                if Config.installed_instance:
                    cb.editable = False
                    self.services_before_this_form.append(service)
                cb.update()
        
        if Config.installed_instance and 'installCasa' in self.services_before_this_form:
            self.oxd_url.hidden = True
            self.oxd_url.update()


    def nextButtonPressed(self):
        service_enable_dict = {
                        'installPassport': ['gluuPassportEnabled', 'enable_scim_access_policy'],
                        'installGluuRadius': ['gluuRadiusEnabled', 'oxauth_legacyIdTokenClaims', 'oxauth_openidScopeBackwardCompatibility', 'enableRadiusScripts'],
                        'installSaml': ['gluuSamlEnabled'],
                        'installScimServer': ['gluuScimEnabled', 'enable_scim_access_policy'],
                        }

        for service in self.services:
            cb_val = getattr(self, service).value
            setattr(Config, service, cb_val)
            if cb_val and service in service_enable_dict:
                for attribute in service_enable_dict[service]:
                    setattr(Config, attribute, 'true')

        if Config.installed_instance and not Config.addPostSetupService:
                exit_result = npyscreen.notify_yes_no(
                    msg.exit_post_setup,
                    title="Warning"
                    )
                if exit_result:
                    sys.exit(False)
                else:
                    return

        if self.installSaml:
            Config.shibboleth_version = 'v3'

        if self.installOxd.value:
            Config.oxd_server_https = 'https://{}:8443'.format(Config.hostname)

        if self.installCasa.value:
            if not self.installOxd.value and not self.oxd_url.value:
                npyscreen.notify_confirm(msg.install_oxd_or_url_warning, title="Warning")
                return

            if not self.installOxd.value:
        
                oxd_server_https = self.oxd_url.value
        
                oxd_connection_result = propertiesUtils.check_oxd_server(oxd_server_https)
        
                if oxd_connection_result != True:
                    npyscreen.notify_confirm(
                            msg.oxd_connection_error.format(oxd_server_https, oxd_connection_result),
                            title="Warning"
                            )
                    return

                oxd_hostname, oxd_port = self.parentApp.gluuInstaller.parse_url(oxd_server_https)
                oxd_ssl_result = propertiesUtils.check_oxd_ssl_cert(oxd_hostname, oxd_port)
                if oxd_ssl_result :

                    npyscreen.notify_confirm(
                            msg.oxd_ssl_cert_error.format(oxd_ssl_result['CN'], oxd_hostname),
                            title="Warning")
                    return
        
                Config.oxd_server_https = oxd_server_https

        propertiesUtils.check_oxd_server_https()

        if self.installOxd.value and not 'installOxd' in self.services_before_this_form:
            result = npyscreen.notify_yes_no(msg.ask_use_gluu_storage_oxd, title=msg.ask_use_gluu_storage_oxd_title)
            if result:
                Config.oxd_use_gluu_storage = True

        # check if we have enough memory
        if not self.parentApp.jettyInstaller.calculate_selected_aplications_memory():
            result = npyscreen.notify_yes_no(msg.memory_warning, title="Warning")
            if not result:
                return

        if Config.installed_instance:
            self.parentApp.switchForm('DisplaySummaryForm')
        else:
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
        self.ask_wrends.value = [int(Config.wrends_install)]

        if Config.wrends_install == static.InstallTypes.REMOTE:
            self.wrends_hosts.hidden = False
        else:
            self.wrends_hosts.hidden = True

        if not Config.wrends_install:
            self.wrends_password.hidden = True
        else:
            self.wrends_password.hidden = False

        if Config.wrends_install == static.InstallTypes.LOCAL:
            if not Config.ldapPass:
                self.wrends_password.value = Config.oxtrust_admin_password

        self.wrends_hosts.value = Config.ldap_hostname

        self.ask_cb.value = [int(Config.cb_install)]

        if not Config.cb_install:
            self.cb_admin.hidden = True
        else:
            self.cb_admin.hidden = False

        if Config.cb_install == static.InstallTypes.REMOTE:
            self.cb_hosts.hidden = False
        else:
            self.cb_hosts.hidden = True

        if not Config.cb_install:
            self.cb_password.hidden = True
        else:
            self.cb_password.hidden = False

        if Config.cb_install == static.InstallTypes.LOCAL:
            if not Config.cb_password:
                self.cb_password.value = Config.oxtrust_admin_password

        self.cb_hosts.value = Config.get('couchbase_hostname', '')
        self.cb_admin.value = Config.get('couchebaseClusterAdmin','')

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

        Config.wrends_install = str(self.ask_wrends.value[0]) if self.ask_wrends.value[0] else 0

        if Config.wrends_install == static.InstallTypes.LOCAL:
            Config.ldap_hostname = 'localhost'
            Config.ldapPass = self.wrends_password.value
        elif Config.wrends_install == static.InstallTypes.REMOTE:
            Config.ldap_hostname = self.wrends_hosts.value
            Config.ldapPass = self.wrends_password.value

            result = propertiesUtils.check_remote_ldap(
                        self.wrends_hosts.value, 
                        Config.ldap_binddn, 
                        self.wrends_password.value
                        )

            if not result['result']:
                npyscreen.notify_confirm(result['reason'], title="Warning")
                return

        Config.cb_install =  str(self.ask_cb.value[0]) if self.ask_cb.value[0] else 0

        if Config.cb_install == static.InstallTypes.LOCAL:
            Config.couchbase_hostname = 'localhost'
            Config.cb_password = self.cb_password.value
        elif Config.cb_install == static.InstallTypes.REMOTE:
            Config.couchbase_hostname =  self.cb_hosts.value
            Config.couchebaseClusterAdmin = self.cb_admin.value
            Config.cb_password = self.cb_password.value
            result = propertiesUtils.test_cb_servers(self.cb_hosts.value)
            if not result['result']:
                npyscreen.notify_confirm(result['reason'], title="Warning")
                return

        if Config.wrends_install == static.InstallTypes.LOCAL and not propertiesUtils.checkPassword(Config.ldapPass):
            npyscreen.notify_confirm(msg.weak_password.format('WrenDS'), title="Warning")
            return

        if Config.cb_install == static.InstallTypes.LOCAL and not propertiesUtils.checkPassword(Config.cb_password):
            npyscreen.notify_confirm(msg.weak_password.format('Couchbase Server'), title="Warning")
            return

        if Config.wrends_install or Config.cb_install:
            if Config.wrends_install and Config.cb_install:
                Config.persistence_type = 'hybrid'
                self.parentApp.switchForm('StorageSelectionForm')
            else:
                storage_list = list(Config.couchbaseBucketDict.keys())
                storage = 'ldap'

                if Config.cb_install:
                    storage = 'couchbase'

                for s in storage_list:
                    Config.mappingLocations[s] = storage
                
                Config.persistence_type = storage

                self.parentApp.switchForm('DisplaySummaryForm')
        else:
            npyscreen.notify_confirm(msg.notify_select_backend, title="Warning")
            return

    def wrends_option_changed(self, widget):
        if self.ask_wrends.value:
            if not self.ask_wrends.value[0]:
                self.wrends_password.hidden = True
                self.wrends_hosts.hidden = True
            elif str(self.ask_wrends.value[0]) == static.InstallTypes.LOCAL:
                self.wrends_password.hidden = False
                self.wrends_hosts.hidden = True
            elif str(self.ask_wrends.value[0]) == static.InstallTypes.REMOTE:
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
            elif str(self.ask_cb.value[0]) == static.InstallTypes.LOCAL:
                self.cb_admin.hidden = False
                self.cb_hosts.hidden = False
                self.cb_password.hidden = False
                self.cb_hosts.hidden = True
            elif str(self.ask_cb.value[0]) == static.InstallTypes.REMOTE:
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

        self.wrends_storage = self.add(npyscreen.TitleMultiSelect, begin_entry_at=30, max_height=len(Config.couchbaseBucketDict), 
            values=list(Config.couchbaseBucketDict.keys()), name=msg.DBBackendForm_label, scroll_exit=True)
        
        self.add(npyscreen.FixedText, value=msg.unselected_storages, rely=len(Config.couchbaseBucketDict)+4, editable=False, color='STANDOUT')

    def backButtonPressed(self):
        self.parentApp.switchForm('DBBackendForm')

    def do_beforeEditing(self):
        self.wrends_storage.values = list(Config.couchbaseBucketDict.keys())

        value = []
        for i, s in enumerate(Config.couchbaseBucketDict.keys()):
            if Config.mappingLocations[s] == 'ldap':
                value.append(i)
        self.wrends_storage.value = value
        
        self.wrends_storage.update()

    def nextButtonPressed(self):
        storage_list = list(Config.couchbaseBucketDict.keys())

        for i, s in enumerate(storage_list):
            if i in self.wrends_storage.value:
                Config.mappingLocations[s] = 'ldap'
            else:
                Config.mappingLocations[s] = 'couchbase'

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
                    if Config.wrends_install == static.InstallTypes.LOCAL:
                        bt_.append('wrends')
                    elif Config.wrends_install == static.InstallTypes.REMOTE:
                        bt_.append('wrends[R]')

                    if Config.cb_install == static.InstallTypes.LOCAL:
                        bt_.append('couchbase')
                    elif Config.cb_install == static.InstallTypes.REMOTE:
                        bt_.append('couchbase[R]')
                    w.value = ', '.join(bt_)
                elif wn == 'wrends_storages':
                    if Config.wrends_install and Config.cb_install:
                        wds_ = []
                        for k in Config.mappingLocations:
                            if Config.mappingLocations[k] == 'ldap':
                                wds_.append(k)
                        w.hidden = False
                        w.value = ', '.join(wds_)
                    else:
                        w.hidden = True
                else:
                    val = Config.get(wn, 'NA')
                    w.value = str(val)

            w.update()

    def backButtonPressed(self):
        if Config.installed_instance:
            self.parentApp.switchForm('MAIN')
        elif Config.wrends_install and Config.cb_install:
            self.parentApp.switchForm('StorageSelectionForm')
        else:
            self.parentApp.switchForm('DBBackendForm')
        

    def nextButtonPressed(self):
        # Validate Properties
        propertiesUtils.check_properties()

        self.parentApp.switchForm('InstallStepsForm')

class InputBox(npyscreen.BoxTitle):
    _contained_widget = npyscreen.MultiLineEdit

class MySlider(npyscreen.SliderPercent):
    pass

class InstallStepsForm(GluuSetupForm):
    
    desc_value = None
    current_stage = 0

    def create(self):
        self.progress_percantage = self.add(MySlider, rely=4, accuracy=0, editable=False, name="Progress")
        self.installing = self.add(npyscreen.TitleFixedText, name=msg.installing_label, value="", editable=False)        
        self.description = self.add(InputBox, name="", max_height=6, rely=8)

    def do_beforeEditing(self):
        gluuProgress.before_start()
        self.progress_percantage.out_of = len(gluuProgress.services) + 1
        self.progress_percantage.update()

        t=threading.Thread(target=self.parentApp.do_installation, args=())
        t.daemon = True
        t.start()

    def do_while_waiting(self):

        if not Config.thread_queue.empty():
            data = Config.thread_queue.get()
            current = data.get('current')
            current_message = data.get('msg','')
            if  current == static.COMPLETED:
                if Config.post_messages:
                    npyscreen.notify_confirm('\n'.join(Config.post_messages), title="Post Install Messages", wide=True)
                
                msg_text = msg.post_installation if Config.installed_instance else msg.installation_completed.format(Config.hostname)
                npyscreen.notify_confirm(msg_text, title="Completed")
                
                self.parentApp.do_notify = False
                self.parentApp.switchForm(None)
            elif current == static.ERROR:
                npyscreen.notify_confirm(msg.installation_error +"\n"+current_message, title="ERROR")
                self.parentApp.do_notify = False
                self.parentApp.switchForm(None)

            self.progress_percantage.value = self.current_stage
            self.progress_percantage.update()
            self.installing.value = current_message
            self.installing.update()

            if self.desc_value != current:

                if self.current_stage < self.progress_percantage.out_of:
                    self.current_stage += 1
                
                if hasattr(msg, 'installation_description_' + str(current)):
                    desc = getattr(msg, 'installation_description_' + current)
                else:
                    desc = msg.installation_description_gluu

                self.description.value = '\n'.join(textwrap.wrap(desc, self.columns - 10))
                self.description.update()
                self.desc_value = current
                

    def backButtonPressed(self):
        pass

    def nextButtonPressed(self):
        pass

GSA = GluuSetupApp()
