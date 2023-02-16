package io.jans.ads;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.ads.model.Deployment;
import io.jans.ads.model.DeploymentDetails;
import io.jans.ads.model.ProjectMetadata;
import io.jans.agama.dsl.TranspilationResult;
import io.jans.agama.dsl.Transpiler;
import io.jans.agama.dsl.TranspilerException;
import io.jans.agama.dsl.error.SyntaxException;
import io.jans.agama.engine.misc.FlowUtils;
import io.jans.agama.engine.service.AgamaPersistenceService;
import io.jans.agama.model.Flow;
import io.jans.agama.model.FlowMetadata;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.Files;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitor;
import java.nio.file.FileVisitResult;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.Collections;
import java.util.function.BiPredicate;
import java.util.Date;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.ZipParameters;

import org.slf4j.Logger;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/*
 * This bean deploys .gama project files. Modifications of this file must not only account a single
 * VM scenario but a multi-node environment, e.g. several containers running this code concurrently 
 */
@ApplicationScoped
public class Deployer {
    
    private static final String BASE_DN = "ou=deployments,ou=agama,o=jans";
    private static final String CUST_LIBS_DIR = "/opt/jans/jetty/jans-auth/custom/libs";
    private static final String ASSETS_DIR = "/opt/jans/jetty/jans-auth/agama";

    private static final String[] ASSETS_SUBDIRS = { "ftl", "fl" };
    private static final String SCRIPTS_SUBDIR = "scripts";
    
    private static final String[] TEMPLATES_EXTENSIONS = new String[] { "ftl", "ftlh" };
    private static final String[] SCRIPTS_EXTENSIONS = new String[] { "java", "groovy" };
    private static final String FLOW_EXT = "flow";

    private static final String METADATA_FILE = "project.json";
    private static final boolean ON_CONTAINERS = System.getenv("CN_VERSION") != null;
    
    @Inject
    private ObjectMapper mapper;
    
    @Inject
    private Logger logger;
    
    @Inject
    private PersistenceEntryManager entryManager;
    
    @Inject
    private FlowUtils futils;
    
    @Inject
    private AgamaPersistenceService aps;

    private Base64.Encoder b64Encoder;
    private Base64.Decoder b64Decoder;
    
    private Map<String, Long> projectsFinishTimes;    
    private Map<String, Set<String>> projectsBasePaths;
    private Map<String, Set<String>> projectsFlows;
    private Map<String, Set<String>> projectsLibs;
    
    public void process() throws IOException {
        
        Filter filter = Filter.createANDFilter(
                            Filter.createEqualityFilter("jansActive", false),
                            Filter.createPresenceFilter("jansStartDate"));

        List<Deployment> depls = entryManager.findEntries(BASE_DN, Deployment.class, filter,
                new String[]{ "jansId", "jansStartDate", "jansEndDate", "adsPrjDeplDetails" });
        
        //Find the oldest, non-active entry without finish timestamp. Pick that one
        Deployment deployment = depls.stream().filter(d -> d.getFinishedAt() == null)
                .min((d1, d2) -> d1.getCreatedAt().compareTo(d2.getCreatedAt())).orElse(null);

        if (deployment == null) {
            updateFlowsAndAssets(depls);
        } else {
            deployProject(deployment.getDn(), deployment.getId(),
                    deployment.getDetails().getProjectMetadata().getProjectName());
        }

    }
    
