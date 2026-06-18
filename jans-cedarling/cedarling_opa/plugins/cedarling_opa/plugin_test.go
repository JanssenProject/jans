package cedarlingopa

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/JanssenProject/jans/jans-cedarling/bindings/cedarling_go"
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

func mockPlugin(t *testing.T) *CedarPlugin {
	t.Helper()
	store := inmem.New()
	manager, _ := plugins.New(
		[]byte(`{}`),
		"test-id",
		store,
	)
	plugin := &CedarPlugin{
		manager: manager,
		logger:  manager.Logger(),
		cedar:   &mockCedarling{},
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
	if w.Result().StatusCode != http.StatusOK {
		t.Errorf("Expected status %d, got %d", http.StatusOK, w.Result().StatusCode)
	}
	var metadata PDPMetadata
	err := json.Unmarshal(w.Body.Bytes(), &metadata)
	if err != nil {
		t.Errorf("Error should not be raised. Raised %s", err.Error())
	}
	if metadata.AccessEvaluationEndpoint != "/access/v1/evaluation" {
		t.Errorf("Got %s, expected /access/v1/evaluation", metadata.AccessEvaluationEndpoint)
	}
	if metadata.PolicyDecisionPoint != "" {
		t.Errorf("Got %s", metadata.PolicyDecisionPoint)
	}
}
