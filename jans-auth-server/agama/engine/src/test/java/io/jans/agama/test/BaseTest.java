package io.jans.agama.test;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebClientOptions;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import io.jans.inbound.oauth2.CodeGrantUtil;
import io.jans.inbound.oauth2.OAuthParams;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.net.URISyntaxException;
import java.net.URLEncoder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.testng.ITestContext;
import org.testng.annotations.BeforeSuite;

import static java.nio.charset.StandardCharsets.UTF_8;

import static org.testng.Assert.*;

public class BaseTest {
    
    private static String AGAMA_ACR = "agama";
    private static Map<String, String> MAP = null;

    Logger logger = LogManager.getLogger(getClass());
    WebClient client = null;
    
    BaseTest() {

        client = new WebClient(BrowserVersion.CHROME);
        WebClientOptions options = client.getOptions();

        options.setThrowExceptionOnFailingStatusCode(false);
        //skip downloading of external resources to avoid time waste
        options.setCssEnabled(false);
        options.setDownloadImages(false); 
        //prevent the finish page to autosubmit the POST to AS's postlogin endpoint
        options.setJavaScriptEnabled(false);

    }
    
    @BeforeSuite
    public void init(ITestContext context) throws IOException {

        String propertiesFile = context.getCurrentXmlTest().getParameter("propertiesFile");
        Properties prop = new Properties();
        prop.load(Files.newBufferedReader(Paths.get(propertiesFile), UTF_8));
		        
        MAP = new Hashtable<>();
        //do not bother about empty keys... but
        //If a value is found null, this will throw a NPE since we are using a Hashtable
        prop.forEach((key, value) -> MAP.put(key.toString(), value.toString()));
        context.getSuite().getXmlSuite().setParameters(MAP);

    }
    
    String authzRequestUrl(String flowQName, Map<String, Object> inputs) {

        OAuthParams p = new OAuthParams();
        p.setAuthzEndpoint(MAP.get("authzEndpoint"));
        p.setClientId(MAP.get("clientId"));
        p.setRedirectUri(MAP.get("redirectUri"));
        p.setScopes(Collections.singletonList("openid"));

        String queryParam = URLEncoder.encode(MAP.get("custParamName"), UTF_8);
        
        StringBuilder builder = new StringBuilder(flowQName);        
        if (inputs != null) {
            JSONObject jo = new JSONObject(inputs);
            builder.append("-").append(jo.toString());
        }
        
        Map<String, String> custParams = new HashMap<>();
        custParams.put("acr_values", AGAMA_ACR);
        custParams.put(queryParam, builder.toString());        
        p.setCustParamsAuthReq(custParams);        
        
        String url = null; 
        CodeGrantUtil grant = new CodeGrantUtil(p);

        try {
            url = grant.makeAuthzRequest().getFirst();
            logger.debug("Authentication request built is: {}", url);
        } catch (URISyntaxException e) {
            fail(e.getMessage(), e);
        } 
        return url;
        
    }
    
    HtmlPage launch(String flowQName, Map<String, Object> parameters) {
        return launch(flowQName, parameters, true);
    }
    
    HtmlPage launch(String flowQName, Map<String, Object> parameters, boolean ensureOKStatus) {
        
        //Generate an authn request and launch it in the htmlUnit browser        
        String url = authzRequestUrl(flowQName, parameters);        
        logger.info("Starting flow {}", flowQName);
        try {
            Page p = client.getPage(url);
    
            //Check it is an ok web page
            assertTrue(p.isHtmlPage(), "Not an html page");
            if (ensureOKStatus) {
                assertOK(p);
            }
            return (HtmlPage) p;

        } catch (IOException e) {
            fail(e.getMessage(), e);
            return null;
        }
        
    }
    
    void validateFinishPage(HtmlPage page, boolean success) {

        assertOK(page);
        //check we are effectively at the finish page
        String title = page.getTitleText().toLowerCase();
        
        if (success) {
            assertTextContained(title, true, "redirect");
            
            List<HtmlForm> forms = page.getForms();
            assertEquals(forms.size(), 1, "Page should have one and only one form");
            
            HtmlForm form = forms.get(0);
            assertTrue(form.getActionAttribute().contains("postlogin"), "Form does not have the expected action attribute");
            assertEquals(form.getMethodAttribute().toLowerCase(), "post", "Form does not use POST");

        } else {
            assertTextContained(title, true, "error");
            assertTextContained(page.getVisibleText().toLowerCase(), true, "authentication", "failed");
        }
        
    }
    
    void assertOK(Page page) {
        assertEquals(page.getWebResponse().getStatusCode(), WebResponse.OK);
    }
    
    void assertServerError(Page page) {
        assertEquals(page.getWebResponse().getStatusCode(), WebResponse.INTERNAL_SERVER_ERROR);
    }
    
    void assertTextContained(String text, String ...words) {
        assertTextContained(text, false, words);
    }
    
    void assertTextContained(String text, boolean includeMessageInAssert, String ...words) {
        
        List<String> list = Arrays.asList(words);
        if (includeMessageInAssert) {
            list.forEach(w -> assertTrue(text.contains(w), String.format("'%s' word not found in page text", w)));
        } else {
            list.forEach(w -> assertTrue(text.contains(w)));
        }
        
    }
    
    <P extends Page> P doClick(DomElement el) {
        
        try {
            return el.click();
        } catch (IOException e) {
            fail(e.getMessage(), e);
            return null;
        }

    }
    
    void typeInInputWithName(HtmlForm form, String name, String text) {
        
        try {
            //See f1/index.ftl
            form.getInputByName("something").type(text);
        } catch (IOException e) {
            fail(e.getMessage(), e);
        }

    }

}