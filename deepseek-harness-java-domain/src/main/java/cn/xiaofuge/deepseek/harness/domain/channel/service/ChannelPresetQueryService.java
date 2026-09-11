package cn.xiaofuge.deepseek.harness.domain.channel.service;

import cn.xiaofuge.deepseek.harness.domain.channel.IChannelPresetQueryService;
import cn.xiaofuge.deepseek.harness.domain.channel.model.valobj.ChannelPresetVO;
import cn.xiaofuge.deepseek.harness.domain.channel.model.valobj.ChannelPresets;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 渠道模板查询领域服务。
 * <p>
 * 执行流程：
 * 1. 读取 ChannelPresets 注册表；
 * 2. 逐条映射为 ChannelPresetVO；
 * 3. 返回给用例层。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class ChannelPresetQueryService implements IChannelPresetQueryService {

    @Override
    public List<ChannelPresetVO> listPresets() {
        return ChannelPresets.all().stream()
                .map(p -> new ChannelPresetVO(
                        p.id(),
                        p.displayName(),
                        p.protocol().code(),
                        p.baseUrl(),
                        p.authScheme().name(),
                        p.modelSuggestions(),
                        p.regionGroup()
                ))
                .toList();
    }
}
