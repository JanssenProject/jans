import SwiftUI
import shared

@main
struct SuperGluuFidoApp: App {
    init() {
        StartDIKt.startDI(
            nativeModule: nativeModule,
            appDeclaration: { _ in }
        )
    }
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
