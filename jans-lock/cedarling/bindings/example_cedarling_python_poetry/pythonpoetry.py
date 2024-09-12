from cedarling_python import init, BootstrapConfig, TokenMapper, PolicyStore, Request, Resource


def example():
    # for example we store it in the variable,
    # but local policy store better to store in the file
    LOCAL_POLICY_STORE = '''
    {
    "policies": {
        "b34fce229be0629e1e17baca42fbfe3621b70540598c": "cGVybWl0ICgKICBwcmluY2lwYWwgaW4gSmFuczo6Um9sZTo6IkFkbWluIiwKICBhY3Rpb24gPT0gSmFuczo6QWN0aW9uOjoiRXhlY3V0ZSIsCiAgcmVzb3VyY2UKKTs="
    },
    "trustedIssuers": { },
    "schema": "bmFtZXNwYWNlIEphbnMgewogICAgLy8gKioqKioqICBUWVBFUyAgKioqKioqCiAgICB0eXBlIFVybCA9IHsKICAgICAgICBwcm90b2NvbDogU3RyaW5nLAogICAgICAgIGhvc3Q6IFN0cmluZywKICAgICAgICBwYXRoOiBTdHJpbmcsCiAgICB9OwogICAgdHlwZSBlbWFpbF9hZGRyZXNzID0gewogICAgICAgIGlkOiBTdHJpbmcsIAogICAgICAgIGRvbWFpbjogU3RyaW5nLAogICAgfTsKICAgIHR5cGUgQ29udGV4dCA9IHsKICAgICAgICAgICAgbmV0d29yazogaXBhZGRyLAogICAgICAgICAgICBuZXR3b3JrX3R5cGU6IFN0cmluZywKICAgICAgICAgICAgdXNlcl9hZ2VudDogU3RyaW5nLCAKICAgICAgICAgICAgb3BlcmF0aW5nX3N5c3RlbTogU3RyaW5nLAogICAgICAgICAgICBkZXZpY2VfaGVhbHRoOiBTZXQ8U3RyaW5nPiwKICAgICAgICAgICAgY3VycmVudF90aW1lOiBMb25nLAogICAgICAgICAgICBnZW9sb2NhdGlvbjogU2V0PFN0cmluZz4sCiAgICAgICAgICAgIGZyYXVkX2luZGljYXRvcnM6IFNldDxTdHJpbmc+LAogICAgfTsKICAgICB0eXBlIEVtcHR5Q29udGV4dCA9IHt9OwoKICAgIC8vICoqKioqKiAgRW50aXRpZXMgICoqKioqKgogICAgZW50aXR5IFRydXN0ZWRJc3N1ZXIgPSB7CiAgICAgICAgaXNzdWVyX2VudGl0eV9pZDogVXJsLAogICAgfTsKICAgIGVudGl0eSBDbGllbnQgID0gewogICAgICAgIGNsaWVudF9pZDogU3RyaW5nLAogICAgICAgIGlzczogVHJ1c3RlZElzc3VlciwKICAgIH07CiAgICBlbnRpdHkgQXBwbGljYXRpb24gPSB7CiAgICAgICAgbmFtZTogU3RyaW5nLAogICAgICAgIGNsaWVudDogQ2xpZW50LAogICAgfTsKICAgIGVudGl0eSBSb2xlOwogICAgZW50aXR5IFVzZXIgaW4gW1JvbGVdIHsKICAgICAgICBzdWI6IFN0cmluZywKICAgICAgICB1c2VybmFtZTogU3RyaW5nLAogICAgICAgIGVtYWlsOiBlbWFpbF9hZGRyZXNzLAogICAgICAgIHBob25lX251bWJlcjogU3RyaW5nLAogICAgICAgIHJvbGU6IFNldDxTdHJpbmc+LAogICAgfTsKCiAgICBlbnRpdHkgQWNjZXNzX3Rva2VuICA9IHsKICAgICAgICBhdWQ6IFN0cmluZywKICAgICAgICBleHA6IExvbmcsCiAgICAgICAgaWF0OiBMb25nLAogICAgICAgIGlzczogVHJ1c3RlZElzc3VlciwKICAgICAgICBqdGk/OiBTdHJpbmcsCiAgICAgICAgc2NvcGU6IFNldDxTdHJpbmc+LAogICAgfTsKICAgIGVudGl0eSBpZF90b2tlbiAgPSB7CiAgICAgICAgYWNyOiBTdHJpbmcsCiAgICAgICAgYW1yOiBTZXQ8U3RyaW5nPiwKICAgICAgICBhdWQ6IFN0cmluZywKICAgICAgICBiaXJ0aGRhdGU6IFN0cmluZywKICAgICAgICBlbWFpbDogZW1haWxfYWRkcmVzcywKICAgICAgICBleHA6IExvbmcsCiAgICAgICAgaWF0OiBMb25nLAogICAgICAgIGlzczogVHJ1c3RlZElzc3VlciwKICAgICAgICBqdGk/OiBTdHJpbmcsICAgICAgICAKICAgICAgICBuYW1lOiBTdHJpbmcsCiAgICAgICAgcGhvbmVfbnVtYmVyOiBTdHJpbmcsCiAgICAgICAgc3ViOiBTdHJpbmcsCiAgICB9OwogICAgZW50aXR5IFVzZXJpbmZvX3Rva2VuICA9IHsKICAgICAgICBhdWQ6IFN0cmluZywKICAgICAgICBiaXJ0aGRhdGU6IFN0cmluZywKICAgICAgICBlbWFpbDogZW1haWxfYWRkcmVzcywKICAgICAgICBpc3M6IFRydXN0ZWRJc3N1ZXIsCiAgICAgICAganRpPzogU3RyaW5nLAogICAgICAgIG5hbWU6IFN0cmluZywKICAgICAgICBwaG9uZV9udW1iZXI6IFN0cmluZywKICAgICAgICBzdWI6IFN0cmluZywKICAgIH07CgogICAgLy8gKioqKioqICBBY3Rpb25zICAqKioqKioKICAgIGFjdGlvbiBFeGVjdXRlIGFwcGxpZXNUbyB7CiAgICAgICAgcHJpbmNpcGFsOiBbVXNlciwgUm9sZV0sCiAgICAgICAgcmVzb3VyY2U6IEFwcGxpY2F0aW9uLAogICAgICAgIGNvbnRleHQ6IEVtcHR5Q29udGV4dCwKICAgIH07Cn0K"
    }
    '''

    # example with raw json
    store = PolicyStore.from_raw_json(LOCAL_POLICY_STORE)

    # example with reading from file
    # store = PolicyStore.from_filepath("../demo/policy-store/local.json")

    # example with loading from remote uri
    # store = PolicyStore.from_remote_uri(
    #     "https://raw.githubusercontent.com/JanssenProject/jans/main/jans-lock/cedarling/demo/policy-store/local.json")

    # none means default mapping
    # in this example, we extract the "role" claim from the userinfo token
    mapper = TokenMapper(
        id_token=None, userinfo_token="role", access_token=None)
    config = BootstrapConfig(application_name="DemoApp",
                             token_mapper=mapper, policy_store=store)

    # also fields support setters and getter
    config.policy_store = store

    authz = init(config)

    # Create a new Request instance
    req = Request()

    req.access_token = input("insert assess_token:")
    req.id_token = input("insert id_token:")
    req.userinfo_token = input("insert userinfo_token:")
    req.action = input("insert action:")
    req.resource = Resource(_type=input("insert resource type:"),
                            id=input("insert resource id:"))
    req.context = {}

    result = authz.is_authorized(req)

    print(f"authorization result: {result}")


if __name__ == '__main__':
    print("start example of application")
    try:
        example()
    except KeyboardInterrupt:
        print("\nexiting...")
