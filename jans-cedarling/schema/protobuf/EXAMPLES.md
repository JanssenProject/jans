# Usage and Testing Examples

## Testing the Converter

### Example 1: Converting Base Schema

```bash
$ python protobuf_to_cedar.py 01-jans-with.proto base_schema.json

Cedar schema saved to base_schema.json

Conversion summary:
  Common types: 4
  Entity types: 9
  Actions: 14
  Base actions: 2

Entity types with inheritance:
  User -> memberOfTypes: ['Role']

Actions by resource type:
  Application:
    - Compare (principals: User, Workload)
    - Execute (principals: User, Workload)
    - Monitor (principals: User, Workload)
    - Read (principals: User, Workload)
    - Search (principals: User, Workload)
    - Share (principals: User, Workload)
    - Tag (principals: User, Workload)
    - Write (principals: User, Workload)
  HTTP_Request:
    - DELETE (principals: Workload)
    - GET (principals: Workload)
    - HEAD (principals: Workload)
    - PATCH (principals: Workload)
    - POST (principals: Workload)
    - PUT (principals: Workload)
```

### Example 2: Converting with Extension (PhotoPrism)

```bash
$ python protobuf_to_cedar.py proto_schemas/ combined_schema.json

Found 2 proto files:
  - 01-jans-with.proto
  - 02-photoprism.proto

Reading 01-jans-with.proto...
Reading 02-photoprism.proto...

Converting to Cedar schema...
Cedar schema saved to combined_schema.json

Conversion summary:
  Common types: 4
  Entity types: 12
  Actions: 28
  Base actions: 5

Entity types with inheritance:
  User -> memberOfTypes: ['Role']

Actions by resource type:
  Album:
    - CreateAlbum (principals: User, Workload)
    - DeleteAlbum (principals: User, Workload)
    - EditAlbum (principals: User, Workload)
    - ShareAlbum (principals: User, Workload)
    - ViewAlbum (principals: User, Workload)
  Application:
    - Compare (principals: User, Workload)
    - Execute (principals: User, Workload)
    - Monitor (principals: User, Workload)
    - Read (principals: User, Workload)
    - Search (principals: User, Workload)
    - Share (principals: User, Workload)
    - Tag (principals: User, Workload)
    - Write (principals: User, Workload)
  Comment:
    - AddComment (principals: User)
    - DeleteComment (principals: User)
    - EditComment (principals: User)
  HTTP_Request:
    - DELETE (principals: Workload)
    - GET (principals: Workload)
    - HEAD (principals: Workload)
    - PATCH (principals: Workload)
    - POST (principals: Workload)
    - PUT (principals: Workload)
  Photo:
    - DeletePhoto (principals: User, Workload)
    - DownloadPhoto (principals: User, Workload)
    - EditPhoto (principals: User, Workload)
    - LikePhoto (principals: User, Workload)
    - UploadPhoto (principals: User, Workload)
    - ViewPhoto (principals: User, Workload)
```

## Generated Cedar Schema Structure

### Common Types (4)

```json
{
  "Context": { "type": "Record", "attributes": {...} },
  "TokensContext": { "type": "Record", "attributes": {...} },
  "Url": { "type": "Record", "attributes": {...} },
  "EmailAddress": { "type": "Record", "attributes": {...} }
}
```

### Entity Types (base - 9)

1. AccessToken
2. Application
3. HTTP_Request
4. Role
5. TrustedIssuer
6. User (memberOf: Role)
7. UserinfoToken
8. Workload
9. IdToken

### Entity Types (PhotoPrism - 3)

10. Photo
11. Album
12. Comment

### Actions (base - 14)

**Application actions (8):**
- Compare, Execute, Monitor, Read, Search, Share, Tag, Write

**HTTP_Request actions (6):**
- DELETE, GET, HEAD, PATCH, POST, PUT

### Actions (PhotoPrism - 14)

