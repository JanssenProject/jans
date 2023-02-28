package provider

import "testing"

func TestURLValidator(t *testing.T) {

	diagnostics := validateURL("https://accounts.google.com", nil)
	if len(diagnostics) != 0 {
		t.Errorf("expected no errors, got %v", diagnostics)
	}

	diagnostics = validateURL("http-accounts-google-com", nil)
	if len(diagnostics) == 0 {
		t.Error("expected an error, got none")
	}

	diagnostics = validateURL(42, nil)
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

	diagnostics = validateEnum(42, enums)
	if len(diagnostics) == 0 {
		t.Error("expected an error, got none")
	}

}

func TestRegex(t *testing.T) {

	// wrong value type
	diagnostics := validateRegex(42, "^[a-zA-Z0-9]{3,}$")
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
