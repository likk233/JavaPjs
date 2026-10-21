package com.example.util;

import org.apache.ibatis.io.DefaultVFS;
import java.io.IOException;
import java.net.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/** 外部卷会生成 ._*.class；扫描接口时跳过全部隐藏路径，不读取或删除其内容。 */
public final class VisibleFilesVfs extends DefaultVFS {
    @Override
    public List<String> list(URL url, String path) throws IOException {
        if (hidden(path)) return List.of();
        if ("file".equals(url.getProtocol())) {
            final Path root;
            try { root = Path.of(url.toURI()); }
            catch (URISyntaxException e) { throw new IOException("无法解析类路径", e); }
            if (Files.isDirectory(root)) {
                List<String> files = new ArrayList<>();
                Files.walkFileTree(root, new SimpleFileVisitor<>() {
                    @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        return !dir.equals(root) && dir.getFileName().toString().startsWith(".")
                                ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
                    }
                    @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (!file.getFileName().toString().startsWith(".") && attrs.isRegularFile())
                            files.add(path + "/" + root.relativize(file).toString().replace('\\', '/'));
                        return FileVisitResult.CONTINUE;
                    }
                });
                return files;
            }
        }
        return super.list(url, path).stream().filter(name -> !hidden(name)).toList();
    }

    private static boolean hidden(String path) {
        return Arrays.stream(path.split("/")).anyMatch(part -> part.startsWith("."));
    }
}
