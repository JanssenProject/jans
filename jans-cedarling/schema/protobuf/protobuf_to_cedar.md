# Protobuf to Cedar Converter - Installation & Usage Guide

## Prerequisites

- Python 3.7 or higher
- pip (Python package manager)

## Installation

### 1. Install Required Dependencies

The converter requires the `protobuf` package for proper protobuf parsing:

```bash
pip install protobuf
```

**Note**: While the current implementation doesn't use the protobuf library directly, it's recommended to have it installed for potential future enhancements and compatibility.

### 2. Download the Converter

Save the `protobuf_to_cedar.py` file to your working directory.

## Usage

### Basic Command

```bash
python protobuf_to_cedar.py <input.proto> <output.json>
```

### Parameters

- `<input.proto>` - Path to your input Protobuf file
- `<output.json>` - Path for the output Cedar schema JSON file

### Example

```bash
python protobuf_to_cedar.py jans.proto cedarling_core.json
```

This will:
1. Read the `jans.proto` file
2. Parse all message definitions
3. Convert them to Cedar schema format
4. Save the result to `cedarling_core.json`

## Quick Start

### Step 1: Install Dependencies

```bash
pip install protobuf
```

### Step 2: Prepare Your Protobuf File

Ensure your `.proto` file follows the jans.proto conventions:

```protobuf
syntax = "proto3";

package jans;

// Common type example
message Url {
  TypeCategory category = 99 [default = COMMON_TYPE];
  
  optional string host = 1;
  optional string path = 2;
  optional string protocol = 3;
}

// Entity type example
message User {
  TypeCategory category = 99 [default = ENTITY_TYPE];
  Role memberOfTypes = 100;
  
  string sub = 1;
  optional string username = 2;
}
```

### Step 3: Run the Converter

```bash
python protobuf_to_cedar.py your_schema.proto output.json
```

### Step 4: Verify Output

The converter will display a summary:

```
Cedar schema saved to output.json

Conversion summary:
  Common types: 4
  Entity types: 9

Entity types with inheritance:
  User -> memberOfTypes: ['Role']

HttpRequest structure:
{
  "shape": {
    "type": "Record",
    "attributes": {
      ...
    }
  }
}
```

## Output Format

The generated JSON file will have the following structure:

```json
{
  "Jans": {
    "commonTypes": {
      "TypeName": {
        "type": "Record",
        "attributes": { ... }
      }
    },
    "entityTypes": {
      "EntityName": {
        "memberOfTypes": ["ParentType"],
        "shape": {
          "type": "Record",
          "attributes": { ... }
        },
        "tags": { ... }
      }
    },
    "actions": {}
  }
}
```

## Common Issues & Solutions

### Issue 1: Python Not Found

```
python: command not found
```

**Solution**: Install Python 3.7+ from [python.org](https://www.python.org/downloads/) or use `python3` instead:

```bash
python3 protobuf_to_cedar.py input.proto output.json
```

### Issue 2: Module Not Found

```
ModuleNotFoundError: No module named 'protobuf'
```

**Solution**: Install the protobuf package:

```bash
pip install protobuf
```

If using Python 3, you might need:

```bash
pip3 install protobuf
```

### Issue 3: Permission Denied

```
PermissionError: [Errno 13] Permission denied: 'output.json'
```

**Solution**: 
- Check file permissions
- Run with appropriate permissions
- Choose a different output directory where you have write access

### Issue 4: Invalid Protobuf Syntax

```
Conversion error or incomplete output
```

**Solution**:
- Verify your protobuf file syntax
- Ensure all messages have `category = 99` field
- Check that all type references are defined
- Validate field numbering (99, 100 reserved; 1+ for data)

## Advanced Usage

### Custom Package Name

The converter uses "Jans" as the default package name. To modify this, edit the converter code:

```python
cedar_schema = {
    "YourPackageName": {  # Change this line
        "commonTypes": { ... },
        "entityTypes": { ... },
        "actions": {}
    }
}
```

### Adding Actions

To add Cedar actions to your schema, modify the `convert()` method to populate the `"actions"` dictionary.

### Batch Conversion

Convert multiple files:

```bash
for file in *.proto; do
    python protobuf_to_cedar.py "$file" "${file%.proto}.json"
done
```

## Validation

After conversion, validate your Cedar schema:

1. **Check JSON syntax**:
   ```bash
   python -m json.tool output.json
   ```

2. **Verify structure**:
   - All common types are present
   - All entity types have proper shapes
   - All type references are valid
   - memberOfTypes point to existing types

3. **Review nested structures**:
   - Nested messages should appear as inline Record types
   - Check that fields aren't incorrectly flattened

## Examples

### Example 1: Simple Conversion

**Input** (`simple.proto`):
```protobuf
syntax = "proto3";
package jans;

message User {
  TypeCategory category = 99 [default = ENTITY_TYPE];
  string sub = 1;
  optional string name = 2;
}
```

**Command**:
```bash
python protobuf_to_cedar.py simple.proto simple.json
```

**Output** (`simple.json`):
```json
{
  "Jans": {
    "commonTypes": {},
    "entityTypes": {
      "User": {
        "shape": {
          "type": "Record",
          "attributes": {
            "sub": {
              "type": "EntityOrCommon",
              "name": "String"
            },
            "name": {
              "type": "EntityOrCommon",
              "name": "String",
              "required": false
            }
          }
        }
      }
    },
    "actions": {}
  }
}
```

### Example 2: With Nested Messages

**Input**:
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

**Output**: The `Header` nested message becomes an inline Record type in the `header` attribute.

## Troubleshooting

### Enable Debug Output

Add print statements to see what's being processed:

```python
# Add after line "for message_name, message_lines in messages:"
print(f"Processing: {message_name}")
```

### Validate Input File

Check your protobuf file:
```bash
# Check file exists
ls -l your_schema.proto

# View contents
cat your_schema.proto

# Check for syntax errors (if protoc is installed)
protoc --proto_path=. --python_out=. your_schema.proto
```

## Support

For issues or questions:
1. Check that your protobuf file follows the jans.proto conventions
2. Verify all dependencies are installed
3. Review the output summary for conversion statistics
4. Check the CHANGES.md file for known issues and fixes

## Additional Resources

- **PROTOBUF_RULES.md**: Detailed mapping rules between Protobuf and Cedar
- **CHANGES.md**: Detailed explanation of fixes and improvements
- **README.md**: Overview and technical details
- Cedar Schema Documentation: [Cedar documentation](https://docs.cedarpolicy.com/)
