# frozen_string_literal: true
require 'json'

RSpec.describe Cedarling do
  it "has a gem version number" do
    expect(Cedarling::VERSION).not_to be nil
  end

  it "has the core version number" do
    expect(Cedarling::version_core).not_to be nil
    expect(Cedarling::version_core).to match(/^\d+.\d+.\d+$/)
  end

  it "core version number matches actual core version" do
    # fetch manifest data using cargo
    json = JSON.parse `cargo metadata --format-version 1 --manifest-path ../../cedarling/Cargo.toml`, symbolize_names: true
    cedarling_manifest_version = json[:packages].find{ _1[:name] == 'cedarling' }.dig(:version)
    # compare to version from source
    expect(cedarling_manifest_version).to eq(Cedarling::version_core)
  end

  it 'bootstraps' do
    policy_store_text = File.read "#{__dir__}/../../../test_files/policy-store_ok.json"
    engine = Cedarling::bootstrap "rubyling", policy_store_text, Cedarling::MAX_LOG_TTL
  end

  it 'bootstrap fails with excessive log_ttl' do
    policy_store_text = File.read "#{__dir__}/../../../test_files/policy-store_ok.json"
    expect {
      Cedarling::bootstrap "rubyling", policy_store_text, Cedarling::MAX_LOG_TTL + 1
    }.to raise_error(RuntimeError, "#{Cedarling::MAX_LOG_TTL + 1} exceeds max of #{Cedarling::MAX_LOG_TTL}")
  end
end
