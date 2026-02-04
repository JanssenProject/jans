#!/usr/bin/env python3
"""
Converter from Protobuf DSL to Cedar schema format.
"""

import json
import re
from typing import Dict, List, Any, Optional, Set, Tuple
from dataclasses import dataclass, field
from enum import Enum


class CedarType(Enum):
    """Cedar type system."""
    STRING = "String"
    LONG = "Long"
    RECORD = "Record"
    SET = "Set"
    ENTITY = "Entity"


@dataclass
class CedarAttribute:
    """Represents a Cedar attribute definition."""
    name: str
    type: str
    element_type: Optional[str] = None
    required: bool = False
    attributes: Optional[Dict[str, 'CedarAttribute']] = None

    def to_dict(self) -> Dict[str, Any]:
        """Convert to Cedar schema dictionary."""
        if self.type == "Set":
            result = {
                "type": "Set",
                "element": {
                    "type": "EntityOrCommon",
                    "name": self.element_type
                }
            }
            if not self.required:
                result["required"] = False
            return result
        elif self.type == "Record":
            result = {
                "type": "Record",
                "attributes": {
                    name: attr.to_dict()
                    for name, attr in (self.attributes or {}).items()
                }
            }
            if not self.required:
                result["required"] = False
            return result
        else:
            # For simple types and references
            result = {
                "type": "EntityOrCommon",
                "name": self.type
            }
            if not self.required:
                result["required"] = False
            return result


@dataclass
class CedarEntityType:
    """Represents a Cedar entity type."""
    name: str
    member_of_types: List[str] = field(default_factory=list)
    attributes: Dict[str, CedarAttribute] = field(default_factory=dict)
    tags: List[str] = field(default_factory=list)

    def to_dict(self) -> Dict[str, Any]:
        """Convert to Cedar entity type dictionary."""
        result = {}

        if self.member_of_types:
            result["memberOfTypes"] = self.member_of_types

        if self.attributes:
            result["shape"] = {
                "type": "Record",
                "attributes": {
                    name: attr.to_dict()
                    for name, attr in self.attributes.items()
                }
            }

        if self.tags:
            result["tags"] = {
                "type": "Set",
                "element": {
                    "type": "EntityOrCommon",
                    "name": "String"
                }
            }

        return result


@dataclass
class CedarCommonType:
    """Represents a Cedar common type."""
    name: str
    type: str = "Record"
    attributes: Dict[str, CedarAttribute] = field(default_factory=dict)

    def to_dict(self) -> Dict[str, Any]:
        """Convert to Cedar common type dictionary."""
        result = {"type": self.type}

        if self.attributes:
            result["attributes"] = {
                name: attr.to_dict()
                for name, attr in self.attributes.items()
            }

        return result


@dataclass
class CedarAction:
    """Represents a Cedar action."""
    name: str
    resource_types: List[str] = field(default_factory=list)
    principal_types: List[str] = field(default_factory=list)
    context_type: Optional[str] = None
    member_of_types: List[str] = field(default_factory=list)

    def to_dict(self) -> Dict[str, Any]:
        """Convert to Cedar action dictionary."""
        result = {
            "appliesTo": {
                "resourceTypes": self.resource_types,
                "principalTypes": self.principal_types
            }
        }

        if self.context_type:
            result["appliesTo"]["context"] = {
                "type": self.context_type
            }

        # Note: memberOf is not included in Cedar schema output
        # It's only used internally for inheritance resolution

        return result


