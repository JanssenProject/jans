# Protobuf to Cedar Schema Mapping Rules

## Overview

The `jans.proto` file defines a Protobuf DSL (Domain Specific Language) for describing Cedar schemas. This document outlines the mapping rules between Protobuf constructs and Cedar schema elements.

## Meta Fields

Special reserved fields that control schema generation:

### Field 99: Type Category
```protobuf
TypeCategory category = 99 [default = COMMON_TYPE | ENTITY_TYPE];
```
- **Purpose**: Distinguishes between Cedar common types and entity types
- **Values**:
  - `COMMON_TYPE` → Generates a Cedar common type
  - `ENTITY_TYPE` → Generates a Cedar entity type
- **Processing**: This field is excluded from the final schema attributes

### Field 100: Inheritance
```protobuf
optional <Type> memberOfTypes = 100;
```
- **Purpose**: Defines entity type inheritance hierarchy
- **Effect**: Generates `memberOfTypes` in the Cedar entity definition
- **Example**: `Role memberOfTypes = 100;` → `"memberOfTypes": ["Role"]`
- **Processing**: This field is excluded from the final schema attributes

### Field 100: Tags (Alternative Use)
```protobuf
repeated string tags = 100;
```
- **Purpose**: Marks entity types that support tags
- **Effect**: Generates a `tags` Set of Strings in Cedar schema
- **Processing**: This field is excluded from regular attributes

## Type Mapping

### Primitive Types

| Protobuf Type | Cedar Type |
|---------------|------------|
| `string` | `String` |
| `int64` | `Long` |
| `bool` | `Boolean` |

### Field Modifiers

#### Optional Fields
```protobuf
optional string field_name = 1;
```
→ 
```json
{
  "field_name": {
    "type": "EntityOrCommon",
    "name": "String",
    "required": false
  }
}
```

#### Required Fields
```protobuf
string field_name = 1;  // No 'optional' keyword
```
→
```json
{
  "field_name": {
    "type": "EntityOrCommon",
    "name": "String"
  }
}
```
Note: `required` is omitted when true (Cedar default)

#### Repeated Fields (Sets)
```protobuf
repeated string roles = 1;
```
→
```json
{
  "roles": {
    "type": "Set",
    "element": {
      "type": "EntityOrCommon",
      "name": "String"
    }
  }
}
```

### Complex Types

#### Common Types (Records)
```protobuf
message Url {
  TypeCategory category = 99 [default = COMMON_TYPE];
  
  optional string host = 1;
  optional string path = 2;
  optional string protocol = 3;
}
```
→
```json
{
  "Url": {
    "type": "Record",
    "attributes": {
      "host": { "type": "EntityOrCommon", "name": "String", "required": false },
      "path": { "type": "EntityOrCommon", "name": "String", "required": false },
      "protocol": { "type": "EntityOrCommon", "name": "String", "required": false }
    }
  }
}
```

#### Entity Types
```protobuf
message User {
  TypeCategory category = 99 [default = ENTITY_TYPE];
  Role memberOfTypes = 100;
  
  string sub = 1;
  optional string username = 2;
  repeated string role = 3;
}
```
→
```json
{
  "User": {
    "memberOfTypes": ["Role"],
    "shape": {
      "type": "Record",
      "attributes": {
        "sub": { "type": "EntityOrCommon", "name": "String" },
        "username": { "type": "EntityOrCommon", "name": "String", "required": false },
        "role": {
          "type": "Set",
          "element": { "type": "EntityOrCommon", "name": "String" }
        }
      }
    }
  }
}
```

#### Nested Messages (Inline Records)
```protobuf
message HttpRequest {
  TypeCategory category = 99 [default = ENTITY_TYPE];

  message Header {
    optional string Accept = 1;
  }

  Header header = 1;
  Url url = 2;
}
```
→
```json
{
  "HttpRequest": {
    "shape": {
      "type": "Record",
      "attributes": {
        "header": {
          "type": "Record",
          "attributes": {
            "Accept": { "type": "EntityOrCommon", "name": "String", "required": false }
          }
        },
        "url": { "type": "EntityOrCommon", "name": "Url" }
      }
    }
  }
}
```

**Key Point**: Nested messages are converted to inline Record types, not separate type definitions.

#### Tags in Entity Types
```protobuf
message AccessToken {
  TypeCategory category = 99 [default = ENTITY_TYPE];
  repeated string tags = 100;
  
  optional string aud = 1;
  optional int64 exp = 2;
}
```
→
```json
{
  "AccessToken": {
    "shape": {
      "type": "Record",
      "attributes": {
        "aud": { "type": "EntityOrCommon", "name": "String", "required": false },
        "exp": { "type": "EntityOrCommon", "name": "Long", "required": false }
      }
    },
    "tags": {
      "type": "Set",
      "element": { "type": "EntityOrCommon", "name": "String" }
    }
  }
}
```

## Schema Structure

The final Cedar schema is organized as:

```json
{
  "Jans": {
    "commonTypes": {
      // All messages with COMMON_TYPE category
    },
    "entityTypes": {
      // All messages with ENTITY_TYPE category
    },
    "actions": {
      // Action definitions (if any)
    }
  }
}
```

## Design Principles

1. **Separation of Concerns**: Meta fields (99, 100) are separate from data fields (1+)
2. **Explicit Categorization**: Every message must declare its category
3. **Type Safety**: All references must point to defined types
4. **Hierarchy Support**: Entity types can inherit via `memberOfTypes`
5. **Nested Structures**: Support for inline Record types via nested messages
6. **Optional by Default**: Fields marked `optional` have `required: false`
7. **Set Collections**: Use `repeated` for Set types in Cedar

## Conversion Process

1. **First Pass**: Collect all type names (for reference validation)
2. **Second Pass**: Parse messages and determine category
3. **Extract Nested**: Identify and extract nested message definitions
4. **Parse Fields**: Convert fields based on type and modifiers
5. **Build Schema**: Organize into commonTypes and entityTypes
6. **Validate**: Ensure all type references are valid

## Example Complete Type

```protobuf
message User {
  TypeCategory category = 99 [default = ENTITY_TYPE];
  Role memberOfTypes = 100;

  optional EmailAddress email = 1;
  optional IdToken id_token = 2;
  optional string phone_number = 3;
  repeated string role = 4;
  string sub = 5;
  optional string username = 6;
}
```

This defines:
- An **entity type** named "User"
- That **inherits from** "Role"
- With **6 attributes**:
  - 5 optional fields (email, id_token, phone_number, role set, username)
  - 1 required field (sub)
- References **3 other types**: EmailAddress, IdToken, and implicitly String

## Notes

- Field numbers 1-98 are for actual data attributes
- Field number 99 is reserved for type category
- Field number 100 is reserved for inheritance/tags
- Nested messages create inline Record types, not separate type definitions
- All type references (e.g., `EmailAddress`, `Role`) must be defined elsewhere
