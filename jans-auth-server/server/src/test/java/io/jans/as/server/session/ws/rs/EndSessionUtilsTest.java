package io.jans.as.server.session.ws.rs;

import com.google.common.collect.Sets;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.testng.Assert.*;

/**
 * @author Yuriy Z
 */
public class EndSessionUtilsTest {

    @Test
    public void createFrontChannelHtml_whenStateContainsQuote_shouldEscapeJsString() {
        String html = EndSessionUtils.createFrontChannelHtml(
                Collections.emptySet(),
                "https://rp.example/after",
                "'+alert(document.cookie)+'");

        assertFalse(html.contains("location='https://rp.example/after?state='+alert"),
                "raw single quote from state must not break out of JS string literal");
        assertTrue(html.contains("\\'+alert(document.cookie)+\\'"),
                "single quotes from state must be JS-escaped");
    }

    @Test
    public void createFrontChannelHtml_whenPostLogoutUrlContainsQuote_shouldEscapeJsString() {
        String html = EndSessionUtils.createFrontChannelHtml(
                Collections.emptySet(),
                "https://rp.example/after?x='+evil()+'",
                null);

        assertFalse(html.contains("location='https://rp.example/after?x='+evil()+''"),
                "single quote in postLogoutUrl must not break out of JS string literal");
        assertTrue(html.contains("\\'+evil()+\\'"), "single quote must be JS-escaped");
    }

    @Test
    public void createFrontChannelHtml_whenLogoutUriContainsQuote_shouldEscapeHtmlAttribute() {
        String html = EndSessionUtils.createFrontChannelHtml(
                Sets.newHashSet("https://rp.example/logout?x=\"><script>alert(1)</script>"),
                null,
                null);

        assertFalse(html.contains("\"><script>alert(1)</script>"),
                "raw double quote must not break out of src attribute");
        assertTrue(html.contains("&quot;&gt;&lt;script&gt;alert(1)&lt;/script&gt;"),
                "double quote and angle brackets must be HTML-escaped");
    }

    @Test
    public void createFrontChannelHtml_withBenignInputs_shouldRenderRedirect() {
        String html = EndSessionUtils.createFrontChannelHtml(
                Sets.newHashSet("https://rp.example/logout"),
                "https://rp.example/after",
                "abc123");

        assertTrue(html.contains("src=\"https://rp.example/logout\""), "HTML escape must not alter benign URL");
        assertTrue(html.contains("?state=abc123"), "state must be appended to postLogoutUrl");
        assertTrue(html.contains("rp.example") && html.contains("after"), "redirect target must be present");
    }

    @Test
    public void createFrontChannelHtml_whenPostLogoutUrlContainsScriptClose_shouldPreventTagClosure() {
        String html = EndSessionUtils.createFrontChannelHtml(
                Collections.emptySet(),
                "https://rp.example/after?x=</script><script>alert(1)//",
                null);

        assertFalse(html.contains("</script><script>alert(1)"),
                "raw </script> sequence in postLogoutUrl must not be able to close the embedded <script> block");
        assertTrue(html.contains("<\\/script><script>alert(1)\\/\\/"),
                "forward slashes in </script> must be JS-escaped so the HTML parser does not see a script close");
    }

    @Test
    public void appendState_whenStateAlreadyPresent_shouldNotDuplicate() {
        String out = EndSessionUtils.appendState("https://rp.example/after?state=foo", "bar");
        assertEquals(out, "https://rp.example/after?state=foo");
    }
}
