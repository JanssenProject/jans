# This software is available under the Apache-2.0 license.
# See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
#
# Copyright (c) 2024, Gluu, Inc.

# frozen_string_literal: true

require "mkmf"
require "rb_sys/mkmf"

create_rust_makefile("cedarling/cedarling_ruby")
