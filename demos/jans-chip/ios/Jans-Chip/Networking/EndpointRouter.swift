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
    case getToken(String, String, String, String, String, String, String, String)
    case getUserInfo(String, String, String)
    case logout(String, String, String, String)
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
        case .doDCR, .getAuthorizationChallenge, .getToken, .getUserInfo, .logout:
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
        case .doDCR(_, let url), .getAuthorizationChallenge(_, _, _, _, _, let url), .getToken(_, _, _, _, _, _, _, let url), .getUserInfo(_, _, let url), .logout(_, _, _, let url):
            return url
        default:
            return ""
        }
    }
    
    // MARK: - Parameters
    public var parameters: Parameters? {
        switch self {
        case .doDCR, .getOPConfiguration, .getAuthorizationChallenge, .getToken, .getUserInfo, .logout, .verifyIntegrityTokenOnAppServer:
            return nil
        }
    }
    
    // MARK: - Headers
    public var headers: HTTPHeaders? {
        switch self {
        case .getAuthorizationChallenge:
            return [
                HTTPHeader(name: "Content-Type", value: "application/x-www-form-urlencoded"),
                HTTPHeader(name: "Cache-Control", value: "no-transform, no-store"),
                HTTPHeader(name: "Connection", value: "Keep-Alive")
            ]
        case .getToken(_, _, _, _, _, let authHeader, let dpoP, _):
            return [
                HTTPHeader(name: "Content-Type", value: "application/x-www-form-urlencoded"),
                HTTPHeader(name: "Authorization", value: authHeader),
                HTTPHeader(name: "DPoP", value: dpoP)
            ]
        case .getUserInfo(_, let authHeader, _), .logout(_, _, let authHeader, _):
            return [
                HTTPHeader(name: "Content-Type", value: "application/x-www-form-urlencoded"),
                HTTPHeader(name: "Authorization", value: authHeader)
            ]
        case .getOPConfiguration, .doDCR, .verifyIntegrityTokenOnAppServer:
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
            case .doDCR, .getOPConfiguration, .getAuthorizationChallenge, .verifyIntegrityTokenOnAppServer:
                break
            case .getToken, .getUserInfo, .logout:
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
            if let data = try? JSONSerialization.data(withJSONObject: dcRequest.asParameters, options: []) {
                urlRequest.httpBody = data
            }
        case let .getAuthorizationChallenge(clientId, username, password, state, nonce, _):
            let data : Data = "client_id=\(clientId)&username=\(username)&password=\(password)&state=\(state)&nonce=\(nonce)".data(using: .utf8) ?? Data()
            urlRequest.httpBody = data
        case let .getToken(clientId, code, grantType, redirectUri, scope, _, _, _):
            let data : Data = "client_id=\(clientId)&code=\(code)&grant_type=\(grantType)&redirect_uri=\(redirectUri)&scope=\(scope)".data(using: .utf8) ?? Data()
            urlRequest.httpBody = data
        case .getUserInfo(let accessToken, _, _):
            let data : Data = "access_token=\(accessToken)".data(using: .utf8) ?? Data()
            urlRequest.httpBody = data
        case .logout(let token, let tokenTypeHint, _, _):
            let data : Data = "token=\(token)&token_type_hint=\(tokenTypeHint)".data(using: .utf8) ?? Data()
            urlRequest.httpBody = data
        default:
            break
        }
        
        headers.flatMap {
            urlRequest.headers = $0
        }
        
        return urlRequest
    }
}
