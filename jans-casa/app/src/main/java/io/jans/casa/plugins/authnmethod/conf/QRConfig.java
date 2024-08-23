package io.jans.casa.plugins.authnmethod.conf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

import org.json.JSONObject;

public class QRConfig {

    static ObjectMapper MAPPER = new ObjectMapper();

    private String label;

    private int qrSize;
    private double qrMSize;

    public void populate(JSONObject properties) throws Exception {

        setLabel(properties.optString("label", null));
        setQrSize(properties.getInt("size"));
        setQrMSize(properties.getDouble("mSize"));

    }

    /**
     * Creates a string for a Javascript object representation of two values: size and mSize for QR code
     * @param maxWidth A parameter employed to limit the size of the QR
     * @return String
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

}
