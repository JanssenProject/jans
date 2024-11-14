//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 01.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation

extension UUID {
    var uuidStringWithUnderScore: String {
        return self.uuidString.replacingOccurrences(of: "-", with: "_")
    }

    func toData() -> Data {
        var data = Data(count: 16)
        data[0] = self.uuid.0
        data[1] = self.uuid.1
        data[2] = self.uuid.2
        data[3] = self.uuid.3
        data[4] = self.uuid.4
        data[5] = self.uuid.5
        data[6] = self.uuid.6
        data[7] = self.uuid.7
        data[8] = self.uuid.8
        data[9] = self.uuid.9
        data[10] = self.uuid.10
        data[11] = self.uuid.11
        data[12] = self.uuid.12
        data[13] = self.uuid.13
        data[14] = self.uuid.14
        data[15] = self.uuid.15
        return data
    }

    static func from(data: Data) -> UUID {
        assert(data.count == 16, "UUID's length must be 16.")
        let uuid = (data[0],
                    data[1],
                    data[2],
                    data[3],
                    data[4],
                    data[5],
                    data[6],
                    data[7],
                    data[8],
                    data[9],
                    data[10],
                    data[11],
                    data[12],
                    data[13],
                    data[14],
                    data[15])
        return UUID.init(uuid: uuid)
    }
}
