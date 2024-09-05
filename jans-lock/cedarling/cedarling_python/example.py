from cedarling_python import Authz, BootstrapConfig, TokenMapper, PolicyStore 

# for example we store it in the variable, 
# but local policy store better to store in the file
LOCAL_POLICY_STORE='''
{
  "policies": {
    "b34fce229be0629e1e17baca42fbfe3621b70540598c": "cGVybWl0ICgKICBwcmluY2lwYWwgaW4gSmFuczo6Um9sZTo6IkFkbWluIiwKICBhY3Rpb24gPT0gSmFuczo6QWN0aW9uOjoiRXhlY3V0ZSIsCiAgcmVzb3VyY2UKKTs="
  },
  "trustedIssuers": { },
  "schema": "bmFtZXNwYWNlIEphbnMgewogICAgLy8gKioqKioqICBUWVBFUyAgKioqKioqCiAgICB0eXBlIFVybCA9IHsKICAgICAgICBwcm90b2NvbDogU3RyaW5nLAogICAgICAgIGhvc3Q6IFN0cmluZywKICAgICAgICBwYXRoOiBTdHJpbmcsCiAgICB9OwogICAgdHlwZSBlbWFpbF9hZGRyZXNzID0gewogICAgICAgIGlkOiBTdHJpbmcsIAogICAgICAgIGRvbWFpbjogU3RyaW5nLAogICAgfTsKICAgIHR5cGUgQ29udGV4dCA9IHsKICAgICAgICAgICAgbmV0d29yazogaXBhZGRyLAogICAgICAgICAgICBuZXR3b3JrX3R5cGU6IFN0cmluZywKICAgICAgICAgICAgdXNlcl9hZ2VudDogU3RyaW5nLCAKICAgICAgICAgICAgb3BlcmF0aW5nX3N5c3RlbTogU3RyaW5nLAogICAgICAgICAgICBkZXZpY2VfaGVhbHRoOiBTZXQ8U3RyaW5nPiwKICAgICAgICAgICAgY3VycmVudF90aW1lOiBMb25nLAogICAgICAgICAgICBnZW9sb2NhdGlvbjogU2V0PFN0cmluZz4sCiAgICAgICAgICAgIGZyYXVkX2luZGljYXRvcnM6IFNldDxTdHJpbmc+LAogICAgfTsKICAgICB0eXBlIEVtcHR5Q29udGV4dCA9IHt9OwoKICAgIC8vICoqKioqKiAgRW50aXRpZXMgICoqKioqKgogICAgZW50aXR5IFRydXN0ZWRJc3N1ZXIgPSB7CiAgICAgICAgaXNzdWVyX2VudGl0eV9pZDogVXJsLAogICAgfTsKICAgIGVudGl0eSBDbGllbnQgID0gewogICAgICAgIGNsaWVudF9pZDogU3RyaW5nLAogICAgICAgIGlzczogVHJ1c3RlZElzc3VlciwKICAgIH07CiAgICBlbnRpdHkgQXBwbGljYXRpb24gPSB7CiAgICAgICAgbmFtZTogU3RyaW5nLAogICAgICAgIGNsaWVudDogQ2xpZW50LAogICAgfTsKICAgIGVudGl0eSBSb2xlOwogICAgZW50aXR5IFVzZXIgaW4gW1JvbGVdIHsKICAgICAgICBzdWI6IFN0cmluZywKICAgICAgICB1c2VybmFtZTogU3RyaW5nLAogICAgICAgIGVtYWlsOiBlbWFpbF9hZGRyZXNzLAogICAgICAgIHBob25lX251bWJlcjogU3RyaW5nLAogICAgICAgIHJvbGU6IFNldDxTdHJpbmc+LAogICAgfTsKCiAgICBlbnRpdHkgQWNjZXNzX3Rva2VuICA9IHsKICAgICAgICBhdWQ6IFN0cmluZywKICAgICAgICBleHA6IExvbmcsCiAgICAgICAgaWF0OiBMb25nLAogICAgICAgIGlzczogVHJ1c3RlZElzc3VlciwKICAgICAgICBqdGk/OiBTdHJpbmcsCiAgICAgICAgc2NvcGU6IFNldDxTdHJpbmc+LAogICAgfTsKICAgIGVudGl0eSBpZF90b2tlbiAgPSB7CiAgICAgICAgYWNyOiBTdHJpbmcsCiAgICAgICAgYW1yOiBTZXQ8U3RyaW5nPiwKICAgICAgICBhdWQ6IFN0cmluZywKICAgICAgICBiaXJ0aGRhdGU6IFN0cmluZywKICAgICAgICBlbWFpbDogZW1haWxfYWRkcmVzcywKICAgICAgICBleHA6IExvbmcsCiAgICAgICAgaWF0OiBMb25nLAogICAgICAgIGlzczogVHJ1c3RlZElzc3VlciwKICAgICAgICBqdGk/OiBTdHJpbmcsICAgICAgICAKICAgICAgICBuYW1lOiBTdHJpbmcsCiAgICAgICAgcGhvbmVfbnVtYmVyOiBTdHJpbmcsCiAgICAgICAgc3ViOiBTdHJpbmcsCiAgICB9OwogICAgZW50aXR5IFVzZXJpbmZvX3Rva2VuICA9IHsKICAgICAgICBhdWQ6IFN0cmluZywKICAgICAgICBiaXJ0aGRhdGU6IFN0cmluZywKICAgICAgICBlbWFpbDogZW1haWxfYWRkcmVzcywKICAgICAgICBpc3M6IFRydXN0ZWRJc3N1ZXIsCiAgICAgICAganRpPzogU3RyaW5nLAogICAgICAgIG5hbWU6IFN0cmluZywKICAgICAgICBwaG9uZV9udW1iZXI6IFN0cmluZywKICAgICAgICBzdWI6IFN0cmluZywKICAgIH07CgogICAgLy8gKioqKioqICBBY3Rpb25zICAqKioqKioKICAgIGFjdGlvbiBFeGVjdXRlIGFwcGxpZXNUbyB7CiAgICAgICAgcHJpbmNpcGFsOiBbVXNlciwgUm9sZV0sCiAgICAgICAgcmVzb3VyY2U6IEFwcGxpY2F0aW9uLAogICAgICAgIGNvbnRleHQ6IEVtcHR5Q29udGV4dCwKICAgIH07Cn0K"
}
'''

