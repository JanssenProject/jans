package builtins

import (
	"fmt"

	"github.com/JanssenProject/jans/jans-cedarling/bindings/cedarling_go"
	cedarlingopa "github.com/JanssenProject/jans/jans-cedarling/cedarling_opa/plugins/cedarling_opa"
	"github.com/open-policy-agent/opa/v1/ast"
	"github.com/open-policy-agent/opa/v1/rego"
	"github.com/open-policy-agent/opa/v1/types"
)

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

var authorizeUnsignedBuiltinDecl = &rego.Function{
	Name: "cedarling_opa.authorize_unsigned",
	Decl: types.NewFunction(
		types.Args(types.A),
		types.A,
	),
}

func authorizeUnsignedBuiltinImpl(bctx rego.BuiltinContext, input *ast.Term) (*ast.Term, error) {
	var in cedarling_go.RequestUnsigned
	if err := ast.As(input.Value, &in); err != nil {
		return nil, err
	}
	instance := cedarlingopa.GetCedarlingInstance()
	if instance == nil {
		return errorAsResult(fmt.Errorf("Cedarling uninitialized"))
	}
	result, err := instance.AuthorizeUnsigned(in)
	if err != nil {
		return errorAsResult(fmt.Errorf("Authorize_unsigned failed: %w", err))
	}
	reasonMap := map[string][]string{}
	errorsMap := map[string][]string{}

	for key, value := range result.Principals {
		reasons := value.Reason()
		errors := value.Errors()
		if reasons == nil {
			reasons = []string{}
		}
		if errors == nil {
			errors = []string{}
		}
		reasonMap[key] = reasons
		errorsMap[key] = errors
	}
	output := map[string]any{
		"decision":   result.Decision,
		"reasons":    reasonMap,
		"errors":     errorsMap,
		"request_id": result.RequestID,
	}
	return_value, err := ast.InterfaceToValue(output)
	if err != nil {
		return errorAsResult(fmt.Errorf("Error in converting return value: %w", err))
	}
	return ast.NewTerm(return_value), nil
}
