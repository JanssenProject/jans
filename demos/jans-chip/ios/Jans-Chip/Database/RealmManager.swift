//
//  RealmManager.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 11.10.2023.
//

import Foundation
import RealmSwift

final class RealmManager {
    
    private init() {}
    
    let shared = RealmManager()
    
    func save(object: Object) {
        do {
            let realm = try Realm()
            
            try realm.write {
                realm.add(object)
            }
        } catch(let error) {
            print("Error saving object, reason: \(error.localizedDescription)")
        }
    }
}
