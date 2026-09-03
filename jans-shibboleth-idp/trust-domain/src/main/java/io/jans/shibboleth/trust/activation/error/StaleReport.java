package io.jans.shibboleth.trust.activation.error;

public class StaleReport extends ActivationError {

    private StaleReport(String message) {

        super(message);
    }

    public static StaleReport instance() {

        return new StaleReport("The report does not name the TR's current live WorkItem");
    }
}
