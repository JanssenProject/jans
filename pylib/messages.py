class msg:
    
    MAIN_label = "System Information"
    HostForm_label = "Gathering Information"
    ServicesForm_label = "Select Services to Install"
    DBBackendForm_label = "Choose DB Backend"
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
    max_ram_label = "Maximum RAM in MB"
    oxtrust_admin_password_label = "oxTrust Admin Password"
    oxtrust_admin_password_warning = "oxTrust Admin Password should be at least six characters"
    max_ram_int_warning = "Please enter and integer value for Max ram"
    
    exit_from_app = "Setup is exiting. %(reason)s"
    not_to_continue = "Since you don't want to continue."
    acknowledge_lisence = "I acknowledge that use of the Gluu Server is under the MIT license"
    acknowledge_lisence_ask = "Please acknowledge that use of the Gluu Server is under the MIT license"
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

    ask_wrends_install = "WrenDS Installation"
    wrends_install_options = ["Don't Install","Install Locally","Use Remote WrenDS"]

    ask_cb_install = "Couchbase Installation"
    cb_install_options = ["Don't Install","Install Locally","Use Remote Couchbase"]
    
    notify_select_backend = "Please select one of the backends either local install or remote" 
    weak_password = "Password for {} must be at least 6 characters and include one uppercase letter, one lowercase letter, one digit, and one special character."
    unselected_storages = "Note: Unselected storages will go Couchbase Server"

    no_help = "No help is provided for this screen."
    
    MainFromHelp = "Detected OS type, system init type, and Apache version is displayed. Inorder to continue to next step, you must check lisecnce acknowledgement."
    HostFromHelp = ("IP Address: ip address of this server. Detected ip address will be provided\n"
                   "Hostname: hostname of this server. Detected hostname will be provided.\n"
                   "Organization Name: ......")
