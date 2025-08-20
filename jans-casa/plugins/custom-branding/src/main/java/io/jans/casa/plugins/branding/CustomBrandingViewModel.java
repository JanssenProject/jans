package io.jans.casa.plugins.branding;

import io.jans.casa.service.IBrandingManager;
import io.jans.casa.ui.UIUtils;
import io.jans.casa.misc.Utils;
import io.jans.util.Pair;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.image.Image;
import org.zkoss.util.media.Media;
import org.zkoss.util.resource.Labels;
import org.zkoss.zul.Messagebox;

/**
 * ZK View Model for the custom branding page.
 */
public class CustomBrandingViewModel {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private IBrandingManager assetsService;

    private BrandingOption previouslySelected;

    private BrandingOption brandingOption;

    private CssSnippetHandler snippetHandler;

    private boolean uiOverrideButtonColors;

    private Pair<String, byte[]> logo;

    private Pair<String, byte[]> favicon;

    public String getBrandingOption() {
        return brandingOption.toString();
    }

    public CssSnippetHandler getSnippetHandler() {
        return snippetHandler;
    }

    public boolean isUiOverrideButtonColors() {
        return uiOverrideButtonColors;
    }

    public Pair<String, byte[]> getLogo() {
        return logo;
    }

    public Pair<String, byte[]> getFavicon() {
        return favicon;
    }

    @Init
    public void init() {

        logger.debug("Initializing ViewModel");
        assetsService = Utils.managedBean(IBrandingManager.class);

        if (Utils.isNotEmpty(assetsService.getPrefix())) {
            brandingOption = BrandingOption.EXTERNAL_PATH;
        } else if (Utils.isNotEmpty(assetsService.getExtraCss())) {
            brandingOption = BrandingOption.EXTRA_CSS;
        } else {
            brandingOption = BrandingOption.NONE;
        }
        changeBranding(brandingOption.toString());

    }

    @NotifyChange({"brandingOption", "logo", "favicon", "snippetHandler", "uiOverrideButtonColors"})
    public void changeBranding(String option) {

        logger.info("Branding option changed to {}", option);
        previouslySelected = brandingOption;
        brandingOption = BrandingOption.valueOf(option);

        if (brandingOption.equals(BrandingOption.EXTRA_CSS)) {
            snippetHandler = new CssSnippetHandler(assetsService.getExtraCss(), Labels.getLabel("adm.branding_footer"));
            uiOverrideButtonColors = snippetHandler.getPrimaryButtonColor() != null;
            logo = new Pair<>(assetsService.getLogoUrl() + randomSuffix(), null);
            favicon = new Pair<>(assetsService.getFaviconUrl() + randomSuffix(), null);
        }

    }

    @NotifyChange("logo")
    public void logoUploaded(Media media) {
        logger.trace("Logo file has been uploaded");
        processUpload(logo, media);
    }

    @NotifyChange("favicon")
    public void faviconUploaded(Media media) {
        logger.trace("Favicon file has been uploaded");
        processUpload(favicon, media);
    }

    public void save() {

        try {
            switch (brandingOption) {
                case NONE:
                    assetsService.factoryReset();
                    UIUtils.showMessageUI(true);
                    break;
                case EXTERNAL_PATH:
                    String brandingPath = IBrandingManager.CUSTOM_FILEPATH;
                    //Check directory exists
                    if (!Files.isDirectory(Paths.get(brandingPath, "images")) || !Files.isDirectory(Paths.get(brandingPath, "styles", "gluu"))) {
                        Messagebox.show(Labels.getLabel("branding.no_subdirs", new String[]{brandingPath}), null,
                                Messagebox.YES | Messagebox.NO, Messagebox.QUESTION,
                                event -> {
                                    if (Messagebox.ON_YES.equals(event.getName())) {
                                        useExternalAssets();
                                    } else {
                                        changeBranding(previouslySelected.toString());
                                        BindUtils.postNotifyChange(CustomBrandingViewModel.this, "brandingOption");
                                    }
                                }
                        );
                    } else {
                        useExternalAssets();
                    }
                    break;
                case EXTRA_CSS:
                    //Write favicon & logo to disk
                    if (favicon.getSecond() != null) {
                        assetsService.setFaviconContent(favicon.getSecond());
                    }
                    if (logo.getSecond() != null) {
                        assetsService.setLogoContent(logo.getSecond());
                    }
                    assetsService.useExtraCss(snippetHandler.getSnippet(uiOverrideButtonColors));
                    //If it gets here, all changes were fine
                    Messagebox.show(Labels.getLabel("adm.branding_changed"), null, Messagebox.OK, Messagebox.INFORMATION);
                    break;
                default:
                    //Added to pass style check
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            UIUtils.showMessageUI(false, e.getMessage());
        }

    }

    @NotifyChange({"snippetHandler", "uiOverrideButtonColors"})
    public void buttonColorChanging(boolean override) throws Exception {
        uiOverrideButtonColors = override;
        if (override) {
            snippetHandler.assignMissingButtonColors();
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

    private void processUpload(Pair<String, byte[]> pair, Media media) {

        if (media instanceof Image) {
            logger.info("Processing blob {}", media.getName());
            pair.setFirst(processImageMedia(media.getName(), media.getByteData()));
            pair.setSecond(media.getByteData());
        } else {
            UIUtils.showMessageUI(false, Labels.getLabel("branding.quick_noimg"));
        }

    }

    private void useExternalAssets() {

        try {
            assetsService.useExternalAssets();
            Messagebox.show(Labels.getLabel("adm.branding_changed"), null, Messagebox.OK, Messagebox.INFORMATION);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            UIUtils.showMessageUI(false, e.getMessage());
        }

    }

    private String randomSuffix() {
        return "?" + Math.random();
    }

}
