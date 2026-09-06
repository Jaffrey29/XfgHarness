package cn.walioffice.deepseek.harness.trigger.http.query;

import cn.walioffice.deepseek.harness.api.response.settings.ModelSettingResponse;
import cn.walioffice.deepseek.harness.api.service.query.HarnessModelSettingQueryApi;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模型设置查询控制器。
 * <p>
 * 执行流程：
 * 1. 接收前端模型设置查询请求；
 * 2. 调用查询适配器读取持久化模型配置；
 * 3. 直接返回模型设置列表。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@RestController
@RequestMapping("/api/harness/settings/models")
public class HarnessModelSettingQueryController {

    private final HarnessModelSettingQueryApi harnessModelSettingQueryApi;

    public HarnessModelSettingQueryController(HarnessModelSettingQueryApi harnessModelSettingQueryApi) {
        this.harnessModelSettingQueryApi = harnessModelSettingQueryApi;
    }

    /**
     * 查询全部模型设置。
     * <p>
     * 执行流程：
     * 1. 调用查询适配器读取全部设置；
     * 2. 组装响应对象；
     * 3. 返回给前端配置页。
     */
    @GetMapping
    public List<ModelSettingResponse> queryAll() {
        return harnessModelSettingQueryApi.queryAll();
    }
}
