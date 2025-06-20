package provider

import (
	"fmt"
	"regexp"
	"testing"

	"github.com/hashicorp/go-cty/cty"
	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
)

func validateEmail(email string, path cty.Path) diag.Diagnostics {
	var diags diag.Diagnostics
	emailRegex := regexp.MustCompile(`^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$`)
	if !emailRegex.MatchString(email) {
		diags = append(diags, diag.Diagnostic{
			Severity: diag.Error,
			Summary:  "Invalid email format",
			Detail:   fmt.Sprintf("The email address %q is not a valid format.", email),
		})
	}
	return diags
}

func TestURLValidator(t *testing.T) {

	diagnostics := validateURL("https://accounts.google.com", cty.Path{})
	if len(diagnostics) != 0 {
		t.Errorf("expected no errors, got %v", diagnostics)
	}

	diagnostics = validateURL("http-accounts-google-com", cty.Path{})
	if len(diagnostics) == 0 {
		t.Error("expected an error, got none")
	}

	diagnostics = validateURL("42", cty.Path{})
	if len(diagnostics) == 0 {
		t.Error("expected an error, got none")
	}
}

func TestEnumValidator(t *testing.T) {

	enums := []string{"code", "token", "id_token"}

	diagnostics := validateEnum("code", enums)
	if len(diagnostics) != 0 {
		t.Errorf("expected no errors, got %v", diagnostics)
	}

	diagnostics = validateEnum("foo", enums)
	if len(diagnostics) == 0 {
		t.Error("expected an error, got none")
	}

	diagnostics = validateEnum("42", enums)
	if len(diagnostics) == 0 {
		t.Error("expected an error, got none")
	}

}

func TestRegex(t *testing.T) {

	// wrong value type
	diagnostics := validateRegex("42", "^[a-zA-Z0-9]{3,}$")
	if len(diagnostics) == 0 {
		t.Error("expected an error, got none")
	}

	// wrong regex
	diagnostics = validateRegex("foo", "^[a-zA-Z0-9{3,}$")
	if len(diagnostics) == 0 {
		t.Error("expected an error, got none")
	}

	// incorrect value
	diagnostics = validateRegex("foo", "^[a-zA-Z0-9]{5,}$")
	if len(diagnostics) == 0 {
		t.Error("expected an error, got none")
	}

	// correct value
	diagnostics = validateRegex("foo", "^[a-zA-Z0-9]{3,}$")
	if len(diagnostics) != 0 {
		t.Errorf("get an unexpected error: %v", diagnostics[0].Summary)
	}

}

func TestValidateURL(t *testing.T) {
	tests := []struct {
		name     string
		value    string
		expected bool
	}{
		{"valid http url", "http://example.com", true},
		{"valid https url", "https://example.com", true},
		{"invalid url", "not-a-url", false},
		{"empty string", "", false},
	}

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			diags := validateURL(test.value, cty.Path{})
			hasError := len(diags) > 0 && diags[0].Severity == diag.Error

			if test.expected && hasError {
				t.Errorf("expected %s to be valid, but got error: %v", test.value, diags)
			}
			if !test.expected && !hasError {
				t.Errorf("expected %s to be invalid, but got no error", test.value)
			}
		})
	}
}

func TestValidateEmail(t *testing.T) {
	tests := []struct {
		name     string
		value    string
		expected bool
	}{
		{"valid email", "test@example.com", true},
		{"invalid email", "not-an-email", false},
		{"empty string", "", false},
	}

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			diags := validateEmail(test.value, cty.Path{})
			hasError := len(diags) > 0 && diags[0].Severity == diag.Error

			if test.expected && hasError {
				t.Errorf("expected %s to be valid, but got error: %v", test.value, diags)
			}
			if !test.expected && !hasError {
				t.Errorf("expected %s to be invalid, but got no error", test.value)
			}
		})
	}
}
