package cn.xiaofuge.deepseek.harness.trigger.service.command;

import cn.xiaofuge.deepseek.harness.api.dto.ActivateModelSettingRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.DeleteModelSettingRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.ModelSettingResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.SaveModelSettingRequestDTO;
import cn.xiaofuge.deepseek.harness.api.IHarnessModelSettingCommandApi;
import cn.xiaofuge.deepseek.harness.api.response.Response;
import cn.xiaofuge.deepseek.harness.cases.runtime.IModelSettingCommandCase;
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
                    request.baseUrl(), request.apiKeyRef(), request.protocol(), request.enabled()
            ));
            return toResponse(setting);
        });
    }

    public Response<Void> delete(String profileCode, String modelCode) {
        return execute(() -> {
            modelSettingCommandCase.delete(new DeleteModelSettingRequestDTO(profileCode, modelCode));
            return null;
        });
    }

    public Response<ModelSettingResponseDTO> activate(ActivateModelSettingRequestDTO request) {
        return execute(() -> {
            var setting = modelSettingCommandCase.activate(request);
            return toResponse(setting);
        });
    }

    private ModelSettingResponseDTO toResponse(ModelSettingResponseDTO setting) {
        return new ModelSettingResponseDTO(
                setting.profileCode(), setting.providerCode(), setting.modelCode(),
                setting.baseUrl(), setting.apiKeyRef(), setting.protocol(),
                setting.enabled(), setting.active(), setting.updatedAt()
        );
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
