resource "jans_script" "test" {
	dn 												= "inum=4A4E-4F3D,ou=scripts,o=jans"
	inum 											= "4A4E-4F3D"
	name 											= "test_script"
	description 							= "Test description"
	script 										= ""
	script_type 							= "INTROSPECTION"
	programming_language 			= "PYTHON"
	level 										= 1
	revision 									= 1
	enabled 									= true
	modified 									= false
	internal 									= false
	location_type 						= "LDAP"
	base_dn 									= "inum=4A4E-4F3D,ou=scripts,o=jans"

	module_properties {
			value1 = "location_type"
			value2 = "ldap"
	}

	module_properties {
			value1 = "location_option"
			value2 = "foo"
	}
	
}