package io.github.nhtuan10.modular.impl.util;

import org.codehaus.plexus.util.DirectoryScanner;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Utils {
    public static List<String> listFiles(URI uri) {
        DirectoryScanner scanner = new DirectoryScanner();
        Path path = Paths.get(uri);
        Path root = path.getRoot();
        scanner.setBasedir(root.toString());
        scanner.setIncludes(new String[]{root.relativize(path).toString()});
        scanner.scan();
        return Arrays.stream(scanner.getIncludedFiles()).map(s -> root.resolve(s).toString()).collect(Collectors.toList());
    }

    public static void extractCompressedFile(Path warFile, Path targetDir) throws IOException {
        try (InputStream is = Files.newInputStream(warFile)) {
            extractCompressedFile(is, targetDir);
        }
    }

    public static void extractCompressedFile(InputStream warFile, Path targetDir) {
        File targetDirectory = targetDir.toFile();
        if (!targetDirectory.exists()) {
            targetDirectory.mkdirs();
        }

        try (ZipInputStream zipInputStream = new ZipInputStream(warFile)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                File file = new File(targetDirectory, entry.getName());

                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    file.getParentFile().mkdirs();

                    try (BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file))) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = zipInputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    }
                }
                zipInputStream.closeEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to extract WAR file", e);
        }
    }
}
