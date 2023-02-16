package provider

import (
	"fmt"
	"net/url"
	"regexp"
	"strings"

	"github.com/hashicorp/go-cty/cty"
	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
)

// validateURL validates that the given value is a valid URL.
func validateURL(v any, p cty.Path) diag.Diagnostics {

	var diags diag.Diagnostics

	value, ok := v.(string)
	if !ok {
		diag := diag.Diagnostic{
			Severity: diag.Error,
			Summary:  "wrong value",
			Detail:   fmt.Sprintf("%q is not of type string", value),
		}
		diags = append(diags, diag)
	}

	_, err := url.ParseRequestURI(value)
	if err != nil {
		diag := diag.Diagnostic{
			Severity: diag.Error,
			Summary:  "wrong value",
			Detail:   fmt.Sprintf("%q is not a valid URL", value),
		}
		diags = append(diags, diag)
	}

	return diags
}

// validateEnum checks that the given string matches one of the given allowed values.
func validateEnum(value any, allowed []string) diag.Diagnostics {
	var diags diag.Diagnostics

	value, ok := value.(string)
	if !ok {
		diag := diag.Diagnostic{
			Severity: diag.Error,
			Summary:  "wrong value",
			Detail:   fmt.Sprintf("%q is not of type string", value),
		}
		diags = append(diags, diag)
	}

	for _, v := range allowed {
		if value == v {
			return diags
		}
	}

	diag := diag.Diagnostic{
		Severity: diag.Error,
		Summary:  "wrong value",
		Detail:   fmt.Sprintf("%q is not a valid value. Valid options are: %v.", value, strings.Join(allowed, ", ")),
	}
	diags = append(diags, diag)

	return diags
}

// validateRegex checks if the provided value matches the given regex pattern
func validateRegex(value any, regexPattern string) diag.Diagnostics {
	var diags diag.Diagnostics

	val, ok := value.(string)
	if !ok {
		diag := diag.Diagnostic{
			Severity: diag.Error,
			Summary:  "wrong value",
			Detail:   fmt.Sprintf("%q is not of type string", value),
		}
		diags = append(diags, diag)

		return diags
	}

	// compile the regex
	regex, err := regexp.Compile(regexPattern)
	if err != nil {
		diag := diag.Diagnostic{
			Severity: diag.Error,
			Summary:  "wrong valiation rule",
			Detail:   fmt.Sprintf("%q is not a valid regex pattern", regexPattern),
		}
		diags = append(diags, diag)

		return diags
	}

	// check the regex
	if !regex.MatchString(val) {
		diag := diag.Diagnostic{
			Severity: diag.Error,
			Summary:  "wrong value",
			Detail:   fmt.Sprintf("%q is not a valid value", value),
		}

		diags = append(diags, diag)
	}

	return diags
}
