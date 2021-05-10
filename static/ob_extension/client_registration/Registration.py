
from io.jans.model.custom.script.type.client import ClientRegistrationType
from io.jans.service.cdi.util import CdiUtil
from io.jans.as.model.util import JwtUtil
from io.jans.as.model.util import CertUtils
from io.jans.as.model.jwt import Jwt
from io.jans.as.server.service.net import HttpService
from org.json import JSONObject
from io.jans.as.model.crypto import  AuthCryptoProvider
from io.jans.as.model.crypto.signature import SignatureAlgorithm
from io.jans.as.server.service.net import HttpService
from java.io import File
from java.io import FileInputStream
from java.io import FileReader
from java.io import IOException
from java.net import URLEncoder
from java.security import KeyFactory
from java.security import KeyStore
from java.security import NoSuchAlgorithmException
from java.security import PrivateKey
from java.security.spec import EncodedKeySpec
from java.security.spec import InvalidKeySpecException
from java.security.spec import PKCS8EncodedKeySpec
from java.time import Instant
from java.util import LinkedHashMap
from java.util import Map
from java.util import UUID
from javax.net.ssl import SSLContext
from org.apache.http import HttpResponse
from org.apache.http.client import HttpClient
from org.apache.http.client.methods import  HttpGet
from org.apache.http.client.methods import HttpPost
from org.apache.http.entity import StringEntity
from org.apache.http.impl.client import HttpClients
from org.apache.http.ssl import SSLContexts
from org.apache.http.util import EntityUtils
from org.bouncycastle.util.io.pem import PemObject
from org.bouncycastle.util.io.pem import PemReader
from org.jose4j.jws import JsonWebSignature
from org.jose4j.jwt import JwtClaims
from org.json import JSONObject
from  java.lang import StringBuilder 
from java.lang import String
from java.lang import System
from java.util import  HashMap
import java
import json
import uuid
import calendar
import time
import sys


