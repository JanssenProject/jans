//
//  ErrorHandler.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 18.07.2024.
//

import Foundation

protocol ErrorHandler {
    var errorMessage: String? { get set }
    var isSuccess: Bool { get set }
}
