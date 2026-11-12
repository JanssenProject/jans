"""Discover docs/script-catalog scripts and resolve each one's custom script type."""
import os
import re
from dataclasses import dataclass, field
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
CATALOG = REPO / "docs" / "script-catalog"
SCRIPT_TYPE_JAVA = REPO / "jans-core/script/src/main/java/io/jans/model/custom/script/CustomScriptType.java"

_ENUM_ENTRY = re.compile(
    r'\b[A-Z0-9_]+\(\s*"([a-z0-9_]+)"\s*,\s*"[^"]*"\s*,\s*(\w+)\.class\s*,\s*\w+\.class\s*,\s*"(\w+)"')
_PY_CLASS = re.compile(r"^class\s+(\w+)\s*\(", re.M)
_JAVA_IMPLEMENTS = re.compile(r"\bclass\s+\w+[^{]*?\bimplements\s+([\w\s,.]+?)\s*\{", re.S)


@dataclass
class ScriptType:
    value: str
    interface: str
    python_class: str


@dataclass
class CatalogScript:
    path: Path
    language: str
    script_type: str = None
    candidates: list = field(default_factory=list)

    @property
    def rel(self):
        return self.path.relative_to(CATALOG).as_posix()

    @property
    def key(self):
        """Same key persistence-loader/jans-linux-setup derive for scripts.ldif placeholders."""
        rel = self.path.relative_to(CATALOG)
        ext_type = rel.parent.as_posix().lower().replace(os.path.sep, "_").replace("-", "_")
        return f"{ext_type}_{self.path.stem.lower()}"

    def text(self):
        return self.path.read_text(encoding="utf-8")


def load_script_types(source=SCRIPT_TYPE_JAVA):
    return [ScriptType(*m) for m in _ENUM_ENTRY.findall(Path(source).read_text(encoding="utf-8"))]


def resolve_type(text, language, top_dir, types):
    """Type whose python class / Java interface the script defines; the catalog dir breaks ties."""
    if language == "python":
        names = set(_PY_CLASS.findall(text))
        cands = [t for t in types if t.python_class in names]
    else:
        names = set()
        for m in _JAVA_IMPLEMENTS.findall(text):
            names.update(n.strip().split(".")[-1] for n in m.split(","))
        cands = [t for t in types if t.interface in names]
    values = [t.value for t in cands]
    if not values:
        return None, values
    for v in values:
        if v == top_dir or v.startswith(top_dir) or top_dir.startswith(v):
            return v, values
    return values[0], values


def discover(catalog=CATALOG, types=None):
    types = types if types is not None else load_script_types()
    out = []
    for p in sorted(Path(catalog).rglob("*")):
        if not p.is_file() or p.suffix.lower() not in (".py", ".java"):
            continue
        lang = "python" if p.suffix.lower() == ".py" else "java"
        s = CatalogScript(path=p, language=lang)
        top = p.relative_to(catalog).parts[0]
        s.script_type, s.candidates = resolve_type(p.read_text(encoding="utf-8"), lang, top, types)
        out.append(s)
    return out
