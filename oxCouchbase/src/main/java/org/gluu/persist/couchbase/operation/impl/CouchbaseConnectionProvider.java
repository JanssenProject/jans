/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */

package org.gluu.persist.couchbase.operation.impl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.gluu.persist.couchbase.model.BucketMapping;
import org.gluu.persist.couchbase.model.ResultCode;
import org.gluu.persist.model.AttributeData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.util.StringHelper;

import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.core.message.kv.subdoc.multi.Mutation;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.subdoc.SubDocumentException;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.query.Select;
import com.couchbase.client.java.query.SimpleN1qlQuery;
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.subdoc.DocumentFragment;
import static com.couchbase.client.java.query.Select.select;
import static com.couchbase.client.java.query.dsl.Expression.i;
/**
 * Perform cluster initialization and open required buckets
 *
 * @author Yuriy Movchan Date: 05/10/2018
 */
public class CouchbaseConnectionProvider {

    private static final Logger log = LoggerFactory.getLogger(CouchbaseConnectionProvider.class);

    private Properties props;

    private String[] servers;
    private String[] buckets;

    private String userName;
    private String userPassword;

    private int creationResultCode;

    private CouchbaseCluster cluster;
    private HashMap<String, BucketMapping> bucketToBaseNameMapping;
    private HashMap<String, BucketMapping> baseNameToBucketMapping;

    private ArrayList<String> binaryAttributes, certificateAttributes;

    protected CouchbaseConnectionProvider() {
    }

    public CouchbaseConnectionProvider(Properties props) {
        this.props = props;
    }

    public void create() {
        try {
            init();
        } catch (Exception ex) {
            this.creationResultCode = ResultCode.OPERATIONS_ERROR_INT_VALUE;

            Properties clonedProperties = (Properties) props.clone();
            if (clonedProperties.getProperty("userName") != null) {
                clonedProperties.setProperty("userPassword", "REDACTED");
            }

            log.error("Failed to create connection with properties: '{}'", clonedProperties, ex);
        }
    }

    protected void init() {
        this.servers = StringHelper.split(props.getProperty("servers"), ",");

        this.userName = props.getProperty("userName");
        this.userPassword = props.getProperty("userPassword");

        this.buckets = StringHelper.split(props.getProperty("buckets"), ",");
        this.bucketToBaseNameMapping = new HashMap<String, BucketMapping>();
        this.baseNameToBucketMapping = new HashMap<String, BucketMapping>();

        openWithWaitImpl();
        log.info("Opended: '{}' buket with base names: '{}'", bucketToBaseNameMapping.keySet(), baseNameToBucketMapping.keySet());

        this.binaryAttributes = new ArrayList<String>();
        if (props.containsKey("binaryAttributes")) {
            String[] binaryAttrs = StringHelper.split(props.get("binaryAttributes").toString().toLowerCase(), ",");
            this.binaryAttributes.addAll(Arrays.asList(binaryAttrs));
        }
        log.debug("Using next binary attributes: '{}'", binaryAttributes);

        this.certificateAttributes = new ArrayList<String>();
        if (props.containsKey("certificateAttributes")) {
            String[] binaryAttrs = StringHelper.split(props.get("certificateAttributes").toString().toLowerCase(), ",");
            this.certificateAttributes.addAll(Arrays.asList(binaryAttrs));
        }
        log.debug("Using next binary certificateAttributes: '{}'", certificateAttributes);

        this.creationResultCode = ResultCode.SUCCESS_INT_VALUE;
    }

    private void openWithWaitImpl() {
        String connectionMaxWaitTime = props.getProperty("connection-max-wait-time");
        int connectionMaxWaitTimeSeconds = 30;
        if (StringHelper.isNotEmpty(connectionMaxWaitTime)) {
            connectionMaxWaitTimeSeconds = Integer.parseInt(connectionMaxWaitTime);
        }
        log.debug("Using Couchbase connection timeout: '{}'", connectionMaxWaitTimeSeconds);

        CouchbaseException lastException = null;

        int attempt = 0;
        long currentTime = System.currentTimeMillis();
        long maxWaitTime = currentTime + connectionMaxWaitTimeSeconds * 1000;
        do {
            attempt++;
            if (attempt > 0) {
                log.info("Attempting to create connection: '{}'", attempt);
            }

            try {
                open();
                break;
            } catch (CouchbaseException ex) {
                lastException = ex;
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                log.error("Exception happened in sleep", ex);
                return;
            }
            currentTime = System.currentTimeMillis();
        } while (maxWaitTime > currentTime);

        if (lastException != null) {
            throw lastException;
        }
    }

