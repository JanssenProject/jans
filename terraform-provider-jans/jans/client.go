package jans

import (
        "bytes"
        "context"
        "crypto/tls"
        "encoding/json"
        "fmt"
        "io"
        "mime/multipart"
        "reflect"
        "sort"
        "sync"
        "time"

        "net/http"
        "net/http/httputil"
        "net/textproto"
        "net/url"
)

var (
        ErrorBadRequest = fmt.Errorf("bad request")
        ErrorNotFound   = fmt.Errorf("not found")
)

// cachedToken stores an access token with its expiration time
type cachedToken struct {
        accessToken string
        expiresAt   time.Time
}

// isValid checks if the token is still valid with a 30 second safety buffer
func (t *cachedToken) isValid() bool {
        return time.Now().Add(30 * time.Second).Before(t.expiresAt)
}

// pagedResponse represents a paginated API response structure
type pagedResponse struct {
        Start             int             `json:"start"`
        TotalEntriesCount int             `json:"totalEntriesCount"`
        EntriesCount      int             `json:"entriesCount"`
        Entries           json.RawMessage `json:"entries"`
}

// requestParams is used as a conveneince struct to pass parameters to the
// request method.
type requestParams struct {
        method      string
        path        string
        accept      string
        contentType string
        token       string
        scope       string
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
        tokenCache    map[string]*cachedToken
        tokenMutex    sync.RWMutex
}

// NewClient creates a new client, which will connect to a server
// at the provided host, using the given credentials.
func NewClient(host, clientId, clientSecret string) (*Client, error) {
        return &Client{
                host:          host,
                clientId:      clientId,
                clientSecret:  clientSecret,
                skipTLSVerify: false,
                tokenCache:    make(map[string]*cachedToken),
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
                tokenCache:    make(map[string]*cachedToken),
        }, nil
}

// fetchToken requests a new OAuth token from the auth server for the given scope.
// Returns the access token and expiration time in seconds. Does not cache the result.
// Caller is responsible for caching (and must hold the write lock when caching).
func (c *Client) fetchToken(ctx context.Context, scope string) (string, int, error) {

        if c.host == "" {
                return "", 0, fmt.Errorf("host is not set")
        }

        urlString := fmt.Sprintf("%s/jans-auth/restv1/token", c.host)

        params := url.Values{}
        params.Add("grant_type", "client_credentials")
        params.Add("scope", scope)

        req, err := http.NewRequestWithContext(ctx, "POST", urlString, bytes.NewReader([]byte(params.Encode())))
        if err != nil {
                return "", 0, fmt.Errorf("could not create request: %w", err)
        }

        req.SetBasicAuth(c.clientId, c.clientSecret)
        req.Header.Add("Accept", "application/json")
        req.Header.Add("Content-Type", "application/x-www-form-urlencoded")

        tr := &http.Transport{}
        if c.skipTLSVerify {
                tr.TLSClientConfig = &tls.Config{InsecureSkipVerify: true}
        }
        client := &http.Client{Transport: tr}

        // b, _ := httputil.DumpRequest(req, true)
        // tflog.Info(ctx, "Request", map[string]any{"req": string(b)})
        // fmt.Printf("Request:\n%s\n", string(b))

        resp, err := client.Do(req)
        if err != nil {
                return "", 0, fmt.Errorf("could not perform request: %w", err)
        }

        // b, _ = httputil.DumpResponse(resp, true)
        // tflog.Info(ctx, "Response", map[string]any{"resp": string(b)})
        // fmt.Printf("Response:\n%s\n", string(b))

        data, err := io.ReadAll(resp.Body)
        if err != nil {
                return "", 0, fmt.Errorf("could not read response body: %w", err)
        }

        if resp.StatusCode != 200 {
                // fmt.Printf("\n\nResponse data:\n%s\n\n", string(data))
                return "", 0, fmt.Errorf("did not get correct response code: %v", resp.Status)
        }

        type tokenResponse struct {
                AccessToken string `json:"access_token"`
                Scope       string `json:"scope"`
                TokenType   string `json:"token_type"`
                ExpiresIn   int    `json:"expires_in"`
        }

        token := &tokenResponse{}

        if err = json.Unmarshal(data, token); err != nil {
                return "", 0, fmt.Errorf("could not unmarshal response: %w", err)
        }

        // Check if all requested scopes are present in the granted scopes
        // OAuth allows servers to return scopes in any order
        requestedScopes := splitScopes(scope)
        grantedScopes := splitScopes(token.Scope)
        
        for _, requested := range requestedScopes {
                found := false
                for _, granted := range grantedScopes {
                        if requested == granted {
                                found = true
                                break
                        }
                }
                if !found {
                        return "", 0, fmt.Errorf("scope not granted: required '%s', got '%s'", scope, token.Scope)
                }
        }

        fmt.Printf("[TOKEN] Obtained new token for scope '%s', expires in %d seconds\n", scope, token.ExpiresIn)

        // Return token info for caller to cache (caller must hold write lock)
        return token.AccessToken, token.ExpiresIn, nil
}

