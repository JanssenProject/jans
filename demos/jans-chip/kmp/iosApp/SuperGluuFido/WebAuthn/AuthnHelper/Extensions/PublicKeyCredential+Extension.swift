//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 01.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

extension PublicKeyCredential {
    func getAll() -> [PublicKeyCredentialSource]? {
        return try? db.loadAll().get()
    }

    func deleteAll(_ aaguid: String) -> Result<Void, Error> {
        do {
            try db.deleteAll(aaguid)
            return .success(())
        } catch {
            return .failure(error)
        }
    }

    func deleteAll() -> Result<Void, Error> {
        do {
            try db.deleteAll()
            return .success(())
        } catch {
            return .failure(error)
        }
    }
}
