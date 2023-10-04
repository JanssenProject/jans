//
//  Jans_ChipApp.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import SwiftUI

@main
struct Jans_ChipApp: App {
    var body: some Scene {
        WindowGroup {
            MainViewAssembler.assembleNavigationWrapped()
        }
    }
}
