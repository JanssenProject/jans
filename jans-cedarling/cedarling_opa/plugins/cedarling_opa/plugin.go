package cedarlingopa

import (
	"context"
	"encoding/json"
	"fmt"
	"sync"

	"github.com/JanssenProject/jans/jans-cedarling/bindings/cedarling_go"
	"github.com/open-policy-agent/opa/v1/plugins"
	"github.com/open-policy-agent/opa/v1/util"
)

// #cgo LDFLAGS: -L. -lcedarling_go
import "C"

var bootstrapConfig string = `
{
        "CEDARLING_APPLICATION_NAME": "TestApp",
        "CEDARLING_USER_AUTHZ": "enabled",
        "CEDARLING_WORKLOAD_AUTHZ": "enabled",
        "CEDARLING_JWT_SIG_VALIDATION": "disabled",
        "CEDARLING_JWT_STATUS_VALIDATION": "disabled",
        "CEDARLING_ID_TOKEN_TRUST_MODE": "never",
        "CEDARLING_LOG_TYPE": "std_out",
        "CEDARLING_LOG_TTL": 60,
        "CEDARLING_LOG_LEVEL": "DEBUG",
        "CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED": ["HS256"]
}
`

const PluginName = "cedarling_opa"

type Config struct {
	BootstrapConfig map[string]any `json:"bootstrap_config"`
	Stderr          bool           `json:"stderr"` // false => stdout, true => stderr
}

type CedarPlugin struct {
	manager *plugins.Manager
	mtx     sync.Mutex
	config  Config
	cedar   *cedarling_go.Cedarling
}

func (p *CedarPlugin) Start(ctx context.Context) error {
	p.mtx.Lock()
	defer p.mtx.Unlock()
	policyStoreJsonPath := "../test_files/policy-store_ok.yaml"
	var config map[string]any = p.config.BootstrapConfig
	config["CEDARLING_POLICY_STORE_LOCAL_FN"] = policyStoreJsonPath
	if err := json.Unmarshal([]byte(bootstrapConfig), &config); err != nil {
		return err
	}
	fmt.Println("Initializing cedarling")
	instance, err := cedarling_go.NewCedarling(config)
	if err != nil {
		p.manager.UpdatePluginStatus(PluginName, &plugins.Status{State: plugins.StateErr})
		return err
	}
	p.cedar = instance
	p.manager.UpdatePluginStatus(PluginName, &plugins.Status{State: plugins.StateOK})
	return nil
}

func (p *CedarPlugin) Stop(ctx context.Context) {
	p.mtx.Lock()
	defer p.mtx.Unlock()
	if p.cedar != nil {
		p.cedar.ShutDown()
	}
	p.manager.UpdatePluginStatus(PluginName, &plugins.Status{State: plugins.StateNotReady})
}

func (p *CedarPlugin) Reconfigure(ctx context.Context, config interface{}) {
	p.mtx.Lock()
	defer p.mtx.Unlock()
	p.config = config.(Config)
}

type Factory struct{}

func (Factory) New(m *plugins.Manager, config interface{}) plugins.Plugin {

	m.UpdatePluginStatus(PluginName, &plugins.Status{State: plugins.StateNotReady})

	return &CedarPlugin{
		manager: m,
		config:  config.(Config),
	}
}

func (Factory) Validate(_ *plugins.Manager, config []byte) (interface{}, error) {
	parsedConfig := Config{}
	return parsedConfig, util.Unmarshal(config, &parsedConfig)
}
