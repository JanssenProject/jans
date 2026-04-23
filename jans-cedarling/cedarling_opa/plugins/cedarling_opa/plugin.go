package cedarlingopa

import (
	"context"
	"errors"
	"fmt"
	"maps"
	"os"
	"sync"

	"github.com/JanssenProject/jans/jans-cedarling/bindings/cedarling_go"
	"github.com/open-policy-agent/opa/v1/plugins"
	"github.com/open-policy-agent/opa/v1/util"
)

// #cgo LDFLAGS: -L${SRCDIR} -lcedarling_go
import "C"

const PluginName = "cedarling_opa"

type Config struct {
	BootstrapConfig map[string]any `json:"bootstrap_config"`
	Stderr          bool           `json:"stderr"` // false => stdout, true => stderr
}

type CedarPlugin struct {
	manager *plugins.Manager
	mtx     sync.RWMutex
	config  Config
	cedar   *cedarling_go.Cedarling
}

var globalInstanceMu sync.RWMutex
var globalInstance *CedarPlugin

func setGlobalInstance(p *CedarPlugin) {
	globalInstanceMu.Lock()
	defer globalInstanceMu.Unlock()
	globalInstance = p
}

func clearGlobalInstance(p *CedarPlugin) {
	globalInstanceMu.Lock()
	defer globalInstanceMu.Unlock()
	if globalInstance == p {
		globalInstance = nil
	}
}

func GetCedarlingInstance() (*cedarling_go.Cedarling, func()) {
	globalInstanceMu.RLock()
	p := globalInstance
	globalInstanceMu.RUnlock()
	if p == nil {
		return nil, func() {}
	}
	p.mtx.RLock()
	if p.cedar == nil {
		p.mtx.RUnlock()
		return nil, func() {}
	}
	return p.cedar, p.mtx.RUnlock
}

func logMessage(stderr bool, msg string) {
	if stderr {
		fmt.Fprintln(os.Stderr, msg)
	} else {
		fmt.Println(msg)
	}
}

func buildBootstrapConfig(cfg Config) (map[string]any, error) {
	newConfig := make(map[string]any)
	maps.Copy(newConfig, cfg.BootstrapConfig)
	return newConfig, nil
}

func (p *CedarPlugin) Start(ctx context.Context) error {
	p.mtx.Lock()
	defer p.mtx.Unlock()
	new_config, err := buildBootstrapConfig(p.config)
	if err != nil {
		p.manager.UpdatePluginStatus(PluginName, &plugins.Status{State: plugins.StateErr})
		return err
	}
	var stderr bool = p.config.Stderr
	logMessage(stderr, "Initializing Cedarling")
	instance, err := cedarling_go.NewCedarling(new_config)
	if err != nil {
		p.manager.UpdatePluginStatus(PluginName, &plugins.Status{State: plugins.StateErr})
		return err
	}
	p.cedar = instance
	p.manager.UpdatePluginStatus(PluginName, &plugins.Status{State: plugins.StateOK})
	setGlobalInstance(p)
	return nil
}

func (p *CedarPlugin) Stop(ctx context.Context) {
	p.mtx.Lock()
	cedar := p.cedar
	p.cedar = nil
	p.manager.UpdatePluginStatus(PluginName, &plugins.Status{State: plugins.StateNotReady})
	clearGlobalInstance(p)
	p.mtx.Unlock()
	if cedar != nil {
		cedar.ShutDown()
	}
}

func (p *CedarPlugin) Reconfigure(ctx context.Context, config interface{}) {
	cfg := config.(Config)
	new_config, err := buildBootstrapConfig(cfg)
	if err != nil {
		fmt.Fprintln(os.Stderr, err)
		p.manager.UpdatePluginStatus(PluginName, &plugins.Status{State: plugins.StateErr})
		return
	}
	var stderr bool = cfg.Stderr
	logMessage(stderr, "Initializing Cedarling")
	new_instance, err := cedarling_go.NewCedarling(new_config)
	if err != nil {
		fmt.Fprintln(os.Stderr, err)
		p.manager.UpdatePluginStatus(PluginName, &plugins.Status{State: plugins.StateErr})
		return
	}
	p.mtx.Lock()
	old_cedar := p.cedar
	p.config = cfg
	p.cedar = new_instance
	p.manager.UpdatePluginStatus(PluginName, &plugins.Status{State: plugins.StateOK})
	setGlobalInstance(p)
	p.mtx.Unlock()
	if old_cedar != nil {
		old_cedar.ShutDown()
	}
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
	err := util.Unmarshal(config, &parsedConfig)
	if err != nil {
		return nil, err
	}
	if parsedConfig.BootstrapConfig == nil {
		return nil, errors.New("Bootstrap config is required")
	}
	return parsedConfig, nil
}
