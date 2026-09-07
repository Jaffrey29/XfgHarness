package cn.xiaofuge.deepseek.harness.trigger.service.command;

import cn.xiaofuge.deepseek.harness.api.dto.SaveModelSettingRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.ModelSettingResponseDTO;
import cn.xiaofuge.deepseek.harness.api.IHarnessModelSettingCommandApi;
import cn.xiaofuge.deepseek.harness.api.response.Response;
import cn.xiaofuge.deepseek.harness.cases.runtime.IModelSettingCommandCase;
import cn.xiaofuge.deepseek.harness.api.dto.SaveModelSettingRequestDTO;
import org.springframework.stereotype.Service;

@Service
public class HarnessModelSettingCommandApi implements IHarnessModelSettingCommandApi {
    private final IModelSettingCommandCase modelSettingCommandCase;

    public HarnessModelSettingCommandApi(IModelSettingCommandCase modelSettingCommandCase) {
        this.modelSettingCommandCase = modelSettingCommandCase;
    }

    public Response<ModelSettingResponseDTO> save(SaveModelSettingRequestDTO request) {
        return execute(() -> {
            var setting = modelSettingCommandCase.save(new SaveModelSettingRequestDTO(
                    request.profileCode(), request.providerCode(), request.modelCode(),
                    request.baseUrl(), request.apiKeyRef(), request.enabled()
            ));
            return new ModelSettingResponseDTO(
                    setting.profileCode(), setting.providerCode(), setting.modelCode(),
                    setting.baseUrl(), setting.apiKeyRef(), setting.enabled(), setting.updatedAt()
            );
        });
    }

    public Response<Void> delete(String profileCode) {
        return execute(() -> {
            modelSettingCommandCase.delete(profileCode);
            return null;
        });
    }

    private <T> Response<T> execute(java.util.function.Supplier<T> action) {
        try {
            return Response.success(action.get());
        } catch (IllegalArgumentException exception) {
            return Response.invalidArgument(exception.getMessage());
        } catch (RuntimeException exception) {
            return Response.internalError(exception.getMessage());
        }
    }
}
