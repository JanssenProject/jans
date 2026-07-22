# This software is available under the Apache-2.0 license.
# See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
#
# Copyright (c) 2026, Gluu, Inc.

"""
This script converts a supported .cjar archive (containing rego policies) to an OPA policy bundle.
"""

import typing as _t
import json
import shutil
import tempfile
import zipfile
import subprocess
from datetime import datetime, timezone
import argparse
from pathlib import Path


def validate_metadata(metadata: dict) -> dict:

    policy_engine = metadata.get("policy_engine")
    if policy_engine not in ("opa", "rego"):
        raise ValueError(
            f"policy_engine must be 'opa' or 'rego', got: '{policy_engine}'"
        )

    policy_store = metadata.get("policy_store")
    if not isinstance(policy_store, dict):
        raise ValueError("metadata.json must contain a 'policy_store' object")

    required_store_fields = {"id", "name", "version"}
    if not required_store_fields.issubset(policy_store.keys()):
        missing = required_store_fields - policy_store.keys()
        raise ValueError(f"policy_store is missing required fields: {missing}")

    return metadata


def aggregate_json_directory(dir_path: Path) -> _t.Dict:
    """Reads all .json files in a directory and aggregates them into a single dict."""
    aggregated = {}
    if not dir_path.exists():
        return aggregated

    for entry in dir_path.iterdir():
        if entry.is_file() and entry.name.endswith(".json"):
            with open(entry, encoding="utf-8") as f:
                try:
                    content = json.load(f)
                    aggregated[entry.stem] = content
                except json.JSONDecodeError:
                    print(f"Skipping file {entry}")
    return aggregated


def convert_cjar_to_opa(
    input_path: Path,
    output_path: Path,
    signing_key: str | None = None,
    signing_alg: str | None = None,
):
    """Main conversion logic."""

    # 1. Verify OPA binary is available
    opa_path = shutil.which("opa")
    if not opa_path:
        raise EnvironmentError(
            "The 'opa' binary was not found in your system PATH.\n"
            "Please install it first: https://www.openpolicyagent.org/docs#install-and-run-opa"
        )

    with tempfile.TemporaryDirectory() as temp_dir:
        temp_path = Path(temp_dir)
        extract_dir = temp_path / "extracted"
        Path.mkdir(extract_dir, exist_ok=True)

        # 2. Extract input (.cjar/.zip or directory)
        if input_path.is_file() and (
            input_path.suffix == ".cjar" or input_path.suffix == ".zip"
        ):
            with zipfile.ZipFile(input_path, "r") as zip_ref:
                resolved_extract_dir = extract_dir.resolve()
                for member in zip_ref.infolist():
                    member_path = (extract_dir / member.filename).resolve()
                    if not str(member_path).startswith(str(resolved_extract_dir)):
                        raise ValueError(f"Unsafe path in archive: {member.filename}")
                zip_ref.extractall(extract_dir)

        elif input_path.is_dir():
            for item in input_path.iterdir():
                dst = extract_dir / item.name
                if item.is_dir():
                    shutil.copytree(item, dst)
                else:
                    shutil.copy2(item, dst)
        else:
            raise FileNotFoundError(
                f"Input path is not a valid .cjar, .zip, or directory: {input_path}"
            )

        # 3. Validate metadata.json
        metadata_path = extract_dir / "metadata.json"
        if not metadata_path.exists():
            raise FileNotFoundError(
                "metadata.json is missing in the input archive/directory"
            )

        with open(metadata_path, "r", encoding="utf-8") as f:
            metadata = json.load(f)

        metadata = validate_metadata(metadata)
        policy_store = metadata["policy_store"]

        # 4. Prepare OPA bundle directory
        bundle_dir = temp_path / "opa_bundle"
        Path.mkdir(bundle_dir, exist_ok=True)

        # 5. Copy policies/**/*.rego
        policies_dir = extract_dir / "policies"

        if policies_dir.exists():
            for src_path in policies_dir.rglob("*.rego"):
                rel_path = src_path.relative_to(policies_dir)
                dst_path = bundle_dir / rel_path
                dst_path.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(src_path, dst_path)
        else:
            print("Warning: No 'policies' directory found in the input.")

        # 6. Aggregate entities and trusted-issuers into a single data.json
        bundle_data = {}

        entities_data = aggregate_json_directory(extract_dir / "entities")
        if entities_data:
            bundle_data["entities"] = entities_data

        trusted_issuers_data = aggregate_json_directory(extract_dir / "trusted-issuers")
        if trusted_issuers_data:
            bundle_data["trusted_issuers"] = trusted_issuers_data

        if bundle_data:
            data_json_path = bundle_dir / "data.json"
            with open(data_json_path, "w", encoding="utf-8") as f:
                json.dump(bundle_data, f, indent=2)

        # 7. Generate .manifest
        manifest = {
            "revision": f"{policy_store['version']}-{datetime.now(timezone.utc).strftime('%Y%m%d%H%M%S')}",
            "rego_version": 1,
            "roots": [],
            "metadata": {
                "policy_store": {
                    "id": policy_store["id"],
                    "name": policy_store["name"],
                    "version": policy_store["version"],
                }
            },
        }

        if "engine" in metadata:
            manifest["engine"] = metadata["engine"]

        manifest_path = bundle_dir / ".manifest"
        with open(manifest_path, "w", encoding="utf-8") as f:
            json.dump(manifest, f, indent=2)

        # 8. Package using the `opa build` binary
        cmd = [opa_path, "build", str(bundle_dir), "-o", str(output_path)]

        if signing_key:
            cmd.extend(["--signing-key", signing_key])
        if signing_alg:
            cmd.extend(["--signing-alg", signing_alg])

        print(f"Building OPA bundle using: {' '.join(cmd)}")
        try:
            result = subprocess.run(cmd, check=True, capture_output=True, text=True)
            if result.stdout:
                print(result.stdout.strip())
            print(f"Successfully converted and built OPA bundle: {output_path}")
        except subprocess.CalledProcessError as e:
            print(f"OPA build failed with exit code {e.returncode}")
            if e.stderr:
                print(f"Error details:\n{e.stderr}")
            raise


def main():
    parser = argparse.ArgumentParser(
        description="Convert a .cjar policy store archive into an OPA bundle.tar.gz using the opa binary"
    )
    parser.add_argument(
        "input", help="Path to the input .cjar, .zip file, or directory", type=Path
    )
    parser.add_argument(
        "-o",
        "--output",
        default="bundle.tar.gz",
        help="Path to the output OPA bundle.tar.gz (default: bundle.tar.gz)",
        type=Path,
    )
    parser.add_argument(
        "--signing-key",
        help="Path to the PEM file containing the private key for bundle signing (optional)",
    )
    parser.add_argument(
        "--signing-alg",
        default="RS256",
        help="Signing algorithm to use (e.g., RS256, ES256). Default: RS256",
    )

    args = parser.parse_args()
    try:
        convert_cjar_to_opa(
            args.input,
            args.output,
            signing_key=args.signing_key,
            signing_alg=args.signing_alg,
        )
    except Exception as e:
        print(f"Error: {e}")
        exit(1)


if __name__ == "__main__":
    main()
