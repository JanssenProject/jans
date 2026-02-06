use serde_json::{json, Value};
use std::collections::{HashMap, HashSet};
use std::fs;
use std::path::Path;

#[derive(Debug, Clone, PartialEq)]
enum TypeCategory {
    CommonType,
    EntityType,
    ActionType,
}

#[derive(Debug, Clone)]
struct CedarAttribute {
    type_name: String,
    element_type: Option<String>,
    required: bool,
    attributes: Option<HashMap<String, CedarAttribute>>,
}

impl CedarAttribute {
    fn to_json(&self) -> Value {
        if self.type_name == "Set" {
            let mut result = json!({
                "type": "Set",
                "element": {
                    "type": "EntityOrCommon",
                    "name": self.element_type.as_ref().unwrap()
                }
            });
            if !self.required {
                result["required"] = json!(false);
            }
            result
        } else if self.type_name == "Record" {
            let mut attrs = serde_json::Map::new();
            if let Some(ref attributes) = self.attributes {
                for (name, attr) in attributes {
                    attrs.insert(name.clone(), attr.to_json());
                }
            }
            let mut result = json!({
                "type": "Record",
                "attributes": attrs
            });
            if !self.required {
                result["required"] = json!(false);
            }
            result
        } else {
            let mut result = json!({
                "type": "EntityOrCommon",
                "name": self.type_name
            });
            if !self.required {
                result["required"] = json!(false);
            }
            result
        }
    }
}

#[derive(Debug, Clone)]
struct CedarEntityType {
    member_of_types: Vec<String>,
    attributes: HashMap<String, CedarAttribute>,
    has_tags: bool,
}

impl CedarEntityType {
    fn to_json(&self) -> Value {
        let mut result = serde_json::Map::new();

        if !self.member_of_types.is_empty() {
            result.insert(
                "memberOfTypes".to_string(),
                json!(self.member_of_types),
            );
        }

        if !self.attributes.is_empty() {
            let mut attrs = serde_json::Map::new();
            for (name, attr) in &self.attributes {
                attrs.insert(name.clone(), attr.to_json());
            }
            result.insert(
                "shape".to_string(),
                json!({
                    "type": "Record",
                    "attributes": attrs
                }),
            );
        }

        if self.has_tags {
            result.insert(
                "tags".to_string(),
                json!({
                    "type": "Set",
                    "element": {
                        "type": "EntityOrCommon",
                        "name": "String"
                    }
                }),
            );
        }

        json!(result)
    }
}

#[derive(Debug, Clone)]
struct CedarCommonType {
    attributes: HashMap<String, CedarAttribute>,
}

impl CedarCommonType {
    fn to_json(&self) -> Value {
        let mut attrs = serde_json::Map::new();
        for (name, attr) in &self.attributes {
            attrs.insert(name.clone(), attr.to_json());
        }
        json!({
            "type": "Record",
            "attributes": attrs
        })
    }
}

#[derive(Debug, Clone)]
struct CedarAction {
    resource_types: Vec<String>,
    principal_types: Vec<String>,
    context_type: Option<String>,
}

impl CedarAction {
    fn to_json(&self) -> Value {
        let mut applies_to = serde_json::Map::new();
        applies_to.insert("resourceTypes".to_string(), json!(self.resource_types));
        applies_to.insert("principalTypes".to_string(), json!(self.principal_types));

        if let Some(ref context_type) = self.context_type {
            applies_to.insert(
                "context".to_string(),
                json!({
                    "type": context_type
                }),
            );
        }

        json!({
            "appliesTo": applies_to
        })
    }
}

#[derive(Debug)]
struct BaseAction {
    resource_types: Vec<String>,
    principal_types: Vec<String>,
    context_type: Option<String>,
}

struct ProtobufToCedarConverter {
    common_types: HashMap<String, CedarCommonType>,
    entity_types: HashMap<String, CedarEntityType>,
    actions: HashMap<String, CedarAction>,
    base_actions: HashMap<String, BaseAction>,
    action_inheritance: HashMap<String, String>, // action_name -> base_action_name
    all_known_types: HashSet<String>,
}

impl ProtobufToCedarConverter {
    fn new() -> Self {
        Self {
            common_types: HashMap::new(),
            entity_types: HashMap::new(),
            actions: HashMap::new(),
            base_actions: HashMap::new(),
            action_inheritance: HashMap::new(),
            all_known_types: HashSet::new(),
        }
    }

