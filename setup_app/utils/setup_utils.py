import os
import sys
import time
import subprocess
import re
import socket
import shutil
import uuid
import base64
import json
import string
import random
import hashlib

from pathlib import Path
from urllib.parse import urlparse
from collections import OrderedDict
from string import Template

from setup_app import paths
from setup_app.config import Config
from setup_app.utils import base
from setup_app.static import InstallTypes
from setup_app.utils.crypto64 import Crypto64

class SetupUtils(Crypto64):

    @classmethod
    def init(self):
        #let's make commands available via Config object
        for attr in dir(paths):
            if attr.startswith('cmd_'):
                setattr(Config, attr, getattr(paths, attr))


    def run(self, *args, **kwargs):
        if kwargs:
            return base.run(*args, **kwargs)
        else:
            return base.run(*args)

    def logOSChanges(self, *args):
        base.logOSChanges(*args)

    def logIt(self, *args, **kwargs):
        #if pbar in args, pass to progress bar
        if 'pbar' in kwargs:
            ptype = kwargs.pop('pbar')
            msg = kwargs['msg'] if 'msg' in kwargs else args[0]
            Config.pbar.progress(ptype, msg)

        base.logIt(*args, **kwargs)


    def backupFile(self, inFile, destFolder=None, move=False, cur_content=''):

        if destFolder:
            if os.path.isfile(destFolder):
                destFile = destFolder
            else:
                inFolder, inName = os.path.split(inFile)
                destFile = os.path.join(destFolder, inName)
        else:
            destFile = inFile

        # check if file is the same
        if os.path.isfile(destFile):
            with open(destFile, 'rb') as f:
                old_content = f.read()
            if isinstance(cur_content, str):
                cur_content = cur_content.encode()
            if cur_content == old_content:
                return

        bc = 1
        while True:
            backupFile_fn = destFile+'.jans-{0}-{1}~'.format(Config.currentJansVersion, bc)
            if not os.path.exists(backupFile_fn):
                break
            bc += 1

        if os.path.exists(destFile):
            self.run(['mv' if move else 'cp', '-f', destFile, backupFile_fn])

        if not destFile.startswith('/opt'):
            self.logOSChanges("File %s was backed up as %s" % (destFile, backupFile_fn))


    def appendLine(self, line, fileName=False):

        self.backupFile(fileName)

        try:
            with open(fileName, 'a') as w:
                w.write('%s\n' % line)
        except:
            self.logIt("Error loading file %s" % fileName)


    # keep this for backward compatibility
    def detect_os_type(self):
        return Config.os_type, Config.os_version

    def detect_ip(self):
        detectedIP = None

        try:
            testSocket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            detectedIP = [(testSocket.connect(('8.8.8.8', 80)),
                           testSocket.getsockname()[0],
                           testSocket.close()) for s in [socket.socket(socket.AF_INET, socket.SOCK_DGRAM)]][0][1]
        except:
            self.logIt("No detected IP address", True)

        return detectedIP

    def get_ip(self):
        if Config.installed_instance:
            return Config.ip

        testIP = None
        detectedIP = Config.ip if Config.ip else self.detect_ip()

        if Config.noPrompt and detectedIP:
            return detectedIP

        while not testIP:
            if detectedIP:
                testIP = self.getPrompt("Enter IP Address", detectedIP)
            else:
                testIP = self.getPrompt("Enter IP Address")
            if not self.isIP(testIP):
                testIP = None
                print('ERROR: The IP Address is invalid. Try again\n')

        return testIP


    def detect_hostname(self):
        if not Config.ip:
            Config.ip = self.detect_ip()

        detectedHostname = None

        try:
            detectedHostname = socket.gethostbyaddr(Config.ip)[0]
        except:
            try:
                detectedHostname = os.popen("/bin/hostname").read().strip()
            except:
                self.logIt("No detected hostname", True)

        return detectedHostname

    def readFile(self, inFilePath, logError=True, rmode='r'):
        self.logIt("Reading file %s" % inFilePath)
        inFilePathText = None
        try:
            with open(inFilePath, rmode) as f:
                inFilePathText = f.read()
        except:
            if logError:
                self.logIt("Error reading %s" % inFilePathText, True)

        return inFilePathText

    def writeFile(self, outFilePath, text, backup=True):
        self.logIt("Writing file %s" % outFilePath)

        dir_name = os.path.dirname(outFilePath)
        if not os.path.exists(dir_name):
            self.run([paths.cmd_mkdir, '-p', dir_name])

        inFilePathText = None
        if backup:
            self.backupFile(outFilePath, cur_content=text)
        try:
            with open(outFilePath, 'w') as w:
                w.write(text)
        except:
            self.logIt("Error writing %s" % inFilePathText, True)

        return inFilePathText

    def loadJson(self, fn):
        json_content = self.readFile(fn)
        json_data =json.loads(json_content, object_pairs_hook=OrderedDict)
        return json_data

    def insertLinesInFile(self, inFilePath, index, text):
            inFilePathLines = None
            try:
                inFilePathLines = self.readFile(inFilePath).splitlines()
                try:
                    inFilePathLines.insert(index, text)
                    inFileText = ''.join(inFilePathLines)
                    self.writeFile(inFilePath, inFileText)
                except:
                    self.logIt("Error writing %s" % inFilePathLines, True)
            except:
                self.logIt("Error reading %s" % inFilePathLines, True)

    def commentOutText(self, text):
        textLines = text.splitlines()

        lines = []
        for textLine in textLines:
            lines.append('#%s' % textLine)

        return "\n".join(lines)

    def replaceInText(self, text, pattern, update):
        rePattern = re.compile(pattern,  flags=re.DOTALL | re.M)
        return rePattern.sub(update, text)

    def applyChangesInFiles(self, changes):
        self.logIt("Applying changes to %s files..." % changes['name'])
        for change in changes['files']:
            file = change['path']

            text = self.readFile(file)
            file_backup = '%s.bak' % file
            self.writeFile(file_backup, text)
            self.logIt("Created backup of %s file %s..." % (changes['name'], file_backup))

            for replace in change['replace']:
                text = self.replaceInText(text, replace['pattern'], replace['update'])

            self.writeFile(file, text)
            self.logIt("Wrote updated %s file %s..." % (changes['name'], file))


    def copyFile(self, inFile, destFolder, backup=True):
        if os.path.isfile(inFile):
            with open(inFile, 'rb') as f:
                cur_content = f.read()
        else:
            cur_content = ''

        if backup:
            self.backupFile(inFile, destFolder, cur_content=cur_content)
        self.logIt("Copying file {} to {}".format(inFile, destFolder))
        try:
            shutil.copy(inFile, destFolder)
            self.logIt("Copied %s to %s" % (inFile, destFolder))
        except:
            self.logIt("Error copying %s to %s" % (inFile, destFolder), True)

    def copyTree(self, src, dst, overwrite=False):
        try:
            if not os.path.exists(dst):
                os.makedirs(dst)

            for item in os.listdir(src):
                s = os.path.join(src, item)
                d = os.path.join(dst, item)
                if os.path.isdir(s):
                    self.copyTree(s, d, overwrite)
                else:
                    if overwrite and os.path.exists(d):
                        self.removeFile(d)

                    if not os.path.exists(d) or os.stat(s).st_mtime - os.stat(d).st_mtime > 1:
                        with open(s, 'rb') as fi:
                            cur_content = fi.read()
                        self.backupFile(s, d, cur_content=cur_content)    
                        shutil.copy2(s, d)
                        

            self.logIt("Copied tree %s to %s" % (src, dst))
        except:
            self.logIt("Error copying tree %s to %s" % (src, dst), True)

    def createDirs(self, name):
        try:
            if not os.path.exists(name):
                os.makedirs(name, 0o700)
                self.logIt('Created dir: %s' % name)
        except:
            self.logIt("Error making directory %s" % name, True)

    def removeDirs(self, name):
        try:
            if os.path.exists(name):
                shutil.rmtree(name)
                self.logIt('Removed dir: %s' % name)
        except:
            self.logIt("Error removing directory %s" % name, True)

    def removeFile(self, fileName):
        try:
            if os.path.exists(fileName):
                os.remove(fileName)
                self.logIt('Removed file: %s' % fileName)
        except:
            self.logIt("Error removing file %s" % fileName, True)

    def parse_url(self, url):
        o = urlparse(url)
        return o.hostname, o.port


    def getPW(self, size=12, chars=string.ascii_uppercase + string.digits + string.ascii_lowercase, special=''):
        
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
                        (nsletter, string.ascii_lowercase)):
            
                random_password += [random.choice(rc) for _ in range(n)]
            
        random.shuffle(random_password)
                
        return ''.join(random_password)

    def isIP(self, address):
        try:
            socket.inet_aton(address)
            return True
        except socket.error:
            return False

    def check_email(self, email):
        return re.match('^[_a-z0-9-]+(\.[_a-z0-9-]+)*@[a-z0-9-]+(\.[a-z0-9-]+)*(\.[a-z]{2,})$', email, re.IGNORECASE)

    def checkPassword(self, pwd):
        return re.search('^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*\W)[a-zA-Z0-9\S]{6,}$', pwd)

    def ldap_encode(self, password):
        salt = os.urandom(4)
        sha = hashlib.sha1(password.encode('utf-8'))
        sha.update(salt)
        digest_ = sha.digest()
        b64encoded = base64.b64encode(digest_+salt).decode('utf-8')
        encrypted_password = '{{SSHA}}{0}'.format(b64encoded)
        return encrypted_password

    def reindent(self, text, num_spaces):
        text = text.splitlines()
        text = [(num_spaces * ' ') + line.lstrip() for line in text]
        text = '\n'.join(text)

        return text
        
    def createLdapPw(self):
        try:
            with open(Config.ldapPassFn, 'w') as w:
                w.write(Config.ldapPass)
            self.run([paths.cmd_chown, 'ldap:ldap', Config.ldapPassFn])
        except:
            self.logIt("Error writing temporary LDAP password.")

    def deleteLdapPw(self):
        if Config.get('ldapPassFn') and os.path.isfile(Config.ldapPassFn):
            os.remove(Config.ldapPassFn)

    def getMappingType(self, mtype):
        location = []
        for group in Config.mappingLocations:
            if group != 'default' and Config.mappingLocations[group] == mtype:
                location.append(group)

        return location

    def merge_dicts(self, *dict_args):
        result = {}
        for dictionary in dict_args:
            result.update(dictionary)
            if 'non_setup_properties' in dictionary:
                result.update(dictionary['non_setup_properties'])
        return result

    def get_filepaths(self, directory):
        file_paths = []

        for root, directories, files in os.walk(directory):
            for filename in files:
                # filepath = os.path.join(root, filename)
                file_paths.append(filename)

        return file_paths

    def fomatWithDict(self, text, dictionary):
        text = re.sub(r"%([^\(])", r"%%\1", text)
        text = re.sub(r"%$", r"%%", text)  # There was a % at the end?

        return text % dictionary


    def renderTemplateInOut(self, filePath, templateFolder, outputFolder, me='', pystring=False):
        fn = os.path.basename(filePath)
        in_fp = os.path.join(templateFolder, fn) 
        self.logIt("Rendering template %s" % in_fp)
        template_text = self.readFile(in_fp)

        # Create output folder if needed
        if not os.path.exists(outputFolder):
            os.makedirs(outputFolder)

        format_dict = self.merge_dicts(Config.__dict__, Config.templateRenderingDict)
        for k in format_dict:
            if isinstance(format_dict[k], bool):
                format_dict[k] = str(format_dict[k]).lower()

        if pystring:
            rendered_text = Template(template_text).substitute(format_dict)
        else:
            rendered_text = self.fomatWithDict(template_text, format_dict)
        out_fp = os.path.join(outputFolder, fn)

        self.writeFile(out_fp, rendered_text)

    def renderTemplate(self, filePath):
        self.renderTemplateInOut(filePath, Config.templateFolder, Config.outputFolder)

    def createUser(self, userName, homeDir, shell='/bin/bash'):

        try:
            useradd = '/usr/sbin/useradd'
            cmd = [useradd, '--system', '--user-group', '--shell', shell, userName]
            if homeDir:
                cmd.insert(-1, '--create-home')
                cmd.insert(-1, '--home-dir')
                cmd.insert(-1, homeDir)
            else:
                cmd.insert(-1, '--no-create-home')
            self.run(cmd)
            if homeDir:
                self.logOSChanges("User %s with homedir %s was created" % (userName, homeDir))
            else:
                self.logOSChanges("User %s without homedir was created" % (userName))
        except:
            self.logIt("Error adding user", True)

    def createGroup(self, groupName):
        try:
            groupadd = '/usr/sbin/groupadd'
            self.run([groupadd, groupName])
            self.logOSChanges("Group %s was created" % (groupName))
        except:
            self.logIt("Error adding group", True)

    def addUserToGroup(self, groupName, userName):
        try:
            usermod = '/usr/sbin/usermod'
            self.run([usermod, '-a', '-G', groupName, userName])
            self.logOSChanges("User %s was added to group %s" % (userName,groupName))
        except:
            self.logIt("Error adding group", True)

    def fix_init_scripts(self, serviceName, initscript_fn):
        if base.snap:
            return

        changeTo = None

        couchbase_mappings = self.getMappingType('couchbase')

        if Config.persistence_type == 'couchbase' or 'default' in couchbase_mappings:
            changeTo = 'couchbase-server'

        if Config.get('wrends_install') == InstallTypes.REMOTE or Config.get('cb_install') == InstallTypes.REMOTE:
            changeTo = ''

        if serviceName in Config.service_requirements:
            if changeTo != None:
                for service in Config.service_requirements:
                    Config.service_requirements[service][0] = Config.service_requirements[service][0].replace('opendj', changeTo)

            with open(initscript_fn) as f:
                initscript = f.readlines()

            for i,l in enumerate(initscript):
                if l.startswith('# Provides:'):
                    initscript[i] = '# Provides:          {0}\n'.format(serviceName)
                elif l.startswith('# description:'):
                    initscript[i] = '# description: Jetty 9 {0}\n'.format(serviceName)
                elif l.startswith('# Required-Start:'):
                    initscript[i] = '# Required-Start:    $local_fs $network {0}\n'.format(Config.service_requirements[serviceName][0])
                elif l.startswith('# chkconfig:'):
                    initscript[i] = '# chkconfig: 345 {0} {1}\n'.format(Config.service_requirements[serviceName][1], 100 - Config.service_requirements[serviceName][1])

            if (base.clone_type == 'rpm' and base.os_initdaemon == 'systemd') or base.deb_sysd_clone:
                service_init_script_fn = os.path.join(Config.distFolder, 'scripts', serviceName)
            else:
                service_init_script_fn = os.path.join('/etc/init.d', serviceName)

            with open(service_init_script_fn, 'w') as W:
                W.write(''.join(initscript))

            self.run([paths.cmd_chmod, '+x', service_init_script_fn])

    def load_certificate_text(self, filePath):
        self.logIt("Load certificate %s" % filePath)
        certificate_text = self.readFile(filePath)
        certificate_text = certificate_text.replace('-----BEGIN CERTIFICATE-----', '').replace('-----END CERTIFICATE-----', '').strip()
        return certificate_text

    def render_templates_folder(self, templatesFolder, ignoredirs=[], ignorefiles=[]):
        self.logIt("Rendering templates folder: %s" % templatesFolder)
        #coucbase_dict = self.couchbaseDict()

        def in_ignoredirs(p):
            for idir in ignoredirs:
                if p.as_posix().startswith(idir):
                    return True

        tp = Path(templatesFolder)
        for te in tp.rglob('*'):
            if in_ignoredirs(te):
                continue

            if te.is_file() and te.name in ignorefiles:
                continue

            if te.is_file() and not te.name.endswith('.nrnd'):
                self.logIt("Rendering template {}".format(te))
                rp = te.relative_to(Config.templateFolder)
                output_dir = rp.parent
                template_name = rp.name

                fullOutputDir = Path(Config.outputFolder, output_dir)
                fullOutputFile = Path(Config.outputFolder, rp)

                if not fullOutputDir.exists():
                    fullOutputDir.mkdir(parents=True, exist_ok=True)

                template_text = te.read_text()
                rendered_text = template_text % self.merge_dicts(Config.templateRenderingDict, Config.__dict__)
                self.logIt("Writing rendered template {}".format(fullOutputFile))
                fullOutputFile.write_text(rendered_text)

    def add_yacron_job(self, command, schedule, name=None, args={}):
        import ruamel.yaml

        if not name:
            name = command

        yacron_yaml_fn = os.path.join(base.snap_common, 'etc/cron-jobs.yaml')

        yml_str = self.readFile(yacron_yaml_fn)
        yacron_yaml = ruamel.yaml.load(yml_str, ruamel.yaml.RoundTripLoader)

        if not yacron_yaml:
            yacron_yaml = {'jobs': []}

        if 'jobs' not in yacron_yaml:
            yacron_yaml['jobs'] = []

        job = { 'command': command, 'schedule': schedule, 'name': name }
        job.update(args)
        
        yacron_yaml['jobs'].append(job)

        yml_str = ruamel.yaml.dump(yacron_yaml, Dumper=ruamel.yaml.RoundTripDumper)
        self.writeFile(yacron_yaml_fn, yml_str)