// splitScopes splits a space-separated scope string into individual scopes
func splitScopes(scope string) []string {
        scopes := []string{}
        for _, s := range splitBySpace(scope) {
                if s != "" {
                        scopes = append(scopes, s)
                }
        }
        return scopes
}

// splitBySpace splits a string by spaces
func splitBySpace(s string) []string {
        result := []string{}
        current := ""
        for _, c := range s {
                if c == ' ' {
                        if current != "" {
                                result = append(result, current)
                                current = ""
                        }
                } else {
                        current += string(c)
                }
        }
        if current != "" {
                result = append(result, current)
        }
        return result
}

// ensureToken checks if a valid cached token exists for the given scope.
// If a valid token exists, it returns the cached token.
// If no valid token exists or it's about to expire, it fetches a new one.
// Uses double-checked locking to prevent multiple concurrent goroutines from
// requesting duplicate tokens when a token expires under load.
func (c *Client) ensureToken(ctx context.Context, scope string) (string, error) {
        // Check context first to fail fast if already canceled
        if err := ctx.Err(); err != nil {
                return "", err
        }

        // Fast-fail guard: reject empty scope before any lock acquisition
        if scope == "" {
                return "", fmt.Errorf("scope is empty")
        }

        // First check: use read lock for fast path
        c.tokenMutex.RLock()
        cached, exists := c.tokenCache[scope]
        c.tokenMutex.RUnlock()

        if exists && cached.isValid() {
                fmt.Printf("[TOKEN] Using cached token for scope '%s'\n", scope)
                return cached.accessToken, nil
        }

        // Token expired or doesn't exist, acquire write lock to refresh
        c.tokenMutex.Lock()
        defer c.tokenMutex.Unlock()

        // Second check: another goroutine may have refreshed while we waited for write lock
        cached, exists = c.tokenCache[scope]
        if exists && cached.isValid() {
                fmt.Printf("[TOKEN] Using cached token for scope '%s' (refreshed by another goroutine)\n", scope)
                return cached.accessToken, nil
        }

        // Still need to refresh - fetch new token while holding write lock
        fmt.Printf("[TOKEN] Token missing or expired for scope '%s', fetching new token\n", scope)
        accessToken, expiresIn, err := c.fetchToken(ctx, scope)
        if err != nil {
                return "", err
        }

        // Cache the token (we already hold the write lock)
        c.tokenCache[scope] = &cachedToken{
                accessToken: accessToken,
                expiresAt:   time.Now().Add(time.Duration(expiresIn) * time.Second),
        }

        return accessToken, nil
}

// invalidateToken removes a token from the cache, forcing a refresh on next use
func (c *Client) invalidateToken(scope string) {
        c.tokenMutex.Lock()
        delete(c.tokenCache, scope)
        c.tokenMutex.Unlock()
        fmt.Printf("[TOKEN] Invalidated cached token for scope '%s'\n", scope)
}

