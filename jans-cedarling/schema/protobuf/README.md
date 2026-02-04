# Protobuf to Cedar Schema Converter

This tool allows you to describe Cedar schemas in a simplified Protobuf format and automatically convert them to Cedar JSON schema.

## Project Structure

```
proto_schemas/
├── 01-jans-with.proto      # Base schema (common types, entity types, base actions)
├── 02-photoprism.proto     # Extension for PhotoPrism application
└── 03-your-app.proto       # Your extension
```

## Usage

### Converting a Single File
```bash
python protobuf_to_cedar.py input.proto output.json
```

### Converting All Files from a Directory
```bash
python protobuf_to_cedar.py proto_schemas/ cedar_schema.json
```

The script automatically:
- Finds all `.proto` files in the directory
- Sorts them by name (01-, 02-, 03-, ...)
- Combines their content
- Generates a unified Cedar schema

## Protobuf DSL Format

### Special Fields

```protobuf
// For all types
TypeCategory category = 100 [default = COMMON_TYPE | ENTITY_TYPE | ACTION_TYPE];

// For inheritance (entity types and actions)
BaseType memberOfTypes = 101;

// For entity types with tags
repeated string tags = 102;
```

### Common Types

```protobuf
message MyCommonType {
  TypeCategory category = 100 [default = COMMON_TYPE];
  
  string field1 = 1;
  optional int64 field2 = 2;
  repeated string field3 = 3;  // Set<String>
}
```

### Entity Types

```protobuf
message MyEntity {
  TypeCategory category = 100 [default = ENTITY_TYPE];
  
  // Optional: inheritance
  Role memberOfTypes = 101;
  
  // Optional: tags support
  repeated string tags = 102;
  
  // Attributes
  string entity_id = 1;
  optional User owner = 2;
  repeated string roles = 3;
}
```

### Actions

#### Base Action

```protobuf
message MyResourceAction {
  TypeCategory category = 100 [default = ACTION_TYPE];
  
  message ResourceTypes {
    optional MyResource my_resource = 1;
  }
  
  message PrincipalTypes {
    optional User user = 1;
    optional Workload workload = 2;
  }
  
  message ContextTypes {
    optional Context context = 1;
  }
  
  ResourceTypes resource_types = 1;
  PrincipalTypes principal_types = 2;
  optional ContextTypes context_types = 3;
}
```

#### Concrete Action (with inheritance)

```protobuf
message DoSomething {
  TypeCategory category = 100 [default = ACTION_TYPE];
  optional MyResourceAction memberOfTypes = 101;
}
```

## Schema Extension Example

### File: 02-photoprism.proto

This file adds to the base schema:
- **3 new entity types**: Photo, Album, Comment
- **3 base action types**: PhotoAction, AlbumAction, CommentAction
- **14 concrete actions**: ViewPhoto, UploadPhoto, EditPhoto, etc.

### Conversion Result

```
Entity types: 12 (9 base + 3 new)
Actions: 28 (14 base + 14 new)
```

## Important Notes

### 1. Avoid Field Name Conflicts

**INCORRECT:**
```protobuf
message Photo {
  repeated string tags = 5;  // ❌ Conflicts with meta-field tags = 102
}
```

**CORRECT:**
```protobuf
message Photo {
  repeated string photo_tags = 5;  // ✅ Unique name
}
```

### 2. File Numbering

Use prefixes to control loading order:
- `01-base-schema.proto` - base schema
- `02-app1.proto` - extension 1
- `03-app2.proto` - extension 2

### 3. Data Types

| Protobuf | Cedar |
|----------|-------|
| `string` | `String` |
| `int64` | `Long` |
| `bool` | `String` (automatic) |
| `repeated Type` | `Set<Type>` |
| `optional Type` | `Type` with `required: false` |

### 4. Type References

```protobuf
message Photo {
  optional User owner = 1;      // reference to User entity
  optional Album album = 2;     // reference to Album entity
  repeated User viewers = 3;    // Set<User>
}
```

## Output Cedar Schema

Result is valid Cedar JSON schema:

```json
{
  "Jans": {
    "commonTypes": { ... },
    "entityTypes": { ... },
    "actions": { ... }
  }
}
```

## Advantages of This Approach

1. ✅ **DRY principle**: base actions defined once
2. ✅ **Modularity**: easy to add new applications
3. ✅ **Type safety**: Protobuf ensures structure validation
4. ✅ **Inheritance**: via `memberOfTypes`
5. ✅ **Readability**: simpler than JSON
6. ✅ **Scalability**: easy to manage large schemas

## Usage Examples

### Adding a New Application

1. Create file `03-myapp.proto`
2. Define new entity types
3. Create base actions for your resources
4. Define concrete actions
5. Run converter on the entire directory

### Result

All schemas (base + all extensions) will be combined into one Cedar schema.
