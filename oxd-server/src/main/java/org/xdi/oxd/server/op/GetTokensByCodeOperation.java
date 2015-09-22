package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.params.GetTokensByCodeParams;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/09/2015
 */

public class GetTokensByCodeOperation extends BaseOperation {

    private static final Logger LOG = LoggerFactory.getLogger(GetTokensByCodeOperation.class);

       /**
        * Base constructor
        *
        * @param p_command command
        */
       protected GetTokensByCodeOperation(Command p_command, final Injector injector) {
           super(p_command, injector);
       }

       @Override
       public CommandResponse execute() {
           try {
               final GetTokensByCodeParams params = asParams(GetTokensByCodeParams.class);
               // todo we need load site conf here
           } catch (Exception e) {
               LOG.error(e.getMessage(), e);
           }
           return CommandResponse.INTERNAL_ERROR_RESPONSE;
       }
}
