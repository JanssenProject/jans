---
tags:
- Casa
- administration
- localization
---

# Casa localization

Casa supports multilingual support through resource bundles. Administrators supply bundles as plaintext files ending with the `.properties` file extension.
 
By default, Casa contains three bundles, each in a separate file. These bundles contain the internationalization labels in the English language, as displayed in a default Casa installation. For example, to add support for French, you would have to create the following files:

| File                    | Description                                                |
|------------------------ |---------------------------                                 |
| `user_fr.properties`    | Contains labels mostly found in user-facing pages          |
| `admin_fr.properties`   | Contains labels mostly found in administrator-facing pages |
| `general_fr.properties` | Contains labels found widely across the app and plugins    |
 
## Adding internationalization labels

To supply labels in a particular language (or even if you want to override the English translation provided), do the following:

1. Log in to the Janssen Server using SSH
1. Extract the Casa default labels: `/opt/jre/bin/jar -xf /opt/jans/jetty/jans-casa/webapps/casa.war WEB-INF/classes/labels`
1. Run `cp WEB-INF/classes/labels/*.properties .` and delete WEB-INF dir: `rm -R WEB-INF/`
1. Add the appropriate suffix to the properties files found in the current directory, ie. `_de` for German, `_es` for Spanish, `_ja` for Japanese, etc.
1. Edit the contents of files accordingly. Use UTF-8 encoding for opening and saving
1. `cd` to `/opt/jans/jetty/jans-casa/static`
1. Create directory `i18n` if it does not exist: `mkdir i18n`
1. Transfer the properties files to the `i18n` folder
1. Ensure `jetty` user has permission for reading the files
1. [Restart](../../janssen-server/vm-ops/restarting-services.md) casa

Log in to the application and review your work. Make necessary edits and repeat the process.

## How are labels picked?

In Casa, the rule for displaying contents is leveraged from the [underlying framework](https://www.zkoss.org/wiki/ZK%20Developer's%20Reference/Internationalization). In short, the locale to use per session is picked based on the end-user browser settings.

As an example, if the browser was configured to use U.S. English, the locale will be `en_US`. This means that files ending in  `_en_US.properties` will be considered first. Then, the country suffix is removed and thus `_en.properties` is looked up. Finally the non-suffixed ones are considered, that is, the default label files bundled with Casa.

Additionally, end users can pick the language of their preference by selecting a language item from the dropdown list appearing at the bottom of any Casa page. The list is only shown if there are two or more languages available to display.

## Localization in plugins

Plugins also support localization through the "zk-label" bundle. If you have installed plugins developed by Gluu, they will only contain a single default English file.

To add your own translation for plugin texts, proceed as follows:

1. `cd` to the folder where you stored the jar file of the plugin of interest.
1. Extract the plugin's default labels (requires Java bin on your path): `jar -xf JAR_FILE labels/zk-label.properties`
1. `cd` to `labels` folder
1. Add the appropriate suffix to the properties file, ie. `_de` for German, `_es` for Spanish, `_ja` for Japanese, etc.
1. Edit the contents accordingly. Use UTF-8 encoding for opening and saving
1. Connect to your Janssen Server using SSH
1. `cd` to `/opt/jans/jetty/jans-casa/static`
1. Create directory `i18n` if it does not exist: `mkdir i18n`
1. Transfer the properties file to the `i18n` folder
1. Ensure `jetty` user has permission for reading
1. [Restart](../../janssen-server/vm-ops/restarting-services.md) casa

!!! Note
    If your plugins have a `zk-label.properties`, you can accumulate all plugin texts into a single file, or you can use a different filename for each plugin.

## Properties file syntax 

Administrators acquainted with the format used for properties files in Java will find Casa resource bundle files familiar. The format used in Casa differs slightly, but it is more powerful. To learn more about this topic, visit this [page](https://www.zkoss.org/wiki/ZK%20Developer's%20Reference/Internationalization/Labels/The%20Format%20of%20Properties%20Files).

## Tips

- Not all entries present in default label files have to be translated in your own localized versions. If you are comfortable with the current text for a particular entry, you can simply remove it to use the one in the default files.

- There is no need to supply specific translations for countries. While supported, most of time it suffices to create files suffixed with the language code, for instance `_es`, and not with country code (e.g `_es_CO`, `_es_AR`, `_es_EC`, `_es_ES`, etc.) 

- Actual filenames for properties files are not relevant. Upon start, Casa will parse all properties files present in `i18n` folder.
