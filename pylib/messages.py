class msg:
    decription = "Use setup.py to configure your Gluu Server and to add initial data required for oxAuth and oxTrust to start. If setup.properties is found in this folder, these properties will automatically be used instead of the interactive setup."
    
    insufficient_free_disk_space = "Available free disk space was determined to be {1:0.1f} GB. This is less than the required disk space of {} GB."
    insufficient_mem_size = "RAM size was determined to be {:0.1f} GB. This is less than the suggested RAM size of {} GB"
    insufficient_number_of_cpu = "Available CPU Units found was {}. This is less than the required amount of {} CPU Units"
    insufficient_file_max = "Maximum number of files that can be opened on this computer is {}. Please increase number of file-max to {} on the host system and re-run setup.py"
    insufficient_free_disk_space = "Available free disk space was determined to be {} GB. This is less than the required disk space of {}"
    
    suggested_free_disk_space = 40 #in GB
    suggested_mem_size = 3.7 # in GB
    suggested_number_of_cpu = 2
    suggested_file_max = 64000
    
    exit_from_app = "Setup is exiting. %(reason)s"
    not_to_continue = "Since you don't want to continue."
    acknowledge_lisence = "I acknowledge that use of the Gluu Server is under the MIT license"
    acknowledge_lisence_ask = "Please acknowledge that use of the Gluu Server is under the MIT license"
    setup_properties_warning = "** All clear text passwords contained in\n/install/community-edition-setup/setup.properties.last."
    
    enter_hostname = "Please enter hostname"
    enter_hostname_local = "Hostname can't be localhost"
    enter_valid_email = "Please enter valid email address"
    enter_valid_ip = "Please enter valid IP Address"
    enter_valid_country_code = "Please enter two letter country code"



