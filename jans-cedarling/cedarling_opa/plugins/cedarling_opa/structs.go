package cedarlingopa

import "encoding/json"

type PDPMetadata struct {
	PolicyDecisionPoint      string `json:"policy_decision_point"`
	AccessEvaluationEndpoint string `json:"access_evaluation_endpoint"`
}

type Token struct {
	Mapping string `json:"mapping"`
	Payload string `json:"payload"`
}

type TokenList struct {
	Tokens []Token `json:"tokens"`
}

type CedarMapping struct {
	EntityType string `json:"entity_type"`
	ID         string `json:"id"`
}

type CedarEntityData struct {
	CedarMapping CedarMapping   `json:"cedar_entity_mapping"`
	Payload      map[string]any `json:"payload"`
}

type Entity struct {
	Type       string          `json:"type"`
	ID         string          `json:"id"`
	Properties json.RawMessage `json:"properties,omitempty"`
}

type Action struct {
	Name       string         `json:"name"`
	Properties map[string]any `json:"properties,omitempty"`
}

type EvaluationRequest struct {
	Subject  Entity         `json:"subject"`
	Resource Entity         `json:"resource"`
	Action   Action         `json:"action"`
	Context  map[string]any `json:"context,omitempty"`
}

type EvaluationResponse struct {
	Decision bool           `json:"decision"`
	Context  map[string]any `json:"context,omitempty"`
}
