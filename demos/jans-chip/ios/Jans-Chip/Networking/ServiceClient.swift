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
    
    func getOPConfiguration(completion: @escaping (Result<OPConfiguration, AFError>) -> Void)
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
    
    func getOPConfiguration(completion: @escaping (Result<OPConfiguration, AFError>) -> Void) {
        let jsonDecoder = JSONDecoder()
        jsonDecoder.keyDecodingStrategy = .convertFromSnakeCase
        performRequest(route: .getOPConfiguration(""), decoder: jsonDecoder, completion: completion)
    }
    
    func getOPConfiguration() -> AnyPublisher<Result<OPConfiguration, AFError>, Never> {
        let jsonDecoder = JSONDecoder()
        jsonDecoder.keyDecodingStrategy = .convertFromSnakeCase
        let publisher: AnyPublisher<Result<OPConfiguration, AFError>, Never> = performRequest(route: .getOPConfiguration(""), decoder: jsonDecoder)
        return publisher
    }
}
