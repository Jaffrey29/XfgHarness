package cn.xiaofuge.deepseek.harness.trigger.http.query;

import cn.xiaofuge.deepseek.harness.api.response.runtime.AvailableModelResponse;
import cn.xiaofuge.deepseek.harness.api.service.query.HarnessRuntimeModelQueryApi;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 运行时模型查询控制器。
 * <p>
 * 执行流程：
 * 1. 接收前端的运行时模型查询请求；
 * 2. 调用查询适配器读取当前可用模型；
 * 3. 返回可直接用于模型选择器的列表。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@RestController
@RequestMapping("/api/harness/runtime/models")
public class HarnessRuntimeModelController {

    private final HarnessRuntimeModelQueryApi harnessRuntimeModelQueryApi;

    /**
     * 处理Harness Runtime Model Controller相关逻辑。
     */
    public HarnessRuntimeModelController(HarnessRuntimeModelQueryApi harnessRuntimeModelQueryApi) {
        this.harnessRuntimeModelQueryApi = harnessRuntimeModelQueryApi;
    }

    /**
     * 查询当前可用的运行时模型。
     * <p>
     * 执行流程：
     * 1. 调用查询适配器读取模型列表；
     * 2. 过滤结果由领域层或适配器完成；
     * 3. 返回给前端渲染。
     */
    @GetMapping
    public List<AvailableModelResponse> queryAll() {
        return harnessRuntimeModelQueryApi.queryAll();
    }
}
