"""Render the Cedarling components diagram used on the docs overview page.

Automatic layout engines (Mermaid, Graphviz) spread this graph over a wide
canvas with long crossing edges, which becomes unreadable at docs page width.
The layout is therefore fixed by hand below: nodes and edges are plain data,
and the script turns them into an SVG and screenshots it into a PNG with a
headless Chrome or Chromium.

Usage (from the repository root):
    python3 jans-cedarling/scripts/render_components_diagram.py

Set CHROME to the browser binary if it is not on PATH.
"""

import argparse
import os
import shutil
import subprocess
import sys
import tempfile
from html import escape

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
DEFAULT_OUTPUT = os.path.join(REPO_ROOT, "docs/assets/cedarling/cedarling-components.png")

CHROME_TIMEOUT_SECONDS = 60

WIDTH = 1400
HEIGHT = 790
FONT = "'Ubuntu Sans', Inter, 'Segoe UI', 'DejaVu Sans', sans-serif"

BACKGROUND = "#071526"
DESC_COLOR = "#c3d0e0"
GROUPS = {
    "interfaces": {"accent": "#2dd4bf", "fill": "#0c2a33", "node": "#0e3540"},
    "core": {"accent": "#60a5fa", "fill": "#0a1d36", "node": "#0f2a4a"},
    "external": {"accent": "#a78bfa", "fill": "#16163a", "node": "#221f4a"},
}
EDGE_COLOR = "#7dd3c0"
OPTIONAL_COLOR = "#a78bfa"

GROUP_BOXES = [
    {"group": "interfaces", "label": "INTERFACES", "x": 40, "y": 90, "w": 270, "h": 640},
    {"group": "core", "label": "CEDARLING", "x": 350, "y": 90, "w": 690, "h": 640},
    {"group": "external", "label": "EXTERNAL SERVICES", "x": 1080, "y": 90, "w": 270, "h": 640},
]

# x, y, w, h are in SVG units; text is centered inside the box.
NODES = {
    "init": {"group": "interfaces", "box": (60, 140, 230, 76),
             "title": "Init", "desc": ["bootstrap config"]},
    "authz": {"group": "interfaces", "box": (60, 280, 230, 140),
              "title": "Authorize",
              "desc": ["multi-issuer tokens or", "asserted principal", "single or batch"]},
    "data": {"group": "interfaces", "box": (60, 470, 230, 96),
             "title": "Data API", "desc": ["push, get, remove", "context data"]},
    "observe": {"group": "interfaces", "box": (60, 620, 230, 96),
                "title": "Inspect & Observe", "desc": ["logs, metrics,", "policy annotations"]},

    "psm": {"group": "core", "box": (380, 140, 590, 76),
            "title": "Policy Store Manager", "desc": ["load, verify signature, refresh"]},
    "jwt": {"group": "core", "box": (720, 300, 250, 76),
            "title": "JWT Service", "desc": ["signature, claims, status list"]},
    "cds": {"group": "core", "box": (380, 470, 230, 96),
            "title": "Context Data Store", "desc": ["pushed data with TTL"]},
    "builder": {"group": "core", "box": (720, 470, 250, 96),
                "title": "Entity & Context Builder",
                "desc": ["principals, resource,", "context.tokens, context.data"]},
    "logs": {"group": "core", "box": (380, 620, 230, 76),
             "title": "Observability", "desc": ["logs, telemetry, evidence"]},
    "cedar": {"group": "core", "box": (720, 620, 250, 76), "emphasis": True,
              "title": "Cedar Engine", "desc": ["Allow / Deny, diagnostics"]},

    "source": {"group": "external", "box": (1100, 140, 230, 76),
               "title": "Policy store source", "desc": ["local file or URI"]},
    "issuers": {"group": "external", "box": (1100, 290, 230, 96),
                "title": "Trusted issuers", "desc": ["OpenID config, JWKS,", "status list"]},
    "lock": {"group": "external", "box": (1100, 602, 230, 112),
             "title": "Lock Server", "role": "Evidence Collector",
             "desc": ["policy store, logs,", "health, telemetry"]},
}

