//
//  CodableClasses.swift
//  iOSApp
//
//  Created by Arnab Dutta on 25/04/25.
//

import Foundation

enum CodableDecision: String, Codable {
    
    case allow
    case deny
    
    init(from decision: Decision) {
        switch decision {
        case .allow: self = .allow
        case .deny: self = .deny
        }
    }
    
    func toDecision() -> Decision {
        switch self {
        case .allow: return .allow
        case .deny: return .deny
        }
    }
}

struct CodableDiagnostics: Codable {
    public var reasons: [String]
    public var errors: [String]
    
    init(from diagnostics: Diagnostics) {
        self.reasons = diagnostics.reasons
        self.errors = diagnostics.errors
    }
    
    func toDiagnostics() -> Diagnostics {
        return Diagnostics(reasons: reasons, errors: errors)
    }
}

struct CodableResponse: Codable {
    public var decision: CodableDecision
    public var diagnostics: CodableDiagnostics
    
    init(from response: Response) {
        self.decision = CodableDecision(from: response.decision)
        self.diagnostics = CodableDiagnostics(from: response.diagnostics)
    }
    
    func toResponse() -> Response {
        return Response(decision: decision.toDecision(),
                        diagnostics: diagnostics.toDiagnostics())
    }
}
