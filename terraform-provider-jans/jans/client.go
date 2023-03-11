package jans

import (
	"bytes"
	"context"
	"crypto/tls"
	"encoding/json"
	"fmt"
	"io"
	"reflect"
	"sort"

	"net/http"
	"net/url"
)

var (
	ErrorBadRequest = fmt.Errorf("bad request")
	ErrorNotFound   = fmt.Errorf("not found")
)

// requestParams is used as a conveneince struct to pass parameters to the
// request method.
type requestParams struct {
	method      string
	path        string
	accept      string
	contentType string
	token       string
	payload     []byte
	resp        any
	queryParams map[string]string
}

// Client is the client via which we can interact with all
// necessary Jans APIs.
type Client struct {
	host          string
	clientId      string
	clientSecret  string
	skipTLSVerify bool
}

// NewClient creates a new client, which will connect to a server
// at the provided host, using the given credentials.
func NewClient(host, clientId, clientSecret string) (*Client, error) {
	return &Client{
		host:          host,
		clientId:      clientId,
		clientSecret:  clientSecret,
		skipTLSVerify: false,
	}, nil
}

// NewInsecureClient creates a new client, which will connect to a server
// at the provided host, using the given credentials. Unlike NewClient,
// this client will skip TLS verification. This should only be used for
// development and testing purposes.
func NewInsecureClient(host, clientId, clientSecret string) (*Client, error) {
	return &Client{
		host:          host,
		clientId:      clientId,
		clientSecret:  clientSecret,
		skipTLSVerify: true,
	}, nil
}

// getToken performs a POST request to the token endpoint for the given scope.
// The call uses the credentials stored in the client.
func (c *Client) getToken(ctx context.Context, scope string) (string, error) {

	if c.host == "" {
		return "", fmt.Errorf("host is not set")
	}

	urlString := fmt.Sprintf("%s/jans-auth/restv1/token", c.host)

	params := url.Values{}
	params.Add("grant_type", "client_credentials")
	params.Add("scope", scope)

	req, err := http.NewRequestWithContext(ctx, "POST", urlString, bytes.NewReader([]byte(params.Encode())))
	if err != nil {
		return "", fmt.Errorf("could not create request: %w", err)
	}

	req.SetBasicAuth(c.clientId, c.clientSecret)
	req.Header.Add("Accept", "application/json")
	req.Header.Add("Content-Type", "application/x-www-form-urlencoded")

	tr := &http.Transport{}
	if c.skipTLSVerify {
		tr.TLSClientConfig = &tls.Config{InsecureSkipVerify: true}
	}
	client := &http.Client{Transport: tr}
	resp, err := client.Do(req)
	if err != nil {
		return "", fmt.Errorf("could not perform request: %w", err)
	}

	data, err := io.ReadAll(resp.Body)
	if err != nil {
		return "", fmt.Errorf("could not read response body: %w", err)
	}

	if resp.StatusCode != 200 {
		// fmt.Printf("\n\nResponse data:\n%s\n\n", string(data))
		return "", fmt.Errorf("did not get correct response code: %v", resp.Status)
	}

	type tokenResponse struct {
		AccessToken string `json:"access_token"`
		Scope       string `json:"scope"`
		TokenType   string `json:"token_type"`
		ExpiresIn   int    `json:"expires_in"`
	}

	token := &tokenResponse{}

	if err = json.Unmarshal(data, token); err != nil {
		return "", fmt.Errorf("could not unmarshal response: %w", err)
	}

	if token.Scope != scope {
		return "", fmt.Errorf("scope not granted: %s", scope)
	}

	return token.AccessToken, nil
}

// get performs an HTTP GET request to the given path, using the given token.
// The response data is unmarshaled into the provided response value, which
// has to be of a pointer type.
func (c *Client) get(ctx context.Context, path, token string, resp any, queryParams ...map[string]string) error {

	params := requestParams{
		method:      "GET",
		path:        path,
		contentType: "application/json",
		accept:      "application/json",
		token:       token,
		resp:        resp,
	}

	if len(queryParams) > 0 {
		params.queryParams = queryParams[0]
	}

	return c.request(ctx, params)
}