# Each edge is a polyline; the last point carries the arrowhead. Edges listed
# later are drawn on top, with a background halo that cuts a gap into any
# line they cross.
EDGES = [
    {"points": [(290, 178), (380, 178)]},
    {"points": [(1100, 170), (970, 170)]},
    {"points": [(845, 216), (845, 300)], "label": "trusted issuers", "at": (845, 258)},
    {"points": [(970, 205), (1005, 205), (1005, 658), (970, 658)],
     "label": "policies", "at": (1005, 610)},
    {"points": [(1005, 518), (970, 518)], "label": "schema", "at": (1005, 462), "junction": True},
    {"points": [(1100, 338), (970, 338)], "halo": True},
    {"points": [(290, 338), (720, 338)], "label": "tokens", "at": (505, 338)},
    {"points": [(290, 400), (760, 400), (760, 470)],
     "label": "principal, resource, context", "at": (505, 400)},
    {"points": [(930, 376), (930, 470)], "label": "validated tokens", "at": (930, 423)},
    {"points": [(290, 518), (380, 518)]},
    {"points": [(610, 518), (720, 518)], "label": "context.data", "at": (665, 496)},
    {"points": [(845, 566), (845, 620)], "label": "entities, context", "at": (845, 593)},
    {"points": [(720, 658), (610, 658)], "label": "decision", "at": (665, 636)},
    {"points": [(380, 658), (290, 658)]},
    {"points": [(495, 696), (495, 752), (1215, 752), (1215, 714)],
     "label": "logs, telemetry, evidence", "at": (860, 752), "optional": True},
    {"points": [(1330, 658), (1378, 658), (1378, 62), (900, 62), (900, 140)],
     "label": "policy store", "at": (1150, 62), "optional": True},
    {"points": [(560, 140), (560, 50), (22, 50), (22, 668), (60, 668)],
     "label": "policy annotations", "at": (290, 50), "dotted": True},
]

CORNER_RADIUS = 10


def rounded_path(points):
    """Polyline with rounded corners, so elbows read as one continuous line."""
    commands = [f"M {points[0][0]} {points[0][1]}"]
    for prev, corner, nxt in zip(points, points[1:], points[2:]):
        before = shift_towards(corner, prev, CORNER_RADIUS)
        after = shift_towards(corner, nxt, CORNER_RADIUS)
        commands.append(f"L {before[0]} {before[1]}")
        commands.append(f"Q {corner[0]} {corner[1]} {after[0]} {after[1]}")
    commands.append(f"L {points[-1][0]} {points[-1][1]}")
    return " ".join(commands)


def shift_towards(origin, target, distance):
    dx, dy = target[0] - origin[0], target[1] - origin[1]
    length = max(abs(dx), abs(dy))
    if length == 0:
        return origin
    return (origin[0] + dx / length * distance, origin[1] + dy / length * distance)


def render_group(box):
    style = GROUPS[box["group"]]
    return (
        f'<rect x="{box["x"]}" y="{box["y"]}" width="{box["w"]}" height="{box["h"]}" rx="22" '
        f'fill="{style["fill"]}" fill-opacity="0.55" stroke="{style["accent"]}" '
        f'stroke-opacity="0.45" stroke-dasharray="6 6"/>'
        f'<text x="{box["x"] + 22}" y="{box["y"] + 30}" class="group" '
        f'fill="{style["accent"]}">{escape(box["label"])}</text>'
    )


def render_node(node):
    style = GROUPS[node["group"]]
    x, y, w, h = node["box"]
    stroke_width = 2.5 if node.get("emphasis") else 1.6
    fill = "#14365e" if node.get("emphasis") else style["node"]
    parts = [
        f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="14" fill="{fill}" '
        f'stroke="{style["accent"]}" stroke-width="{stroke_width}" filter="url(#shadow)"/>'
    ]

    lines = []
    if "role" in node:
        lines.append((node["role"], "role", style["accent"]))
    lines += [(line, "desc", DESC_COLOR) for line in node["desc"]]

    title_height, line_height = 26, 21
    block = title_height + line_height * len(lines)
    top = y + (h - block) / 2
    cx = x + w / 2
    parts.append(
        f'<text x="{cx}" y="{top + 20}" class="title" fill="#ffffff">{escape(node["title"])}</text>'
    )
    for i, (line, css, color) in enumerate(lines):
        parts.append(
            f'<text x="{cx}" y="{top + title_height + 16 + i * line_height}" class="{css}" '
            f'fill="{color}">{escape(line)}</text>'
        )
    return "".join(parts)


