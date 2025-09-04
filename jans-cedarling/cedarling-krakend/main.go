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
	// The config variable contains all the keys you have defined in the configuration
	// if the key doesn't exists or is not a map the plugin returns an error and the default handler
	config, ok := extra[pluginName].(map[string]interface{})
	if !ok {
		return h, errors.New("configuration not found")
	}

	// The plugin will look for this path:
	path, ok := config["path"].(string)
	if !ok {
		return h, errors.New("No path provided")
	}
	namespace, ok := config["namespace"].(string)
	if !ok {
		return h, errors.New("No cedar namespace provided")
	}
	cedarling_instance, err := cedarling_go.NewCedarlingWithEnv(nil)
	if err != nil {
		return h, errors.New(fmt.Sprintf("Error initializing Cedarling: %s", err))
	}
	logger.Debug(fmt.Sprintf("The plugin is now protecting %s\n", path))

	customHandler := func(w http.ResponseWriter, req *http.Request) {

		// If the requested path is not what we defined, continue.
		if req.URL.Path != path {
			h.ServeHTTP(w, req)
			return
		}

		// The path has to be hijacked:
		if req.Header.Get("Authorization") == "" {
			http.Error(w, "Authorization not found", http.StatusForbidden)
			return
		} else {
			bearer := req.Header.Get("Authorization")
			split_bearer := strings.Split(bearer, " ")
			token := split_bearer[1]
			action := fmt.Sprintf("%s::Action::\"%s\"", namespace, req.Method)
			resource := createResource(req, namespace)
			request := cedarling_go.Request{
				Tokens: map[string]string{
					"access_token": token,
				},
				Action:   action,
				Resource: resource,
				Context:  nil,
			}
			result, err := cedarling_instance.Authorize(request)
			if err != nil {
				logger.Debug(fmt.Sprintf("%s", err))
				http.Error(w, "Forbidden", http.StatusForbidden)
				return
			}
			if result.Decision == true {
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
