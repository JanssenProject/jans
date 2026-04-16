package builtins

import "github.com/open-policy-agent/opa/v1/ast"

func errorAsResult(err error) (*ast.Term, error) {
	output := map[string]any{
		"decision": false,
		"reason":   map[string][]string{},
		"errors": map[string][]string{
			"builtin": {err.Error()},
		},
		"request_id": "",
	}

	val, _ := ast.InterfaceToValue(output)
	return ast.NewTerm(val), nil
}
