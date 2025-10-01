package io.jans.casa.ui.vm;

import java.util.Comparator;
import java.util.Optional;
import java.util.Locale;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import io.jans.casa.core.ZKService;
import io.jans.casa.misc.Utils;
import io.jans.casa.misc.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.annotation.*;
import org.zkoss.web.Attributes;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;

public class FooterViewModel {

    private static SortedSet<Locale> locales;

    private Logger logger = LoggerFactory.getLogger(getClass());
    private Locale selectedLocale;

    @Init
    public void init(@ContextParam(ContextType.SESSION) Session session) {

        try {
            //this check prevents recomputing the list to be displayed upon every page load
            if (locales == null) {
                Set<Locale> tmp = Utils.managedBean(ZKService.class).getSupportedLocales();
                if (tmp != null) {
                    //Use a comparator based on locales' display name
                    locales = new TreeSet<>(Comparator.comparing(locale -> locale.getDisplayName(locale).toLowerCase()));
                    locales.addAll(tmp);
                }
            }

            selectedLocale = Optional.ofNullable(session.getAttribute(Attributes.PREFERRED_LOCALE))
                    .map(Locale.class::cast).orElse(WebUtils.DEFAULT_LOCALE);
            //It always holds selectedLocale is contained in supported locales (See io.jans.casa.core.filter.LocaleInterceptor#request)
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

    public void localeChanged(Locale locale) {
        selectedLocale = locale;
        WebUtils.getServletRequest().getSession().setAttribute(Attributes.PREFERRED_LOCALE, selectedLocale);
        Executions.sendRedirect(null); // reload the same page
    }

    public Locale getSelectedLocale() {
        return selectedLocale;
    }

    public Set<Locale> getLocales() {
        return locales;
    }

}
