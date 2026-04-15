package builtins

import (
	"fmt"
	"os"

	"github.com/JanssenProject/jans/jans-cedarling/bindings/cedarling_go"
	cedarlingopa "github.com/JanssenProject/jans/jans-cedarling/cedarling_opa/plugins/cedarling_opa"
	"github.com/open-policy-agent/opa/v1/ast"
	"github.com/open-policy-agent/opa/v1/rego"
	"github.com/open-policy-agent/opa/v1/types"
)

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
		return nil, fmt.Errorf("Cedarling not initialized; please check plugin status")
	}
	result, err := instance.AuthorizeUnsigned(in)
	if err != nil {
		fmt.Fprintf(os.Stderr, "DEBUG: %v\n", err)
		return nil, err
	}
	return_value := ast.NewObject()
	return_value.Insert(ast.StringTerm("decision"), ast.BooleanTerm(result.Decision))
	fmt.Fprintf(os.Stderr, "DEBUG: %v\n", result.Principals[in.Principals[0].CedarMapping.ID].Reason())
	return ast.NewTerm(return_value), nil
}
