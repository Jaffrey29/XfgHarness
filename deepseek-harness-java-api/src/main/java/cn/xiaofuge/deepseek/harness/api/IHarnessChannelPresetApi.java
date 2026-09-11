package cn.xiaofuge.deepseek.harness.api;

import cn.xiaofuge.deepseek.harness.api.dto.ChannelPresetResponseDTO;
import cn.xiaofuge.deepseek.harness.api.response.Response;
import java.util.List;

/**
 * 渠道模板查询 API。
 * <p>返回内置渠道模板注册表，供前端做渠道选择与快捷填充；数据只读不落库。</p>
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IHarnessChannelPresetApi {

    /**
     * 查询全部渠道模板。
     *
     * @return 模板列表
     */
    Response<List<ChannelPresetResponseDTO>> listPresets();
}
