//
//  ServiceClient.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 05.10.2023.
//

import Foundation
import Alamofire
import Combine

typealias ServiceResult = (Bool, Error?) -> Void

protocol ServiceClientProtocol: AnyObject {
    
    func getOPConfiguration(url: String, completion: @escaping (Result<OPConfiguration, AFError>) -> Void)
    func getFidoConfiguration(url: String) -> AnyPublisher<Result<FidoConfiguration, AFError>, Never>
    func doDCR(dcRequest: DCRequest, url: String) -> AnyPublisher<Result<DCResponse, AFError>, Never>
    func getAuthorizationChallenge(clientId: String, username: String, password: String, useDeviceSession: Bool, acrValues: String, authMethod: String, assertionResultRequest: String, authorizationChallengeEndpoint: String) -> AnyPublisher<Result<LoginResponse, AFError>, Never>
    func getToken(clientId: String, code: String, grantType: String, redirectUri: String, scope: String, authHeader: String, dpopJwt: String, url: String) -> AnyPublisher<Result<TokenResponse, AFError>, Never>
    func getUserInfo(accessToken: String, authHeader: String, url: String) -> AnyPublisher<Result<[String: String], AFError>, Never>
    func logout(token: String, tokenTypeHint: String, authHeader: String, url: String) -> AnyPublisher<Result<String, AFError>, Never>
    func attestationOption(attestationRequest: AttestationOptionRequest, url: String) -> AnyPublisher<Result<AttestationOptionResponse, AFError>, Never>
    func attestationResult(request: AttestationResultRequest, url: String) -> AnyPublisher<Result<AttestationOptionResponse, AFError>, Never>
}

public final class ServiceClient {
    
    private let jsonDecoder = JSONDecoder()
    
    @discardableResult
    private func performRequest<T: Decodable>(route: EndpointRouter, decoder: JSONDecoder = JSONDecoder(), completion: @escaping (Result<T, AFError>) -> Void) -> DataRequest {
        return AF.request(route).responseDecodable(of: T.self, decoder: decoder) { response in
            completion(response.result)
        }
    }
    
    private func performRequest<T: Decodable>(route: EndpointRouter, decoder: JSONDecoder = JSONDecoder()) -> AnyPublisher<Result<T, AFError>, Never> {
        return AF.request(route).publishDecodable(type: T.self, decoder: decoder, emptyResponseCodes: [200, 204, 205]).result()
    }
}

// MARK: - ServiceClientProtocol
extension ServiceClient: ServiceClientProtocol {
    
    func getOPConfiguration(url: String, completion: @escaping (Result<OPConfiguration, AFError>) -> Void) {
        performRequest(route: .getOPConfiguration(url), decoder: jsonDecoder, completion: completion)
    }
    
    func getOPConfiguration(url: String) -> AnyPublisher<Result<OPConfiguration, AFError>, Never> {
        let publisher: AnyPublisher<Result<OPConfiguration, AFError>, Never> = performRequest(route: .getOPConfiguration(url), decoder: jsonDecoder)
        return publisher
    }
    
    func getFidoConfiguration(url: String) -> AnyPublisher<Result<FidoConfiguration, AFError>, Never> {
        let publisher: AnyPublisher<Result<FidoConfiguration, AFError>, Never> = performRequest(route: .getFidoConfiguration(url), decoder: jsonDecoder)
        return publisher
    }
    
    func doDCR(dcRequest: DCRequest, url: String) -> AnyPublisher<Result<DCResponse, AFError>, Never> {
        let publisher: AnyPublisher<Result<DCResponse, AFError>, Never> = performRequest(route: .doDCR(dcRequest, url), decoder: jsonDecoder)
        return publisher
    }
    
    func getAuthorizationChallenge(clientId: String, username: String, password: String, useDeviceSession: Bool, acrValues: String, authMethod: String, assertionResultRequest: String, authorizationChallengeEndpoint: String) -> AnyPublisher<Result<LoginResponse, AFError>, Never> {
        let publisher: AnyPublisher<Result<LoginResponse, AFError>, Never> = performRequest(route: .getAuthorizationChallenge(clientId, username, password, UUID().uuidString, UUID().uuidString, useDeviceSession, acrValues, authMethod, assertionResultRequest, authorizationChallengeEndpoint), decoder: jsonDecoder)
        return publisher
    }
    
    func getToken(clientId: String, code: String, grantType: String, redirectUri: String, scope: String, authHeader: String, dpopJwt: String, url: String) -> AnyPublisher<Result<TokenResponse, AFError>, Never> {
        let publisher: AnyPublisher<Result<TokenResponse, AFError>, Never> = performRequest(route: .getToken(clientId, code, grantType, redirectUri, scope, authHeader, dpopJwt, url), decoder: jsonDecoder)
        return publisher
    }
    
    func getUserInfo(accessToken: String, authHeader: String, url: String) -> AnyPublisher<Result<[String: String], AFError>, Never> {
        let publisher: AnyPublisher<Result<[String: String], AFError>, Never> = performRequest(route: .getUserInfo(accessToken, authHeader, url), decoder: jsonDecoder)
        return publisher
    }
    
    func logout(token: String, tokenTypeHint: String, authHeader: String, url: String) -> AnyPublisher<Result<String, AFError>, Never> {
        let publisher: AnyPublisher<Result<String, AFError>, Never> = performRequest(route: .logout(token, tokenTypeHint, authHeader, url), decoder: jsonDecoder)
        return publisher
    }
    
    func attestationOption(attestationRequest: AttestationOptionRequest, url: String) -> AnyPublisher<Result<AttestationOptionResponse, AFError>, Never> {
        let publisher: AnyPublisher<Result<AttestationOptionResponse, AFError>, Never> = performRequest(route: .attestationOption(attestationRequest, url), decoder: jsonDecoder)
        return publisher
    }
    
    func attestationResult(request: AttestationResultRequest, url: String) -> AnyPublisher<Result<AttestationOptionResponse, AFError>, Never> {
        let publisher: AnyPublisher<Result<AttestationOptionResponse, AFError>, Never> = performRequest(route: .attestationResult(request, url), decoder: jsonDecoder)
        return publisher
    }
}