// get performs an HTTP GET request to the given path, using the given token.
// The response data is unmarshaled into the provided response value, which
// has to be of a pointer type. Optionally accepts scope as last parameter for 401 retry support.
func (c *Client) get(ctx context.Context, path, token, scope string, resp any, queryParams ...map[string]string) error {

        params := requestParams{
                method:      "GET",
                path:        path,
                contentType: "application/json",
                accept:      "application/json",
                token:       token,
                scope:       scope,
                resp:        resp,
        }

        if len(queryParams) > 0 {
                params.queryParams = queryParams[0]
        }

        return c.request(ctx, params)
}

// getAllPaginated fetches all entries from a paginated endpoint by making multiple
// requests with increasing startIndex values until all entries are retrieved.
// It returns a slice of raw JSON entries that the caller must unmarshal into the target type.
func (c *Client) getAllPaginated(ctx context.Context, path, scope string, pageSize int) ([]json.RawMessage, error) {
        token, err := c.ensureToken(ctx, scope)
        if err != nil {
                return nil, fmt.Errorf("failed to get token: %w", err)
        }

        var allEntries []json.RawMessage
        startIndex := 1
        fetchedCount := 0

        for {
                var page pagedResponse
                queryParams := map[string]string{
                        "limit":      fmt.Sprintf("%d", pageSize),
                        "startIndex": fmt.Sprintf("%d", startIndex),
                }

                if err := c.get(ctx, path, token, scope, &page, queryParams); err != nil {
                        return nil, fmt.Errorf("failed to fetch page at index %d: %w", startIndex, err)
                }

                // Unmarshal the entries array
                var pageEntries []json.RawMessage
                if len(page.Entries) > 0 {
                        if err := json.Unmarshal(page.Entries, &pageEntries); err != nil {
                                return nil, fmt.Errorf("failed to unmarshal page entries: %w", err)
                        }
                        allEntries = append(allEntries, pageEntries...)
                }

                // Use actual unmarshaled entries count for accurate pagination
                n := len(pageEntries)
                fetchedCount += n

                // Stop if we've fetched all entries or the current page was empty
                if page.TotalEntriesCount > 0 && fetchedCount >= page.TotalEntriesCount {
                        break
                }

                // If no entries were unmarshaled, fall back to API count or break if both zero
                if n == 0 {
                        if page.EntriesCount == 0 {
                                break
                        }
                        // Fall back to API count to avoid infinite loop
                        startIndex += page.EntriesCount
                } else {
                        // Move to next page based on actual unmarshaled entries to avoid gaps/duplicates
                        startIndex += n
                }
        }

        return allEntries, nil
}

// getScim performs an HTTP GET request to the given path, using the given token.
// The response data is unmarshaled into the provided response value, which
// has to be of a pointer type.
func (c *Client) getScim(ctx context.Context, path, token, scope string, resp any) error {

        params := requestParams{
                method:      "GET",
                path:        path,
                contentType: "application/json",
                accept:      "application/scim+json",
                token:       token,
                scope:       scope,
                resp:        resp,
        }

        return c.request(ctx, params)
}

// patch performs an HTTP PATCH request to the given path, using the given
// token and the provided list of patch requests.
func (c *Client) patch(ctx context.Context, path, token, scope string, req []PatchRequest) error {

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
                scope:       scope,
                payload:     payload,
        }

        return c.request(ctx, params)
}

// put performs an HTTP PUT request to the given path, using the given token
// and the provided request entity, which is marshaled into JSON. The response
// data is unmarshaled into the provided response value, which has to be of
// a pointer type.
func (c *Client) put(ctx context.Context, path, token, scope string, req, resp any) error {

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
                scope:       scope,
                payload:     payload,
                resp:        resp,
        }

        return c.request(ctx, params)
}

