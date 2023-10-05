package io.jans.casa.plugins.branding;

import io.jans.casa.misc.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author jgomer
 */
public class CssSnippetHandler {

    private Logger logger = LoggerFactory.getLogger(getClass());

    /* Values of this constants are chosen with respect to file css-component-rules.properties */
    private static final String HEADER_SELECTOR = "cust-header";
    private static final String PRIMARY_BUTTON_SELECTOR = "cust-primary-button";
    private static final String SECONDARY_BUTTON_SELECTOR = "cust-cancel-button";
    private static final String TERTIARY_BUTTON_SELECTOR = "cust-misc-button";
    private static final String FOOTER_SELECTOR = "cust-footer-msg-rule";

    private static final String PRIMARY_BUTTON_DEF_COLOR = "#FF80CC"; /* pink */
    private static final String SECONDARY_BUTTON_DEF_COLOR = "#96CCFF"; /* light-blue */
    private static final String TERTIARY_BUTTON_DEF_COLOR = "#9EEBCF"; /* light-green */
    private static final String HEADER_DEF_COLOR = "#FBF1A9" /* light yellow */;

    private String headerColor;

    private String primaryButtonColor;

    private String secondaryButtonColor;

    private String tertiaryButtonColor;

    private String footerInnerHtml;

    CssSnippetHandler(String str, String defaultFooterHtml) {

        if (str != null) {
            headerColor = getMatchingString(HEADER_SELECTOR, "\\s*background-color\\s*:\\s*([^;]+)", str);
            primaryButtonColor = getMatchingString(PRIMARY_BUTTON_SELECTOR, "\\s*background-color\\s*:\\s*([^;]+)", str);
            secondaryButtonColor = getMatchingString(SECONDARY_BUTTON_SELECTOR, "\\s*background-color\\s*:\\s*([^;]+)", str);
            tertiaryButtonColor = getMatchingString(TERTIARY_BUTTON_SELECTOR, "\\s*background-color\\s*:\\s*([^;]+)", str);

            footerInnerHtml = getMatchingString(FOOTER_SELECTOR, "\\s*content\\s*:\\s*([^;]+)", str);
            if (footerInnerHtml != null) {
                footerInnerHtml = footerInnerHtml.substring(1, footerInnerHtml.length() - 1); //Drops surrounding single quotes
            }
        }
        footerInnerHtml = Optional.ofNullable(footerInnerHtml).orElse(defaultFooterHtml);
        assignMissingHeaderColors();

    }

    private String getMatchingString(String selector, String subregexp, String cssString) {

        String match = null;
        try {
            String regexp = "\\." + selector + "\\s*\\{([^\\}]+)\\}";

            Matcher m = Pattern.compile(regexp).matcher(cssString);
            if (m.find()) {
                match = m.group(1);
                if (Utils.isNotEmpty(match)) {
                    m = Pattern.compile(subregexp).matcher(match);
                    if (m.find()) {
                        match = m.group(1);
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return match;

    }

    private String toned(String color, boolean lighter) {

        String cl;
        if (lighter) {
            cl = Integer.toString(Math.min(255, Integer.parseInt(color, 16) + 16), 16);
        } else {
            cl = Integer.toString(Math.max(0, Integer.parseInt(color, 16) - 16), 16);
        }
        cl += "0";
        return cl.substring(0, 2);

    }

    private String getColorFrom(String hexaColor, boolean lighter) {

        try {
            String r = toned(hexaColor.substring(1, 3), lighter);
            String g = toned(hexaColor.substring(3, 5), lighter);
            String b = toned(hexaColor.substring(5, 7), lighter);
            hexaColor = String.format("#%s%s%s", r, g, b);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return hexaColor;

    }

    private String getSnippetForButton(String selector, String color) {

        //Button css generation is more involved: we need to set states and autogenerate lighter and darker colors
        String snip = "";

        //this way of building correlates tightly with parsing logic at class constructor

        //Note that border-color must always be the same as background-color (only 1 color picker is shown in the UI for button)
        snip += String.format(".%s{ background-color : %s; border-color: %s }\n", selector, color, color);

        String tone = getColorFrom(color, true);
        snip += String.format(".%s{ background-color : %s; border-color: %s }\n", selector + ":disabled", tone, tone);
        snip += String.format(".%s{ background-color : %s; border-color: %s }\n", selector + ":disabled:hover", tone, tone);

        tone = getColorFrom(color, false);
        snip += String.format(".%s{ background-color : %s; border-color: %s }\n", selector + ":hover", tone, tone);
        snip += String.format(".%s{ background-color : %s; border-color: %s }\n", selector + ":focus", tone, tone);

        tone = getColorFrom(tone, false);
        snip += String.format(".%s{ background-color : %s; border-color: %s }\n", selector + ":focus:active", tone, tone);

        return snip;

    }

    private void assignMissingHeaderColors() {
        if (headerColor == null) {
            headerColor = HEADER_DEF_COLOR;
        }
    }

    String getSnippet(boolean includeButtons) {

        String snip = "";
        //this way of building correlates tightly with parsing logic at class constructor
        snip += String.format(".%s{ background-color : %s; }\n", HEADER_SELECTOR, headerColor);

        //Note that content CSS property applies only to pseudoselectors ::before and ::after when using a string.
        //This rules is just a dummy one aimed at storing the content. Actual footer content is set via Javascript
        snip += String.format(".%s{ content: '%s'; }\n", FOOTER_SELECTOR, footerInnerHtml.replaceAll("\"", "\\\"").replaceAll("'", "\\\\'"));

        if (includeButtons) {
            snip += getSnippetForButton(PRIMARY_BUTTON_SELECTOR, primaryButtonColor);
            snip += getSnippetForButton(SECONDARY_BUTTON_SELECTOR, secondaryButtonColor);
            snip += getSnippetForButton(TERTIARY_BUTTON_SELECTOR, tertiaryButtonColor);
        }
        logger.debug("Generated CSS snippet is {}", snip);
        return snip;

    }

    void assignMissingButtonColors() {
        if (primaryButtonColor == null) {
            primaryButtonColor = PRIMARY_BUTTON_DEF_COLOR;
        }
        if (secondaryButtonColor == null) {
            secondaryButtonColor = SECONDARY_BUTTON_DEF_COLOR;
        }
        if (tertiaryButtonColor == null) {
            tertiaryButtonColor = TERTIARY_BUTTON_DEF_COLOR;
        }
    }

    public String getHeaderColor() {
        return headerColor;
    }

    public String getPrimaryButtonColor() {
        return primaryButtonColor;
    }

    public String getSecondaryButtonColor() {
        return secondaryButtonColor;
    }

    public String getTertiaryButtonColor() {
        return tertiaryButtonColor;
    }

    public String getFooterInnerHtml() {
        return footerInnerHtml;
    }

    public void setHeaderColor(String headerColor) {
        this.headerColor = headerColor;
    }

    public void setPrimaryButtonColor(String primaryButtonColor) {
        this.primaryButtonColor = primaryButtonColor;
    }

    public void setSecondaryButtonColor(String secondaryButtonColor) {
        this.secondaryButtonColor = secondaryButtonColor;
    }

    public void setTertiaryButtonColor(String tertiaryButtonColor) {
        this.tertiaryButtonColor = tertiaryButtonColor;
    }

    public void setFooterInnerHtml(String footerInnerHtml) {
        this.footerInnerHtml = footerInnerHtml;
    }

}
