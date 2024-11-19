class msg:

    MAIN_label = "System Information"
    HostForm_label = "Gathering Information"
    ServicesForm_label = "Select Services to Install"
    StorageSelectionForm_label = "Hybrid Storage Selection"
    InstallStepsForm_label = "Installing Janssen Server"
    DisplaySummaryForm_label = "Janssen Server Installation Summary"
    version_label = 'Janssen CE'
    decription = "Use setup.py to configure your Janssen Server and to add initial data required for jans-auth to start. If setup.properties is found in this folder, these properties will automatically be used instead of the interactive setup."

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

    install_jans_auth_label = "Install Jans Auth"
    backend_types_label = "Backend Types"
    java_type_label = "Java Type"
    install_httpd_label = "Install Apache"
    installing_label = "Current"
    install_casa_label = "Install Casa"
    install_scim_server_label = "Install Scim"
    install_fido2_label = "Install Fido2"

    insufficient_free_disk_space = "Available free disk space was determined to be {1:0.1f} GB. This is less than the required disk space of {} GB."
    insufficient_mem_size = "RAM size was determined to be {:0.1f} GB. This is less than the suggested RAM size of {} GB"
    insufficient_number_of_cpu = "Available CPU Units found was {}. This is less than the required amount of {} CPU Units"
    insufficient_file_max = "Maximum number of files that can be opened on this computer is {}. Please increase number of file-max to {} on the host system and re-run setup.py"
    insufficient_free_disk_space = "Available free disk space was determined to be {} GB. This is less than the required disk space of {}"

    suggested_free_disk_space = 40 #in GB
    suggested_number_of_cpu = 2
    suggested_file_max = 64000

    cert_info_label = "Information to Generate Certificates"
    sys_info_label =  "System Information"
    application_max_ram_label = "Maximum RAM in MB"
    oxtrust_admin_password_label = "oxTrust Admin Password"
    oxtrust_admin_password_warning = "oxTrust Admin Password should be at least six characters"
    max_ram_int_warning = "Please enter and integer value for Max ram"
    memory_warning = "WARINIG: You don't have enough memory to run Janssen CE properly with selected applications. Continue anyway?"

    exit_from_app = "Setup is exiting. %(reason)s"
    not_to_continue = "Since you don't want to continue."
    acknowledge_lisence = "I acknowledge that use of the Janssen Server is under the Apache-2.0 license"
    acknowledge_lisence_ask = "Please check License Agreement"
    setup_properties_warning = "** All clear text passwords contained in\n/install/community-edition-setup/setup.properties.last."
    
    enter_hostname = "Please enter hostname"
    enter_hostname_local = "Hostname can't be localhost"
    enter_valid_email = "Please enter valid email address"
    enter_valid_ip = "Please enter valid IP Address"
    enter_valid_countryCode = "Please enter two letter country code"


    ask_install_httpd = "Install Apache HTTPD Server"
    ask_install_casa = "Install Casa"
    ask_install_scim_server = "Install Scim Server"
    ask_install_fido2 = "Install Fido2"

    notify_select_backend = "Please select one of the backends either local install or remote" 
    weak_password = "Password for {} must be at least 6 characters and include one uppercase letter, one lowercase letter, one digit, and one special character."

    no_help = "No help is provided for this screen."

    MainFromHelp = "Detected OS type, system init type, and Apache version is displayed. Inorder to continue to next step, you must check lisecnce acknowledgement."
    HostFromHelp = ("IP Address: ip address of this server. Detected ip address will be provided\n"
                   "Hostname: hostname of this server. Detected hostname will be provided.\n"
                   "Organization Name: ......")

    installation_completed = "Janssen Server installation successful!\n"
    post_installation = "Please restart all Janssen Services with command \033[1m/opt/jans/bin/jans restart\033[0m"

    installation_description_java = "Corretto is a build of the Open Java Development Kit (OpenJDK) with long-term support from Amazon. Corretto is certified using the Java Technical Compatibility Kit (TCK) to ensure it meets the Java SE standard."
    installation_description_jans_auth = "Jans Auth is an open source OpenID Connect Provider (OP) and UMA Authorization Server (AS). The project also includes OpenID Connect Client code which can be used by websites to validate tokens."
    installation_description_saml = "The Janssen Server acts as a SAML identity provider (IDP) to support outbound SAML single sign-on (SSO)."
    installation_description_jans = "Janssen Server is identity & access management (IAM) platform for web & mobile single sign-on (SSO), two-factor authentication (2FA) and API access management."
    installation_description_jetty = "Eclipse Jetty provides a Web server and jakarta.servlet container, plus support for HTTP/2, WebSocket, OSGi, JMX, JNDI, JAAS and many other integrations."
    installation_description_jython = "Jython is a Java implementation of Python that combines expressive power with clarity. Jython is freely available for both commercial and non-commercial use and is distributed with source code under the PSF License v2."
    installation_description_node = "As an asynchronous event-driven JavaScript runtime, Node.js is designed to build scalable network applications."
    installation_description_casa = "Janssen Casa is a self-service web portal for end-users to manage authentication and authorization preferences for their account in a Janssen Server."
    installation_description_scim = "The Janssen Server implements SCIM to offer standard REST APIs for performing CRUD operations (create, read, update and delete) against user data."
    installation_description_fido2 = "FIDO 2.0 (FIDO2) is an open authentication standard that enables people to leverage common devices to authenticate to online services in both mobile and desktop environments"

    installation_description_scripts = "Interception scripts can be used to implement custom business logic for authentication, authorization and more in a way that is upgrade-proof and doesn't require forking the Janssen Server code."

    installation_error = "The following error occurred while installing Janssen Server:"
    exit_post_setup = "No service was selected to install. Exit now?"

    used_ports = "Port(s) {} should be free to continue. Please check."
    setup_removal_warning = "Please remove the whole setup directory /opt/jans/jans-setup post-installation for a production deployment."
