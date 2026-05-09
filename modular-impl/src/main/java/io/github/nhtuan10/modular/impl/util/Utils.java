package io.github.nhtuan10.modular.impl.util;

import org.codehaus.plexus.util.DirectoryScanner;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
}
