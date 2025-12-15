package io.jans.casa.plugins.certauthn.vm;

import io.jans.casa.core.pojo.User;
import io.jans.casa.misc.Utils;
import io.jans.casa.plugins.certauthn.model.Certificate;
import io.jans.casa.plugins.certauthn.service.CertService;
import io.jans.casa.service.ISessionContext;
import io.jans.casa.service.SndFactorAuthenticationUtils;
import io.jans.casa.ui.UIUtils;

import java.util.List;

import org.slf4j.*;
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.Init;
import org.zkoss.util.Pair;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zul.Messagebox;

/**
 * This is the ViewModel of page cert-detail.zul. It controls the display of user certs
 */
public class CertAuthenticationSummaryVM {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @WireVariable
    private ISessionContext sessionContext;

    private User user;
    private List<Certificate> certificates;
    private CertService certService;
    private SndFactorAuthenticationUtils sndFactorUtils;

    public List<Certificate> getCertificates() {
        return certificates;
    }

    @Init
    public void init() {
        certService = CertService.getInstance();
        user = sessionContext.getLoggedUser();
        sndFactorUtils = Utils.managedBean(SndFactorAuthenticationUtils.class);
        certificates = certService.getUserCerts(user.getId());
    }

    public void delete(Certificate certificate) {

        String resetMessages = sndFactorUtils.removalConflict(CertService.AGAMA_FLOW, certificates.size(), user).getY();
        boolean reset = resetMessages != null;
        Pair<String, String> delMessages = getDeleteMessages(resetMessages);

        Messagebox.show(delMessages.getY(), delMessages.getX(), Messagebox.YES | Messagebox.NO,
                reset ? Messagebox.EXCLAMATION : Messagebox.QUESTION,
                event -> {
                    if (Messagebox.ON_YES.equals(event.getName())) {
                        try {
                            boolean success = certService.removeFromUser(certificate.getFingerPrint(), user.getId());
                            if (success) {
                                if (reset) {
                                    sndFactorUtils.turn2faOff(user);
                                }
                                certificates.remove(certificate);

                                BindUtils.postNotifyChange(CertAuthenticationSummaryVM.this, "certificates");
                            }
                            UIUtils.showMessageUI(success);
                        } catch (Exception e) {
                            UIUtils.showMessageUI(false);
                            logger.error(e.getMessage(), e);
                        }
                    }
                });

    }

    private Pair<String, String> getDeleteMessages(String msg){

        StringBuilder text=new StringBuilder();
        if (msg != null) {
            text.append(msg).append("\n\n");
        }
        text.append(Labels.getLabel("usercert.del_confirm"));
        if (msg != null) {
            text.append("\n");
        }

        return new Pair<>(null, text.toString());

    }

}
