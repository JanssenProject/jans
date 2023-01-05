# Developers Guide for Janssen Project Setup Application

This guide is prepared for developers who will edit and/or create new installer forJanssen Project Setup.
We will refer Janssen Project Application as **SetupApp**
SetupApp is written with Python3 (we try make it compatible at least version 3.5)

## Direcory Structure

```
  docs/
  logs/
  output/
  schema/
  setup_app/
    data/
    installers/
    pylib/
    utils/
  static/
  templates/
  tests/
```

### `docs/`
This is the directory where documentation is kept, for example this doc

### `logs/`
Log files wil be written to this directory

### `schema/`
This directory contains schema utilities for OpenDj Server. OpenDj schema files are created with json files. Please
refer to own [documentation](/schema/README.md)

### `setup_app/`
All files related to SetupApp are saved under this directory and subdirectories.

- `static.py` This Python file contains static definitions, mostly enumerations. We won't import anything to this file.
  Thus, this is the most basic Python file. If you are going to write another application that will use components of ,
  SetupApp, this file should be imported first.

- `paths.py` We write static paths to this file and imports limided basic python modules. Unless you specify installation path
  and application root we determine those initial paths relative to `setup_app/`. This file also contains os commands with 
  full path. This is the second file you need to import if you are going to write another application.

- `config.py` Configuration settings related to SetupApp are written to this file. Most of the definitions are static,
  and some path combinations. We only define variables and assign values inside this file only if those variables and values
  are global, will be needed by other components of this application and will be written to `setup.properties.last`. Static
  variables are defined as class variables, so the can be accesible without any construction. You need to call `init()` static
  method with argument installaltion directory. Since we don't create varbiles related to services/applications unless they are
  installed, you can query a variable with static method `get()` by providing variable name and default value if you need. 
  For example if you code `Config.get('installJans', False)`, this will return value of variable `installJans` if defined, 
  otherwise  returns `False`. This is the third file you need to import if you are going to write another application and sould
  be constructed by `init()`. You won't need initialize this class.
  
- `setup_options.py` Command line arguments are kept in this file. It has a single function `get_setup_options()` which returns 
  `argsp` object and `setupOptions` as dictionary.
  
- `test_data_loader.py` Test data loader class found in this file.

- `messages.py` This file contains message texts used in TUI. The aim was to use same messages in command line and tui

### `utils/`

Collection of utilities used by SetupApp.
- `base.py` Contains simple functions used by SetupApp components. It also contains some variables such as `os_type`,
  `os_version`, `httpd_name`, rousources etc. Since this file contains paths defined in `paths.py`
   you need to import this file after `paths.py`

- `ldif_utils.py` Utilities related to ldif files, such as parser, ldif to couchbase document conversion, 
   dn to key conversion and others

- `progress.py` Includes SetupApp progress indication class

- `attributes.py` This module contains `AttribDataTypes` class which is used to determine types of attrbiutes when converting ldif
   to couchbase document.

- `cbm.py` Couchbase management utilities. We generally don't use this module directly, instead use `DBUtils` class
   found in `db_utils.py`

- `db_utils.py` Database management (both ldap and cocuhbase) class is inclueded `DBUtils` in this file. Since `DBUtils`
   class is assigned to `self.dbutils` variable in base class for installers, we generally don't include this module
   in installers. Functions in `DBUtils` class automatically determines which database to be used for operations by examining
   `mapping_locations` defined in `Config` and populated during install time (or later from `setup.properties`).
   Some most commanly functions are:
   
   - `import_lidf(ldif_files)`: imports to list of ldif files to database. It automatically determines database location 
     (ldap or couchbase) and which bucket to import according to `dn`
   - `set_oxAuthConfDynamic(entries)`: takes entries as dctionary and updates `oxAuthConfDynamic` in database
   - `set_oxAuthConfDynamic(entries)`: the same for `oxAuthConfDynamic`
   - `enable_script(inum)`: enables script
   - `enable_service(service)`: enables jans service in oxtrust ui
   - `add_client2script(inum)`: adds client to script's allowed client property
   
- `crypto64.py` Cryptographic and base64 utilities are included in this module. Since this module is inherited by `SetupUtils`
  class, all methods are available in installers.
  
- `setup_utils.py` Basic SetapApp utilities is in class `SetupUtils`. We need to class `init()` classic method after initializing
  the class. This class in inherited by base installer class, thus all methods are available in installers.
  
- `tui.py` TUI (Text User Interface) installing utility. It provides ncurses widgets for nice installation screens.

### `installers/`
In this directory we have installer modules for SetupApp, acutal installations are done in these modules. We try to keep
each installer seperate from others, so that we can install Janssen Project components any time we want (except, basic setup: jre, node,
jetty, jython etc). Rather then explaining each module, a brief description for how to write and installer will be given, after
desciribng base installers

