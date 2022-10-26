package io.jans.as.model.util;

import io.jans.as.model.BaseTest;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.*;
import static org.testng.Assert.assertTrue;

public class URLPatternListTest extends BaseTest {

    @Test
    public void isUrlListed_listStringMatchPattern_true() {
        showTitle("isUrlListed_listStringMatchPattern_true");
        List<String> urlPatterns = Arrays.asList(
                "*.webpage.org/foo*bar",
                "https://example.org/foo/bar.html",
                "*.domain.com/*");

        URLPatternList urlPatternList = new URLPatternList(urlPatterns);
        assertNotNull(urlPatternList);

        assertFalse(urlPatternList.isUrlListed("webpage.org"));
        assertFalse(urlPatternList.isUrlListed("www.webpage.org"));
        assertTrue(urlPatternList.isUrlListed("http://webpage.org/foo/bar"));
        assertTrue(urlPatternList.isUrlListed("https://mail.webpage.org/foo/bar"));
        assertTrue(urlPatternList.isUrlListed("http://www.webpage.org/foobar"));
        assertTrue(urlPatternList.isUrlListed("https://www.webpage.org/foo/baz/bar"));
        assertFalse(urlPatternList.isUrlListed("http://example.org"));
        assertFalse(urlPatternList.isUrlListed("http://example.org/foo/bar.html"));
        assertTrue(urlPatternList.isUrlListed("https://example.org/foo/bar.html"));
        assertTrue(urlPatternList.isUrlListed("http://domain.com"));
        assertTrue(urlPatternList.isUrlListed("https://www.domain.com"));
        assertTrue(urlPatternList.isUrlListed("https://www.domain.com/foo/bar"));
    }

}
