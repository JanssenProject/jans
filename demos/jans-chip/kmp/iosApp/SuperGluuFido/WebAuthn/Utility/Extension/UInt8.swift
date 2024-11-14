//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 01.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation

extension UInt8 {
    func toData() -> Data {
        return withUnsafeBytes(of: self) { Data($0) }
    }
}
