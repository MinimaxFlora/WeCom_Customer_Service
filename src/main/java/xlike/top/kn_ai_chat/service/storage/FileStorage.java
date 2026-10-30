package xlike.top.kn_ai_chat.service.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

public interface FileStorage {

    /**
     * 保存文件，返回相对路径（存数据库用）。
     */
    String store(MultipartFile file, String subdir) throws IOException;

    Path resolve(String relativePath);

    void delete(String relativePath);
}
