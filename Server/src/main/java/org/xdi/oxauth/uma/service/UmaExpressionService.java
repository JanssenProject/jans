package org.xdi.oxauth.uma.service;

import com.ocpsoft.pretty.faces.util.StringUtils;
import org.slf4j.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author yuriyz
 */
@Stateless
@Named
public class UmaExpressionService {

    @Inject
    private Logger log;

    public boolean isExpressionValid(String expression) {
        if (StringUtils.isNotBlank(expression)) {
            return true;
        }
        return false;
    }
}