store = PolicyStore.from_raw_json(LOCAL_POLICY_STORE)

# none means default mapping
mapper=TokenMapper(id_token=None, userinfo_token=None, access_token=None)
config = BootstrapConfig(application_name="DemoApp",token_mapper=mapper,policy_store=store)


authz = Authz(config)

result = authz.is_authorized({
    "access_token": "eyJraWQiOiJjb25uZWN0XzdjZTcxNDA5LTkwMjQtNDc1OC1hN2NmLWU4OTJkYWM1YjkzMV9zaWdfcnMyNTYiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJib0c4ZGZjNU1LVG4zN283Z3NkQ2V5cUw4THBXUXRnb080MW0xS1p3ZHEwIiwiY29kZSI6IjE1NTUyNGM2LTdmMjYtNDM5Mi1iNzAxLWUwMDYyYjQ4MjY2OSIsImlzcyI6Imh0dHBzOi8vYWRtaW4tdWktdGVzdC5nbHV1Lm9yZyIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJjbGllbnRfaWQiOiIzM2Q4YzAyMC01YzkxLTRmYTYtODA0MS00ODRlYWFlMzk5MjYiLCJhdWQiOiIzM2Q4YzAyMC01YzkxLTRmYTYtODA0MS00ODRlYWFlMzk5MjYiLCJhY3IiOiJiYXNpYyIsIng1dCNTMjU2IjoiIiwic2NvcGUiOlsib3BlbmlkIiwicHJvZmlsZSJdLCJhdXRoX3RpbWUiOjE3MjUwMTg5MzEsImV4cCI6MTcyNTE4Njk4MiwiaWF0IjoxNzI1MDE4OTMyLCJqdGkiOiJCdkkzUGliSlM2S2VsQWFsMnp0b29BIiwidXNlcm5hbWUiOiJEZWZhdWx0IEFkbWluIFVzZXIiLCJzdGF0dXMiOnsic3RhdHVzX2xpc3QiOnsiaWR4IjoyMDAzLCJ1cmkiOiJodHRwczovL2FkbWluLXVpLXRlc3QuZ2x1dS5vcmcvamFucy1hdXRoL3Jlc3R2MS9zdGF0dXNfbGlzdCJ9fX0.3e3XPxE_ox2auDgBpYNCAPg3k8TFZYWEjV3o5yUtiCUB5Dlu1NBoY0nKO8j2LYnIckI8XHk-imUs4Wk6zGQXypT1LhpZkYGIvX9ZqTA-B7tSD7SgBs-BN0y4ZGhyMC6tmzUL-7DAYoqPmqI-HBwvN8fuDYZgTV9UtnFhVoa6Ti9Q28E187V1E2dUpEq57Re8c_90FYLl10ypqeyR8rqEqYVpDAJIMJnpW7IMzsWuEfMEXnGBN2zlda6Y_Go3sPr9MCI-EKJt1Zw8ukAspEgA5dsJiTrLgxOP_oinctz1avczVqr8dlSEF6ujZE1sMA3yAn_LebokEZAZ9U3bKd_9aw",
    "id_token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6ImJjNWQxZmM4MjM5MDdlYzUxMDNhM2YxN2MyMjNmZWY4In0.eyJhdF9oYXNoIjoiYjVDS21YdVBVdElRMW9VZFN4YkdQUSIsInN1YiI6ImJvRzhkZmM1TUtUbjM3bzdnc2RDZXlxTDhMcFdRdGdvTzQxbTFLWndkcTAiLCJjb3VudHJ5IjoiSFUiLCJiaXJ0aGRhdGUiOiIyMDAwLTAxLTAxIiwidXNlcl9uYW1lIjoiYWRtaW4iLCJhbXIiOlsiMTAiXSwiaXNzIjoiaHR0cHM6Ly9hZG1pbi11aS10ZXN0LmdsdXUub3JnIiwiaW51bSI6IjhkMWNkZTZhLTE0NDctNDc2Ni1iM2M4LTE2NjYzZTEzYjQ1OCIsInNpZCI6ImNiMDNkZWU3LWIyYTktNGVhZC04Mjg3LWU3OGFhNWFiYjIyNSIsImphbnNPcGVuSURDb25uZWN0VmVyc2lvbiI6Im9wZW5pZGNvbm5lY3QtMS4wIiwiYWNyIjoiYmFzaWMiLCJ1cGRhdGVkX2F0IjoxNzI1MDE4OTAyLCJhdXRoX3RpbWUiOjE3MjUwMTg5MzEsIm5pY2tuYW1lIjoiQWRtaW4iLCJleHAiOjE3MjUwMjI1MzIsImlhdCI6MTcyNTAxODkzMiwianRpIjoia01HZGhVbDFSYXFqOVlsaTRXSzlYQSIsImVtYWlsIjoiYWRtaW5AYWRtaW4tdWktdGVzdC5nbHV1Lm9yZyIsImdpdmVuX25hbWUiOiJBZG1pbiIsIm1pZGRsZV9uYW1lIjoiQWRtaW4iLCJub25jZSI6IjQwZmU3ZGQ0LWM5YmYtNGFlOS1iMWFkLTk2MDQwYjRhZGUxMCIsImF1ZCI6IjMzZDhjMDIwLTVjOTEtNGZhNi04MDQxLTQ4NGVhYWUzOTkyNiIsImNfaGFzaCI6IlIxeDlYWk0zQ0FQU09DOC1XRjBGeEEiLCJuYW1lIjoiRGVmYXVsdCBBZG1pbiBVc2VyIiwidXNlcl9wZXJtaXNzaW9uIjpbIkNhc2FBZG1pbiJdLCJwaG9uZV9udW1iZXIiOiIrOTE3ODM3Njc5MzQwIiwiZ3JhbnQiOiJhdXRob3JpemF0aW9uX2NvZGUiLCJmYW1pbHlfbmFtZSI6IlVzZXIiLCJzdGF0dXMiOnsic3RhdHVzX2xpc3QiOnsiaWR4IjoyMDA0LCJ1cmkiOiJodHRwczovL2FkbWluLXVpLXRlc3QuZ2x1dS5vcmcvamFucy1hdXRoL3Jlc3R2MS9zdGF0dXNfbGlzdCJ9fSwiamFuc0FkbWluVUlSb2xlIjpbImFwaS1hZG1pbiJdLCJyb2xlIjpbImlkX3Rva2VuX3JvbGUiLCJBZG1pbiJdfQ.xaskfCr9iU00wombMPD7rR9yT3jSlLtjdBMtNgX01KcbFP2VoQGLukGkf4ket-rAVFHfurxNYVIU31vXUJbWMQ",
    "userinfo_token": "eyJraWQiOiJjb25uZWN0XzdjZTcxNDA5LTkwMjQtNDc1OC1hN2NmLWU4OTJkYWM1YjkzMV9zaWdfcnMyNTYiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJjb3VudHJ5IjoiSFUiLCJzdWIiOiJib0c4ZGZjNU1LVG4zN283Z3NkQ2V5cUw4THBXUXRnb080MW0xS1p3ZHEwIiwiYmlydGhkYXRlIjoiMjAwMC0wMS0wMSIsInVzZXJfbmFtZSI6ImFkbWluIiwiaXNzIjoiaHR0cHM6Ly9hZG1pbi11aS10ZXN0LmdsdXUub3JnIiwiZ2l2ZW5fbmFtZSI6IkFkbWluIiwibWlkZGxlX25hbWUiOiJBZG1pbiIsImludW0iOiI4ZDFjZGU2YS0xNDQ3LTQ3NjYtYjNjOC0xNjY2M2UxM2I0NTgiLCJjbGllbnRfaWQiOiIzM2Q4YzAyMC01YzkxLTRmYTYtODA0MS00ODRlYWFlMzk5MjYiLCJhdWQiOiIzM2Q4YzAyMC01YzkxLTRmYTYtODA0MS00ODRlYWFlMzk5MjYiLCJ1cGRhdGVkX2F0IjoxNzI1MDE4OTAyLCJuYW1lIjoiRGVmYXVsdCBBZG1pbiBVc2VyIiwibmlja25hbWUiOiJBZG1pbiIsInVzZXJfcGVybWlzc2lvbiI6WyJDYXNhQWRtaW4iXSwicGhvbmVfbnVtYmVyIjoiKzkxNzgzNzY3OTM0MCIsImZhbWlseV9uYW1lIjoiVXNlciIsImp0aSI6IlNVZ2VpMUNLUWppV0N6WnlmZWNiRFEiLCJlbWFpbCI6ImFkbWluQGFkbWluLXVpLXRlc3QuZ2x1dS5vcmciLCJqYW5zQWRtaW5VSVJvbGUiOlsiYXBpLWFkbWluIl19.XWBySoPzgfkoErjWuTWeqDTTAvpXv0OQfyZKyv7wu4Zg7dzJ4Ct9eBibGKeZwELHH2_UOlqagovT0b5KyFuGc8c0wKWPzhFyMEJogwTX2FKYUVgJ95_sNa0dRDDqklxKcyhPVhDAtYEZr3FgeAOq5P17W9K9RXOhS4eSK4XyxafC7LPnl9ZRSuqzB0s3etxfSgatXXsDmBCJ2U_JTaVoB6mmxLFlvboJ0YhcSBGPXbU7A-98840rk3VPoy91tq3jRCFdZyiSNPZWJygYNyXAqSE5Om8lmf6lUoE5Rg8r7rNNQe7-Vaitu_JANzKw4Gy3FTagQQ_1hu2-TIRORAFFzg",
    # "tx_token": "eyJjbGc...",
    "action": "Jans::Action::\"Execute\"",
    "resource": {
        "type": "Jans::Application",
        "id": "Support Portal" # ID is illustrative in our example
    },
    "context": {}
})

print(f"authorization result: {result}")

