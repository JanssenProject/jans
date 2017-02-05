#!/usr/bin/python

import sys
import string
import random
import commands
import random
import getpass
import os

class ReplicationTool:
    def __init__(self):
        self.replication_type = None
        self.slapd_conf = '/opt/symas/etc/openldap/slapd.conf'
        self.master_list = []
        self.slave_list = []
        self.encode_pw = """/opt/opendj/bin/encode-password -c %s -s SSHA"""
        self.repMgrDN = 'cn=replication manager,o=gluu'
        self.repMgrPW = ''

    def duplicateServer(self, server):
        all_servers = self.master_list + self.slave_list
        if server in all_servers:
            return True
        return False

    def getMasters(self):
        print
        while True:
            master_server = raw_input("Master host:port <return> to finish : " ).strip().lower()
            if len(master_server):
                    try:
                        host, port = master_server.split(":") 
                        test = int(port)
                        if self.duplicateServer(master_server):
                            print "Error: duplicate server"
                            continue
                    except:
                        print "Error parsing server and port... please try again\n"
                        continue
                    self.master_list.append(master_server)
            else:
                if not len(self.master_list):
                    print "You must enter at least one master server\n"
                    continue
                print "\nMasters:"
                for master_server in self.master_list:
                    print '\t%s' % master_server
                test = raw_input("\nCorrect? [y/n] ").lower()[0]
                if test != 'y':
                    self.master_list = []
                    continue
                else:
                    print
                    break

    def getSlaves(self):
        while True:
            slave_server = raw_input("Slave host:port or <return> to finish : " )
            if len(slave_server.strip()):
                    try:
                        host, port = slave_server.split(":") 
                        test = int(port)
                        if self.duplicateServer(slave_server):
                            print "Error: duplicate server"
                            continue
                    except:
                        print "Error parsing server and port... please try again\n"
                        continue
                    self.slave_list.append(slave_server)
            else:
                if not len(self.slave_list):
                    print "You must enter at least one slave server\n"
                    continue
                print "\nSlaves:"
                for slave_server in self.slave_list:
                    print '\t%s' % slave_server
                test = raw_input("\nCorrect? [y/n] ").lower()[0]
                if test != 'y':
                    self.slave_list = []
                    continue
                else:
                    print
                    break

    def beginsWith(self, original, arg):
        length = len(arg)
        if original[:length] == arg: return True
        return False
    
    def encodePW(self, pw):
        output = commands.getoutput(self.encode_pw % pw)
        return output.split('"')[1]
    
    def getPW(self, length):
        return ''.join([random.choice(string.ascii_letters + string.digits) for n in xrange(length)])

    def getRepMgrLDIF(self):
        return """\n\n\n# Replication Manager LDIF
dn: cn=replication Manager,o=gluu
objectclass: top
objectclass: person
cn: Replication Manager
sn: Replication Manager
userpassword: %s

""" % self.repMgrPW
    
    def getMasterConfigs(self):
        master_configs = {}
        for master in self.master_list:
            conf = []
            current_config = open(self.slapd_conf)
            lines = current_config.readlines()
            current_config.close()
            loadModuleFlag = False
            indexFlag = False
            overlayFlag = False
            for line in lines:
                printAfter = ""
                if not loadModuleFlag and self.beginsWith(line, 'moduleload'):
                    conf.append('moduleload syncprov.la')
                    loadModuleFlag = True
        
                if not indexFlag and self.beginsWith(line, 'index'):
                    printAfter = "index entryCSN,entryUUID eq"
                    indexFlag = True
        
                if not overlayFlag and self.beginsWith(line, 'overlay'):
                    conf.append("""overlay syncprov
syncprov-checkpoint 100 10
syncprov-sessionlog 10000
syncprov-reloadhint TRUE""")
                    overlayFlag = True

                conf.append(line.strip())
                if printAfter:
                    conf.append(printAfter)
            master_configs[master] = "\n".join(conf)
        return master_configs

    def getSlaveConfigs(self):
        slave_configs = {}
        for slave in self.slave_list:
            conf = []
            current_config = open(self.slapd_conf)
            lines = current_config.readlines()
            current_config.close()
            loadModuleFlag = False
            indexFlag = False
            overlayFlag = False
            directoryFlag = False
            for line in lines: 
                printAfter = ""
                if not loadModuleFlag and self.beginsWith(line, 'moduleload'):
                    conf.append("moduleload back_ldap.la")
                    loadModuleFlag = True

                if not overlayFlag and self.beginsWith(line, 'overlay'):
                    conf.append('overlay chain')
                    for master_server in self.master_list:
                        conf.append('chain-uri "ldap://%s/"' % master_server)
                    conf.append("""chain-idassert-bind bindmethod="simple"
binddn="cn=replication Manager,o=gluu"
credentials="%s"
mode="self"
chain-return-error TRUE""" % self.repMgrPW)
                    overlayFlag = True
    
                if not indexFlag and self.beginsWith(line, 'index'):
                    printAfter = "index entryCSN,entryUUID eq"
                    indexFlag = True
    
                if not directoryFlag and self.beginsWith(line, 'directory'):
                    conf.append("syncrepl")
                    conf.append("rid=1")
                    for master_server in self.master_list:
                        conf.append(" provider=ldap://%s" % master_server)
                    conf.append(""" binddn="cn=replication manager,o=gluu"
 credentials=%s
 bindmethod=simple
 searchbase="o=gluu"
 type=refreshAndPersist
 retry="60 +" """ % self.repMgrPW)
                    for master_server in self.master_list:
                        conf.append("updateref=ldap://%s/" % master_server)

                conf.append(line.strip())
                if printAfter: 
                    conf.append(printAfter)
            slave_configs[slave] = "\n".join(conf)
        return slave_configs

########################################################################
#                           MAIN PROGRAM                               #
########################################################################

r = ReplicationTool()

r.getMasters()
r.getSlaves()

pw = r.getPW(random.randint(10,15))
r.repMgrPW = r.encodePW(pw)
print "\n Replication Manager password is: %s\n" % pw

outFolder = "./output"
if not os.path.exists(outFolder):
    os.makedirs(outFolder)

master_dict = r.getMasterConfigs()
for master in master_dict.keys():
    fn = "%s/slapd.conf-%s" % (outFolder, master.replace(":", "_"))
    f = open(fn, 'a')
    f.write(master_dict[master])
    f.close()

slave_dict = r.getSlaveConfigs()
for slave in slave_dict.keys():
    fn = "%s/slapd.conf-%s" % (outFolder, slave.replace(":", "_"))
    f = open(fn, 'a')
    f.write(slave_dict[slave])
    f.close()

