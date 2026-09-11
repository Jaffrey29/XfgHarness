package cn.xiaofuge.deepseek.harness.trigger.service.query;

import cn.xiaofuge.deepseek.harness.api.IHarnessChannelPresetApi;
import cn.xiaofuge.deepseek.harness.api.dto.ChannelPresetResponseDTO;
import cn.xiaofuge.deepseek.harness.api.response.Response;
import cn.xiaofuge.deepseek.harness.cases.runtime.IChannelPresetQueryCase;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 渠道模板查询适配器。
 * <p>流程：委托渠道模板用例读取注册表 → 包装为统一响应；模板只读不落库。</p>
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class HarnessChannelPresetQueryApi implements IHarnessChannelPresetApi {

    private final IChannelPresetQueryCase channelPresetQueryCase;

    public HarnessChannelPresetQueryApi(IChannelPresetQueryCase channelPresetQueryCase) {
        this.channelPresetQueryCase = channelPresetQueryCase;
    }

    @Override
    public Response<List<ChannelPresetResponseDTO>> listPresets() {
        try {
            return Response.success(channelPresetQueryCase.listPresets());
        } catch (RuntimeException exception) {
            return Response.internalError(exception.getMessage());
        }
    }
}
