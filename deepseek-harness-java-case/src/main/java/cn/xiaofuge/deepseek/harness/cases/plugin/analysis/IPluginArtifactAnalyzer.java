package cn.xiaofuge.deepseek.harness.cases.plugin.analysis;

import cn.xiaofuge.deepseek.harness.api.dto.PluginArtifactCandidateDTO;
import java.nio.file.Path;
import java.util.List;

public interface IPluginArtifactAnalyzer {

    PluginArtifactCandidateDTO analyzeJar(Path jarPath);

    List<PluginArtifactCandidateDTO> analyzeMavenDependency(String pomXml);
}