// get performs an HTTP GET request to the given path, using the given token.
// The response data is unmarshaled into the provided response value, which
// has to be of a pointer type.
func (c *Client) getScim(ctx context.Context, path, token string, resp any) error {

	params := requestParams{
		method:      "GET",
		path:        path,
		contentType: "application/json",
		accept:      "application/scim+json",
		token:       token,
		resp:        resp,
	}

	return c.request(ctx, params)
}

// patch performs an HTTP PATCH request to the given path, using the given
// token and the provided list of patch requests.
func (c *Client) patch(ctx context.Context, path, token string, req []PatchRequest) error {

	payload, err := json.Marshal(req)
	if err != nil {
		return fmt.Errorf("could not marshal request: %w", err)
	}

	params := requestParams{
		method:      "PATCH",
		path:        path,
		contentType: "application/json-patch+json",
		accept:      "application/json",
		token:       token,
		payload:     payload,
	}

	return c.request(ctx, params)
}

// put performs an HTTP PUT request to the given path, using the given token
// and the provided request entity, which is marshaled into JSON. The response
// data is unmarshaled into the provided response value, which has to be of
// a pointer type.
func (c *Client) put(ctx context.Context, path, token string, req, resp any) error {

	payload, err := json.Marshal(req)
	if err != nil {
		return fmt.Errorf("could not marshal request: %w", err)
	}

	params := requestParams{
		method:      "PUT",
		path:        path,
		contentType: "application/json",
		accept:      "application/json",
		token:       token,
		payload:     payload,
		resp:        resp,
	}

	return c.request(ctx, params)
}

// putText performs an HTTP PUT request to the given path, using the given token
// and the provided request entity, which is marshaled into JSON. The response
// data is unmarshaled into the provided response value, which has to be of
// a pointer type. Unlike put, putText uses the "text/plain" content type.
func (c *Client) putText(ctx context.Context, path, token, req string, resp any) error {

	params := requestParams{
		method:      "PUT",
		path:        path,
		contentType: "text/plain",
		accept:      "application/json",
		token:       token,
		payload:     []byte(req),
		resp:        resp,
	}

	return c.request(ctx, params)
}

// post performs an HTTP POST request to the given path, using the given token
// and the provided request entity, which is marshaled into JSON. The response
// data is unmarshaled into the provided response value, which has to be of
// a pointer type.
func (c *Client) post(ctx context.Context, path, token string, req, resp any) error {

	payload, err := json.Marshal(req)
	if err != nil {
		return fmt.Errorf("could not marshal request: %w", err)
	}

	params := requestParams{
		method:      "POST",
		path:        path,
		contentType: "application/json",
		accept:      "application/json",
		token:       token,
		payload:     payload,
		resp:        resp,
	}

	return c.request(ctx, params)
}

// delete performs an HTTP DELETE request to the given path, using the given
// token.
func (c *Client) delete(ctx context.Context, path, token string) error {

	params := requestParams{
		method:      "DELETE",
		path:        path,
		contentType: "application/json",
		accept:      "application/json",
		token:       token,
	}

	return c.request(ctx, params)
}

// delete performs an HTTP DELETE request to the given path, using the given
// token.
func (c *Client) deleteEntity(ctx context.Context, path, token string, entity any) error {

	payload, err := json.Marshal(entity)
	if err != nil {
		return fmt.Errorf("could not marshal request: %w", err)
	}

	req := requestParams{
		method:      "DELETE",
		path:        path,
		contentType: "application/json",
		accept:      "application/json",
		token:       token,
		payload:     payload,
	}

	return c.request(ctx, req)
}

