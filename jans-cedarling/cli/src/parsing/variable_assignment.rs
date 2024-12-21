use regex::Regex;

pub const VAR_NAME_REGEX_SRC: &str = r"^[a-zA-Z_][a-zA-Z0-9_]*$";

pub fn parse_variable_assignment(input: &str, regex: &Regex) -> Option<(String, String)> {
    let parts: Vec<&str> = input.splitn(2, '=').map(|s| s.trim()).collect();

    if parts.len() == 2 {
        let var_name = parts[0];
        let value = parts[1];

        if regex.is_match(var_name) {
            return Some((var_name.to_string(), value.to_string()));
        }
    }
    None
}

#[cfg(test)]
mod tests {
    use super::*;
    use regex::Regex;

    #[test]
    fn test_parse_variable_assignment() {
        let regex = Regex::new(VAR_NAME_REGEX_SRC).unwrap();

        let test_cases = vec![
            (
                "var_name2 = value",
                Some(("var_name2".to_string(), "value".to_string())),
                "should successfully parse a valid variable assignment with alphanumeric variable name",
            ),
            (
                "2var_name = value",
                None,
                "should fail parsing variable assignment because variable name starts with a digit",
            ),
            (
                "var_name! = value",
                None,
                "should fail parsing variable assignment because variable name contains invalid character",
            ),
            (
                "<><> = value",
                None,
                "should fail parsing variable assignment because variable name contains invalid characters",
            ),
            (
                "var_name =",
                Some(("var_name".to_string(), "".to_string())),
                "should parse a variable assignment with an empty value",
            ),
            (
                "var_name = value with spaces",
                Some(("var_name".to_string(), "value with spaces".to_string())),
                "should parse a variable assignment where value contains spaces",
            ),
            (
                "var_name=",
                Some(("var_name".to_string(), "".to_string())),
                "should parse a variable assignment with no space before the equals sign and an empty value",
            ),
            (
                "var_name=value",
                Some(("var_name".to_string(), "value".to_string())),
                "should parse a variable assignment with no spaces around the equals sign",
            ),
            (
                "var_name",
                None,
                "should fail parsing because there is no equals sign or value",
            ),
            (
                "var_name = value = extra",
                Some(("var_name".to_string(), "value = extra".to_string())),
                "should parse a variable assignment where the value contains an equals sign",
            ),
        ];

        for (input, expected, description) in test_cases {
            assert_eq!(
                parse_variable_assignment(input, &regex),
                expected,
                "{}: input='{}', expected={:?}",
                description,
                input,
                expected
            );
        }
    }
}
