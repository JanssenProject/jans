#!/usr/bin/env python3
"""CycloneDX 1.6 CBOM helpers for scan-cbom.yml.

  cbom.py rust CARGO_LOCK OUT          crypto crates in a Cargo.lock -> CBOM
  cbom.py merge NAME VERSION OUT IN... merge CBOMs into one
  cbom.py label IN REF [PLATFORM] [INDEX]  tag every finding with the scanned image
  cbom.py stamp IN NAME=VALUE...       set metadata properties
  cbom.py gate IN [OUT]                strip key values, fail on secret material
"""
import json
import sys
import tomllib
import uuid
from urllib.parse import quote
from datetime import datetime, timezone

# cbomkit has no Rust scanner. Map the crypto crates Cedarling resolves to the
# algorithms/protocols they provide; 0 = broken by a cryptographically relevant
# quantum computer (Shor).
SIG = ["keygen", "sign", "verify"]
RUST_CRATES = {
    "ed25519-dalek": [("Ed25519", {"primitive": "signature", "curve": "Ed25519", "cryptoFunctions": SIG, "nistQuantumSecurityLevel": 0})],
    "p256": [("ECDSA-P256", {"primitive": "signature", "curve": "secp256r1", "cryptoFunctions": SIG, "nistQuantumSecurityLevel": 0})],
    "p384": [("ECDSA-P384", {"primitive": "signature", "curve": "secp384r1", "cryptoFunctions": SIG, "nistQuantumSecurityLevel": 0})],
    "rsa": [("RSA", {"primitive": "signature", "cryptoFunctions": SIG, "nistQuantumSecurityLevel": 0})],
    "sha2": [(n, {"primitive": "hash", "cryptoFunctions": ["digest"]}) for n in ("SHA-256", "SHA-384", "SHA-512")],
    "sha3": [(n, {"primitive": "hash", "cryptoFunctions": ["digest"]}) for n in ("SHA3-256", "SHA3-512")],
    "hmac": [("HMAC", {"primitive": "mac", "cryptoFunctions": ["tag"]})],
    "hkdf": [("HKDF", {"primitive": "kdf", "cryptoFunctions": ["keyderive"]})],
}
RUST_PROTOCOLS = {"rustls": [("TLSv1.2", "1.2"), ("TLSv1.3", "1.3")]}
RUST_PROVIDERS = {"ring", "aws-lc-rs"}

CRATES_IO = {"registry+https://github.com/rust-lang/crates.io-index", "sparse+https://index.crates.io/"}

BLOCKED = {"private-key", "secret-key", "shared-secret", "seed", "password", "credential", "token", "key"}
# gitleaks generic-api-key false positive on stock JDK config (no value is captured).
ALLOWED = {("key", "generic-api-key", "/conf/security/java.security")}


