# Quick Start Guide

## Quick Start in 3 Steps

### Step 1: Preparation

Create a directory for your proto schemas:

```bash
mkdir proto_schemas
cd proto_schemas
```

### Step 2: Copy the Base Schema

```bash
cp 01-jans-with.proto proto_schemas/
```

The base schema contains:
- Common types: Context, Url, EmailAddress, TokensContext
- Entity types: User, Workload, Application, HTTP_Request, etc.
- Base actions: ApplicationAction, HttpRequestAction
- Actions: Read, Write, Execute, GET, POST, etc.

### Step 3: Add Your Extension

Create file `02-myapp.proto`:

```protobuf
syntax = "proto3";

package jans;

// Your entity type
message MyResource {
  TypeCategory category = 100 [default = ENTITY_TYPE];
  
  string resource_id = 1;
  string name = 2;
  optional User owner = 3;
}

// Base action for your resource
message MyResourceAction {
  TypeCategory category = 100 [default = ACTION_TYPE];
  
  message ResourceTypes {
    optional MyResource my_resource = 1;
  }
  
  message PrincipalTypes {
    optional User user = 1;
  }
  
  message ContextTypes {
    optional Context context = 1;
  }
  
  ResourceTypes resource_types = 1;
  PrincipalTypes principal_types = 2;
  optional ContextTypes context_types = 3;
}

// Concrete actions
message ViewMyResource {
  TypeCategory category = 100 [default = ACTION_TYPE];
  optional MyResourceAction memberOfTypes = 101;
}

message EditMyResource {
  TypeCategory category = 100 [default = ACTION_TYPE];
  optional MyResourceAction memberOfTypes = 101;
}
```

### Step 4: Conversion

```bash
python protobuf_to_cedar.py proto_schemas/ output.json
```

Result:
```
Found 2 proto files:
  - 01-jans-with.proto
  - 02-myapp.proto

Converting to Cedar schema...
Cedar schema saved to output.json

Conversion summary:
  Common types: 4
  Entity types: 10  (9 base + 1 new)
  Actions: 16      (14 base + 2 new)
  Base actions: 3
```

## Real-World Examples

### Example: PhotoPrism Application

See file `02-photoprism.proto` - a complete example of schema extension for a photo management application.

Adds:
- **Entity types**: Photo, Album, Comment
- **Actions**: ViewPhoto, UploadPhoto, EditPhoto, CreateAlbum, AddComment, etc.

### Using the Result

The generated `output.json` is a valid Cedar schema, ready to use:

```python
import json

# Load schema
with open('output.json') as f:
    cedar_schema = json.load(f)

# Use in Cedar Policy Engine
# (specific code depends on your implementation)
```

## Frequently Asked Questions

### Q: Can I modify the base schema?

A: Yes, but it's better to create a new file (e.g., `00-custom-base.proto`) to preserve the original base schema.

### Q: How do I add a new common type?

A: In any `.proto` file, add:

```protobuf
message MyCommonType {
  TypeCategory category = 100 [default = COMMON_TYPE];
  
  string field1 = 1;
  optional int64 field2 = 2;
}
```

### Q: Can I use one file for everything?

A: Yes, but a modular approach (base schema + extensions) is more convenient for maintenance.

### Q: How do I update only my part of the schema?

A: Modify your `02-myapp.proto` and re-run the converter - the base schema won't change.

## Next Steps

1. Study `02-photoprism.proto` as a reference
2. Create your own `02-yourapp.proto`
3. Test conversion at each step
4. Integrate the generated Cedar schema into your application

## Support

For questions and suggestions, see the main README.md
