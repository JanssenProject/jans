//
//  KotlinByteArray.swift
//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 22.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation
import shared

extension KotlinByteArray {
    static func from(data: Data) -> KotlinByteArray {
        let int8array = [UInt8](data)
            .map(Int8.init(bitPattern:))
        
        let result = KotlinByteArray(size: Int32(data.count))
        for i in 0..<data.count {
            result.set(index: Int32(i), value: int8array[i])
        }
        
        return result
    }
}
