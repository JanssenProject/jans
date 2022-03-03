package io.jans.as.client.client.testcasebuilders;

import io.jans.as.client.BaseTest;

public abstract class BaseTestCase {

    private String title;

    public BaseTestCase(String title) {
        this.title = title;
    }

    protected abstract Object excuteTestCase();
}
