package cn.xiaofuge.deepseek.harness.trigger.http.query;

import cn.xiaofuge.deepseek.harness.api.IHarnessChannelPresetApi;
import cn.xiaofuge.deepseek.harness.api.dto.ChannelPresetResponseDTO;
import cn.xiaofuge.deepseek.harness.api.response.Response;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 面向 Web 控制台暴露渠道模板查询。
 * <p>前端渲染「渠道模板」下拉：选中模板即可自动填充协议、默认地址与推荐模型。</p>
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@RestController
@RequestMapping("/api/harness/channels")
public class HarnessChannelPresetController {

    private final IHarnessChannelPresetApi channelPresetApi;

    public HarnessChannelPresetController(IHarnessChannelPresetApi channelPresetApi) {
        this.channelPresetApi = channelPresetApi;
    }

    /**
     * 查询全部渠道模板。
     *
     * @return 模板列表
     */
    @GetMapping("/presets")
    public Response<List<ChannelPresetResponseDTO>> listPresets() {
        return channelPresetApi.listPresets();
    }
}
