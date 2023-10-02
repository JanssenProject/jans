package io.jans.casa.core;

import org.slf4j.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * @author jgomer
 */
@ApplicationScoped
public class ResourceExtractor {

    @Inject
    private Logger logger;

    private void recursiveCopy(Path source, Path target) throws IOException {

        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {

                logger.trace("Copying directory {}", dir.toString());
                Path targetdir = target.resolve(source.relativize(dir));

                Files.copy(dir, targetdir, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;

            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

                logger.trace("Copying file {}", file.toString());
                Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;

            }
        });

    }

    public void recursiveDelete(Path start) throws IOException {

        logger.debug("Removing directory {}", start);
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
                if (e == null) {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                } else {
                    // directory iteration failed
                    throw e;
                }
            }
        });

    }

    private void prepareDirectory(Path destinationPath) throws IOException {

        //Flush destination path if already exists
        if (Files.isDirectory(destinationPath)) {
            recursiveDelete(destinationPath);
        }
        Files.createDirectory(destinationPath);

    }

    public void createDirectory(Path sourcePath, Path destinationPath) throws IOException {
        prepareDirectory(destinationPath);
        recursiveCopy(sourcePath, destinationPath);
    }

    public void createDirectory(JarInputStream inStream, String pattern, Path destinationPath) throws IOException {

        String destination = destinationPath.toString();
        prepareDirectory(destinationPath);

        JarEntry entry = inStream.getNextJarEntry();
        for (; entry != null; entry = inStream.getNextJarEntry()) {

            if (entry.getName().startsWith(pattern)) {

                String entryName = entry.getName();
                logger.trace("Extracting {}", entryName);
                Path path = Paths.get(destination, entryName.substring(pattern.length()).split("/"));

                if (entry.isDirectory()) {
                    path.toFile().mkdirs();
                } else {
                    try (OutputStream outStream = new BufferedOutputStream(new FileOutputStream(path.toString()))) {

                        byte[] buffer = new byte[4096];

                        int read = inStream.read(buffer, 0, buffer.length);
                        while (read > 0) {
                            outStream.write(buffer, 0, read);
                            read = inStream.read(buffer, 0, buffer.length);
                        }
                    }
                }
            }
        }

    }

}
