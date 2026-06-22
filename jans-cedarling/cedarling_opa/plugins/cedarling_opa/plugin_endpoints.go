package cedarlingopa

import (
	"encoding/json"
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
	w.WriteHeader(200)
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
	w.WriteHeader(200)
	response := p.buildEvaluationResponse(request.Subject, request.Action, request.Resource, request.Context, p.config.Evaluation_Logic)
	if err := json.NewEncoder(w).Encode(response); err != nil {
		writeError(w, http.StatusInternalServerError, "Something went wrong")
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
		writeError(w, http.StatusBadRequest, "Invalid Request")
		p.logger.Info(err.Error())
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
		w.WriteHeader(200)
		response := p.buildEvaluationResponse(request.Subject, request.Action, request.Resource, request.Context, p.config.Evaluation_Logic)
		if err := json.NewEncoder(w).Encode(response); err != nil {
			writeError(w, http.StatusInternalServerError, "Something went wrong")
			p.logger.Info(err.Error())
			return
		}
		return
	} else {
		responseList := []EvaluationResponse{}
		if request.Options == nil {
			request.Options = &Option{
				EvaluationSemantic: ExecuteAll,
			}
		}
		for _, eval := range request.Evaluations {
			subject, err := mergeEntityHelper(eval.Subject, request.Subject, "subject")
			if err != nil {
				writeError(w, http.StatusBadRequest, err.Error())
				return
			}
			resource, err := mergeEntityHelper(eval.Resource, request.Resource, "resource")
			if err != nil {
				writeError(w, http.StatusBadRequest, err.Error())
				return
			}
			action := eval.Action
			if action == nil {
				if request.Action == nil {
					writeError(w, http.StatusBadRequest, "Invalid request: missing action")
					return
				} else {
					action = request.Action
				}
			}
			context := eval.Context
			response := p.buildEvaluationResponse(subject, action, resource, context, p.config.Evaluation_Logic)
			responseList = append(responseList, *response)
			if request.Options.EvaluationSemantic == DenyOnFirstDeny {
				if !response.Decision {
					break
				}
			} else if request.Options.EvaluationSemantic == PermitOnFirstPermit {
				if response.Decision {
					break
				}
			}
		}
		if err := json.NewEncoder(w).Encode(responseList); err != nil {
			writeError(w, http.StatusInternalServerError, "Something went wrong")
			p.logger.Info(err.Error())
			return
		}
	}
}
