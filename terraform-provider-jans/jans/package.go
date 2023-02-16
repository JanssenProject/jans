// The jans package provides a client for the Janssen server. It's exposing
// functions for CRUD operations (or where applicalble a subset of it) for
// all resources that are intended to be managed by Terraform.
//
// All operations follow the same pattern:
// - get a token from the server for the respective scope
// - make a request to the server, providing the token and potential input data
// - parse the response and return the result
// All functions are using the context.Context to allow for cancellation and
// timeouts. Errors are wrapped using the fmt.Errorf function, which allows
// for better checking of the type of error.
//
// For each entity, there is a corresponding struct that represents the data.
// Those structs are annotated with the schema and json tags. This way, they
// can be used for communicating with the Janssen APIs, by matching the
// respective json fields. At the same time, the schema tags are used to
// map the fields to the Terraform schema. Entities, that are updated via
// PATCH requests, don't have the `omitempty` tag, as the server will reject
// the request if a field is missing.
package jans
