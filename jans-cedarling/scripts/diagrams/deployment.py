"""Where Cedarling runs: browsers, mobile apps and backends, all reporting to Lock Server."""

DIAGRAM = {
    "output": "docs/assets/cedarling/cedarling-deployment.png",
    "size": (1400, 640),
    "groups": [
        {"color": "teal", "label": "BROWSER", "x": 40, "y": 40, "w": 340, "h": 310},
        {"color": "blue", "label": "MOBILE DEVICE", "x": 420, "y": 40, "w": 340, "h": 310},
        {"color": "amber", "label": "BACKEND", "x": 800, "y": 40, "w": 560, "h": 380},
    ],
    "nodes": {
        "web": {"color": "teal", "box": (60, 90, 300, 90),
                "title": "Web application", "desc": ["JavaScript in the browser"]},
        "wasm": {"color": "teal", "box": (60, 230, 300, 90), "emphasis": True,
                 "title": "Cedarling WASM", "desc": ["npm package"]},

        "mobile": {"color": "blue", "box": (440, 90, 300, 90),
                   "title": "Mobile app", "desc": ["iOS or Android"]},
        "sdk": {"color": "blue", "box": (440, 230, 300, 90), "emphasis": True,
                "title": "Cedarling SDK", "desc": ["Swift or Kotlin"]},

        "service": {"color": "amber", "box": (820, 90, 250, 90),
                    "title": "Backend service", "desc": ["embeds Cedarling"]},
        "bindings": {"color": "amber", "box": (820, 230, 250, 90), "emphasis": True,
                     "title": "Cedarling SDK", "desc": ["Rust, Python, Java,", "Go, C"]},
        "any": {"color": "amber", "box": (1090, 90, 250, 90),
                "title": "Any service", "desc": ["any language"]},
        "sidecar": {"color": "amber", "box": (1090, 230, 250, 90), "emphasis": True,
                    "title": "Cedarling Sidecar", "desc": ["AuthZEN API"]},
        "gateways": {"color": "amber", "box": (820, 340, 520, 60), "emphasis": True,
                     "title": "Gateway plugins: KrakenD, OPA"},

        "lock": {"color": "violet", "box": (500, 480, 400, 120),
                 "title": "Lock Server", "role": "Evidence Collector",
                 "desc": ["policy store, logs, telemetry, evidence"]},
    },
    "edges": [
        {"points": [(210, 180), (210, 230)], "label": "authorize", "at": (210, 205)},
        {"points": [(590, 180), (590, 230)], "label": "authorize", "at": (590, 205)},
        {"points": [(945, 180), (945, 230)], "label": "authorize", "at": (945, 205)},
        {"points": [(1215, 180), (1215, 230)], "label": "REST", "at": (1215, 205)},
        {"points": [(210, 350), (210, 540), (500, 540)],
         "color": "violet", "style": "dashed", "both": True},
        {"points": [(590, 350), (590, 480)],
         "color": "violet", "style": "dashed", "both": True},
        {"points": [(1080, 420), (1080, 540), (900, 540)],
         "color": "violet", "style": "dashed", "both": True},
    ],
}