    fn is_meta_field_number(&self, field_num: i32) -> bool {
        field_num == 100 || field_num == 101 || field_num == 102
    }

    fn parse_field(&self, line: &str) -> Option<(String, String, bool, bool, bool)> {
        let line = line.trim();
        
        // Skip comments and empty lines
        if line.is_empty() || line.starts_with("//") {
            return None;
        }

        // Check for meta field numbers
        if let Some(field_num_pos) = line.find(" = ") {
            let after_equals = &line[field_num_pos + 3..];
            if let Some(semicolon_pos) = after_equals.find(';') {
                let field_num_str = &after_equals[..semicolon_pos].split('[').next().unwrap().trim();
                if let Ok(field_num) = field_num_str.parse::<i32>() {
                    if self.is_meta_field_number(field_num) {
                        return None;
                    }
                }
            }
        }

        // Parse field pattern: [repeated] [optional] Type name = number;
        let mut is_repeated = false;
        let mut is_optional = false;
        let mut parts: Vec<&str> = line.split_whitespace().collect();

        if parts.is_empty() {
            return None;
        }

        // Remove repeated
        if parts[0] == "repeated" {
            is_repeated = true;
            parts.remove(0);
        }

        // Remove optional
        if !parts.is_empty() && parts[0] == "optional" {
            is_optional = true;
            parts.remove(0);
        }

        if parts.len() < 4 {
            return None;
        }

        let field_type = parts[0].to_string();
        let field_name = parts[1].to_string();

        // Skip meta fields by name
        if field_name == "category" || field_name == "memberOfTypes" || field_name == "tags" {
            return None;
        }

        let required = !is_optional && !is_repeated;

        Some((field_name, field_type, is_repeated, is_optional, required))
    }

    fn extract_category(&self, content: &str) -> TypeCategory {
        if content.contains("category = 100") {
            if content.contains("ENTITY_TYPE") {
                TypeCategory::EntityType
            } else if content.contains("ACTION_TYPE") {
                TypeCategory::ActionType
            } else {
                TypeCategory::CommonType
            }
        } else {
            TypeCategory::CommonType
        }
    }

    fn extract_member_of_types(&self, content: &str) -> Vec<String> {
        let mut members = Vec::new();
        for line in content.lines() {
            if line.contains("memberOfTypes = 101") {
                let parts: Vec<&str> = line.trim().split_whitespace().collect();
                for (i, part) in parts.iter().enumerate() {
                    if *part == "memberOfTypes" && i > 0 {
                        let type_name = parts[i - 1];
                        // Extract the type name (skip 'optional' keyword)
                        if type_name == "optional" && i > 1 {
                            members.push(parts[i - 2].to_string());
                        } else if type_name != "optional" {
                            members.push(type_name.to_string());
                        }
                        break;
                    }
                }
            }
        }
        members
    }

    fn has_tags(&self, content: &str) -> bool {
        content.contains("tags = 102")
    }

