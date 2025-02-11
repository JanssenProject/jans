resource "jans_script" "test" {
	dn 												= "inum=4A4E-4F3D,ou=scripts,o=jans"
	inum 											= "4A4E-4F3D"
	name 											= "test_script"
	description 							= "Test description"
	script 										= "<path-of-script>" 
	script_type 							= "introspection"
	programming_language 			= "python"
	level 										= 1
	revision 									= 1
	enabled 									= true
	modified 									= false
	internal 									= false
	location_type 						= "db"
	base_dn 									= "inum=4A4E-4F3D,ou=scripts,o=jans"

	module_properties {
			value1 = "location_type"
			value2 = "db"
	}

	module_properties {
			value1 = "location_option"
			value2 = "foo"
	}
	
}