class ClientRegistration(ClientRegistrationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Client registration. Initialization"
 
        if (not configurationAttributes.containsKey("keyId")):
	        print "Client registration. Initialization. Property keyId is not specified"
	        return False
        else: 
        	self.keyId = configurationAttributes.get("keyId").getValue2() 
            
        if (not configurationAttributes.containsKey("clientScopes")):
	        print "Client registration. Initialization. Property clientScopes is not specified"
	        return False
        else: 
        	self.clientScopes = configurationAttributes.get("clientScopes").getValue2() 

        if (not configurationAttributes.containsKey("signingCert")):
	        print "Client registration. Initialization. Property signingCert is not specified"
	        return False
        else: 
        	self.signingCert = configurationAttributes.get("signingCert").getValue2() 

        if (not configurationAttributes.containsKey("signingKey")):
	        print "Client registration. Initialization. Property signingKey is not specified"
	        return False
        else: 
        	self.signingKey = configurationAttributes.get("signingKey").getValue2() 
        
        if (not configurationAttributes.containsKey("transportKeyStore")):
	        print "Client registration. Initialization. Property transportKeyStore is not specified"
	        return False
        else: 
        	self.transportKeyStore = configurationAttributes.get("transportKeyStore").getValue2() 

        if (not configurationAttributes.containsKey("transportKeyStorePassword")):
	        print "Client registration. Initialization. Property transportKeyStorePassword is not specified"
	        return False
        else: 
        	self.transportKeyStorePassword = configurationAttributes.get("transportKeyStorePassword").getValue2() 


	if (not configurationAttributes.containsKey("trustKeyStore")):
	        print "Client registration. Initialization. Property trustKeyStore is not specified"
	        return False
        else: 
        	self.trustKeyStore = configurationAttributes.get("trustKeyStore").getValue2() 

        if (not configurationAttributes.containsKey("trustKeyStorePassword")):
	        print "Client registration. Initialization. Property trustKeyStorePassword is not specified"
	        return False
        else: 
        	self.trustKeyStorePassword = configurationAttributes.get("trustKeyStorePassword").getValue2() 

        if (not configurationAttributes.containsKey("jwks_endpoint")):
	        print "Client registration. Initialization. Property jwks_endpoint is not specified"
	        return False
        else: 
        	self.jwks_endpoint = configurationAttributes.get("jwks_endpoint").getValue2() 
            
        if (not configurationAttributes.containsKey("tokenUrl")):
	        print "Client registration. Initialization. Property tokenUrl is not specified"
	        return False
        else: 
        	self.tokenUrl = configurationAttributes.get("tokenUrl").getValue2() 
            
            
        if (not configurationAttributes.containsKey("tppUrl")):
	        print "Client registration. Initialization. Property tppUrl is not specified"
	        return False
        else: 
        	self.tppUrl = configurationAttributes.get("tppUrl").getValue2() 
        
        print "Client registration. Initialized successfully"
        
        if (not configurationAttributes.containsKey("aud")):
	        print "Client registration. Initialization. Property aud is not specified"
	        return False
        else: 
        	self.aud = configurationAttributes.get("aud").getValue2() 
       

        if (not configurationAttributes.containsKey("rootCert")):
	        print "Client registration. Initialization. Property rootCert is not specified"
	        return False
        else: 
        	self.rootCert = configurationAttributes.get("rootCert").getValue2() 
              
        print "Client registration. Initialized successfully"

        if (not configurationAttributes.containsKey("issuingaca")):
	        print "Client registration. Initialization. Property issuingaca is not specified"
	        return False
        else: 
        	self.issuingaca = configurationAttributes.get("issuingaca").getValue2() 
  
        if (not configurationAttributes.containsKey("signingaca")):
	        print "Client registration. Initialization. Property signingaca is not specified"
	        return False
        else: 
        	self.signingaca = configurationAttributes.get("signingaca").getValue2() 
  
  
        print "Client registration. Initialized successfully"

        
        return True

    def destroy(self, configurationAttributes):
        print "Client registration. Destroy"
        print "Client registration. Destroyed successfully"
        return True

    def createClient(self, registerRequest, client, configurationAttributes):
        print "Client registration. CreateClient method"

        cert = CertUtils.x509CertificateFromPem(configurationAttributes.get("certProperty").getValue1())
        cn = CertUtils.getCN(cert)
        valid = self.validateSoftwareStatement(cn)
        if valid == False:
             print "Invalid software statement"
             return False
        print "Client registration. cn: " + cn
        client.setDn("inum=" + cn + ",ou=clients,o=jans")

        #only authentication, consent is managed by Internal OP
        client.setTrustedClient(True)
        client.setPersistClientAuthorizations(False)

        # inorder to run introspection script
        client.setAccessTokenAsJwt(True)
        client.getAttributes().setRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims(True)  
        dnOfIntrospectionScript = "inum=CABA-2222,ou=scripts,o=jans"
        client.getAttributes().getIntrospectionScripts().add(dnOfIntrospectionScript)
        
        client.setClientId(cn)
        client.setJwksUri(Jwt.parse(registerRequest.getSoftwareStatement()).getClaims().getClaimAsString("org_jwks_endpoint"))
        
        return True
  
    def updateClient(self, registerRequest, client, configurationAttributes):
        print "Client registration. UpdateClient method"
        return True

    def getApiVersion(self):
        return 11

    def getSoftwareStatementHmacSecret(self, context):
        return ""

    # used for signing the software statement
    def getSoftwareStatementJwks(self, context):
        print "Client registration. getSoftwareStatementJwks method"
        return JwtUtil.getJSONWebKeys(self.jwks_endpoint).toString()

    def getDcrHmacSecret(self, context):
        return ""
        
    # used for signing the request object (DCR) 
    def getDcrJwks(self, context):
        print "Client registration. getDcrJwks method"
        return JwtUtil.getJSONWebKeys(self.jwks_endpoint).toString()

    # implementation details - https://openbanking.atlassian.net/wiki/spaces/DZ/pages/1150124033/Directory+2.0+Technical+Overview+v1.5#Directory2.0TechnicalOverviewv1.5-ManageDirectoryInformation
    def validateSoftwareStatement(self, softwareStatementId):
    
        accessToken = self.getAccessToken(softwareStatementId)
	passed = self.verifyRoles(accessToken, softwareStatementId)
	print "Software verification passed : "+ str(passed)
        return passed
   
		        
    def buildPostDataFortoken(self, encodedJWT, softwareStatementId) :
		postParameters = LinkedHashMap()
		postParameters.put("scope", self.clientScopes)
		postParameters.put("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
		postParameters.put("grant_type", "client_credentials")
		postParameters.put("client_id", softwareStatementId)
		postParameters.put("client_assertion", encodedJWT)

		postData = StringBuilder()
		for param in postParameters.entrySet():
			if postData.length() != 0:
				postData.append('&')
			postData.append(URLEncoder.encode(param.getKey(), "UTF-8"))
			postData.append('=')
			postData.append(URLEncoder.encode(String(param.getValue()), "UTF-8"))
		print "Post data: "+postData.toString()
		return postData.toString()

    def getSslContext(self) :
	keyStore = KeyStore.getInstance("PKCS12")
        pwdArray = [x for x in self.transportKeyStorePassword]
        trustPwdArray = [x for x in self.trustKeyStorePassword]
	keyStore.load( FileInputStream(self.transportKeyStore), pwdArray)
        
	sslContext = SSLContexts.custom().loadKeyMaterial(keyStore,   pwdArray).loadTrustMaterial( File(self.trustKeyStore), trustPwdArray).build()
	return sslContext	

    def verifyRoles1(self, accessToken, softwareStatementId) :
		try :
			sslContext = self.getSslContext()
			httpClient = HttpClients.custom().setSSLContext(sslContext).build()
			get = HttpGet(self.tppUrl+"?filter="+ URLEncoder.encode(self.buildFilter(softwareStatementId)) + "&attributes=totalResults")
			get.setHeader("Authorization", "Bearer " + accessToken)
			response = httpClient.execute(get)
			print(response.getStatusLine())
			jsonResponse = EntityUtils.toString(response.getEntity())
			object = JSONObject(jsonResponse)
			total = int(object.get("totalResults"))
			return True if total > 0 else False
		except:
			print "Client registration. Failed to verifyRoles: %s" %(sys.exc_info()[1])
			return False

    def buildFilter(self, softwareStatementId) :
		#filter = "(urn:openbanking:softwarestatement:1.0:SoftwareStatements[Id eq \"" + softwareStatementId
		#		+ "\" and Active eq true] ) and urn:openbanking:competentauthorityclaims:1.0:Authorisations[  #MemberState eq \"GBR\"  and Psd2Role eq \"PISP\" and Status eq \"Active\" ])"
		filter = "(urn:openbanking:softwarestatement:1.0:SoftwareStatements[Id eq \"" + softwareStatementId	+ "\" and Active eq true] ) "
		return filter	
    

    def getEncodedJWTForToken(self, softwareStatementId, clientScopes, aud, kid,signingKeyFile):
		jws = JsonWebSignature()
		claims = JwtClaims()
		claims.setClaim("iss", softwareStatementId)
		claims.setClaim("sub", softwareStatementId)
		claims.setClaim("scope", clientScopes)
		claims.setClaim("aud", aud)
		claims.setClaim("jti", UUID.randomUUID())
		unixTime = Instant.now().getEpochSecond()
		claims.setClaim("iat", unixTime)
		claims.setClaim("exp", unixTime + 1000) # 60000 one min
		jws.setPayload(claims.toJson())

		# dont change the order
		jws.getHeaders().setObjectHeaderValue("typ", "JWT");
		jws.setAlgorithmHeaderValue("RS256");
		jws.getHeaders().setObjectHeaderValue("kid", kid);

		privateKey = self.getPrivateKey(signingKeyFile);
		jws.setKey(privateKey);

		jwsCompactSerialization = jws.getCompactSerialization();
		return jwsCompactSerialization;
	
    def getPrivateKey(self, file) :
		reader = PemReader(FileReader(File(file)))
		pemObject = reader.readPemObject()
		content = pemObject.getContent()
		try:
			kf = KeyFactory.getInstance("RSA");
			keySpec = PKCS8EncodedKeySpec(content);
			privateKey = kf.generatePrivate(keySpec);
			return privateKey
		except:
			print "Client registration. Failed to getPrivateKey: %s" %(sys.exc_info()[1])
			return False

    def getAccessToken(self, softwareStatementId) :
           
	   try:
	     	sslContext = self.getSslContext()
	    	httpClient = HttpClients.custom().setSSLContext(sslContext).build()
           	headers = { "Content-type" : "application/x-www-form-urlencoded" }
                httpService = CdiUtil.bean(HttpService)
                jwt = self.getEncodedJWTForToken(softwareStatementId, self.clientScopes, self.tokenUrl , self.keyId, self.signingKey )
                http_service_response = httpService.executePost(httpClient, self.tokenUrl, None, headers , self.buildPostDataFortoken(jwt,softwareStatementId))
						
                http_response = http_service_response.getHttpResponse()
           except:
            	print "Client Registration. getAccessToken", sys.exc_info()[1]
            	return False

           try:
                if not httpService.isResponseStastusCodeOk(http_response):
                   	print "Cert. Client Registration. getAccessToken. Get invalid response from server: ", str(http_response.getStatusLine().getStatusCode())
                	httpService.consume(http_response)
                	return False
    
            	response_bytes = httpService.getResponseContent(http_response)
            	response_string = httpService.convertEntityToString(response_bytes)
            	httpService.consume(http_response)
           finally:
           	http_service_response.closeConnection()

           if response_string == None:
            	print "Client Registration. getAccessToken. Got empty response from validation server"
            	return False
        
	   response = json.loads(response_string)
           print "response access token: "+ response["access_token"]
	   return response["access_token"]

    def  verifyRoles(self, accessToken, softwareStatementId) :
		header =  { "Authorization": "Bearer " + accessToken }
        	try:
            		sslContext = self.getSslContext()
	    		httpClient = HttpClients.custom().setSSLContext(sslContext).build()
                        httpService = CdiUtil.bean(HttpService)
                        http_service_response = httpService.executeGet(httpClient, self.tppUrl+"?filter="+ URLEncoder.encode(self.buildFilter(softwareStatementId)) + "&attributes=totalResults",  header )
			http_response = http_service_response.getHttpResponse()
		except:
                        print "Client Registration. verification. Exception: ", sys.exc_info()[1]
                        return False

                try:
                        if not httpService.isResponseStastusCodeOk(http_response):
                        	print "Client Registration. verification. Got invalid response from validation server: ", str(http_response.getStatusLine().getStatusCode())
                        	httpService.consume(http_response)
                        	return False
    
                        response_bytes = httpService.getResponseContent(http_response)
                        response_string = httpService.convertEntityToString(response_bytes)
                        httpService.consume(http_response)
                finally:
                        http_service_response.closeConnection()

                if response_string == None:
                        print "Client Registration. verification. Got empty response from location server"
                        return False
        
                response = json.loads(response_string)
                
                print "int --> "+ str(int(response['totalResults']))
                if int(response['totalResults']) <= 0  :
                        print "Client Registration. verification. No matches found: '%s'" % response['totalResults']
                        return False

                return True
    