// putText performs an HTTP PUT request to the given path, using the given token
// and the provided request entity, which is marshaled into JSON. The response
// data is unmarshaled into the provided response value, which has to be of
// a pointer type. Unlike put, putText uses the "text/plain" content type.
func (c *Client) putText(ctx context.Context, path, token, scope, req string, resp any) error {

        params := requestParams{
                method:      "PUT",
                path:        path,
                contentType: "text/plain",
                accept:      "application/json",
                token:       token,
                scope:       scope,
                payload:     []byte(req),
                resp:        resp,
        }

        return c.request(ctx, params)
}

// post performs an HTTP POST request to the given path, using the given token
// and the provided request entity, which is marshaled into JSON. The response
// data is unmarshaled into the provided response value, which has to be of
// a pointer type.
func (c *Client) post(ctx context.Context, path, token, scope string, req, resp any) error {

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
                scope:       scope,
                payload:     payload,
                resp:        resp,
        }

        return c.request(ctx, params)
}

type FormField struct {
        Typ  string
        Data io.Reader
}

type requestParamsOptions func(*requestParams) error

func (c *Client) newParams(method, path string, resp any, options ...requestParamsOptions) (*requestParams, error) {
        params := &requestParams{
                method: method,
                accept: "application/json",
                path:   path,
                resp:   resp,
        }

        for _, o := range options {
                if err := o(params); err != nil {
                        return nil, err
                }
        }

        return params, nil
}

func (c *Client) withToken(ctx context.Context, scope string) requestParamsOptions {
        return func(params *requestParams) (err error) {
                params.token, err = c.ensureToken(ctx, scope)
                params.scope = scope
                return
        }
}

func (c *Client) withFormData(req map[string]FormField) requestParamsOptions {
        return func(params *requestParams) (err error) {
                var b bytes.Buffer

                w := multipart.NewWriter(&b)
                defer w.Close()

                for key, r := range req {
                        var fw io.Writer

                        switch r.Typ {
                        case "file":
                                if fw, err = w.CreateFormFile(key, "file"); err != nil {
                                        return
                                }
                        case "json":
                                h := make(textproto.MIMEHeader)
                                h.Set("Content-Disposition", fmt.Sprintf(`form-data; name="%s"`, key))
                                h.Set("Content-Type", "application/json")
                                if fw, err = w.CreatePart(h); err != nil {
                                        return
                                }
                        }

                        if _, err = io.Copy(fw, r.Data); err != nil {
                                return
                        }
                }

                w.Close()

                params.contentType = w.FormDataContentType()
                params.payload = b.Bytes()

                return nil
        }
}

func (c *Client) postFormData(ctx context.Context, path, token, scope string, req map[string]FormField, resp any) (err error) {
        var b bytes.Buffer

        w := multipart.NewWriter(&b)
        defer w.Close()

        for key, r := range req {
                var fw io.Writer

                switch r.Typ {
                case "file":
                        if fw, err = w.CreateFormFile(key, "file"); err != nil {
                                return
                        }
                case "json":
                        h := make(textproto.MIMEHeader)
                        h.Set("Content-Disposition", fmt.Sprintf(`form-data; name="%s"`, key))
                        h.Set("Content-Type", "application/json")
                        if fw, err = w.CreatePart(h); err != nil {
                                return
                        }
                }

                if _, err = io.Copy(fw, r.Data); err != nil {
                        return
                }
        }

        w.Close()

        params := requestParams{
                method:      "POST",
                path:        path,
                contentType: w.FormDataContentType(),
                accept:      "application/json",
                token:       token,
                scope:       scope,
                payload:     b.Bytes(),
                resp:        resp,
        }

        return c.request(ctx, params)
}

// postZipFile performs an HTTP POST request to the given path, using the given token
// and the provided request entity, which is marshaled into JSON. The response
// data is unmarshaled into the provided response value, which has to be of
// a pointer type.
func (c *Client) postZipFile(ctx context.Context, path, token, scope string, req []byte, resp any) error {

        params := requestParams{
                method:      "POST",
                path:        path,
                contentType: "application/zip",
                accept:      "application/json",
                token:       token,
                scope:       scope,
                payload:     req,
                resp:        resp,
        }

        return c.request(ctx, params)
}

