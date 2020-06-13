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

### `docs`
This is the directory where documentation is kept, for example this doc

### `logs`
Log files wil be written to this directory

### `schema`
This directory contains schema utilities for OpenDj Server. OpenDj schema files are created with json files. Please
refer to own [documentation](/schema/README.md)

### `setup_app`
All related files related to SetupApp are saved under this directory and subdirectories.

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
  


