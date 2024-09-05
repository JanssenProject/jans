package io.jans.as.client.client.assertbuilders;

import io.jans.as.client.ssa.validate.SsaValidateResponse;
import io.jans.as.model.error.IErrorType;
import io.jans.as.model.ssa.SsaErrorResponseType;
import org.apache.http.HttpStatus;

import java.util.Arrays;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class SsaValidationAssertBuilder extends BaseAssertBuilder {

    private final SsaValidateResponse response;
    private int status = HttpStatus.SC_OK;

    private IErrorType errorType;

    public SsaValidationAssertBuilder(SsaValidateResponse response) {
        this.response = response;
    }

    public SsaValidationAssertBuilder status(int status) {
        this.status = status;
        return this;
    }

    public SsaValidationAssertBuilder errorType(SsaErrorResponseType errorType) {
        this.errorType = errorType;
        return this;
    }

    @Override
    public void check() {
        assertNotNull(response, "SsaValidateResponse is null");
        assertEquals(response.getStatus(), status, "Unexpected response code: " + response.getStatus());
        if (Arrays.asList(HttpStatus.SC_OK, HttpStatus.SC_CREATED).contains(status)) {
            assertNotNull(response.getEntity(), "The entity is null");
        } else {
            assertEquals(response.getStatus(), status, "Unexpected HTTP status response: " + response.getEntity());
            assertNotNull(response.getEntity(), "The entity is null");
            if (errorType != null) {
                assertNotNull(response.getErrorType());
                assertEquals(response.getErrorType(), errorType, "Unexpected ErrorType response: " + response.getErrorType());
            }
            assertNotNull(response.getErrorDescription());
        }
    }
}
