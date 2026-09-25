package cn.xiaofuge.deepseek.harness.trigger.http.command;

import cn.xiaofuge.deepseek.harness.api.response.Response;
import cn.xiaofuge.deepseek.harness.trigger.service.workspace.WorkspaceRegistryService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Web 端附件上传端点。
 * <p>
 * 背景：桌面端（DSH-Java-Desktop）与 Agent 同机，文件资源以本地绝对路径
 * 注入消息上下文（sandboxRoots 授权）即可被工具读取；Web 端浏览器与
 * 服务不同机，必须先把文件落到服务端工作区，Agent 工具才能访问。
 * <p>
 * 存储位置：{@code <workspace>/.dsh-attachments/<时间戳>-<文件名>}，
 * 工作区目录本身即 Agent 的 cwd / 沙箱根，附件随消息上下文的绝对路径
 * 引用即可被直接读取，无需额外授权。
 * <p>调用流程：Controller → WorkspaceRegistryService 解析工作区 → 落盘 → 返回路径。</p>
 */
@RestController
@RequestMapping("/api/agent")
public class AttachmentUploadController {

    private static final String ATTACHMENT_DIR = ".dsh-attachments";
    private static final long MAX_ATTACHMENT_BYTES = 30L * 1024 * 1024;

    private final WorkspaceRegistryService workspaceRegistryService;

    public AttachmentUploadController(WorkspaceRegistryService workspaceRegistryService) {
        this.workspaceRegistryService = workspaceRegistryService;
    }

    /**
     * 上传附件到指定工作区。
     *
     * @param file      附件文件（multipart）
     * @param workspace 目标工作区名称（附件必须落在某个工作区内，Agent 才能以 cwd 相对/绝对路径读取）
     * @return path=服务端绝对路径；name=原始文件名；size=字节数；mimeType=文件类型
     */
    @PostMapping("/attachments")
    public Response<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("workspace") String workspace
    ) {
        if (file == null || file.isEmpty()) {
            return Response.invalidArgument("附件内容为空");
        }
        if (file.getSize() > MAX_ATTACHMENT_BYTES) {
            return Response.invalidArgument("附件不能超过 30MB");
        }
        String original = file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()
                ? "attachment"
                : file.getOriginalFilename();
        try {
            Path workspaceDir = workspaceRegistryService.resolveWorkspaceDir(workspace);
            Path attachDir = workspaceDir.resolve(ATTACHMENT_DIR);
            Files.createDirectories(attachDir);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS"));
            Path target = attachDir.resolve(timestamp + "-" + sanitizeFileName(original))
                    .toAbsolutePath().normalize();
            file.transferTo(target);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("path", target.toString());
            data.put("name", original);
            data.put("size", file.getSize());
            data.put("mimeType", file.getContentType() == null ? "application/octet-stream" : file.getContentType());
            return Response.success(data);
        } catch (IllegalArgumentException exception) {
            return Response.invalidArgument(exception.getMessage());
        } catch (IOException exception) {
            return Response.internalError("附件保存失败：" + exception.getMessage());
        } catch (Exception exception) {
            return Response.internalError("附件上传失败：" + exception.getMessage());
        }
    }

    /** 文件名清洗：保留常用 Unicode 字符，替换路径分隔符与控制字符。 */
    private static String sanitizeFileName(String name) {
        String cleaned = name.replaceAll("[\\\\/\\x00-\\x1f]", "_").trim();
        if (cleaned.length() > 120) {
            String ext = "";
            int dot = cleaned.lastIndexOf('.');
            if (dot > 0) {
                ext = cleaned.substring(dot).toLowerCase(Locale.ROOT);
            }
            cleaned = cleaned.substring(0, 120 - ext.length()) + ext;
        }
        return cleaned;
    }
}
