/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */
package io.jans.as.server.model.authorize;

import io.jans.as.model.common.Prompt;
import io.jans.as.model.common.ResponseMode;
import io.jans.as.model.common.ResponseType;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Unit tests for AuthorizeParamsValidator.
 *
 * @author Yuriy Z
 */
public class AuthorizeParamsValidatorTest {

    @Test
    public void validateParamsWithReason_whenResponseTypeIsEmpty_shouldReturnReason() {
        List<ResponseType> responseTypes = Collections.emptyList();
        List<Prompt> prompts = Collections.emptyList();

        String reason = AuthorizeParamsValidator.validateParamsWithReason(responseTypes, prompts, "nonce123", false, ResponseMode.QUERY);

        assertNotNull(reason);
        assertEquals(reason, "response_type is required");
    }

    @Test
    public void validateParamsWithReason_whenResponseTypeCodeAndNoNonce_shouldReturnNull() {
        List<ResponseType> responseTypes = Collections.singletonList(ResponseType.CODE);
        List<Prompt> prompts = Collections.emptyList();

        String reason = AuthorizeParamsValidator.validateParamsWithReason(responseTypes, prompts, null, false, ResponseMode.QUERY);

        assertNull(reason); // code alone doesn't require nonce
    }

    @Test
    public void validateParamsWithReason_whenResponseTypeIdTokenAndNoNonce_shouldReturnReason() {
        List<ResponseType> responseTypes = Collections.singletonList(ResponseType.ID_TOKEN);
        List<Prompt> prompts = Collections.emptyList();

        String reason = AuthorizeParamsValidator.validateParamsWithReason(responseTypes, prompts, null, false, ResponseMode.FRAGMENT);

        assertNotNull(reason);
        assertEquals(reason, "nonce is required for response_type=id_token");
    }

    @Test
    public void validateParamsWithReason_whenResponseTypeIdTokenWithNonce_shouldReturnNull() {
        List<ResponseType> responseTypes = Collections.singletonList(ResponseType.ID_TOKEN);
        List<Prompt> prompts = Collections.emptyList();

        String reason = AuthorizeParamsValidator.validateParamsWithReason(responseTypes, prompts, "nonce123", false, ResponseMode.FRAGMENT);

        assertNull(reason);
    }

    @Test
    public void validateParamsWithReason_whenResponseTypeTokenAndNoNonce_shouldReturnReason() {
        List<ResponseType> responseTypes = Collections.singletonList(ResponseType.TOKEN);
        List<Prompt> prompts = Collections.emptyList();

        String reason = AuthorizeParamsValidator.validateParamsWithReason(responseTypes, prompts, null, false, ResponseMode.FRAGMENT);

        assertNotNull(reason);
        assertEquals(reason, "nonce is required for response_type=token");
    }

    @Test
    public void validateParamsWithReason_whenResponseTypeCodeIdTokenAndNoNonce_shouldReturnReason() {
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN);
        List<Prompt> prompts = Collections.emptyList();

        String reason = AuthorizeParamsValidator.validateParamsWithReason(responseTypes, prompts, null, false, ResponseMode.FRAGMENT);

        assertNotNull(reason);
        assertEquals(reason, "nonce is required for response_type=code id_token");
    }

    @Test
    public void validateParamsWithReason_whenResponseTypeIdTokenTokenAndNoNonce_shouldReturnReason() {
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.ID_TOKEN, ResponseType.TOKEN);
        List<Prompt> prompts = Collections.emptyList();

        String reason = AuthorizeParamsValidator.validateParamsWithReason(responseTypes, prompts, null, false, ResponseMode.FRAGMENT);

        assertNotNull(reason);
        assertEquals(reason, "nonce is required for response_type=id_token token");
    }

    @Test
    public void validateParamsWithReason_whenPromptNoneWithOtherPrompts_shouldReturnReason() {
        List<ResponseType> responseTypes = Collections.singletonList(ResponseType.CODE);
        List<Prompt> prompts = Arrays.asList(Prompt.NONE, Prompt.LOGIN);

        String reason = AuthorizeParamsValidator.validateParamsWithReason(responseTypes, prompts, "nonce123", false, ResponseMode.QUERY);

        assertNotNull(reason);
        assertEquals(reason, "prompt=none cannot be combined with other prompt values");
    }

    @Test
    public void validateParamsWithReason_whenPromptNoneAlone_shouldReturnNull() {
        List<ResponseType> responseTypes = Collections.singletonList(ResponseType.CODE);
        List<Prompt> prompts = Collections.singletonList(Prompt.NONE);

        String reason = AuthorizeParamsValidator.validateParamsWithReason(responseTypes, prompts, "nonce123", false, ResponseMode.QUERY);

        assertNull(reason);
    }

    @Test
    public void validateParamsWithReason_whenFapiAndResponseModeQuery_shouldReturnReason() {
        // Use response_type=code id_token so the first FAPI check (response_type=code + response_mode!=jwt) doesn't trigger
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN);
        List<Prompt> prompts = Collections.emptyList();

        String reason = AuthorizeParamsValidator.validateParamsWithReason(responseTypes, prompts, "nonce123", true, ResponseMode.QUERY);

        assertNotNull(reason);
        assertEquals(reason, "response_mode=query is not allowed for FAPI");
    }

    @Test
    public void validateParamsWithReason_whenFapiAndResponseTypeCodeWithoutJwtMode_shouldReturnReason() {
        List<ResponseType> responseTypes = Collections.singletonList(ResponseType.CODE);
        List<Prompt> prompts = Collections.emptyList();

        String reason = AuthorizeParamsValidator.validateParamsWithReason(responseTypes, prompts, "nonce123", true, ResponseMode.FRAGMENT);

        assertNotNull(reason);
        assertEquals(reason, "FAPI requires response_mode=jwt when response_type=code");
    }

    @Test
    public void validateParamsWithReason_whenFapiAndResponseTypeCodeWithJwtMode_shouldReturnNull() {
        List<ResponseType> responseTypes = Collections.singletonList(ResponseType.CODE);
        List<Prompt> prompts = Collections.emptyList();

        String reason = AuthorizeParamsValidator.validateParamsWithReason(responseTypes, prompts, "nonce123", true, ResponseMode.JWT);

        assertNull(reason);
    }

    @Test
    public void validateParamsWithReason_whenFapiAndQueryModeAndEmptyResponseType_shouldReturnResponseTypeRequiredReason() {
        List<ResponseType> responseTypes = Collections.emptyList();
        List<Prompt> prompts = Collections.emptyList();

        String reason = AuthorizeParamsValidator.validateParamsWithReason(responseTypes, prompts, "nonce123", true, ResponseMode.QUERY);

        assertNotNull(reason);
        assertEquals(reason, "response_type is required");
    }

    @Test
    public void validateParamsWithReason_whenAllValid_shouldReturnNull() {
        List<ResponseType> responseTypes = Collections.singletonList(ResponseType.CODE);
        List<Prompt> prompts = Collections.singletonList(Prompt.LOGIN);

        String reason = AuthorizeParamsValidator.validateParamsWithReason(responseTypes, prompts, "nonce123", false, ResponseMode.QUERY);

        assertNull(reason);
    }
}
