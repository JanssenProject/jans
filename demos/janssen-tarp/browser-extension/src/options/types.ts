// Types for better type safety
export interface OpenIDConfiguration {
    issuer: string;
    registration_endpoint: string;
    authorization_endpoint: string;
    token_endpoint: string;
    userinfo_endpoint: string;
    end_session_endpoint?: string;
    acr_values_supported?: string[];
    [key: string]: any;
  }
  
  export interface OIDCClient {
    id: string;
    opHost: string;
    clientId: string;
    clientSecret: string;
    scope: string;
    redirectUris: string[];
    authorizationEndpoint: string;
    tokenEndpoint: string;
    userinfoEndpoint: string;
    acrValuesSupported?: string[];
    endSessionEndpoint?: string;
    responseType: string[];
    postLogoutRedirectUris: string[];
    expireAt?: number;
    showClientExpiry: boolean;
    registrationDate: number;
    openidConfiguration: OpenIDConfiguration;
  }
  
  export interface RegistrationRequest {
    redirect_uris: string[];
    scope: string;
    post_logout_redirect_uris: string[];
    response_types: string[];
    grant_types: string[];
    application_type: string;
    client_name: string;
    token_endpoint_auth_method: string;
    access_token_as_jwt: boolean;
    userinfo_signed_response_alg: string;
    jansInclClaimsInIdTkn: string;
    access_token_lifetime: number;
    lifetime?: number;
  }

  // Types for logout functionality
export interface LoginDetails {
    id_token: string;
    access_token?: string;
    refresh_token?: string;
    user_info?: any;
    [key: string]: any;
  }
  
  export interface LogoutOptions {
    forceSilentLogout?: boolean;
    notifyOnComplete?: boolean;
  }
  