    private void open() {
        this.cluster = CouchbaseCluster.create(servers);
        cluster.authenticate(userName, userPassword);

        // Open required buckets
        for (String bucketName : buckets) {
            String baseNamesProp = props.getProperty(String.format("bucket.%s.mapping", bucketName), "");
            String[] baseNames = StringHelper.split(baseNamesProp, ",");

            Bucket bucket = cluster.openBucket(bucketName);

            BucketMapping bucketMapping = new BucketMapping(bucketName, bucket);

            // Store in separate map to speed up search by base name
            bucketToBaseNameMapping.put(bucketName, bucketMapping);
            for (String baseName : baseNames) {
                baseNameToBucketMapping.put(baseName, bucketMapping);
            }

            // Create primary index if needed
            bucket.bucketManager().createN1qlPrimaryIndex(true, false);
        }
    }

    public boolean destory() {
        for (BucketMapping bucketMapping : bucketToBaseNameMapping.values()) {
            try {
                bucketMapping.getBucket().close();
            } catch (CouchbaseException ex) {
                log.error("Failed to close bucket '{}'", bucketMapping.getBucketName(), ex);

                return false;
            }
        }

        return cluster.disconnect();
    }

    public boolean isConnected() {
        if (cluster == null) {
            return false;
        }

        boolean isConnected = true;
        Statement query = Select.select("1");
        for (BucketMapping bucketMapping : bucketToBaseNameMapping.values()) {
            try {
                Bucket bucket = bucketMapping.getBucket();
                if (bucket.isClosed() || !bucket.query(query).finalSuccess()) {
                    log.error("Bucket '{}' is invalid", bucketMapping.getBucketName());
                    isConnected = false;
                    break;
                }
            } catch (CouchbaseException ex) {
                log.error("Failed to check bucket", ex);
            }
        }

        return isConnected;
    }

    public BucketMapping getBucketMapping(String baseName) {
        BucketMapping bucketMapping = baseNameToBucketMapping.get(baseName);
        if (bucketMapping == null) {
            return null;
        }

        return bucketMapping;
    }

    public BucketMapping getBucketMappingByKey(String key) {
        // TODO Auto-generated method stub
        return null;
    }

    public int getCreationResultCode() {
        return creationResultCode;
    }

    public boolean isCreated() {
        return ResultCode.SUCCESS_INT_VALUE == creationResultCode;
    }

    public String[] getServers() {
        return servers;
    }

    public final String getUserName() {
        return userName;
    }

    public final String getUserPassword() {
        return userPassword;
    }

    public ArrayList<String> getBinaryAttributes() {
        return binaryAttributes;
    }

    public ArrayList<String> getCertificateAttributes() {
        return certificateAttributes;
    }

    public boolean isBinaryAttribute(String attributeName) {
        if (StringHelper.isEmpty(attributeName)) {
            return false;
        }

        return binaryAttributes.contains(attributeName.toLowerCase());
    }

    public boolean isCertificateAttribute(String attributeName) {
        if (StringHelper.isEmpty(attributeName)) {
            return false;
        }

        return certificateAttributes.contains(attributeName.toLowerCase());
    }

    public static void main(String[] args) {
        Properties prop = new Properties();
        prop.put("servers", "localhost");
        prop.put("userName", "admin");
        prop.put("userPassword", "secret");
        prop.put("buckets", "gluu, travel-sample");
        prop.put("bucket.gluu.mapping", "gluu");

        CouchbaseConnectionProvider provider = new CouchbaseConnectionProvider(prop);
        provider.create();

        System.err.println(provider.isConnected());

        selectListByOC(provider);
//        updatePerson2(provider, "people_@!5304.5F36.0E64.E1AC!0001!179C.62D7!0000!1248.7F09.A58E.703D");
//        JsonDocument doc = getPerson(provider, "people_@!5304.5F36.0E64.E1AC!0001!179C.62D7!0000!1248.7F09.A58E.703D");
//        byte[] data = new byte[200];
//        data[0] = 1;
//        data[199] = 1;
//        doc.content().put("test", new BigInteger(data));
//        System.out.println(doc);
//        getAttributeDataList(doc);
        


        provider.destory();
    }

