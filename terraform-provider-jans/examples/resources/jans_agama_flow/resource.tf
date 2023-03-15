resource "jans_agama_flow" "test" {
	qname 		 = "test"
	revision 	 = 1
	enabled 	 = true
	source 		 = <<EOF
Flow test
    Basepath "hello"

in = { name: "John" }
RRF "index.ftlh" in

Log "Done!"
Finish "john_doe"
EOF
}
