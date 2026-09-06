package cn.walioffice.deepseek.harness.domain.e2b.adapter.port;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

/**
 * Port for E2B filesystem operations within a sandbox.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IE2BFsPort {

    /**
     * Read a file from the sandbox.
     *
     * @param sandboxId sandbox identifier
     * @param path      file path in the sandbox
     * @return file content as string
     */
    String readFile(String sandboxId, String path);

    /**
     * Write a file to the sandbox.
     *
     * @param sandboxId sandbox identifier
     * @param path      file path in the sandbox
     * @param content   file content
     */
    void writeFile(String sandboxId, String path, String content);

    /**
     * List files in a directory.
     *
     * @param sandboxId sandbox identifier
     * @param dir       directory path
     * @return list of file names
     */
    List<String> listDir(String sandboxId, String dir);

    /**
     * Delete a file or directory.
     *
     * @param sandboxId sandbox identifier
     * @param path      path to delete
     */
    void delete(String sandboxId, String path);
}