    fn parse_attributes(&self, content: &str) -> HashMap<String, CedarAttribute> {
        let mut attributes = HashMap::new();
        
        // First, extract nested messages (like Header in HTTP_Request)
        let mut nested_messages: HashMap<String, HashMap<String, CedarAttribute>> = HashMap::new();
        let mut pos = 0;
        while let Some(msg_start) = content[pos..].find("  message ") {  // Two spaces = nested
            let actual_pos = pos + msg_start;
            if let Some((nested_name, nested_content, end_pos)) = self.extract_message(content, actual_pos) {
                // Parse nested message attributes
                let nested_attrs = self.parse_nested_message_attributes(&nested_content);
                nested_messages.insert(nested_name, nested_attrs);
                pos = end_pos;
            } else {
                break;
            }
        }

        // Track which lines are inside nested messages to skip them
        let mut skip_ranges: Vec<(usize, usize)> = Vec::new();
        let lines: Vec<&str> = content.lines().collect();
        let mut i = 0;
        while i < lines.len() {
            let line = lines[i];
            if line.trim().starts_with("message ") && line.starts_with("  ") {
                // Found nested message, find its end
                let mut depth = 0;
                let start_i = i;
                for j in i..lines.len() {
                    if lines[j].contains('{') {
                        depth += lines[j].matches('{').count();
                    }
                    if lines[j].contains('}') {
                        depth -= lines[j].matches('}').count();
                    }
                    if depth == 0 && j > start_i {
                        skip_ranges.push((start_i, j));
                        i = j + 1;
                        break;
                    }
                }
            } else {
                i += 1;
            }
        }

        // Parse fields, skipping those inside nested messages
        for (line_idx, line) in lines.iter().enumerate() {
            // Check if this line is inside a nested message
            let in_nested = skip_ranges.iter().any(|(start, end)| line_idx >= *start && line_idx <= *end);
            if in_nested {
                continue;
            }

            if let Some((field_name, field_type, is_repeated, _is_optional, required)) =
                self.parse_field(line)
            {
                // Check if this field references a nested message
                if let Some(nested_attrs) = nested_messages.get(&field_type) {
                    // Create Record attribute with nested message attributes
                    let attr = CedarAttribute {
                        type_name: "Record".to_string(),
                        element_type: None,
                        required,
                        attributes: Some(nested_attrs.clone()),
                    };
                    attributes.insert(field_name, attr);
                } else {
                    // Regular field
                    // Map protobuf types to Cedar types
                    let cedar_type = if field_type == "int64" {
                        "Long".to_string()
                    } else if field_type == "bool" {
                        "String".to_string()
                    } else if field_type == "string" {
                        "String".to_string()  // Capitalize string -> String
                    } else if self.all_known_types.contains(&field_type) {
                        field_type.clone()
                    } else {
                        field_type.clone()
                    };

                    let attr = if is_repeated {
                        CedarAttribute {
                            type_name: "Set".to_string(),
                            element_type: Some(cedar_type),
                            required,
                            attributes: None,
                        }
                    } else {
                        CedarAttribute {
                            type_name: cedar_type,
                            element_type: None,
                            required,
                            attributes: None,
                        }
                    };

                    attributes.insert(field_name, attr);
                }
            }
        }

        attributes
    }
    
    fn parse_nested_message_attributes(&self, content: &str) -> HashMap<String, CedarAttribute> {
        let mut attributes = HashMap::new();

        for line in content.lines() {
            if let Some((field_name, field_type, is_repeated, _is_optional, required)) =
                self.parse_field(line)
            {
                // Map protobuf types to Cedar types
                let cedar_type = if field_type == "int64" {
                    "Long".to_string()
                } else if field_type == "bool" {
                    "String".to_string()
                } else if field_type == "string" {
                    "String".to_string()  // Capitalize string -> String
                } else if self.all_known_types.contains(&field_type) {
                    field_type.clone()
                } else {
                    field_type.clone()
                };

                let attr = if is_repeated {
                    CedarAttribute {
                        type_name: "Set".to_string(),
                        element_type: Some(cedar_type),
                        required,
                        attributes: None,
                    }
                } else {
                    CedarAttribute {
                        type_name: cedar_type,
                        element_type: None,
                        required,
                        attributes: None,
                    }
                };

                attributes.insert(field_name, attr);
            }
        }

        attributes
    }

    fn extract_types_from_nested(&self, content: &str) -> Vec<String> {
        let mut types = Vec::new();
        for line in content.lines() {
            let line = line.trim();
            if line.starts_with("optional") {
                let parts: Vec<&str> = line.split_whitespace().collect();
                if parts.len() >= 2 {
                    types.push(parts[1].to_string());
                }
            }
        }
        types
    }

    fn extract_context_type(&self, content: &str) -> Option<String> {
        for line in content.lines() {
            let line = line.trim();
            if line.starts_with("optional") && line.contains("context") {
                let parts: Vec<&str> = line.split_whitespace().collect();
                if parts.len() >= 2 {
                    return Some(parts[1].to_string());
                }
            }
        }
        None
    }

    fn extract_message(&self, content: &str, start_pos: usize) -> Option<(String, String, usize)> {
        let lines: Vec<&str> = content[start_pos..].lines().collect();
        
        // Find message name
        let mut message_name = String::new();
        for line in &lines[..3.min(lines.len())] {
            if line.trim().starts_with("message ") {
                let parts: Vec<&str> = line.trim().split_whitespace().collect();
                if parts.len() >= 2 {
                    message_name = parts[1].to_string();
                    break;
                }
            }
        }

        if message_name.is_empty() {
            return None;
        }

        // Extract message content
        let mut depth = 0;
        let mut started = false;
        let mut message_content = String::new();
        let mut char_count = 0;

        for ch in content[start_pos..].chars() {
            message_content.push(ch);
            char_count += 1;

            if ch == '{' {
                depth += 1;
                started = true;
            } else if ch == '}' {
                depth -= 1;
            }

            if started && depth == 0 {
                return Some((message_name, message_content, start_pos + char_count));
            }
        }

        None
    }

