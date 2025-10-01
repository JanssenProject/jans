package io.jans.casa.timer;

import io.jans.casa.core.ExtensionsManager;
import io.jans.casa.core.TimerService;
import io.jans.casa.misc.Utils;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.quartz.JobExecutionContext;
import org.quartz.listeners.JobListenerSupport;
import org.slf4j.Logger;
import org.zkoss.util.Pair;

/**
 * @author jgomer
 */
@Named
@ApplicationScoped
public class FSPluginChecker extends JobListenerSupport {

    private static final int SCAN_INTERVAL = 60;    //check the plugins dir every 60sec

    @Inject
    private Logger logger;

    @Inject
    private TimerService timerService;

    @Inject
    private ExtensionsManager extManager;

    private String jobName;

    private Path pluginsRoot;

    private Map<String, Long> timeStamps;

    private List<Pair<String, File>> contents;

    public void activate(int gap) {

        try {
            if (extManager.getPluginsRoot() != null) {
                timerService.addListener(this, jobName);
                //Start in 2 seconds and repeat indefinitely
                timerService.schedule(jobName, gap, -1, SCAN_INTERVAL);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

    public boolean removePluginFile(String pluginId) {

        boolean success = false;
        File f = contents.stream().filter(pair -> pair.getX().equals(pluginId)).findFirst().map(Pair::getY).orElse(null);
        try {
            success = f != null && f.delete();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return success;

    }

    @Override
    public String getName() {
        return jobName;
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {

        logger.info("FSPluginChecker. Running timer job");
        //Obtain list of jar files in plugins root
        List<Pair<String, File>> currContents = getCurrentContents();

        //Contrast currContents vs. contents

        List<Pair<String, File>> toDelete = new ArrayList<>();
        //Search those in contents and not in currContents
        for (Pair<String, File> pair : contents) {
            String pluginId = pair.getX();
            if (currContents.stream().noneMatch(p -> p.getX().equals(pluginId))) {
                toDelete.add(pair);
                logger.info("File for plugin {} seems to have been deleted", pluginId);
            }
        }

        List<Pair<String, File>> toAdd = new ArrayList<>();
        //Search those in currContents and not in contents
        for (Pair<String, File> pair : currContents) {
            String pluginId = pair.getX();
            if (contents.stream().noneMatch(p -> p.getX().equals(pluginId))) {
                toAdd.add(pair);
                logger.info("File for plugin {} seems to have been added", pluginId);
            }
        }

        //Search those matching but with differences in file contents
        for (Pair<String, File> pair : contents) {
            String pluginId = pair.getX();
            File fileMatch = currContents.stream().filter(p -> p.getX().equals(pluginId))
                    .findFirst().map(Pair::getY).orElse(null);

            if (fileMatch != null) {
                String name = fileMatch.getName();
                if (!name.equals(pair.getY().getName())
                        || fileMatch.lastModified() != Optional.ofNullable(timeStamps.get(name)).orElse(0L)) {
                    logger.info("File for plugin {} has changed. Plugin will be redeployed", pluginId);
                    toDelete.add(pair);
                    toAdd.add(new Pair<>(pluginId, fileMatch));
                }
            }
        }
        extManager.updatePlugins(toAdd, toDelete);
        contents = currContents;
        //Update timestamps
        timeStamps = contents.stream().collect(Collectors.toMap(p -> p.getY().getName(), p -> p.getY().lastModified()));

    }

    private List<Pair<String, File>> getCurrentContents() {

        List<Pair<String, File>> currContents = new ArrayList<>();
        logger.info("Checking files in plugins directory {}", pluginsRoot);

		//https://stackoverflow.com/questions/38652295/java-nio2-directory-is-not-closed-causes-too-many-open-files-error
        try (Stream<Path> directoryStream = Files.list(pluginsRoot)) {
            List<Path> list = directoryStream.filter(Utils::isJarFile).collect(Collectors.toList());
			Set<String> pluginIdsList = new HashSet<>();

            for (Path p : list) {
                File f = p.toFile();
                String name = f.getName();

                boolean delete = true;
                try (JarInputStream jis = new JarInputStream(new BufferedInputStream(new FileInputStream(f)), false)) {
                    Manifest m = jis.getManifest();

                    if (m != null) {
                        String pluginId = m.getMainAttributes().getValue("Plugin-Id");
                        if (pluginId != null) {
                            logger.info("File '{}' implements plugin {}", name, pluginId);

                            if (pluginIdsList.contains(pluginId)) {
                                logger.info("There is another file already implementing this plugin. Current file will be removed");
                            } else {
                                pluginIdsList.add(pluginId);
                                currContents.add(new Pair<>(pluginId, f));
                                delete = false;
                            }
                        } else {
                            logger.info("File '{}' does not seem to implement a plugin for Casa", name);
                        }
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }

                try {
                    if (delete) {
                        logger.warn("Deleting file '{}'", name);
                        Files.delete(p);
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return currContents;

    }

    @PostConstruct
    private void inited() {
        jobName = getClass().getSimpleName() + "_fschecker";
        pluginsRoot = extManager.getPluginsRoot();
        contents = new ArrayList<>();
        timeStamps = new HashMap<>();
    }

}
