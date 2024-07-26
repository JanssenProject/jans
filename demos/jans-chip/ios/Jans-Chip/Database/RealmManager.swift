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
                realm.add(object, update: .all)
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
    
    func getObjects<T: Object>() -> [T]? {
        var result: [T]? = nil
        do {
            let realm = try Realm()
            try realm.write {
                let objects = realm.objects(T.self)
                result = Array(objects)
            }
        } catch(let error) {
            print("Error getting object, reason: \(error.localizedDescription)")
        }
        
        return result
    }
    
    func updateOIDCClient(oidcCClient: OIDCClient, with recentGeneratedAccessToken: String, and recentGeneratedIdToken: String) {
        do {
            let realm = try Realm()
            
            try realm.write {
                oidcCClient.recentGeneratedAccessToken = recentGeneratedAccessToken
                oidcCClient.recentGeneratedIdToken = recentGeneratedIdToken
            }
        } catch(let error) {
            print("Error saving object, reason: \(error.localizedDescription)")
        }
    }
    
    // MARK: - OP Configuration
    func deleteAllConfiguration() {
        do {
            let realm = try Realm()
            try realm.write {
                let configurations = realm.objects(OPConfiguration.self)
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
    
    func deleteAllOIDCClient() {
        do {
            let realm = try Realm()
            try realm.write {
                let configurations = realm.objects(OIDCClient.self)
                realm.delete(configurations)
            }
        } catch(let error) {
            print("Error deleting object, reason: \(error.localizedDescription)")
        }
    }
    
    func incrementUseCounter(credential: PublicKeyCredentialSource) -> Int {
        if let publicKeyCredentialSources: [PublicKeyCredentialSource] = getObjects(), let publicKeyCredentialSource = publicKeyCredentialSources.first(where: { $0.rpId == credential.rpId }) {
            do {
                let realm = try Realm()
                
                try realm.write {
                    var keyUseCounter = publicKeyCredentialSource.keyUseCounter
                    keyUseCounter += 1
                    publicKeyCredentialSource.keyUseCounter = keyUseCounter
                    
                    return keyUseCounter
                }
            } catch(let error) {
                print("Error saving object, reason: \(error.localizedDescription)")
                return credential.keyUseCounter
            }
        } else {
            return credential.keyUseCounter
        }
        
        return credential.keyUseCounter
    }
}
