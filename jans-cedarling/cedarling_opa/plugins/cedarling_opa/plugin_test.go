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

type AuthorizeFunc func(cedarling_go.AuthorizeMultiIssuerRequest) (cedarling_go.MultiIssuerAuthorizeResult, error)

type UnsignedAuthorizeFunc func(cedarling_go.RequestUnsigned) (cedarling_go.AuthorizeResult, error)

type mockCedarling struct {
	authorizeFn         AuthorizeFunc
	unsignedAuthorizeFn UnsignedAuthorizeFunc
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

func authorizeResult(decision bool) AuthorizeFunc {
	return func(_ cedarling_go.AuthorizeMultiIssuerRequest) (cedarling_go.MultiIssuerAuthorizeResult, error) {
		return cedarling_go.MultiIssuerAuthorizeResult{
			Decision: decision,
		}, nil
	}
}

func authorizeResultUnsigned(decision bool) UnsignedAuthorizeFunc {
	return func(_ cedarling_go.RequestUnsigned) (cedarling_go.AuthorizeResult, error) {
		return cedarling_go.AuthorizeResult{
			Decision: decision,
		}, nil
	}
}

func authorizeError(err error) AuthorizeFunc {
	return func(_ cedarling_go.AuthorizeMultiIssuerRequest) (cedarling_go.MultiIssuerAuthorizeResult, error) {
		return cedarling_go.MultiIssuerAuthorizeResult{}, err
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
			authorizeFn:         authorizeResult(true),
			unsignedAuthorizeFn: authorizeResultUnsigned(true),
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

func mockPluginWithAuthorize(t *testing.T, authorize AuthorizeFunc, authorizeUnsigned UnsignedAuthorizeFunc) *CedarPlugin {
	t.Helper()
	return mockPlugin(
		t,
		func(p *CedarPlugin) {
			p.cedar = &mockCedarling{
				authorizeFn:         authorize,
				unsignedAuthorizeFn: authorizeUnsigned,
			}
		},
	)
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
	t.Cleanup(func() {
		clearGlobalInstance(plugin)
	})
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
	t.Cleanup(func() {
		clearGlobalInstance(plugin)
	})
}

func TestEvaluationFunctionUnsigned(t *testing.T) {
	plugin := mockPlugin(t, overrideEvaluationLogic(Unsigned))
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
	t.Cleanup(func() {
		clearGlobalInstance(plugin)
	})
}

func TestEvaluationFunctionMissingFields(t *testing.T) {
	tests := []struct {
		name string
		body []byte
	}{
		{"subject", []byte(`{"resource": {}, "action": {}}`)},
		{"resource", []byte(`{"subject": {}, "action": {}}`)},
		{"action", []byte(`{"subject": {}, "resource": {}}`)},
	}
	evaluation_modes := []EvaluationMode{MultiIssuer, Unsigned}
	for _, mode := range evaluation_modes {
		plugin := mockPlugin(t, overrideEvaluationLogic(mode))
		setGlobalInstance(plugin)
		for _, test := range tests {
			body_reader := bytes.NewReader(test.body)
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
			expected := fmt.Sprintf("Invalid input: missing %s", test.name)
			if e.Error != expected {
				t.Errorf("Error should be %s, got %s", expected, e.Error)
			}
		}
		t.Cleanup(func() {
			clearGlobalInstance(plugin)
		})
	}
}

func TestEvaluationsFunction(t *testing.T) {
	plugin := mockPlugin(t)
	setGlobalInstance(plugin)
	body := []byte(`{"evaluations": [
    {"subject": {"type": "user","id": "alice@example.com"},
    "action": {"name": "can_read"},
    "resource": {"type": "document","id": "boxcarring.md"},
    "context": {"time": "2024-05-31T15:22-07:00"}},
    {"subject": {"type": "user","id": "alice@example.com"},
    "action": {"name": "can_read"},
    "resource": {"type": "document","id": "subject-search.md"},
    "context": {"time": "2024-05-31T15:22-07:00"}},
    {"subject": {"type": "user","id": "alice@example.com"},
    "action": {"name": "can_read"},
    "resource": {"type": "document", "id": "resource-search.md"},
    "context": {"time": "2024-05-31T15:22-07:00"}}
  ]}`)
	body_reader := bytes.NewReader(body)
	req := httptest.NewRequest(http.MethodPost, "/access/v1/evaluations", body_reader)
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()
	plugin.AccessEvaluationsHandler(w, req)
	defer w.Result().Body.Close()
	response := w.Body.Bytes()
	if w.Result().StatusCode != http.StatusOK {
		t.Errorf("Expected status %d, got %d, %s", http.StatusOK, w.Result().StatusCode, string(response))
	}
	var result []EvaluationResponse
	err := json.Unmarshal(response, &result)
	if err != nil {
		t.Errorf("Error should not be raised. Raised %s", err.Error())
	}
	for _, r := range result {
		if !r.Decision {
			t.Errorf("Got decision %t, should be true", r.Decision)
		}
	}
	t.Cleanup(func() {
		clearGlobalInstance(plugin)
	})
}

func TestEvaluationsFunctionError(t *testing.T) {
	plugin := mockPlugin(t)
	setGlobalInstance(plugin)
	body := []byte(`{}`)
	body_reader := bytes.NewReader(body)
	req := httptest.NewRequest(http.MethodPost, "/access/v1/evaluations", body_reader)
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()
	plugin.AccessEvaluationsHandler(w, req)
	defer w.Result().Body.Close()
	response := w.Body.Bytes()
	if w.Result().StatusCode != http.StatusBadRequest {
		t.Errorf("Expected status %d, got %d, %s", http.StatusBadRequest, w.Result().StatusCode, string(response))
	}
	t.Cleanup(func() {
		clearGlobalInstance(plugin)
	})
}

func TestEvaluationsFunctionFallback(t *testing.T) {
	plugin := mockPlugin(t)
	setGlobalInstance(plugin)
	body := []byte(`{"subject":{"type":"user","id":"alice@example.com"},
		"resource":{"type":"account","id":"123"},
		"action":{"name":"can_read","properties":{"method":"GET"}},
		"context":{"time":"1985-10-26T01:22-07:00"}}`)
	body_reader := bytes.NewReader(body)
	req := httptest.NewRequest(http.MethodPost, "/access/v1/evaluations", body_reader)
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()
	plugin.AccessEvaluationsHandler(w, req)
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
	t.Cleanup(func() {
		clearGlobalInstance(plugin)
	})
}

func TestEvaluationsFunctionDefaultsError(t *testing.T) {
	plugin := mockPlugin(t)
	setGlobalInstance(plugin)
	tests := []struct {
		name        string
		body        []byte
		expectedErr bool
	}{
		{"subject", []byte(`{"evaluations": [{"resource":{"type":"account","id":"456"}}],
			"resource":{"type":"account","id":"123"}, "action":{"name":"can_read","properties":{"method":"GET"}}}`), true},
		{"action", []byte(`{"evaluations": [{"subject":{"type":"user","id":"bob@example.com"}}],
			"subject":{"type":"user","id":"alice@example.com"}, "resource":{"type":"account","id":"123"}`), true},
		{"resource", []byte(`{"evaluations": [{"subject":{"type":"user","id":"alice@example.com"}}],
			"subject":{"type":"user","id":"alice@example.com"}, "action":{"name":"can_read","properties":{"method":"GET"}}}`), true},
		{"none", []byte(`{"evaluations": [{"subject":{"type":"user","id":"alice@example.com"}}], "subject":{"type":"user","id":"alice@example.com"},
			"resource":{"type":"account","id":"123"},
			"action":{"name":"can_read","properties":{"method":"GET"}},
			"context":{"time":"1985-10-26T01:22-07:00"}}`), false},
	}
	for _, test := range tests {
		body_reader := bytes.NewReader(test.body)
		req := httptest.NewRequest(http.MethodPost, "/access/v1/evaluations", body_reader)
		req.Header.Set("Content-Type", "application/json")
		w := httptest.NewRecorder()
		plugin.AccessEvaluationsHandler(w, req)
		defer w.Result().Body.Close()
		response := w.Body.Bytes()
		if test.expectedErr {
			if w.Result().StatusCode != http.StatusBadRequest {
				t.Errorf("Expected status %d, got %d", http.StatusBadRequest, w.Result().StatusCode)
			}
		} else {
			if w.Result().StatusCode != http.StatusOK {
				t.Errorf("Expected status %d, got %d", http.StatusOK, w.Result().StatusCode)
				var e GenericError
				err := json.Unmarshal(response, &e)
				if err != nil {
					t.Errorf("Error should not be raised. Raised %s", err.Error())
				}
				expected := fmt.Sprintf("Invalid input: missing %s", test.name)
				if e.Error != expected {
					t.Errorf("Error should be %s, got %s", expected, e.Error)
				}
			}
		}
	}
	t.Cleanup(func() {
		clearGlobalInstance(plugin)
	})
}

func TestEvaluationsFunctionOptions(t *testing.T) {
	plugin := mockPlugin(t)
	setGlobalInstance(plugin)
	baseRequest := MultipleEvaluationRequest{
		Evaluations: []EvaluationRequest{
			{
				Subject:  &Entity{Type: "user", ID: "alice"},
				Resource: &Entity{Type: "document", ID: "boxcarring.md"},
				Action:   &Action{Name: "can_read"},
				Context:  map[string]any{"time": "2024-01-01T00:00:00Z"},
			},
			{
				Subject:  &Entity{Type: "user", ID: "alice"},
				Resource: &Entity{Type: "document", ID: "subject-search.md"},
				Action:   &Action{Name: "can_read"},
				Context:  map[string]any{"time": "2024-01-01T00:00:00Z"},
			},
			{
				Subject:  &Entity{Type: "user", ID: "alice"},
				Resource: &Entity{Type: "document", ID: "resource-search.md"},
				Action:   &Action{Name: "can_read"},
				Context:  map[string]any{"time": "2024-01-01T00:00:00Z"},
			},
		},
	}
	tests := []struct {
		option         EvaluationsSemantic
		authorizeFunc  AuthorizeFunc
		responseLength int
	}{
		{ExecuteAll, authorizeResult(true), 3},
		{DenyOnFirstDeny, authorizeResult(false), 1},
		{PermitOnFirstPermit, authorizeResult(true), 1},
	}
	for _, test := range tests {
		baseRequest.Options = &Option{EvaluationSemantic: test.option}
		plugin := mockPluginWithAuthorize(t, test.authorizeFunc, authorizeResultUnsigned(true))
		body, _ := json.Marshal(baseRequest)
		setGlobalInstance(plugin)
		body_reader := bytes.NewReader(body)
		req := httptest.NewRequest(http.MethodPost, "/access/v1/evaluations", body_reader)
		req.Header.Set("Content-Type", "application/json")
		w := httptest.NewRecorder()
		plugin.AccessEvaluationsHandler(w, req)
		defer w.Result().Body.Close()
		response := w.Body.Bytes()
		if w.Result().StatusCode != http.StatusOK {
			t.Errorf("Expected status %d, got %d, %s", http.StatusOK, w.Result().StatusCode, string(response))
		}
		var authorizeResult []EvaluationResponse
		err := json.Unmarshal(response, &authorizeResult)
		if err != nil {
			t.Errorf("Error should not be raised. Raised %s", err.Error())
			t.Log(string(response))
		}
		if len(authorizeResult) != test.responseLength {
			t.Errorf("Got %d values, expected %d", len(authorizeResult), test.responseLength)
		}
		t.Cleanup(func() {
			clearGlobalInstance(plugin)
		})
	}
}