// delete performs an HTTP DELETE request to the given path, using the given
// token.
func (c *Client) delete(ctx context.Context, path, token, scope string) error {

        params := requestParams{
                method:      "DELETE",
                path:        path,
                contentType: "application/json",
                accept:      "application/json",
                token:       token,
                scope:       scope,
        }

        return c.request(ctx, params)
}

// deleteEntity performs an HTTP DELETE request to the given path, using the given
// token.
func (c *Client) deleteEntity(ctx context.Context, path, token, scope string, entity any) error {

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
                scope:       scope,
                payload:     payload,
        }

        return c.request(ctx, req)
}

// createRequest builds an *http.Request from requestParams, setting method, URL, payload,
// query params, and all necessary headers (Accept, Content-Type, jans-client, user-inum).
// Authorization header is added only if params.token is non-empty.
func (c *Client) createRequest(ctx context.Context, params requestParams, url string) (*http.Request, error) {
        req, err := http.NewRequestWithContext(ctx, params.method, url, bytes.NewReader(params.payload))
        if err != nil {
                return nil, fmt.Errorf("could not create request: %w", err)
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
        req.Header.Add("jans-client", "infrastructure-as-code-tool")
        req.Header.Add("user-inum", c.clientId)

        if params.token != "" {
                req.Header.Add("Authorization", fmt.Sprintf("Bearer %s", params.token))
        }

        return req, nil
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

        req, err := c.createRequest(ctx, params, url)
        if err != nil {
                return err
        }

        tr := &http.Transport{}
        if c.skipTLSVerify {
                tr.TLSClientConfig = &tls.Config{InsecureSkipVerify: true}
        }
        client := &http.Client{Transport: tr}

        b, _ := httputil.DumpRequest(req, true)
        // tflog.Info(ctx, "Request", map[string]any{"req": string(b)})
        fmt.Printf("Request:\n%s\n", string(b))

        resp, err := client.Do(req)
        if err != nil {
                return fmt.Errorf("could not perform request: %w", err)
        }

        b, _ = httputil.DumpResponse(resp, true)
        // tflog.Info(ctx, "Response", map[string]any{"resp": string(b)})
        fmt.Printf("Response:\n%s\n", string(b))

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

        if resp.StatusCode == 401 && params.scope != "" {
                // Token may have expired or been invalidated
                // Invalidate cached token and retry once with a fresh token
                fmt.Printf("[TOKEN] Received 401 Unauthorized, invalidating token for scope '%s' and retrying\n", params.scope)
                c.invalidateToken(params.scope)
                
                // Get a fresh token
                newToken, err := c.ensureToken(ctx, params.scope)
                if err != nil {
                        return fmt.Errorf("failed to refresh token after 401: %w", err)
                }
                
                // Update the params with the new token
                params.token = newToken
                
                // Create retry request with fresh token using the same helper
                retryReq, err := c.createRequest(ctx, params, url)
                if err != nil {
                        return err
                }
                
                fmt.Printf("[TOKEN] Retrying request with fresh token\n")
                resp, err = client.Do(retryReq)
                if err != nil {
                        return fmt.Errorf("could not perform retry request: %w", err)
                }
                
                b, _ = httputil.DumpResponse(resp, true)
                fmt.Printf("Retry Response:\n%s\n", string(b))
                
                // Check the retry response status
                if resp.StatusCode == 400 {
                        data, err := io.ReadAll(resp.Body)
                        if err != nil {
                                return ErrorBadRequest
                        }
                        return fmt.Errorf("%w: %v", ErrorBadRequest, string(data))
                }
                
                if resp.StatusCode == 404 {
                        return ErrorNotFound
                }
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

        if len(data) == 0 || params.resp == nil {
                return nil
        }

        // if json.Valid(data) {
        //      return fmt.Errorf("response is not valid json")
        // }

        if err = json.Unmarshal(data, params.resp); err != nil {
                return fmt.Errorf("could not unmarshal response: %w", err)
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