    fn parse_base_action(&mut self, name: &str, content: &str) {
        let mut resource_types = Vec::new();
        let mut principal_types = Vec::new();
        let mut context_type = None;

        // Find nested messages (indented with spaces)
        let mut pos = 0;
        while let Some(msg_start) = content[pos..].find("  message ") {  // Two spaces = nested
            let actual_pos = pos + msg_start;
            if let Some((nested_name, nested_content, end_pos)) = self.extract_message(content, actual_pos) {
                if nested_name == "ResourceTypes" {
                    resource_types = self.extract_types_from_nested(&nested_content);
                } else if nested_name == "PrincipalTypes" {
                    principal_types = self.extract_types_from_nested(&nested_content);
                } else if nested_name == "ContextTypes" {
                    context_type = self.extract_context_type(&nested_content);
                }
                pos = end_pos;
            } else {
                break;
            }
        }

        self.base_actions.insert(
            name.to_string(),
            BaseAction {
                resource_types,
                principal_types,
                context_type,
            },
        );
    }

    fn parse_action(&mut self, name: &str, content: &str) {
        let member_of_types = self.extract_member_of_types(content);

        if !member_of_types.is_empty() {
            // This action inherits from a base action
            let base_action_name = member_of_types[0].clone();
            self.action_inheritance.insert(name.to_string(), base_action_name);
            
            self.actions.insert(
                name.to_string(),
                CedarAction {
                    resource_types: vec![],
                    principal_types: vec![],
                    context_type: None,
                },
            );
        } else {
            // This is a base action
            self.parse_base_action(name, content);
        }
    }

    fn parse_entity(&mut self, name: &str, content: &str) {
        let member_of_types = self.extract_member_of_types(content);
        let has_tags = self.has_tags(content);
        let attributes = self.parse_attributes(content);

        self.entity_types.insert(
            name.to_string(),
            CedarEntityType {
                member_of_types,
                attributes,
                has_tags,
            },
        );
    }

    fn parse_common(&mut self, name: &str, content: &str) {
        let attributes = self.parse_attributes(content);

        self.common_types.insert(
            name.to_string(),
            CedarCommonType {
                attributes,
            },
        );
    }

    fn collect_all_types(&mut self, content: &str) {
        for line in content.lines() {
            let line = line.trim();
            if line.starts_with("message ") {
                let parts: Vec<&str> = line.split_whitespace().collect();
                if parts.len() >= 2 {
                    self.all_known_types.insert(parts[1].to_string());
                }
            }
        }
    }

    fn resolve_action_inheritance(&mut self) {
        let mut resolved_actions = HashMap::new();

        for (name, action) in &self.actions {
            let mut resolved_action = action.clone();
            
            // Check if this action has a base action
            if let Some(base_name) = self.action_inheritance.get(name) {
                if let Some(base) = self.base_actions.get(base_name) {
                    resolved_action.resource_types = base.resource_types.clone();
                    resolved_action.principal_types = base.principal_types.clone();
                    resolved_action.context_type = base.context_type.clone();
                }
            }

            resolved_actions.insert(name.clone(), resolved_action);
        }

        self.actions = resolved_actions;
    }

    fn parse(&mut self, content: &str) {
        // First pass: collect all type names
        self.collect_all_types(content);

        // Second pass: parse all messages
        let mut pos = 0;
        while let Some(msg_start) = content[pos..].find("\nmessage ") {
            let actual_pos = pos + msg_start + 1;
            if let Some((name, message_content, end_pos)) = self.extract_message(content, actual_pos) {
                let category = self.extract_category(&message_content);

                match category {
                    TypeCategory::EntityType => self.parse_entity(&name, &message_content),
                    TypeCategory::ActionType => self.parse_action(&name, &message_content),
                    TypeCategory::CommonType => self.parse_common(&name, &message_content),
                }

                pos = end_pos;
            } else {
                break;
            }
        }

        // Resolve action inheritance
        self.resolve_action_inheritance();
    }

