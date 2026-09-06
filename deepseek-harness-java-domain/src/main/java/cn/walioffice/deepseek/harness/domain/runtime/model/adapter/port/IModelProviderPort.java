package cn.walioffice.deepseek.harness.domain.runtime.model.adapter.port;

import cn.walioffice.deepseek.harness.domain.runtime.model.model.valobj.ModelProviderProfileVO;

/**
 * 模型提供方端口。
 * <p>
 * 执行流程：
 * 1. 领域层提交执行画像编码；
 * 2. 基础设施层定位具体模型提供方；
 * 3. 返回模型提供方画像。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IModelProviderPort {

    /**
     * 解析执行画像对应的模型提供方画像。
     * <p>
     * 执行流程：
     * 1. 读取 profileCode；
     * 2. 调用具体适配器完成路由；
     * 3. 返回模型提供方画像。
     *
     * @param profileCode 执行画像编码
     * @return 解析后的模型提供方画像
     */
    ModelProviderProfileVO resolve(String profileCode);

}
