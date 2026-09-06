package cn.xiaofuge.deepseek.harness.domain.typert.adapter.port;

/**
 * Minimal Typert runtime consumed through dependency inversion.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface ITypertRegistryContract {

    ITypertLocalRegistry local();

    ITypertRemoteRegistry remotes();

    ITypertLookupRegistry lookups();

    ITypertContextRegistry contexts();
}
