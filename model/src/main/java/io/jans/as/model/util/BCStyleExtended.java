package io.jans.as.model.util;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.X500NameStyle;
import org.bouncycastle.asn1.x500.style.BCStyle;

public class BCStyleExtended extends BCStyle {

    public static final ASN1ObjectIdentifier JURISDICTION_COUNTRY_NAME = new ASN1ObjectIdentifier("1.3.6.1.4.1.311.60.2.1.3").intern();

    public static final X500NameStyle INSTANCE = new BCStyleExtended();

    protected BCStyleExtended() {
        defaultSymbols.put(JURISDICTION_COUNTRY_NAME, "jurisdictionCountryName");
        defaultLookUp.put("jurisdictioncountryname", JURISDICTION_COUNTRY_NAME);
    }

    public String oidToDisplayName(ASN1ObjectIdentifier oid) {
        return (String) defaultSymbols.get(oid);
    }
}