- `base.py` This module contains `BaseInstaller` class which is inherited by all pther installers. It provides a skeleton and 
  auto-called functions. An installer will be inititated to install by calling it's `start_installation()` method. It automatically
  binds to related database and provides through `self.dbutils` property. `start_installation()` method will call the follwoing
  methods/fucntions automatically:
 
   - `check_for_download()`: checks wheter source files are neded to download,
   - `download_files()`: if war files to be downloaded, downloads from `source_files` defined in installer, you can implement this function within installer, see for example `oxd.py`
   - `create_user()`: if defined user creation function is called,
   - `create_folders()`: if defined directory creation function is called,
   - `install()`: write actual installation codes in this fucntion,
   - `copy_static()`: if you need to copy some static files, write here,
   - `generate_configuration()`: many installers needs configuration generations, such as client_id's, password before rendering templates. 
      So include these code here,
   - `render_import_templates()`: template renderings are done under this function, then imported to database
   - `update_backend()`: if you need to update database (other than importing templates) write here. For example enabling script, 
      updating oxauth and/or oxtrust configurations etc.
   
   This module also contains start/stop/restart/enable linux services.
   
- `jetty.py` Provides `JettyInstaller`. Janssen Project components either jetty or node services. So `jetty.py` provides installers for jetty services as well as
   installing jetty itself.

- `node.py` Provides `NodeInstaller` Acts as both node installer and Janssen node component installers.


#### Writing an Installer Module

Chose either `JettyInstaller` or `NodeInstaller` depending on your application to setup. Let's give example for a jetty application.
Start with creating class and variables defined in `__init__()`

For this example let say we need to update/add these variables  `ThisApplicationKeystorePath`, `ThisApplicationKeystorePassword`,
`ThisApplicationApiBase` and `ThisApplicationTestMode` on **oxTrustConfApplication**. Prepare a json file 
`templates/sample-app/oxtrust_config.json` having the follwoing content:

```
{
    "ThisApplicationKeystorePath": "%(app_client_jks_fn)s",
    "ThisApplicationKeystorePassword": "%(app_client_jks_pass)s",
    "ThisApplicationApiBase": "http://%(hostname)s/identity/%(service_name)s",
    "ThisApplicationTestMode" : true
}
```

Please refer to variables defined in the `SampleInstaller` below. `hostname` will come from `Config`.
And I have a configuration ldif file `templates/sample-app/config.ldif` as follows:

```
dn: ou=sampleapplication,ou=configuration,o=jans
objectClass: top
objectClass: oxApplicationConfiguration
ou: oxpassport
jansPassportConfiguration:: {"name": "%(service_name)s", "some_variable": "same value"}

# Application Client
dn: inum=%(app_client_id)s,ou=clients,o=jans
objectClass: oxAuthClient
objectClass: top
inum: %(app_client_id)s
displayName: Sample Sapplication Client
oxAuthAppType: native
oxAuthGrantType: client_credentials
oxAuthIdTokenSignedResponseAlg: HS256
oxAuthScope: inum=ABCD-DEF0,ou=scopes,o=jans
oxAuthJwks:: %(app_client_base64_jwks)s
oxAuthTokenEndpointAuthMethod: private_key_jwt
oxPersistClientAuthorizations: false
oxAuthLogoutSessionRequired: false
oxAuthRequireAuthTime: false

# Scope
dn: inum=ABCD-DEF0,ou=scopes,o=jans
objectClass: oxAuthCustomScope
objectClass: top
displayName: Sample Apllication Access
inum: ABCD-DEF0
oxId: https://%(hostname)s/oxauth/restv1/uma/scopes/sample_app_access
oxUmaPolicyScriptDn: inum=2DAF-F9A5,ou=scripts,o=jans
oxScopeType: uma


# Resource
dn: oxId=%(application_resource_id)s,ou=resources,ou=uma,o=jans
objectClass: oxUmaResource
objectClass: top
displayName: Sample Application Resource
owner: inum=%(admin_inum)s,ou=people,o=jans
oxFaviconImage: http://www.jans.org/img/sample_app_logo.png
oxAssociatedClient: inum=%(app_client_id)s,ou=clients,o=jans
oxAuthUmaScope: inum=ABCD-DEF0,ou=scopes,o=jans
oxId: %(application_resource_id)s
oxResource: https://%(hostname)s/identity/restv1/sample_app/v2
oxRevision: 1

```

We can write installer as follows

