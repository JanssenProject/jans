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

  let :policy_store_yaml do
    File.read "#{__dir__}/../../../test_files/policy-store_ok.yaml"
  end

  describe 'create' do
    it 'bootstraps using old-style plain yaml string' do
      Cedarling::new policy_store: policy_store_yaml, name: "rubyling"
    end

    it 'minimal' do
      Cedarling::new policy_store: {yaml: policy_store_yaml}
    end

    it 'signature algorithms' do
      Cedarling::new signature_algorithms: %w[HS256 RS256], policy_store: {yaml: policy_store_yaml}
    end

    it 'has a name' do
      Cedarling::new policy_store: policy_store_yaml, name: "rubyling"
    end

    it 'fails for incorrect signature algorithms' do
      expect do
        Cedarling::new signature_algorithms: %w[HS123 RS456], policy_store: {yaml: policy_store_yaml}
      end.to raise_error(RuntimeError, /ServiceConfig.ParseAlgorithm.UnimplementedAlgorithm."HS123".../)
    end

    it 'fails with excessive log_ttl' do
      expect do
        Cedarling::new log_ttl: Cedarling::MAX_LOG_TTL + 1, policy_store: {yaml: policy_store_yaml}, name: "rubyling"
      end.to raise_error(RuntimeError, /#{Cedarling::MAX_LOG_TTL + 1} exceeds max of #{Cedarling::MAX_LOG_TTL}/)
    end

    it 'fails with zero log_ttl' do
      expect do
        Cedarling.new log_ttl: 0, policy_store: {yaml: policy_store_yaml}
      end.to raise_error(RuntimeError, /minimum 1/)
    end

    it 'suitable log_ttl' do
      Cedarling.new log_ttl: 1000, policy_store: {yaml: policy_store_yaml}
    end

    it 'fails with bad signature algorithms' do
      expect do
        Cedarling::new signature_algorithms: %w[HS123], policy_store: {yaml: policy_store_yaml}
      end.to raise_error(RuntimeError, /ServiceConfig.ParseAlgorithm.UnimplementedAlgorithm."HS123"/)
    end
  end
    end
  end
end
