const BOOTSTRAP_CONFIG = {
    "CEDARLING_APPLICATION_NAME": "My App",
    "CEDARLING_POLICY_STORE_LOCAL": {
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
                        "tokens_metadata": {
                            "access_token": {
                                "entity_type_name": "Jans::Access_token",
                            },
                            "id_token": {
                                "entity_type_name": "Jans::Id_token",
                            },
                            "userinfo_token": {
                                "entity_type_name": "Jans::Userinfo_token",
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
                            }
                        }
                    }
                },
                "schema": "eyJKYW5zIjp7ImNvbW1vblR5cGVzIjp7IkNvbnRleHQiOnsidHlwZSI6IlJlY29yZCIsImF0dHJpYnV0ZXMiOnsiY3VycmVudF90aW1lIjp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJMb25nIn0sImRldmljZV9oZWFsdGgiOnsidHlwZSI6IlNldCIsImVsZW1lbnQiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyJ9fSwiZnJhdWRfaW5kaWNhdG9ycyI6eyJ0eXBlIjoiU2V0IiwiZWxlbWVudCI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiU3RyaW5nIn19LCJnZW9sb2NhdGlvbiI6eyJ0eXBlIjoiU2V0IiwiZWxlbWVudCI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiU3RyaW5nIn19LCJuZXR3b3JrIjp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJTdHJpbmcifSwibmV0d29ya190eXBlIjp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJTdHJpbmcifSwib3BlcmF0aW5nX3N5c3RlbSI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiU3RyaW5nIn0sInVzZXJfYWdlbnQiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyJ9fX0sIlVybCI6eyJ0eXBlIjoiUmVjb3JkIiwiYXR0cmlidXRlcyI6eyJob3N0Ijp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJTdHJpbmcifSwicGF0aCI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiU3RyaW5nIn0sInByb3RvY29sIjp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJTdHJpbmcifX19LCJlbWFpbF9hZGRyZXNzIjp7InR5cGUiOiJSZWNvcmQiLCJhdHRyaWJ1dGVzIjp7ImRvbWFpbiI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiU3RyaW5nIn0sInVpZCI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiU3RyaW5nIn19fX0sImVudGl0eVR5cGVzIjp7IkFjY2Vzc190b2tlbiI6eyJzaGFwZSI6eyJ0eXBlIjoiUmVjb3JkIiwiYXR0cmlidXRlcyI6eyJhdWQiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyJ9LCJleHAiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IkxvbmcifSwiaWF0Ijp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJMb25nIn0sImlzcyI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiVHJ1c3RlZElzc3VlciJ9LCJqdGkiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyJ9LCJuYmYiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IkxvbmcifSwic2NvcGUiOnsidHlwZSI6IlNldCIsImVsZW1lbnQiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyJ9fX19fSwiQXBwbGljYXRpb24iOnsic2hhcGUiOnsidHlwZSI6IlJlY29yZCIsImF0dHJpYnV0ZXMiOnsiYXBwX2lkIjp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJTdHJpbmcifSwibmFtZSI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiU3RyaW5nIn0sInVybCI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiVXJsIn19fX0sIkhUVFBfUmVxdWVzdCI6eyJzaGFwZSI6eyJ0eXBlIjoiUmVjb3JkIiwiYXR0cmlidXRlcyI6eyJhY2NlcHQiOnsidHlwZSI6IlNldCIsImVsZW1lbnQiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyJ9fSwiaGVhZGVyIjp7InR5cGUiOiJTZXQiLCJlbGVtZW50Ijp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJTdHJpbmcifX0sInVybCI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiVXJsIn19fX0sIlJvbGUiOnt9LCJUcnVzdGVkSXNzdWVyIjp7InNoYXBlIjp7InR5cGUiOiJSZWNvcmQiLCJhdHRyaWJ1dGVzIjp7Imlzc3Vlcl9lbnRpdHlfaWQiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlVybCJ9fX19LCJVc2VyIjp7Im1lbWJlck9mVHlwZXMiOlsiUm9sZSJdLCJzaGFwZSI6eyJ0eXBlIjoiUmVjb3JkIiwiYXR0cmlidXRlcyI6eyJlbWFpbCI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiZW1haWxfYWRkcmVzcyJ9LCJwaG9uZV9udW1iZXIiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyIsInJlcXVpcmVkIjpmYWxzZX0sInJvbGUiOnsidHlwZSI6IlNldCIsImVsZW1lbnQiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyJ9fSwic3ViIjp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJTdHJpbmcifSwidXNlcm5hbWUiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyIsInJlcXVpcmVkIjpmYWxzZX19fX0sIlVzZXJpbmZvX3Rva2VuIjp7InNoYXBlIjp7InR5cGUiOiJSZWNvcmQiLCJhdHRyaWJ1dGVzIjp7ImF1ZCI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiU3RyaW5nIn0sImJpcnRoZGF0ZSI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiU3RyaW5nIiwicmVxdWlyZWQiOmZhbHNlfSwiZW1haWwiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6ImVtYWlsX2FkZHJlc3MiLCJyZXF1aXJlZCI6ZmFsc2V9LCJleHAiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IkxvbmciLCJyZXF1aXJlZCI6ZmFsc2V9LCJpYXQiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IkxvbmciLCJyZXF1aXJlZCI6ZmFsc2V9LCJpc3MiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlRydXN0ZWRJc3N1ZXIifSwianRpIjp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJTdHJpbmcifSwibmFtZSI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiU3RyaW5nIiwicmVxdWlyZWQiOmZhbHNlfSwicGhvbmVfbnVtYmVyIjp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJTdHJpbmciLCJyZXF1aXJlZCI6ZmFsc2V9LCJyb2xlIjp7InR5cGUiOiJTZXQiLCJlbGVtZW50Ijp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJTdHJpbmcifSwicmVxdWlyZWQiOmZhbHNlfSwic3ViIjp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJTdHJpbmcifX19fSwiV29ya2xvYWQiOnsic2hhcGUiOnsidHlwZSI6IlJlY29yZCIsImF0dHJpYnV0ZXMiOnsiY2xpZW50X2lkIjp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJTdHJpbmcifSwiaXNzIjp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJUcnVzdGVkSXNzdWVyIn0sIm5hbWUiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyIsInJlcXVpcmVkIjpmYWxzZX0sInJwX2lkIjp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJTdHJpbmciLCJyZXF1aXJlZCI6ZmFsc2V9LCJzcGlmZmVfaWQiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyIsInJlcXVpcmVkIjpmYWxzZX19fX0sIklkX3Rva2VuIjp7InNoYXBlIjp7InR5cGUiOiJSZWNvcmQiLCJhdHRyaWJ1dGVzIjp7ImFjciI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiU3RyaW5nIn0sImFtciI6eyJ0eXBlIjoiU2V0IiwiZWxlbWVudCI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiU3RyaW5nIn19LCJhdWQiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyJ9LCJhenAiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyIsInJlcXVpcmVkIjpmYWxzZX0sImJpcnRoZGF0ZSI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiU3RyaW5nIiwicmVxdWlyZWQiOmZhbHNlfSwiZW1haWwiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6ImVtYWlsX2FkZHJlc3MiLCJyZXF1aXJlZCI6ZmFsc2V9LCJleHAiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IkxvbmcifSwiaWF0Ijp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJMb25nIn0sImlzcyI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiVHJ1c3RlZElzc3VlciJ9LCJqdGkiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyJ9LCJuYW1lIjp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJTdHJpbmciLCJyZXF1aXJlZCI6ZmFsc2V9LCJwaG9uZV9udW1iZXIiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyIsInJlcXVpcmVkIjpmYWxzZX0sInJvbGUiOnsidHlwZSI6IlNldCIsImVsZW1lbnQiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyJ9LCJyZXF1aXJlZCI6ZmFsc2V9LCJzdWIiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyJ9fX19fSwiYWN0aW9ucyI6eyJDb21wYXJlIjp7ImFwcGxpZXNUbyI6eyJyZXNvdXJjZVR5cGVzIjpbIkFwcGxpY2F0aW9uIl0sInByaW5jaXBhbFR5cGVzIjpbIlVzZXIiLCJSb2xlIiwiV29ya2xvYWQiXSwiY29udGV4dCI6eyJ0eXBlIjoiQ29udGV4dCJ9fX0sIkRFTEVURSI6eyJhcHBsaWVzVG8iOnsicmVzb3VyY2VUeXBlcyI6WyJIVFRQX1JlcXVlc3QiXSwicHJpbmNpcGFsVHlwZXMiOlsiV29ya2xvYWQiXSwiY29udGV4dCI6eyJ0eXBlIjoiQ29udGV4dCJ9fX0sIkV4ZWN1dGUiOnsiYXBwbGllc1RvIjp7InJlc291cmNlVHlwZXMiOlsiQXBwbGljYXRpb24iXSwicHJpbmNpcGFsVHlwZXMiOlsiVXNlciIsIlJvbGUiLCJXb3JrbG9hZCJdLCJjb250ZXh0Ijp7InR5cGUiOiJDb250ZXh0In19fSwiR0VUIjp7ImFwcGxpZXNUbyI6eyJyZXNvdXJjZVR5cGVzIjpbIkhUVFBfUmVxdWVzdCJdLCJwcmluY2lwYWxUeXBlcyI6WyJXb3JrbG9hZCJdLCJjb250ZXh0Ijp7InR5cGUiOiJDb250ZXh0In19fSwiSEVBRCI6eyJhcHBsaWVzVG8iOnsicmVzb3VyY2VUeXBlcyI6WyJIVFRQX1JlcXVlc3QiXSwicHJpbmNpcGFsVHlwZXMiOlsiV29ya2xvYWQiXSwiY29udGV4dCI6eyJ0eXBlIjoiQ29udGV4dCJ9fX0sIk1vbml0b3IiOnsiYXBwbGllc1RvIjp7InJlc291cmNlVHlwZXMiOlsiQXBwbGljYXRpb24iXSwicHJpbmNpcGFsVHlwZXMiOlsiVXNlciIsIlJvbGUiLCJXb3JrbG9hZCJdLCJjb250ZXh0Ijp7InR5cGUiOiJDb250ZXh0In19fSwiUEFUQ0giOnsiYXBwbGllc1RvIjp7InJlc291cmNlVHlwZXMiOlsiSFRUUF9SZXF1ZXN0Il0sInByaW5jaXBhbFR5cGVzIjpbIldvcmtsb2FkIl0sImNvbnRleHQiOnsidHlwZSI6IkNvbnRleHQifX19LCJQVVQiOnsiYXBwbGllc1RvIjp7InJlc291cmNlVHlwZXMiOlsiSFRUUF9SZXF1ZXN0Il0sInByaW5jaXBhbFR5cGVzIjpbIldvcmtsb2FkIl0sImNvbnRleHQiOnsidHlwZSI6IkNvbnRleHQifX19LCJSZWFkIjp7ImFwcGxpZXNUbyI6eyJyZXNvdXJjZVR5cGVzIjpbIkFwcGxpY2F0aW9uIl0sInByaW5jaXBhbFR5cGVzIjpbIlVzZXIiLCJSb2xlIiwiV29ya2xvYWQiXSwiY29udGV4dCI6eyJ0eXBlIjoiQ29udGV4dCJ9fX0sIlNlYXJjaCI6eyJhcHBsaWVzVG8iOnsicmVzb3VyY2VUeXBlcyI6WyJBcHBsaWNhdGlvbiJdLCJwcmluY2lwYWxUeXBlcyI6WyJVc2VyIiwiUm9sZSIsIldvcmtsb2FkIl0sImNvbnRleHQiOnsidHlwZSI6IkNvbnRleHQifX19LCJTaGFyZSI6eyJhcHBsaWVzVG8iOnsicmVzb3VyY2VUeXBlcyI6WyJBcHBsaWNhdGlvbiJdLCJwcmluY2lwYWxUeXBlcyI6WyJVc2VyIiwiUm9sZSIsIldvcmtsb2FkIl0sImNvbnRleHQiOnsidHlwZSI6IkNvbnRleHQifX19LCJUYWciOnsiYXBwbGllc1RvIjp7InJlc291cmNlVHlwZXMiOlsiQXBwbGljYXRpb24iXSwicHJpbmNpcGFsVHlwZXMiOlsiVXNlciIsIlJvbGUiLCJXb3JrbG9hZCJdLCJjb250ZXh0Ijp7InR5cGUiOiJDb250ZXh0In19fSwiVGVzdCI6eyJhcHBsaWVzVG8iOnsicmVzb3VyY2VUeXBlcyI6WyJBcHBsaWNhdGlvbiJdLCJwcmluY2lwYWxUeXBlcyI6WyJVc2VyIiwiUm9sZSIsIldvcmtsb2FkIl0sImNvbnRleHQiOnsidHlwZSI6IkNvbnRleHQifX19LCJXcml0ZSI6eyJhcHBsaWVzVG8iOnsicmVzb3VyY2VUeXBlcyI6WyJBcHBsaWNhdGlvbiJdLCJwcmluY2lwYWxUeXBlcyI6WyJVc2VyIiwiUm9sZSIsIldvcmtsb2FkIl0sImNvbnRleHQiOnsidHlwZSI6IkNvbnRleHQifX19fX19"
            }
        }
    },
    "CEDARLING_LOG_TYPE": "memory",
    "CEDARLING_LOG_LEVEL": "DEBUG",
    "CEDARLING_LOG_TTL": 120,
    "CEDARLING_DECISION_LOG_USER_CLAIMS ": ["aud", "sub", "email", "username"],
    "CEDARLING_DECISION_LOG_WORKLOAD_CLAIMS ": ["aud", "client_id", "rp_id"],
    "CEDARLING_USER_AUTHZ": "enabled",
    "CEDARLING_WORKLOAD_AUTHZ": "enabled",
    "CEDARLING_PRINCIPAL_BOOLEAN_OPERATION": {
        "and": [
            {
                "===": [
                    {
                        "var": "Jans::Workload"
                    },
                    "ALLOW"
                ]
            },
            {
                "===": [
                    {
                        "var": "Jans::User"
                    },
                    "ALLOW"
                ]
            }
        ]
    },
    "CEDARLING_LOCAL_JWKS": null,
    "CEDARLING_POLICY_STORE_LOCAL_FN": null,
    "CEDARLING_JWT_SIG_VALIDATION": "disabled",
    "CEDARLING_JWT_STATUS_VALIDATION": "disabled",
    "CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED": [
        "HS256",
        "RS256"
    ],
    "CEDARLING_ID_TOKEN_TRUST_MODE": "strict",
    "CEDARLING_LOCK": "disabled",
    "CEDARLING_LOCK_SERVER_CONFIGURATION_URI": null,
    "CEDARLING_LOCK_DYNAMIC_CONFIGURATION": "disabled",
    "CEDARLING_LOCK_SSA_JWT": "",
    "CEDARLING_LOCK_HEALTH_INTERVAL": 0,
    "CEDARLING_LOCK_TELEMETRY_INTERVAL": 0,
    "CEDARLING_LOCK_LISTEN_SSE": "disabled"
};

