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
    
    static let shared = RealmManager()
    
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
    
    func getObject<T: Object>() -> T? {
        var result: T? = nil
        do {
            let realm = try Realm()
            try realm.write {
                let objects = realm.objects(T.self)
                result = objects.first
            }
        } catch(let error) {
            print("Error getting object, reason: \(error.localizedDescription)")
        }
        
        return result
    }
    
    // MARK: - OP Configuration
    func deleteAllConfiguration() {
        do {
            let realm = try Realm()
            try realm.write {
                let configurations = realm.objects(OPConfigurationObject.self)
                realm.delete(configurations)
            }
        } catch(let error) {
            print("Error deleting object, reason: \(error.localizedDescription)")
        }
    }
    
    // MARK: - AppIntegrity
    func deleteAllAppIntegrity() {
        do {
            let realm = try Realm()
            try realm.write {
                let configurations = realm.objects(AppIntegrityEntity.self)
                realm.delete(configurations)
            }
        } catch(let error) {
            print("Error deleting object, reason: \(error.localizedDescription)")
        }
    }
}
