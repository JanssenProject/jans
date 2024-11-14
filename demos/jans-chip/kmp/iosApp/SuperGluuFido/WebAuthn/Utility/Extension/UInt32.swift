//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 01.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation

extension UInt32 {
    func toDataBigEndian() -> Data {
        return withUnsafeBytes(of: self.bigEndian) { Data($0) }
    }
}
