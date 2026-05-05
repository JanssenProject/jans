package builtins

import (
	"github.com/open-policy-agent/opa/v1/ast"
)

func errorAsResult(err error) (*ast.Term, error) {
	output := map[string]any{
		"decision":   false,
		"reasons":    []string{},
		"errors":     []string{err.Error()},
		"request_id": "",
	}

	val, _ := ast.InterfaceToValue(output)
	return ast.NewTerm(val), nil
}

func buildAuthzOutput(decision bool, reasons []string, errors []string, requestID string) map[string]any {
	if reasons == nil {
		reasons = []string{}
	}
	if errors == nil {
		errors = []string{}
	}
	output := map[string]any{
		"decision":   decision,
		"reasons":    reasons,
		"errors":     errors,
		"request_id": requestID,
	}
	return output
}
