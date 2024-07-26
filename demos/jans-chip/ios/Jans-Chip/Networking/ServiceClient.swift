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
    
    func getOPConfiguration(url: String) -> AnyPublisher<DataResponse<OPConfiguration, NetworkError>, Never>
    func doDCR(dcRequest: DCRequest, url: String) -> AnyPublisher<DataResponse<DCResponse, NetworkError>, Never>
    func doDCR(dcRequest: SSARegRequest, url: String) -> AnyPublisher<DataResponse<DCResponse, NetworkError>, Never>
    func getAuthorizationChallenge(clientId: String, username: String, password: String, useDeviceSession: Bool, acrValues: String, authMethod: String, assertionResultRequest: String, authorizationChallengeEndpoint: String) -> AnyPublisher<DataResponse<LoginResponse, NetworkError>, Never>
    func getUserInfo(accessToken: String, authHeader: String, url: String) -> AnyPublisher<DataResponse<[String: String], NetworkError>, Never>
    func getToken(clientId: String, code: String, grantType: String, redirectUri: String, scope: String, authHeader: String, dpopJwt: String, url: String) -> AnyPublisher<DataResponse<TokenResponse, NetworkError>, Never>
    func logout(token: String, tokenTypeHint: String, authHeader: String, url: String) -> AnyPublisher<DataResponse<String, NetworkError>, Never>
    func getFidoConfiguration(url: String) -> AnyPublisher<DataResponse<FidoConfiguration, NetworkError>, Never>
    func attestationOption(attestationRequest: AttestationOptionRequest, url: String) -> AnyPublisher<DataResponse<AttestationOptionResponse, NetworkError>, Never>
    func attestationResult(request: AttestationResultRequest, url: String, completion: ((String) -> Void)?)
    func assertionOption(request: AssertionOptionRequest, url: String) -> AnyPublisher<DataResponse<AssertionOptionResponse, NetworkError>, Never>
    func verifyIntegrityTokenOnAppServer(url: String) -> AnyPublisher<DataResponse<AppIntegrityResponse, NetworkError>, Never>
}

public final class ServiceClient {
    public static let simpleSuccess = "OK"
    
    private let jsonDecoder = JSONDecoder()
    
    private func performRequest<T: Decodable>(route: EndpointRouter, decoder: JSONDecoder = JSONDecoder()) -> AnyPublisher<DataResponse<T, NetworkError>, Never> {
        AF
            .request(route)
            .publishDecodable(type: T.self, decoder: decoder, emptyResponseCodes: [200, 204, 205])
            .map { response in
                response.mapError { error in
                    let backendError = response.data.flatMap { try? JSONDecoder().decode(BackendError.self, from: $0)}
                    return NetworkError(initialError: error, backendError: backendError)
                }
            }
            .receive(on: DispatchQueue.main)
            .eraseToAnyPublisher()
    }
}

// MARK: - ServiceClientProtocol
extension ServiceClient: ServiceClientProtocol {
    
    func getOPConfiguration(url: String) -> AnyPublisher<DataResponse<OPConfiguration, NetworkError>, Never> {
        performRequest(route: .getOPConfiguration(url), decoder: jsonDecoder)
    }
    
    func doDCR(dcRequest: DCRequest, url: String) -> AnyPublisher<DataResponse<DCResponse, NetworkError>, Never> {
        performRequest(route: .doDCR(dcRequest, url), decoder: jsonDecoder)
    }
    
    func doDCR(dcRequest: SSARegRequest, url: String) -> AnyPublisher<DataResponse<DCResponse, NetworkError>, Never> {
        performRequest(route: .doDCRSSA(dcRequest, url), decoder: jsonDecoder)
    }
    
    func getAuthorizationChallenge(clientId: String, username: String, password: String, useDeviceSession: Bool, acrValues: String, authMethod: String, assertionResultRequest: String, authorizationChallengeEndpoint: String) -> AnyPublisher<DataResponse<LoginResponse, NetworkError>, Never> {
        performRequest(route: .getAuthorizationChallenge(clientId, username, password, UUID().uuidString, UUID().uuidString, useDeviceSession, acrValues, authMethod, assertionResultRequest, authorizationChallengeEndpoint), decoder: jsonDecoder)
    }
    
    func getUserInfo(accessToken: String, authHeader: String, url: String) -> AnyPublisher<DataResponse<[String: String], NetworkError>, Never> {
        performRequest(route: .getUserInfo(accessToken, authHeader, url), decoder: jsonDecoder)
    }
    
    func getToken(clientId: String, code: String, grantType: String, redirectUri: String, scope: String, authHeader: String, dpopJwt: String, url: String) -> AnyPublisher<DataResponse<TokenResponse, NetworkError>, Never> {
        performRequest(route: .getToken(clientId, code, grantType, redirectUri, scope, authHeader, dpopJwt, url), decoder: jsonDecoder)
    }
    
    func logout(token: String, tokenTypeHint: String, authHeader: String, url: String) -> AnyPublisher<DataResponse<String, NetworkError>, Never> {
        performRequest(route: .logout(token, tokenTypeHint, authHeader, url), decoder: jsonDecoder)
    }
    
    func getFidoConfiguration(url: String) -> AnyPublisher<DataResponse<FidoConfiguration, NetworkError>, Never> {
        performRequest(route: .getFidoConfiguration(url), decoder: jsonDecoder)
    }
    
    func attestationOption(attestationRequest: AttestationOptionRequest, url: String) -> AnyPublisher<DataResponse<AttestationOptionResponse, NetworkError>, Never> {
        performRequest(route: .attestationOption(attestationRequest, url), decoder: jsonDecoder)
    }
    
    func attestationResult(request: AttestationResultRequest, url: String, completion: ((String) -> Void)?) {
        AF.request(url, method: .post, encoding: JSONEncoding.default).responseDecodable(of: String.self) { response in
            if response.response?.statusCode == 200 {
                completion?(String(data: response.data ?? Data(), encoding: .utf8) ?? "")
                return
            }
            switch response.result {
            case .success(let value):
                completion?(value)
            case .failure(let error):
                print(error)
            }
        }
    }
    
    func assertionOption(request: AssertionOptionRequest, url: String) -> AnyPublisher<DataResponse<AssertionOptionResponse, NetworkError>, Never> {
        performRequest(route: .assertionOption(request, url), decoder: jsonDecoder)
    }
    
    func verifyIntegrityTokenOnAppServer(url: String) -> AnyPublisher<DataResponse<AppIntegrityResponse, NetworkError>, Never> {
        performRequest(route: .verifyIntegrityTokenOnAppServer(url), decoder: jsonDecoder)
    }
}