**Photo actions (6):**
- ViewPhoto, UploadPhoto, EditPhoto, DeletePhoto, DownloadPhoto, LikePhoto

**Album actions (5):**
- ViewAlbum, CreateAlbum, EditAlbum, DeleteAlbum, ShareAlbum

**Comment actions (3):**
- AddComment, EditComment, DeleteComment

## Generated Entity Example

### Photo entity in Cedar schema:

```json
{
  "Photo": {
    "shape": {
      "type": "Record",
      "attributes": {
        "photo_id": {
          "type": "EntityOrCommon",
          "name": "String"
        },
        "title": {
          "type": "EntityOrCommon",
          "name": "String"
        },
        "description": {
          "type": "EntityOrCommon",
          "name": "String",
          "required": false
        },
        "owner": {
          "type": "EntityOrCommon",
          "name": "User",
          "required": false
        },
        "photo_tags": {
          "type": "Set",
          "element": {
            "type": "EntityOrCommon",
            "name": "String"
          }
        },
        "taken_at": {
          "type": "EntityOrCommon",
          "name": "Long",
          "required": false
        },
        "uploaded_at": {
          "type": "EntityOrCommon",
          "name": "Long",
          "required": false
        },
        "location": {
          "type": "EntityOrCommon",
          "name": "String",
          "required": false
        },
        "album": {
          "type": "EntityOrCommon",
          "name": "Album",
          "required": false
        },
        "is_private": {
          "type": "EntityOrCommon",
          "name": "String",
          "required": false
        }
      }
    }
  }
}
```

## Generated Action Example

### ViewPhoto action in Cedar schema:

```json
{
  "ViewPhoto": {
    "appliesTo": {
      "resourceTypes": [
        "Photo"
      ],
      "principalTypes": [
        "User",
        "Workload"
      ],
      "context": {
        "type": "Context"
      }
    }
  }
}
```

## Validation

### Check type counts:

```bash
$ python3 << EOF
import json
with open('combined_schema.json') as f:
    schema = json.load(f)['Jans']
print(f"Common types: {len(schema['commonTypes'])}")
print(f"Entity types: {len(schema['entityTypes'])}")
print(f"Actions: {len(schema['actions'])}")
EOF

Common types: 4
Entity types: 12
Actions: 28
```

### Check specific entity:

```bash
$ python3 << EOF
import json
with open('combined_schema.json') as f:
    photo = json.load(f)['Jans']['entityTypes']['Photo']
print("Photo attributes:")
for attr in sorted(photo['shape']['attributes'].keys()):
    print(f"  - {attr}")
EOF

Photo attributes:
  - album
  - description
  - is_private
  - location
  - owner
  - photo_id
  - photo_tags
  - taken_at
  - title
  - uploaded_at
```

## Comparison: Before and After

### Before (manual Cedar JSON writing):

❌ Lots of duplication
❌ Hard to maintain
❌ Easy to make mistakes
❌ Difficult to add new applications

### After (Protobuf DSL):

✅ DRY - base actions defined once
✅ Modularity - each application in its own file
✅ Inheritance - via memberOfTypes
✅ Type safety - Protobuf validates structure
✅ Readability - clearer than JSON
✅ Scalability - easy to add new schemas

## Performance Metrics

### Base Schema (01-jans-with.proto)

- Proto size: ~8.4 KB
- Cedar JSON size: ~16.5 KB
- Conversion time: ~0.1s
- Code reduction: ~50% thanks to action inheritance

### With PhotoPrism Extension

- Proto size: ~14 KB (8.4 KB base + 5.6 KB extension)
- Cedar JSON size: ~24 KB
- Conversion time: ~0.15s
- Added: 3 entities, 14 actions in just ~5.6 KB proto

## Extension Possibilities

Easy to add:
- New applications (03-app.proto, 04-app.proto, ...)
- New entity types
- New actions
- New common types
- Custom attributes

All changes are modular and don't affect the base schema!
