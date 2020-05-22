import os
import glob


from setup_app.utils.base import httpd_name, clone_type, \
    os_initdaemon, os_type, determineApacheVersion

from setup_app.config import Config
from setup_app.utils.setup_utils import SetupUtils
from setup_app.installers.base import BaseInstaller
from setup_app import paths


class CasaInstaller(SetupUtils, BaseInstaller):

    def __init__(self):
        super().__init__()
        self.service_name = 'oxauth'


    def install(self):
        self.logIt("Installing Casa...")

        self.run(['chmod', 'g+w', '/opt/gluu/python/libs'])
        self.logIt("Copying casa.war into jetty webapps folder...")
        self.installJettyService(self.jetty_app_configuration['casa'])

        jettyServiceWebapps = os.path.join(self.jetty_base,
                                            'casa',
                                            'webapps'
                                            )

        self.copyFile(
                    os.path.join(self.distGluuFolder, 'casa.war'),
                    jettyServiceWebapps
                    )

        jettyServiceOxAuthCustomLibsPath = os.path.join(self.jetty_base,
                                                        "oxauth", 
                                                        "custom/libs"
                                                        )
        
        self.copyFile(
                os.path.join(self.distGluuFolder, 
                'twilio-{0}.jar'.format(self.twilio_version)), 
                jettyServiceOxAuthCustomLibsPath
                )
        
        self.copyFile(
                os.path.join(self.distGluuFolder, 'jsmpp-{}.jar'.format(self.jsmmp_version)), 
                jettyServiceOxAuthCustomLibsPath
                )
        
        self.run([self.cmd_chown, '-R', 'jetty:jetty', jettyServiceOxAuthCustomLibsPath])

        # Make necessary Directories for Casa
        for path in ('/opt/gluu/jetty/casa/static/', '/opt/gluu/jetty/casa/plugins'):
            if not os.path.exists(path):
                self.run(['mkdir', '-p', path])
                self.run(['chown', '-R', 'jetty:jetty', path])
        
        #Adding twilio jar path to oxauth.xml
        oxauth_xml_fn = '/opt/gluu/jetty/oxauth/webapps/oxauth.xml'
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

            extraClasspath_list.append('./custom/libs/twilio-{}.jar'.format(self.twilio_version))
            extraClasspath_list.append('./custom/libs/jsmpp-{}.jar'.format(self.jsmmp_version))
            element.text = ','.join(extraClasspath_list)

            self.writeFile(oxauth_xml_fn, xml_headers+ElementTree.tostring(root).decode('utf-8'))

        pylib_folder = os.path.join(self.gluuOptPythonFolder, 'libs')
        for script_fn in glob.glob(os.path.join(self.staticFolder, 'casa/scripts/*.*')):
            self.run(['cp', script_fn, pylib_folder])

        self.enable_service_at_start('casa')
