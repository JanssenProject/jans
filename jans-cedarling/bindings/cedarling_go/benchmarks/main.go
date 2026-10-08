// Go cross-platform bench harness. See bindings/benchmarks/CONTRACT.md.
// Requires LD_LIBRARY_PATH pointing at libcedarling_go.so at runtime.
package main

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"runtime"
	"sort"
	"time"

	cedarling "github.com/JanssenProject/jans/jans-cedarling/bindings/cedarling_go"
)

const bindingName = "go"

type manifest struct {
	IterationPolicy struct {
		WarmupIters  int `json:"warmup_iters"`
		MeasureIters int `json:"measure_iters"`
	} `json:"iteration_policy"`
	Scenarios []scenario `json:"scenarios"`
}

type scenario struct {
	ID              string         `json:"id"`
	Kind            string         `json:"kind"`
	ItemCount       int            `json:"item_count"`
	PolicyStoreFn   string         `json:"policy_store_fn"`
	ConfigOverrides map[string]any `json:"config_overrides"`
	Principal       map[string]any `json:"principal"`
	Action          string         `json:"action"`
	Resource        map[string]any `json:"resource"`
	Context         string         `json:"context"`
	Tokens          []struct {
		Mapping string `json:"mapping"`
		Payload string `json:"payload"`
	} `json:"tokens"`
	MockOpRequired bool `json:"mock_op_required"`
}

type result struct {
	Binding     string   `json:"binding"`
	Scenario    string   `json:"scenario"`
	Iter        *int     `json:"iter,omitempty"`
	MeanNs      *int64   `json:"mean_ns,omitempty"`
	P50Ns       *int64   `json:"p50_ns,omitempty"`
	P95Ns       *int64   `json:"p95_ns,omitempty"`
	P99Ns       *int64   `json:"p99_ns,omitempty"`
	MinNs       *int64   `json:"min_ns,omitempty"`
	MaxNs       *int64   `json:"max_ns,omitempty"`
	AllocsPerOp *float64 `json:"allocs_per_op"`
	Status      string   `json:"status"`
	Reason      string   `json:"reason,omitempty"`
}

func main() {
	repoRoot := resolveRepoRoot()
	manifestPath := filepath.Join(repoRoot, "bindings", "benchmarks", "fixtures", "scenarios.json")
	raw, err := os.ReadFile(manifestPath)
	if err != nil {
		fmt.Fprintf(os.Stderr, "failed to read manifest %s: %v\n", manifestPath, err)
		os.Exit(1)
	}
	var m manifest
	if err := json.Unmarshal(raw, &m); err != nil {
		fmt.Fprintf(os.Stderr, "failed to parse manifest: %v\n", err)
		os.Exit(1)
	}
	for _, s := range m.Scenarios {
		runScenario(s, repoRoot, m.IterationPolicy.WarmupIters, m.IterationPolicy.MeasureIters)
	}
}

func resolveRepoRoot() string {
	if v := os.Getenv("CEDARLING_REPO_ROOT"); v != "" {
		if abs, err := filepath.Abs(v); err == nil {
			return abs
		}
	}
	// Default cwd is bindings/cedarling_go when invoked via `go run ./benchmarks/`.
	wd, _ := os.Getwd()
	abs, _ := filepath.Abs(filepath.Join(wd, "..", ".."))
	return abs
}

