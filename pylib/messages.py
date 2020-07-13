class msg:
    
    MAIN_label = "System Information"
    HostForm_label = "Gathering Information"
    ServicesForm_label = "Select Services to Install"
    DBBackendForm_label = "Choose to Store in WrenDS"
    StorageSelectionForm_label = "Hybrid Storage Selection"
    InstallStepsForm_label = "Installing Gluu Server"
    DisplaySummaryForm_label = "Gluu Server Installation Summary"

    decription = "Use setup.py to configure your Gluu Server and to add initial data required for oxAuth and oxTrust to start. If setup.properties is found in this folder, these properties will automatically be used instead of the interactive setup."

    os_type_label = "Detected OS"
    init_type_label = "Detected init"
    httpd_type_label = "Apache Version"
    ip_label = "IP Address"
    hostname_label = "Hostname"
    orgName_label = "Organization Name"
    state_label = "State"
    admin_email_label = "Support Email"
    city_label = "City or Locality"
    state_label = "State or Province"
    countryCode_label = "Country Code"
    password_label = "Password"
    hosts_label = "Hosts"
    username_label = "Username"

    installOxAuth_label = "Install OxAuth"
    installOxTrust_label = "Install OxTrust"
    backend_types_label = "Backend Types"
    java_type_label = "Java Type"
    installHttpd_label = "Install Apache" 
    installSaml_label = "Install Saml" 
    installOxAuthRP_label = "Install OxAuthRP" 
    installPassport_label = "Install Passport" 
    installGluuRadius_label = "Install Radius"
    wrends_storages_label = "Store on WrenDS"
    installing_label = "Installing"
    installOxd_label = "Install Oxd"
    installCasa_label = "Install Casa"
    installScimServer_label = "Install Scim"
    installFido2_label = "Install Fido2"

    insufficient_free_disk_space = "Available free disk space was determined to be {1:0.1f} GB. This is less than the required disk space of {} GB."
    insufficient_mem_size = "RAM size was determined to be {:0.1f} GB. This is less than the suggested RAM size of {} GB"
    insufficient_number_of_cpu = "Available CPU Units found was {}. This is less than the required amount of {} CPU Units"
    insufficient_file_max = "Maximum number of files that can be opened on this computer is {}. Please increase number of file-max to {} on the host system and re-run setup.py"
    insufficient_free_disk_space = "Available free disk space was determined to be {} GB. This is less than the required disk space of {}"

    suggested_free_disk_space = 40 #in GB
    suggested_mem_size = 3.7 # in GB
    suggested_number_of_cpu = 2
    suggested_file_max = 64000

    cert_info_label = "Information to Generate Certificates"
    sys_info_label =  "System Information"
    application_max_ram_label = "Maximum RAM in MB"
    oxtrust_admin_password_label = "oxTrust Admin Password"
    oxtrust_admin_password_warning = "oxTrust Admin Password should be at least six characters"
    max_ram_int_warning = "Please enter and integer value for Max ram"
    memory_warning = "WARINIG: You don't have enough memory to run Gluu CE properly with selected applications."

    exit_from_app = "Setup is exiting. %(reason)s"
    not_to_continue = "Since you don't want to continue."
    acknowledge_lisence = "I acknowledge that use of the Gluu Server is under the Apache-2.0 license"
    acknowledge_lisence_ask = "Please check License Agreement"
    setup_properties_warning = "** All clear text passwords contained in\n/install/community-edition-setup/setup.properties.last."
    
    enter_hostname = "Please enter hostname"
    enter_hostname_local = "Hostname can't be localhost"
    enter_valid_email = "Please enter valid email address"
    enter_valid_ip = "Please enter valid IP Address"
    enter_valid_countryCode = "Please enter two letter country code"


    ask_installHttpd = "Install Apache HTTPD Server"
    ask_installSaml = "Install Shibboleth SAML IDP"
    ask_installOxAuthRP  = "Install oxAuth RP"
    ask_installPassport  = "Install Passport"
    ask_installGluuRadius = "Install Gluu Radius"
    ask_installCasa = "Install Casa"
    ask_installOxd = "Install Oxd"
    ask_wrends_install = "Install WrenDS"
    ask_installScimServer = "Install Scim Server"
    ask_installFido2 = "Install Fido2"


    wrends_install_options = ["Don't Install","Install Locally","Use Remote WrenDS"]
    oxd_url_label = "oxd Server URL"
    install_oxd_or_url_warning = "Please either enter oxd Server URL or check Install Oxd"
    oxd_connection_error = "Can't connect to oxd-server with url {}. Reason: {}"
    oxd_ssl_cert_error = "Hostname of oxd ssl certificate is {} which does not match {} casa won't start properly"

    ask_cb_install = "Couchbase Installation"
    cb_install_options = ["Don't Install","Install Locally","Use Remote Couchbase"]
    
    ask_use_gluu_storage_oxd = "By default oxd uses its own db. Do you want to use Gluu Storage for Oxd?"
    ask_use_gluu_storage_oxd_title = "Use Gluu Storage for Oxd?"
    
    notify_select_backend = "Please select one of the backends either local install or remote" 
    weak_password = "Password for {} must be at least 6 characters and include one uppercase letter, one lowercase letter, one digit, and one special character."
    unselected_storages = "Note: Unselected storages will go Couchbase Server"

    no_help = "No help is provided for this screen."
    
    MainFromHelp = "Detected OS type, system init type, and Apache version is displayed. Inorder to continue to next step, you must check lisecnce acknowledgement."
    HostFromHelp = ("IP Address: ip address of this server. Detected ip address will be provided\n"
                   "Hostname: hostname of this server. Detected hostname will be provided.\n"
                   "Organization Name: ......")

    installation_completed = "Gluu Server installation successful! Point your browser to https://{}"

    installation_description_java = "Corretto is a build of the Open Java Development Kit (OpenJDK) with long-term support from Amazon. Corretto is certified using the Java Technical Compatibility Kit (TCK) to ensure it meets the Java SE standard."
    installation_description_opendj = "WrenDS is an LDAPv3 compliant directory service, which has been developed for the Java platform, providing a high performance, highly available, and secure store for the identities managed by your organization."
    installation_description_oxauth = "oxAuth is an open source OpenID Connect Provider (OP) and UMA Authorization Server (AS). The project also includes OpenID Connect Client code which can be used by websites to validate tokens."
    installation_description_oxtrust = "oxTrust is a Weld based web application for Gluu Server administration."
    installation_description_saml = "The Gluu Server acts as a SAML identity provider (IDP) to support outbound SAML single sign-on (SSO)."
    installation_description_passport = "Gluu bundles the Passport.js authentication middleware project to support user authentication at external SAML, OAuth, and OpenID Connect providers "
    installation_description_radius = "The Gluu Server now ships with a RADIUS server called Gluu Radius. It is based on the TinyRadius Java library."
    installation_description_gluu = "Gluu Server is identity & access management (IAM) platform for web & mobile single sign-on (SSO), two-factor authentication (2FA) and API access management."
    installation_description_jetty = "Eclipse Jetty provides a Web server and javax.servlet container, plus support for HTTP/2, WebSocket, OSGi, JMX, JNDI, JAAS and many other integrations."
    installation_description_jython = "Jython is a Java implementation of Python that combines expressive power with clarity. Jython is freely available for both commercial and non-commercial use and is distributed with source code under the PSF License v2."
    installation_description_node = "As an asynchronous event-driven JavaScript runtime, Node.js is designed to build scalable network applications."
    installation_description_oxd = "oxd exposes simple, static APIs web application developers can use to implement user authentication and authorization against an OAuth 2.0 authorization server like Gluu."
    installation_description_casa = "Gluu Casa is a self-service web portal for end-users to manage authentication and authorization preferences for their account in a Gluu Server."
    installation_description_scim = "The Gluu Server implements SCIM to offer standard REST APIs for performing CRUD operations (create, read, update and delete) against user data."
    installation_description_fido2 = "FIDO 2.0 (FIDO2) is an open authentication standard that enables people to leverage common devices to authenticate to online services in both mobile and desktop environments"
    
    installation_description_scripts = "Interception scripts can be used to implement custom business logic for authentication, authorization and more in a way that is upgrade-proof and doesn't require forking the Gluu Server code."

    installation_error = "The following error was occurred while installing Gluu Server:"
