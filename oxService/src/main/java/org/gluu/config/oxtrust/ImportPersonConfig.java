package org.gluu.config.oxtrust;

import java.io.Serializable;
import java.util.List;

import javax.enterprise.inject.Vetoed;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * oxTrust configuration
 *
 * @author shekhar laad
 * @date 12/10/2015
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Vetoed
public class ImportPersonConfig implements Configuration, Serializable {

    private static final long serialVersionUID = 2686538577505167695L;

    private List<ImportPerson> mappings;

    public List<ImportPerson> getMappings() {
        return mappings;
    }

    public void setMappings(List<ImportPerson> mappings) {
        this.mappings = mappings;
    }

}
