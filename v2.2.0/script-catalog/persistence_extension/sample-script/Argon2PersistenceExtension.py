# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2025, Janssen
#
# Author: Yuriy Movchan
#

from io.jans.service.cdi.util import CdiUtil
from io.jans.model.custom.script.type.persistence import PersistenceType
from io.jans.orm.util import StringHelper
from io.jans.orm.operation.auth import PasswordEncryptionHelper
from io.jans.orm.operation.auth import PasswordEncryptionMethod
from org.bouncycastle.crypto.generators import Argon2BytesGenerator
from org.bouncycastle.crypto.params import Argon2Parameters

from java.util import Base64
from java.security import SecureRandom

import jarray
import base64
import java

class PersistenceExtension(PersistenceType):

    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis
        self.argon2Utils = Argon2Utils()

    def init(self, customScript, configurationAttributes):
        print "Persistence extension. Initialization"
        
        extended_params = {
            'type': 2, # 0 - "argon2d", 1 - "argon2i", 2 - "argon2id"
            'version': 19, # Version 1.3
            'memory': 7168,
            'iterations': 5,
            'parallelism': 1
        }

        self.argon_parameters =  {"salt_length" : 16, "hash_length" : 32, "extended_parameters" : extended_params }
        
        print "Persistence extension. Initialization. Argon 2 parameters for hash generation: %s" % self.argon_parameters
    
        return True

    def destroy(self, configurationAttributes):
        print "Persistence extension. Destroy"
        return True

    def getApiVersion(self):
        return 11

    def onAfterCreate(self, context, configurationAttributes):
        print "Persistence extension. Method: onAfterCreate"

    def onAfterDestroy(self, context, configurationAttributes):
        print "Persistence extension. Method: onAfterDestroy"

    def createHashedPassword(self, credential):
        print "Persistence extension. Method: createHashedPassword"
        
        if StringHelper.isEmpty(credential):
            return credential

        salt = self.argon2Utils.generate_salt(self.argon_parameters["salt_length"])
        hashed = self.argon2Utils.generate_argon2_hash(StringHelper.getBytesUtf8(credential), self.argon_parameters, salt, self.argon_parameters["hash_length"])
        hashed_password = self.argon2Utils.encode(salt, hashed, self.argon_parameters)

        return hashed_password

    def compareHashedPasswords(self, credential, storedCredential):
        print "Persistence extension. Method: compareHashedPasswords"
        
        auth_result = False
        if StringHelper.isNotEmpty(storedCredential) and storedCredential.startswith("{ARGON2}"):
            decoded = self.argon2Utils.decode(storedCredential)
            provided_hash = self.argon2Utils.generate_argon2_hash(StringHelper.getBytesUtf8(credential), decoded, decoded["salt"], len(decoded["hash"]))

            auth_result = self.argon2Utils.compare_hash(provided_hash, decoded["hash"])
        else:
            auth_result = PasswordEncryptionHelper.compareCredentials(credential, storedCredential)

        return auth_result 

class Argon2Utils:
    def __init__(self):
        self._b64encoder = Base64.getEncoder().withoutPadding()
        self._b64decoder = Base64.getDecoder()
        
        self._algDict = {0 : "argon2d", 1 : "argon2i", 2 : "argon2id"}
        self.secureRandom = SecureRandom()

    def encode(self, salt_bytes, hash_bytes, parameter_dict):
        extended_params_dict = parameter_dict["extended_parameters"]
        parts = []

        argon_type = self._algDict.get(extended_params_dict["type"])
        if argon_type is None:
            raise ValueError("Invalid algorithm type: " + extended_params_dict["type"])

        parts.append("$" + argon_type)
        parts.append("$v=" + str(extended_params_dict["version"]))
        parts.append("$m=" + str(extended_params_dict["memory"]) + ",t=" + str(extended_params_dict["iterations"]) + ",p=" + str(extended_params_dict["parallelism"]))
        
        parts.append("$" + self._b64encoder.encodeToString(salt_bytes))
        parts.append("$" + self._b64encoder.encodeToString(hash_bytes))

        return "{ARGON2}" + self._b64encoder.encodeToString(StringHelper.getBytesUtf8("".join(parts)))
    
    def decode(self, encoded_hash):
        idxEnd = encoded_hash.find("}")
        if (encoded_hash[0] != '{') or (idxEnd == -1):
            raise ValueError("Invalid encoded Argon2-hash")
        
        alg = encoded_hash[1: idxEnd]
        if alg != "ARGON2":
            raise ValueError("Password is not in Argon2-hash encoded format")

        argon2_encoded = StringHelper.utf8ToString(self._b64decoder.decode(encoded_hash[idxEnd + 1:]));
            
        parts = argon2_encoded.split('$')
        if len(parts) < 6:
            raise ValueError("Invalid encoded Argon2-hash")
        
        parts = parts[1:]
        
        argon_type = None
        type = -1
        for key, value in self._algDict.items():
            if value == parts[0]:
                type = key
                argon_type = value
                break
        
        if argon_type is None:
            raise ValueError("Invalid algorithm type: " + parts[0])
        
        version = int(parts[1][2:]) if parts[1].startswith("v=") else 0x10
        
        perf_params = parts[2].split(',')
        if len(perf_params) != 3:
            raise ValueError("Not all performance parameters specified")
        
        memory = int(perf_params[0][2:])
        iterations = int(perf_params[1][2:])
        parallelism = int(perf_params[2][2:])
        
        salt = self._b64decoder.decode(parts[3].encode('ascii'))
        hash_bytes = self._b64decoder.decode(parts[4].encode('ascii'))
        
        extended_params = {
            'type': type,
            'version': version,
            'memory': memory,
            'iterations': iterations,
            'parallelism': parallelism
        }

        return {"salt" : salt, "hash" : hash_bytes, "extended_parameters" : extended_params }

    def generate_salt(self, salt_length):
        salt = jarray.zeros(salt_length, 'b')
        self.secureRandom.nextBytes(salt);

        return salt

    def generate_argon2_hash(self, credentials, parameter_dict, salt_bytes, hash_length):
        extended_params_dict = parameter_dict["extended_parameters"]

        paramsBuilder = Argon2Parameters.Builder(extended_params_dict["type"])
        paramsBuilder.withVersion(extended_params_dict["version"])
        paramsBuilder.withMemoryAsKB(extended_params_dict["memory"])
        paramsBuilder.withIterations(extended_params_dict["iterations"])
        paramsBuilder.withParallelism(extended_params_dict["parallelism"])
        paramsBuilder.withSalt(salt_bytes)
        
        generator = Argon2BytesGenerator()
        generator.init(paramsBuilder.build())
        
        result = jarray.zeros(hash_length, 'b')
        generator.generateBytes(credentials, result)
        
        return result

    def compare_hash(self, provided, stored):
        if stored is None:
            return provided is None
        elif provided is None:
            return False
    
        if len(stored) != len(provided):
            return False
    
        result = 0
        for i in xrange(len(stored)):
            # If both bytes are equal, xor will be == 0, otherwise it will be != 0 and it will be result
            result |= (stored[i] ^ provided[i])

        return result == 0