// Payload of access_token:
// {
//   "sub": "qzxn1Scrb9lWtGxVedMCky-Ql_ILspZaQA6fyuYktw0",
//   "code": "3e2a2012-099c-464f-890b-448160c2ab25",
//   "iss": "https://account.gluu.org",
//   "token_type": "Bearer",
//   "client_id": "d7f71bea-c38d-4caf-a1ba-e43c74a11a62",
//   "aud": "d7f71bea-c38d-4caf-a1ba-e43c74a11a62",
//   "acr": "simple_password_auth",
//   "x5t#S256": "",
//   "nbf": 1731953030,
//   "scope": [
//     "role",
//     "openid",
//     "profile",
//     "email"
//   ],
//   "auth_time": 1731953027,
//   "exp": 1732121460,
//   "iat": 1731953030,
//   "jti": "uZUh1hDUQo6PFkBPnwpGzg",
//   "username": "Default Admin User",
//   "status": {
//     "status_list": {
//       "idx": 306,
//       "uri": "https://jans.test/jans-auth/restv1/status_list"
//     }
//   }
// }
let ACCESS_TOKEN = "eyJraWQiOiJjb25uZWN0X2Y5YTAwN2EyLTZkMGItNDkyYS05MGNkLWYwYzliMWMyYjVkYl9zaWdfcnMyNTYiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJxenhuMVNjcmI5bFd0R3hWZWRNQ2t5LVFsX0lMc3BaYVFBNmZ5dVlrdHcwIiwiY29kZSI6IjNlMmEyMDEyLTA5OWMtNDY0Zi04OTBiLTQ0ODE2MGMyYWIyNSIsImlzcyI6Imh0dHBzOi8vYWNjb3VudC5nbHV1Lm9yZyIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJjbGllbnRfaWQiOiJkN2Y3MWJlYS1jMzhkLTRjYWYtYTFiYS1lNDNjNzRhMTFhNjIiLCJhdWQiOiJkN2Y3MWJlYS1jMzhkLTRjYWYtYTFiYS1lNDNjNzRhMTFhNjIiLCJhY3IiOiJzaW1wbGVfcGFzc3dvcmRfYXV0aCIsIng1dCNTMjU2IjoiIiwibmJmIjoxNzMxOTUzMDMwLCJzY29wZSI6WyJyb2xlIiwib3BlbmlkIiwicHJvZmlsZSIsImVtYWlsIl0sImF1dGhfdGltZSI6MTczMTk1MzAyNywiZXhwIjoxNzMyMTIxNDYwLCJpYXQiOjE3MzE5NTMwMzAsImp0aSI6InVaVWgxaERVUW82UEZrQlBud3BHemciLCJ1c2VybmFtZSI6IkRlZmF1bHQgQWRtaW4gVXNlciIsInN0YXR1cyI6eyJzdGF0dXNfbGlzdCI6eyJpZHgiOjMwNiwidXJpIjoiaHR0cHM6Ly9qYW5zLnRlc3QvamFucy1hdXRoL3Jlc3R2MS9zdGF0dXNfbGlzdCJ9fX0.Pt-Y7F-hfde_WP7ZYwyvvSS11rKYQWGZXTzjH_aJKC5VPxzOjAXqI3Igr6gJLsP1aOd9WJvOPchflZYArctopXMWClbX_TxpmADqyCMsz78r4P450TaMKj-WKEa9cL5KtgnFa0fmhZ1ZWolkDTQ_M00Xr4EIvv4zf-92Wu5fOrdjmsIGFot0jt-12WxQlJFfs5qVZ9P-cDjxvQSrO1wbyKfHQ_txkl1GDATXsw5SIpC5wct92vjAVm5CJNuv_PE8dHAY-KfPTxOuDYBuWI5uA2Yjd1WUFyicbJgcmYzUSVt03xZ0kQX9dxKExwU2YnpDorfwebaAPO7G114Bkw208g";

