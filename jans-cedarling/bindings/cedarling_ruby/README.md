# Cedarling

This is a ruby wrapper for Cedarling.

For a really quick start, see [development](#development) below.

## Installation

TODO: Replace `UPDATE_WITH_YOUR_GEM_NAME_IMMEDIATELY_AFTER_RELEASE_TO_RUBYGEMS_ORG` with your gem name right after releasing it to RubyGems.org. Please do not do it earlier due to security reasons. Alternatively, replace this section with instructions to install your gem from git if you don't plan to release to RubyGems.org.

Install the gem and add to the application's Gemfile by executing:

```
$ bundle add UPDATE_WITH_YOUR_GEM_NAME_IMMEDIATELY_AFTER_RELEASE_TO_RUBYGEMS_ORG
```

If bundler is not being used to manage dependencies, install the gem by executing:

```
$ gem install UPDATE_WITH_YOUR_GEM_NAME_IMMEDIATELY_AFTER_RELEASE_TO_RUBYGEMS_ORG
```

## Usage

```
cdl = Cedarling::new policy_store: {yaml: File.read('../../test_files/policy-store_ok.yaml')}

auth_request = {
  access_token: your_access_token_here,
  id_token: your_id_token_here,
  userinfo_token: your_userinfo_token_here,
  action: %q(Jans::Action::"Update"),
  resource: {type: 'Jans::Issue', id: 'random_id', org_id: 'some_long_id', country: 'US'},
  context: {}
}

cdl.authorize auth_request
```

## Development

[Install ruby](https://www.ruby-lang.org/en/downloads/) with version >= 3.3.0. You can use your package manager, or [rbenv](https://github.com/rbenv/rbenv?tab=readme-ov-file#seamlessly-manage-your-apps-ruby-environment-with-rbenv), or [rvm](http://rvm.io/), or whatever you want.

Once you have a working ruby installation:

1) `bin/setup` to install dependencies

2) `rake compile` to compile the rust binding

3) `bin/console` for an interactive prompt that will allow you to experiment.

In the console you can do the following:

```
$ bin/console
3.3.0 :001 > Cedarling::VERSION 
 => "0.1.0" 
3.3.0 :002 > Cedarling.version_core
 => "1.1.6" 
3.3.0 :003 > cdl = Cedarling::new policy_store: {yaml: File.read('../../test_files/policy-s
tore_ok.yaml')}
 => #<Engine:0x00007f8f184c7b70> 
3.3.0 :004 > 
```

### development loop

The simplest is to just say `rake`, which will build the necessary rust code, and run the ruby specs.

Effectively, it does `cargo build -p cedarling_ruby` and then `rspec`.

You could also say, for example: `cargo build -p cedarling_ruby && rspec`.

Or even just `rspec` if you want a really tight loop on the specs. Of course that won't build the rust code.

## Local Installation

NOTE not working yet.

You won't want to do this, unless it fails to install via `bundler` or `Gemfile`

To install this gem onto your local machine, run `bundle exec rake install`. To release a new version, update the version number in `version.rb`, and then run `bundle exec rake release`, which will create a git tag for the version, push git commits and the created tag, and push the `.gem` file to [rubygems.org](https://rubygems.org).

## Contributing

Bug reports and pull requests are welcome on GitHub at https://github.com/gluu/cedarling.

## License

The gem is available as open source under the terms of the Apache License. See the [copy in jans project](https://github.com/JanssenProject/jans/blob/main/LICENSE).
