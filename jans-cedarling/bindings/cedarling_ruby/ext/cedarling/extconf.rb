# frozen_string_literal: true

require "mkmf"
require "rb_sys/mkmf"

# in Rakefile
# create the makefile which ultimately builds {dir}/{rust_lib}
# NOTE {rust_lib} must correspond with
# RbSys::ExtensionTask.new("{rust_lib}")
# currently 'cedarling_ruby'
create_rust_makefile("cedarling/cedarling_ruby")
