resource "jans_uma_resource" "test" {
	inum 							= "4A4E-4F3D"
	name 							= "test_uma_resource"
	scopes 						= []
	scope_expression 	= ""
	clients 				 	= []
	resources 				= []
	rev 							= "1"
	creator 					= "user"
	description 			= "test UMA resource"
	type 							= "uma_resource_type"
	deletable 				= "true"
}