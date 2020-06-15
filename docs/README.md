# Developers Guide for Gluu CE Setup Application

This guide is prepared for developers who will edit and/or create new installer for Gluu CE Setup.
We will refer Gluu CE Setup Application as **SetupApp**
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
  For example if you code `Config.get('scimTestMode', False)`, this will return value of variable `scimTestMode` if defined, 
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
   `mappingLocations` defined in `Config` and populated during install time (or later from `setup.properties`).
   Some most commanly functions are:
   
   - `import_lidf(ldif_files)`: imports to list of ldif files to database. It automatically determines database location 
     (ldap or couchbase) and which bucket to import according to `dn`
   - `set_oxAuthConfDynamic(entries)`: takes entries as dctionary and updates `oxAuthConfDynamic` in database
   - `set_oxAuthConfDynamic(entries)`: the same for `oxAuthConfDynamic`
   - `enable_script(inum)`: enables script
   - `enable_service(service)`: enables gluu service in oxtrust ui
   - `add_client2script(inum)`: adds client to script's allowed client property
   
- `crypto64.py` Cryptographic and base64 utilities are included in this module. Since this module is inherited by `SetupUtils`
  class, all methods are available in installers.
  
- `setup_utils.py` Basic SetapApp utilities is in class `SetupUtils`. We need to class `init()` classic method after initializing
  the class. This class in inherited by base installer class, thus all methods are available in installers.
  
- `tui.py` TUI (Text User Interface) installing utility. It provides ncurses widgets for nice installation screens.

### `installers/`
In this directory we have installer modules for SetupApp, acutal installations are done in these modules. We try to keep
each installer seperate from others, so that we can install Gluu CE components any time we want (except, basic setup: jre, node,
jetty, jython etc). Rather then explaining each module, a brief description for how to write and installer will be given, after
desciribng base installers

- `base.py` This module contains `BaseInstaller` class which is inherited by all pther installers. It provides a skeleton and 
  auto-called functions. An installer will be inititated to install by calling it's `start_installation()` method. It automatically
  binds to related database and provides through `self.dbutils` property. `start_installation()` method will call the follwoing
  methods/fucntions automatically:
 
   - `download_files()`: if war files to be installed, it is called automatically,
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
   
- `jetty.py` Provides `JettyInstaller`. Gluu CE components either jetty or node services. So `jetty.py` provides installers for jetty services as well as
   installing jetty itself.

- `node.py` Provides `NodeInstaller` Acts as both node installer and Gluu node component installers.


#### Writing an Installer Module

Chose either `JettyInstaller` or `NodeInstaller` depending on your application to setup. Let's give example for a jetty application.
Start with creating class and variables defined in `__init__()`


```
from setup_app import static
from setup_app.config import Config
from setup_app.installers.jetty import JettyInstaller

class SampleInstaller(JettyInstaller):

    def __init__(self):
        self.service_name = 'application' # This variable is used in various places, so chose right name for this service
                                          # This should match linux service name, since it is used for starting and stopping
                                          # services, for example, systemctl start application

        self.app_type = static.AppType.SERVICE   # enumartiation for application type, etiher APPLICATION or SERVICE
        self.install_type = static.InstallOption.OPTONAL # enumartiation for installation type, etiher MONDATORY or OPTONAL
        self.install_var = 'installApplication' # variale defined in Config to determine if thi application is going to be installed or not
        self.register_progess() # we need to register to progress indicator so that it will shows status of installation during installation process
        
        # We need to define files to be downloaded if Config.downloadWars was set to True.
        # You can spicify multiple download files in list. For example self.oxtrust_war = ['ftp://server/app.zip', 'http://piblic/myapp.war']
        self.oxtrust_war = 'https://ox.gluu.org/maven/org/gluu/oxtrust-server/%s/oxtrust-server-%s.war' % (Config.oxVersion, Config.oxVersion)
        
        self.templates_folder = os.path.join(Config.templateFolder, 'oxtrust') # folder where themplates of this application exists
        self.output_folder = os.path.join(Config.outputFolder, 'oxtrust') # folder where rendered templates to be written

```
