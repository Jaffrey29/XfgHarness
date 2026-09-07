package cn.xiaofuge.deepseek.harness.domain.storage.model.valobj;

/**
 * StorageException 定义当前模块中的class契约与职责。
 */
public class StorageException extends RuntimeException {

    private final StorageErrorCode code;

    /**
     * 处理Storage Exception相关逻辑。
     */
    public StorageException(StorageErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 处理Storage Exception相关逻辑。
     */
    public StorageException(StorageErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /**
     * 处理code相关逻辑。
     */
    public StorageErrorCode code() {
        return code;
    }
}
