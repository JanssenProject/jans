package cedarlingopa

import (
	"encoding/json"
	"fmt"
	"mime"
	"net/http"

	"github.com/JanssenProject/jans/jans-cedarling/bindings/cedarling_go"
)

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

func createCedarlingRequestFromAuthZen(tokens_raw []byte, resource_raw []byte, action string, context map[string]any) (*cedarling_go.AuthorizeMultiIssuerRequest, error) {
	var tokens TokenList
	if err := json.Unmarshal(tokens_raw, &tokens); err != nil {
		return nil, err
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
	if err := json.Unmarshal(resource_raw, &resourceEntity); err != nil {
		return nil, err
	}
	cedarling_request := cedarling_go.AuthorizeMultiIssuerRequest{
		Tokens:   tokenEntities,
		Action:   action,
		Resource: resourceEntity,
		Context:  context,
	}
	return &cedarling_request, nil
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
	result, err := p.Evaluate(&request.Subject, request.Action.Name, &request.Resource, request.Context)
	w.WriteHeader(200)
	response := EvaluationResponse{}
	if err != nil {
		if result == nil {
			// something else was wrong
			w.WriteHeader(http.StatusInternalServerError)
			json.NewEncoder(w).Encode(map[string]any{"error": "Something went wrong"})
			p.logger.Info(err.Error())
			return
		} else {
			// error during authorization
			response.Decision = false
			response.Context = map[string]any{
				"error": err.Error(),
			}
		}
	} else {
		response.Decision = result.Decision
	}
	if err := json.NewEncoder(w).Encode(response); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]any{"error": "Something went wrong"})
		p.logger.Info(err.Error())
		return
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
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]any{"error": "Invalid Request"})
		p.logger.Info(err.Error())
		return
	}
	w.Header().Add("Content-Type", "application/json")
	var request MultipleEvaluationRequest
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]any{"error": "Invalid Request"})
		p.logger.Info(err.Error())
		return
	}
	if request.Evaluation == nil {
		// handle same as single evaluation request
		result, err := p.Evaluate(request.Subject, request.Action.Name, request.Resource, request.Context)
		w.WriteHeader(200)
		response := EvaluationResponse{}
		if err != nil {
			if result == nil {
				// something else was wrong
				w.WriteHeader(http.StatusInternalServerError)
				json.NewEncoder(w).Encode(map[string]any{"error": "Something went wrong"})
				p.logger.Info(err.Error())
				return
			} else {
				// error during authorization
				response.Decision = false
				response.Context = map[string]any{
					"error": err.Error(),
				}
			}
		} else {
			response.Decision = result.Decision
		}

		if err := json.NewEncoder(w).Encode(response); err != nil {
			w.WriteHeader(http.StatusInternalServerError)
			json.NewEncoder(w).Encode(map[string]any{"error": "Something went wrong"})
			p.logger.Info(err.Error())
			return
		}
		return
	}
	responseList := []EvaluationResponse{}
	for _, eval := range request.Evaluation {
		subject, err := mergeEntityHelper(eval.Subject, request.Subject, "subject")
		if err != nil {
			w.WriteHeader(http.StatusBadRequest)
			json.NewEncoder(w).Encode(map[string]any{"error": err.Error()})
			return
		}
		resource, err := mergeEntityHelper(eval.Resource, request.Resource, "resource")
		if err != nil {
			w.WriteHeader(http.StatusBadRequest)
			json.NewEncoder(w).Encode(map[string]any{"error": err.Error()})
			return
		}
		action := eval.Action
		if action == nil {
			w.WriteHeader(http.StatusBadRequest)
			json.NewEncoder(w).Encode(map[string]any{"error": "Invalid request: missing action"})
			return
		}
		context := eval.Context
		response := EvaluationResponse{}
		result, err := p.Evaluate(subject, action.Name, resource, context)
		if err != nil {
			if result == nil {
				// something else was wrong
				w.WriteHeader(http.StatusInternalServerError)
				json.NewEncoder(w).Encode(map[string]any{"error": "Something went wrong"})
				p.logger.Info(err.Error())
				return
			} else {
				// error during authorization
				response.Decision = false
				response.Context = map[string]any{
					"error": err.Error(),
				}
			}
		} else {
			response.Decision = result.Decision
		}
		responseList = append(responseList, response)
	}
	if err := json.NewEncoder(w).Encode(responseList); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]any{"error": "Something went wrong"})
		p.logger.Info(err.Error())
		return
	}
}
