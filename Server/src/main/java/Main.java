import org.xdi.oxauth.client.RegisterClient;
import org.xdi.oxauth.client.RegisterRequest;
import org.xdi.oxauth.client.RegisterResponse;
import org.xdi.oxauth.model.register.ApplicationType;
import org.xdi.oxauth.model.util.StringUtils;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by eugeniuparvan on 1/3/17.
 */
public class Main {

    public static void main(String[] args) {
        for (int i = 0; i < 1; ++i) {
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app" + new Date().getTime(),
                    StringUtils.spaceSeparatedToList("https://gluu.localhost.info"));

            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_MONTH, 100);
            registerRequest.setClientSecretExpiresAt(c.getTime());

            RegisterClient registerClient = new RegisterClient("https://gluu.localhost.info/oxauth/seam/resource/restv1/oxauth/register");
            registerClient.setRequest(registerRequest);
            RegisterResponse response = registerClient.exec();
            int status = response.getStatus();
        }
        System.out.print("");
    }
}