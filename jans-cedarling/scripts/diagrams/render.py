"""Render the Cedarling diagrams used in the docs.

Automatic layout engines (Mermaid, Graphviz) spread these graphs over a wide
canvas with long crossing edges, which becomes unreadable at docs page width.
The layout is therefore fixed by hand: every diagram is a module next to this
script that holds its nodes and edges as plain data, and this script turns it
into an SVG and screenshots it into a PNG with a headless Chrome or Chromium.

Usage (from the repository root):
    python3 jans-cedarling/scripts/diagrams/render.py              # all diagrams
    python3 jans-cedarling/scripts/diagrams/render.py components   # one diagram

Set CHROME to the browser binary if it is not on PATH. Install pngquant to
compress the PNGs; without it they are written uncompressed.

A diagram module defines DIAGRAM with these keys:
    output  PNG path relative to the repository root
    size    (width, height) of the canvas
    groups  dashed boxes: color, label, x, y, w, h
    nodes   boxes: color, box (x, y, w, h), or (x, y, w) for a code card that
            fits its code, title, and optionally role,
            desc (centered lines), code (left-aligned monospace lines; a line
            starting with "*" is highlighted), badge (number), emphasis
    edges   polylines: points (the last one carries the arrowhead), and
            optionally label with its position "at", color, style ("dashed"
            or "dotted"), both (arrowheads at both ends), arrow (False for a
            plain line), halo (cut a gap into lines it crosses), junction
"""

import argparse
import importlib
import os
import shutil
import subprocess
import sys
import tempfile
from html import escape

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
DIAGRAMS = ["components", "jwt_validation", "authz", "deployment", "topology"]

CHROME_TIMEOUT_SECONDS = 60
PNGQUANT_TIMEOUT_SECONDS = 60

FONT = "'Ubuntu Sans', Inter, 'Segoe UI', 'DejaVu Sans', sans-serif"
MONO_FONT = "'Ubuntu Mono', 'DejaVu Sans Mono', Menlo, Consolas, monospace"

BACKGROUND = "#071526"
TITLE_COLOR = "#ffffff"
DESC_COLOR = "#c3d0e0"
CODE_COLOR = "#d6e2f0"
EMPHASIS_FILL = "#14365e"
COLORS = {
    "teal": {"accent": "#2dd4bf", "fill": "#0c2a33", "node": "#0e3540", "edge": "#7dd3c0"},
    "blue": {"accent": "#60a5fa", "fill": "#0a1d36", "node": "#0f2a4a", "edge": "#93c5fd"},
    "violet": {"accent": "#a78bfa", "fill": "#16163a", "node": "#221f4a", "edge": "#a78bfa"},
    "amber": {"accent": "#fbbf24", "fill": "#241c0c", "node": "#2e2410", "edge": "#fcd34d"},
}

CORNER_RADIUS = 10
TITLE_HEIGHT = 26
LINE_HEIGHT = 21
CODE_LINE_HEIGHT = 20
CARD_PADDING = 14
# The arrow marker is 7 stroke widths long on a 2px line.
ARROW_LENGTH = 14


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


def render_group(group):
    style = COLORS[group["color"]]
    return (
        f'<rect x="{group["x"]}" y="{group["y"]}" width="{group["w"]}" height="{group["h"]}" '
        f'rx="22" fill="{style["fill"]}" fill-opacity="0.55" stroke="{style["accent"]}" '
        f'stroke-opacity="0.45" stroke-dasharray="6 6"/>'
        f'<text x="{group["x"] + 22}" y="{group["y"] + 30}" class="group" '
        f'fill="{style["accent"]}">{escape(group["label"])}</text>'
    )