def render_edge(edge):
    color = OPTIONAL_COLOR if edge.get("optional") else EDGE_COLOR
    marker = "arrow-optional" if edge.get("optional") else "arrow"
    if edge.get("dotted"):
        dash = ' stroke-dasharray="2 6" stroke-linecap="round"'
    elif edge.get("optional"):
        dash = ' stroke-dasharray="8 6"'
    else:
        dash = ""
    path = rounded_path(edge["points"])
    parts = []
    if edge.get("halo"):
        parts.append(f'<path d="{path}" fill="none" stroke="{BACKGROUND}" stroke-width="12"/>')
    parts.append(
        f'<path d="{path}" fill="none" stroke="{color}" stroke-width="2"{dash} '
        f'marker-end="url(#{marker})"/>'
    )
    if edge.get("junction"):
        jx, jy = edge["points"][0]
        parts.append(f'<circle cx="{jx}" cy="{jy}" r="4.5" fill="{color}"/>')
    if "label" in edge:
        lx, ly = edge["at"]
        width = 9 + len(edge["label"]) * 7.6
        parts.append(
            f'<rect x="{lx - width / 2}" y="{ly - 12}" width="{width}" height="24" rx="12" '
            f'fill="{BACKGROUND}" stroke="{color}" stroke-opacity="0.35"/>'
            f'<text x="{lx}" y="{ly + 5}" class="edge" fill="{color}">{escape(edge["label"])}</text>'
        )
    return "".join(parts)


def render_svg():
    arrow = (
        '<marker id="{id}" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" '
        'markerHeight="7" orient="auto-start-reverse"><path d="M 0 0 L 10 5 L 0 10 z" '
        'fill="{color}"/></marker>'
    )
    defs = (
        "<defs>"
        + arrow.format(id="arrow", color=EDGE_COLOR)
        + arrow.format(id="arrow-optional", color=OPTIONAL_COLOR)
        + '<filter id="shadow" x="-10%" y="-10%" width="120%" height="140%">'
        '<feDropShadow dx="0" dy="4" stdDeviation="6" flood-color="#000" flood-opacity="0.45"/>'
        "</filter></defs>"
    )
    style = (
        "<style>"
        f"text {{ font-family: {FONT}; text-anchor: middle; }}"
        ".group { font-size: 15px; font-weight: 700; letter-spacing: 0.14em; text-anchor: start; }"
        ".title { font-size: 20px; font-weight: 700; }"
        ".desc { font-size: 15.5px; }"
        ".role { font-size: 17px; font-weight: 700; }"
        ".edge { font-size: 13.5px; font-weight: 600; }"
        "</style>"
    )
    body = [f'<rect width="{WIDTH}" height="{HEIGHT}" fill="{BACKGROUND}"/>']
    body += [render_group(box) for box in GROUP_BOXES]
    body += [render_edge(edge) for edge in EDGES]
    body += [render_node(node) for node in NODES.values()]
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{WIDTH}" height="{HEIGHT}" '
        f'viewBox="0 0 {WIDTH} {HEIGHT}">{defs}{style}{"".join(body)}</svg>'
    )


def find_chrome():
    configured = os.environ.get("CHROME")
    if configured:
        return shutil.which(configured)
    for name in ("google-chrome", "chromium", "chromium-browser", "chrome"):
        path = shutil.which(name)
        if path:
            return path
    return None


def screenshot(svg, output, scale, chrome):
    with tempfile.TemporaryDirectory() as tmp:
        page = os.path.join(tmp, "diagram.html")
        with open(page, "w") as f:
            f.write(f'<!doctype html><html><body style="margin:0">{svg}</body></html>')
        command = [
            chrome, "--headless", "--no-sandbox", "--disable-gpu", "--hide-scrollbars",
            f"--window-size={WIDTH},{HEIGHT}",
            f"--force-device-scale-factor={scale}",
            f"--screenshot={os.path.abspath(output)}",
            f"file://{page}",
        ]
        try:
            result = subprocess.run(
                command, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE,
                text=True, errors="replace", timeout=CHROME_TIMEOUT_SECONDS,
            )
        except subprocess.TimeoutExpired:
            print(f"Chrome did not finish within {CHROME_TIMEOUT_SECONDS}s", file=sys.stderr)
            return 1
        if result.returncode != 0:
            print(result.stderr, file=sys.stderr)
        return result.returncode


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--output", default=DEFAULT_OUTPUT, help="PNG file to write")
    parser.add_argument("--svg", help="also write the SVG source to this path")
    parser.add_argument("--scale", type=int, default=2, help="pixel density multiplier")
    args = parser.parse_args()

    svg = render_svg()
    if args.svg:
        with open(args.svg, "w") as f:
            f.write(svg)

    chrome = find_chrome()
    if chrome is None:
        print("Chrome or Chromium not found; set CHROME to the browser binary", file=sys.stderr)
        return 1
    return screenshot(svg, args.output, args.scale, chrome)


if __name__ == "__main__":
    sys.exit(main())
