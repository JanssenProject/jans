class msg:
    decription = "Use setup.py to configure your Gluu Server and to add initial data required for oxAuth and oxTrust to start. If setup.properties is found in this folder, these properties will automatically be used instead of the interactive setup."
    insufficient_disk_space = "Available free disk space was determined to be {1:0.1f} GB. This is less than the required disk space of {} GB."
    insufficient_mem = "RAM size was determined to be {:0.1f} GB. This is less than the suggested RAM size of {} GB"
    insufficient_cpu = "Available CPU Units found was %(determined_number_of_cpu)s. This is less than the required amount of %(suggested_number_of_cpu)s CPU Units."
    exit_from_app = "Setup is exiting. %(reason)s"
    not_to_continue = "Since you don't want to continue."
    acknowledge_lisence = "I acknowledge that use of the Gluu Server is under the MIT license"
    acknowledge_lisence_ask = "Please acknowledge that use of the Gluu Server is under the MIT license"
    setup_properties_warning = "** All clear text passwords contained in\n/install/community-edition-setup/setup.properties.last."
    
    suggested_mem_size = 3.7 # in GB
    suggested_number_of_cpu = 2
    suggested_free_disk_space = 40 #in GB
