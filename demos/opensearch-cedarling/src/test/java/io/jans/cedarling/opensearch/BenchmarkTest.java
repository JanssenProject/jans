package io.jans.cedarling.opensearch;

import java.util.*;
import java.security.*;

import org.apache.logging.log4j.*;
import org.json.*;
import org.testng.annotations.*;
import org.testng.ITestContext;

import static org.testng.Assert.*;
import static java.nio.charset.StandardCharsets.UTF_8;

public class BenchmarkTest {
    
    private static final int MAX_GPA = 5;
     
    private Logger logger = LogManager.getLogger(getClass());
    private NetworkUtil nu = null;
    private Random ranma = new SecureRandom();
    
    private int entries;
    private String indexName;
    private String bulkEntryTemplate;
    private String queryTemplate;
    private boolean useCedarling;
    
    @BeforeClass
    public void initTestSuite(ITestContext context) throws Exception {

        //See AlterSuiteListener class first please
        Map<String, String> params = context.getSuite().getXmlSuite().getParameters();
        String user = params.get("user");
        String pwd = params.get("password");
        
        byte[] bytes = Base64.getUrlEncoder().encode((user + ":" + pwd).getBytes());
        nu = new NetworkUtil(params.get("apiBase"), "Basic " + new String(bytes, UTF_8));
        
        entries = Integer.parseInt(params.get("entries"));
        indexName = params.get("indexName"); 
        bulkEntryTemplate = params.get("bulkEntryTemplate");
        queryTemplate = params.get("queryFile");
        useCedarling = Boolean.valueOf(params.get("useCedarling"));
        
    }
    
    @Test
    public void dropIndex() throws Exception {
        
        logger.info("Deleting index {}...", indexName);
        JSONObject obj = nu.sendDelete(indexName, 200, 404);

        logger.debug("Checking result of delete operation");
        if (obj.optInt("status") != 404) {
            assertEquals(obj.getBoolean("acknowledged"), true);
        }

    }
    
    @Test(dependsOnMethods="dropIndex")
    public void fillIndex() throws Exception {
        
        logger.info("Creating documents...");
        String payload = "";
        
        for (int i = 0; i < entries; i++) {
            payload += String.format(bulkEntryTemplate, getAString(), 
                        getADecimal(2024, 2028), ranma.nextFloat() * MAX_GPA);
        }
        
        logger.info("Payload of {} {} documents generated ({} bytes)", entries, indexName, payload.getBytes().length);
        JSONObject obj = nu.sendPost(indexName + "/_bulk?refresh=true&filter_path=-items", 200, payload);
        //refresh param allows the inserted documents to be immediately available for search after the POST is submitted
        //filter_path elides the metadata associated to every insertion attempt, reducing the response size considerably

        logger.debug("Checking result of bulk operation");
        assertFalse(obj.getBoolean("errors"));
        
    }
    
    @Test(dependsOnMethods="fillIndex")
    public void runQueries() throws Exception {
        
        int perfectScorers = warmUpQuery();
        if (useCedarling) {
            cedarlingQueries(perfectScorers);
        } else {
            regularQueries();
        }
        
    }
    
    //The very first query after the bulk is run tends to be very slow. Hence, a dummy query is issued and discarded
    public int warmUpQuery() throws Exception {
        
        //This one will likely produce zero results, however in practice MAX_GPA multiplied by a random float
        //(see method fillIndex) may yield exactly MAX_GPA
        String query = String.format(queryTemplate, MAX_GPA, MAX_GPA + 1);        
        logger.info("Sending warmup query...");
        
        JSONObject obj = nu.sendPost(indexName + "/_search?size=" + entries, 200, query);        
        //filter_path=-hits.hits can be used to elide the actual document hits from the response reducing its size considerably
        logger.info("Took {}ms \n", obj.getInt("took"));
        return obj.getJSONObject("hits").getJSONObject("total").getInt("value");
        
    }
    
    public void regularQueries() throws Exception {
        
        int queryTookMs = 0;
        //Issue several different queries and compute average "took" time
        for (int i = 0; i < MAX_GPA; i++) {
            String query = String.format(queryTemplate, i, i + 1);
            logger.info("Sending regular query #{}...", i + 1);
            
            JSONObject obj = nu.sendPost(indexName + "/_search?size=" + entries, 200, query);
            queryTookMs += obj.getInt("took");
        }
        logger.info("");
        logger.info("Average regular query time (ms): {}", String.format("%.3f", 1.0f*queryTookMs / MAX_GPA));
        
    }

    public void cedarlingQueries(int perfectScorers) throws Exception {

        long decisionTime = 0;
        int queryTookMs = 0;
        int totalResults = 0, emptyResultSets = 0;
        //Issue several different queries and compute average "took" and decision time
        for (int i = 0; i < MAX_GPA; i++) {
            String query = String.format(queryTemplate, i, i + 1);
            logger.info("Sending regular query #{}...", i + 1);
            
            JSONObject obj = nu.sendPost(indexName + "/_search?search_pipeline=cedarling_search&size=" + entries, 200, query);
            queryTookMs += obj.getInt("took");
            
            long adt = obj.getJSONObject("ext").getJSONObject("cedarling").getInt("average_decision_time");
            int res = obj.getJSONObject("hits").getJSONObject("total").getInt("value");
            
            if (adt == -1) {
                //No decisions performed, ie. empty result set. This may occur when the amount of generated documents is small
                emptyResultSets++;
                assertEquals(res, 0);
            } else {
                decisionTime += adt;
                totalResults += res;
            } 
        }
        
        assertEquals(totalResults + perfectScorers, entries);
        logger.info("");
        logger.info("Average plugin query time (ms): {}", String.format("%.3f", 1.0f*queryTookMs / MAX_GPA));
        logger.info("Average Cedarling Java decision time per document (ms): {}",
                String.format("%.3f", decisionTime / ((MAX_GPA - emptyResultSets) * 1000.0f)));
        
    }
    
    private String getAString() {

        //radix 36 entails characters: 0-9 plus a-z
        String path = Integer.toString(ranma.nextInt(), Math.min(36, Character.MAX_RADIX));
        //path will have at most 6 chars in practice
        return path.substring(path.charAt(0) == '-' ? 1 : 0);
        
    }
    
    private int getADecimal(int min, int max) {
        //Pick a uniformly distributed random number from the range [min, max)
        return ranma.nextInt(max - min + 1) + min;
    }    
    
}
