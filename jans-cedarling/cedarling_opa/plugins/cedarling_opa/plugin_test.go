package cedarlingopa

import (
	"bytes"
	"encoding/json"
	"fmt"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/JanssenProject/jans/jans-cedarling/bindings/cedarling_go"
	"github.com/open-policy-agent/opa/v1/logging"
	"github.com/open-policy-agent/opa/v1/plugins"
	"github.com/open-policy-agent/opa/v1/storage/inmem"
)

type mockCedarling struct {
	authorizeFn         func(cedarling_go.AuthorizeMultiIssuerRequest) (cedarling_go.MultiIssuerAuthorizeResult, error)
	unsignedAuthorizeFn func(cedarling_go.RequestUnsigned) (cedarling_go.AuthorizeResult, error)
	authorizeErr        error
	shutdownCalled      bool
}

func (m *mockCedarling) AuthorizeMultiIssuer(request cedarling_go.AuthorizeMultiIssuerRequest) (cedarling_go.MultiIssuerAuthorizeResult, error) {
	return m.authorizeFn(request)
}

func (m *mockCedarling) AuthorizeUnsigned(request cedarling_go.RequestUnsigned) (cedarling_go.AuthorizeResult, error) {
	return m.unsignedAuthorizeFn(request)
}

func (m *mockCedarling) ShutDown() {
	m.shutdownCalled = true
}

func overrideEvaluationLogic(logic EvaluationMode) func(*CedarPlugin) {
	return func(p *CedarPlugin) {
		p.config.Evaluation_Logic = logic
	}
}

func mockPlugin(t *testing.T, opts ...func(*CedarPlugin)) *CedarPlugin {
	t.Helper()
	store := inmem.New()
	manager, _ := plugins.New(
		[]byte(`{}`),
		"test-id",
		store,
	)
	plugin := &CedarPlugin{
		manager: manager,
		logger:  logging.NewNoOpLogger(),
		cedar: &mockCedarling{
			authorizeFn: func(cedarling_go.AuthorizeMultiIssuerRequest) (cedarling_go.MultiIssuerAuthorizeResult, error) {
				return cedarling_go.MultiIssuerAuthorizeResult{
					Decision: true,
				}, nil
			},
			unsignedAuthorizeFn: func(cedarling_go.RequestUnsigned) (cedarling_go.AuthorizeResult, error) {
				return cedarling_go.AuthorizeResult{
					Decision: true,
				}, nil
			},
		},
		config: Config{
			BootstrapConfig:  map[string]any{},
			Host:             "https://host.test.org",
			Evaluation_Logic: MultiIssuer,
		},
	}
	for _, opt := range opts {
		opt(plugin)
	}
	return plugin
}

func TestPluginName(t *testing.T) {
	if PluginName != "cedarling_opa" {
		t.Errorf("Plugin name should be cedarling_opa, got %s", PluginName)
	}
}

func TestValidateConfig(t *testing.T) {
	factory := Factory{}
	config := []byte(`{"bootstrap_config": {}, "host": "test.example.org"}`)
	result, err := factory.Validate(nil, config)
	if err != nil {
		t.Errorf("Error should not be raised. Raised %s", err.Error())
	}
	cfg, ok := result.(Config)
	if !ok {
		t.Errorf("Validate should return Config, returned %T", result)
	}
	if cfg.Host != "test.example.org" {
		t.Errorf("Host should be test.example.org, got %s", cfg.Host)
	}
}

func TestStopPlugin(t *testing.T) {
	plugin := mockPlugin(t)
	plugin.Stop(t.Context())
	WithCedarlingInstance(func(instance Cedarling) error {
		if instance != nil {
			t.Errorf("Cedarling should be nil. Is %s", instance)
		}
		return nil
	})
}

func TestMetaData(t *testing.T) {
	plugin := mockPlugin(t)
	setGlobalInstance(plugin)
	req := httptest.NewRequest(http.MethodGet, "/.well-known/authzen-configuration", nil)
	w := httptest.NewRecorder()
	plugin.MetaDataHandler(w, req)
	defer w.Result().Body.Close()
	if w.Result().StatusCode != http.StatusOK {
		t.Errorf("Expected status %d, got %d", http.StatusOK, w.Result().StatusCode)
	}
	var metadata PDPMetadata
	err := json.Unmarshal(w.Body.Bytes(), &metadata)
	if err != nil {
		t.Errorf("Error should not be raised. Raised %s", err.Error())
	}
	if metadata.AccessEvaluationEndpoint != "https://host.test.org/access/v1/evaluation" {
		t.Errorf("Got %s, expected https://host.test.org/access/v1/evaluation", metadata.AccessEvaluationEndpoint)
	}
	if metadata.PolicyDecisionPoint != "https://host.test.org" {
		t.Errorf("Got %s, expected https://host.test.org", metadata.PolicyDecisionPoint)
	}
}

func TestEvaluationFunction(t *testing.T) {
	plugin := mockPlugin(t)
	setGlobalInstance(plugin)
	body := []byte(`{"subject":{"type":"user","id":"alice@example.com"},
		"resource":{"type":"account","id":"123"},
		"action":{"name":"can_read","properties":{"method":"GET"}},
		"context":{"time":"1985-10-26T01:22-07:00"}}`)
	body_reader := bytes.NewReader(body)
	req := httptest.NewRequest(http.MethodPost, "/access/v1/evaluation", body_reader)
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()
	plugin.AccessEvaluationHandler(w, req)
	defer w.Result().Body.Close()
	response := w.Body.Bytes()
	if w.Result().StatusCode != http.StatusOK {
		t.Errorf("Expected status %d, got %d, %s", http.StatusOK, w.Result().StatusCode, string(response))
	}
	var result EvaluationResponse
	err := json.Unmarshal(response, &result)
	if err != nil {
		t.Errorf("Error should not be raised. Raised %s", err.Error())
	}
	if !result.Decision {
		t.Errorf("Got decision %t, should be true", result.Decision)
	}
}

func TestEvaluationFunctionMissingFields(t *testing.T) {
	input1 := []byte(`{"resource": {}, "action": {}}`)
	input2 := []byte(`{"subject": {}, "action": {}}`)
	input3 := []byte(`{"subject": {}, "resource": {}}`)
	tests := []string{"subject", "resource", "action"}
	input_array := [][]byte{input1, input2, input3}
	evaluation_modes := []EvaluationMode{MultiIssuer, Unsigned}
	for _, mode := range evaluation_modes {
		plugin := mockPlugin(t, overrideEvaluationLogic(mode))
		setGlobalInstance(plugin)
		for i, input := range input_array {
			body_reader := bytes.NewReader(input)
			req := httptest.NewRequest(http.MethodPost, "/access/v1/evaluation", body_reader)
			req.Header.Set("Content-Type", "application/json")
			w := httptest.NewRecorder()
			plugin.AccessEvaluationHandler(w, req)
			defer w.Result().Body.Close()
			response := w.Body.Bytes()
			if w.Result().StatusCode != http.StatusBadRequest {
				t.Errorf("Expected status %d, got %d, %s", http.StatusBadRequest, w.Result().StatusCode, string(response))
			}
			var e GenericError
			err := json.Unmarshal(response, &e)
			if err != nil {
				t.Errorf("Error should not be raised. Raised %s", err.Error())
			}
			expected := fmt.Sprintf("Invalid input: missing %s", tests[i])
			if e.Error != expected {
				t.Errorf("Error should be %s, got %s", expected, e.Error)
			}
		}

	}
}
