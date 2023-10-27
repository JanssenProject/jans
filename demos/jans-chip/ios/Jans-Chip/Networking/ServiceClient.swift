//
//  ServiceClient.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 05.10.2023.
//

import Foundation
import Alamofire
import Combine

protocol ServiceClientProtocol: AnyObject {
    
    func getOPConfiguration(url: String, completion: @escaping (Result<OPConfiguration, AFError>) -> Void)
    func doDCR(dcRequest: DCRequest, url: String) -> AnyPublisher<Result<DCResponse, AFError>, Never>
    func getAuthorizationChallenge(clientId: String, username: String, password: String, authorizationChallengeEndpoint: String, url: String) -> AnyPublisher<Result<LoginResponse, AFError>, Never>
    func getToken(clientId: String, code: String, grantType: String, redirectUri: String, scope: String, authHeader: String, dpopJwt: String, url: String) -> AnyPublisher<Result<TokenResponse, AFError>, Never>
    func getUserInfo(accessToken: String, authHeader: String, url: String) -> AnyPublisher<Result<UserInfo, AFError>, Never>
}

public final class ServiceClient {
    
    @discardableResult
    private func performRequest<T: Decodable>(route: EndpointRouter, decoder: JSONDecoder = JSONDecoder(), completion: @escaping (Result<T, AFError>) -> Void) -> DataRequest {
        return AF.request(route).responseDecodable(of: T.self, decoder: decoder) { response in
            completion(response.result)
        }
    }
    
    private func performRequest<T: Decodable>(route: EndpointRouter, decoder: JSONDecoder = JSONDecoder()) -> AnyPublisher<Result<T, AFError>, Never> {
        return AF.request(route).publishDecodable(type: T.self, decoder: decoder).result()
    }
}

// MARK: - ServiceClientProtocol
extension ServiceClient: ServiceClientProtocol {
    
    func getOPConfiguration(url: String, completion: @escaping (Result<OPConfiguration, AFError>) -> Void) {
        let jsonDecoder = JSONDecoder()
        performRequest(route: .getOPConfiguration(url), decoder: jsonDecoder, completion: completion)
    }
    
    func getOPConfiguration(url: String) -> AnyPublisher<Result<OPConfiguration, AFError>, Never> {
        let jsonDecoder = JSONDecoder()
        let publisher: AnyPublisher<Result<OPConfiguration, AFError>, Never> = performRequest(route: .getOPConfiguration(url), decoder: jsonDecoder)
        return publisher
    }
    
    func doDCR(dcRequest: DCRequest, url: String) -> AnyPublisher<Result<DCResponse, AFError>, Never> {
        let jsonDecoder = JSONDecoder()
        let publisher: AnyPublisher<Result<DCResponse, AFError>, Never> = performRequest(route: .doDCR(dcRequest, url), decoder: jsonDecoder)
        return publisher
    }
    
    func getAuthorizationChallenge(clientId: String, username: String, password: String, authorizationChallengeEndpoint: String, url: String) -> AnyPublisher<Result<LoginResponse, AFError>, Never> {
        let jsonDecoder = JSONDecoder()
        let publisher: AnyPublisher<Result<LoginResponse, AFError>, Never> = performRequest(route: .getAuthorizationChallenge(clientId, username, password, UUID().uuidString, UUID().uuidString, authorizationChallengeEndpoint), decoder: jsonDecoder)
        return publisher
    }
    
    func getToken(clientId: String, code: String, grantType: String, redirectUri: String, scope: String, authHeader: String, dpopJwt: String, url: String) -> AnyPublisher<Result<TokenResponse, AFError>, Never> {
        let jsonDecoder = JSONDecoder()
        let publisher: AnyPublisher<Result<TokenResponse, AFError>, Never> = performRequest(route: .getToken(clientId, code, grantType, redirectUri, scope, authHeader, dpopJwt, url), decoder: jsonDecoder)
        return publisher
    }
    
    func getUserInfo(accessToken: String, authHeader: String, url: String) -> AnyPublisher<Result<UserInfo, AFError>, Never> {
        let jsonDecoder = JSONDecoder()
        let publisher: AnyPublisher<Result<UserInfo, AFError>, Never> = performRequest(route: .getUserInfo(accessToken, authHeader, url), decoder: jsonDecoder)
        return publisher
    }
}
