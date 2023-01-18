package io.jans.as.client.util;

import io.jans.as.model.crypto.AbstractCryptoProvider;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * @author Yuriy Z
 */
public class KeyGeneratorContext {

    private TestPropFile testPropFile;
    private AbstractCryptoProvider cryptoProvider;
    private int keyLength;

    private int expirationDays;
    private int expirationHours;
    private Calendar expiration;

    public Calendar calculateExpiration() {
        Calendar calendar = new GregorianCalendar();
        calendar.add(Calendar.DATE, getExpirationDays());
        calendar.add(Calendar.HOUR, getExpirationHours());
        this.expiration = calendar;
        return calendar;
    }

    public Calendar getExpiration() {
        return expiration;
    }

    public void setExpiration(Calendar expiration) {
        this.expiration = expiration;
    }

    public int getKeyLength() {
        return keyLength;
    }

    public void setKeyLength(int keyLength) {
        this.keyLength = keyLength;
    }

    public TestPropFile getTestPropFile() {
        return testPropFile;
    }

    public void setTestPropFile(TestPropFile testPropFile) {
        this.testPropFile = testPropFile;
    }

    public AbstractCryptoProvider getCryptoProvider() {
        return cryptoProvider;
    }

    public void setCryptoProvider(AbstractCryptoProvider cryptoProvider) {
        this.cryptoProvider = cryptoProvider;
    }

    public int getExpirationDays() {
        return expirationDays;
    }

    public void setExpirationDays(int expirationDays) {
        this.expirationDays = expirationDays;
    }

    public int getExpirationHours() {
        return expirationHours;
    }

    public void setExpirationHours(int expirationHours) {
        this.expirationHours = expirationHours;
    }
}
