#!/usr/bin/env python3
"""CycloneDX 1.6 CBOM helpers for scan-cbom.yml.

  cbom.py rust CARGO_LOCK OUT          crypto crates in a Cargo.lock -> CBOM
  cbom.py merge NAME VERSION OUT IN... merge CBOMs into one
  cbom.py gate IN [OUT]                strip key values, fail on secret material
"""
import json
import sys
import tomllib
import uuid
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

BLOCKED = {"private-key", "secret-key", "shared-secret", "seed", "password", "credential", "token"}


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
    for pkg in sorted(packages, key=lambda p: (p["name"], p["version"])):
        name, version = pkg["name"], pkg["version"]
        if name not in RUST_CRATES and name not in RUST_PROTOCOLS and name not in RUST_PROVIDERS:
            continue
        ref = f"pkg:cargo/{name}@{version}"
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
    components, seen, deps = [], set(), {}
    for path in inputs:
        with open(path) as f:
            doc = json.load(f)
        for c in doc.get("components", []):
            ref = c.get("bom-ref")
            if ref and ref in seen:
                continue
            if ref:
                seen.add(ref)
            components.append(c)
        for d in doc.get("dependencies", []):
            cur = deps.setdefault(d["ref"], {"ref": d["ref"]})
            for key in ("dependsOn", "provides"):
                if d.get(key):
                    cur[key] = sorted(set(cur.get(key, [])) | set(d[key]))
    meta = {"type": "application", "bom-ref": f"pkg:github/JanssenProject/jans@{version}",
            "name": name, "version": version}
    write(out, bom(components, list(deps.values()), meta))


def gate(path, out=None):
    with open(path) as f:
        doc = json.load(f)
    blocked = []
    for c in doc.get("components", []):
        rcm = (c.get("cryptoProperties") or {}).get("relatedCryptoMaterialProperties")
        if not rcm:
            continue
        # Never publish key material, even public keys: bulky and unneeded for an inventory.
        rcm.pop("value", None)
        if rcm.get("type") in BLOCKED:
            locs = [o.get("location") for o in (c.get("evidence") or {}).get("occurrences", [])]
            blocked.append(f"{rcm['type']}: {c.get('name')} at {', '.join(filter(None, locs)) or 'unknown'}")
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
    elif cmd == "gate":
        gate(*args)
    else:
        sys.exit(__doc__)
