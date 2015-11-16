#!/usr/bin/python

import sys, base64, json, ldap.modlist
from ldif import LDIFParser, LDIFWriter, CreateLDIF

fn = None
targetString = None
replaceString = None

logfile = "./update_config.log"

def log(s):
    f = open(logfile, 'a')
    f.write("%s\n" % s)
    f.close()

try:
    fn = sys.argv[1]
    targetString = sys.argv[2]
    replaceString = sys.argv[3]
except:
    pass

if not fn:
    print "Input ldif filename not found"
    sys.exit(2)

if not targetString:
    print "Target String not found"
    sys.exit(2)

if not replaceString:
    print "Replacement string not found"
    sys.exit(2)

json_attrs = [ 'oxAuthConfDynamic',
		'oxAuthConfStatic',
		'oxAuthConfWebKeys',
		'oxAuthConfErrors',
		'oxTrustConfApplication',
		'oxTrustConfCacheRefresh',
		'oxConfApplication'
             ]

class MyLDIF(LDIFParser):
    def __init__(self,input,output):
        LDIFParser.__init__(self,input)
        self.writer = LDIFWriter(output)

    def handle(self, dn, entry):
        changed = False 
        for attr in entry.keys():
            if attr in json_attrs:
                json_object = json.loads(entry[attr][0])
                for json_key in json_object.keys():
                    value = json_object[json_key]
                    if type(value) == type([]):
			if len(value) == 1:
                            value = value[0]
                    if type(value) != type(unicode("")):
                        continue
                    if value.find(targetString)>=0:
                        json_object[json_key] = json_object[json_key].replace(targetString, replaceString)
                        log("dn: %s\nattr: %s\nkey: %s\nvalue: %s\n" % (dn, attr, json_key, value))
                new_json = json.dumps(json_object)
                old_value = {attr: entry[attr]}
                new_value = {attr: [new_json]}
                mod_list = ldap.modlist.modifyModlist(old_value, new_value)
                print CreateLDIF(dn, mod_list, [attr])
                log("New JSON Object:\n %s" % new_json)
            else:
                updated = False
                updated_value = []
                for value in entry[attr]:
                    if value.find(targetString)>=0:
                        updated_value.append(value.replace(targetString, replaceString))
                        updated = True
                        log("dn: %s\nattr: %s\nvalue: %s\n" % (dn, attr, value))
                    else:
                        updated_value.append(value)
                if updated:
                    old_value = {attr: entry[attr]}
                    new_value = {attr: updated_value}
                    mod_list = ldap.modlist.modifyModlist(old_value, new_value)
                    print CreateLDIF(dn, mod_list)
                     
parser = MyLDIF(open(fn, 'rb'), sys.stdout)
parser.parse()

