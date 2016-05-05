<?php
namespace org\xdi\oxauth\client;
require_once 'Display.php';
require_once 'Parameters.php';
require_once 'Prompt.php';
require_once 'ResponseType.php';

/**
 * Represents an authorization request to send to the authorization server.
 *
 * @author Gabin Dongmo 29/03/2013
 */
class AuthorizationRequest extends BaseRequest {
    
    private $responseTypes;
    private $clientId;
    private $scopes;
    private $redirectUri;

    private $nonce;
    private $state;
    private $request;
    private $requestUri;
    private $display;
    private $prompts;
    private $requestSessionState;
    private $sessionState;

    private $accessToken;
    private $useNoRedirectHeader;
    
    public function __construct($responseTypes, $clientId, $scopes, $redirectUri, $nonce) {
        parent::__construct();
        $this->responseTypes = $responseTypes;
        $this->clientId = $clientId;
        $this->scopes = $scopes;
        $this->redirectUri = $redirectUri;
        $this->nonce = $nonce;
        $prompts = array();
        $useNoRedirectHeader = false;        
    }

    /**
     * Returns the response types.
     *
     * @return The response types.
     */
    public function getResponseTypes() {
        return $this->responseTypes;
    }
    
    /**
     * Sets the response types.
     *
     * @param responseTypes The response types.
     */
    public function setResponseTypes($responseTypes) {
        $this->responseTypes = $responseTypes;
    }
    
    /**
     * Returns the client identifier.
     *
     * @return The client identifier.
     */
    public function getClientId() {
        return $this->clientId;
    }
    
    /**
     * Sets the client identifier.
     *
     * @param clientId The client identifier.
     */
    public function setClientId($clientId) {
        $this->clientId = $clientId;
    }

    /**
     * Returns the scopes of the access request. The authorization endpoint allow
     * the client to specify the scope of the access request using the scope
     * request parameter. In turn, the authorization server uses the scope
     * response parameter to inform the client of the scope of the access token
     * issued. The value of the scope parameter is expressed as a list of
     * space-delimited, case sensitive strings.
     *
     * @return The scopes of the access request.
     */
    public function getScopes() {
        return $this->scopes;
    }
    
    /**
     * Sets the scope of the access request. The authorization endpoint allow
     * the client to specify the scope of the access request using the scope
     * request parameter. In turn, the authorization server uses the scope
     * response parameter to inform the client of the scope of the access token
     * issued. The value of the scope parameter is expressed as a list of
     * space-delimited, case sensitive strings.
     *
     * @param scopes The scope of the access request.
     */
    public function setScopes($scopes) {
        $this->scopes = $scopes;
    }
    
    /**
     * Returns whether session state is requested.
     *
     * @return whether session state is requested.
     */
    public function isRequestSessionState() {
        return $this->requestSessionState;
    }
    
    /**
     * Sets whether session state should be requested.
     *
     * @param p_requestSessionState session state.
     */
    public function setRequestSessionState($p_requestSessionState) {
        $this->requestSessionState = $p_requestSessionState;
    }
    
    /**
     * Gets session state.
     *
     * @return session state.
     */
    public function getSessionState() {
        return $this->sessionState;
    }    
    
    /**
     * Sets session state.
     *
     * @param p_sessionState session state.
     */
    public function setSessionState($p_sessionState) {
        $this->sessionState = $p_sessionState;
    }
    
    /**
     * Returns the redirection URI.
     *
     * @return The redirection URI.
     */
    public function getRedirectUri() {
        return $this->redirectUri;
    }
    
    /**
     * Sets the redirection URI.
     *
     * @param redirectUri The redirection URI.
     */
    public function setRedirectUri($redirectUri) {
        $this->redirectUri = $redirectUri;
    }
    
    /**
     * Returns a string value used to associate a user agent session with an ID Token,
     * and to mitigate replay attacks.
     *
     * @return The nonce value.
     */
    public function getNonce() {
        return $this->nonce;
    }
    
    /**
     * Sets a string value used to associate a user agent session with an ID Token,
     * and to mitigate replay attacks.
     *
     * @param nonce The nonce value.
     */
    public function setNonce($nonce) {
        $this->nonce = $nonce;
    }
    
    /**
     * Returns the state. The state is an opaque value used by the client to
     * maintain state between the request and callback. The authorization server
     * includes this value when redirecting the user-agent back to the client.
     * The parameter should be used for preventing cross-site request forgery.
     *
     * @return The state.
     */
    public function getState() {
        return $this->state;
    }
    
    /**
     * Sets the state. The state is an opaque value used by the client to
     * maintain state between the request and callback. The authorization server
     * includes this value when redirecting the user-agent back to the client.
     * The parameter should be used for preventing cross-site request forgery.
     *
     * @param state The state.
     */
    public function setState($state) {
        $this->state = $state;
    }
    
    /**
     * Returns a JWT  encoded OpenID Request Object.
     *
     * @return A JWT  encoded OpenID Request Object.
     */
    public function getRequest() {
        return $this->request;
    }

    /**
     * Sets a JWT  encoded OpenID Request Object.
     *
     * @param request A JWT  encoded OpenID Request Object.
     */
    public function setRequest($request) {
        $this->request = $request;
    }
    
    /**
     * Returns an URL that points to an OpenID Request Object.
     *
     * @return An URL that points to an OpenID Request Object.
     */
    public function getRequestUri() {
        return $this->requestUri;
    }

    /**
     * Sets an URL that points to an OpenID Request Object.
     *
     * @param requestUri An URL that points to an OpenID Request Object.
     */
    public function setRequestUri($requestUri) {
        $this->requestUri = $requestUri;
    }
    
    /**
     * Returns an ASCII string value that specifies how the Authorization Server displays the
     * authentication page to the End-User.
     *
     * @return The display value.
     */
    public function getDisplay() {
        return $this->display;
    }

    /**
     * Sets an ASCII string value that specifies how the Authorization Server displays the
     * authentication page to the End-User.
     *
     * @param display The display value.
     */
    public function setDisplay($display) {
        $this->display = $display;
    }

    /**
     * Returns a space delimited list of ASCII strings that can contain the values login, consent,
     * select_account, and none.
     *
     * @return The prompt list.
     */
    public function getPrompts() {
        return $this->prompts;
    }
    
    public function getAccessToken() {
        return $this->accessToken;
    }

    public function setAccessToken($accessToken) {
        $this->accessToken = $accessToken;
    }
    
    public function isUseNoRedirectHeader() {
        return $this->useNoRedirectHeader;
    }
    
    public function setUseNoRedirectHeader($useNoRedirectHeader) {
        $this->useNoRedirectHeader = $useNoRedirectHeader;
    }

    public function getResponseTypesAsString() {
        return Util.asString(responseTypes);
    }
    
    /**
     * Returns a query string with the parameters of the authorization request.
     * Any <code>null</code> or empty parameter will be omitted.
     *
     * @return A query string of parameters.
     */
    public function getQueryString(){
        
        $queryStringBuilder;
        
    }
}

?>
