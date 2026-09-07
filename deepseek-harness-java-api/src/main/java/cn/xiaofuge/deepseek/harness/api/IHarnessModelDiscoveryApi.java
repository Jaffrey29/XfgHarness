package cn.xiaofuge.deepseek.harness.api;

import cn.xiaofuge.deepseek.harness.api.dto.DiscoverModelsRequestDTO;
import cn.xiaofuge.deepseek.harness.api.response.Response;
import java.util.List;

public interface IHarnessModelDiscoveryApi {

    Response<List<String>> discover(DiscoverModelsRequestDTO request);
}