def bom(components, dependencies, meta_component=None):
    meta = {"timestamp": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")}
    if meta_component:
        meta["component"] = meta_component
    return {
        "$schema": "http://cyclonedx.org/schema/bom-1.6.schema.json",
        "bomFormat": "CycloneDX",
        "specVersion": "1.6",
        "serialNumber": f"urn:uuid:{uuid.uuid4()}",
        "version": 1,
        "metadata": meta,
        "components": components,
        "dependencies": dependencies,
    }


def rust(lock_path, out):
    with open(lock_path, "rb") as f:
        packages = tomllib.load(f).get("package", [])
    components, dependencies, assets = [], [], {}
    for pkg in sorted(packages, key=lambda p: (p["name"], p["version"], p.get("source", ""))):
        name, version, source = pkg["name"], pkg["version"], pkg.get("source", "")
        if name not in RUST_CRATES and name not in RUST_PROTOCOLS and name not in RUST_PROVIDERS:
            continue
        ref = f"pkg:cargo/{name}@{version}"
        if source.startswith("git+"):
            ref += "?vcs_url=" + quote(source.split("#")[0], safe="")
        elif source and source not in CRATES_IO:
            ref += "?repository_url=" + quote(source.split("+", 1)[-1], safe="")
        components.append({"type": "library", "bom-ref": ref, "name": name, "version": version, "purl": ref})
        provides = []
        for algo, props in RUST_CRATES.get(name, []):
            aref = f"crypto/algorithm/{algo}"
            assets.setdefault(aref, {"type": "cryptographic-asset", "bom-ref": aref, "name": algo,
                                     "cryptoProperties": {"assetType": "algorithm", "algorithmProperties": props}})
            provides.append(aref)
        for proto, ver in RUST_PROTOCOLS.get(name, []):
            pref = f"crypto/protocol/{proto}"
            assets.setdefault(pref, {"type": "cryptographic-asset", "bom-ref": pref, "name": proto,
                                     "cryptoProperties": {"assetType": "protocol",
                                                          "protocolProperties": {"type": "tls", "version": ver}}})
            provides.append(pref)
        if provides:
            dependencies.append({"ref": ref, "provides": provides})
    components.extend(assets.values())
    write(out, bom(components, dependencies))


def merge(name, version, out, inputs):
    # Allocated first so an input root carrying the same ref is not duplicated.
    aggregate = f"pkg:github/JanssenProject/jans@{version}#{name}"
    components, seen, deps, by_ref = [], {aggregate}, {}, {}
    for path in inputs:
        with open(path) as f:
            doc = json.load(f)
        root = (doc.get("metadata") or {}).get("component")
        # Keep each input's root so its dependency edges still resolve.
        for c in ([root] if root and root.get("bom-ref") else []) + doc.get("components", []):
            key = c.get("bom-ref") or json.dumps(c, sort_keys=True)
            if key in seen:
                # Same finding from another input (e.g. platform): keep its provenance too.
                kept = by_ref.get(key)
                if kept is not None:
                    props = kept.setdefault("properties", [])
                    props.extend(p for p in c.get("properties", []) if p not in props)
                continue
            seen.add(key)
            if c.get("bom-ref"):
                by_ref[key] = c
            components.append(c)
        for d in doc.get("dependencies", []):
            cur = deps.setdefault(d["ref"], {"ref": d["ref"]})
            for key in ("dependsOn", "provides"):
                if d.get(key):
                    cur[key] = sorted(set(cur.get(key, [])) | set(d[key]))
    meta = {"type": "application", "bom-ref": aggregate, "name": name, "version": version}
    write(out, bom(components, list(deps.values()), meta))


def walk(components):
    for c in components:
        yield c
        yield from walk(c.get("components", []))


def label(path, image, platform="", index=""):
    with open(path) as f:
        doc = json.load(f)
    props = [{"name": "jans:image", "value": image}]
    if platform:
        props.append({"name": "jans:platform", "value": platform})
    for c in walk(doc.get("components", [])):
        c.setdefault("properties", []).extend(props)
    meta = doc.setdefault("metadata", {})
    old = meta.get("component")
    if old and old.get("bom-ref"):
        # Keep the scanner's root so its dependency edges still resolve.
        doc["components"].append(old)
    name, _, digest = image.partition("@")
    container = {"type": "container", "bom-ref": image, "name": name, "version": digest or name,
                 "properties": props + ([{"name": "jans:image-index", "value": index}] if index else [])}
    meta["component"] = container
    write(path, doc)


def stamp(path, pairs):
    with open(path) as f:
        doc = json.load(f)
    new = dict(p.split("=", 1) for p in pairs)
    meta = doc.setdefault("metadata", {})
    meta["properties"] = [p for p in meta.get("properties", []) if p.get("name") not in new] + [
        {"name": k, "value": v} for k, v in new.items()]
    write(path, doc)


def allowed(kind, name, locs):
    return bool(locs) and all(
        any(kind == a and name == n and loc.endswith(sfx) for a, n, sfx in ALLOWED) for loc in locs)


def gate(path, out=None):
    with open(path) as f:
        doc = json.load(f)
    blocked = []
    root = (doc.get("metadata") or {}).get("component")
    for c in walk(([root] if root else []) + doc.get("components", [])):
        rcm = (c.get("cryptoProperties") or {}).get("relatedCryptoMaterialProperties")
        if not rcm:
            continue
        # Never publish key material, even public keys: bulky and unneeded for an inventory.
        rcm.pop("value", None)
        locs = [o.get("location") or "" for o in (c.get("evidence") or {}).get("occurrences", [])]
        kind = rcm.get("type")
        if kind in BLOCKED and not allowed(kind, c.get("name"), locs):
            blocked.append(f"{kind}: {c.get('name')} at {', '.join(filter(None, locs)) or 'unknown'}")
    write(out or path, doc)
    if blocked:
        print(f"::error::{len(blocked)} secret crypto material finding(s); not publishing", file=sys.stderr)
        for b in blocked:
            print(f"  {b}", file=sys.stderr)
        sys.exit(1)


def write(path, doc):
    with open(path, "w") as f:
        json.dump(doc, f, indent=1)
        f.write("\n")
    print(f"{path}: {len(doc.get('components', []))} components")


if __name__ == "__main__":
    cmd, args = sys.argv[1], sys.argv[2:]
    if cmd == "rust":
        rust(*args)
    elif cmd == "merge":
        merge(args[0], args[1], args[2], args[3:])
    elif cmd == "label":
        label(*args)
    elif cmd == "stamp":
        stamp(args[0], args[1:])
    elif cmd == "gate":
        gate(*args)
    else:
        sys.exit(__doc__)
