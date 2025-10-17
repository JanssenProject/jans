use std::fs;
use std::io::{self};
use std::path::Path;

/// Loads the contents of a Cedar schema file from disk.
pub fn load_schema<P: AsRef<Path>>(path: P) -> Result<String, io::Error> {
    fs::read_to_string(path)
}

/// Performs basic validation on the schema's content
pub fn validate_schema(schema_content: &str) -> Result<(), String> {
    if schema_content.contains("entity_types") {
        Ok(())
    } else {
        Err("Missing 'entity_types' key in Cedar schema.".to_string())
    }
}

/// Formats error messages with file info
pub fn report_error(err: &str, file: &str) -> String {
    format!("Schema validation error in '{}': {}", file, err)
}


#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_valid_schema() {
        let schema = r#"{"entity_types": ["User"]}"#;
        assert!(validate_schema(schema).is_ok());
    }
    #[test]
    fn test_invalid_schema() {
        let schema = r#"{"not_entity": ["User"]}"#;
        assert!(validate_schema(schema).is_err());
    }
}