    private void deployProject(String dn, String prjId, String name) throws IOException {

        logger.info("Deploying project {}", name);
        DeploymentDetails dd = new DeploymentDetails();

        Deployment dep = entryManager.find(dn, Deployment.class, null);
        String b64EncodedAssets = dep.getAssets();
        //Here, b64EncodedAssets has the layout of a .gama file
        dep.setTaskActive(true);
        dep.setAssets(null);
        
        logger.info("Marking deployment task as active");
        //This merge helps other nodes/pods not to take charge of this very deployment task
        entryManager.merge(dep);
  
        Path p = extractGamaFile(b64EncodedAssets);
        String tmpdir = p.toString();
        dd.setProjectMetadata(computeMetadata(name, tmpdir));

        //Check the zip has the expected layout      
        Path pcode = Paths.get(tmpdir, "code");
        Path pweb = Paths.get(tmpdir, "web");
        Path plib = Paths.get(tmpdir, "lib");

        if (Files.isDirectory(pcode) && Files.isDirectory(pweb)) {
            
            try {
                Set<String> flowIds = createFlows(pcode, dd);
                if (dd.getError() == null) {
                    projectsFlows.put(prjId, flowIds);

                    Set<String> libsPaths = transferJarFiles(plib);
                    ZipFile zip = compileAssetsArchive(p, pweb, plib);
                    byte[] bytes = extractZipFileWithPurge(zip, ASSETS_DIR,
                            projectsBasePaths.get(prjId), projectsLibs.get(prjId));

                    Set<String> basePaths = new HashSet<>();
                    //Update this project's base paths: use the subdirs of web folder
                    Files.find​(pweb, 1, (pa, attrs) -> attrs.isDirectory())
                        .map(pa -> pa.getFileName().toString()).forEach(basePaths::add);
                    basePaths.remove(pweb.getFileName().toString());
                    projectsBasePaths.put(prjId, basePaths);

                    //Update this project's libs paths
                    libsPaths.addAll(computeSourcePaths(plib));
                    projectsLibs.put(prjId, libsPaths);

                    dd.setFolders(new ArrayList<>(basePaths));
                    dd.setLibs(new ArrayList<>(libsPaths));
                    //Update binary in DB - not a gama file anymore!
                    dep.setAssets(new String(b64Encoder.encode(bytes), UTF_8));
                }
            } catch (Exception e) {
                String msg = e.getMessage();
                logger.error(msg, e);
                dd.setError("An error occurred: " + msg);
            }

        } else {
            logger.warn("This does not seem to be a .gama file");
            dd.setError("Archive missing web and/or code subdirectories");
        }

        dep.setDetails(dd);
        //Mark as finished
        dep.setTaskActive(false);
        Date d = new Date();
        dep.setFinishedAt(d);

        if (dd.getError() != null) {
            logger.warn("Deployment of project {} was not successful: {}", name, dd.getError());
        }
        logger.info("Finishing deployment task...");

        projectsFinishTimes.put(prjId, d.getTime());
        entryManager.merge(dep);
        
        try {
            logger.debug("Cleaning .gama extraction dir");
            removeDir(p);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

    }
    
    private Set<String> createFlows(Path dir, DeploymentDetails dd) throws IOException {

        BiPredicate<Path, BasicFileAttributes> matcher = 
            (path, attrs) -> attrs.isRegularFile() && path.getFileName().toString().endsWith("." + FLOW_EXT);
        
        logger.info("Looking for .{} files under {}", FLOW_EXT, dir); 
        Map<Path, String> flowsCode = Files.find​(dir, 3, matcher).collect(Collectors.toMap(p -> p, p -> ""));
        flowsCode = new HashMap<>(flowsCode);   //Make map modifiable
        
        Set<Path> flowsPaths = flowsCode.keySet();
        for (Path p: flowsPaths) {
            if (logger.isDebugEnabled()) {
                logger.debug("Reading {}", p.getFileName());
            }
            flowsCode.put(p, Files.readString(p));
        }
        
        if (flowsPaths.isEmpty()) {
            dd.setError("There are no flows in this archive");
        }
        
        Map<String, String> flowsOutcome = new HashMap<>();
        
        for (Path p: flowsPaths) {
            String error = null;
            String source = flowsCode.get(p);
            String qname = p.getFileName().toString();
            
            try {
                qname = qname.substring(0, qname.length() - FLOW_EXT.length() - 1);
                
                logger.info("Processing flow {}", qname);
                //Despite the Transpilation timer automatically processes the flows as they are added
                //to DB, we make transpilation here to be able to supply an immediate response, i.e.
                //no need to wait for an async task to occur
                TranspilationResult tresult = Transpiler.transpile(qname, source);
                logger.info("Successful transpilation");
                
                Flow fl = aps.getFlow(qname, true);
                boolean add = fl == null;
                if (add) {
                    fl = new Flow();
                } else {
                    logger.info("Flow already existing in DB");
                }
                
                FlowMetadata meta = fl.getMetadata();
                meta.setFuncName(tresult.getFuncName());
                meta.setInputs(tresult.getInputs());
                meta.setTimeout(tresult.getTimeout());
                meta.setTimestamp(System.currentTimeMillis());
                meta.setAuthor(dd.getProjectMetadata().getAuthor());
                //No displayname or description. No handling of properties either
    
                String compiled = tresult.getCode();
                fl.setMetadata(meta);
                fl.setSource(source);
                fl.setTranspiled(compiled);
                
                fl.setQname(qname);
                fl.setTransHash(futils.hash(compiled));
                // revision = 0 and enabled by default assumed
                fl.setEnabled(true);
                
                if (add) {
                    fl.setDn(dnFromQname(qname));
                    logger.info("Persisting flow {}", qname);
                    entryManager.persist(fl);
                } else {
                    logger.info("Updating flow {}", qname);
                    entryManager.merge(fl);
                }
                
            } catch (SyntaxException se) {
                error = se.getMessage();
            } catch (TranspilerException te) {
                error = te.getMessage();
                if (te.getCause() != null) {
                    error += "\n" + te.getCause().getMessage();
                }
            } catch (Exception e) {
                error = e.getMessage();
                logger.error(error, e);
            }
            
            if (error != null) {
                logger.error("Transpilation failed!");
                
                if (dd.getError() == null) {
                    dd.setError("There were problems processing one or more flows");
                }
            }
            flowsOutcome.put(qname, error);
        }
        dd.setFlowsError(flowsOutcome);
        return new HashSet<>(flowsOutcome.keySet());
        
    }
    
    private ZipFile compileAssetsArchive(Path root, Path webroot, Path lib) throws IOException {
        
        String rnd = rndName();

        Path agama = Files.createDirectory(Paths.get(root.toString(), rnd));
        String agamStr = agama.toString();
        logger.debug("Created temp directory");

        Path ftl = Files.createDirectory(Paths.get(agamStr, "ftl"));
        Path fl = Files.createDirectory(Paths.get(agamStr, "fl"));
        Path scripts = Files.createDirectory(Paths.get(agamStr, SCRIPTS_SUBDIR));

        logger.debug("Copying templates to {}", ftl);
        Files.walkFileTree(webroot, copyVisitor(webroot, ftl, TEMPLATES_EXTENSIONS, true));
        logger.debug("Copying assets to {}", fl);
        Files.walkFileTree(webroot, copyVisitor(webroot, fl, TEMPLATES_EXTENSIONS, false));
        
        if (Files.isDirectory(lib)) {
            logger.debug("Copying .java and .groovy sources to {}", scripts);
            Files.walkFileTree(lib, copyVisitor(lib, scripts, SCRIPTS_EXTENSIONS, true));
        }

        //Make a zip with scripts, ftl, and fl folders
        ZipParameters params = new ZipParameters();
        params.setCompressionMethod(CompressionMethod.STORE);
        
        Path newZipPath = Paths.get(root.toString(), rndName());
        logger.info("Compressing to {}", newZipPath);

        ZipFile newZip = new ZipFile(newZipPath.toFile());
        newZip.addFolder(ftl.toFile(), params);
        newZip.addFolder(fl.toFile(), params);
        newZip.addFolder(scripts.toFile(), params);

        return newZip;

    }
    
    private Set<String> computeSourcePaths(Path lib) throws IOException {
        
        BiPredicate<Path, BasicFileAttributes> matcher = (path, attrs) -> attrs.isRegularFile() &&  
            Stream.of(SCRIPTS_EXTENSIONS).anyMatch(ext -> path.getFileName().toString().endsWith("." + ext));
            
        if (Files.isDirectory(lib)) {
            String slib = lib.toString();

            try (Stream<Path> stream = Files.find(lib, 20, matcher)) {
                return stream.map(Path::toString)
                        .map(s -> s.substring(slib.length() + 1)).collect(Collectors.toSet());
            }
        }
        return Collections.emptySet();
        
    }
    
    private Set<String> transferJarFiles(Path lib) throws IOException {

        Set<String> paths = new HashSet<>();
        //All .jar files found at the top level are moved to the custom libs destination.
        //This applies for VM-based installations only
        if (!ON_CONTAINERS && Files.isDirectory(lib)) {
            BiPredicate<Path, BasicFileAttributes> matcher = 
                (path, attrs) -> attrs.isRegularFile() && path.getFileName().toString().endsWith(".jar");

            List<Path> list = null;
            try (Stream<Path> stream = Files.find(lib, 1, matcher)) {
                list = stream.collect(Collectors.toList());
            }
            logger.debug("Moving {} jar files to custom libs dir", list.size());

            for (Path jar : list) {
                String fn = jar.getFileName().toString();
                paths.add(fn);

                Files.move(jar, Paths.get(CUST_LIBS_DIR, fn), REPLACE_EXISTING);
                logger.debug("{} moved", fn);
            }
        }
        return paths;

    }

    private void updateFlowsAndAssets(List<Deployment> deployments) {

        logger.info("Syncing in-memory state with DB state");
        Set<String> actualPrjIds = new HashSet<>();
        
        //Retrieve all finished, successfully processed deployments
        List<Deployment> depls = deployments.stream().filter(d -> d.getFinishedAt() != null && 
                d.getDetails().getError() == null).collect(Collectors.toList());
        logger.info("{} successful deployments found", depls.size());                

        for (Deployment d : depls) {
            //In this case d only has id, start date, and end date populated
            String prjId = d.getId();
            actualPrjIds.add(prjId);
            String name = d.getDetails().getProjectMetadata().getProjectName();

            Long finishedAt = projectsFinishTimes.get(prjId);
            
            //If local map does not contain the given project or the local finishedAt value is less
            //than the DB value, extract to disk the assets (including a previous directory purge)
            //This conditional can only evaluate truthy in a multinode environment (containers) or
            //upon application startup in a VM installation
            if (finishedAt == null || finishedAt < d.getFinishedAt().getTime()) {
                //Retrieve associated assets
                String b64EncodedAssets = entryManager.find(d.getDn(), Deployment.class, 
                        new String[]{ Deployment.ASSETS_ATTR }).getAssets();

                try {
                    if (finishedAt != null) {
                        purge(projectsBasePaths.get(prjId), projectsLibs.get(prjId));
                    }
                    if (b64EncodedAssets != null) {
                        extract(b64EncodedAssets, ASSETS_DIR);
                    }
                    
                    logger.info("Assets of project {} were synced", name);
                    projectsFinishTimes.put(prjId, d.getFinishedAt().getTime());
                } catch (IOException e) {
                    logger.error("Error syncing assets of project " + name, e);
                }

            } else {
                logger.info("Assets of project {} are already synced to disk", name);
            }
        }

        //Iterate over the projects "known" to this bean. Note these may differ from the
        //currently stored projects (actualPrjIds)
        for (String prjId : projectsFlows.keySet()) {

            if (!actualPrjIds.contains(prjId)) {
                //If a project has disappeared, do flows removal and directories removal
                logger.info("Project with id {} has been removed recently. Removing references...", prjId);

                try {
                    projectsFinishTimes.remove(prjId);
                    purge(projectsBasePaths.get(prjId), projectsLibs.get(prjId));
                } catch(IOException e) {
                    logger.error(e.getMessage());
                }

                removeFlows(projectsFlows.get(prjId));
            }
        }
        
        projectsFlows.clear();
        projectsBasePaths.clear();
        projectsLibs.clear();
        //Refresh maps wrt DB content
        for (Deployment d : depls) {
            String prjId = d.getId();
            DeploymentDetails dd = d.getDetails();

            Set<String> set = Optional.ofNullable(dd.getFlowsError()).map(Map::keySet)
                    .orElse(new HashSet<>());
            projectsFlows.put(prjId, set);
            
            set = Optional.ofNullable(dd.getFolders()).map(HashSet::new)
                    .orElse(new HashSet<>());
            projectsBasePaths.put(prjId, set);
            
            set = Optional.ofNullable(dd.getLibs()).map(HashSet::new)
                    .orElse(new HashSet<>());
            projectsLibs.put(prjId, set);
        }

    }
    
    private void removeFlows(Set<String> flows) {
        
        for (String flow : flows) {
            try {
                String dn = dnFromQname(flow);
                if (entryManager.contains(dn, Flow.class)) {
                    logger.info("Removing flow {}", flow);
                    entryManager.remove(dn, Flow.class);
                }
            } catch (Exception e) {
                logger.error("Error removing flow " + flow, e);
            }
        }
        
    }
    
    private ProjectMetadata computeMetadata(String name, String path) {
        
        ProjectMetadata meta = new ProjectMetadata();
        Path p = Paths.get(path, METADATA_FILE);

        if (!Files.isRegularFile(p)) {
            logger.warn("Archive has no metadata file");
        } else {
            try {            
                meta =  mapper.readValue(Files.readString(p, UTF_8), ProjectMetadata.class);
            } catch (IOException e) {
                logger.error("Unable to read archive metadata", e);
            }
        }
        meta.setProjectName(name);
        return meta;
        
    }

    private static String dnFromQname(String qname) {
        return String.format("%s=%s,%s", Flow.ATTR_NAMES.QNAME,
                qname, AgamaPersistenceService.AGAMA_FLOWS_BASE);
    }
    
    // ========== File-system related utilities follow: ===========

    //Walpurgis
    private void purge(Set<String> dirs, Set<String> filesToRemove) throws IOException {
        
        if (dirs != null) {
            for (String dir : dirs) {
                for (String subdir : ASSETS_SUBDIRS) {
                    Path p = Paths.get(ASSETS_DIR, subdir, dir);

                    if (Files.isDirectory(p)) {
                        logger.info("Flushing folder {}", p);
                        removeDir(p);
                    }
                }
            }
        }

        if (filesToRemove == null) return;

        for (String f : filesToRemove) {
            Path p = null;

            if (f.endsWith(".jar")) {
                p = Paths.get(CUST_LIBS_DIR, f);
            } else {
                p = Paths.get(ASSETS_DIR, SCRIPTS_SUBDIR, f);
            }

            logger.debug("Removing file {}", f);
            Files.deleteIfExists(p);
        }
        
    }

    private void extract(String b64EncodedAssets, String destination) throws IOException {
        
        if (b64EncodedAssets == null) return;
        
        String name = rndName();
        Path p = Files.createTempFile​(name, null);
        logger.debug("Dumping decoded Base64 representation to {}", p);
        Files.write(p, b64Decoder.decode(b64EncodedAssets.getBytes(UTF_8)));

        try (ZipFile zip = new ZipFile(p.toFile())) {
            logger.info("Extracting contents of {} to {}", p, destination);
            zip.extractAll(destination);
        } finally {
            logger.trace("Removing temp file");
            Files.delete(p);
        }

    }
    
    private Path extractGamaFile(String b64EncodedContents) throws IOException {
        
        String tmpdir = rndName();
        Path p = Files.createTempDirectory(tmpdir);

        logger.info("Extracting .gama file to {}", p);
        extract(b64EncodedContents, p.toString());
        return p;
        
    }    

    private byte[] extractZipFileWithPurge(ZipFile zip, String destination,
            Set<String> dirsPurge, Set<String> filesToRemove) throws IOException {

        Path zipPath = zip.getFile().toPath();
        purge(dirsPurge, filesToRemove);

        logger.debug("Extracting contents of {} to {}", zipPath, destination); 
        zip.extractAll(destination);
        return Files.readAllBytes(zipPath);

    }

    private static void removeDir(Path p) throws IOException {
        
        Files.walkFileTree(p, new SimpleFileVisitor<Path>() {
            
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException {
                    
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException e)
                throws IOException {
            
                if (e == null) {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                } else {
                    throw e;
                }
            }
            
        });
        
    }

    private static FileVisitor copyVisitor(Path source, Path target, String[] extensions, boolean include) {
        
        List<String> suffixes = Stream.of(extensions).map(s -> "." + s).collect(Collectors.toList());
        
        return new SimpleFileVisitor<Path>() {
            
             @Override
             public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                 throws IOException {

                 Path targetdir = target.resolve(source.relativize(dir));
                 try {
                     Files.copy(dir, targetdir);
                 } catch (FileAlreadyExistsException e) {
                      if (!Files.isDirectory(targetdir))
                          throw e;
                 }
                 return FileVisitResult.CONTINUE;

             }

             @Override
             public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                 throws IOException {

                 String fn = file.getFileName().toString().toLowerCase();
                 boolean match = suffixes.stream().anyMatch(fn::endsWith);

                 if ((match && include) || (!match && !include)) {
                     Files.copy(file, target.resolve(source.relativize(file)));
                 }
                 return FileVisitResult.CONTINUE;

             }

         };

    }

    private static String rndName() {
        return ("" + Math.random()).substring(2);
    }

    @PostConstruct
    private void init() {

        b64Encoder = Base64.getEncoder();
        b64Decoder = Base64.getDecoder();
        projectsBasePaths = new HashMap<>();
        projectsFinishTimes = new HashMap<>();
        projectsFlows = new HashMap<>();
        projectsLibs = new HashMap<>();

    }

}