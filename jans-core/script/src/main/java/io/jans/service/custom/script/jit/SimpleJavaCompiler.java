package io.jans.service.custom.script.jit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;

/**
 * @author Yuriy Z
 */
public class SimpleJavaCompiler {

    private static final Logger log = LoggerFactory.getLogger(SimpleJavaCompiler.class);

    /**
     * A javac instance
     */
    private final JavaCompiler compiler;
    /**
     * The Standard file manager for the compiler
     */
    private final StandardJavaFileManager standardJavaFileManager;
    private final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

    /**
     * We keep an initialized copy for each thread.
     */
    private static final ThreadLocal<SimpleJavaCompiler> BY_THREAD = new ThreadLocal<SimpleJavaCompiler>() {
        @Override
        protected SimpleJavaCompiler initialValue() {
            return new SimpleJavaCompiler();
        }
    };

    /**
     * Create a new compiler
     */
    private SimpleJavaCompiler() {
        compiler = ToolProvider.getSystemJavaCompiler();
        standardJavaFileManager = compiler.getStandardFileManager(diagnostics, null, null);
    }

    /**
     * Compiles and class-loads a single class from the specified Java source code.
     * <p>
     * To amortize the cost of initializing a JavaCompiler, underlying instances are cached
     * on a per-thread basis. It is recommended that callers of this method reuse threads
     * for increased performance and reduced memory consumption.
     *
     * @param source a String containing Java source code to generate the class
     * @return the compiled and class-loaded class
     */
    public static <T> Class<? extends T> compile(Class<T> superClass, String source) {
        return BY_THREAD.get().compile0(superClass, source);
    }


    /**
     * Compiles and class-loads a single class from the specified Java source code.
     * <p>
     * To amortize the cost of initializing a JavaCompiler, underlying instances are cached
     * on a per-thread basis. It is recommended that callers of this method reuse threads
     * for increased performance and reduced memory consumption.
     *
     * @param sourceGen a String containing Java source code to generate the class
     * @return the compiled and class-loaded class
     */
    public static <T> Class<? extends T> compile(Class<T> superClass, Consumer<JavaCodeGenerator> sourceGen) {
        final JavaCodeGenerator cg = new JavaCodeGenerator();
        sourceGen.accept(cg);

        final StringWriter sw = new StringWriter();
        cg.generate(sw);

        final Class<? extends T> compiledClass = compile(superClass, sw.toString());
        dump(sw, compiledClass);
        return compiledClass;
    }

    public static volatile boolean dump = Boolean.getBoolean("compiler.java.dump");

    public static volatile String dumpDir = System.getProperty("compiler.java.dump.dir");

    private static void dump(StringWriter sw, Class<?> compiledClass) {
        if (dump) {
            System.out.println(sw);
        }
        if (dumpDir != null) {
            try {
                Files.write(Paths.get(dumpDir, compiledClass.getCanonicalName().replace('.', File.separatorChar) + ".java"), sw.toString().getBytes());
            } catch (IOException e) {
                throw new RuntimeException("unable to dump source file", e);
            }
        }
    }

    private static String classpath;

    public static String getClasspath() {
        if (StringUtils.isBlank(classpath)) {
            URL[] urls = ((URLClassLoader) SimpleJavaCompiler.class.getClassLoader()).getURLs();

            StringBuilder sb = new StringBuilder(System.getProperty("java.class.path"));
            sb.append(File.pathSeparator);
            for (URL url : urls) {
                sb.append(url.toString()).append(File.pathSeparator);
            }

            classpath = fixClasspath(sb.toString());
        }
        return classpath;
    }

    /**
     * Compiles and class-loads a single class from the specified Java source code.
     *
     * @param source a String containing Java source code to generate the class
     * @return the compiled and class-loaded class
     */
    private <T> Class<? extends T> compile0(Class<T> superClass, String source) {
        final List<JavaFileObject> compilationUnits = Collections.singletonList(new SourceFile(toUri("Generated"), source));
        final FileManager fileManager = new FileManager(standardJavaFileManager);
        final StringWriter output = new StringWriter();
        final String localClasspath = getClasspath();
        final JavaCompiler.CompilationTask task = compiler.getTask(output, fileManager, diagnostics, Arrays.asList("-g", "-verbose", "-proc:none", "-classpath", localClasspath), null, compilationUnits);

        if (!task.call()) {
            log.error("Compilation diagnostics:");
            for (Diagnostic<? extends JavaFileObject> diag : diagnostics.getDiagnostics()) {
               log.error(diag.getMessage(Locale.getDefault()));
            }

            log.error("Full classpath: {}", localClasspath);

            throw new IllegalArgumentException("Compilation failed:\n" + output + "\n Source code: \n" + source);
        }

        int classCount = fileManager.output.size();
        // 2nd condition to work around the case where additional "GuardedBy" generated in TA Spark tagging
        if (classCount == 1 || (classCount > 0 && "CompiledStage"
                .equals(fileManager.output.get(0).getName()))) {
            return DiscardableClassLoader.classFromBytes(superClass, null, fileManager.output.get(0).outputStream.toByteArray());
        }
        throw new IllegalArgumentException("Compilation yielded an unexpected number of classes: " + classCount);
    }

