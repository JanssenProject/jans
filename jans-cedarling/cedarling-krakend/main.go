// SPDX-License-Identifier: Apache-2.0

package main

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"html"
	"io"
	"net/http"
	"strings"
)

// pluginName is the plugin name
var pluginName = "cedarling-krakend"

// HandlerRegisterer is the symbol the plugin loader will try to load. It must implement the Registerer interface
var HandlerRegisterer = registerer(pluginName)

func (r registerer) RegisterHandlers(f func(name string, handler func(context.Context, map[string]interface{}, http.Handler) (http.Handler, error))) {
	f(string(r), r.registerHandlers)
}

func createPayload(req *http.Request, namespace string) AuthZenPayload {
	bearer := req.Header.Get("Authorization")
	split_bearer := strings.Split(bearer, " ")
	token := split_bearer[1]
	var subject_properties = map[string]string{}
	subject_properties["access_token"] = token
	resource_properties := map[string]map[string]string{}
	resource_properties["header"] = map[string]string{}
	resource_properties["url"] = map[string]string{}
	resource_properties["url"]["host"] = req.Host
	resource_properties["url"]["path"] = req.URL.Path
	resource_properties["url"]["protocol"] = "http"
	payload := AuthZenPayload{
		Subject: Subject{
			Type:       "JWT",
			Id:         "cedarling",
			Properties: subject_properties,
		},
		Resource: Resource{
			Type:       fmt.Sprintf("%s::HTTP_Request", namespace),
			Id:         "some_id",
			Properties: resource_properties,
		},
		Action: Action{
			Name: fmt.Sprintf("%s::Action::\"%s\"", namespace, req.Method),
		},
	}
	return payload
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
	sidecar_endpoint, ok := config["sidecar_endpoint"].(string)
	if !ok {
		return h, errors.New("No sidecar endpoint provided")
	}
	namespace, ok := config["namespace"].(string)
	if !ok {
		return h, errors.New("No cedar namespace provided")
	}
	logger.Debug(fmt.Sprintf("The plugin is now protecting %s\n", path))

	customHandler := func(w http.ResponseWriter, req *http.Request) {

		// If the requested path is not what we defined, continue.
		if req.URL.Path != path {
			h.ServeHTTP(w, req)
		}

		// The path has to be hijacked:
		if req.Header.Get("Authorization") == "" {
			http.Error(w, "Authorization not found", http.StatusForbidden)
		} else {
			payload := createPayload(req, namespace)
			body_bytes, _ := json.Marshal(payload)
			response, err := http.Post(sidecar_endpoint, "application/json", bytes.NewReader(body_bytes))
			if err != nil {
				logger.Warning(err)
				http.Error(w, "Forbidden", http.StatusForbidden)
				return
			}
			defer response.Body.Close()
			response_bytes, err := io.ReadAll(response.Body)
			if err != nil {
				logger.Warning(err)
				http.Error(w, "Forbidden", http.StatusForbidden)
				return
			}
			var response_json AuthzenResponse
			json.Unmarshal(response_bytes, &response_json)
			if response_json.Decision == true {
				h.ServeHTTP(w, req)
			} else {
				http.Error(w, "Forbidden", http.StatusForbidden)
			}
		}
		logger.Debug("request:", html.EscapeString(req.URL.Path))
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
