// SPDX-License-Identifier: Apache-2.0

package main

// #cgo LDFLAGS: -L. -lcedarling_go
import "C"

import (
	"context"
	"errors"
	"fmt"
	"net/http"
	"strings"

	"github.com/JanssenProject/jans/jans-cedarling/bindings/cedarling_go"

	"cedarling-krakend/internal/pathmatch"
)

// pluginName is the plugin name
var pluginName = "cedarling-krakend"

// HandlerRegisterer is the symbol the plugin loader will try to load. It must implement the Registerer interface
var HandlerRegisterer = registerer(pluginName)

func (r registerer) RegisterHandlers(f func(name string, handler func(context.Context, map[string]interface{}, http.Handler) (http.Handler, error))) {
	f(string(r), r.registerHandlers)
}

func createResource(req *http.Request, namespace string) cedarling_go.EntityData {
	resource := cedarling_go.EntityData{
		CedarMapping: cedarling_go.CedarEntityMapping{
			EntityType: fmt.Sprintf("%s::HTTP_Request", namespace),
			ID:         "krakend_request",
		},
		Payload: map[string]any{
			"header": map[string]string{},
			"url": map[string]string{
				"host":     req.Host,
				"path":     req.URL.Path,
				"protocol": "http",
			},
		},
	}
	return resource
}

func (r registerer) registerHandlers(_ context.Context, extra map[string]interface{}, h http.Handler) (http.Handler, error) {
	// If the plugin requires some configuration, it should be under the name of the plugin. E.g.:
	/*
	   "extra_config":{
	       "plugin/http-server":{
	           "name":["krakend-server-example"],
	           "krakend-server-example":{
	               "path": "/some-path"
	           }
	       }
	   }
	*/
	// KrakenD calls this only for plugins listed in "name", so the plugin is enabled here.
	// On any configuration error deny requests rather than returning an error: the host
	// ignores the error and keeps serving the endpoints unprotected.
	config, ok := extra[pluginName].(map[string]interface{})
	if !ok {
		return denyAll(errors.New("configuration not found")), nil
	}

	// Without a path the protected endpoint is unknown, so every endpoint is denied.
	protectedPath, ok := config["path"].(string)
	if !ok || strings.TrimSpace(protectedPath) == "" {
		return denyAll(errors.New("no path provided")), nil
	}
	namespace, ok := config["namespace"].(string)
	if !ok || strings.TrimSpace(namespace) == "" {
		return denyPath(protectedPath, h, errors.New("no cedar namespace provided")), nil
	}
	cedarling_instance, err := cedarling_go.NewCedarlingWithEnv(nil)
	if err != nil {
		return denyPath(protectedPath, h, fmt.Errorf("error initializing Cedarling: %s", err)), nil
	}
	logger.Debug(fmt.Sprintf("The plugin is now protecting %s\n", protectedPath))

	customHandler := func(w http.ResponseWriter, req *http.Request) {

		// If the requested path is not what we defined, continue.
		if !pathmatch.Matches(protectedPath, req.URL.Path) {
			h.ServeHTTP(w, req)
			return
		}

		// The path has to be hijacked:
		if req.Header.Get("Authorization") == "" {
			http.Error(w, "Authorization not found", http.StatusForbidden)
			return
		} else {
			bearer := req.Header.Get("Authorization")
			split_bearer := strings.SplitN(bearer, " ", 2)
			if len(split_bearer) != 2 || strings.TrimSpace(split_bearer[1]) == "" {
				http.Error(w, "Authorization not found", http.StatusForbidden)
				return
			}
			token := strings.TrimSpace(split_bearer[1])
			action := fmt.Sprintf("%s::Action::\"%s\"", namespace, req.Method)
			resource := createResource(req, namespace)
			request := cedarling_go.AuthorizeMultiIssuerRequest{
				Tokens: []cedarling_go.TokenInput{
					{
						Mapping: fmt.Sprintf("%s::Access_Token", namespace),
						Payload: token,
					},
				},
				Action:   action,
				Resource: resource,
				Context:  nil,
			}
			result, err := cedarling_instance.AuthorizeMultiIssuer(request)
			if err != nil {
				logger.Debug(fmt.Sprintf("%s", err))
				http.Error(w, "Forbidden", http.StatusForbidden)
				return
			}
			if result.Decision {
				h.ServeHTTP(w, req)
				return
			} else {
				http.Error(w, "Forbidden", http.StatusForbidden)
				return
			}
		}
	}
	return http.HandlerFunc(customHandler), nil
}

// denyAll rejects every request; used when the plugin is configured but cannot enforce authorization.
func denyAll(err error) http.Handler {
	logger.Error(fmt.Sprintf("[PLUGIN: %s] %s; denying all requests", pluginName, err))
	return http.HandlerFunc(func(w http.ResponseWriter, _ *http.Request) {
		http.Error(w, "Service Unavailable", http.StatusServiceUnavailable)
	})
}

// denyPath fails closed only for the protected path, leaving unrelated endpoints served.
func denyPath(protectedPath string, h http.Handler, err error) http.Handler {
	logger.Error(fmt.Sprintf("[PLUGIN: %s] %s; denying requests to %s", pluginName, err, protectedPath))
	return http.HandlerFunc(func(w http.ResponseWriter, req *http.Request) {
		if !pathmatch.Matches(protectedPath, req.URL.Path) {
			h.ServeHTTP(w, req)
			return
		}
		http.Error(w, "Service Unavailable", http.StatusServiceUnavailable)
	})
}

func main() {}

// This logger is replaced by the RegisterLogger method to load the one from KrakenD
var logger Logger = noopLogger{}

func (registerer) RegisterLogger(v interface{}) {
	l, ok := v.(Logger)
	if !ok {
		return
	}
	logger = l
	logger.Debug(fmt.Sprintf("[PLUGIN: %s] Logger loaded", HandlerRegisterer))
}

type Logger interface {
	Debug(v ...interface{})
	Info(v ...interface{})
	Warning(v ...interface{})
	Error(v ...interface{})
	Critical(v ...interface{})
	Fatal(v ...interface{})
}

// Empty logger implementation
type noopLogger struct{}

func (n noopLogger) Debug(_ ...interface{})    {}
func (n noopLogger) Info(_ ...interface{})     {}
func (n noopLogger) Warning(_ ...interface{})  {}
func (n noopLogger) Error(_ ...interface{})    {}
func (n noopLogger) Critical(_ ...interface{}) {}
func (n noopLogger) Fatal(_ ...interface{})    {}
