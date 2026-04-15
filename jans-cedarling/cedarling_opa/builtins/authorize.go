package builtins

import (
	"github.com/open-policy-agent/opa/v1/ast"
	"github.com/open-policy-agent/opa/v1/rego"
)

var authorizeBuiltinDecl = &rego.Function{}

func authorizeBuiltinImpl(bctx rego.BuiltinContext, input *ast.Term) (*ast.Term, error) {
	return nil, nil
}
