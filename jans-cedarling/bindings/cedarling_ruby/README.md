# Cedarling

This is a ruby wrapper for Cedarling.

For a really quick start, see 'Development' below.

## Installation

TODO: Replace `UPDATE_WITH_YOUR_GEM_NAME_IMMEDIATELY_AFTER_RELEASE_TO_RUBYGEMS_ORG` with your gem name right after releasing it to RubyGems.org. Please do not do it earlier due to security reasons. Alternatively, replace this section with instructions to install your gem from git if you don't plan to release to RubyGems.org.

Install the gem and add to the application's Gemfile by executing:

    $ bundle add UPDATE_WITH_YOUR_GEM_NAME_IMMEDIATELY_AFTER_RELEASE_TO_RUBYGEMS_ORG

If bundler is not being used to manage dependencies, install the gem by executing:

    $ gem install UPDATE_WITH_YOUR_GEM_NAME_IMMEDIATELY_AFTER_RELEASE_TO_RUBYGEMS_ORG

## Usage

TODO: Write usage instructions here, after there is some actual code to use.

## Development

Install ruby >= 3.3.0 using your package manager, or from source, or whatever you want.

run `bin/setup` to install dependencies

run `rake spec` to run the tests

You can also run `bin/console` for an interactive prompt that will allow you to experiment. Here you can say something like

  Cedarling::hello "there"

### development loop

`cargo build -p cedarling_ruby` will build your latest changes.

So you can say `cargo build -p cedarling_ruby && rake spec`

## Local Installation

You won't want to do this, unless it fails to install via `bundler` or `Gemfile`

To install this gem onto your local machine, run `bundle exec rake install`. To release a new version, update the version number in `version.rb`, and then run `bundle exec rake release`, which will create a git tag for the version, push git commits and the created tag, and push the `.gem` file to [rubygems.org](https://rubygems.org).

## Contributing

Bug reports and pull requests are welcome on GitHub at https://github.com/gluu/cedarling.

## License

TODO: check on gluu licencing policy and update if necessary.

The gem is available as open source under the terms of the [MIT License](https://opensource.org/licenses/MIT).
