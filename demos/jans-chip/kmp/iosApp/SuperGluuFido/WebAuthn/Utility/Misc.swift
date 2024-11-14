//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 01.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation

func generateRandomBytes(len: Int) -> Data? {
    var randomBytes = [Int8](repeating: 0, count: len)
    guard SecRandomCopyBytes(kSecRandomDefault, randomBytes.count, &randomBytes) == errSecSuccess else {
        return nil
    }
    return Data(bytes: randomBytes, count: len)
}
