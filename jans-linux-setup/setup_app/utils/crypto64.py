import os
import re
import base64
import json

from collections import OrderedDict
from pathlib import Path

from Crypto.Cipher import AES
from setup_app.pylib.pyAes import AESCipher, AESKeyLength, AES_CBC_P, AES_ECB_P, AES_GCM_NP

from setup_app import paths
from setup_app import static
from setup_app.config import Config

class Crypto64:

    def get_ssl_subject(self, ssl_fn):
        retDict = {}
        cmd = paths.cmd_openssl + ' x509  -noout -subject -nameopt RFC2253 -in {}'.format(ssl_fn)
        s = self.run(cmd, shell=True)
        s = s.strip() + ','

        for k in ('emailAddress', 'CN', 'O', 'L', 'ST', 'C'):
            rex = re.search('{}=(.*?),'.format(k), s)
            retDict[k] = rex.groups()[0] if rex else ''

        return retDict

    def obscure(self, data=""):
        engine = self.get_engine()
        en_data = engine.encrypt(data)
        return base64.b64encode(en_data).decode()

    def unobscure(self, s=""):
        engine = self.get_engine()
        decrypted = engine.decrypt(base64.b64decode(s))
        return decrypted.decode()

    def gen_cert(self, suffix, password, user='root', cn=None, truststore_fn=None):
        self.logIt('Generating Certificate for %s' % suffix)
        key_with_password = '%s/%s.key.orig' % (Config.certFolder, suffix)
        key = '%s/%s.key' % (Config.certFolder, suffix)
        csr = '%s/%s.csr' % (Config.certFolder, suffix)
        public_certificate = '%s/%s.crt' % (Config.certFolder, suffix)
        if not truststore_fn:
            truststore_fn = Config.defaultTrustStoreFN

        self.run([paths.cmd_openssl,
                  'genrsa',
                  '-des3',
                  '-out',
                  key_with_password,
                  '-passout',
                  'pass:%s' % password,
                  '2048'
                  ])
        self.run([paths.cmd_openssl,
                  'rsa',
                  '-in',
                  key_with_password,
                  '-passin',
                  'pass:%s' % password,
                  '-out',
                  key
                  ])

        certCn = cn
        if certCn == None:
            certCn = Config.hostname

        self.run([paths.cmd_openssl,
                  'req',
                  '-new',
                  '-key',
                  key,
                  '-out',
                  csr,
                  '-subj',
                  '/C=%s/ST=%s/L=%s/O=%s/CN=%s/emailAddress=%s' % (Config.countryCode, Config.state, Config.city, Config.orgName, certCn, Config.admin_email)
                  ])
        self.run([paths.cmd_openssl,
                  'x509',
                  '-req',
                  '-days',
                  '365',
                  '-in',
                  csr,
                  '-signkey',
                  key,
                  '-out',
                  public_certificate
                  ])
        self.run([paths.cmd_chown, '%s:%s' % (user, user), key_with_password])
        self.run([paths.cmd_chmod, '700', key_with_password])
        self.run([paths.cmd_chown, '%s:%s' % (user, user), key])
        self.run([paths.cmd_chmod, '700', key])

        self.run([Config.cmd_keytool, "-import", "-trustcacerts", "-alias", "%s_%s" % (Config.hostname, suffix), \
                  "-file", public_certificate, "-keystore", truststore_fn, \
                  "-storepass", "changeit", "-noprompt"])

    def prepare_base64_extension_scripts(self, extensions=[]):
        self.logIt("Preparing scripts")
        extension_path = Path(Config.extensionFolder)
        for ep in extension_path.glob("**/*"):
            if ep.is_file() and ep.suffix in ['.py']:
                extension_type = ep.parent.name.lower()
                extension_name = ep.stem.lower()
                extension_script_name = '{}_{}'.format(extension_type, extension_name)

                if extensions and extension_script_name in extensions:
                    continue

                # Prepare key for dictionary
                base64_script_file = self.generate_base64_file(ep.as_posix(), 1)
                Config.templateRenderingDict[extension_script_name] = base64_script_file


    def generate_base64_file(self, fn, num_spaces):
        self.logIt('Loading file %s' % fn)
        plain_file_b64encoded_text = None
        try:
            plain_file_text = self.readFile(fn, rmode='rb')
            plain_file_b64encoded_text = base64.b64encode(plain_file_text).decode('utf-8').strip()
        except:
            self.logIt("Error loading file", True)

        if num_spaces > 0:
            plain_file_b64encoded_text = self.reindent(plain_file_b64encoded_text, num_spaces)

        return plain_file_b64encoded_text

    def generate_base64_ldap_file(self, fn):
        return self.generate_base64_file(fn, 1)

    def gen_keystore(self, suffix, keystoreFN, keystorePW, inKey, inCert, alias=None):

        self.logIt("Creating keystore %s" % suffix)
        # Convert key to pkcs12
        pkcs_fn = '%s/%s.pkcs12' % (Config.certFolder, suffix)
        self.run([paths.cmd_openssl,
                  'pkcs12',
                  '-export',
                  '-inkey',
                  inKey,
                  '-in',
                  inCert,
                  '-out',
                  pkcs_fn,
                  '-name',
                  alias or Config.hostname,
                  '-passout',
                  'pass:%s' % keystorePW
                  ])
        # Import p12 to keystore
        import_cmd = [Config.cmd_keytool,
                  '-importkeystore',
                  '-srckeystore',
                  '%s/%s.pkcs12' % (Config.certFolder, suffix),
                  '-srcstorepass',
                  keystorePW,
                  '-srcstoretype',
                  'PKCS12',
                  '-destkeystore',
                  keystoreFN,
                  '-deststorepass',
                  keystorePW,
                  '-deststoretype',
                  'JKS',
                  '-keyalg',
                  'RSA',
                  '-noprompt'
                  ]
        if alias:
            import_cmd += ['-alias', alias]

        self.run(import_cmd)


    def gen_openid_jwks_jks_keys(self, jks_path, jks_pwd, jks_create=True, key_expiration=None, dn_name=None, key_algs=None, enc_keys=None):
        self.logIt("Generating oxAuth OpenID Connect keys")

        if dn_name == None:
            dn_name = Config.default_openid_jks_dn_name

        if key_algs == None:
            key_algs = Config.default_sig_key_algs

        if key_expiration == None:
            key_expiration = Config.default_key_expiration

        if not enc_keys:
            enc_keys = Config.default_enc_key_algs

        # We can remove this once KeyGenerator will do the same
        if jks_create == True:
            self.logIt("Creating empty JKS keystore")
            # Create JKS with dummy key
            cmd = " ".join([Config.cmd_keytool,
                            '-genkey',
                            '-alias',
                            'dummy',
                            '-keystore',
                            jks_path,
                            '-storepass',
                            jks_pwd,
                            '-keypass',
                            jks_pwd,
                            '-dname',
                            '"%s"' % dn_name])
            self.run([cmd], shell=True)

            # Delete dummy key from JKS
            cmd = " ".join([Config.cmd_keytool,
                            '-delete',
                            '-alias',
                            'dummy',
                            '-keystore',
                            jks_path,
                            '-storepass',
                            jks_pwd,
                            '-keypass',
                            jks_pwd,
                            '-dname',
                            '"%s"' % dn_name])
            self.run([cmd], shell=True)

        cmd = " ".join([Config.cmd_java,
                        "-Dlog4j.defaultInitOverride=true",
                        "-cp", Config.non_setup_properties['oxauth_client_jar_fn'], 
                        Config.non_setup_properties['key_gen_path'],
                        "-keystore",
                        jks_path,
                        "-keypasswd",
                        jks_pwd,
                        "-sig_keys",
                        "%s" % key_algs,
                        "-enc_keys",
                        "%s" % enc_keys,
                        "-dnname",
                        '"%s"' % dn_name,
                        "-expiration",
                        "%s" % key_expiration])

        output = self.run([cmd], shell=True)

        if output:
            return output.splitlines()

    def export_openid_key(self, jks_path, jks_pwd, cert_alias, cert_path):
        self.logIt("Exporting oxAuth OpenID Connect keys")

        cmd = " ".join([Config.cmd_java,
                        "-Dlog4j.defaultInitOverride=true",
                        "-cp",
                        Config.non_setup_properties['oxauth_client_jar_fn'], 
                        Config.non_setup_properties['key_export_path'],
                        "-keystore",
                        jks_path,
                        "-keypasswd",
                        jks_pwd,
                        "-alias",
                        cert_alias,
                        "-exportfile",
                        cert_path])
        self.run(['/bin/sh', '-c', cmd])

    def write_openid_keys(self, fn, jwks):
        self.logIt("Writing oxAuth OpenID Connect keys")

        if not jwks:
            self.logIt("Failed to write oxAuth OpenID Connect key to %s" % fn)
            return

        self.backupFile(fn)

        try:
            jwks_text = '\n'.join(jwks)
            self.writeFile(fn, jwks_text)
            self.run([Config.cmd_chown, 'jetty:jetty', fn])
            self.run([Config.cmd_chmod, '600', fn])
            self.logIt("Wrote oxAuth OpenID Connect key to %s" % fn)
        except:
            self.logIt("Error writing command : %s" % fn, True)



    def generate_base64_string(self, lines, num_spaces):
        if not lines:
            return None

        plain_text = ''.join(lines)
        plain_b64encoded_text = base64.encodestring(plain_text.encode('utf-8')).decode('utf-8').strip()

        if num_spaces > 0:
            plain_b64encoded_text = self.reindent(plain_b64encoded_text, num_spaces)

        return plain_b64encoded_text

    def encode_passwords(self):
        self.logIt("Encoding passwords")

        try:
            if Config.get('ldapPass'):
                Config.encoded_ox_ldap_pw = self.obscure(Config.ldapPass)
            if Config.get('cb_password'):
                Config.encoded_cb_password = self.obscure(Config.cb_password)
            if Config.get('opendj_p12_pass'):
                Config.encoded_opendj_p12_pass = self.obscure(Config.opendj_p12_pass)
        except:
            self.logIt("Error encoding passwords", True, True)

    def encode_test_passwords(self):
        self.logIt("Encoding test passwords")
        hostname = Config.hostname.split('.')[0]
        try:
            Config.templateRenderingDict['oxauthClient_2_pw'] = Config.templateRenderingDict['oxauthClient_2_inum'] + '-' + hostname
            Config.templateRenderingDict['oxauthClient_2_encoded_pw'] = self.obscure(Config.templateRenderingDict['oxauthClient_2_pw'])

            Config.templateRenderingDict['oxauthClient_3_pw'] =  Config.templateRenderingDict['oxauthClient_3_inum'] + '-' + hostname
            Config.templateRenderingDict['oxauthClient_3_encoded_pw'] = self.obscure(Config.templateRenderingDict['oxauthClient_3_pw'])

            Config.templateRenderingDict['oxauthClient_4_pw'] = Config.templateRenderingDict['oxauthClient_4_inum'] + '-' + hostname
            Config.templateRenderingDict['oxauthClient_4_encoded_pw'] = self.obscure(Config.templateRenderingDict['oxauthClient_4_pw'])
        except:
            self.logIt("Error encoding test passwords", True)

    def get_engine(self):
        if Config.encode_alg is None or len(Config.encode_alg) == 0:
            return self.get_aes_engine(AES_GCM_NP, '256')
        alg_sep_array = re.split(":", Config.encode_alg)
        if len(alg_sep_array) == 0 or len(alg_sep_array) == 1 and alg_sep_array[0] == 'AES':
            return self.get_aes_engine(AES_GCM_NP, '256')
        elif len(alg_sep_array) == 3 and alg_sep_array[0] == 'AES':
            return self.get_aes_engine(alg_sep_array[1], alg_sep_array[2])
        else:
            raise AttributeError("wrong alg value: alg = " + alg)

    def get_aes_engine(self, mode, key_length):
        eff_mode = None
        eff_key_length = None
        if key_length == '128':
            eff_key_length = AESKeyLength.KL128
        elif key_length == '192':
            eff_key_length = AESKeyLength.KL192
        elif key_length == '256':
            eff_key_length = AESKeyLength.KL256
        else:
            raise AttributeError("wrong key_length value: key_length = " + key_length)
        if mode == AES_CBC_P:
            eff_mode = AES.MODE_CBC
        elif mode == AES_GCM_NP:
            eff_mode = AES.MODE_GCM
        elif mode == AES_ECB_P:
            eff_mode = AES.MODE_ECB
        else:
            raise AttributeError("this mode isn't supported: mode = " + mode)
        return AESCipher(eff_mode, eff_key_length, Config.encode_passw, Config.encode_salt) 
