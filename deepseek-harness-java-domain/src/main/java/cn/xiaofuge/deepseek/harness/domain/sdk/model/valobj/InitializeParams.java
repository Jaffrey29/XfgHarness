package cn.xiaofuge.deepseek.harness.domain.sdk.model.valobj;

/**
 * Parameters for the process-wide SDK handshake.
 *
 * @param cwd        working directory recorded on every SDK-created session's header
 * @param provider   provider route every SDK-created agent runs on
 * @param model      model name every SDK-created agent runs on
 * @param maxTokens  optional positive output-token cap
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record InitializeParams(String cwd, String provider, String model, Integer maxTokens) {}
