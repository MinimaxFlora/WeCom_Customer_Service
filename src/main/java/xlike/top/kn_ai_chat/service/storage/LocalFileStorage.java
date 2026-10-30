package xlike.top.kn_ai_chat.service.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
public class LocalFileStorage implements FileStorage {

    private static final Logger logger = LoggerFactory.getLogger(LocalFileStorage.class);
    private final Path root;

    public LocalFileStorage(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public String store(MultipartFile file, String subdir) throws IOException {
        String original = file.getOriginalFilename() == null ? "file.txt" : file.getOriginalFilename();
        String extension = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0 && dot < original.length() - 1) {
            extension = original.substring(dot).toLowerCase();
        }
        Path directory = root.resolve(subdir).normalize();
        if (!directory.startsWith(root)) {
            throw new IOException("非法存储目录");
        }
        Files.createDirectories(directory);
        String filename = UUID.randomUUID() + extension;
        Path target = directory.resolve(filename).normalize();
        if (!target.startsWith(directory)) {
            throw new IOException("非法文件路径");
        }
        file.transferTo(target);
        return root.relativize(target).toString().replace('\\', '/');
    }

    @Override
    public Path resolve(String relativePath) {
        Path path = root.resolve(relativePath).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("非法文件路径");
        }
        return path;
    }

    @Override
    public void delete(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolve(relativePath));
        } catch (Exception e) {
            logger.warn("删除文件失败: {}", relativePath);
        }
    }
}