def render_node(node):
    style = COLORS[node["color"]]
    x, y, w, *rest = node["box"]
    lines = []
    if "role" in node:
        lines.append((node["role"], "role", style["accent"]))
    lines += [(line, "desc", DESC_COLOR) for line in node.get("desc", [])]
    code = node.get("code", [])
    block = TITLE_HEIGHT + LINE_HEIGHT * len(lines)
    # A box given without a height fits its code, so code cards never end in
    # empty space.
    h = rest[0] if rest else CARD_PADDING + block + 22 + CODE_LINE_HEIGHT * (len(code) - 1) + 20

    stroke_width = 2.5 if node.get("emphasis") else 1.6
    fill = EMPHASIS_FILL if node.get("emphasis") else style["node"]
    parts = [
        f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="14" fill="{fill}" '
        f'stroke="{style["accent"]}" stroke-width="{stroke_width}" filter="url(#shadow)"/>'
    ]

    # Text-only boxes center their text; boxes with code start at the top so
    # the code reads like a document.
    top = y + CARD_PADDING if code else y + (h - block) / 2
    cx = x + w / 2
    parts.append(
        f'<text x="{cx}" y="{top + 20}" class="title" fill="{TITLE_COLOR}">'
        f'{escape(node["title"])}</text>'
    )
    for i, (line, css, color) in enumerate(lines):
        parts.append(
            f'<text x="{cx}" y="{top + TITLE_HEIGHT + 16 + i * LINE_HEIGHT}" class="{css}" '
            f'fill="{color}">{escape(line)}</text>'
        )
    code_top = top + block + 22
    for i, line in enumerate(code):
        highlighted = line.startswith("*")
        text = line[1:] if highlighted else line
        css = "code hl" if highlighted else "code"
        color = style["accent"] if highlighted else CODE_COLOR
        parts.append(
            f'<text x="{x + 20}" y="{code_top + i * CODE_LINE_HEIGHT}" class="{css}" '
            f'fill="{color}" xml:space="preserve">{escape(text)}</text>'
        )

    if "badge" in node:
        bx, by = x + w - 22, y + 22
        parts.append(
            f'<circle cx="{bx}" cy="{by}" r="14" fill="{style["accent"]}"/>'
            f'<text x="{bx}" y="{by + 5.5}" class="badge" fill="{BACKGROUND}">{node["badge"]}</text>'
        )
    return "".join(parts)


def label_position(edge, width):
    """Keep a label on the last segment clear of the arrowhead.

    A label centered on a short last segment ends right where the arrowhead
    starts, so it is centered between the segment start and the arrowhead base
    instead.
    """
    lx, ly = edge["at"]
    if not edge.get("arrow", True) or len(edge["points"]) < 2:
        return lx, ly
    (sx, sy), (ex, ey) = edge["points"][-2], edge["points"][-1]
    if sx == ex == lx and min(sy, ey) <= ly <= max(sy, ey):
        direction = 1 if ey > sy else -1
        base = ey - direction * ARROW_LENGTH
        return lx, (sy + base) / 2
    if sy == ey == ly and min(sx, ex) <= lx <= max(sx, ex):
        direction = 1 if ex > sx else -1
        base = ex - direction * ARROW_LENGTH
        return (sx + base) / 2, ly
    return lx, ly


def render_edge(edge):
    color_name = edge.get("color", "teal")
    color = COLORS[color_name]["edge"]
    style = edge.get("style")
    if style == "dotted":
        dash = ' stroke-dasharray="2 6" stroke-linecap="round"'
    elif style == "dashed":
        dash = ' stroke-dasharray="8 6"'
    else:
        dash = ""
    markers = ""
    if edge.get("arrow", True):
        markers += f' marker-end="url(#arrow-{color_name})"'
    if edge.get("both"):
        markers += f' marker-start="url(#arrow-{color_name})"'

    path = rounded_path(edge["points"])
    parts = []
    if edge.get("halo"):
        parts.append(f'<path d="{path}" fill="none" stroke="{BACKGROUND}" stroke-width="12"/>')
    parts.append(f'<path d="{path}" fill="none" stroke="{color}" stroke-width="2"{dash}{markers}/>')
    if edge.get("junction"):
        jx, jy = edge["points"][0]
        parts.append(f'<circle cx="{jx}" cy="{jy}" r="4.5" fill="{color}"/>')
    if "label" in edge:
        width = 9 + len(edge["label"]) * 7.6
        lx, ly = label_position(edge, width)
        parts.append(
            f'<rect x="{lx - width / 2}" y="{ly - 12}" width="{width}" height="24" rx="12" '
            f'fill="{BACKGROUND}" stroke="{color}" stroke-opacity="0.35"/>'
            f'<text x="{lx}" y="{ly + 5}" class="edge" fill="{color}">{escape(edge["label"])}</text>'
        )
    return "".join(parts)