```
from setup_app.utils import base
from setup_app import static
from setup_app.config import Config
from setup_app.installers.jetty import JettyInstaller

class SampleInstaller(JettyInstaller):

    def __init__(self):
        self.needdb = True # If you need database operations, set this to True so that database connection is done
                           # automatically and self.dbutils functions are ready for use

        self.service_name = 'sample-application' # This variable is used in various places, so chose right name for this service
                                          # This should match linux service name, since it is used for starting and stopping
                                          # services, for example, systemctl start application

        self.app_type = static.AppType.SERVICE   # enumartiation for application type, etiher APPLICATION or SERVICE
        self.install_type = static.InstallOption.OPTONAL # enumartiation for installation type, etiher MANDATORY or OPTONAL
        self.install_var = 'installApplication' # variale defined in Config to determine if thi application is going to be installed or not
        self.register_progess() # we need to register to progress indicator so that it will shows status of installation during installation process

        self.source_files = [('sample.war', 'http://sample.org/sample')] # Define source files here, this is a list contains tuples.
                                                                         # First element of each touple is filename of package, and second
                                                                         # element is url where the file is going to be downloaded
                                                                         # BaseInstaller automatically determines if files are going to be
                                                                         # downloaded. If it is post-install, current files are checked if up to date before
                                                                         # attempting to download. If setup.py is called with -w argument, 
                                                                         # files are forced to download

        # We need to define files to be downloaded if Config.downloadWars was set to True.
        # You can spicify multiple download files in list. For example self.oxtrust_war = ['ftp://server/app.zip', 'http://piblic/myapp.war']
        # You need to implement `download_files()` function inside the class. It will be called automatically.
        self.oxtrust_war = 'https://ox.gluu.org/maven/org/gluu/oxtrust-server/%s/oxtrust-server-%s.war' % (Config.oxVersion, Config.oxVersion)
        
        self.templates_folder = os.path.join(Config.templateFolder, 'sample-app') # folder where themplates of this application exists
        self.output_folder = os.path.join(Config.output_dir, 'sample-app') # folder where rendered templates to be written


        self.app_client_jks_fn = os.path.join(Config.certFolder, 'sample-app.jks')
        # Define templates in output_folder, rather than to templates_folder
        # This is confusing, but template rendering function was written in this way
        self.oxtrust_config_fn = os.path.join(self.output_folder, 'oxtrust_config.json')
        self.config_ldif = os.path.join(self.output_folder, 'config.ldif')

    def install(self):
        # this is the first function called automatically after binding database
        
        # deploy jetty application
        self.installJettyService(self.jetty_app_configuration[self.service_name], True)
        jettyServiceWebapps = os.path.join(self.jetty_base, self.service_name, 'webapps')
        src_war = os.path.join(Config.distGluuFolder, 'app.war')
        self.copyFile(src_war, jettyServiceWebapps)

        # enable linux ssystem service
        self.enable()
    
    def create_folders(self):
        # create folders here before installing anything
        self.createDirs(os.path.join(Config.jansBaseFolder, 'conf/app'))
    
    def generate_configuration(self):

        # This is configuration generation fucntion
        # Config and check_properties() (in properties_utils.py) don't provide default configuration valus.
        # So you need to check and create them under this function. If a value is going to be written to 
        # setup.properties, set it an attrbiute of Config, otherwise just define them as an attrbiute of
        # this class. Attrbiutes of this class will be available for template rendering cunctions
        
        self.check_clients([('app_client_id', '1901.')]) # This function takes list of cients in (client_var, client_prefix) format.
                                                         # For this example, the function first looks if client is defined in Config.app_client_id
                                                         # If not defined, looks database if there is a client with inum 1901.*
                                                         # if it finds database it assigns to Config.app_client_id
                                                         # Otherwise it creates new one and assigns to Config.app_client_id

        self.check_clients([('application_resource_id', '1905.')], resource=True) # Does the same thing for resource

        if not Config.get('ap_client_jks_pass'):
            Config.app_client_jks_pass = self.getPw()
            Config.app_client_jks_pass_encoded = self.obscure(Config.app_client_jks_pass)
        self.app_client_jwks = self.gen_openid_jwks_jks_keys(self.app_client_jks_fn, Config.app_client_jks_pass)
        Config.templateRenderingDict['app_client_base64_jwks'] = self.generate_base64_string(self.app_client_jwks, 1)

    def render_import_templates(self):
        # we need to render and templates here. This fucntion is called after configuration generation fucntion
        # and all attributes of this class is available fot template rendering fucntion, since update_rendering_dict()
        # of base installer is called. You can call function update_rendering_dict() at any time if you change/set  value
        # of an attrbiute either locally or in Config
        
        
        # For this sample, lets say we have some update for oxTrustConfApplication and needs to be rendered
        self.renderTemplateInOut(self.oxtrust_config_fn, self.templates_folder, self.output_folder)
        
        # This is configuration ldap for this application
        self.renderTemplateInOut(self.config_ldif, self.templates_folder, self.output_folder)


        # Import (load) ldif to database
        ldif_files = [self.config_ldif]
        self.dbUtils.import_ldif(ldif_files) #accept only list of ldif files in case multiple files

    def update_backend(self):
        # After importing tamplates, we may need some tweaks in database.
        # Do them here

        self.dbUtils.enable_service('jansAppEnabled') # enable this app in oxtrust ui
        oxtrust_conf = base.readJsonFile(self.oxtrust_config_fn)
        self.dbUtils.set_oxTrustConfApplication(oxtrust_conf)

```
