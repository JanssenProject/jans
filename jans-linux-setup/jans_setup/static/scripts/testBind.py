#!/usr/bin/python

# The MIT License (MIT)
#
# Copyright (c) 2014 Gluu
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.

import sys, ldap, getpass

host = "localhost"
port = 1636
ssl = True
bindDN = "cn=directory manager"
bindPW = "$ Enter the password before running the script"
base = "o=gluu"
scope =  ldap.SCOPE_SUBTREE
attrVal = None
userPassword = None
attr = "uid"

# GET THE ARGS / PROMPT FOR ARGS IF NONE PROVIDED
try:
    attrVal = sys.argv[1]
    userPassword = sys.argv[2]
except:
    attrVal = raw_input("Enter %s: " % attr)
    userPassword = getpass.getpass("Enter password: ")

# CREAT THE FILTER: FOR EXAMPLE UID=*FOO* ; SUBSTRING SEARCH
filter = "%s=*%s*" % (attr, attrVal)

# SPECIFY SSL
protocol = "ldap"
if ssl:
    options = [(ldap.OPT_X_TLS_REQUIRE_CERT, ldap.OPT_X_TLS_NEVER)]
    ldap.set_option(*options[0])
    protocol = "ldaps"

l = ldap.initialize("%s://%s:%s" % (protocol, host, port))
l.protocol_version = ldap.VERSION3
l.simple_bind_s(bindDN, bindPW)
res = l.search_s(base, scope, filter)
l.unbind_s()

if len(res)==0:
    print "%s not found" % attrVal
    sys.exit(1)

if len(res)>1:
    print "Non deterministic uid. Found:"
    for tup in res:
        print "\t%s" % tup[0]
    sys.exit(2)

dn = res[0][0]

# IF ONLY 1 MATCH IS FOUND: PRINT DN AND BIND AS THAT USER WITH THE DN
print "Binding as: %s" % dn
l = ldap.initialize("%s://%s:%s" % (protocol, host, port))
l.protocol_version = ldap.VERSION3
l.simple_bind_s(dn, userPassword)
