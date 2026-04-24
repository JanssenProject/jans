package builtins

import (
	"github.com/open-policy-agent/opa/v1/rego"
)

func Register() {
	rego.RegisterBuiltin1(authorizeUnsignedBuiltinDecl, authorizeUnsignedBuiltinImpl)
	rego.RegisterBuiltin1(authorizeBuiltinDecl, authorizeBuiltinImpl)
}
