/**
 * 
 */
package io.jans.model.metric.audit;

import java.util.Date;

import io.jans.model.metric.ldap.MetricEntry;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.JsonObject;

/**
 * @author Sergey Manoylo
 * @version July 9, 2023
 */
public class AuditMetricEntry extends MetricEntry {
	
    @JsonObject
    @AttributeName(name = "jansData")
    private AuditMetricData metricData;	

    /**
     * 
     */
    public AuditMetricEntry() {
    }

    /**
     * 
     * @param dn
     * @param id
     * @param creationDate
     * @param metricData
     */
    public AuditMetricEntry(String dn, String id, Date creationDate, AuditMetricData metricData) {
        super(dn, id, creationDate);
        this.metricData = metricData;
    }

    /**
     * 
     * @return
     */
    public AuditMetricData getMetricData() {
        return metricData;
    }

    /**
     * 
     * @param metricData
     */
    public void setMetricData(AuditMetricData metricData) {
        this.metricData = metricData;
    }

    /**
     * 
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("AuditMetricData [metricData=").append(metricData).append(", toString()=").append(super.toString()).append("]");
        return builder.toString();
    }
}
