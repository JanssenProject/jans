package cedarling_go

import (
	"encoding/json"
	"fmt"
	"time"
)

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
	Response  CedarResponse `json:"response"`
	Decision  bool          `json:"decision"`
	RequestID string        `json:"request_id"`
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

// Represents an unsigned authorization request.
// Principal is optional; when nil the request is evaluated via Cedar's partial
// evaluator and any residual (principal-dependent) policies fail closed to Deny.
type RequestUnsigned struct {
	Principal *EntityData
	Action    string
	Resource  EntityData
	Context   any
	// Options is per-call metadata such as a TRACE context; nil means none.
	Options *AuthorizeOptions
}

func (r RequestUnsigned) MarshalJSON() ([]byte, error) {
	context := r.Context
	if context == nil {
		context = json.RawMessage(`{}`)
	}

	aux := struct {
		Principal *EntityData       `json:"principal,omitempty"`
		Action    string            `json:"action"`
		Resource  EntityData        `json:"resource"`
		Context   any               `json:"context"`
		Options   *AuthorizeOptions `json:"options,omitempty"`
	}{
		Principal: r.Principal,
		Action:    r.Action,
		Resource:  r.Resource,
		Context:   context,
		Options:   r.Options,
	}
	return json.Marshal(aux)

}

// TraceContext is the TRACE execution context of a call, already normalized by
// the host. Cedarling never generates ExecutionID: the execution initiator
// (gateway, PEP, host) owns it together with ExecutionAuthority.
type TraceContext struct {
	ExecutionID        string  `json:"execution_id"`
	ExecutionAuthority string  `json:"execution_authority"`
	WorkloadID         string  `json:"workload_id"`
	WorkloadInstanceID *string `json:"workload_instance_id,omitempty"`
}

// AuthorizeOptions is per-call metadata of an authorization request. None of
// it takes part in policy evaluation: the decision is the same with or without
// options. A nil Trace means the host has no execution context.
type AuthorizeOptions struct {
	Trace *TraceContext `json:"trace,omitempty"`
}

// TokenInput represents a JWT token with explicit type mapping
type TokenInput struct {
	Mapping string `json:"mapping"`
	Payload string `json:"payload"`
}

// AuthorizeMultiIssuerRequest represents a multi-issuer authorization request
type AuthorizeMultiIssuerRequest struct {
	Tokens   []TokenInput `json:"tokens"`
	Resource EntityData   `json:"resource"`
	Action   string       `json:"action"`
	Context  any          `json:"context"`
	// Options is per-call metadata such as a TRACE context; nil means none.
	Options *AuthorizeOptions `json:"options,omitempty"`
}

func (r AuthorizeMultiIssuerRequest) MarshalJSON() ([]byte, error) {
	context := r.Context
	if context == nil {
		context = json.RawMessage(`{}`)
	}

	aux := struct {
		Tokens   []TokenInput      `json:"tokens"`
		Resource EntityData        `json:"resource"`
		Action   string            `json:"action"`
		Context  any               `json:"context"`
		Options  *AuthorizeOptions `json:"options,omitempty"`
	}{
		Tokens:   r.Tokens,
		Resource: r.Resource,
		Action:   r.Action,
		Context:  context,
		Options:  r.Options,
	}
	return json.Marshal(aux)
}

// MultiIssuerAuthorizeResult represents the result of a multi-issuer authorization request
type MultiIssuerAuthorizeResult struct {
	Response  CedarResponse `json:"response"`
	Decision  bool          `json:"decision"`
	RequestID string        `json:"request_id"`
	// DroppedTokens lists the supplied tokens that were not used in this decision.
	DroppedTokens []DroppedToken `json:"dropped_tokens,omitempty"`
}

// DroppedToken is a token the caller supplied that multi-issuer authorization
// did not use, identified by its input Mapping and zero-based Index.
type DroppedToken struct {
	Mapping string     `json:"mapping"`
	Index   uint64     `json:"index"`
	Reason  DropReason `json:"reason"`
}

// DropReason explains why a token was dropped. Kind is the stable slug
// ("jwt_validation_failed", ...); Detail is set only for "invalid_input"
// ("empty_mapping" / "empty_payload"); Message is the non-empty claim-free text.
type DropReason struct {
	Kind    string  `json:"kind"`
	Detail  *string `json:"detail,omitempty"`
	Message string  `json:"message"`
}

// BatchItem is one {resource, action, context} triple in a batch
// authorization request. When Context is nil it is serialized as an empty
// object; explicit non-object values are rejected at validate-time.
type BatchItem struct {
	Resource EntityData `json:"resource"`
	Action   string     `json:"action"`
	Context  any        `json:"context"`
}

// MarshalJSON emits Context as `{}` when nil so the batch validate step doesn't
// reject the item.
func (b BatchItem) MarshalJSON() ([]byte, error) {
	context := b.Context
	if context == nil {
		context = json.RawMessage(`{}`)
	}
	aux := struct {
		Resource EntityData `json:"resource"`
		Action   string     `json:"action"`
		Context  any        `json:"context"`
	}{
		Resource: b.Resource,
		Action:   b.Action,
		Context:  context,
	}
	return json.Marshal(aux)
}

