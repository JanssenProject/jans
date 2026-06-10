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
	"mime"
	"net/http"
	"os"
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
	cedar   *cedarling_go.Cedarling
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

func WithCedarlingInstance(fn func(*cedarling_go.Cedarling) error) error {
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
	p.cedar = instance
	p.manager.ExtraRoute("/.well-known/authzen-configuration", "metadata", p.MetaDataHandler)
	p.manager.ExtraRoute("/access/v1/evaluation", "evaluation", p.AccessEvaluationHandler)
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
		fmt.Fprintln(os.Stderr, err)
		p.manager.UpdatePluginStatus(PluginName, &plugins.Status{State: plugins.StateErr})
		return
	}
	p.logger.Info("Initializing Cedarling")
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

func (p *CedarPlugin) MetaDataHandler(w http.ResponseWriter, r *http.Request) {
	request_id := r.Header.Get("X-Request-ID")
	if request_id != "" {
		w.Header().Add("X-Request-ID", request_id)
	}
	if r.Method != "GET" {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]any{"error": "Invalid Request"})
		return
	}
	w.Header().Add("Content-Type", "application/json")
	globalInstanceMu.RLock()
	plugin := globalInstance
	globalInstanceMu.RUnlock()
	if plugin == nil {
		w.WriteHeader(http.StatusServiceUnavailable)
		json.NewEncoder(w).Encode(map[string]any{"error": "Plugin not available"})
		return
	}
	metadata := PDPMetadata{
		PolicyDecisionPoint:      p.config.Host,
		AccessEvaluationEndpoint: fmt.Sprintf("%s/access/v1/evaluation", p.config.Host),
	}
	w.WriteHeader(200)
	if err := json.NewEncoder(w).Encode(metadata); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]any{"error": err.Error()})
		return
	}
}

func (p *CedarPlugin) AccessEvaluationHandler(w http.ResponseWriter, r *http.Request) {
	requestId := r.Header.Get("X-Request-ID")
	if requestId != "" {
		w.Header().Add("X-Request-ID", requestId)
	}
	contentType := r.Header.Get("Content-Type")
	mediaType, _, err := mime.ParseMediaType(contentType)
	if r.Method != "POST" || err != nil || mediaType != "application/json" {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]any{"error": "Invalid Request"})
		p.logger.Info(err.Error())
		return
	}
	w.Header().Add("Content-Type", "application/json")
	var request EvaluationRequest
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]any{"error": "Invalid Request"})
		p.logger.Info(err.Error())
		return
	}
	var tokens TokenList
	if err := json.Unmarshal(request.Subject.Properties, &tokens); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]any{"error": "Invalid token input"})
		p.logger.Info(err.Error())
		return
	}
	tokenEntities := []cedarling_go.TokenInput{}
	for _, token := range tokens.Tokens {
		new_token := cedarling_go.TokenInput{
			Mapping: token.Mapping,
			Payload: token.Payload,
		}
		tokenEntities = append(tokenEntities, new_token)
	}
	var resourceEntity cedarling_go.EntityData
	err = json.Unmarshal(request.Resource.Properties, &resourceEntity)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]any{"error": "Invalid resource input"})
		p.logger.Info(err.Error())
		return
	}
	cedarAction := request.Action.Name
	cedarling_request := cedarling_go.AuthorizeMultiIssuerRequest{
		Tokens:   tokenEntities,
		Action:   cedarAction,
		Resource: resourceEntity,
		Context:  request.Context,
	}
	result, err := p.cedar.AuthorizeMultiIssuer(cedarling_request)
	if err != nil {
		w.WriteHeader(http.StatusOK)
		json.NewEncoder(w).Encode(map[string]any{"error": err.Error()})
		return
	}
	response := EvaluationResponse{
		Decision: result.Decision,
	}
	w.WriteHeader(200)
	if err := json.NewEncoder(w).Encode(response); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]any{"error": "Something went wrong"})
		p.logger.Info(err.Error())
		return
	}
}
