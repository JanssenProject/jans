import os
import re
import glob
import ssl
import time

from xml.etree import ElementTree

from setup_app import paths
from setup_app.static import AppType, InstallOption
from setup_app.utils import base
from setup_app.config import Config
from setup_app.utils.properties_utils import PropertiesUtils
from setup_app.installers.jetty import JettyInstaller


class CasaInstaller(JettyInstaller):

    def __init__(self):
        self.service_name = 'casa'
        self.app_type = AppType.SERVICE
        self.install_type = InstallOption.OPTONAL
        self.install_var = 'installCasa'
        self.register_progess()
        
        self.source_files = [
                (os.path.join(Config.distJansFolder, 'casa.war'), 'https://ox.gluu.org/maven/org/gluu/casa/{0}/casa-{0}.war'.format(Config.oxVersion))
                ]

        self.templates_folder = os.path.join(Config.templateFolder, 'casa')
        self.output_folder = os.path.join(Config.outputFolder, 'casa')
        self.ldif = os.path.join(Config.outputFolder, 'casa/casa.ldif')
        self.ldif_scripts = os.path.join(Config.outputFolder, 'casa/scripts.ldif')
        self.pylib_folder = os.path.join(Config.jansOptPythonFolder, 'libs')
        self.casa_jetty_dir = os.path.join(self.jetty_base, 'casa')

    def install(self):

        self.run([paths.cmd_chmod , 'g+w', self.pylib_folder])
        self.logIt("Copying casa.war into jetty webapps folder...")
        self.installJettyService(self.jetty_app_configuration['casa'])

        jettyServiceWebapps = os.path.join(self.casa_jetty_dir, 'webapps')
        self.copyFile(self.source_files[0][0], jettyServiceWebapps)

        jettyServiceOxAuthCustomLibsPath = os.path.join(self.jetty_base,
                                                        "oxauth", 
                                                        "custom/libs"
                                                        )

        twillo_package = base.determine_package(os.path.join(Config.distJansFolder, 'twilio-*.jar'))
        self.copyFile(twillo_package, jettyServiceOxAuthCustomLibsPath)

        jsmpp_package = base.determine_package(os.path.join(Config.distJansFolder, 'jsmpp-*.jar'))
        self.copyFile(jsmpp_package, jettyServiceOxAuthCustomLibsPath)

        self.run([paths.cmd_chown, '-R', 'jetty:jetty', jettyServiceOxAuthCustomLibsPath])

        #Adding twilio jar path to oxauth.xml
        oxauth_xml_fn = os.path.join(self.jetty_base,  'oxauth/webapps/oxauth.xml')
        if os.path.exists(oxauth_xml_fn):
            
            class CommentedTreeBuilder(ElementTree.TreeBuilder):
                def comment(self, data):
                    self.start(ElementTree.Comment, {})
                    self.data(data)
                    self.end(ElementTree.Comment)

            parser = ElementTree.XMLParser(target=CommentedTreeBuilder())
            tree = ElementTree.parse(oxauth_xml_fn, parser)
            root = tree.getroot()

            xml_headers = '<?xml version="1.0"  encoding="ISO-8859-1"?>\n<!DOCTYPE Configure PUBLIC "-//Jetty//Configure//EN" "http://www.eclipse.org/jetty/configure_9_0.dtd">\n\n'

            for element in root:
                if element.tag == 'Set' and element.attrib.get('name') == 'extraClasspath':
                    break
            else:
                element = ElementTree.SubElement(root, 'Set', name='extraClasspath')
                element.text = ''

            extraClasspath_list = element.text.split(',')

            for ecp in extraClasspath_list[:]:
                if (not ecp) or re.search('twilio-(.*)\.jar', ecp) or re.search('jsmpp-(.*)\.jar', ecp):
                    extraClasspath_list.remove(ecp)

            extraClasspath_list.append('./custom/libs/{}'.format(os.path.basename(twillo_package)))
            extraClasspath_list.append('./custom/libs/{}'.format(os.path.basename(jsmpp_package)))
            element.text = ','.join(extraClasspath_list)

            self.writeFile(oxauth_xml_fn, xml_headers+ElementTree.tostring(root).decode('utf-8'))
        
        self.import_oxd_certificate()
        
        self.enable('casa')

    def copy_static(self):
        for script_fn in glob.glob(os.path.join(Config.staticFolder, 'casa/scripts/*.*')):
            self.run(['cp', script_fn, self.pylib_folder])

    def render_import_templates(self):

        scripts_template = os.path.join(self.templates_folder, os.path.basename(self.ldif_scripts))
        extensions = base.find_script_names(scripts_template)
        self.prepare_base64_extension_scripts(extensions=extensions)
        
        ldif_files = (self.ldif, self.ldif_scripts)
        for tmp in ldif_files:
            self.renderTemplateInOut(tmp, self.templates_folder, self.output_folder)

        self.dbUtils.import_ldif(ldif_files)


    def import_oxd_certificate(self):

        # import_oxd_certificate2javatruststore:
        self.logIt("Importing oxd certificate")

        # restart oxd-server

        self.stop('oxd-server')
        self.start('oxd-server')

        # check oxd status for 25 seconds:
        propertiesUtils = PropertiesUtils()
        for i in range(5):
            self.logIt("Checking oxd-server status. Try {}".format(i+1))
            if propertiesUtils.check_oxd_server(Config.oxd_server_https, log_error=False):
                self.logIt("oxd-server seems good")
                break
            time.sleep(5)
        else:
            self.logIt("oxd server at  {} did not repond in 15 seconds".format(Config.oxd_server_https), True)

        try:

            oxd_hostname, oxd_port = self.parse_url(Config.oxd_server_https)
            if not oxd_port: oxd_port=8443

            oxd_cert = ssl.get_server_certificate((oxd_hostname, oxd_port))
            oxd_alias = 'oxd_' + oxd_hostname.replace('.','_')
            oxd_cert_tmp_fn = '/tmp/{}.crt'.format(oxd_alias)
            self.writeFile(oxd_cert_tmp_fn, oxd_cert)
            
            self.run([Config.cmd_keytool, '-import', '-trustcacerts', '-keystore', 
                            '/opt/jre/jre/lib/security/cacerts', '-storepass', 'changeit', 
                            '-noprompt', '-alias', oxd_alias, '-file', oxd_cert_tmp_fn])
            
            os.remove(oxd_cert_tmp_fn)

        except:
            self.logIt("Error importing oxd server certificate", True)

    def create_folders(self):
        for path_name in ('static', 'plugins'):
            path = os.path.join(self.casa_jetty_dir, path_name)
            if not os.path.exists(path):
                self.run([paths.cmd_mkdir, '-p', path])
            self.run([paths.cmd_chown, '-R', 'jetty:jetty', path])

    def installed(self):
        return os.path.exists(os.path.join(Config.jetty_base, self.service_name, 'start.ini'))
