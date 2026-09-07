package cn.xiaofuge.deepseek.harness.domain.spi;

public enum PluginLifecycleState {
    PENDING,
    LOADING,
    ACTIVE,
    UNLOADING,
    DISPOSED,
    FAILED
}
