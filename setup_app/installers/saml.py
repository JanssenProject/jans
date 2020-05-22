import os
import glob
import shutil

from setup_app import paths
from setup_app.config.config import Config
from setup_app.utils.setup_utils import SetupUtils

class SamlInstaller(SetupUtils):

    def __init__(self):
        self.idp3_war = 'https://ox.gluu.org/maven/org/gluu/oxshibbolethIdp/%s/oxshibbolethIdp-%s.war' % (Config.oxVersion, Config.oxVersion)
        self.idp3_dist_jar = 'https://ox.gluu.org/maven/org/gluu/oxShibbolethStatic/%s/oxShibbolethStatic-%s.jar' % (Config.oxVersion, Config.oxVersion)
        self.idp3_cml_keygenerator = 'https://ox.gluu.org/maven/org/gluu/oxShibbolethKeyGenerator/%s/oxShibbolethKeyGenerator-%s.jar' % (Config.oxVersion, Config.oxVersion)

    def install(self):
        self.logIt("Install SAML Shibboleth IDP v3...")

        # Put latest SAML templates
        identityWar = 'identity.war'

        self.createDirs('%s/conf/shibboleth3' % self.gluuBaseFolder)
        self.createDirs('%s/identity/conf/shibboleth3/idp' % self.jetty_base)
        self.createDirs('%s/identity/conf/shibboleth3/sp' % self.jetty_base)

        # unpack IDP3 JAR with static configs
        self.run([self.cmd_jar, 'xf', self.distGluuFolder + '/shibboleth-idp.jar'], '/opt')
        self.removeDirs('/opt/META-INF')

        if self.mappingLocations['user'] == 'couchbase':
            self.templateRenderingDict['idp_attribute_resolver_ldap.search_filter'] = '(&(|(lower(uid)=$requestContext.principalName)(mail=$requestContext.principalName))(objectClass=gluuPerson))'

        # Process templates
        self.renderTemplateInOut(self.idp3_configuration_properties, self.staticIDP3FolderConf, self.idp3ConfFolder)
        self.renderTemplateInOut(self.idp3_configuration_ldap_properties, self.staticIDP3FolderConf, self.idp3ConfFolder)
        self.renderTemplateInOut(self.idp3_configuration_saml_nameid, self.staticIDP3FolderConf, self.idp3ConfFolder)
        self.renderTemplateInOut(self.idp3_configuration_services, self.staticIDP3FolderConf, self.idp3ConfFolder)
        self.renderTemplateInOut(self.idp3_configuration_password_authn, self.staticIDP3FolderConf + '/authn', self.idp3ConfFolder + '/authn')

        # load certificates to update metadata
        self.templateRenderingDict['idp3EncryptionCertificateText'] = self.load_certificate_text(self.certFolder + '/idp-encryption.crt')
        self.templateRenderingDict['idp3SigningCertificateText'] = self.load_certificate_text(self.certFolder + '/idp-signing.crt')
        # update IDP3 metadata
        self.renderTemplateInOut(self.idp3_metadata, self.staticIDP3FolderMetadata, self.idp3MetadataFolder)

        self.idpWarFullPath = '%s/idp.war' % self.distGluuFolder

        jettyIdpServiceName = 'idp'
        jettyIdpServiceWebapps = '%s/%s/webapps' % (self.jetty_base, jettyIdpServiceName)

        self.installJettyService(self.jetty_app_configuration[jettyIdpServiceName], True, True)
        self.copyFile('%s/idp.war' % self.distGluuFolder, jettyIdpServiceWebapps)

        # Prepare libraries needed to for command line IDP3 utilities
        self.install_saml_libraries()

        # generate new keystore with AES symmetric key
        # there is one throuble with Shibboleth IDP 3.x - it doesn't load keystore from /etc/certs. It accepts %{idp.home}/credentials/sealer.jks  %{idp.home}/credentials/sealer.kver path format only.
        cmd = [self.cmd_java,'-classpath', '"{}"'.format(os.path.join(self.idp3Folder,'webapp/WEB-INF/lib/*')),
                'net.shibboleth.utilities.java.support.security.BasicKeystoreKeyStrategyTool',
                '--storefile', os.path.join(self.idp3Folder,'credentials/sealer.jks'),
                '--versionfile',  os.path.join(self.idp3Folder, 'credentials/sealer.kver'),
                '--alias secret',
                '--storepass', self.shibJksPass]
            
        self.run(' '.join(cmd), shell=True)

        # chown -R jetty:jetty /opt/shibboleth-idp
        # self.run([self.cmd_chown,'-R', 'jetty:jetty', self.idp3Folder], '/opt')
        self.run([self.cmd_chown, '-R', 'jetty:jetty', jettyIdpServiceWebapps], '/opt')


        if self.persistence_type == 'couchbase':
            self.saml_couchbase_settings()
        elif self.persistence_type == 'hybrid':
            couchbase_mappings = self.getMappingType('couchbase')
            if 'user' in couchbase_mappings:
                self.saml_couchbase_settings()


    def install_saml_libraries(self):
        # Unpack oxauth.war to get bcprov-jdk16.jar
        idpWar = 'idp.war'
        distIdpPath = '%s/idp.war' % self.distGluuFolder

        tmpIdpDir = '%s/tmp/tmp_idp' % self.distFolder

        self.logIt("Unpacking %s..." % idpWar)
        self.removeDirs(tmpIdpDir)
        self.createDirs(tmpIdpDir)

        self.run([self.cmd_jar,
                  'xf',
                  distIdpPath], tmpIdpDir)

        # Copy libraries into webapp
        idp3WebappLibFolder = "%s/WEB-INF/lib" % self.idp3WebappFolder
        self.createDirs(idp3WebappLibFolder)
        self.copyTree('%s/WEB-INF/lib' % tmpIdpDir, idp3WebappLibFolder)

        self.removeDirs(tmpIdpDir)


    def saml_couchbase_settings(self):
        # Add couchbase bean to global.xml
        couchbase_bean_xml_fn = '%s/couchbase/couchbase_bean.xml' % self.staticFolder
        global_xml_fn = '%s/global.xml' % self.idp3ConfFolder
        couchbase_bean_xml = self.readFile(couchbase_bean_xml_fn)
        global_xml = self.readFile(global_xml_fn)
        global_xml = global_xml.replace('</beans>', couchbase_bean_xml+'\n\n</beans>')
        self.writeFile(global_xml_fn, global_xml)

        # Add datasource.properties to idp.properties
        idp3_configuration_properties_fn = os.path.join(self.idp3ConfFolder, self.idp3_configuration_properties)

        with open(idp3_configuration_properties_fn) as r:
            idp3_properties = r.readlines()

        for i,l in enumerate(idp3_properties[:]):
            if l.strip().startswith('idp.additionalProperties'):
                idp3_properties[i] = l.strip() + ', /conf/datasource.properties\n'

        new_idp3_props = ''.join(idp3_properties)
        self.writeFile(idp3_configuration_properties_fn, new_idp3_props)

        data_source_properties = os.path.join(self.outputFolder, self.data_source_properties)

        self.copyFile(data_source_properties, self.idp3ConfFolder)

    def create_folders(self):
        for folder in (Config.idp3Folder, Config.idp3MetadataFolder, Config.idp3MetadataCredentialsFolder,
                        Config.idp3LogsFolder, Config.idp3LibFolder, Config.idp3ConfFolder, 
                        Config.idp3ConfAuthnFolder, Config.idp3CredentialsFolder, iConfig.dp3WebappFolder):
            
            self.run([paths.cmd_mkdir, '-p', folder])

        self.run([paths.cmd_chown, '-R', 'jetty:jetty', Config.idp3Folder])

    def download_files(self):
        self.pbar.progress('saml', "Downloading Shibboleth IDP v3 war file", False)
        self.run([paths.cmd_wget, self.idp3_war, '--no-verbose', '-c', '--retry-connrefused', '--tries=10', '-O', os.path.join(Config.distGluuFolder, 'idp.war')])
        self.pbar.progress('saml', "Downloading Shibboleth IDP v3 keygenerator", False)
        self.run([paths.cmd_wget, self.idp3_cml_keygenerator, '--no-verbose', '-c', '--retry-connrefused', '--tries=10', '-O', os.path.join(Config.distGluuFolder, 'idp3_cml_keygenerator.jar')])
        self.pbar.progress('saml', "Downloading Shibboleth IDP v3 binary distributive file", False)
        self.run([paths.cmd_wget, self.idp3_dist_jar, '--no-verbose', '-c', '--retry-connrefused', '--tries=10', '-O', os.path.join(Config.distGluuFolder, 'shibboleth-idp.jar')])
