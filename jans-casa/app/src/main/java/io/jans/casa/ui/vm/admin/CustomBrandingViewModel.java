package io.jans.casa.ui.vm.admin;

import io.jans.as.model.util.Pair;

import io.jans.casa.core.AssetsService;
import io.jans.casa.ui.UIUtils;
import io.jans.casa.misc.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.image.Image;
import org.zkoss.util.media.Media;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zul.Messagebox;

/**
 * @author jgomer
 */
public class CustomBrandingViewModel extends MainViewModel {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @WireVariable
    private AssetsService assetsService;

    private Pair<String, byte[]> logo;

    private Pair<String, byte[]> favicon;

    public Pair<String, byte[]> getLogo() {
        return logo;
    }

    public Pair<String, byte[]> getFavicon() {
        return favicon;
    }

    @Init(superclass = true)
    public void childInit() {
        logo = new Pair<>(assetsService.getLogoUrl() + randomSuffix(), null);
        favicon = new Pair<>(assetsService.getFaviconUrl() + randomSuffix(), null);
    }

    public void save() {
        
        boolean success = false;
        try {
            if (favicon.getSecond() != null) {
                assetsService.setFaviconContent(favicon.getSecond());
            }
            if (logo.getSecond() != null) {
                assetsService.setLogoContent(logo.getSecond());
            }
            assetsService.useExtraCss(AssetsService.EMPTY_SNIPPET);
            success = true;
            Messagebox.show(Labels.getLabel("adm.branding_changed"), null, Messagebox.OK, Messagebox.INFORMATION);
        } catch (Exception e) {
            UIUtils.showMessageUI(false, e.getMessage());
        }
        logActionDetails(Labels.getLabel("adm.branding_action"), success);

    }

    @NotifyChange("*")
    public void revert() {

        boolean success = false;
        try {
            assetsService.factoryReset();
            init();
            success = true;
            UIUtils.showMessageUI(true);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            UIUtils.showMessageUI(false, e.getMessage());
        }
        logActionDetails(Labels.getLabel("adm.branding_revert_action"), success);

    }

    @NotifyChange("logo")
    public void logoUploaded(Media media) {
        processUpload(logo, media);
    }

    @NotifyChange("favicon")
    public void faviconUploaded(Media media) {
        processUpload(favicon, media);
    }

    private String randomSuffix() {
        return "?" + Math.random();
    }

    private void processUpload(Pair<String, byte[]> pair, Media media) {

        if (media instanceof Image) {
            logger.info("Processing upload {}", media.getName());
            pair.setFirst(processImageMedia(media.getName(), media.getByteData()));
            pair.setSecond(media.getByteData());
        } else {
            UIUtils.showMessageUI(false, Labels.getLabel("adm.branding_noimg"));
        }

    }

    private String processImageMedia(String name, byte[] data) {

        String dataUri = null;
        try {
            dataUri = Utils.getImageDataUriEncoding(data, name);
        } catch (Exception e) {
            logger.debug(e.getMessage(), e);
            UIUtils.showMessageUI(false);
        }
        return dataUri;

    }

}
