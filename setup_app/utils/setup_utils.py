import os
import sys
import time
import subprocess
import traceback
import re
import socket
import shutil
import uuid
import base64
import json
import string
import random

from urllib.parse import urlparse

from setup_app import paths

from setup_app.pylib.pyDes import triple_des, ECB, PAD_PKCS5
from setup_app.utils.base import logIt, logOSChanges, run
from setup_app.static import InstallTypes
from setup_app.config import Config

class SetupUtils:

    def __init__(self):
        self.logIt = logIt
        self.logOSChanges = logOSChanges
        self.run = run

    def obscure(self, data=""):
        engine = triple_des(Config.encode_salt, ECB, pad=None, padmode=PAD_PKCS5)
        data = data.encode('ascii')
        en_data = engine.encrypt(data)
        encoded_pw = base64.b64encode(en_data)
        return encoded_pw.decode('utf-8')

    def backupFile(self, inFile, destFolder=None):

        if destFolder:
            if os.path.isfile(destFolder):
                destFile = destFolder
            else:
                inFolder, inName = os.path.split(inFile)
                destFile = os.path.join(destFolder, inName)
        else:
            destFile = inFile

        bc = 1
        while True:
            backupFile_fn = destFile+'.gluu-{0}-{1}~'.format(Config.currentGluuVersion, bc)
            if not os.path.exists(backupFile_fn):
                break
            bc += 1

        if os.path.exists(destFile):
            self.run(['cp', '-f', destFile, backupFile_fn])

        if not destFile.startswith('/opt'):
            self.logOSChanges("File %s was backed up as %s" % (destFile, backupFile_fn))


    def appendLine(self, line, fileName=False):

        self.backupFile(fileName)

        try:
            with open(fileName, 'a') as w:
                w.write('%s\n' % line)
        except:
            self.logIt("Error loading file %s" % fileName)

    def get_ssl_subject(self, ssl_fn):
        retDict = {}
        cmd = paths.opensslCommand + ' x509  -noout -subject -nameopt RFC2253 -in {}'.format(ssl_fn)
        s = self.run(cmd, shell=True)
        s = s.strip() + ','

        for k in ('emailAddress', 'CN', 'O', 'L', 'ST', 'C'):
            rex = re.search('{}=(.*?),'.format(k), s)
            retDict[k] = rex.groups()[0] if rex else ''

        return retDict


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
            self.logIt(traceback.format_exc(), True)

        return detectedIP

    def get_ip(self):
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
                self.logIt(traceback.format_exc(), True)

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
                self.logIt(traceback.format_exc(), True)

        return inFilePathText

    def writeFile(self, outFilePath, text):
        self.logIt("Writing file %s" % outFilePath)
        inFilePathText = None
        self.backupFile(outFilePath)
        try:
            with open(outFilePath, 'w') as w:
                w.write(text)
        except:
            self.logIt("Error writing %s" % inFilePathText, True)
            self.logIt(traceback.format_exc(), True)

        return inFilePathText

    def insertLinesInFile(self, inFilePath, index, text):        
            inFilePathLines = None                    
            try:            
                inFilePathLines = self.readFile(inFilePath).splitlines()            
                try:
                    self.backupFile(inFilePath)
                    inFilePathLines.insert(index, text)    
                    inFileText = ''.join(inFilePathLines)
                    self.writeFile(inFilePath, inFileText)
                except:            
                    self.logIt("Error writing %s" % inFilePathLines, True)            
                    self.logIt(traceback.format_exc(), True)
            except:            
                self.logIt("Error reading %s" % inFilePathLines, True)
                self.logIt(traceback.format_exc(), True)        
                    
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


    def copyFile(self, inFile, destFolder):
        self.backupFile(inFile, destFolder)
        self.logIt("Copying file {} to {}".format(inFile, destFolder))
        try:
            shutil.copy(inFile, destFolder)
            self.logIt("Copied %s to %s" % (inFile, destFolder))
        except:
            self.logIt("Error copying %s to %s" % (inFile, destFolder), True)
            self.logIt(traceback.format_exc(), True)

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
                        shutil.copy2(s, d)
                        self.backupFile(s, d)

            self.logIt("Copied tree %s to %s" % (src, dst))
        except:
            self.logIt("Error copying tree %s to %s" % (src, dst), True)
            self.logIt(traceback.format_exc(), True)

    def createDirs(self, name):
        try:
            if not os.path.exists(name):
                os.makedirs(name, 0o700)
                self.logIt('Created dir: %s' % name)
        except:
            self.logIt("Error making directory %s" % name, True)
            self.logIt(traceback.format_exc(), True)

    def removeDirs(self, name):
        try:
            if os.path.exists(name):
                shutil.rmtree(name)
                self.logIt('Removed dir: %s' % name)
        except:
            self.logIt("Error removing directory %s" % name, True)
            self.logIt(traceback.format_exc(), True)

    def removeFile(self, fileName):
        try:
            if os.path.exists(fileName):
                os.remove(fileName)
                self.logIt('Removed file: %s' % fileName)
        except:
            self.logIt("Error removing file %s" % fileName, True)
            self.logIt(traceback.format_exc(), True)

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
            with open(self.ldapPassFn, 'w') as w:
                w.write(self.ldapPass)
            self.run([paths.cmd_chown, 'ldap:ldap', self.ldapPassFn])
        except:
            self.logIt("Error writing temporary LDAP password.")
            self.logIt(traceback.format_exc(), True)

    def deleteLdapPw(self):
        if os.path.isfile(self.ldapPassFn):
            os.remove(self.ldapPassFn)

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

    def renderTemplateInOut(self, filePath, templateFolder, outputFolder):
        fn = os.path.basename(filePath)
        in_fp = os.path.join(templateFolder, fn) 
        self.logIt("Rendering template %s" % in_fp)
        template_text = self.readFile(in_fp)

        # Create output folder if needed
        if not os.path.exists(outputFolder):
            os.makedirs(outputFolder)

        rendered_text = self.fomatWithDict(template_text, self.merge_dicts(Config.__dict__, Config.templateRenderingDict))
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
            self.logIt(traceback.format_exc(), True)

    def createGroup(self, groupName):
        try:
            groupadd = '/usr/sbin/groupadd'
            self.run([groupadd, groupName])
            self.logOSChanges("Group %s was created" % (groupName))
        except:
            self.logIt("Error adding group", True)
            self.logIt(traceback.format_exc(), True)

    def addUserToGroup(self, groupName, userName):
        try:
            usermod = '/usr/sbin/usermod'
            self.run([usermod, '-a', '-G', groupName, userName])
            self.logOSChanges("User %s was added to group %s" % (userName,groupName))
        except:
            self.logIt("Error adding group", True)
            self.logIt(traceback.format_exc(), True)
