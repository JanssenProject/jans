//
//  AnalyticImpl.swift
//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 01.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation
import shared

class AnalyticImpl: Analytic {
    private let logger: Logger
    
    init(logger: Logger) {
        self.logger = logger
    }
    
    func logEvent(event: String) {
        logger.log(text: "Event \"\(event)\" sent to analytic by iOS implementation")
    }
}