    protected static void selectListByOC(CouchbaseConnectionProvider provider) {
        Bucket bucket = provider.getBucketMapping("gluu").getBucket();
        SimpleN1qlQuery query = N1qlQuery.simple("SELECT * FROM `gluu` b WHERE META(b).id LIKE 'people_%' AND 'gluuPerson' IN objectClass");
        N1qlQueryResult result = bucket.query(query);
        if (result.finalSuccess()) {
            System.out.println(result.allRows().get(0).value());
            System.out.println(result.info().resultCount() + " :" + query);
        } else {
            System.err.println("Error! " + result.errors());
        }
    }
/*
    protected static void selectListByOCWithFilter(CouchbaseConnectionProvider provider) {
        Bucket bucket = provider.getBucketMapping("gluu").getBucket();
        SimpleN1qlQuery query = N1qlQuery.simple(select("*").from(i("gluu")).where(expression) b WHERE META(b).id LIKE 'people_%' AND 'gluuPerson' IN objectClass");
        N1qlQueryResult result = bucket.query(query);
        if (result.finalSuccess()) {
            System.out.println(result.allRows().get(0).value());
            System.out.println(result.info().resultCount() + " :" + query);
        } else {
            System.err.println("Error! " + result.errors());
        }
    }
*/
    protected static void updatePerson(CouchbaseConnectionProvider provider, String docId) {
        try {
            Bucket bucket = provider.getBucketMapping("gluu").getBucket();
            DocumentFragment<Mutation> result = bucket.mutateIn(docId).remove("givenName").upsert("givenName", "bla-bla2").insert("aaa", "aaa").execute();
            System.out.println(result.size());
        } catch (SubDocumentException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        }
    }
    protected static void addPerson(CouchbaseConnectionProvider provider, String docId, JsonObject content) {
        try {
            Bucket bucket = provider.getBucketMapping("gluu").getBucket();
            JsonDocument doc = JsonDocument.create(docId, content);
            JsonDocument result = bucket.upsert(doc);
            System.out.println(result);
        } catch (CouchbaseException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        }
    }

    protected static void updatePerson2(CouchbaseConnectionProvider provider, String docId) {
        try {
            byte[] data = new byte[200];
            data[0] = 1;
            data[199] = 1;

            Bucket bucket = provider.getBucketMapping("gluu").getBucket();
//            DocumentFragment<Mutation> result = bucket.mutateIn(docId).insert("a2", new Integer(12345)).execute();
//            DocumentFragment<Mutation> result2 = bucket.mutateIn(docId).insert("a3", new BigInteger(data)).execute();
            DocumentFragment<Mutation> result3 = bucket.mutateIn(docId).insert("a4", new Date()).execute();
            System.out.println(result3.size());
        } catch (SubDocumentException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        }
    }

    protected static JsonDocument getPerson(CouchbaseConnectionProvider provider, String docId) {
        try {
            Bucket bucket = provider.getBucketMapping("gluu").getBucket();
            JsonDocument result = bucket.get(docId);
            System.out.println(result);
            
            return result;
        } catch (CouchbaseException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        }
        
        return null;
    }

    protected static void importSql(Bucket bucket) {
        try {
            FileReader inFile = new FileReader("V:/gluu.sql");
            BufferedReader inStream = new BufferedReader(inFile);
            String inString;

            while ((inString = inStream.readLine()) != null) {
                SimpleN1qlQuery query = N1qlQuery.simple(inString);
                N1qlQueryResult result = bucket.query(query);
                // int idx = inString.indexOf("ENTRY:");
                // String doc_key = inString.substring(5, idx - 1);
                // String doc_value = inString.substring(idx + 7, inString.length());
                // System.out.println(doc_value);
                // doc_value = doc_value.replaceAll("\'", "");
                // JsonDocument result = bucket.upsert(JsonDocument.create(doc_key,
                // JsonObject.fromJson(doc_value)));

                if (!result.finalSuccess()) {
                    System.out.println(query);
                }
            }
            inStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void  getAttributeDataList(JsonDocument entry) {
        List<AttributeData> result = new ArrayList<AttributeData>();
        JsonObject content = entry.content();
        for (String attributeName : content.getNames()) {
            Object attribute = content.get(attributeName);
            System.out.println(attributeName + " : " + attribute + " : " + attribute.getClass().toGenericString());
        }
    }

}
