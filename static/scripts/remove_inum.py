# Before running this script
# install python-ldap
# for ubuntu: apt-get install python-ldap
# for centos: yum install python-ldap
# dump ldap: /opt/opendj/bin/export-ldif --includeBranch "o=gluu" --backendID userRoot --ldifFile gluu.ldif
# replace /opt/opendj/config/schema/101-ox.ldif with newset one from https://raw.githubusercontent.com/GluuFederation/community-edition-setup/master/static/opendj/101-ox.ldif
# Run this script
# After running script update ldap: /opt/opendj/bin/import-ldif  -b "o=gluu" -n userRoot -l gluu_noinum.ldif -R gluu.ldif.rejects

import json

from ldif import LDIFParser, LDIFWriter
from ldap.dn import explode_dn, str2dn, dn2str

class MyLDIF(LDIFParser):
    def __init__(self, input_fd):
        LDIFParser.__init__(self, input_fd)
        self.DNs = []
        self.entries = {}
        self.inumOrg = None
        self.inumOrg_dn = None
        self.inumApllience = None
        self.inumApllience_dn = None

    def handle(self, dn, entry):
        if (dn != 'o=gluu') and (dn != 'ou=appliances,o=gluu'):
            self.DNs.append(dn)
            self.entries[dn] = entry
            
            if not self.inumOrg and 'gluuOrganization' in entry['objectClass']:
                self.inumOrg_dn  = dn
                dne = str2dn(dn)
                self.inumOrg = dne[0][0][1]

            if not self.inumApllience and 'gluuAppliance' in entry['objectClass']:
                self.inumApllience_dn = dn
                dne = str2dn(dn)
                self.inumApllience = dne[0][0][1]


ldif_parser = MyLDIF(open('gluu.ldif'))
ldif_parser.parse()

inumOrg_ou = 'o=' + ldif_parser.inumOrg
inumApllience_inum = 'inum='+ ldif_parser.inumApllience

processed_fp = open('gluu_noinum.ldif','w')
ldif_writer = LDIFWriter(processed_fp)

for dn in ldif_parser.DNs:
    dne = explode_dn(dn)

    new_entry = ldif_parser.entries[dn]

    if inumOrg_ou in dne:
        dne.remove(inumOrg_ou)

    if inumApllience_inum in dne:
        dne.remove(inumApllience_inum)
        dne.remove('ou=appliances')

        if dn == ldif_parser.inumApllience_dn:
            dne.insert(0,'ou=configuration')
            new_entry['ou'] = 'configuration'
            new_entry['objectClass'].insert(1, 'organizationalUnit')
            
        
    new_dn = ','.join(dne)

    if dn == ldif_parser.inumOrg_dn:
        new_entry['o'][0] = 'gluu'

    elif dn == ldif_parser.inumApllience_dn:
        new_entry['objectClass'].remove('gluuAppliance')
        new_entry['objectClass'].insert(1, 'gluuConfiguration')
        new_entry['ou'] = ['configuration']
        new_entry.pop('inum')

        oxIDPAuthentication = json.loads(new_entry['oxIDPAuthentication'][0])
        oxIDPAuthentication_config = json.loads(oxIDPAuthentication['config'])
        oxIDPAuthentication_config['baseDNs'][0] = 'ou=people,o=gluu'
        oxIDPAuthentication['config'] = json.dumps(oxIDPAuthentication_config)
        new_entry['oxIDPAuthentication'][0] = json.dumps(oxIDPAuthentication)
                   

    if 'oxAuthConfDynamic' in new_entry:
        oxAuthConfDynamic = json.loads(new_entry['oxAuthConfDynamic'][0])
        oxAuthConfDynamic.pop('organizationInum')
        oxAuthConfDynamic.pop('applianceInum')        
        oxAuthConfDynamic['clientAuthenticationFilters'][0]['baseDn'] = 'ou=clients,o=gluu'        
        new_entry['oxAuthConfDynamic'][0] = json.dumps(oxAuthConfDynamic)

        
        oxAuthConfStatic = {
                            "baseDn":{
                                "configuration":"ou=configuration,o=gluu",
                                "people":"ou=people,o=gluu",
                                "groups":"ou=groups,o=gluu",
                                "clients":"ou=clients,o=gluu",
                                "scopes":"ou=scopes,o=gluu",
                                "attributes":"ou=attributes,o=gluu",
                                "scripts": "ou=scripts,o=gluu",
                                "umaBase":"ou=uma,o=gluu",
                                "umaPolicy":"ou=policies,ou=uma,o=gluu",
                                "u2fBase":"ou=u2f,o=gluu",
                                "metric":"ou=statistic,o=metric",
                                "sectorIdentifiers": "ou=sector_identifiers,o=gluu"
                            }
                        }

    

        new_entry['oxAuthConfStatic'][0] = json.dumps(oxAuthConfStatic, indent=2)
        

    elif 'oxTrustConfApplication' in new_entry:
        oxTrustConfApplication = json.loads(new_entry['oxTrustConfApplication'][0])
        oxTrustConfApplication.pop('orgInum')
        oxTrustConfApplication.pop('applianceInum')
        new_entry['oxTrustConfApplication'][0] = json.dumps(oxTrustConfApplication)


    if 'ou=configuration,o=gluu' == new_dn:
        if not 'oxCacheConfiguration' in new_entry:
            continue

    for p in ('oxAuthClaim', 'owner', 'oxAssociatedClient', 
                'oxAuthUmaScope', 'gluuManagerGroup', 'member', 
                'oxPolicyScriptDn','oxScriptDn', 'oxAuthScope',
                'memberOf',):

        if p in new_entry:
            for i, oac in enumerate(new_entry[p][:]):
                new_entry[p][i] = oac.replace(inumOrg_ou+',','')


    
    ldif_writer.unparse(new_dn, new_entry)

processed_fp.close()

ox_ldap_prop_fn = '/etc/gluu/conf/ox-ldap.properties'

ox_ldap_prop = open(ox_ldap_prop_fn).read()
ox_ldap_prop = ox_ldap_prop.replace(inumApllience_inum+',ou=appliances,', '')

with open(ox_ldap_prop_fn,'w') as w:
    w.write(ox_ldap_prop)
