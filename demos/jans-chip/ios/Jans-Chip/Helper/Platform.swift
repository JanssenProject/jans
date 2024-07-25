//
//  Platform.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 22.07.2024.
//

import Foundation

struct Platform {
    
    static var isSimulator: Bool {
        TARGET_OS_SIMULATOR != 0
    }
}