class ProtobufToCedarConverter:
    """Converts Protobuf DSL to Cedar schema."""

    # Meta fields that should be ignored during conversion
    META_FIELDS = {'category', 'memberOfTypes', 'tags'}
    META_FIELD_NUMBERS = {100, 101, 102}  # Updated field numbers

    def __init__(self, protobuf_content: str):
        self.protobuf_content = protobuf_content
        self.common_types: Dict[str, CedarCommonType] = {}
        self.entity_types: Dict[str, CedarEntityType] = {}
        self.actions: Dict[str, CedarAction] = {}
        self.base_actions: Dict[str, Dict[str, Any]] = {}  # Store base action definitions
        self.all_known_types: Set[str] = set()

    def parse_field(self, field_line: str) -> Optional[Dict[str, Any]]:
        """Parse a single protobuf field definition."""
        field_line = field_line.strip()

        # Skip comments and empty lines
        if not field_line or field_line.startswith('//'):
            return None

        # Extract field number to check if it's a meta field
        field_num_match = re.search(r'=\s*(\d+)', field_line)
        if field_num_match:
            field_num = int(field_num_match.group(1))
            if field_num in self.META_FIELD_NUMBERS:
                return None

        # Match field pattern
        pattern = r'^(?P<repeated>repeated\s+)?(?P<optional>optional\s+)?(?P<type>\w+)\s+(?P<n>\w+)\s*=\s*\d+(?:\s*\[[^\]]+\])*;'
        match = re.match(pattern, field_line)

        if not match:
            return None

        field_type = match.group('type')
        field_name = match.group('n')
        is_repeated = bool(match.group('repeated'))
        is_optional = bool(match.group('optional'))

        # Skip meta fields by name
        if field_name in self.META_FIELDS:
            return None

        # Determine if field is required
        # In proto3, optional keyword makes it explicitly optional
        # Fields without optional are required unless they're message types
        required = not is_optional and not is_repeated

        return {
            'name': field_name,
            'type': field_type,
            'repeated': is_repeated,
            'optional': is_optional,
            'required': required,
            'raw_line': field_line
        }

    def is_known_type(self, type_name: str) -> bool:
        """Check if type is known."""
        return type_name in self.all_known_types

    def extract_nested_message(self, lines: List[str], start_idx: int) -> Tuple[str, List[str], int]:
        """
        Extract a nested message starting from start_idx.
        Returns (message_name, message_lines, end_idx).
        """
        # Find message name
        message_name = None
        for i in range(start_idx, min(start_idx + 3, len(lines))):
            match = re.search(r'message\s+(\w+)', lines[i])
            if match:
                message_name = match.group(1)
                break

        if not message_name:
            return None, [], start_idx

        # Extract message content
        message_lines = []
        depth = 0
        i = start_idx
        started = False

        while i < len(lines):
            line = lines[i]
            message_lines.append(line)

            # Count braces
            depth += line.count('{') - line.count('}')

            if '{' in line:
                started = True

            # When depth returns to 0 after starting, we're done
            if started and depth == 0:
                return message_name, message_lines, i + 1

            i += 1

        return message_name, message_lines, i

    def parse_nested_message_attributes(self, message_lines: List[str]) -> Dict[str, CedarAttribute]:
        """Parse attributes from nested message lines."""
        attributes = {}

        for line in message_lines:
            stripped = line.strip()

            # Skip message declaration, braces, and meta fields (updated field numbers)
            if (stripped.startswith('message ') or
                    stripped in ['{', '}', ''] or
                    'category = 100' in stripped or
                    'memberOfTypes = 101' in stripped or
                    'tags = 102' in stripped):
                continue

            # Parse field
            field_info = self.parse_field(stripped)
            if field_info:
                # Determine Cedar type
                if self.is_known_type(field_info['type']):
                    cedar_type = field_info['type']
                elif field_info['type'] == 'int64':
                    cedar_type = 'Long'
                else:
                    cedar_type = 'String'

                if field_info['repeated']:
                    attr = CedarAttribute(
                        name=field_info['name'],
                        type="Set",
                        element_type=cedar_type,
                        required=field_info['required']
                    )
                else:
                    attr = CedarAttribute(
                        name=field_info['name'],
                        type=cedar_type,
                        required=field_info['required']
                    )

                attributes[field_info['name']] = attr

        return attributes

    def collect_all_types(self):
        """First pass: collect all type names."""
        lines = self.protobuf_content.split('\n')
        for line in lines:
            match = re.match(r'^message\s+(\w+)', line.strip())
            if match:
                self.all_known_types.add(match.group(1))

    def parse_message(self, message_name: str, message_lines: List[str]):
        """Parse a message and add it to common_types, entity_types, or actions."""
        # Determine message category
        category = None
        for line in message_lines:
            if 'category = 100' in line:
                if 'ENTITY_TYPE' in line:
                    category = 'ENTITY_TYPE'
                elif 'ACTION_TYPE' in line:
                    category = 'ACTION_TYPE'
                elif 'COMMON_TYPE' in line:
                    category = 'COMMON_TYPE'
                break

        if category == 'ENTITY_TYPE':
            self.parse_entity_message(message_name, message_lines)
        elif category == 'ACTION_TYPE':
            self.parse_action_message(message_name, message_lines)
        else:
            self.parse_common_message(message_name, message_lines)

    def parse_action_message(self, message_name: str, message_lines: List[str]):
        """Parse an action type message."""
        action = CedarAction(name=message_name)
        
        # Check if this action inherits from a base action
        inherits_from = None
        for line in message_lines:
            if 'memberOfTypes = 101' in line:
                # Extract the base action type
                parts = line.strip().split()
                for i, part in enumerate(parts):
                    if part == 'memberOfTypes' and i > 0:
                        inherits_from = parts[i - 1]
                        if inherits_from == 'optional':
                            inherits_from = None
                        break
                break
        
        if inherits_from:
            # This action inherits from a base action
            # Store it for later processing after base actions are parsed
            action.member_of_types = [inherits_from]
            self.actions[message_name] = action
        else:
            # This is a base action - parse its structure
            self.parse_base_action(message_name, message_lines)

    def parse_base_action(self, message_name: str, message_lines: List[str]):
        """Parse a base action and store its appliesTo definition."""
        resource_types = []
        principal_types = []
        context_type = None
        
        # Store nested messages
        nested_messages: Dict[str, Dict[str, CedarAttribute]] = {}
        
        i = 0
        while i < len(message_lines):
            line = message_lines[i]
            stripped = line.strip()
            
            # Check for nested message
            if re.match(r'^\s+message\s+\w+', line):
                nested_name, nested_lines, end_idx = self.extract_nested_message(message_lines, i)
                if nested_name:
                    # Parse nested message to extract types
                    if nested_name == 'ResourceTypes':
                        resource_types = self.extract_types_from_nested_message(nested_lines)
                    elif nested_name == 'PrincipalTypes':
                        principal_types = self.extract_types_from_nested_message(nested_lines)
                    elif nested_name == 'ContextTypes':
                        context_type = self.extract_context_type(nested_lines)
                    i = end_idx
                    continue
            
            i += 1
        
        # Store base action definition
        self.base_actions[message_name] = {
            'resource_types': resource_types,
            'principal_types': principal_types,
            'context_type': context_type
        }

    def extract_types_from_nested_message(self, nested_lines: List[str]) -> List[str]:
        """Extract type names from ResourceTypes or PrincipalTypes nested message."""
        types = []
        for line in nested_lines:
            stripped = line.strip()
            # Match: optional TypeName field_name = N;
            match = re.match(r'optional\s+(\w+)\s+\w+\s*=\s*\d+;', stripped)
            if match:
                types.append(match.group(1))
        return types

    def extract_context_type(self, nested_lines: List[str]) -> Optional[str]:
        """Extract context type from ContextTypes nested message."""
        for line in nested_lines:
            stripped = line.strip()
            # Match: optional Context context = 1;
            match = re.match(r'optional\s+(\w+)\s+\w+\s*=\s*\d+;', stripped)
            if match:
                return match.group(1)
        return None

    def resolve_action_inheritance(self):
        """Resolve action inheritance after all base actions are parsed."""
        for action_name, action in self.actions.items():
            if action.member_of_types:
                base_action_name = action.member_of_types[0]
                if base_action_name in self.base_actions:
                    base = self.base_actions[base_action_name]
                    action.resource_types = base['resource_types']
                    action.principal_types = base['principal_types']
                    action.context_type = base['context_type']

    def parse_entity_message(self, message_name: str, message_lines: List[str]):
        """Parse an entity type message."""
        entity = CedarEntityType(name=message_name)

        # Store nested messages found in this entity
        nested_messages: Dict[str, Dict[str, CedarAttribute]] = {}

        i = 0
        while i < len(message_lines):
            line = message_lines[i]
            stripped = line.strip()

            # Check for nested message
            if re.match(r'^\s+message\s+\w+', line):  # Indented message = nested
                nested_name, nested_lines, end_idx = self.extract_nested_message(message_lines, i)
                if nested_name:
                    # Parse nested message attributes
                    nested_attrs = self.parse_nested_message_attributes(nested_lines)
                    nested_messages[nested_name] = nested_attrs
                    i = end_idx
                    continue

            # Handle memberOfTypes (updated field number)
            if 'memberOfTypes = 101' in stripped:
                parts = stripped.split()
                for j, part in enumerate(parts):
                    if part == 'memberOfTypes' and j > 0:
                        member_type = parts[j - 1]
                        if member_type != 'optional':
                            entity.member_of_types.append(member_type)
                        break
                i += 1
                continue

            # Handle tags (updated field number)
            if 'tags = 102' in stripped or 'repeated string tags = 102' in stripped:
                entity.tags = ["String"]
                i += 1
                continue

            # Skip other meta fields (updated field number)
            if 'category = 100' in stripped or stripped.startswith('message '):
                i += 1
                continue

            # Parse regular field
            field_info = self.parse_field(stripped)
            if field_info:
                # Check if this field references a nested message
                if field_info['type'] in nested_messages:
                    # Create Record attribute with nested message attributes
                    attr = CedarAttribute(
                        name=field_info['name'],
                        type="Record",
                        attributes=nested_messages[field_info['type']],
                        required=field_info['required']
                    )
                else:
                    # Determine Cedar type for regular fields
                    if self.is_known_type(field_info['type']):
                        cedar_type = field_info['type']
                    elif field_info['type'] == 'int64':
                        cedar_type = 'Long'
                    else:
                        cedar_type = 'String'

                    if field_info['repeated']:
                        attr = CedarAttribute(
                            name=field_info['name'],
                            type="Set",
                            element_type=cedar_type,
                            required=field_info['required']
                        )
                    else:
                        attr = CedarAttribute(
                            name=field_info['name'],
                            type=cedar_type,
                            required=field_info['required']
                        )

                entity.attributes[field_info['name']] = attr

            i += 1

        self.entity_types[message_name] = entity

    def parse_common_message(self, message_name: str, message_lines: List[str]):
        """Parse a common type message."""
        common_type = CedarCommonType(name=message_name)

        for line in message_lines:
            stripped = line.strip()

            # Skip meta fields and message declaration (updated field numbers)
            if (stripped.startswith('message ') or
                    stripped in ['{', '}', ''] or
                    'category = 100' in stripped or
                    'memberOfTypes = 101' in stripped or
                    'tags = 102' in stripped):
                continue

            # Parse regular field
            field_info = self.parse_field(stripped)
            if field_info:
                # Determine Cedar type
                if self.is_known_type(field_info['type']):
                    cedar_type = field_info['type']
                elif field_info['type'] == 'int64':
                    cedar_type = 'Long'
                else:
                    cedar_type = 'String'

                if field_info['repeated']:
                    attr = CedarAttribute(
                        name=field_info['name'],
                        type="Set",
                        element_type=cedar_type,
                        required=field_info['required']
                    )
                else:
                    attr = CedarAttribute(
                        name=field_info['name'],
                        type=cedar_type,
                        required=field_info['required']
                    )

                common_type.attributes[field_info['name']] = attr

        self.common_types[message_name] = common_type

    def extract_all_top_level_messages(self) -> List[Tuple[str, List[str]]]:
        """Extract all top-level message blocks from protobuf."""
        messages = []
        lines = self.protobuf_content.split('\n')
        i = 0

        while i < len(lines):
            line = lines[i]
            stripped = line.strip()

            # Look for top-level message declarations (not indented)
            if re.match(r'^message\s+\w+', line):
                message_name, message_lines, end_idx = self.extract_nested_message(lines, i)
                if message_name:
                    messages.append((message_name, message_lines))
                    i = end_idx
                    continue

            i += 1

        return messages

    def convert(self) -> Dict[str, Any]:
        """Convert Protobuf to Cedar schema."""
        # First pass: collect all type names
        self.collect_all_types()

        # Second pass: extract and parse all messages
        messages = self.extract_all_top_level_messages()

        for message_name, message_lines in messages:
            self.parse_message(message_name, message_lines)

        # Resolve action inheritance
        self.resolve_action_inheritance()

        # Build the final Cedar schema
        cedar_schema = {
            "Jans": {
                "commonTypes": {
                    name: common_type.to_dict()
                    for name, common_type in self.common_types.items()
                },
                "entityTypes": {
                    name: entity_type.to_dict()
                    for name, entity_type in self.entity_types.items()
                },
                "actions": {
                    name: action.to_dict()
                    for name, action in self.actions.items()
                }
            }
        }

        return cedar_schema

    def save_to_file(self, output_path: str):
        """Convert and save to file."""
        cedar_schema = self.convert()

        with open(output_path, 'w', encoding='utf-8') as f:
            json.dump(cedar_schema, f, indent=2, ensure_ascii=False)

        print(f"Cedar schema saved to {output_path}")
        return cedar_schema


