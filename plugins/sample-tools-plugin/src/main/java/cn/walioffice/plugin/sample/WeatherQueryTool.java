package cn.walioffice.plugin.sample;

import cn.walioffice.deepseek.harness.domain.tool.model.entity.AbstractTool;
import cn.walioffice.deepseek.harness.domain.tool.model.entity.ToolExecutionResult;
import cn.walioffice.deepseek.harness.domain.tool.model.entity.ToolRunContext;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 天气查询工具（模拟数据）。
 * <p>
 * 继承 {@link AbstractTool}，只需实现 name/description/parameters/run。
 * 演示一个无副作用、可并发调用的只读工具。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class WeatherQueryTool extends AbstractTool {

    @Override
    public String name() {
        return "weather_query";
    }

    @Override
    public String description() {
        return "查询指定城市的天气信息（温度、天气状况、湿度）。";
    }

    @Override
    public Map<String, Object> parameters() {
        return objectSchema()
                .prop("city", stringSchema("城市名称，如 北京、上海、深圳"))
                .required("city")
                .build();
    }

    @Override
    public Map<String, Object> outputSchema() {
        return objectSchema()
                .prop("city", stringSchema(null))
                .prop("temperature", stringSchema(null))
                .prop("condition", stringSchema(null))
                .prop("humidity", stringSchema(null))
                .build();
    }

    @Override
    public boolean isConcurrencySafe(Object args) {
        return true;
    }

    @Override
    protected CompletableFuture<ToolExecutionResult> run(Map<String, Object> args, ToolRunContext ctx) {
        String city = str(args, "city", "未知");
        return ok(mockWeather(city));
    }

    private String mockWeather(String city) {
        int temp = switch (city) {
            case "北京", "Beijing" -> 28;
            case "上海", "Shanghai" -> 31;
            case "深圳", "Shenzhen" -> 33;
            case "广州", "Guangzhou" -> 34;
            default -> 25;
        };
        String condition = temp >= 32 ? "晴" : temp >= 28 ? "多云" : "阴";
        int humidity = temp >= 32 ? 70 : 55;

        return String.format("【%s 天气】温度 %d°C，天气 %s，湿度 %d%%", city, temp, condition, humidity);
    }
}
