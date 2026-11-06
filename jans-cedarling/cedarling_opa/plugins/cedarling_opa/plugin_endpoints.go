package cedarlingopa

import (
	"encoding/json"
	"errors"
	"fmt"
	"mime"
	"net/http"
	"strings"
)

func writeError(w http.ResponseWriter, status int, message string) {
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(map[string]any{"error": message})
}

func validateInput(subject *Entity, action *Action, resource *Entity) error {
	var missing []string
	if subject == nil {
		missing = append(missing, "subject")
	}
	if action == nil {
		missing = append(missing, "action")
	}
	if resource == nil {
		missing = append(missing, "resource")
	}
	if len(missing) > 0 {
		missing_fields := strings.Join(missing, ", ")
		return fmt.Errorf("Invalid input: missing %s", missing_fields)
	}
	return nil
}

func (p *CedarPlugin) MetaDataHandler(w http.ResponseWriter, r *http.Request) {
	request_id := r.Header.Get("X-Request-ID")
	if request_id != "" {
		w.Header().Add("X-Request-ID", request_id)
	}
	if r.Method != "GET" {
		writeError(w, http.StatusMethodNotAllowed, "Method not allowed")
		return
	}
	w.Header().Add("Content-Type", "application/json")
	globalInstanceMu.RLock()
	plugin := globalInstance
	globalInstanceMu.RUnlock()
	if plugin == nil {
		writeError(w, http.StatusServiceUnavailable, "Plugin unavailable")
		return
	}
	metadata := PDPMetadata{
		PolicyDecisionPoint:       p.config.Host,
		AccessEvaluationEndpoint:  fmt.Sprintf("%s/access/v1/evaluation", p.config.Host),
		AccessEvaluationsEndpoint: fmt.Sprintf("%s/access/v1/evaluations", p.config.Host),
	}
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
		writeError(w, http.StatusBadRequest, "Invalid Request")
		if err != nil {
			p.logger.Info(err.Error())
		}
		return
	}
	w.Header().Add("Content-Type", "application/json")
	var request EvaluationRequest
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		writeError(w, http.StatusBadRequest, "Invalid Request")
		p.logger.Info(err.Error())
		return
	}
	if err = validateInput(request.Subject, request.Action, request.Resource); err != nil {
		writeError(w, http.StatusBadRequest, err.Error())
		return
	}
	p.writeResponse(w, p.evaluateOne(evaluationInput{
		subject:  request.Subject,
		action:   request.Action,
		resource: request.Resource,
		context:  request.Context,
	}))
}

// evaluationInput is one access evaluation, with the batch defaults applied.
type evaluationInput struct {
	subject  *Entity
	action   *Action
	resource *Entity
	context  map[string]any
}

// evaluateOne returns before the response is written, so the read lock is not
// held while a client reads it. A slow client would otherwise keep the lock, and
// once Reconfigure waits for the write lock no new request could take a read lock.
func (p *CedarPlugin) evaluateOne(input evaluationInput) EvaluationResponse {
	p.mtx.RLock()
	defer p.mtx.RUnlock()
	return *p.buildEvaluationResponse(input.subject, input.action, input.resource, input.context, p.config.Evaluation_Logic)
}

// evaluateBatch takes one read lock for the whole batch: re-acquiring it per item
// while a writer waits in Reconfigure would deadlock. Like evaluateOne, it returns
// before the response is written.
func (p *CedarPlugin) evaluateBatch(inputs []evaluationInput, semantic EvaluationsSemantic) []EvaluationResponse {
	p.mtx.RLock()
	defer p.mtx.RUnlock()
	responses := []EvaluationResponse{}
	for _, input := range inputs {
		response := p.buildEvaluationResponse(input.subject, input.action, input.resource, input.context, p.config.Evaluation_Logic)
		responses = append(responses, *response)
		if (semantic == DenyOnFirstDeny && !response.Decision) || (semantic == PermitOnFirstPermit && response.Decision) {
			break
		}
	}
	return responses
}

func (p *CedarPlugin) writeResponse(w http.ResponseWriter, response any) {
	if err := json.NewEncoder(w).Encode(response); err != nil {
		writeError(w, http.StatusInternalServerError, "Something went wrong")
		p.logger.Info(err.Error())
	}
}

func mergeEntityHelper(eval *Entity, parent *Entity, name string) (*Entity, error) {
	if eval != nil {
		return eval, nil
	}
	if parent != nil {
		return parent, nil
	}
	return nil, fmt.Errorf("Invalid request: missing %s", name)
}

func (p *CedarPlugin) AccessEvaluationsHandler(w http.ResponseWriter, r *http.Request) {
	requestId := r.Header.Get("X-Request-ID")
	if requestId != "" {
		w.Header().Add("X-Request-ID", requestId)
	}
	contentType := r.Header.Get("Content-Type")
	mediaType, _, err := mime.ParseMediaType(contentType)
	if r.Method != "POST" || err != nil || mediaType != "application/json" {
		writeError(w, http.StatusBadRequest, "Invalid Request")
		if err != nil {
			p.logger.Info(err.Error())
		}
		return
	}
	w.Header().Add("Content-Type", "application/json")
	var request MultipleEvaluationRequest
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		writeError(w, http.StatusBadRequest, "Invalid Request")
		p.logger.Info(err.Error())
		return
	}
	if request.Evaluations == nil {
		// handle same as single evaluation request
		if err = validateInput(request.Subject, request.Action, request.Resource); err != nil {
			writeError(w, http.StatusBadRequest, err.Error())
			return
		}
		p.writeResponse(w, p.evaluateOne(evaluationInput{
			subject:  request.Subject,
			action:   request.Action,
			resource: request.Resource,
			context:  request.Context,
		}))
		return
	}
	inputs, err := resolveEvaluations(request)
	if err != nil {
		writeError(w, http.StatusBadRequest, err.Error())
		return
	}
	semantic := ExecuteAll
	if request.Options != nil {
		semantic = request.Options.EvaluationSemantic
	}
	p.writeResponse(w, p.evaluateBatch(inputs, semantic))
}

// resolveEvaluations fills each evaluation's missing subject, action and resource
// from the request, so a batch with an incomplete item is rejected before any is evaluated.
func resolveEvaluations(request MultipleEvaluationRequest) ([]evaluationInput, error) {
	inputs := make([]evaluationInput, 0, len(request.Evaluations))
	for _, eval := range request.Evaluations {
		subject, err := mergeEntityHelper(eval.Subject, request.Subject, "subject")
		if err != nil {
			return nil, err
		}
		resource, err := mergeEntityHelper(eval.Resource, request.Resource, "resource")
		if err != nil {
			return nil, err
		}
		action := eval.Action
		if action == nil {
			action = request.Action
		}
		if action == nil {
			return nil, errors.New("Invalid request: missing action")
		}
		inputs = append(inputs, evaluationInput{
			subject:  subject,
			action:   action,
			resource: resource,
			context:  eval.Context,
		})
	}
	return inputs, nil
}
