package main

type registerer string

type Subject struct {
	Type       string            `json:"type"`
	Id         string            `json:"id"`
	Properties map[string]string `json:"properties,omitempty"`
}
type Resource struct {
	Type       string                       `json:"type"`
	Id         string                       `json:"id"`
	Properties map[string]map[string]string `json:"properties,omitempty"`
}

type Action struct {
	Name       string            `json:"name"`
	Properties map[string]string `json:"properties,omitempty"`
}

type AuthZenPayload struct {
	Subject  Subject           `json:"subject"`
	Resource Resource          `json:"resource"`
	Action   Action            `json:"action"`
	Context  map[string]string `json:"context,omitempty"`
}

type AuthzenResponse struct {
	Decision bool `json:"decision"`
}
