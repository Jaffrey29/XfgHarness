package cn.xiaofuge.deepseek.harness.trigger.http.query;

import cn.xiaofuge.deepseek.harness.trigger.service.workspace.WorkspaceRegistryService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工作区受限文件读取端点（Web 端工件预览 / 下载）。
 * <p>
 * 背景：桌面端工件预览直接读本机文件；Web 端浏览器与服务器不同机，
 * 消息中出现的工件文件路径（md / docx / drawio / 图片等）需要一个受控的
 * 读取通道才能预览。
 * <p>
 * 安全边界：仅允许读取 {@code harness.workspace.root} 目录内的文件
 * （见 {@link WorkspaceRegistryService#resolveInsideRoot(String)}），
 * 越出根目录的路径一律拒绝。
 * <p>调用流程：Controller → WorkspaceRegistryService 校验路径 → 读取字节 → 按类型返回。</p>
 */
@RestController
@RequestMapping("/api/agent/workspace")
public class WorkspaceFileQueryController {

    private final WorkspaceRegistryService workspaceRegistryService;

    public WorkspaceFileQueryController(WorkspaceRegistryService workspaceRegistryService) {
        this.workspaceRegistryService = workspaceRegistryService;
    }

    /**
     * 读取工作区内的一个文件，按探测到的媒体类型返回原始字节。
     *
     * @param path 服务端绝对路径（必须位于工作区根目录内）
     * @return 文件字节流；download=1 时附加下载响应头
     */
    @GetMapping("/file")
    public ResponseEntity<ByteArrayResource> file(
            @RequestParam("path") String path,
            @RequestParam(value = "download", required = false) String download
    ) {
        Path target;
        try {
            target = workspaceRegistryService.resolveInsideRoot(path);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ByteArrayResource(("{\"code\":\"A0001\",\"info\":\""
                            + exception.getMessage() + "\"}").getBytes()));
        }
        if (!Files.isRegularFile(target)) {
            return ResponseEntity.notFound().build();
        }
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(target);
        } catch (IOException exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        String mime;
        try {
            mime = Files.probeContentType(target);
        } catch (IOException ignored) {
            mime = null;
        }
        MediaType mediaType = mime == null || mime.isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(mime);
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .contentType(mediaType)
                .header("Cache-Control", "no-store")
                .contentLength(bytes.length);
        if ("1".equals(download)) {
            String fileName = target.getFileName() == null ? "file" : target.getFileName().toString();
            builder.header("Content-Disposition", "attachment; filename*=UTF-8''"
                    + java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8));
        }
        return builder.body(new ByteArrayResource(bytes));
    }
}
