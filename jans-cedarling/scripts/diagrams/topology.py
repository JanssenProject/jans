"""Zero Trust topology: Cedarling enforces policy at every layer of a mobile app stack.

The numbered badges match the numbered list under the diagram in the docs.
"""

DIAGRAM = {
    "output": "docs/assets/cedarling/cedarling-zero-trust-topology.png",
    "size": (1400, 720),
    "groups": [
        {"color": "teal", "label": "MOBILE DEVICE", "x": 300, "y": 40, "w": 380, "h": 370},
        {"color": "violet", "label": "IDENTITY PROVIDER", "x": 900, "y": 40, "w": 460, "h": 370},
        {"color": "blue", "label": "API GATEWAY", "x": 270, "y": 490, "w": 400, "h": 200},
        {"color": "blue", "label": "BACKEND APPLICATION", "x": 730, "y": 490, "w": 400,
         "h": 200},
    ],
    "nodes": {
        "store": {"color": "violet", "box": (40, 100, 200, 90),
                  "title": "App store", "desc": ["Google Play,", "Apple App Store"]},

        "app": {"color": "teal", "box": (320, 90, 340, 130),
                "title": "App", "desc": ["ships with a Software", "Statement Assertion (SSA)"]},
        "device_pdp": {"color": "teal", "box": (320, 290, 250, 90), "emphasis": True,
                       "badge": 2, "title": "Cedarling", "desc": ["gates UI and API calls"]},

        "register": {"color": "violet", "box": (920, 90, 200, 90),
                     "title": "/register", "desc": ["dynamic client", "registration"]},
        "idp_pdp": {"color": "violet", "box": (1140, 90, 200, 90), "emphasis": True,
                    "badge": 1, "title": "Cedarling"},
        "token": {"color": "violet", "box": (920, 290, 200, 90),
                  "title": "/token", "desc": ["issues access", "token"]},

        "gateway_pdp": {"color": "blue", "box": (290, 545, 360, 110), "emphasis": True,
                        "badge": 3, "title": "Cedarling",
                        "desc": ["checks scopes and", "revocation before forwarding"]},
        "backend_pdp": {"color": "blue", "box": (750, 545, 360, 110), "emphasis": True,
                        "badge": 4, "title": "Cedarling",
                        "desc": ["re-checks the request,", "trusts no upstream layer"]},
    },
    "edges": [
        {"points": [(240, 145), (320, 145)], "label": "install", "at": (280, 123),
         "color": "violet"},
        {"points": [(660, 120), (920, 120)], "label": "register with SSA", "at": (790, 120),
         "color": "violet"},
        {"points": [(920, 160), (660, 160)], "label": "client credentials", "at": (790, 160),
         "color": "violet"},
        {"points": [(1140, 135), (1120, 135)], "arrow": False, "color": "violet"},
        {"points": [(920, 335), (790, 335), (790, 200), (660, 200)],
         "label": "get access token", "at": (790, 260), "color": "violet", "both": True},
        {"points": [(445, 220), (445, 290)], "label": "authorize", "at": (445, 255)},
        {"points": [(620, 220), (620, 410), (620, 490)],
         "label": "API call with access token", "at": (620, 450)},
        {"points": [(670, 600), (730, 600)]},
    ],
}