    fn to_json(&self) -> Value {
        let mut common_types = serde_json::Map::new();
        for (name, ct) in &self.common_types {
            common_types.insert(name.clone(), ct.to_json());
        }

        let mut entity_types = serde_json::Map::new();
        for (name, et) in &self.entity_types {
            entity_types.insert(name.clone(), et.to_json());
        }

        let mut actions = serde_json::Map::new();
        for (name, action) in &self.actions {
            actions.insert(name.clone(), action.to_json());
        }

        json!({
            "Jans": {
                "commonTypes": common_types,
                "entityTypes": entity_types,
                "actions": actions
            }
        })
    }
}

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let args: Vec<String> = std::env::args().collect();

    if args.len() < 2 || args.len() > 3 {
        eprintln!("Usage: {} <input_directory_or_file> [output.json]", args[0]);
        eprintln!("  If input is a directory, all .proto files will be processed");
        eprintln!("  If input is a file, only that file will be processed");
        std::process::exit(1);
    }

    let input_path = &args[1];
    let output_file = if args.len() == 3 {
        &args[2]
    } else {
        "cedar_schema.json"
    };

    // Determine if input is directory or file
    let path = Path::new(input_path);
    let mut proto_files = Vec::new();

    if path.is_dir() {
        // Find all .proto files in directory
        for entry in fs::read_dir(path)? {
            let entry = entry?;
            let file_path = entry.path();
            if file_path.extension().and_then(|s| s.to_str()) == Some("proto") {
                proto_files.push(file_path);
            }
        }
        proto_files.sort();

        if proto_files.is_empty() {
            eprintln!("Error: No .proto files found in directory: {}", input_path);
            std::process::exit(1);
        }

        println!("Found {} proto files:", proto_files.len());
        for file in &proto_files {
            println!("  - {}", file.file_name().unwrap().to_str().unwrap());
        }
    } else if path.is_file() {
        proto_files.push(path.to_path_buf());
    } else {
        eprintln!("Error: Input path does not exist: {}", input_path);
        std::process::exit(1);
    }

    // Read and concatenate all protobuf files
    let mut combined_content = String::new();
    for (i, proto_file) in proto_files.iter().enumerate() {
        println!("\nReading {}...", proto_file.file_name().unwrap().to_str().unwrap());
        let mut content = fs::read_to_string(proto_file)?;

        // Skip syntax and package declarations after the first file
        if i > 0 {
            let lines: Vec<&str> = content.lines().collect();
            let filtered_lines: Vec<&str> = lines
                .into_iter()
                .filter(|line| {
                    let trimmed = line.trim();
                    !trimmed.starts_with("syntax =") && !trimmed.starts_with("package ")
                })
                .collect();
            content = filtered_lines.join("\n");
        }

        combined_content.push_str(&content);
        combined_content.push_str("\n\n");
    }

    // Convert
    println!("\nConverting to Cedar schema...");
    let mut converter = ProtobufToCedarConverter::new();
    converter.parse(&combined_content);

    // Generate JSON
    let schema = converter.to_json();
    let pretty_json = serde_json::to_string_pretty(&schema)?;

    // Write to file
    fs::write(output_file, pretty_json)?;
    println!("Cedar schema saved to {}", output_file);

    // Print summary
    println!("\nConversion summary:");
    println!("  Common types: {}", converter.common_types.len());
    println!("  Entity types: {}", converter.entity_types.len());
    println!("  Actions: {}", converter.actions.len());
    println!("  Base actions: {}", converter.base_actions.len());

    // Show entity types with inheritance
    let entities_with_inheritance: Vec<_> = converter
        .entity_types
        .iter()
        .filter(|(_, e)| !e.member_of_types.is_empty())
        .collect();

    if !entities_with_inheritance.is_empty() {
        println!("\nEntity types with inheritance:");
        for (name, entity) in entities_with_inheritance {
            println!("  {} -> memberOfTypes: {:?}", name, entity.member_of_types);
        }
    }

    // Show actions grouped by resource type
    println!("\nActions by resource type:");
    let mut actions_by_resource: HashMap<String, Vec<String>> = HashMap::new();
    for (name, action) in &converter.actions {
        for resource in &action.resource_types {
            actions_by_resource
                .entry(resource.clone())
                .or_insert_with(Vec::new)
                .push(name.clone());
        }
    }

    let mut sorted_resources: Vec<_> = actions_by_resource.keys().collect();
    sorted_resources.sort();

    for resource in sorted_resources {
        println!("  {}:", resource);
        let mut action_names = actions_by_resource[resource].clone();
        action_names.sort();
        for action_name in action_names {
            let action = &converter.actions[&action_name];
            let principals = action.principal_types.join(", ");
            println!("    - {} (principals: {})", action_name, principals);
        }
    }

    Ok(())
}
