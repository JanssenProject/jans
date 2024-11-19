# frozen_string_literal: true
require 'jwt'

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
      end.to raise_error(Cedarling::Error, /algorithm is not yet implemented: HS123/)
    end

    it 'fails with excessive log_ttl' do
      expect do
        Cedarling::new log_ttl: Cedarling::MAX_LOG_TTL + 1, policy_store: {yaml: policy_store_yaml}, name: "rubyling"
      end.to raise_error(Cedarling::Error, /#{Cedarling::MAX_LOG_TTL + 1} exceeds max of #{Cedarling::MAX_LOG_TTL}/)
    end

    it 'fails with zero log_ttl' do
      expect do
        Cedarling.new log_ttl: 0, policy_store: {yaml: policy_store_yaml}
      end.to raise_error(Cedarling::Error, /minimum 1/)
    end

    it 'suitable log_ttl' do
      Cedarling.new log_ttl: 1000, policy_store: {yaml: policy_store_yaml}
    end

    it 'fails with bad signature algorithms' do
      expect do
        Cedarling::new signature_algorithms: %w[HS123], policy_store: {yaml: policy_store_yaml}
      end.to raise_error(Cedarling::Error, /algorithm is not yet implemented: HS123/)
    end
  end

  describe 'authorize' do
    let :access_token_payload do
      {
        sub: 'boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0',
        code: 'bf1934f6-3905-420a-8299-6b2e3ffddd6e',
        iss: 'https://admin-ui-test.gluu.org',
        token_type: 'Bearer',
        client_id: '5b4487c4-8db1-409d-a653-f907b8094039',
        aud: '5b4487c4-8db1-409d-a653-f907b8094039',
        acr: 'basic',
        'x5t#S256': '',
        scope: [
          'openid',
          'profile'
        ],
        org_id: 'some_long_id',
        auth_time: 1724830746,
        exp: 1724945978,
        iat: 1724832259,
        jti: 'lxTmCVRFTxOjJgvEEpozMQ',
        name: 'Default Admin User',
        status: {
          status_list: {
            idx: 201,
            uri: 'https://admin-ui-test.gluu.org/jans-auth/restv1/status_list'
          }
        }
      }
    end

    let :access_token do
      JWT.encode(access_token_payload, nil, 'HS256', {kid: 'a96f9a86-2e80-447e-bb81-a59b8759a53a'}) # nil means no password
    end

    let :id_token do
      payload = {
        acr: 'basic',
        amr: '10',
        aud: '5b4487c4-8db1-409d-a653-f907b8094039',
        exp: 1724835859,
        iat: 1724832259,
        sub: 'boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0',
        iss: 'https://admin-ui-test.gluu.org',
        jti: 'sk3T40NYSYuk5saHZNpkZw',
        nonce: 'c3872af9-a0f5-4c3f-a1af-f9d0e8846e81',
        sid: '6a7fe50a-d810-454d-be5d-549d29595a09',
        jansOpenIDConnectVersion: 'openidconnect-1.0',
        c_hash: 'pGoK6Y_RKcWHkUecM9uw6Q',
        auth_time: 1724830746,
        grant: 'authorization_code',
        status: {
          status_list: {idx: 202, uri: 'https://admin-ui-test.gluu.org/jans-auth/restv1/status_list'}
        },
        role: 'Admin'
      }
      JWT.encode(payload, nil, 'HS256') # nil means no password
    end

    let :userinfo_payload do
      {
        country: 'US',
        email: 'user@example.com',
        username: 'UserNameExample',
        sub: 'boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0',
        iss: 'https://admin-ui-test.gluu.org',
        given_name: 'Admin',
        middle_name: 'Admin',
        inum: '8d1cde6a-1447-4766-b3c8-16663e13b458',
        client_id: '5b4487c4-8db1-409d-a653-f907b8094039',
        aud: '5b4487c4-8db1-409d-a653-f907b8094039',
        updated_at: 1724778591,
        name: 'Default Admin User',
        nickname: 'Admin',
        family_name: 'User',
        jti: 'faiYvaYIT0cDAT7Fow0pQw',
        jansAdminUIRole: %w[api-admin],
        exp: 1724945978
      }
    end

    let :userinfo_token do
      JWT.encode(userinfo_payload, nil, 'HS256') # nil means no password
    end

    let :request_no_tokens do
      {
        access_token:,
        id_token:,
        userinfo_token:,
        action: %q(Jans::Action::"Update"),
        resource: {type: 'Jans::Issue', id: 'random_id', org_id: 'some_long_id', country: 'US'},
        context: {}
      }
    end

    let :request_with_tokens do
      request = request_no_tokens.dup
      request[:access_token] = access_token
      request[:userinfo_token] = userinfo_token
      request[:id_token] = id_token
      request
    end

    it 'fails with invalid token encoding and without validation' do
      engine = Cedarling::new policy_store: {yaml: policy_store_yaml}
      broken_access_token = "borked" + access_token
      request_no_tokens[:access_token] = broken_access_token
      expect do
        engine.authorize(request_no_tokens)
      end.to raise_error(RuntimeError, /Error parsing the JWT: Base64 error: Invalid last symbol/)
    end

    it 'fails with valid but incorrect token and with validation' do
      engine = Cedarling::new policy_store: {yaml: policy_store_yaml}
      access_token_payload[:org_id] = 'incorrect_id'
      modified_access_token = JWT.encode(access_token_payload, nil, 'HS256')
      request_with_tokens[:access_token] = modified_access_token

      rsp = engine.authorize(request_with_tokens)
      rsp.should_not be_allowed

      rsp.to_h.should == {
        "workload" => {"allow"=>false, "policy_ids"=>[]},
        "person"   => {"allow"=>true, "policy_ids"=>["444da5d85403f35ea76519ed1a18a33989f855bf1cf8"]},
      }
    end

    describe 'allowed with no validation' do
      it 'no tokens' do
        engine = Cedarling::new policy_store: {yaml: policy_store_yaml}
        rsp = engine.authorize(request_no_tokens)
        rsp.should be_allowed
      end

      it 'to_h shows policies and errors' do
        engine = Cedarling::new policy_store: {yaml: policy_store_yaml}
        rsp = engine.authorize(request_no_tokens)
        rsp.to_h.should == {
          "workload" => {"allow"=>true, "policy_ids"=>["840da5d85403f35ea76519ed1a18a33989f855bf1cf8"]},
          "person"   => {"allow"=>true, "policy_ids"=>["444da5d85403f35ea76519ed1a18a33989f855bf1cf8"]},
        }
      end
    end

    xit 'succeeds with validation' do
      # requires Cedarling to be set up with the relevant keys referenced by kid in key header
      engine = Cedarling::new signature_algorithms: %w[HS256 RS256], policy_store: {yaml: policy_store_yaml}
      require 'pry'; binding.pry
      rsp = engine.authorize(request_with_tokens)
      rsp.should be_allowed
    end
  end
end