// request performs an HTTP request of the requested method to the given path.
// The token is used as authorization header. If the request entity is not nil,
// it is marshaled into JSON and used as request body. If the response value
// is not nil, the response data is unmarshaled into it. The response value
// has to be of a pointer type.
func (c *Client) request(ctx context.Context, params requestParams) error {

	if c.host == "" {
		return fmt.Errorf("host is not set")
	}

	if params.path == "" {
		return fmt.Errorf("no request path provided")
	}

	url := fmt.Sprintf("%s%s", c.host, params.path)

	// fmt.Printf("URL: %s %s\n", params.method, url)
	// fmt.Printf("Payload:\n%s\n", string(params.payload))

	req, err := http.NewRequestWithContext(ctx, params.method, url, bytes.NewReader(params.payload))
	if err != nil {
		return fmt.Errorf("could not create request: %w", err)
	}

	if len(params.queryParams) != 0 {
		q := req.URL.Query()
		for k, v := range params.queryParams {
			q.Add(k, v)
		}
		req.URL.RawQuery = q.Encode()
	}

	req.Header.Add("Accept", params.accept)
	req.Header.Add("Content-Type", params.contentType)

	if params.token != "" {
		req.Header.Add("Authorization", fmt.Sprintf("Bearer %s", params.token))
	}

	tr := &http.Transport{}
	if c.skipTLSVerify {
		tr.TLSClientConfig = &tls.Config{InsecureSkipVerify: true}
	}
	client := &http.Client{Transport: tr}

	resp, err := client.Do(req)
	if err != nil {
		return fmt.Errorf("could not perform request: %w", err)
	}

	if resp.StatusCode == 400 {
		// try to read error message
		data, err := io.ReadAll(resp.Body)
		if err != nil {
			return ErrorBadRequest
		}

		return fmt.Errorf("%w: %v", ErrorBadRequest, string(data))
	}

	if resp.StatusCode == 404 {
		return ErrorNotFound
	}

	if resp.StatusCode < 200 || resp.StatusCode > 299 {
		return fmt.Errorf("did not get correct response code: %v", resp.Status)
	}

	if params.resp == nil {
		// fmt.Printf("Response:\n%s\n", "<empty>")
		return nil
	}

	data, err := io.ReadAll(resp.Body)
	if err != nil {
		return fmt.Errorf("could not read response body: %w", err)
	}

	// fmt.Printf("Response:\n%s\n", string(data))

	if len(data) == 0 {
		return nil
	}

	if err = json.Unmarshal(data, params.resp); err != nil {
		return fmt.Errorf("could not unmarshaling response: %w", err)
	}

	return nil
}

// Since some arrays in the JSON we get from the server are unsorted,
// but HCL is sorted, we sort all arrays we from the API before we
// compare them with the HCL arrays. This way we can avoid getting
// diverging plans.
func sortArrays(entity any) {

	if reflect.ValueOf(entity).Kind() != reflect.Ptr {
		panic("entity is not a pointer")
	}

	t := reflect.TypeOf(entity).Elem()
	v := reflect.ValueOf(entity).Elem()

	if t.Kind() == reflect.Slice {

		if t.Elem().Kind() == reflect.Struct {

			// slices of structs are recursively sorted
			for i := 0; i < v.Len(); i++ {
				sortArrays(v.Index(i).Addr().Interface())
			}

		}

		// all slices are then sorted themselves. We use
		// the string representation. More complex sorting
		// can be added here if needed.
		sort.Slice(v.Interface(), func(i, j int) bool {
			a := fmt.Sprintf("%v", v.Index(i).Interface())
			b := fmt.Sprintf("%v", v.Index(j).Interface())
			return a < b
		})

		return
	}

	if v.Kind() != reflect.Struct {
		panic("entity is not a pointer to struct, nor to a slice")
	}

	// iterate over all fields of the entity
	for i := 0; i < v.NumField(); i++ {

		field := v.Field(i)

		// check if the field is an array
		if field.Kind() == reflect.Slice {

			sort.Slice(field.Interface(), func(i, j int) bool {
				a := fmt.Sprintf("%v", field.Index(i).Interface())
				b := fmt.Sprintf("%v", field.Index(j).Interface())
				return a < b
			})
		}
	}

}
