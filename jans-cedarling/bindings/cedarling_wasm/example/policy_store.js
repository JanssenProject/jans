export const policy_store = JSON.stringify({
  "cedar_version": "v4.0.0",
  "policy_stores": {
    "gICAgcHJpbmNpcGFsIGlz": {
      "name": "Jans",
      "description": "",
      "policies": {
        "840da5d85403f35ea76519ed1a18a33989f855bf1cf8": {
          "description": "simple policy example for principal user",
          "creation_date": "2024-09-20T17:22:39.996050",
          "policy_content": "cGVybWl0CigKcHJpbmNpcGFsIGlzIEphbnM6OlVzZXIsCmFjdGlvbiBpbiBbSmFuczo6QWN0aW9uOjoiUmVhZCJdLApyZXNvdXJjZSBpcyBKYW5zOjpBcHBsaWNhdGlvbgopd2hlbnsKcmVzb3VyY2UubmFtZSA9PSAiU29tZSBBcHBsaWNhdGlvbiIKfTs="
        },
        "444da5d85403f35ea76519ed1a18a33989f855bf1cf8": {
          "description": "simple policy example for principal workload",
          "creation_date": "2024-09-20T17:22:39.996050",
          "policy_content": "cGVybWl0CigKcHJpbmNpcGFsIGlzIEphbnM6Oldvcmtsb2FkLAphY3Rpb24gaW4gW0phbnM6OkFjdGlvbjo6IlJlYWQiXSwKcmVzb3VyY2UgaXMgSmFuczo6QXBwbGljYXRpb24KKXdoZW57CnJlc291cmNlLm5hbWUgPT0gIlNvbWUgQXBwbGljYXRpb24iCn07"
        }
      },
      "trusted_issuers": {
        "595426354a058891fa795ba3d5109af177c684ab5875": {
          "name": "Jans",
          "description": "Janssen",
          "openid_configuration_endpoint": "https://account.gluu.org/.well-known/openid-configuration",
          "access_tokens": {
            "trusted": true,
            "principal_identifier": "jti"
          },
          "id_tokens": {},
          "userinfo_tokens": {
            "user_id": "sub",
            "role_mapping": "role",
            "claim_mapping": {
              "email": {
                "parser": "regex",
                "type": "Jans::email_address",
                "regex_expression": "^(?P<UID>[^@]+)@(?P<DOMAIN>.+)$",
                "UID": {
                  "attr": "uid",
                  "type": "String"
                },
                "DOMAIN": {
                  "attr": "domain",
                  "type": "String"
                }
              },
              "profile": {
                "parser": "regex",
                "type": "Jans::Url",
                "regex_expression": "(?x) ^(?P<SCHEME>[a-zA-Z][a-zA-Z0-9+.-]*):\\/\\/(?P<HOST>[^\\/:\\#?]+)(?::(?<PORT>\\d+))?(?P<PATH>\\/[^?\\#]*)?(?:\\?(?P<QUERY>[^\\#]*))?(?:(?P<FRAGMENT>.*))?/gm",
                "SCHEME": {
                  "attr": "scheme",
                  "type": "String"
                },
                "HOST": {
                  "attr": "host",
                  "type": "String"
                },
                "PORT": {
                  "attr": "port",
                  "type": "String"
                },
                "PATH": {
                  "attr": "path",
                  "type": "String"
                },
                "QUERY": {
                  "attr": "query",
                  "type": "String"
                },
                "FRAGMENT": {
                  "attr": "fragment",
                  "type": "String"
                }
              }
            }
          },
          "tx_tokens": {}
        }
      },
      "schema": "eyJKYW5zIjp7ImNvbW1vblR5cGVzIjp7IkNvbnRleHQiOnsidHlwZSI6IlJlY29yZCIsImF0dHJpYnV0ZXMiOnsiY3VycmVudF90aW1lIjp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJMb25nIn0sImRldmljZV9oZWFsdGgiOnsidHlwZSI6IlNldCIsImVsZW1lbnQiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyJ9fSwiZnJhdWRfaW5kaWNhdG9ycyI6eyJ0eXBlIjoiU2V0IiwiZWxlbWVudCI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiU3RyaW5nIn19LCJnZW9sb2NhdGlvbiI6eyJ0eXBlIjoiU2V0IiwiZWxlbWVudCI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiU3RyaW5nIn19LCJuZXR3b3JrIjp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJTdHJpbmcifSwibmV0d29ya190eXBlIjp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJTdHJpbmcifSwib3BlcmF0aW5nX3N5c3RlbSI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiU3RyaW5nIn0sInVzZXJfYWdlbnQiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyJ9fX0sIlVybCI6eyJ0eXBlIjoiUmVjb3JkIiwiYXR0cmlidXRlcyI6eyJob3N0Ijp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJTdHJpbmcifSwicGF0aCI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiU3RyaW5nIn0sInByb3RvY29sIjp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJTdHJpbmcifX19LCJlbWFpbF9hZGRyZXNzIjp7InR5cGUiOiJSZWNvcmQiLCJhdHRyaWJ1dGVzIjp7ImRvbWFpbiI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiU3RyaW5nIn0sInVpZCI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiU3RyaW5nIn19fX0sImVudGl0eVR5cGVzIjp7IkFjY2Vzc190b2tlbiI6eyJzaGFwZSI6eyJ0eXBlIjoiUmVjb3JkIiwiYXR0cmlidXRlcyI6eyJhdWQiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyJ9LCJleHAiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IkxvbmcifSwiaWF0Ijp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJMb25nIn0sImlzcyI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiVHJ1c3RlZElzc3VlciJ9LCJqdGkiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyJ9LCJuYmYiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IkxvbmcifSwic2NvcGUiOnsidHlwZSI6IlNldCIsImVsZW1lbnQiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyJ9fX19fSwiQXBwbGljYXRpb24iOnsic2hhcGUiOnsidHlwZSI6IlJlY29yZCIsImF0dHJpYnV0ZXMiOnsiYXBwX2lkIjp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJTdHJpbmcifSwibmFtZSI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiU3RyaW5nIn0sInVybCI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiVXJsIn19fX0sIkhUVFBfUmVxdWVzdCI6eyJzaGFwZSI6eyJ0eXBlIjoiUmVjb3JkIiwiYXR0cmlidXRlcyI6eyJhY2NlcHQiOnsidHlwZSI6IlNldCIsImVsZW1lbnQiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyJ9fSwiaGVhZGVyIjp7InR5cGUiOiJTZXQiLCJlbGVtZW50Ijp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJTdHJpbmcifX0sInVybCI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiVXJsIn19fX0sIlJvbGUiOnt9LCJUcnVzdGVkSXNzdWVyIjp7InNoYXBlIjp7InR5cGUiOiJSZWNvcmQiLCJhdHRyaWJ1dGVzIjp7Imlzc3Vlcl9lbnRpdHlfaWQiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlVybCJ9fX19LCJVc2VyIjp7Im1lbWJlck9mVHlwZXMiOlsiUm9sZSJdLCJzaGFwZSI6eyJ0eXBlIjoiUmVjb3JkIiwiYXR0cmlidXRlcyI6eyJlbWFpbCI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiZW1haWxfYWRkcmVzcyJ9LCJwaG9uZV9udW1iZXIiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyIsInJlcXVpcmVkIjpmYWxzZX0sInJvbGUiOnsidHlwZSI6IlNldCIsImVsZW1lbnQiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyJ9fSwic3ViIjp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJTdHJpbmcifSwidXNlcm5hbWUiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyIsInJlcXVpcmVkIjpmYWxzZX19fX0sIlVzZXJpbmZvX3Rva2VuIjp7InNoYXBlIjp7InR5cGUiOiJSZWNvcmQiLCJhdHRyaWJ1dGVzIjp7ImF1ZCI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiU3RyaW5nIn0sImJpcnRoZGF0ZSI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiU3RyaW5nIiwicmVxdWlyZWQiOmZhbHNlfSwiZW1haWwiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6ImVtYWlsX2FkZHJlc3MiLCJyZXF1aXJlZCI6ZmFsc2V9LCJleHAiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IkxvbmciLCJyZXF1aXJlZCI6ZmFsc2V9LCJpYXQiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IkxvbmciLCJyZXF1aXJlZCI6ZmFsc2V9LCJpc3MiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlRydXN0ZWRJc3N1ZXIifSwianRpIjp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJTdHJpbmcifSwibmFtZSI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiU3RyaW5nIiwicmVxdWlyZWQiOmZhbHNlfSwicGhvbmVfbnVtYmVyIjp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJTdHJpbmciLCJyZXF1aXJlZCI6ZmFsc2V9LCJyb2xlIjp7InR5cGUiOiJTZXQiLCJlbGVtZW50Ijp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJTdHJpbmcifSwicmVxdWlyZWQiOmZhbHNlfSwic3ViIjp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJTdHJpbmcifX19fSwiV29ya2xvYWQiOnsic2hhcGUiOnsidHlwZSI6IlJlY29yZCIsImF0dHJpYnV0ZXMiOnsiY2xpZW50X2lkIjp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJTdHJpbmcifSwiaXNzIjp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJUcnVzdGVkSXNzdWVyIn0sIm5hbWUiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyIsInJlcXVpcmVkIjpmYWxzZX0sInJwX2lkIjp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJTdHJpbmciLCJyZXF1aXJlZCI6ZmFsc2V9LCJzcGlmZmVfaWQiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyIsInJlcXVpcmVkIjpmYWxzZX19fX0sImlkX3Rva2VuIjp7InNoYXBlIjp7InR5cGUiOiJSZWNvcmQiLCJhdHRyaWJ1dGVzIjp7ImFjciI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiU3RyaW5nIn0sImFtciI6eyJ0eXBlIjoiU2V0IiwiZWxlbWVudCI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiU3RyaW5nIn19LCJhdWQiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyJ9LCJhenAiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyIsInJlcXVpcmVkIjpmYWxzZX0sImJpcnRoZGF0ZSI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiU3RyaW5nIiwicmVxdWlyZWQiOmZhbHNlfSwiZW1haWwiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6ImVtYWlsX2FkZHJlc3MiLCJyZXF1aXJlZCI6ZmFsc2V9LCJleHAiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IkxvbmcifSwiaWF0Ijp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJMb25nIn0sImlzcyI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiVHJ1c3RlZElzc3VlciJ9LCJqdGkiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyJ9LCJuYW1lIjp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJTdHJpbmciLCJyZXF1aXJlZCI6ZmFsc2V9LCJwaG9uZV9udW1iZXIiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyIsInJlcXVpcmVkIjpmYWxzZX0sInJvbGUiOnsidHlwZSI6IlNldCIsImVsZW1lbnQiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyJ9LCJyZXF1aXJlZCI6ZmFsc2V9LCJzdWIiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyJ9fX19fSwiYWN0aW9ucyI6eyJDb21wYXJlIjp7ImFwcGxpZXNUbyI6eyJyZXNvdXJjZVR5cGVzIjpbIkFwcGxpY2F0aW9uIl0sInByaW5jaXBhbFR5cGVzIjpbIlVzZXIiLCJSb2xlIiwiV29ya2xvYWQiXSwiY29udGV4dCI6eyJ0eXBlIjoiQ29udGV4dCJ9fX0sIkRFTEVURSI6eyJhcHBsaWVzVG8iOnsicmVzb3VyY2VUeXBlcyI6WyJIVFRQX1JlcXVlc3QiXSwicHJpbmNpcGFsVHlwZXMiOlsiV29ya2xvYWQiXSwiY29udGV4dCI6eyJ0eXBlIjoiQ29udGV4dCJ9fX0sIkV4ZWN1dGUiOnsiYXBwbGllc1RvIjp7InJlc291cmNlVHlwZXMiOlsiQXBwbGljYXRpb24iXSwicHJpbmNpcGFsVHlwZXMiOlsiVXNlciIsIlJvbGUiLCJXb3JrbG9hZCJdLCJjb250ZXh0Ijp7InR5cGUiOiJDb250ZXh0In19fSwiR0VUIjp7ImFwcGxpZXNUbyI6eyJyZXNvdXJjZVR5cGVzIjpbIkhUVFBfUmVxdWVzdCJdLCJwcmluY2lwYWxUeXBlcyI6WyJXb3JrbG9hZCJdLCJjb250ZXh0Ijp7InR5cGUiOiJDb250ZXh0In19fSwiSEVBRCI6eyJhcHBsaWVzVG8iOnsicmVzb3VyY2VUeXBlcyI6WyJIVFRQX1JlcXVlc3QiXSwicHJpbmNpcGFsVHlwZXMiOlsiV29ya2xvYWQiXSwiY29udGV4dCI6eyJ0eXBlIjoiQ29udGV4dCJ9fX0sIk1vbml0b3IiOnsiYXBwbGllc1RvIjp7InJlc291cmNlVHlwZXMiOlsiQXBwbGljYXRpb24iXSwicHJpbmNpcGFsVHlwZXMiOlsiVXNlciIsIlJvbGUiLCJXb3JrbG9hZCJdLCJjb250ZXh0Ijp7InR5cGUiOiJDb250ZXh0In19fSwiUEFUQ0giOnsiYXBwbGllc1RvIjp7InJlc291cmNlVHlwZXMiOlsiSFRUUF9SZXF1ZXN0Il0sInByaW5jaXBhbFR5cGVzIjpbIldvcmtsb2FkIl0sImNvbnRleHQiOnsidHlwZSI6IkNvbnRleHQifX19LCJQVVQiOnsiYXBwbGllc1RvIjp7InJlc291cmNlVHlwZXMiOlsiSFRUUF9SZXF1ZXN0Il0sInByaW5jaXBhbFR5cGVzIjpbIldvcmtsb2FkIl0sImNvbnRleHQiOnsidHlwZSI6IkNvbnRleHQifX19LCJSZWFkIjp7ImFwcGxpZXNUbyI6eyJyZXNvdXJjZVR5cGVzIjpbIkFwcGxpY2F0aW9uIl0sInByaW5jaXBhbFR5cGVzIjpbIlVzZXIiLCJSb2xlIiwiV29ya2xvYWQiXSwiY29udGV4dCI6eyJ0eXBlIjoiQ29udGV4dCJ9fX0sIlNlYXJjaCI6eyJhcHBsaWVzVG8iOnsicmVzb3VyY2VUeXBlcyI6WyJBcHBsaWNhdGlvbiJdLCJwcmluY2lwYWxUeXBlcyI6WyJVc2VyIiwiUm9sZSIsIldvcmtsb2FkIl0sImNvbnRleHQiOnsidHlwZSI6IkNvbnRleHQifX19LCJTaGFyZSI6eyJhcHBsaWVzVG8iOnsicmVzb3VyY2VUeXBlcyI6WyJBcHBsaWNhdGlvbiJdLCJwcmluY2lwYWxUeXBlcyI6WyJVc2VyIiwiUm9sZSIsIldvcmtsb2FkIl0sImNvbnRleHQiOnsidHlwZSI6IkNvbnRleHQifX19LCJUYWciOnsiYXBwbGllc1RvIjp7InJlc291cmNlVHlwZXMiOlsiQXBwbGljYXRpb24iXSwicHJpbmNpcGFsVHlwZXMiOlsiVXNlciIsIlJvbGUiLCJXb3JrbG9hZCJdLCJjb250ZXh0Ijp7InR5cGUiOiJDb250ZXh0In19fSwiVGVzdCI6eyJhcHBsaWVzVG8iOnsicmVzb3VyY2VUeXBlcyI6WyJBcHBsaWNhdGlvbiJdLCJwcmluY2lwYWxUeXBlcyI6WyJVc2VyIiwiUm9sZSIsIldvcmtsb2FkIl0sImNvbnRleHQiOnsidHlwZSI6IkNvbnRleHQifX19LCJXcml0ZSI6eyJhcHBsaWVzVG8iOnsicmVzb3VyY2VUeXBlcyI6WyJBcHBsaWNhdGlvbiJdLCJwcmluY2lwYWxUeXBlcyI6WyJVc2VyIiwiUm9sZSIsIldvcmtsb2FkIl0sImNvbnRleHQiOnsidHlwZSI6IkNvbnRleHQifX19fX19"
    }
  }
});
