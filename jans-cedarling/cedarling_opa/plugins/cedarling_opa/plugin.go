package cedarlingopa

import (
	"context"
	"encoding/json"
	"fmt"
	"github.com/JanssenProject/jans/jans-cedarling/bindings/cedarling_go"
	"github.com/open-policy-agent/opa/v1/plugins"
	"github.com/open-policy-agent/opa/v1/util"
	"sync"
)

// #cgo LDFLAGS: -L. -lcedarling_go
import "C"

const PluginName = "cedarling_opa"

type Config struct {
	BootstrapConfig map[string]any `json:"bootstrap_config"`
	Stderr          bool           `json:"stderr"` // false => stdout, true => stderr
	PolicyStore     map[string]any `json:"policy_store"`
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
	//policyStoreJsonPath := "../test_files/policy-store_ok.yaml"
	var config map[string]any = p.config.BootstrapConfig
	var policy_store map[string]any = p.config.PolicyStore
	policy_store_bytes, err := json.Marshal(policy_store)
	if err != nil {
		return err
	}
	config["CEDARLING_POLICY_STORE_LOCAL"] = string(policy_store_bytes)
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