// BatchAuthorizeUnsignedRequest bundles one optional principal with N BatchItems.
type BatchAuthorizeUnsignedRequest struct {
	Principal *EntityData `json:"principal,omitempty"`
	Items     []BatchItem `json:"items"`
	// Options applies to the whole batch, not to individual items.
	Options *AuthorizeOptions `json:"options,omitempty"`
}

// BatchAuthorizeMultiIssuerRequest bundles one token set with N BatchItems.
type BatchAuthorizeMultiIssuerRequest struct {
	Tokens []TokenInput `json:"tokens"`
	Items  []BatchItem  `json:"items"`
	// Options applies to the whole batch, not to individual items.
	Options *AuthorizeOptions `json:"options,omitempty"`
}

// BatchItemError is the per-item build failure surfaced inside a batch
// response when Cedar couldn't be reached for that item. Category is the
// stable variant slug ("action_parse", "resource_build", …); ItemIndex is
// the position of the failing item in the original items list.
type BatchItemError struct {
	Variant   string `json:"variant"`
	Message   string `json:"message"`
	ItemIndex uint64 `json:"item_index"`
}

// BatchItemUnsignedResult is one slot in BatchAuthorizeUnsignedResponse.Results.
// Exactly one of Ok / Err is non-nil: Ok when Cedar evaluated the item
// (Allow or Deny), Err when the item failed to build. Use IsOk() to switch.
type BatchItemUnsignedResult struct {
	Ok  *AuthorizeResult
	Err *BatchItemError
}

// IsOk reports whether Cedar reached a decision on this item.
func (r BatchItemUnsignedResult) IsOk() bool { return r.Ok != nil }

// UnmarshalJSON decodes the {"Ok":{...}} / {"Err":{...}} discriminated
// union emitted by the core lib.
func (r *BatchItemUnsignedResult) UnmarshalJSON(data []byte) error {
	var raw struct {
		Ok  *AuthorizeResult `json:"Ok,omitempty"`
		Err *BatchItemError  `json:"Err,omitempty"`
	}
	if err := json.Unmarshal(data, &raw); err != nil {
		return err
	}
	*r = BatchItemUnsignedResult{Ok: raw.Ok, Err: raw.Err}
	return nil
}

// BatchItemMultiIssuerResult is the multi-issuer analog of
// BatchItemUnsignedResult.
type BatchItemMultiIssuerResult struct {
	Ok  *MultiIssuerAuthorizeResult
	Err *BatchItemError
}

// IsOk reports whether Cedar reached a decision on this item.
func (r BatchItemMultiIssuerResult) IsOk() bool { return r.Ok != nil }

// UnmarshalJSON decodes the {"Ok":{...}} / {"Err":{...}} discriminated union.
func (r *BatchItemMultiIssuerResult) UnmarshalJSON(data []byte) error {
	var raw struct {
		Ok  *MultiIssuerAuthorizeResult `json:"Ok,omitempty"`
		Err *BatchItemError             `json:"Err,omitempty"`
	}
	if err := json.Unmarshal(data, &raw); err != nil {
		return err
	}
	*r = BatchItemMultiIssuerResult{Ok: raw.Ok, Err: raw.Err}
	return nil
}

// BatchAuthorizeUnsignedResponse carries a shared batch_id alongside per-item
// results. Results[i] corresponds to the Items[i] supplied in the request.
type BatchAuthorizeUnsignedResponse struct {
	BatchID string                    `json:"batch_id"`
	Results []BatchItemUnsignedResult `json:"results"`
}

// BatchAuthorizeMultiIssuerResponse carries a shared batch_id alongside
// per-item multi-issuer results. Results[i] corresponds to the Items[i]
// supplied in the request.
type BatchAuthorizeMultiIssuerResponse struct {
	BatchID string                       `json:"batch_id"`
	Results []BatchItemMultiIssuerResult `json:"results"`
}

// DataEntry represents a single entry in the data store with metadata
type DataEntry struct {
	Key         string  `json:"key"`
	Value       any     `json:"value"`
	DataType    string  `json:"data_type"`
	CreatedAt   string  `json:"created_at"`
	ExpiresAt   *string `json:"expires_at,omitempty"`
	AccessCount uint64  `json:"access_count"`
}

// DataStoreStats represents statistics about the data store
type DataStoreStats struct {
	EntryCount           uint64  `json:"entry_count"`
	MaxEntries           uint64  `json:"max_entries"`
	MaxEntrySize         uint64  `json:"max_entry_size"`
	MetricsEnabled       bool    `json:"metrics_enabled"`
	TotalSizeBytes       uint64  `json:"total_size_bytes"`
	AvgEntrySizeBytes    uint64  `json:"avg_entry_size_bytes"`
	CapacityUsagePercent float64 `json:"capacity_usage_percent"`
	MemoryAlertThreshold float64 `json:"memory_alert_threshold"`
	MemoryAlertTriggered bool    `json:"memory_alert_triggered"`
}

// MetricsSnapshot represents the telemetry metrics snapshot for the current interval.
// Local snapshots are only available when CEDARLING_METRICS_COLLECTION is enabled.
type MetricsSnapshot struct {
	PolicyStats      map[string]int64 `json:"policy_stats"`
	ErrorCounters    map[string]int64 `json:"error_counters"`
	OperationalStats map[string]int64 `json:"operational_stats"`
	// Interval elapsed since the previous drain. Crosses the binding as
	// whole nanoseconds, which is exactly how time.Duration is represented.
	Interval time.Duration `json:"interval_nanos"`
}
