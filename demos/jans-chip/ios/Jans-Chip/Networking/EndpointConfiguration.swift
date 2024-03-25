//
//  EndpointConfiguration.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 04.10.2023.
//

import Alamofire
import Foundation

public protocol EndpointConfiguration: URLRequestConvertible {
    
    var method: HTTPMethod { get }
    var baseURL: String { get }
    var path: String { get }
    var parameters: Parameters? { get }
    var headers: HTTPHeaders? { get }
    
    func asURLRequest() throws -> URLRequest
}
