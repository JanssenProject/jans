# frozen_string_literal: true

RSpec.describe Cedarling do
  it "has a version number" do
    expect(Cedarling::VERSION).not_to be nil
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
