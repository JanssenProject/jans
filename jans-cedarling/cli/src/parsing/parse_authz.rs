use regex::Regex;

pub const FN_AUTHZ_REGEX_SRC: &str = r"^authz\(([\w\s,]+)\)$";

pub fn parse_authz(input: &str, fn_regex: &Regex, var_regex: &Regex) -> Option<Vec<String>> {
    if let Some(captures) = fn_regex.captures(input) {
        // capture the group inside the parentheses
        if let Some(vars) = captures.get(1) {
            let vars_str = vars.as_str();

            let vars_vec: Vec<String> = vars_str.split(',').map(|s| s.trim().to_string()).collect();

            if vars_vec.is_empty()
                || vars_vec
                    .iter()
                    .any(|s| s.is_empty() || !var_regex.is_match(s))
            {
                return None;
            }

            return Some(vars_vec);
        }
    }
    None
}

#[cfg(test)]
mod tests {
    use crate::parsing::VAR_NAME_REGEX_SRC;

    use super::*;
    use regex::Regex;

    #[test]
    fn test_parse_authz_valid_cases() {
        let fn_regex = Regex::new(FN_AUTHZ_REGEX_SRC).unwrap();
        let var_regex = Regex::new(VAR_NAME_REGEX_SRC).unwrap();

        assert_eq!(
            parse_authz("authz(var1, var2, var3)", &fn_regex, &var_regex),
            Some(vec![
                "var1".to_string(),
                "var2".to_string(),
                "var3".to_string()
            ])
        );

        assert_eq!(
            parse_authz(
                "authz(var_name_1, var2, var_3, _var4, __var5)",
                &fn_regex,
                &var_regex
            ),
            Some(vec![
                "var_name_1".to_string(),
                "var2".to_string(),
                "var_3".to_string(),
                "_var4".to_string(),
                "__var5".to_string(),
            ])
        );

        assert_eq!(
            parse_authz("authz(single_var)", &fn_regex, &var_regex),
            Some(vec!["single_var".to_string()])
        );

        assert_eq!(
            parse_authz("authz(var1,    var2,   var3)", &fn_regex, &var_regex),
            Some(vec![
                "var1".to_string(),
                "var2".to_string(),
                "var3".to_string()
            ])
        );
    }

    #[test]
    fn test_parse_authz_invalid_cases() {
        let fn_regex = Regex::new(FN_AUTHZ_REGEX_SRC).unwrap();
        let var_regex = Regex::new(VAR_NAME_REGEX_SRC).unwrap();

        // invalid format cases
        let invalid_fmt_cases = [
            ("authz()", &fn_regex, &var_regex),
            ("authz(var1, , var3)", &fn_regex, &var_regex),
            ("authz(var1,,var3)", &fn_regex, &var_regex),
            ("auth(var1, var2, var3)", &fn_regex, &var_regex),
            ("authz(var1 var2, var3)", &fn_regex, &var_regex),
        ];
        invalid_fmt_cases
            .iter()
            .for_each(|args| assert_eq!(parse_authz(args.0, args.1, args.2), None));

        let invalid_var_name_cases = [
            ("authz(1var, var2)", &fn_regex, &var_regex),
            ("authz(var1, !var2)", &fn_regex, &var_regex),
            ("authz(var1, var 2)", &fn_regex, &var_regex),
        ];
        invalid_var_name_cases
            .iter()
            .for_each(|args| assert_eq!(parse_authz(args.0, args.1, args.2), None));

        let completely_malformed_cases = [
            ("authz()", &fn_regex, &var_regex),
            ("authz(   )", &fn_regex, &var_regex),
            ("authz)", &fn_regex, &var_regex),
            ("authz(", &fn_regex, &var_regex),
            ("authz(var1", &fn_regex, &var_regex),
            ("var1, var2, var3", &fn_regex, &var_regex),
            ("", &fn_regex, &var_regex),
        ];
        completely_malformed_cases
            .iter()
            .for_each(|args| assert_eq!(parse_authz(args.0, args.1, args.2), None));
    }
}
