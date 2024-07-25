//
//  Error.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 04.10.2023.
//

import Foundation
import Alamofire

protocol NetworkErrorHandler {
    var localizedDescription: String { get }
}

struct NetworkError: Error, NetworkErrorHandler {
    let initialError: AFError
    let backendError: BackendError?
    
    var localizedDescription: String {
        if let backendError {
            return backendError.error
        }
        
        return initialError.localizedDescription
    }
}

struct BackendError: Codable, Error {
    
    private enum CodingKeys: String, CodingKey {
        case error
        case deviceSession = "device_session"
    }
    
    var error: String
    var deviceSession: String?
}
