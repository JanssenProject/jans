package org.xdi.oxauth.i18n;

import org.xdi.oxauth.service.AuthenticationService;
import org.xdi.oxauth.service.SessionStateService;

import javax.enterprise.context.ApplicationScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Named("language")
@ApplicationScoped
public class LanguageBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private SessionStateService sessionStateService;

    @Inject
    private AuthenticationService authenticationService;

    private String localeCode = "en";

    private static Map<String, Object> countries;

    static {
        countries = new LinkedHashMap<String, Object>();
        countries.put("Bulgarian", new Locale("bg"));
        countries.put("Germany", new Locale("de"));
        countries.put("English", Locale.ENGLISH); //label, value
        countries.put("Spanish", new Locale("es"));
        countries.put("French", Locale.FRENCH);
        countries.put("Italian", new Locale("it"));
        countries.put("Russian", new Locale("ru"));
        countries.put("Turkish", new Locale("tr"));
    }

    public Map<String, Object> getCountriesInMap() {
        return countries;
    }

    public String getLocaleCode() {
        if(FacesContext.getCurrentInstance().getViewRoot().getLocale().getLanguage() != localeCode)
            FacesContext.getCurrentInstance().getViewRoot().setLocale(new Locale(localeCode));
        return localeCode;
    }

    public void setLocaleCode(String localeCode) {
        this.localeCode = localeCode;
    }

    public void countryLocaleCodeChanged(ValueChangeEvent e) {
        String newLocaleValue = e.getNewValue().toString();
        for (Map.Entry<String, Object> entry : countries.entrySet()) {
            if (entry.getValue().toString().equals(newLocaleValue)) {
                FacesContext.getCurrentInstance().getViewRoot().setLocale((Locale) entry.getValue());
            }
        }
    }
}