func runScenario(s scenario, repoRoot string, warmupIters, measureIters int) {
	if s.MockOpRequired {
		emit(result{Binding: bindingName, Scenario: s.ID, Status: "skipped", Reason: "mock_op_unavailable"})
		return
	}

	defer func() {
		if r := recover(); r != nil {
			emit(result{Binding: bindingName, Scenario: s.ID, Status: "skipped", Reason: fmt.Sprintf("panic:%v", r)})
		}
	}()

	config := buildConfig(s, repoRoot)
	instance, err := cedarling.NewCedarling(config)
	if err != nil {
		emit(result{Binding: bindingName, Scenario: s.ID, Status: "skipped", Reason: fmt.Sprintf("init:%v", err)})
		return
	}
	defer instance.ShutDown()

	fn, err := buildBenchFn(instance, s)
	if err != nil {
		emit(result{Binding: bindingName, Scenario: s.ID, Status: "skipped", Reason: fmt.Sprintf("build_fn:%v", err)})
		return
	}

	allowed, err := fn()
	if err != nil {
		emit(result{Binding: bindingName, Scenario: s.ID, Status: "skipped", Reason: fmt.Sprintf("validation_error:%v", err)})
		return
	}
	if !allowed {
		emit(result{Binding: bindingName, Scenario: s.ID, Status: "skipped", Reason: "validation_deny"})
		return
	}

	for i := 0; i < warmupIters; i++ {
		if _, err := fn(); err != nil {
			emit(result{Binding: bindingName, Scenario: s.ID, Status: "skipped", Reason: fmt.Sprintf("warmup_loop:%v", err)})
			return
		}
	}

	runtime.GC()
	samples := make([]int64, measureIters)
	for i := 0; i < measureIters; i++ {
		t0 := time.Now()
		_, err := fn()
		samples[i] = time.Since(t0).Nanoseconds()
		if err != nil {
			emit(result{Binding: bindingName, Scenario: s.ID, Status: "skipped", Reason: fmt.Sprintf("measure_loop:%v", err)})
			return
		}
	}

	// Allocs measured OUTSIDE the timed loop via runtime.MemStats diff.
	runtime.GC()
	var msStart, msEnd runtime.MemStats
	runtime.ReadMemStats(&msStart)
	for i := 0; i < measureIters; i++ {
		if _, err := fn(); err != nil {
			emit(result{Binding: bindingName, Scenario: s.ID, Status: "skipped", Reason: fmt.Sprintf("allocs_loop:%v", err)})
			return
		}
	}
	runtime.ReadMemStats(&msEnd)
	allocs := float64(msEnd.Mallocs-msStart.Mallocs) / float64(measureIters)
	emit(buildOkResult(s.ID, samples, allocs))
}

func buildConfig(s scenario, repoRoot string) map[string]any {
	config := make(map[string]any, len(s.ConfigOverrides)+1)
	for k, v := range s.ConfigOverrides {
		config[k] = v
	}
	config["CEDARLING_POLICY_STORE_LOCAL_FN"] = filepath.Join(repoRoot, s.PolicyStoreFn)
	return config
}

// Returns a closure that authorizes once and yields the decision so the runner
// can validate before warmup. A scenario that authorizes cleanly but returns
// Deny would otherwise be timed silently for measureIters iterations.
func buildBenchFn(instance *cedarling.Cedarling, s scenario) (func() (bool, error), error) {
	switch s.Kind {
	case "unsigned":
		var principal *cedarling.EntityData
		if s.Principal != nil {
			p, err := entityDataFromMap(s.Principal)
			if err != nil {
				return nil, err
			}
			principal = &p
		}
		resource, err := entityDataFromMap(s.Resource)
		if err != nil {
			return nil, err
		}
		ctx, err := parseContext(s.Context)
		if err != nil {
			return nil, err
		}
		req := cedarling.RequestUnsigned{
			Principal: principal,
			Action:    s.Action,
			Resource:  resource,
			Context:   ctx,
		}
		return func() (bool, error) {
			r, err := instance.AuthorizeUnsigned(req)
			if err != nil {
				return false, err
			}
			return r.Decision, nil
		}, nil
	case "multi_issuer":
		tokens := make([]cedarling.TokenInput, 0, len(s.Tokens))
		for _, t := range s.Tokens {
			tokens = append(tokens, cedarling.TokenInput{Mapping: t.Mapping, Payload: t.Payload})
		}
		resource, err := entityDataFromMap(s.Resource)
		if err != nil {
			return nil, err
		}
		ctx, err := parseContext(s.Context)
		if err != nil {
			return nil, err
		}
		req := cedarling.AuthorizeMultiIssuerRequest{
			Tokens:   tokens,
			Resource: resource,
			Action:   s.Action,
			Context:  ctx,
		}
		return func() (bool, error) {
			r, err := instance.AuthorizeMultiIssuer(req)
			if err != nil {
				return false, err
			}
			return r.Decision, nil
		}, nil
	case "unsigned_batch":
		if s.ItemCount <= 0 {
			return nil, fmt.Errorf("item_count must be > 0 for batch scenario")
		}
		var principal *cedarling.EntityData
		if s.Principal != nil {
			p, err := entityDataFromMap(s.Principal)
			if err != nil {
				return nil, err
			}
			principal = &p
		}
		items, err := buildBatchItems(s)
		if err != nil {
			return nil, err
		}
		req := cedarling.BatchAuthorizeUnsignedRequest{
			Principal: principal,
			Items:     items,
		}
		return func() (bool, error) {
			r, err := instance.AuthorizeUnsignedBatch(req)
			if err != nil {
				return false, err
			}
			return allAllow(r.Results), nil
		}, nil
	case "multi_issuer_batch":
		if s.ItemCount <= 0 {
			return nil, fmt.Errorf("item_count must be > 0 for batch scenario")
		}
		tokens := make([]cedarling.TokenInput, 0, len(s.Tokens))
		for _, t := range s.Tokens {
			tokens = append(tokens, cedarling.TokenInput{Mapping: t.Mapping, Payload: t.Payload})
		}
		items, err := buildBatchItems(s)
		if err != nil {
			return nil, err
		}
		req := cedarling.BatchAuthorizeMultiIssuerRequest{
			Tokens: tokens,
			Items:  items,
		}
		return func() (bool, error) {
			r, err := instance.AuthorizeMultiIssuerBatch(req)
			if err != nil {
				return false, err
			}
			return allAllowMulti(r.Results), nil
		}, nil
	default:
		return nil, fmt.Errorf("unknown scenario kind %q", s.Kind)
	}
}

