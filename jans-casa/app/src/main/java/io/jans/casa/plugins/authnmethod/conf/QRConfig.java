package io.jans.casa.plugins.authnmethod.conf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author jgomer
 */
public class QRConfig {

    static ObjectMapper MAPPER = new ObjectMapper();

    private String label;
    private String registrationUri;

    private int qrSize;
    private double qrMSize;

    public void populate(Map<String, String> properties) throws Exception {

        setLabel(properties.get("label"));
        setRegistrationUri(properties.get("registration_uri"));

        String value = properties.get("qr_options");
        //value may come not properly formated (without quotes, for instance...)
        if (!value.contains("\"")) {
            value = value.replaceFirst("mSize", "\"mSize\"").replaceFirst("size", "\"size\"");
        }

        JsonNode tree = MAPPER.readTree(value);

        if (tree.get("size") != null) {
            setQrSize(tree.get("size").asInt());
        }

        if (tree.get("mSize") != null) {
            setQrMSize(tree.get("mSize").asDouble());
        }

    }

    /**
     * Creates a string for a Json representation of two values: size and mSize for QR code
     * @param maxWidth A parameter employed to limit the size of the QR
     * @return Json String
     */
    public String getFormattedQROptions(int maxWidth) {

        List<String> list = new ArrayList<>();
        int size = getQrSize();
        int ival = maxWidth > 0 ? Math.min(size, maxWidth - 30) : size;

        if (ival > 0) {
            list.add("size:" + ival);
        }

        double dval = getQrMSize();
        if (dval > 0) {
            list.add("mSize: " + dval);
        }

        return list.toString().replaceFirst("\\[", "{").replaceFirst("\\]", "}");

    }

    public String getFormattedQROptions() {
        return getFormattedQROptions(0);
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getQrSize() {
        return qrSize;
    }

    public void setQrSize(int qrSize) {
        this.qrSize = qrSize;
    }

    public double getQrMSize() {
        return qrMSize;
    }

    public void setQrMSize(double qrMSize) {
        this.qrMSize = qrMSize;
    }

    public String getRegistrationUri() {
        return registrationUri;
    }

    public void setRegistrationUri(String registrationUri) {
        this.registrationUri = registrationUri;
    }

}
