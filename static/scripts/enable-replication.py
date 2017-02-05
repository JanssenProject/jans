#!/usr/bin/python

import sys, string, random, commands, random, getpass

class ReplicationTool:
    def __init__(self):
        self.replication_type = None
        self.slapd_conf = '/opt/symas/etc/openldap/slapd.conf'
        self.master_list = []
        self.encode_pw = """/opt/opendj/bin/encode-password -c %s -s SSHA"""
        self.repMgrDN = 'cn=replication manager,o=gluu'
        self.repMgrPW = ''

    def getMasters(self):
        while True:
            master_server = raw_input("Enter host:port of master <return> to finish : " )
            if len(master_server.strip()):
                    try:
                        host, port = master_server.split(":") 
                        test = int(port)
                    except:
                        print "Error parsing server and port... please try again\n"
                        continue
                    self.master_list.append(master_server)
            else:
                if not len(self.master_list):
                    print "You must enter at least one master server\n"
                    continue
                print "\nMaster Servers:"
                for master_server in self.master_list:
                    print '\t%s' % master_server
                test = raw_input("\nCorrect? [y/n] ").lower()[0]
                if test != 'y':
                    self.master_list = []
                    continue
                else:
                    break

    def getRepMgrPW(self):
        while True:
            pw1= getpass.getpass("Enter replication manager password: ")
            pw2= getpass.getpass("re-enter password: ")
            if pw1 != pw2:
                print "Passwords don't match, try again"
                continue
            if len(pw1)>4:
                self.repMgrPW = self.encodePW(pw1)
                print
                break
            else:
                print "too short... try again\n"

    def beginsWith(self, original, arg):
        length = len(arg)
        if original[:length] == arg: return True
        return False
    
    def encodePW(self, pw):
        output = commands.getoutput(self.encode_pw % pw)
        return output.split('"')[1]
    
    def getPW(self, length):
        return ''.join([random.choice(string.ascii_letters + string.digits) for n in xrange(length)])

    def printMasterConf(self, lines):
        loadModuleFlag = False
        indexFlag = False
        overlayFlag = False
        for line in lines:
            printAfter = ""
            if not loadModuleFlag and self.beginsWith(line, 'moduleload'):
                print 'moduleload syncprov.la'
                loadModuleFlag = True
        
            if not indexFlag and self.beginsWith(line, 'index'):
                printAfter = "index entryCSN,entryUUID eq"
                indexFlag = True
        
            if not overlayFlag and self.beginsWith(line, 'overlay'):
                print """overlay syncprov
syncprov-checkpoint 100 10
syncprov-sessionlog 10000
syncprov-reloadhint TRUE"""
                overlayFlag = True

            print line.strip()
            if printAfter:
                print printAfter

        pw = self.getPW(random.randint(10,15))
        print """\n\n\n# Replication Manager LDIF
# dn: cn=replication Manager,o=gluu
# objectclass: top
# objectclass: person
# cn: Replication Manager
# sn: Replication Manager
# userpassword: %s
#
""" % self.encodePW(pw)
        print "\n Replication Manager password is: %s\n" % pw
    

    def printSlaveConf(self, lines):
        loadModuleFlag = False
        indexFlag = False
        overlayFlag = False
        directoryFlag = False
        for line in lines: 
            printAfter = ""
            if not loadModuleFlag and self.beginsWith(line, 'moduleload'):
                print "moduleload back_ldap.la"
                loadModuleFlag = True

            if not overlayFlag and self.beginsWith(line, 'overlay'):
                print 'overlay chain'
                for master_server in self.master_list:
                    print 'chain-uri "ldap://%s/"' % master_server
                print """chain-idassert-bind bindmethod="simple"
binddn="cn=replication Manager,o=gluu"
credentials="%s"
mode="self"
chain-return-error TRUE""" % self.repMgrPW 
                overlayFlag = True
    
            if not indexFlag and self.beginsWith(line, 'index'):
                printAfter = "index entryCSN,entryUUID eq"
                indexFlag = True
    
            if not directoryFlag and self.beginsWith(line, 'directory'):
                print "syncrepl"
                print "rid=1"
                for master_server in self.master_list:
                    print " provider=ldap://%s" % master_server
                print """ binddn="cn=replication manager,o=gluu"
 credentials=%s
 bindmethod=simple
 searchbase="o=gluu"
 type=refreshAndPersist
 retry="60 +" """ % self.repMgrPW
                for master_server in self.master_list:
                    print "updateref=ldap://%s/" % master_server

            print line.strip()
            if printAfter: 
                print printAfter

########################################################################
#                           MAIN PROGRAM                               #
########################################################################

r = ReplicationTool()

try:
    r.replication_type = sys.argv[1].lower()
    if not r.replication_type in ['master', 'slave']:
        raise Exception
except:
    print '  ** Error: must specify "master" or "slave" **\n'
    sys.exit()

current_config = open(r.slapd_conf)
lines = current_config.readlines()
current_config.close()

if r.replication_type == 'master':
    r.printMasterConf(lines)
if r.replication_type == 'slave':
    r.getMasters()
    r.getRepMgrPW()
    r.printSlaveConf(lines)

