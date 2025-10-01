package io.jans.casa.ui.vm.admin;

import io.jans.casa.core.ExtensionsManager;
import io.jans.casa.extension.AuthnMethod;
import io.jans.casa.timer.FSPluginChecker;
import io.jans.casa.ui.UIUtils;
import io.jans.casa.misc.Utils;
import io.jans.casa.ui.model.PluginData;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginManager;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.util.media.Media;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zul.Messagebox;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;

/**
 * @author jgomer
 */
public class PluginViewModel extends MainViewModel {

    private static final Class<AuthnMethod> AUTHN_METHOD = AuthnMethod.class;

    private Logger logger = LoggerFactory.getLogger(getClass());

    @WireVariable("extensionsManager")
    private ExtensionsManager extManager;

    @WireVariable("fSPluginChecker")
    private FSPluginChecker fspchecker;

    private List<PluginData> pluginList;

    private PluginData pluginToShow;

    private boolean engineAvailable;

    public List<PluginData> getPluginList() {
        return pluginList;
    }

    public PluginData getPluginToShow() {
        return pluginToShow;
    }

    public boolean isEngineAvailable() {
        return engineAvailable;
    }

    @Init(superclass = true)
    public void childInit() {
        pluginList = new ArrayList<>();
        engineAvailable = extManager.getPluginsRoot() != null;
        extManager.getPlugins().forEach(wrapper -> pluginList.add(buildPluginData(wrapper)));
    }

    @NotifyChange({"pluginToShow"})
    public void showPlugin(String pluginId) {
        pluginToShow = pluginList.stream().filter(pl -> pl.getDescriptor().getPluginId().equals(pluginId)).findAny().orElse(null);
    }

    @NotifyChange({"pluginToShow"})
    public void uploaded(Media media) {

        boolean success = false;
        try {
            pluginToShow = null;
            byte[] blob = media.getByteData();
            logger.debug("Size of blob received: {} bytes", blob.length);

            try (JarInputStream jis = new JarInputStream(new ByteArrayInputStream(blob), false)) {

                Manifest m = jis.getManifest();
                if (m != null) {
                    String id = m.getMainAttributes().getValue("Plugin-Id");
                    String version = m.getMainAttributes().getValue("Plugin-Version");
                    String deps = m.getMainAttributes().getValue("Plugin-Dependencies");

                    if (pluginList.stream().anyMatch(pl -> pl.getDescriptor().getPluginId().equals(id))) {
                        UIUtils.showMessageUI(false, Labels.getLabel("adm.plugins_already_existing", new String[] { id }));
                    } else if (Stream.of(id, version).allMatch(Utils::isNotEmpty)) {
                        try {
                            if (Utils.isNotEmpty(deps)) {
                                logger.warn("This plugin reports dependencies. This feature is not available in Gluu Casa");
                                logger.warn("Your plugin may not work properly");
                            }
                            //Copy the jar to plugins dir
                            Files.write(Paths.get(extManager.getPluginsRoot().toString(), media.getName()), blob, StandardOpenOption.CREATE_NEW);
                            logger.info("Plugin jar file copied to app plugins directory");
                            Messagebox.show(Labels.getLabel("adm.plugins_deploy_pending"));
                            success = true;
                        } catch (Exception e) {
                            logger.error(e.getMessage(), e);
                            UIUtils.showMessageUI(false);
                        }
                    } else {
                        UIUtils.showMessageUI(false, Labels.getLabel("adm.plugins_invalid_plugin"));
                        logger.error("Plugin's manifest file missing ID and/or Version");
                    }

                } else {
                    UIUtils.showMessageUI(false, Labels.getLabel("adm.plugins_invalid_plugin"));
                    logger.error("Jar file with no manifest file");
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        logActionDetails(Labels.getLabel("adm.plugins_action_add"), success);
        
    }

    public void deletePlugin(String pluginId, String provider) {

        if (getSettings().getAcrPluginMap().values().contains(pluginId)) {
            Messagebox.show(Labels.getLabel("adm.plugin_plugin_bound_method"), null, Messagebox.OK, Messagebox.EXCLAMATION);
        } else {
            logger.info("Attempting to remove plugin {}", pluginId);
            provider = Utils.isEmpty(provider) ? Labels.getLabel("adm.plugins_nodata") : provider;
            String msg = Labels.getLabel("adm.plugins_confirm_del", new String[]{ pluginId, provider });

            Messagebox.show(msg, null, Messagebox.YES | Messagebox.NO, Messagebox.QUESTION,
                    event -> {
                        if (Messagebox.ON_YES.equals(event.getName())) {

                            boolean success = fspchecker.removePluginFile(pluginId);
                            Messagebox.show(Labels.getLabel(
                                    success ? "adm.plugins_undeploy_pending" : "adm.plugins_removal_failed"));
                            
                            logActionDetails(Labels.getLabel("adm.plugins_action_remove"), success);

                            pluginToShow = null;
                            BindUtils.postNotifyChange(PluginViewModel.this, "pluginToShow");
                        }
                    }
            );
        }

    }

    @NotifyChange({"pluginToShow"})
    public void hidePluginDetails() {
        pluginToShow = null;
    }

    private PluginData buildPluginData(PluginWrapper pw) {

        PluginDescriptor pluginDescriptor = pw.getDescriptor();
        logger.debug("Building a PluginData instance for plugin {}", pw.getPluginId());
        PluginData pl = new PluginData();

        PluginState plState = pw.getPluginState();
        //In practice resolved (that is, just loaded not started) could be seen as stopped
        plState = plState.equals(PluginState.RESOLVED) ? PluginState.STOPPED : plState;

        pl.setState(Labels.getLabel("adm.plugins_state." + plState.toString()));
        pl.setPath(pw.getPluginPath().toString());
        pl.setDescriptor(pluginDescriptor);

        if (PluginState.STARTED.equals(plState)) {
            //pf4j doesn't give any info if not in started state
            pl.setExtensions(buildExtensionList(pw));
        }

        return pl;

    }

    private List<String> buildExtensionList(PluginWrapper wrapper) {

        List<String> extList = new ArrayList<>();
        PluginManager manager = wrapper.getPluginManager();
        String pluginId = wrapper.getPluginId();
        logger.trace("Building human-readable extensions list for plugin {}", pluginId);

        //plugin manager's getExtension methods outputs data only when the plugin is already started! (not simply loaded)
        for (Object obj : manager.getExtensions(pluginId)) {
            Class cls = obj.getClass();

            if (!AUTHN_METHOD.isAssignableFrom(cls)) {
                extList.add(getExtensionLabel(
                        Stream.of(cls.getInterfaces()).findFirst().map(Class::getName).orElse(""),
                        cls.getSimpleName()));
            }
        }

        for (AuthnMethod method : manager.getExtensions(AUTHN_METHOD, pluginId)) {
            String text = Labels.getLabel(method.getUINameKey());
            String acr = method.getAcr();

            if (Optional.ofNullable(getSettings().getAcrPluginMap().get(acr)).map(pluginId::equals).orElse(false)) {
                text += Labels.getLabel("adm.plugins_acr_handler", new String[]{ acr });
            }
            extList.add(getExtensionLabel(AUTHN_METHOD.getName(), text));
        }

        return extList;

    }

    private String getExtensionLabel(String clsName, Object ...args) {
        String text = Labels.getLabel("adm.plugins_extension." + clsName, args);
        return text == null ? clsName.substring(clsName.lastIndexOf(".") + 1) : text;
    }

}