    /**
     * Cleans a raw classpath string by:
     * 1. Removing any leading/trailing single quotes.
     * 2. Stripping out “jar:file:///” and “file:/” URI prefixes.
     * 3. Removing “!/” suffixes from JAR‐in‐WAR entries.
     * 4. Eliminating any literal newline characters.
     *
     * After this, each entry is a plain filesystem path, separated by File.pathSeparator.
     *
     * @param rawClasspath the unprocessed classpath (e.g. containing "jar:file://…!/:" or "file:/…" fragments)
     * @return a cleaned classpath suitable for passing to JavaCompiler or ClassLoader
     */
    public static String fixClasspath(String rawClasspath) {
        if (rawClasspath == null) {
            return null;
        }

        String cp = rawClasspath;

        // 1) Remove leading and trailing single quotes, if present
        if (cp.startsWith("'") && cp.endsWith("'") && cp.length() > 1) {
            cp = cp.substring(1, cp.length() - 1);
        }

        // 2) Remove any newline or carriage‐return characters
        //    (we only want one long line with proper path separators)
        cp = cp.replaceAll("[\\r\\n]", "");

        // 3) Remove “jar:file:///” prefixes
        //    We target exactly “jar:file:///” (three slashes) to get "/opt/…"
        cp = cp.replaceAll("jar:file:///", "");

        // 4) Remove “file:/” prefixes
        //    This turns "file:/opt/jetty/…" into "/opt/jetty/…"
        cp = cp.replaceAll("file:/+", "/");

        // 5) Remove the “!/” suffix that often appears in "jar:file://…!/…"
        //    We replace any literal "!/" with an empty string
        cp = cp.replace("!/", "");

        // 6) (Optional) If there are any accidental duplicate path‐separators,
        //    collapse them. For Unix/Linux, File.pathSeparator is ":"
        String sep = File.pathSeparator;
        String doubleSep = sep + sep;
        while (cp.contains(doubleSep)) {
            cp = cp.replace(doubleSep, sep);
        }

        // 7) Trim any accidental whitespace around each entry
        //    Split by the path separator and re‐join, trimming each segment.
        String[] parts = cp.split(sep);
        StringBuilder cleaned = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            if (!part.isEmpty()) {
                cleaned.append(part);
                if (i < parts.length - 1) {
                    cleaned.append(sep);
                }
            }
        }

        return cleaned.toString();
    }


    /**
     * Represents a source file whose contents is loaded from a String
     */
    private static class SourceFile extends SimpleJavaFileObject {
        private final String contents;

        private SourceFile(URI uri, String contents) {
            super(uri, Kind.SOURCE);
            this.contents = contents;
        }

        @Override
        public String getName() {
            return uri.getRawSchemeSpecificPart();
        }

        /**
         * Ignore the file name for public classes
         */
        @Override
        public boolean isNameCompatible(String simpleName, Kind kind) {
            return true;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return contents;
        }
    }

    /**
     * A compiled class file. It's content is stored in a ByteArrayOutputStream.
     */
    private static class ClassFile extends SimpleJavaFileObject {
        private ByteArrayOutputStream outputStream;

        private ClassFile(URI uri) {
            super(uri, Kind.CLASS);
        }

        @Override
        public String getName() {
            return uri.getRawSchemeSpecificPart();
        }

        @Override
        public OutputStream openOutputStream() throws IOException {
            return outputStream = new ByteArrayOutputStream();
        }
    }

    /**
     * A simple file manager that collects written files in memory
     */
    private static class FileManager
            extends ForwardingJavaFileManager<StandardJavaFileManager> {

        private List<ClassFile> output = new ArrayList<>();

        FileManager(StandardJavaFileManager target) {
            super(target);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) {
            final ClassFile file = new ClassFile(toUri(className));
            output.add(file);
            return file;
        }
    }

    private static URI toUri(String path) {
        try {
            return new URI(null, null, path, null);
        } catch (URISyntaxException e) {
            throw new RuntimeException("exception parsing uri", e);
        }
    }
}
