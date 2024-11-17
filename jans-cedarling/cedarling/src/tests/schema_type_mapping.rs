/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::utils::*;

static POLICY_STORE_RAW_YAML: &str = include_str!("../../../test_files/agama-store_2.yaml");

/// Test loading real policy store from agama lab
#[test]
fn load_agama_policy_store() {
    let cedarling = get_cedarling(PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()));

    // deserialize `Request` from json
    let request = Request::deserialize(serde_json::json!(
        {
            "access_token": "eyJraWQiOiJjb25uZWN0X2JmZmVhYzA4LTEyZTgtNGJiYy04YjA3LTAxOTk0NGI3MjJlNF9zaWdfcnMyNTYiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJKM0JtdG5QUEI4QmpNYlNjV21SOGNqVDlnV0NDVEhLZlNmMGRrYk92aEdnIiwiY29kZSI6IjY5N2RhODBkLTE2YWQtNDFmOC1hZDhlLWM3MWY4ODFjNTQ3MyIsImlzcyI6Imh0dHBzOi8vdGVzdC1jYXNhLmdsdXUuaW5mbyIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJjbGllbnRfaWQiOiI5NWJkNjNkMi04NWVkLTQwYWQtYmQwMy0zYzE4YWY3OTdjYTQiLCJhdWQiOiI5NWJkNjNkMi04NWVkLTQwYWQtYmQwMy0zYzE4YWY3OTdjYTQiLCJhY3IiOiJzaW1wbGVfcGFzc3dvcmRfYXV0aCIsIng1dCNTMjU2IjoiIiwibmJmIjoxNzMwNDk0NTQzLCJzY29wZSI6WyJyb2xlIiwib3BlbmlkIl0sImF1dGhfdGltZSI6MTczMDQ5NDU0MiwiZXhwIjoxNzMwNTc0MjQ1LCJpYXQiOjE3MzA0OTQ1NDMsImp0aSI6InFwQ3U1MlowUzh5bmZaN3VmQ1hRb3ciLCJ1c2VybmFtZSI6Ik9sZWggQm96aG9rIiwic3RhdHVzIjp7InN0YXR1c19saXN0Ijp7ImlkeCI6MTAwMywidXJpIjoiaHR0cHM6Ly90ZXN0LWNhc2EuZ2x1dS5pbmZvL2phbnMtYXV0aC9yZXN0djEvc3RhdHVzX2xpc3QifX19.A-k8fP9yqF-LwyNniohlLzwy2y0smbkavubNHKiNn8zAWAz0PFcQXnDKJEpcigXS7iwBRieHcEAgCrBCbsPKZDHOohwx2CFNPK1AvECRmd0U69siaEJqljECiUHqLkHOT9LD89Ag752QNauuQvXnHKuVIKJ7ykg7Jcc5-gi_mH_OfGAz-yYPYJLy0tBiT9LDFu1sQT04P5MqzrSxwBk3PW7Af3W0Dl-hl77SSAAKy7TkYuTEPWLat0nMwuLNzgBkmsGwPdPGmZl5YqMv1VOpr19Xjopr8lMAHN1CdEEFCXoBZewwXGPQFzoF_J9CW95tx_T16Z6iM2EXoIJyplgSwg".to_string(),
            "id_token": "eyJraWQiOiJjb25uZWN0X2JmZmVhYzA4LTEyZTgtNGJiYy04YjA3LTAxOTk0NGI3MjJlNF9zaWdfcnMyNTYiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJhdF9oYXNoIjoiemFqTC1JRVBiSjdYcHJiQWdpNUxBZyIsInN1YiI6IkozQm10blBQQjhCak1iU2NXbVI4Y2pUOWdXQ0NUSEtmU2YwZGtiT3ZoR2ciLCJhbXIiOltdLCJpc3MiOiJodHRwczovL3Rlc3QtY2FzYS5nbHV1LmluZm8iLCJub25jZSI6ImI5YjZkZjUxLWEwNGEtNDc1YS05MTQxLTNmZTU4OWMyYWFiOCIsInNpZCI6IjcxZWVkYjNhLTdjMTgtNDIwYy05ZmVhLTM3ZDY1MzI5OTBlNiIsImphbnNPcGVuSURDb25uZWN0VmVyc2lvbiI6Im9wZW5pZGNvbm5lY3QtMS4wIiwiYXVkIjoiOTViZDYzZDItODVlZC00MGFkLWJkMDMtM2MxOGFmNzk3Y2E0IiwiYWNyIjoic2ltcGxlX3Bhc3N3b3JkX2F1dGgiLCJjX2hhc2giOiJwUWk5cllxbVNDVmMzdEstLTJBZ2lBIiwibmJmIjoxNzMwNDk0NTQzLCJhdXRoX3RpbWUiOjE3MzA0OTQ1NDIsImV4cCI6MTczMDQ5ODE0MywiZ3JhbnQiOiJhdXRob3JpemF0aW9uX2NvZGUiLCJpYXQiOjE3MzA0OTQ1NDMsImp0aSI6InYyU1dHZkFFUUdXWjFtUERTSlB2YmciLCJzdGF0dXMiOnsic3RhdHVzX2xpc3QiOnsiaWR4IjoxMDA0LCJ1cmkiOiJodHRwczovL3Rlc3QtY2FzYS5nbHV1LmluZm8vamFucy1hdXRoL3Jlc3R2MS9zdGF0dXNfbGlzdCJ9fX0.Ot6WNQlg4hVPA4b6dRPO-tr6V20EzEm_3SMN-qRkfpkWQ-GSccFLed5G4sBLh_YIN-qh-P7gsFGg7Y6QS7tsR6CgNB0uVu3lqpqqrkvwxUw0DS4DYQFZ2pZygfVqQc9o7V9JThdVG7VG_SZXnKa8H8ORmp9JbTOTrLqAOgoQ1YdFfcceWob5BcFLCOXXOao90ESC5ntIHXm4lVwjN19odJHgoJ9qRFE69pm4vgqZ211cbfkoA_D12TDEaVmJ5n_982i7OvwK2zdNHlqlVTKN9Ncy6gvvRHb1RsgaVjp5Nd--oMdlb76wy94VIqdbFqDAXpogzS-K2m5n0yGfOhchAw".to_string(),
            "userinfo_token":  "eyJraWQiOiJjb25uZWN0X2JmZmVhYzA4LTEyZTgtNGJiYy04YjA3LTAxOTk0NGI3MjJlNF9zaWdfcnMyNTYiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJKM0JtdG5QUEI4QmpNYlNjV21SOGNqVDlnV0NDVEhLZlNmMGRrYk92aEdnIiwiYXVkIjoiOTViZDYzZDItODVlZC00MGFkLWJkMDMtM2MxOGFmNzk3Y2E0Iiwicm9sZSI6WyJNYW5hZ2VyIiwiU3VwcG9ydCJdLCJpc3MiOiJodHRwczovL3Rlc3QtY2FzYS5nbHV1LmluZm8iLCJqdGkiOiJxT3hrbE1ZZlNmcWRZZ1hsMDFqOXdBIiwiY2xpZW50X2lkIjoiOTViZDYzZDItODVlZC00MGFkLWJkMDMtM2MxOGFmNzk3Y2E0In0.DyXs_NEN6-KMebTHJu1_54CXOlrEWube85pV4ZIoNz_EqePZnirSydfNQJZMf1RLXauZIhug0EOpGxbIqRMfGTOlHqTc9nwXN82lRSkF0ctUkl-t3jeJNOXmQQLjDGEhI2IXjmDcIwvms1qy-QUtct9ccniEt6SdfROnSYhY8rAVYLaf34UJmUCav01Q9iGBn5E_ASr4G8zZibq4b9z_AX6DNZilmVeJIy4HLPRNdAtsJs6YHuQDN1QzQNJiFxrJlytMXwdMh1mXRIADBFVsIVte0fHOJBqhPS60t81qsa4r9tE9tJ-li5yRLGNFgab0zdUjPp0M6DrKUigq-nPBQg".to_string(),
            "action": "Test::Action::\"Search\"",
            "resource": {
                "id": "SomeID",
                "type": "Test::Empty",
            },
            "context": {
                "current_time":"1",
                "device_health": [],
                "fraud_indicators": [],
                "geolocation": [],
                "network": "vpn",
                "network_type": "vpn",
                "operating_system": "linux",
                "user_agent": "Smith",
            },
        }
    ))
    .expect("Request should be deserialized from json");

    let result = cedarling
        .authorize(request)
        .expect("request should be parsed without errors");

    assert!(result.is_allowed(), "request result should be allowed");
}
