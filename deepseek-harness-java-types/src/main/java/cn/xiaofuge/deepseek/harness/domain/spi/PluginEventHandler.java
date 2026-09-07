package cn.xiaofuge.deepseek.harness.domain.spi;

/**
 * 匹配事件发布到插件总线时执行的回调。
 * <p>处理器应快速返回且不阻塞调用线程；耗时工作应由实现方转交线程池或队列处理。
 */
@FunctionalInterface
public interface PluginEventHandler {

    /**
     * 处理一个插件事件。
     *
     * @param event 不可变事件信封
     */
    void handle(PluginEvent event);
}
