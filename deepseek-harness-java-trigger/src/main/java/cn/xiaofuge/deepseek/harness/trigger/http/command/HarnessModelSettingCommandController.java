package cn.xiaofuge.deepseek.harness.trigger.http.command;

import cn.xiaofuge.deepseek.harness.api.dto.ActivateModelSettingRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.SaveModelSettingRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.ModelSettingResponseDTO;
import cn.xiaofuge.deepseek.harness.api.IHarnessModelSettingCommandApi;
import cn.xiaofuge.deepseek.harness.api.response.Response;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 面向 Web 控制台暴露模型设置写操作。 */
@RestController
@RequestMapping("/api/harness/settings/models")
public class HarnessModelSettingCommandController {

    private final IHarnessModelSettingCommandApi harnessModelSettingCommandApi;

    /**
     * 处理Harness 模型 Setting 命令 Controller。
     * 流程：解析请求参数 → 调用应用门面 → 返回响应。
     * @param harnessModelSettingCommandApi harness 模型 Setting 命令 API参数。
     * @return 方法执行结果。
     */
    public HarnessModelSettingCommandController(IHarnessModelSettingCommandApi harnessModelSettingCommandApi) {
        this.harnessModelSettingCommandApi = harnessModelSettingCommandApi;
    }

    @PostMapping
    public Response<ModelSettingResponseDTO> save(@RequestBody SaveModelSettingRequestDTO request) {
        return harnessModelSettingCommandApi.save(request);
    }

    /**
     * 删除目标对象。
     * 流程：解析请求参数 → 调用应用门面 → 返回响应。
     * @param profileCode profile 编码参数。
     * @param modelCode 模型编码（可空；为空删除该 profile 全部配置）。
     */
    @DeleteMapping("/{profileCode}")
    public Response<Void> delete(@PathVariable("profileCode") String profileCode,
                                 @RequestParam(value = "modelCode", required = false) String modelCode) {
        return harnessModelSettingCommandApi.delete(profileCode, modelCode);
    }

    /**
     * 将某 profile 下的指定模型切换为当前生效模型。
     * 流程：解析请求参数 → 调用应用门面 → 返回激活后的配置。
     */
    @PostMapping("/active")
    public Response<ModelSettingResponseDTO> activate(@RequestBody ActivateModelSettingRequestDTO request) {
        return harnessModelSettingCommandApi.activate(request);
    }
}
