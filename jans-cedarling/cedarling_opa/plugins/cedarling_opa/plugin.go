/*
Lock Ordering (CRITICAL):

1. globalInstanceMu (R/W) -> CedarPlugin.mtx (R/W)
   - Always acquire globalInstanceMu BEFORE CedarPlugin.mtx
   - Never hold CedarPlugin.mtx while trying to acquire globalInstanceMu

2. CedarPlugin.mtx protects:
   - p.cedar (*cedarling_go.Cedarling)
   - p.config (Config)

3. globalInstanceMu protects:
   - globalInstance (*CedarPlugin)

WithCedarlingInstance follows this order:
   globalInstanceMu.RLock() -> get p -> globalInstanceMu.RUnlock() → p.mtx.RLock()
*/

package cedarlingopa

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"maps"
	"sync"

	"github.com/JanssenProject/jans/jans-cedarling/bindings/cedarling_go"
	"github.com/open-policy-agent/opa/v1/logging"
	"github.com/open-policy-agent/opa/v1/plugins"
	"github.com/open-policy-agent/opa/v1/util"
)

// #cgo LDFLAGS: -L${SRCDIR} -lcedarling_go
import "C"

const PluginName = "cedarling_opa"

type Config struct {
	BootstrapConfig map[string]any `json:"bootstrap_config"`
	Host            string         `json:"host"`
}

type CedarPlugin struct {
	manager *plugins.Manager
	mtx     sync.RWMutex
	config  Config
	cedar   Cedarling
	logger  logging.Logger
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

func WithCedarlingInstance(fn func(Cedarling) error) error {
	globalInstanceMu.RLock()
	p := globalInstance
	globalInstanceMu.RUnlock()
	if p == nil {
		return errors.New("Cedarling plugin not initialized")
	}
	p.mtx.RLock()
	defer p.mtx.RUnlock()
	if p.cedar == nil {
		return errors.New("Cedarling instance not ready")
	}
	return fn(p.cedar)
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
	p.logger.Info("Initializing Cedarling")
	instance, err := cedarling_go.NewCedarling(new_config)
	if err != nil {
		p.manager.UpdatePluginStatus(PluginName, &plugins.Status{State: plugins.StateErr})
		return err
	}
	p.cedar = Cedarling(instance)
	p.manager.ExtraRoute("/.well-known/authzen-configuration", "metadata", p.MetaDataHandler)
	p.manager.ExtraRoute("/access/v1/evaluation", "evaluation", p.AccessEvaluationHandler)
	p.manager.ExtraRoute("/access/v1/evaluations", "evaluations", p.AccessEvaluationsHandler)
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

func (p *CedarPlugin) Reconfigure(ctx context.Context, config any) {
	cfg := config.(Config)
	new_config, err := buildBootstrapConfig(cfg)
	if err != nil {
		p.logger.Info(err.Error())
		p.manager.UpdatePluginStatus(PluginName, &plugins.Status{State: plugins.StateErr})
		return
	}
	p.logger.Info("Initializing Cedarling")
	new_instance, err := cedarling_go.NewCedarling(new_config)
	if err != nil {
		p.logger.Info(err.Error())
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

func (Factory) New(m *plugins.Manager, config any) plugins.Plugin {

	m.UpdatePluginStatus(PluginName, &plugins.Status{State: plugins.StateNotReady})

	return &CedarPlugin{
		manager: m,
		config:  config.(Config),
		logger:  m.Logger().WithFields(map[string]any{"plugin": PluginName}),
	}
}

func (Factory) Validate(_ *plugins.Manager, config []byte) (any, error) {
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

func extractTokens(subject *Entity) ([]cedarling_go.TokenInput, error) {
	if subject == nil {
		return nil, fmt.Errorf("Subject empty")
	}
	entity := *subject
	var tokens TokenList
	if err := json.Unmarshal(entity.Properties, &tokens); err != nil {
		return nil, err
	}
	output := []cedarling_go.TokenInput{}
	for _, token := range tokens.Tokens {
		temp := cedarling_go.TokenInput{
			Mapping: token.Mapping,
			Payload: token.Payload,
		}
		output = append(output, temp)
	}
	return output, nil
}

func extractResource(resource *Entity) (cedarling_go.EntityData, error) {
	var output cedarling_go.EntityData
	if resource == nil {
		return output, fmt.Errorf("Resource empty")
	}
	err := json.Unmarshal(resource.Properties, &output)
	return output, err
}

func (p *CedarPlugin) evaluate(subject *Entity, action string, resource *Entity, context map[string]any) (*cedarling_go.MultiIssuerAuthorizeResult, error) {
	tokens, err := extractTokens(subject)
	if err != nil {
		return nil, err
	}
	resourceEntity, err := extractResource(resource)
	if err != nil {
		return nil, err
	}
	authorization_request := cedarling_go.AuthorizeMultiIssuerRequest{
		Tokens:   tokens,
		Action:   action,
		Resource: resourceEntity,
		Context:  context,
	}
	authorization_result, err := p.cedar.AuthorizeMultiIssuer(authorization_request)
	if err != nil {
		return nil, err
	}
	return &authorization_result, nil
}

func (p *CedarPlugin) buildEvaluationResponse(subject *Entity, action string, resource *Entity, context map[string]any) (*EvaluationResponse, error) {
	result, err := p.evaluate(subject, action, resource, context)
	response := EvaluationResponse{}
	if err != nil {
		if result == nil {
			return nil, err
		} else {
			response.Decision = false
			response.Context = map[string]any{
				"error": err.Error(),
			}
		}
	} else {
		response.Decision = result.Decision
	}
	return &response, nil
}
