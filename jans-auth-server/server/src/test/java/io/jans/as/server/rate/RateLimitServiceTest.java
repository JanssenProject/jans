package io.jans.as.server.rate;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.configuration.rate.KeyExtractor;
import io.jans.as.model.configuration.rate.KeySource;
import io.jans.as.model.configuration.rate.RateLimitConfig;
import io.jans.as.model.configuration.rate.RateLimitRule;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.util.ServerUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class RateLimitServiceTest {

    @InjectMocks
    @Spy
    private RateLimitService rateLimitService;

    @Mock
    private Logger log;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Test
    public void validateRateLimit_forSingleCall_shouldPassSuccessfully() throws RateLimitedException, IOException {

        String requestBody = "{ \"software_statement\":\"dummy_ssa\", " +
                "\"redirect_uris\": [\n" +
                "      \"https://client.example.com/callback\",\n" +
                "      \"https://client.example.com/callback2\"\n" +
                "    ],}";

        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        when(httpServletRequest.getRequestURI()).thenReturn("/jans-auth/restv1/register");
        when(httpServletRequest.getMethod()).thenReturn("POST");
        when(httpServletRequest.getInputStream()).thenReturn(new TestServletInputStream(requestBody));
        when(httpServletRequest.getContentType()).thenReturn("application/json;charset=UTF-8");

        KeyExtractor keyExtractor = new KeyExtractor();
        keyExtractor.setSource(KeySource.BODY);
        keyExtractor.setParameterNames(Arrays.asList("software_statement", "redirect_uris"));

        RateLimitRule rule = new RateLimitRule();
        rule.setPath("/jans-auth/restv1/register");
        rule.setMethods(List.of("POST"));
        rule.setRequestCount(10);
        rule.setPeriodInSeconds(60);
        rule.setKeyExtractors(List.of(keyExtractor));

        RateLimitConfig rateLimitConfiguration = new RateLimitConfig();
        rateLimitConfiguration.setRateLimitRules(List.of(rule));
        rateLimitConfiguration.setRateLoggingEnabled(true);

        when(errorResponseFactory.isFeatureFlagEnabled(FeatureFlagType.RATE_LIMIT)).thenReturn(true);
        when(appConfiguration.getRateLimitConfiguration()).thenReturn(rateLimitConfiguration);

        rateLimitService.updateConfiguration(appConfiguration);
        rateLimitService.validateRateLimit(httpServletRequest);
    }

    @Test
    public void validateRateLimit_forManyCallOverRateLimit_shouldFail() throws IOException {
        try {
            String requestBody = "{ \"software_statement\":\"dummy_ssa\", " +
                    "\"redirect_uris\": [\n" +
                    "      \"https://client.example.com/callback\",\n" +
                    "      \"https://client.example.com/callback2\"\n" +
                    "    ],}";

            KeyExtractor bodyExtractor = new KeyExtractor();
            bodyExtractor.setSource(KeySource.BODY);
            bodyExtractor.setParameterNames(Arrays.asList("software_statement", "redirect_uris"));

            KeyExtractor headerExtractor = new KeyExtractor();
            headerExtractor.setSource(KeySource.HEADER);
            headerExtractor.setParameterNames(List.of("X-ClientCert"));

            RateLimitRule rule = new RateLimitRule();
            rule.setPath("/jans-auth/restv1/register");
            rule.setMethods(List.of("POST"));
            rule.setRequestCount(3);
            rule.setPeriodInSeconds(40);
            rule.setKeyExtractors(List.of(bodyExtractor, headerExtractor));

            RateLimitConfig rateLimitConfiguration = new RateLimitConfig();
            rateLimitConfiguration.setRateLimitRules(List.of(rule));
            rateLimitConfiguration.setRateLoggingEnabled(true);

            when(errorResponseFactory.isFeatureFlagEnabled(FeatureFlagType.RATE_LIMIT)).thenReturn(true);
            when(appConfiguration.getRateLimitConfiguration()).thenReturn(rateLimitConfiguration);

            rateLimitService.updateConfiguration(appConfiguration);

            for (int i = 0; i < 10; i++) {
                System.out.println("validateRateLimit - " + i);

                HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
                when(httpServletRequest.getRequestURI()).thenReturn("/jans-auth/restv1/register");
                when(httpServletRequest.getMethod()).thenReturn("POST");
                when(httpServletRequest.getInputStream()).thenReturn(new TestServletInputStream(requestBody));
                when(httpServletRequest.getContentType()).thenReturn("application/json;charset=UTF-8");
                when(httpServletRequest.getHeader("X-ClientCert")).thenReturn("test_cert");

                rateLimitService.validateRateLimit(httpServletRequest);
            }
        } catch (RateLimitedException e) {
            System.out.println("validateRateLimitForRegister - rate limit exception. Passed successfully.");
            return;
        }

        Assert.fail("Rate limit exception was not thrown. But it's expected to get it.");
    }

    @Test
    public void validateRateLimit_invalidEndpoint_shouldNotApplyLimits() throws RateLimitedException, IOException {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getRequestURI()).thenReturn("/invalid/endpoint");
        when(mockRequest.getMethod()).thenReturn("POST");

        RateLimitRule rule = new RateLimitRule();
        rule.setPath("/jans-auth/restv1/register");
        rule.setMethods(List.of("POST"));
        rule.setRequestCount(5);
        rule.setPeriodInSeconds(10);

        RateLimitConfig config = new RateLimitConfig();
        config.setRateLimitRules(List.of(rule));

        when(errorResponseFactory.isFeatureFlagEnabled(FeatureFlagType.RATE_LIMIT)).thenReturn(true);
        when(appConfiguration.getRateLimitConfiguration()).thenReturn(config);

        rateLimitService.updateConfiguration(appConfiguration);

        rateLimitService.validateRateLimit(mockRequest); // Should not throw; no limits for "/invalid/endpoint".
    }

    @Test
    public void updateConfiguration_emptyConfiguration_shouldApplyDefaults() {
        RateLimitConfig emptyConfig = null;
        when(appConfiguration.getRateLimitConfiguration()).thenReturn(emptyConfig);

        rateLimitService.updateConfiguration(appConfiguration); // Should not throw; system should handle null/empty gracefully.
    }

    @Test
    public void validateRateLimit_whenFeatureFlagDisabled_shouldAllowAllRequests() throws Exception {
        // Mock request
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);

        // Mock feature flag
        when(errorResponseFactory.isFeatureFlagEnabled(FeatureFlagType.RATE_LIMIT)).thenReturn(false);

        // Validate without triggering rate limiting
        rateLimitService.validateRateLimit(mockRequest); // Should pass unconditionally
        rateLimitService.validateRateLimit(mockRequest); // Should pass unconditionally
    }

    @Test
    public void rateLimitConfiguration_deserialization_shouldPassSuccessfully() throws JsonProcessingException {
        String rateConfiguration = "{\n" +
                "  \"rateLimitConfiguration\": {\n" +
                "    \"rateLimitRules\": [\n" +
                "      {\n" +
                "        \"path\": \"/jans-auth/restv1/token\",\n" +
                "        \"methods\": [\n" +
                "          \"POST\"\n" +
                "        ],\n" +
                "        \"requestCount\": 10,\n" +
                "        \"periodInSeconds\": 60,\n" +
                "        \"keyExtractors\": [\n" +
                "          {\n" +
                "            \"source\": \"body\",\n" +
                "            \"parameterNames\": [\n" +
                "              \"client_id\"\n" +
                "            ]\n" +
                "          },\n" +
                "          {\n" +
                "            \"source\": \"header\",\n" +
                "            \"parameterNames\": [\n" +
                "              \"X-Real-IP\"\n" +
                "            ]\n" +
                "          }\n" +
                "        ]\n" +
                "      },\n" +
                "      {\n" +
                "        \"path\": \"/jans-auth/restv1/token\",\n" +
                "        \"methods\": [\n" +
                "          \"POST\"\n" +
                "        ],\n" +
                "        \"requestCount\": 10,\n" +
                "        \"periodInSeconds\": 60,\n" +
                "        \"keyExtractors\": [\n" +
                "          {\n" +
                "            \"source\": \"header\",\n" +
                "            \"parameterNames\": [\n" +
                "              \"X-Real-IP\"\n" +
                "            ]\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";

        RateLimitConfig rateLimitConfiguration = ServerUtil.createJsonMapper().readValue(rateConfiguration, AppConfiguration.class).getRateLimitConfiguration();
        assertNotNull(rateLimitConfiguration);
    }

    @Test
    public void buildKey_whenValidContextAndRule_shouldBuildKeySuccessfully() throws IOException {
        String requestBody = "{ \"software_statement\":\"dummy_ssa\", " +
                "\"redirect_uris\": [\n" +
                "      \"https://client.example.com/callback\",\n" +
                "      \"https://client.example.com/callback2\"\n" +
                "    ]}";

        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        when(httpServletRequest.getRequestURI()).thenReturn("/jans-auth/restv1/register");
        when(httpServletRequest.getInputStream()).thenReturn(new TestServletInputStream(requestBody));
        when(httpServletRequest.getContentType()).thenReturn("application/json;charset=UTF-8");
        when(httpServletRequest.getHeader("X-ClientCert")).thenReturn("test_cert");

        // Mock a valid RateLimitContext
        RateLimitContext context = new RateLimitContext(httpServletRequest, true);

        // Create a valid key extractor and rule
        KeyExtractor bodyExtractor = new KeyExtractor();
        bodyExtractor.setSource(KeySource.BODY);
        bodyExtractor.setParameterNames(List.of("software_statement", "redirect_uris"));

        KeyExtractor headerExtractor = new KeyExtractor();
        headerExtractor.setSource(KeySource.HEADER);
        headerExtractor.setParameterNames(List.of("X-ClientCert"));

        RateLimitRule rule = new RateLimitRule();
        rule.setKeyExtractors(List.of(bodyExtractor, headerExtractor));
        rule.setPath("/jans-auth/restv1/register");
        rule.setMethods(List.of("POST"));

        String key = rateLimitService.buildKey(context, rule);

        assertEquals("/jans-auth/restv1/register_[dummy_ssa]_[https://client.example.com/callback, https://client.example.com/callback2]__test_cert__", key);
    }

    @Test
    public void extractKey_whenExtractingBody_shouldReturnCorrectKey() throws IOException {
        String requestBody = "{ \"software_statement\":\"dummy_ssa\", " +
                "\"redirect_uris\": [\n" +
                "      \"https://client.example.com/callback\",\n" +
                "      \"https://client.example.com/callback2\"\n" +
                "    ]}";

        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        when(httpServletRequest.getInputStream()).thenReturn(new TestServletInputStream(requestBody));
        when(httpServletRequest.getContentType()).thenReturn("application/json;charset=UTF-8");

        RateLimitContext context = new RateLimitContext(httpServletRequest, true);

        // Body extractor
        KeyExtractor bodyExtractor = new KeyExtractor();
        bodyExtractor.setSource(KeySource.BODY);
        bodyExtractor.setParameterNames(List.of("software_statement"));

        String extractedKey = rateLimitService.extractKey(bodyExtractor, context);

        assertEquals("[dummy_ssa]_", extractedKey);
    }

    @Test
    public void extractKey_whenExtractingHeader_shouldReturnCorrectKey() throws IOException {
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        when(httpServletRequest.getHeader("X-ClientCert")).thenReturn("test_cert");

        RateLimitContext context = new RateLimitContext(httpServletRequest, true);

        // Body extractor
        KeyExtractor bodyExtractor = new KeyExtractor();
        bodyExtractor.setSource(KeySource.HEADER);
        bodyExtractor.setParameterNames(List.of("X-ClientCert"));

        String extractedKey = rateLimitService.extractKey(bodyExtractor, context);

        assertEquals("test_cert_", extractedKey);
    }

    @Test
    public void matchRulesByPathAndMethod_whenValidInputs_shouldReturnMatchingRules() {
        RateLimitRule rule1 = new RateLimitRule();
        rule1.setPath("/jans-auth/restv1/register");
        rule1.setMethods(List.of("POST"));
        rule1.setPeriodInSeconds(60);
        rule1.setRequestCount(10);
        rule1.setKeyExtractors(List.of(new KeyExtractor()));
        AssertJUnit.assertTrue(rule1.isWellFormed());

        RateLimitRule rule2 = new RateLimitRule();
        rule2.setPath("/jans-auth/restv1/token");
        rule2.setMethods(List.of("POST"));
        rule2.setPeriodInSeconds(60);
        rule2.setRequestCount(10);
        rule2.setKeyExtractors(List.of(new KeyExtractor()));
        AssertJUnit.assertTrue(rule2.isWellFormed());

        RateLimitConfig config = new RateLimitConfig();
        config.setRateLimitRules(List.of(rule1, rule2));
        when(appConfiguration.getRateLimitConfiguration()).thenReturn(config);

        rateLimitService.updateConfiguration(appConfiguration);

        // Match rules by path and method
        List<RateLimitRule> matchedRules = rateLimitService.matchRulesByPathAndMethod("/jans-auth/restv1/register", "POST");

        // Validate matched rules
        assertEquals(1, matchedRules.size());
        assertEquals("/jans-auth/restv1/register", matchedRules.get(0).getPath());
    }

    @Test
    public void matchRulesByPathAndMethod_whenNoMatchingRules_shouldReturnEmptyList() {
        RateLimitRule rule1 = new RateLimitRule();
        rule1.setPath("/jans-auth/restv1/register");
        rule1.setMethods(List.of("POST"));
        rule1.setPeriodInSeconds(60);
        rule1.setRequestCount(10);
        rule1.setKeyExtractors(List.of(new KeyExtractor()));
        AssertJUnit.assertTrue(rule1.isWellFormed());

        RateLimitConfig config = new RateLimitConfig();
        config.setRateLimitRules(List.of(rule1));
        when(appConfiguration.getRateLimitConfiguration()).thenReturn(config);

        rateLimitService.updateConfiguration(appConfiguration);

        // Try matching rules for unmatched path
        List<RateLimitRule> matchedRules = rateLimitService.matchRulesByPathAndMethod("/invalid/path", "GET");

        // Validate matched rules
        assertTrue(matchedRules.isEmpty(), "No rules should match for invalid path/method.");
    }

    @Test
    public void saveSpaceIfNeeded_whenKeyIsLong_shouldReturnHashedKey() {
        String longKey = StringUtils.repeat("a", RateLimitService.KEY_LENGTH_LIMIT_FOR_DIGEST + 1);
        String hashedKey = RateLimitService.saveSpaceIfNeeded(longKey);
        assertEquals(hashedKey, DigestUtils.sha256Hex(longKey));
    }

    @Test
    public void saveSpaceIfNeeded_whenKeyIsShort_shouldReturnSameKey() {
        String shortKey = StringUtils.repeat("a", RateLimitService.KEY_LENGTH_LIMIT_FOR_DIGEST - 1);
        String resultKey = RateLimitService.saveSpaceIfNeeded(shortKey);
        assertEquals(resultKey, shortKey);
    }
}
