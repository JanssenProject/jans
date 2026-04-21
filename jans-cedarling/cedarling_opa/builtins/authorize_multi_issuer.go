package builtins

import (
	"fmt"

	"github.com/JanssenProject/jans/jans-cedarling/bindings/cedarling_go"
	cedarlingopa "github.com/JanssenProject/jans/jans-cedarling/cedarling_opa/plugins/cedarling_opa"
	"github.com/open-policy-agent/opa/v1/ast"
	"github.com/open-policy-agent/opa/v1/rego"
	"github.com/open-policy-agent/opa/v1/types"
)

var authorizeBuiltinDecl = &rego.Function{
	Name: "cedarling.opa.authorize_multi_issuer",
	Decl: types.NewFunction(
		types.Args(types.A),
		types.A,
	)}

func authorizeBuiltinImpl(bctx rego.BuiltinContext, input *ast.Term) (*ast.Term, error) {
	var in cedarling_go.AuthorizeMultiIssuerRequest
	if err := ast.As(input.Value, &in); err != nil {
		return nil, err
	}
	instance, release := cedarlingopa.GetCedarlingInstance()
	if instance == nil {
		return errorAsResult(fmt.Errorf("Cedarling uninitialized"))
	}
	result, err := instance.AuthorizeMultiIssuer(in)
	if err != nil {
		return errorAsResult(fmt.Errorf("Authorize failed: %w", err))
	}
	defer release()
	reasons := result.Response.Reason()
	if reasons == nil {
		reasons = []string{}
	}
	errors := result.Response.Errors()
	if errors == nil {
		errors = []string{}
	}
	output := buildAuthzOutput(result.Decision, result.Response.Reason(), result.Response.Errors(), result.RequestID)
	return_value, err := ast.InterfaceToValue(output)
	if err != nil {
		return errorAsResult(fmt.Errorf("Error in converting return value: %w", err))
	}
	return ast.NewTerm(return_value), nil
}