def render_svg(diagram):
    width, height = diagram["size"]
    arrow = (
        '<marker id="arrow-{name}" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" '
        'markerHeight="7" orient="auto-start-reverse"><path d="M 0 0 L 10 5 L 0 10 z" '
        'fill="{color}"/></marker>'
    )
    defs = (
        "<defs>"
        + "".join(arrow.format(name=name, color=c["edge"]) for name, c in COLORS.items())
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
        f".code {{ font-family: {MONO_FONT}; font-size: 15px; text-anchor: start; }}"
        ".hl { font-weight: 700; }"
        ".edge { font-size: 13.5px; font-weight: 600; }"
        ".badge { font-size: 15px; font-weight: 700; }"
        "</style>"
    )
    body = [f'<rect width="{width}" height="{height}" fill="{BACKGROUND}"/>']
    body += [render_group(group) for group in diagram["groups"]]
    body += [render_edge(edge) for edge in diagram["edges"]]
    body += [render_node(node) for node in diagram["nodes"].values()]
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" '
        f'viewBox="0 0 {width} {height}">{defs}{style}{"".join(body)}</svg>'
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


def screenshot(svg, size, output, scale, chrome):
    width, height = size
    with tempfile.TemporaryDirectory() as tmp:
        page = os.path.join(tmp, "diagram.html")
        with open(page, "w") as f:
            f.write(f'<!doctype html><html><body style="margin:0">{svg}</body></html>')
        command = [
            chrome, "--headless", "--no-sandbox", "--disable-gpu", "--hide-scrollbars",
            f"--window-size={width},{height}",
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


def quantize(output, pngquant):
    """Reduce the PNG to a 256-color palette.

    The diagrams use a few flat colors plus antialiasing, so a palette keeps
    them visually unchanged at well under half the size, and PNG still shows
    on every device.
    """
    command = [pngquant, "256", "--speed", "1", "--strip", "--force",
               "--output", output, "--", output]
    try:
        result = subprocess.run(command, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE,
                                text=True, errors="replace", timeout=PNGQUANT_TIMEOUT_SECONDS)
    except subprocess.TimeoutExpired:
        print(f"pngquant did not finish within {PNGQUANT_TIMEOUT_SECONDS}s", file=sys.stderr)
        return 1
    if result.returncode != 0:
        print(result.stderr, file=sys.stderr)
    return result.returncode


def main():
    parser = argparse.ArgumentParser(description="Render the Cedarling diagrams used in the docs.")
    parser.add_argument("names", nargs="*", help=f"diagrams to render: {', '.join(DIAGRAMS)} "
                        "(default: all)")
    parser.add_argument("--svg-dir", help="also write the SVG sources to this directory")
    parser.add_argument("--scale", type=int, default=2, help="pixel density multiplier")
    args = parser.parse_args()
    unknown = [name for name in args.names if name not in DIAGRAMS]
    if unknown:
        parser.error(f"unknown diagram: {', '.join(unknown)}")

    chrome = find_chrome()
    if chrome is None:
        print("Chrome or Chromium not found; set CHROME to the browser binary", file=sys.stderr)
        return 1
    pngquant = shutil.which("pngquant")
    if pngquant is None:
        print("pngquant not found; PNGs are written uncompressed (about 2.5x larger)",
              file=sys.stderr)

    for name in args.names or DIAGRAMS:
        diagram = importlib.import_module(name).DIAGRAM
        svg = render_svg(diagram)
        if args.svg_dir:
            with open(os.path.join(args.svg_dir, f"{name}.svg"), "w") as f:
                f.write(svg)
        output = os.path.join(REPO_ROOT, diagram["output"])
        code = screenshot(svg, diagram["size"], output, args.scale, chrome)
        if code == 0 and pngquant:
            code = quantize(output, pngquant)
        if code != 0:
            return code
        print(f"{name}: {diagram['output']}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