// Payload of id_token:
// {
//   "sub": "qzxn1Scrb9lWtGxVedMCky-Ql_ILspZaQA6fyuYktw0",
//   "code": "3e2a2012-099c-464f-890b-448160c2ab25",
//   "iss": "https://account.gluu.org",
//   "token_type": "Bearer",
//   "client_id": "d7f71bea-c38d-4caf-a1ba-e43c74a11a62",
//   "aud": "d7f71bea-c38d-4caf-a1ba-e43c74a11a62",
//   "acr": "simple_password_auth",
//   "x5t#S256": "",
//   "nbf": 1731953030,
//   "scope": [
//     "role",
//     "openid",
//     "profile",
//     "email"
//   ],
//   "auth_time": 1731953027,
//   "exp": 1732121460,
//   "iat": 1731953030,
//   "jti": "uZUh1hDUQo6PFkBPnwpGzg",
//   "username": "Default Admin User",
//   "status": {
//     "status_list": {
//       "idx": 306,
//       "uri": "https://jans.test/jans-auth/restv1/status_list"
//     }
//   }
// }
let ID_TOKEN = "eyJraWQiOiJjb25uZWN0X2Y5YTAwN2EyLTZkMGItNDkyYS05MGNkLWYwYzliMWMyYjVkYl9zaWdfcnMyNTYiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJhdF9oYXNoIjoiYnhhQ1QwWlFYYnY0c2J6alNEck5pQSIsInN1YiI6InF6eG4xU2NyYjlsV3RHeFZlZE1Da3ktUWxfSUxzcFphUUE2Znl1WWt0dzAiLCJhbXIiOltdLCJpc3MiOiJodHRwczovL2FjY291bnQuZ2x1dS5vcmciLCJub25jZSI6IjI1YjJiMTZiLTMyYTItNDJkNi04YThlLWU1ZmE5YWI4ODhjMCIsInNpZCI6IjZkNDQzNzM0LWI3YTItNGVkOC05ZDNhLTE2MDZkMmY5OTI0NCIsImphbnNPcGVuSURDb25uZWN0VmVyc2lvbiI6Im9wZW5pZGNvbm5lY3QtMS4wIiwiYXVkIjoiZDdmNzFiZWEtYzM4ZC00Y2FmLWExYmEtZTQzYzc0YTExYTYyIiwiYWNyIjoic2ltcGxlX3Bhc3N3b3JkX2F1dGgiLCJjX2hhc2giOiJWOGg0c085Tnp1TEthd1BPLTNETkxBIiwibmJmIjoxNzMxOTUzMDMwLCJhdXRoX3RpbWUiOjE3MzE5NTMwMjcsImV4cCI6MTczMTk1NjYzMCwiZ3JhbnQiOiJhdXRob3JpemF0aW9uX2NvZGUiLCJpYXQiOjE3MzE5NTMwMzAsImp0aSI6ImlqTFpPMW9vUnlXcmdJbjdjSWROeUEiLCJzdGF0dXMiOnsic3RhdHVzX2xpc3QiOnsiaWR4IjozMDcsInVyaSI6Imh0dHBzOi8vamFucy50ZXN0L2phbnMtYXV0aC9yZXN0djEvc3RhdHVzX2xpc3QifX19.Nw7MRaJ5LtDak_LdEjrICgVOxDwd1p1I8WxD7IYw0_mKlIJ-J_78rGPski9p3L5ZNCpXiHtVbnhc4lJdmbh-y6mrD3_EY_AmjK50xpuf6YuUuNVtFENCSkj_irPLkIDG65HeZherWsvH0hUn4FVGv8Sw9fjny9Doi-HGHnKg9Qvphqre1U8hCphCVLQlzXAXmBkbPOC8tDwId5yigBKXP50cdqDcT-bjXf9leIdGgq0jxb57kYaFSElprLN9nUygM4RNCn9mtmo1l4IsdTlvvUb3OMAMQkRLfMkiKBjjeSF3819mYRLb3AUBaFH16ZdHFBzTSB6oA22TYpUqOLihMg";

