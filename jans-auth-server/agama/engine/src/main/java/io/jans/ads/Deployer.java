package io.jans.ads;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.ads.model.*;
import io.jans.agama.dsl.*;
import io.jans.agama.dsl.error.SyntaxException;
import io.jans.agama.engine.misc.FlowUtils;
import io.jans.agama.engine.service.*;
import io.jans.agama.model.*;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import java.util.regex.*;
import java.util.stream.*;

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
    
    private static final String[] TEMPLATES_EXTENSIONS = new String[] { "ftl", "ftlh", "txt" };
    private static final String[] SCRIPTS_EXTENSIONS = new String[] { "java", "groovy" };
    private static final String FLOW_EXT = "flow";

    private static final String METADATA_FILE = "project.json";
    private static final boolean ON_CONTAINERS = System.getenv("CN_VERSION") != null;
    
    private static final long DEPLOY_TIMEOUT = TimeUnit.MINUTES.toMillis(5);
    private static final Pattern BP_PATT = Pattern.compile("\n[ \\t]+Basepath[ \\t]+\"");
    
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
    
    @Inject
    private LabelsService lbls;

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
        Deployment dep = depls.stream().filter(d -> d.getFinishedAt() == null)
                .min((d1, d2) -> d1.getCreatedAt().compareTo(d2.getCreatedAt())).orElse(null);

        if (dep == null) {
            updateFlowsAndAssets(depls);

            //find deployments in course
            filter = Filter.createANDFilter(Filter.createEqualityFilter("jansActive", true),
                            Filter.createNOTFilter(Filter.createPresenceFilter("jansEndDate")));

            removeStaleDeployments(entryManager.findEntries(BASE_DN, Deployment.class, filter,
                        new String[]{ "jansId", "jansStartDate" }), System.currentTimeMillis());
        } else {
            deployProject(dep.getDn(), dep.getId(), dep.getDetails().isAutoconfigure(),
                    dep.getDetails().getProjectMetadata().getProjectName());
        }
        
    }
    
    private void deployProject(String dn, String prjId, boolean autoconf, String name) throws IOException {

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
                //craft a path so assets of different projects do not collide, see jans#4501
                String prjBasepath = makeShortSafePath(prjId);
                Set<String> flowIds = createFlows(pcode, dd, prjBasepath, autoconf);
                if (dd.getError() == null) {
                    projectsFlows.put(prjId, flowIds);

                    ZipFile zip = compileAssetsArchive(p, pweb, plib, prjBasepath);
                    byte[] bytes = extractZipFileWithPurge(zip, ASSETS_DIR,
                            projectsBasePaths.get(prjId), projectsLibs.get(prjId));

                    Set<String> basePaths = Set.of(prjBasepath);
                    projectsBasePaths.put(prjId, basePaths);

                    Set<String> libsPaths = transferJarFiles(plib);
                    //Update this project's libs paths
                    libsPaths.addAll(computeSourcePaths(plib));
                    projectsLibs.put(prjId, libsPaths);

                    dd.setFolders(new ArrayList<>(basePaths));
                    dd.setLibs(new ArrayList<>(libsPaths));
                    //Update binary in DB - not a gama file anymore!
                    dep.setAssets(new String(b64Encoder.encode(bytes), UTF_8));
                    lbls.addLabels(prjBasepath);
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

        entryManager.merge(dep);    //If this fails, deployment will remain pending, see #removeStaleDeployments
        projectsFinishTimes.put(prjId, d.getTime());
        
        try {
            logger.debug("Cleaning .gama extraction dir");
            removeDir(p);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

    }
    
    private Set<String> createFlows(Path dir, DeploymentDetails dd, 
                String prjBasepath, boolean autoconfigure) throws IOException {

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
        } else {
            logger.debug("Flows' basepaths will all be prefixed with '{}'", prjBasepath);  
        }
        
        Map<String, String> flowsOutcome = new HashMap<>();
        ProjectMetadata prjMetadata = dd.getProjectMetadata();
        List<String> noDirectLaunch = Optional.ofNullable(
                prjMetadata.getNoDirectLaunchFlows()).orElse(Collections.emptyList());
        Map<String, Map<String, Object>> configHints = Optional.ofNullable(
                prjMetadata.getConfigHints()).orElse(Collections.emptyMap());
        
        for (Path p: flowsPaths) {
            String error = null;
            String source = flowsCode.get(p);
            String qname = p.getFileName().toString();
            
            try {
                //this is a workaround to avoid assets mess/loss when different projects use the same folder/file names
                source = insertProjectBasepath(source, prjBasepath);
                qname = qname.substring(0, qname.length() - FLOW_EXT.length() - 1);
                
                logger.info("Processing flow {}", qname);
                //Despite the Transpilation timer automatically processes the flows as they are added
                //to DB, we make transpilation here to be able to supply an immediate response, i.e.
                //no need to wait for an async task to occur
                TranspilationResult tresult = Transpiler.transpile(qname, source);
                logger.info("Successful transpilation");
                
                Flow fl = aps.getFlow(qname, true);
                boolean add = fl == null;
                FlowMetadata prvMeta = null;

                if (add) {
                    fl = new Flow();
                } else {
                    logger.info("Flow already existing in DB");
                    prvMeta = fl.getMetadata();
                }
                
                FlowMetadata meta = fl.getMetadata();
                meta.setFuncName(tresult.getFuncName());
                meta.setInputs(tresult.getInputs());
                meta.setTimeout(tresult.getTimeout());
                meta.setTimestamp(System.currentTimeMillis());
                meta.setAuthor(prjMetadata.getAuthor());

                if (prvMeta != null) {
                    meta.setDisplayName(prvMeta.getDisplayName());
                    meta.setDescription(prvMeta.getDescription());
                    meta.setProperties(prvMeta.getProperties());
                }

                if (autoconfigure) {
                    logger.warn("Setting flow configuration as provided in project archive");
                    meta.setProperties(configHints.get(qname));
                }
                
                String compiled = tresult.getCode();
                fl.setMetadata(meta);
                fl.setSource(source);
                fl.setTranspiled(compiled);
                
                fl.setQname(qname);
                fl.setTransHash(futils.hash(compiled));
                // revision = 0 assumed by default                
                fl.setEnabled(!noDirectLaunch.contains(qname));
                
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
    
    private ZipFile compileAssetsArchive(Path root, Path webroot, Path lib, String prjBasepath) throws IOException {

        Path agama = Files.createDirectory(Paths.get(root.toString(), rndName()));
        String agamStr = agama.toString();
        logger.debug("Created temp directory");

        Path ftl = Files.createDirectories(Paths.get(agamStr, "ftl", prjBasepath));
        Path fl = Files.createDirectories(Paths.get(agamStr, "fl", prjBasepath));
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
        newZip.addFolder(ftl.toFile().getParentFile(), params);
        newZip.addFolder(fl.toFile().getParentFile(), params);
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
                try {
                    //Retrieve associated assets
                    String b64EncodedAssets = entryManager.find(d.getDn(), Deployment.class, 
                            new String[]{ Deployment.ASSETS_ATTR }).getAssets();

                    if (finishedAt != null) {
                        purge(projectsBasePaths.get(prjId), projectsLibs.get(prjId));
                    }
                    if (b64EncodedAssets != null) {
                        extract(b64EncodedAssets, ASSETS_DIR);
                    }

                    logger.info("Assets of project {} were synced", name);
                    lbls.addLabels(makeShortSafePath(prjId));
                    projectsFinishTimes.put(prjId, d.getFinishedAt().getTime());

                } catch (Exception e) {
                    logger.error("Error syncing assets of project " + name, e);
                }

            } else {
                logger.info("Assets of project {} are already synced to disk", name);
            }
        }

        Set<String> toRemove = new HashSet<>();
        //Iterate over the projects "known" to this bean. Note these may differ from the
        //currently stored projects (actualPrjIds)
        for (String prjId : projectsFlows.keySet()) {

            if (!actualPrjIds.contains(prjId)) {
                //If a project has disappeared, do flows removal and directories removal
                logger.info("Project with id {} has been removed recently. Removing references...", prjId);
                Set<String> basePaths = projectsBasePaths.get(prjId);

                try {
                    toRemove.addAll(projectsLibs.get(prjId));

                    projectsFinishTimes.remove(prjId);
                    purge(basePaths, null);
                } catch(IOException e) {
                    logger.error(e.getMessage());
                }

                removeFlows(projectsFlows.get(prjId));
                lbls.removeLabels(basePaths.toArray(new String[0])[0]);
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
            toRemove.removeAll(set);
        }

        //Do safe source file removal, see jans#9153
        try {
            purge(null, toRemove);
        } catch(IOException e) {
            logger.error(e.getMessage());
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
    
    private void removeStaleDeployments(List<Deployment> deployments, long instant) {

        for (Deployment d : deployments) {
            if (d.getCreatedAt().getTime() + DEPLOY_TIMEOUT < instant) {

                try {
                    String prjId = d.getId();
                    logger.info("Removing stale deployment {}", prjId);
                    entryManager.remove(d.getDn(), Deployment.class);
                } catch (Exception e) {
                    logger.error("Error removing deployment", e);
                } 
            }
        }

    }

    private static String dnFromQname(String qname) {
        return String.format("%s=%s,%s", Flow.ATTR_NAMES.QNAME,
                qname, AgamaPersistenceService.AGAMA_FLOWS_BASE);
    }
    
    // ========== File-system related utilities follow: ===========

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

        Path p = Files.createTempFile​(rndName(), null);
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

        Path p = Files.createTempDirectory(rndName());
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
                      if (!Files.isDirectory(targetdir)) throw e;
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
    
    private String insertProjectBasepath(String code, String basepath) {
        
        Matcher m = BP_PATT.matcher(code);
        if (m.find()) {
            int i = m.end();
            if (!m.find()) {    //Ensure there is only one occurrence
                return code.substring(0, i) + basepath + "/" + code.substring(i);
            }
        }
        return code;
    }
    
    private String makeShortSafePath(String id) {
        //radix 36 entails safe filename/url characters: 0-9 plus a-z
        String path = Integer.toString(id.hashCode(), Math.min(36, Character.MAX_RADIX));
        return path.substring(path.charAt(0) == '-' ? 1 : 0);
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
