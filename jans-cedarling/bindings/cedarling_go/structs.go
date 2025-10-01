package cedarling_go

import (
	"encoding/json"
	"fmt"
)

// Represents a cedarling request
type Request struct {
	Tokens   map[string]string
	Action   string
	Resource EntityData
	Context  any
}

func (r Request) MarshalJSON() ([]byte, error) {
	context := r.Context
	if context == nil {
		context = json.RawMessage(`{}`)
	}

	aux := struct {
		Tokens   map[string]string `json:"tokens"`
		Action   string            `json:"action"`
		Resource EntityData        `json:"resource"`
		Context  any               `json:"context"`
	}{
		Tokens:   r.Tokens,
		Action:   r.Action,
		Resource: r.Resource,
		Context:  context,
	}
	return json.Marshal(aux)

}

// Represents a cedarling principal or resource entity
type EntityData struct {
	CedarMapping CedarEntityMapping
	// Payload will be flattened into the JSON object.
	Payload map[string]any
}

// Represents type and id for a cedarling entity
type CedarEntityMapping struct {
	EntityType string `json:"entity_type"`
	ID         string `json:"id"`
}

// MarshalJSON flattens Payload into the top-level JSON object.
func (e EntityData) MarshalJSON() ([]byte, error) {
	m := map[string]any{
		"cedar_entity_mapping": e.CedarMapping,
	}
	for k, v := range e.Payload {
		m[k] = v
	}
	return json.Marshal(m)
}

// UnmarshalJSON extracts "cedar_entity_mapping" then stores the rest in Payload.
func (e *EntityData) UnmarshalJSON(data []byte) error {
	m := make(map[string]any)
	if err := json.Unmarshal(data, &m); err != nil {
		return err
	}
	if cedarMapping, ok := m["cedar_entity_mapping"].(map[string]any); ok {
		if entityType, ok := cedarMapping["entity_type"].(string); ok {
			e.CedarMapping.EntityType = entityType
		}
		if id, ok := cedarMapping["id"].(string); ok {
			e.CedarMapping.ID = id
		}
		delete(m, "cedar_entity_mapping")
	}
	e.Payload = m
	return nil
}

// Represents the result of an authorization request
type AuthorizeResult struct {
	Workload   *CedarResponse           `json:"workload,omitempty"`
	Person     *CedarResponse           `json:"person,omitempty"`
	Principals map[string]CedarResponse `json:"principals"`
	Decision   bool                     `json:"decision"`
	RequestID  string                   `json:"request_id"`
}

// Represents the authorization decision
type CedarResponse struct {
	decision DecisionType
	reason   []string
	errors   []string
}

func (r CedarResponse) Decision() DecisionType {
	return r.decision
}

func (r CedarResponse) IsAllowed() bool {
	return r.decision == DecisionAllow
}

func (r CedarResponse) Reason() []string {
	return r.reason
}

func (r CedarResponse) Errors() []string {
	return r.errors
}

type DecisionType int8

// Decision constants that can be used to represent the decision made by Cedar.
const (
	// we use +1 to be sure that the values are not zero
	DecisionAllow DecisionType = iota + 1
	DecisionDeny
)

// rust Cedar type representations the Cedar decision type.
func (d DecisionType) jsonValue() string {
	switch d {
	case DecisionAllow:
		return "allow"
	case DecisionDeny:
		return "deny"
	default:
		// should never happen
		return ""
	}
}

func (d DecisionType) ToString() string {
	switch d {
	case DecisionAllow:
		return "Allow"
	case DecisionDeny:
		return "Deny"
	default:
		// should never happen
		return ""
	}
}

func (r *CedarResponse) UnmarshalJSON(data []byte) error {
	aux := struct {
		Decision string   `json:"decision"`
		Reason   []string `json:"reason"`
		Errors   []string `json:"errors"`
	}{}

	if err := json.Unmarshal(data, &aux); err != nil {
		return err
	}
	switch aux.Decision {
	case DecisionAllow.jsonValue():
		r.decision = DecisionAllow
	case DecisionDeny.jsonValue():
		r.decision = DecisionDeny
	default:
		return fmt.Errorf("invalid decision value: %q", aux.Decision)
	}

	r.reason = aux.Reason
	r.errors = aux.Errors

	return nil
}

func (r CedarResponse) MarshalJSON() ([]byte, error) {
	aux := struct {
		Decision string   `json:"decision"`
		Reason   []string `json:"reason"`
		Errors   []string `json:"errors"`
	}{
		Decision: r.decision.jsonValue(),
		Reason:   r.reason,
		Errors:   r.errors,
	}
	return json.Marshal(aux)
}

// Represents an unsigned authorization request
type RequestUnsigned struct {
	Principals []EntityData
	Action     string
	Resource   EntityData
	Context    any
}

func (r RequestUnsigned) MarshalJSON() ([]byte, error) {
	context := r.Context
	if context == nil {
		context = json.RawMessage(`{}`)
	}

	aux := struct {
		Principals []EntityData `json:"principals"`
		Action     string       `json:"action"`
		Resource   EntityData   `json:"resource"`
		Context    any          `json:"context"`
	}{
		Principals: r.Principals,
		Action:     r.Action,
		Resource:   r.Resource,
		Context:    context,
	}
	return json.Marshal(aux)

}
