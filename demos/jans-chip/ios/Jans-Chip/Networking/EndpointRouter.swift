//
//  EndpointRouter.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 04.10.2023.
//

import Foundation
import Alamofire

public enum EndpointRouter: EndpointConfiguration {
    
    case getOPConfiguration(String)
    case doDCR(DCRequest, String)
    case getAuthorizationChallenge(String, String, String, String, String, String)
    case getToken(String, String, String, String, String, String)
    case getUserInfo(String, String, String)
    case logout(String, String, String)
    case verifyIntegrityTokenOnAppServer(String)
    
    // MARK: - HTTPMethod
    public var method: HTTPMethod {
        
        switch self {
        case .getOPConfiguration, .verifyIntegrityTokenOnAppServer:
            return .get
        case .doDCR, .getAuthorizationChallenge, .getToken, .getUserInfo, .logout:
            return .post
        }
    }
    
    // MARK: - BaseURL
    public var baseURL: String {
        switch self {
        case .doDCR, .getAuthorizationChallenge:
            return ""
        default:
            return "https://duttarnab-coherent-imp.gluu.info/"
        }
    }
    
    // MARK: - Path
    public var path: String {
        switch self {
        case .getOPConfiguration:
            return ".well-known/openid-configuration"
        case .doDCR(_, let url), .getAuthorizationChallenge(_, _, _, _, _, let url):
            return url
        default:
            return ""
        }
    }
    
    // MARK: - Parameters
    public var parameters: Parameters? {
        switch self {
        case .doDCR, .getOPConfiguration, .verifyIntegrityTokenOnAppServer:
            return nil
        case let .getAuthorizationChallenge(clientId, username, password, state, nonce, _):
            return [
                "client_id": clientId,
                "username": username,
                "password": password,
                "state": state,
                "nonce": nonce
            ]
        case let .getToken(clientId, code, grantType, redirectUri, scope, _):
            return [
                "client_id": clientId,
                "code": code,
                "grant_type": grantType,
                "redirect_uri": redirectUri,
                "scope": scope
            ]
        case .getUserInfo(let accessToken, _, _):
            return [
                "access_token": accessToken
            ]
        case let .logout(token, tokenTypeHint, _):
            return [
                "token": token,
                "token_type_hint": tokenTypeHint
            ]
        }
    }
    
    // MARK: - Headers
    public var headers: HTTPHeaders? {
        switch self {
        case .getToken(_, _, _, _, _, let authHeader):
            return [
                HTTPHeader(name: "Authorization", value: authHeader)
            ]
        case .getUserInfo(_, let authHeader, _):
            return [
                HTTPHeader(name: "Authorization", value: authHeader)
            ]
        case .logout(_, _, let authHeader):
            return [
                HTTPHeader(name: "Authorization", value: authHeader)
            ]
        case .getOPConfiguration, .doDCR, .getAuthorizationChallenge, .verifyIntegrityTokenOnAppServer:
            return nil
        }
    }
    
    // MARK: - URLRequestConvertible
    public func asURLRequest() throws -> URLRequest {
        let urlWithPathValue = baseURL + path
        var url = try urlWithPathValue.asURL()
        var urlRequest = URLRequest(url: url)
        urlRequest.addValue("application/json", forHTTPHeaderField: "Content-Type")
        urlRequest.httpMethod = method.rawValue
        
        if let parameters = parameters {
            switch self {
            case .doDCR, .getOPConfiguration, .verifyIntegrityTokenOnAppServer:
                return urlRequest
            case .getAuthorizationChallenge, .getToken, .getUserInfo, .logout:
                var urlComponents = URLComponents(string: urlWithPathValue)!
                urlComponents.queryItems = []
                
                parameters.forEach { (key, value) in
                    let item = URLQueryItem(name: key, value: value as? String)
                    urlComponents.queryItems?.append(item)
                }
                
                urlComponents.url.flatMap {
                    url = $0
                }
                urlRequest.url = url
            }
        }
        
        switch self {
        case .doDCR(let dcRequest, _):
            let jsonBody: [String: Any] = dcRequest.asParameters
            let jsonData = try? JSONSerialization.data(withJSONObject: jsonBody)
            urlRequest.httpBody = jsonData
            
        default:
            break
        }
        
        return urlRequest
    }
}
