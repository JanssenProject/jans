# frozen_string_literal: true

require_relative "lib/cedarling/version"

Gem::Specification.new do |spec|
  spec.name = "cedarling"
  spec.version = Cedarling::VERSION
  spec.authors = ["John Anderson"]
  spec.email = ["john@gluu.org"]

  spec.summary = "ruby bindings for cedarling"
  spec.description = "ruby bindings for cedarling"
  spec.homepage = "https://github.com/JanssenProject/jans/tree/main/jans-cedarling/bindings/cedarling_ruby"
  spec.license = "Apache"
  spec.required_ruby_version = ">= 3.3.0"
  spec.required_rubygems_version = ">= 3.3.11"

  spec.metadata["allowed_push_host"] = "TODO: Set to your gem server 'https://example.com'"

  spec.metadata["homepage_uri"] = spec.homepage
  spec.metadata["source_code_uri"] = "https://github.com/JanssenProject/jans/tree/main/jans-cedarling/bindings/cedarling_ruby"

  # Specify which files should be added to the gem when it is released.
  # The `git ls-files -z` loads the files in the RubyGem that have been added into git.
  spec.files = Dir.chdir(__dir__) do
    `git ls-files -z`.split("\x0").reject do |f|
      (File.expand_path(f) == __FILE__) ||
        f.start_with?(*%w[bin/ test/ spec/ features/ .git .github appveyor Gemfile])
    end
  end
  spec.bindir = "exe"
  spec.executables = spec.files.grep(%r{\Aexe/}) { |f| File.basename(f) }
  spec.require_paths = ["lib"]
  spec.extensions = ["ext/cedarling/Cargo.toml"]

  spec.add_development_dependency "jwt", "~> 2.9.3"
  spec.add_development_dependency "pry", "~> 0.14.2"
  spec.add_development_dependency "rspec", "~> 3.13.0"
end
