package cn.walioffice.deepseek.harness.domain.runtime;

import cn.walioffice.deepseek.harness.domain.runtime.profile.model.valobj.ExecutionProfileVO;

/**
 * 执行画像查询服务。
 * <p>
 * 执行流程：
 * 1. 接收执行画像编码；
 * 2. 解析对应执行模式；
 * 3. 返回供后续调度使用的执行画像。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IExecutionProfileService {

    /**
     * 解析一个执行画像。
     * <p>
     * 执行流程：
     * 1. 读取 profileCode；
     * 2. 定位对应策略；
     * 3. 返回解析后的执行画像。
     *
     * @param profileCode 执行画像编码
     * @return 解析后的执行画像
     */
    ExecutionProfileVO resolve(String profileCode);
}
