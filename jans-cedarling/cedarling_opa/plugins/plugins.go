package plugins

import (
	"github.com/JanssenProject/jans/jans-cedarling/cedarling_opa/plugins/cedarling_opa"
	"github.com/open-policy-agent/opa/v1/runtime"
)

func Register() {
	runtime.RegisterPlugin(cedarlingopa.PluginName, cedarlingopa.Factory{})
}