// Payload of userinfo_token:
// {
//   "sub": "qzxn1Scrb9lWtGxVedMCky-Ql_ILspZaQA6fyuYktw0",
//   "email_verified": true,
//   "role": [
//     "CasaAdmin"
//   ],
//   "iss": "https://account.gluu.org",
//   "given_name": "Admin",
//   "middle_name": "Admin",
//   "inum": "a6a70301-af49-4901-9687-0bcdcf4e34fa",
//   "client_id": "d7f71bea-c38d-4caf-a1ba-e43c74a11a62",
//   "aud": "d7f71bea-c38d-4caf-a1ba-e43c74a11a62",
//   "updated_at": 1731698135,
//   "name": "Default Admin User",
//   "nickname": "Admin",
//   "family_name": "User",
//   "jti": "OIn3g1SPSDSKAYDzENVoug",
//   "email": "admin@jans.test",
//   "jansAdminUIRole": [
//     "api-admin"
//   ]
// }
let USERINFO_TOKEN = "eyJraWQiOiJjb25uZWN0X2Y5YTAwN2EyLTZkMGItNDkyYS05MGNkLWYwYzliMWMyYjVkYl9zaWdfcnMyNTYiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJxenhuMVNjcmI5bFd0R3hWZWRNQ2t5LVFsX0lMc3BaYVFBNmZ5dVlrdHcwIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsInJvbGUiOlsiQ2FzYUFkbWluIl0sImlzcyI6Imh0dHBzOi8vYWNjb3VudC5nbHV1Lm9yZyIsImdpdmVuX25hbWUiOiJBZG1pbiIsIm1pZGRsZV9uYW1lIjoiQWRtaW4iLCJpbnVtIjoiYTZhNzAzMDEtYWY0OS00OTAxLTk2ODctMGJjZGNmNGUzNGZhIiwiY2xpZW50X2lkIjoiZDdmNzFiZWEtYzM4ZC00Y2FmLWExYmEtZTQzYzc0YTExYTYyIiwiYXVkIjoiZDdmNzFiZWEtYzM4ZC00Y2FmLWExYmEtZTQzYzc0YTExYTYyIiwidXBkYXRlZF9hdCI6MTczMTY5ODEzNSwibmFtZSI6IkRlZmF1bHQgQWRtaW4gVXNlciIsIm5pY2tuYW1lIjoiQWRtaW4iLCJmYW1pbHlfbmFtZSI6IlVzZXIiLCJqdGkiOiJPSW4zZzFTUFNEU0tBWUR6RU5Wb3VnIiwiZW1haWwiOiJhZG1pbkBqYW5zLnRlc3QiLCJqYW5zQWRtaW5VSVJvbGUiOlsiYXBpLWFkbWluIl19.CIahQtRpoTkIQx8KttLPIKH7gvGG8OmYCMzz7wch6k792DVYQG1R7q3sS9Ema1rO5Fm_GgjOsR0yTTMKsyhHDLBwkDd3cnMLgsh2AwVFZvxtpafTlUAPfjvMAy9YTtkPcY6rNUhsYLSSOA83kt6pHdIv5nI-G6ybqgg-bLBRpwZDoOV0TulRhmuukdiuugTXHT6Bb-K3ZeYs8CwewztnxoFTSDghSzq7VZIraV8SLTBLx5_xswn9mefamyB2XNN3o6vXuMyf4BEbYSCuJ3pu6YtNgfyWwt9cF8PYe4PVLoXZuJKN-cy4qrtgy43QXPCg96jSQUJqgLb5ZL5_3udm2Q";

let REQUEST = {
    "tokens": {
        "access_token": ACCESS_TOKEN,
        "id_token": ID_TOKEN,
        "userinfo_token": USERINFO_TOKEN,
    },
    "action": 'Jans::Action::"Read"',
    "resource": {
        "type": "Jans::Application",
        "id": "some_id",
        "app_id": "application_id",
        "name": "Some Application",
        "url": {
            "host": "jans.test",
            "path": "/protected-endpoint",
            "protocol": "http"
        }
    },
    "context": {
        "current_time": Math.floor(Date.now() / 1000),
        "device_health": ["Healthy"],
        "fraud_indicators": ["Allowed"],
        "geolocation": ["America"],
        "network": "127.0.0.1",
        "network_type": "Local",
        "operating_system": "Linux",
        "user_agent": "Linux"
    },
};

export { BOOTSTRAP_CONFIG, ACCESS_TOKEN, ID_TOKEN, USERINFO_TOKEN, REQUEST }
