//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 01.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation
import SQLite3

enum SqlLiteError: Error {
    case storeFailed
    case loadFailed
    case deleteFailed
}

class WebAuthnCredentialSourceStorage: CredentialSourceStorage {
    let fileManager = FileManager.default
    let filePath = try? FileManager.default.url(for: .documentDirectory,
                                                in: .userDomainMask,
                                                appropriateFor: nil,
                                                create: false).appendingPathComponent("credential.sqlite")
    var db: OpaquePointer?
    let tablename = "WebAuthnCredentialSource"

    init?() {
        guard let path = filePath else { return nil }
        let flags = SQLITE_OPEN_CREATE|SQLITE_OPEN_READWRITE|SQLITE_OPEN_FULLMUTEX // full mutex
        guard sqlite3_open_v2(path.path, &db, flags, nil) == SQLITE_OK  else {
            return nil
        }
        let query = """
        CREATE TABLE IF NOT EXISTS \(tablename) (
            credId TEXT PRIMARY KEY NOT NULL,
            type TEXT NOT NULL,
            aaguid TEXT NOT NULL,
            userId TEXT,
            rpId TEXT NOT NULL,
            userName TEXT,
            userDisplayName TEXT,
            signatureCounter UNSIGNED INTEGER NOT NULL
        );
        """
        guard sqlite3_exec(db, query, nil, nil, nil) == SQLITE_OK else {
            return nil
        }
    }

    deinit {
        sqlite3_close(db)
    }

    func store(_ credSrc: PublicKeyCredentialSource) -> Result<(), Error> {
        let userIdQuery = if let userId = credSrc.userId { "'\(userId)'" } else { "NULL" }
        let query = """
        INSERT INTO \(tablename) (credId, type, aaguid, userId, rpId, userName, userDisplayName, signatureCounter)
        VALUES ('\(credSrc.id)', '\(credSrc.type)', '\(credSrc.aaguid)', \(userIdQuery), '\(credSrc.rpId)', '\(credSrc.userName)', '\(credSrc.userDisplayName)', 0)
        ON CONFLICT(credId) DO UPDATE SET
            type = excluded.type,
            aaguid = excluded.aaguid,
            userId = excluded.userId,
            rpId = excluded.rpId,
            userName = excluded.userName,
            userDisplayName = excluded.userDisplayName,
            signatureCounter = 0;
        """
        guard sqlite3_exec(db, query, nil, nil, nil) == SQLITE_OK else {
            return .failure(SqlLiteError.storeFailed)
        }
        return .success(())
    }

    func load(_ credId: String) -> Result<PublicKeyCredentialSource?, Error> {
        let query = """
        SELECT * FROM \(tablename)
        WHERE credId = '\(credId)';
        """
        var stmt: OpaquePointer?
        guard sqlite3_prepare_v2(db, query, -1, &stmt, nil) == SQLITE_OK else {
            return .failure(SqlLiteError.loadFailed)
        }
        guard sqlite3_step(stmt) == SQLITE_ROW else {
            return .success(nil)
        }
        let id = String(cString: sqlite3_column_text(stmt, 0))
        let type = String(cString: sqlite3_column_text(stmt, 1))
        let aaguid = String(cString: sqlite3_column_text(stmt, 2))
        let userId: String? = if let userId = sqlite3_column_text(stmt, 3) { String(cString: userId) } else { nil }
        let rpId = String(cString: sqlite3_column_text(stmt, 4))
        let userName = String(cString: sqlite3_column_text(stmt, 5))
        let userDisplayName = String(cString: sqlite3_column_text(stmt, 6))
        sqlite3_finalize(stmt)

        return .success(PublicKeyCredentialSource(id: id, type: type, aaguid: aaguid, userId: userId, rpId: rpId, userName: userName, userDisplayName: userDisplayName))
    }

    func loadAll() -> Result<[PublicKeyCredentialSource]?, Error> {
        let query = "SELECT * FROM \(tablename);"
        var stmt: OpaquePointer?
        guard sqlite3_prepare_v2(db, query, -1, &stmt, nil) == SQLITE_OK else {
            return .failure(SqlLiteError.loadFailed)
        }
        var credSrcs: [PublicKeyCredentialSource] = []
        while sqlite3_step(stmt) == SQLITE_ROW {
            let id = String(cString: sqlite3_column_text(stmt, 0))
            let type = String(cString: sqlite3_column_text(stmt, 1))
            let aaguid = String(cString: sqlite3_column_text(stmt, 2))
            let userId: String? = if let userId = sqlite3_column_text(stmt, 3) { String(cString: userId) } else { nil }
            let rpId = String(cString: sqlite3_column_text(stmt, 4))
            let userName = String(cString: sqlite3_column_text(stmt, 5))
            let userDisplayName = String(cString: sqlite3_column_text(stmt, 6))
            let credSrc = PublicKeyCredentialSource(id: id, type: type, aaguid: aaguid, userId: userId, rpId: rpId, userName: userName, userDisplayName: userDisplayName)
            credSrcs.append(credSrc)
        }
        sqlite3_finalize(stmt)
        guard !credSrcs.isEmpty else {
            return .success(nil)
        }
        return .success(credSrcs)
    }

    func delete(_ credId: String) -> Result<(), Error> {
        let query = """
        DELETE FROM \(tablename)
        WHERE credId = '\(credId)';
        """
        guard sqlite3_exec(db, query, nil, nil, nil) == SQLITE_OK else {
            return .failure(SqlLiteError.deleteFailed)
        }
        return .success(())
    }

    func increaseSignatureCounter(_ credSrcId: String) -> Result<UInt32, Error> {
        let query = """
        UPDATE \(tablename)
        SET signatureCounter = signatureCounter + 1
        WHERE credId = '\(credSrcId)';
        """
        guard sqlite3_exec(db, query, nil, nil, nil) == SQLITE_OK else {
            return .failure(SqlLiteError.storeFailed)
        }
        let query2 = """
        SELECT signatureCounter FROM \(tablename)
        WHERE credId = '\(credSrcId)';
        """
        var stmt: OpaquePointer?
        guard sqlite3_prepare_v2(db, query2, -1, &stmt, nil) == SQLITE_OK else {
            return .failure(SqlLiteError.loadFailed)
        }
        guard sqlite3_step(stmt) == SQLITE_ROW else {
            return .failure(SqlLiteError.loadFailed)
        }
        let signatureCounter = UInt32(sqlite3_column_int(stmt, 0))
        return .success(signatureCounter)
    }
}

extension WebAuthnCredentialSourceStorage {
    func deleteAll(_ aaguid: String) throws {
        let query = """
        DELETE FROM \(tablename)
        WHERE aaguid = '\(aaguid)';
        """
        guard sqlite3_exec(db, query, nil, nil, nil) == SQLITE_OK else {
            throw SqlLiteError.deleteFailed
        }
    }

    func deleteAll() throws {
        let query = """
        DELETE FROM \(tablename);
        """
        guard sqlite3_exec(db, query, nil, nil, nil) == SQLITE_OK else {
            throw SqlLiteError.deleteFailed
        }
    }
}