// Clones the fixture resource item_count times with distinct entity ids
// ({base}-0..{base}-N-1) so the batch reflects N distinct authorizations.
func buildBatchItems(s scenario) ([]cedarling.BatchItem, error) {
	baseResource, err := entityDataFromMap(s.Resource)
	if err != nil {
		return nil, err
	}
	ctx, err := parseContext(s.Context)
	if err != nil {
		return nil, err
	}
	baseID := baseResource.CedarMapping.ID
	items := make([]cedarling.BatchItem, s.ItemCount)
	for i := 0; i < s.ItemCount; i++ {
		r := baseResource
		r.Payload = clonePayload(baseResource.Payload)
		r.CedarMapping = cedarling.CedarEntityMapping{
			EntityType: baseResource.CedarMapping.EntityType,
			ID:         fmt.Sprintf("%s-%d", baseID, i),
		}
		items[i] = cedarling.BatchItem{
			Resource: r,
			Action:   s.Action,
			Context:  ctx,
		}
	}
	return items, nil
}

func clonePayload(src map[string]any) map[string]any {
	dst := make(map[string]any, len(src))
	for k, v := range src {
		dst[k] = v
	}
	return dst
}

func allAllow(rs []cedarling.BatchItemUnsignedResult) bool {
	for _, r := range rs {
		if !r.IsOk() || !r.Ok.Decision {
			return false
		}
	}
	return len(rs) > 0
}

func allAllowMulti(rs []cedarling.BatchItemMultiIssuerResult) bool {
	for _, r := range rs {
		if !r.IsOk() || !r.Ok.Decision {
			return false
		}
	}
	return len(rs) > 0
}

func entityDataFromMap(m map[string]any) (cedarling.EntityData, error) {
	mappingRaw, ok := m["cedar_entity_mapping"].(map[string]any)
	if !ok {
		return cedarling.EntityData{}, fmt.Errorf("missing cedar_entity_mapping")
	}
	entityType, _ := mappingRaw["entity_type"].(string)
	id, _ := mappingRaw["id"].(string)
	payload := make(map[string]any, len(m)-1)
	for k, v := range m {
		if k == "cedar_entity_mapping" {
			continue
		}
		payload[k] = v
	}
	return cedarling.EntityData{
		CedarMapping: cedarling.CedarEntityMapping{EntityType: entityType, ID: id},
		Payload:      payload,
	}, nil
}

func parseContext(s string) (any, error) {
	if s == "" || s == "{}" {
		return map[string]any{}, nil
	}
	var v any
	if err := json.Unmarshal([]byte(s), &v); err != nil {
		return nil, err
	}
	return v, nil
}

func buildOkResult(id string, samples []int64, allocs float64) result {
	sorted := make([]int64, len(samples))
	copy(sorted, samples)
	sort.Slice(sorted, func(i, j int) bool { return sorted[i] < sorted[j] })

	var sum int64
	for _, v := range samples {
		sum += v
	}
	iter := len(samples)
	mean := sum / int64(iter)
	min := sorted[0]
	max := sorted[len(sorted)-1]
	p50 := sorted[int(float64(iter)*0.50)]
	p95 := sorted[int(float64(iter)*0.95)]
	p99 := sorted[int(float64(iter)*0.99)]
	return result{
		Binding:     bindingName,
		Scenario:    id,
		Iter:        &iter,
		MeanNs:      &mean,
		P50Ns:       &p50,
		P95Ns:       &p95,
		P99Ns:       &p99,
		MinNs:       &min,
		MaxNs:       &max,
		AllocsPerOp: &allocs,
		Status:      "ok",
	}
}

func emit(r result) {
	b, err := json.Marshal(r)
	if err != nil {
		fmt.Fprintf(os.Stderr, "marshal failed: %v\n", err)
		return
	}
	fmt.Println(string(b))
}
