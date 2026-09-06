package cn.xiaofuge.deepseek.harness.domain.runtime.setting;

import cn.xiaofuge.deepseek.harness.domain.runtime.setting.model.valobj.ModelSettingVO;
import java.util.List;
import java.util.Optional;

/**
 * 模型设置查询服务接口。
 * <p>
 * 执行流程：
 * 1. 上层请求模型配置；
 * 2. 领域层从仓储中装配视图对象；
 * 3. 返回给运行时路由或前端展示。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IModelSettingQueryService {

    /**
     * 查询全部模型设置。
     * <p>
     * 执行流程：
     * 1. 从仓储读取全部设置；
     * 2. 转成领域视图对象；
     * 3. 返回列表。
     */
    List<ModelSettingVO> queryAll();

    /**
     * 按画像编码查询模型设置。
     * <p>
     * 执行流程：
     * 1. 读取 profileCode；
     * 2. 定位对应设置；
     * 3. 返回可选值对象。
     */
    Optional<ModelSettingVO> findByProfileCode(String profileCode);
}
