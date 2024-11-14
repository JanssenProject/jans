//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 01.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

enum Transport: String, Codable {
    case usb = "usb"
    case nfc = "nfc"
    case ble = "ble"
    case itn = "internal" // platform authenticator
    case net = "net"
    case qr = "qr"
}
