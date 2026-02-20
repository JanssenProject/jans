# Cedarling Policy Store Schema

This directory contains the JSON Schema definition for validating Cedarling policy store files, along with examples and validation instructions.

## Files

- `policy_store_schema.json` - JSON Schema (Draft 2020-12) that defines the structure of policy store files
- `cedarling_core.cedarschema` - Cedar schema defining entity types, actions, and context
- `minimal_policy_store.json` - Minimal working example of a policy store

## Quick Start

### Generate JSON Schema from Cedar Schema

Generate the JSON representation of the Cedar schema using the Cedar CLI:

```bash
cedar translate-schema --direction cedar-to-json --schema jans-cedarling/schema/cedarling_core.cedarschema
```

The Cedar schema can also be generated from JSON representation with the below command:

```bash
cedar translate-schema --direction json-to-cedar --schema jans-cedarling/schema/cedarling_core.json
```

### Validate a Policy Store

Using Python's `jsonschema` module:

```bash
# Install jsonschema if needed
pip install jsonschema

# Validate a policy store file
python -m jsonschema -i jans-cedarling/schema/minimal_policy_store.json jans-cedarling/schema/policy_store_schema.json
```

Using an online validator:

1. Go to [JSON Schema Validator](https://www.jsonschemavalidator.net/)
2. Paste `policy_store_schema.json` in the left panel
3. Paste your policy store JSON in the right panel
4. Check for validation errors
