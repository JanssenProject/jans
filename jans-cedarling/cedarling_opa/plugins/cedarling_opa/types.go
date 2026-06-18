package cedarlingopa

import (
	"encoding/json"
	"fmt"

	"github.com/JanssenProject/jans/jans-cedarling/bindings/cedarling_go"
)

type EvaluationsSemantic string

type Cedarling interface {
	AuthorizeMultiIssuer(request cedarling_go.AuthorizeMultiIssuerRequest) (cedarling_go.MultiIssuerAuthorizeResult, error)
	AuthorizeUnsigned(request cedarling_go.RequestUnsigned) (cedarling_go.AuthorizeResult, error)
	ShutDown()
}

const (
	ExecuteAll          EvaluationsSemantic = "execute_all"
	DenyOnFirstDeny     EvaluationsSemantic = "deny_on_first_deny"
	PermitOnFirstPermit EvaluationsSemantic = "permit_on_first_permit"
)

func (e *EvaluationsSemantic) UnmarshalJSON(data []byte) error {
	var s string
	if err := json.Unmarshal(data, &s); err != nil {
		return err
	}
	switch EvaluationsSemantic(s) {
	case PermitOnFirstPermit, DenyOnFirstDeny, ExecuteAll:
		*e = EvaluationsSemantic(s)
		return nil
	default:
		error := fmt.Errorf("Invalid semantic provided: %s", s)
		return error
	}
}

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

type MultipleEvaluationBase struct {
	Subject  *Entity        `json:"subject,omitempty"`
	Resource *Entity        `json:"resource,omitempty"`
	Action   *Action        `json:"action,omitempty"`
	Context  map[string]any `json:"context,omitempty"`
}

type Option struct {
	EvaluationSemantic EvaluationsSemantic `json:"evaluation_semantic,omitempty"`
}

type MultipleEvaluationRequest struct {
	Subject    *Entity                  `json:"subject,omitempty"`
	Resource   *Entity                  `json:"resource,omitempty"`
	Action     *Action                  `json:"action,omitempty"`
	Context    map[string]any           `json:"context,omitempty"`
	Evaluation []MultipleEvaluationBase `json:"evaluations,omitempty"`
	Options    *Option                  `json:"options,omitempty"`
}