def main():
    """Main function to run the converter."""
    import sys
    import os
    import glob

    if len(sys.argv) not in [2, 3]:
        print("Usage: python protobuf_to_cedar.py <input_directory_or_file> [output.json]")
        print("  If input is a directory, all .proto files will be processed")
        print("  If input is a file, only that file will be processed")
        sys.exit(1)

    input_path = sys.argv[1]
    output_file = sys.argv[2] if len(sys.argv) == 3 else "cedar_schema.json"

    # Determine if input is directory or file
    proto_files = []
    if os.path.isdir(input_path):
        # Find all .proto files in directory
        pattern = os.path.join(input_path, "*.proto")
        proto_files = sorted(glob.glob(pattern))
        if not proto_files:
            print(f"Error: No .proto files found in directory: {input_path}")
            sys.exit(1)
        print(f"Found {len(proto_files)} proto files:")
        for f in proto_files:
            print(f"  - {os.path.basename(f)}")
    elif os.path.isfile(input_path):
        proto_files = [input_path]
    else:
        print(f"Error: Input path does not exist: {input_path}")
        sys.exit(1)

    # Read and concatenate all protobuf files
    combined_content = []
    for proto_file in proto_files:
        print(f"\nReading {os.path.basename(proto_file)}...")
        with open(proto_file, 'r', encoding='utf-8') as f:
            content = f.read()
            # Skip the syntax and package declarations after the first file
            if len(combined_content) > 0:
                # Remove syntax and package lines from subsequent files
                lines = content.split('\n')
                filtered_lines = []
                for line in lines:
                    stripped = line.strip()
                    if not (stripped.startswith('syntax =') or 
                           stripped.startswith('package ')):
                        filtered_lines.append(line)
                content = '\n'.join(filtered_lines)
            combined_content.append(content)

    # Combine all content
    protobuf_content = '\n\n'.join(combined_content)

    # Convert
    print(f"\nConverting to Cedar schema...")
    converter = ProtobufToCedarConverter(protobuf_content)
    cedar_schema = converter.save_to_file(output_file)

    # Print summary
    print(f"\nConversion summary:")
    print(f"  Common types: {len(converter.common_types)}")
    print(f"  Entity types: {len(converter.entity_types)}")
    print(f"  Actions: {len(converter.actions)}")
    print(f"  Base actions: {len(converter.base_actions)}")

    # Show entity types with their memberOfTypes
    if any(e.member_of_types for e in converter.entity_types.values()):
        print("\nEntity types with inheritance:")
        for name, entity in converter.entity_types.items():
            if entity.member_of_types:
                print(f"  {name} -> memberOfTypes: {entity.member_of_types}")

    # Show actions grouped by resource type
    print("\nActions by resource type:")
    actions_by_resource = {}
    for name, action in converter.actions.items():
        for resource in action.resource_types:
            if resource not in actions_by_resource:
                actions_by_resource[resource] = []
            actions_by_resource[resource].append(name)
    
    for resource, action_names in sorted(actions_by_resource.items()):
        print(f"  {resource}:")
        for action_name in sorted(action_names):
            action = converter.actions[action_name]
            principals = ', '.join(action.principal_types)
            print(f"    - {action_name} (principals: {principals})")


if __name__ == "__main__":
    main()
