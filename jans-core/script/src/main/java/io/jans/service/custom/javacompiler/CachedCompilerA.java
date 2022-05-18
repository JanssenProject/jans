package io.jans.service.custom.javacompiler;

import net.openhft.compiler.CompilerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static net.openhft.compiler.CompilerUtils.writeBytes;
import static net.openhft.compiler.CompilerUtils.writeText;


public class CachedCompilerA implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(CachedCompilerA.class);
    private static final PrintWriter DEFAULT_WRITER = new PrintWriter(System.err);

    static JavaCompiler s_compiler;
    static StandardJavaFileManager s_standardJavaFileManager;

    public static final CachedCompilerA CACHED_COMPILER;

    static {
        CompilerUtils.addClassPath("WEB-INF/lib");
        CompilerUtils.addClassPath("WEB-INF/classes");

        CACHED_COMPILER = new CachedCompilerA(null, null);
        s_compiler = ToolProvider.getSystemJavaCompiler();
        if (s_compiler == null) {
            try {
                Class<?> javacTool = Class.forName("com.sun.tools.javac.api.JavacTool");
                Method create = javacTool.getMethod("create");
                s_compiler = (JavaCompiler) create.invoke(null);
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }
    }

    private final Map<ClassLoader, Map<String, Class<?>>> loadedClassesMap = Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<ClassLoader, MyJavaFileManager> fileManagerMap = Collections.synchronizedMap(new WeakHashMap<>());

    private final File sourceDir;
    private final File classDir;

    private final ConcurrentMap<String, JavaFileObject> javaFileObjects = new ConcurrentHashMap<>();

    public CachedCompilerA(File sourceDir, File classDir) {
        this.sourceDir = sourceDir;
        this.classDir = classDir;
    }

    public void close() {
        try {
            for (MyJavaFileManager fileManager : fileManagerMap.values()) {
                fileManager.close();
            }
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public Class loadFromJava(String className, String javaCode) throws ClassNotFoundException {
        return loadFromJava(getClass().getClassLoader(), className, javaCode, DEFAULT_WRITER);
    }

    public Class loadFromJava(ClassLoader classLoader,
                              String className,
                              String javaCode) throws ClassNotFoundException {
        return loadFromJava(classLoader, className, javaCode, DEFAULT_WRITER);
    }

    public Map<String, byte[]> compileFromJava(String className, String javaCode, MyJavaFileManager fileManager) {
        return compileFromJava(className, javaCode, DEFAULT_WRITER, fileManager);
    }


    public Map<String, byte[]> compileFromJava(String className,
                                               String javaCode,
                                               final PrintWriter writer,
                                               MyJavaFileManager fileManager) {
        Iterable<? extends JavaFileObject> compilationUnits;
        if (sourceDir != null) {
            String filename = className.replaceAll("\\.", '\\' + File.separator) + ".java";
            File file = new File(sourceDir, filename);
            writeText(file, javaCode);
            if (s_standardJavaFileManager == null)
                s_standardJavaFileManager = s_compiler.getStandardFileManager(null, null, null);
            compilationUnits = s_standardJavaFileManager.getJavaFileObjects(file);

        } else {
            javaFileObjects.put(className, new JavaSourceFromString(className, javaCode));
            compilationUnits = new ArrayList<>(javaFileObjects.values()); // To prevent CME from compiler code
        }
        // reuse the same file manager to allow caching of jar files
        List<String> options = Arrays.asList("-g", "-nowarn");
        boolean ok = s_compiler.getTask(writer, fileManager, new DiagnosticListener<JavaFileObject>() {
            @Override
            public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
                if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                    writer.println(diagnostic);
                }
                LOG.info("diagnostic: " + diagnostic);
            }
        }, options, null, compilationUnits).call();

        LOG.info("Is compiled: " + ok);

        if (!ok) {
            // compilation error, so we want to exclude this file from future compilation passes
            if (sourceDir == null)
                javaFileObjects.remove(className);

            // nothing to return due to compiler error
            return Collections.emptyMap();
        } else {
            Map<String, byte[]> result = fileManager.getAllBuffers();

            return result;
        }
    }

    public Class loadFromJava(ClassLoader classLoader,
                              String className,
                              String javaCode,
                              PrintWriter writer) throws ClassNotFoundException {
        Class<?> clazz = null;
        Map<String, Class<?>> loadedClasses;
        synchronized (loadedClassesMap) {
            loadedClasses = loadedClassesMap.get(classLoader);
            if (loadedClasses == null)
                loadedClassesMap.put(classLoader, loadedClasses = new LinkedHashMap<>());
            else
                clazz = loadedClasses.get(className);
        }
        PrintWriter printWriter = (writer == null ? DEFAULT_WRITER : writer);
        if (clazz != null)
            return clazz;

        MyJavaFileManager fileManager = fileManagerMap.get(classLoader);
        if (fileManager == null) {
            StandardJavaFileManager standardJavaFileManager = s_compiler.getStandardFileManager(null, null, null);
            fileManagerMap.put(classLoader, fileManager = new MyJavaFileManager(standardJavaFileManager));
        }
        final Map<String, byte[]> compiled = compileFromJava(className, javaCode, printWriter, fileManager);
        LOG.info("COMPILED: " + compiled.size());
        for (Map.Entry<String, byte[]> entry : compiled.entrySet()) {
            String className2 = entry.getKey();
            synchronized (loadedClassesMap) {
                if (loadedClasses.containsKey(className2))
                    continue;
            }
            byte[] bytes = entry.getValue();
            if (classDir != null) {
                String filename = className2.replaceAll("\\.", '\\' + File.separator) + ".class";
                boolean changed = writeBytes(new File(classDir, filename), bytes);
                if (changed) {
                    LOG.info("Updated {} in {}", className2, classDir);
                }
            }

            synchronized (className2.intern()) { // To prevent duplicate class definition error
                synchronized (loadedClassesMap) {
                    if (loadedClasses.containsKey(className2))
                        continue;
                }

                Class<?> clazz2 = CompilerUtils.defineClass(classLoader, className2, bytes);
                LOG.info("DEFINED: " + clazz2);
                synchronized (loadedClassesMap) {
                    loadedClasses.put(className2, clazz2);

                    LOG.info("LOADED loadedClassesMap: " + loadedClassesMap);
                }
            }
        }
        synchronized (loadedClassesMap) {
            loadedClasses.put(className, clazz = classLoader.loadClass(className));
        }
        return clazz;
    }
}
