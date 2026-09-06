package cn.walioffice.deepseek.harness.infrastructure.adapter.skill;

import cn.walioffice.deepseek.harness.domain.skill.adapter.port.ISkillProviderPort;
import cn.walioffice.deepseek.harness.domain.skill.model.entity.SkillCandidate;
import cn.walioffice.deepseek.harness.domain.skill.model.entity.SkillDefinition;
import cn.walioffice.deepseek.harness.domain.skill.model.valobj.SkillInvocationPolicy;
import cn.walioffice.deepseek.harness.domain.skill.model.valobj.SkillSource;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Local filesystem skill provider. Discovers directory-bundle and flat Markdown
 * skills from project, custom, and user roots, parses YAML frontmatter.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class FilesystemSkillProviderPort implements ISkillProviderPort {

    private static final int PROJECT_DSH_RANK = 100;
    private static final int USER_DSH_RANK = 400;
    private static final int BUNDLED_RANK = 600;
    private static final Pattern FRONTMATTER = Pattern.compile("^---\\s*\\n(.*?)\\n---\\s*\\n(.*)$", Pattern.DOTALL);

    private final Path dshHome;
    private final Path bundledDir;
    private final String providerName;

    public FilesystemSkillProviderPort(Path dshHome, Path bundledDir) {
        this(dshHome, bundledDir, "filesystem");
    }

    public FilesystemSkillProviderPort(Path dshHome, Path bundledDir, String providerName) {
        this.dshHome = dshHome;
        this.bundledDir = bundledDir;
        this.providerName = providerName;
    }

    @Override
    public List<SkillCandidate> discover(String cwd) {
        List<SkillCandidate> candidates = new ArrayList<>();
        // Project skills: <cwd>/.dsh/skills
        if (cwd != null) {
            Path projectSkills = Path.of(cwd).resolve(".dsh").resolve("skills");
            candidates.addAll(discoverRoot(projectSkills, SkillSource.PROJECT_DSH, PROJECT_DSH_RANK));
        }
        // User skills: <dshHome>/skills
        candidates.addAll(discoverRoot(dshHome.resolve("skills"), SkillSource.USER_DSH, USER_DSH_RANK));
        // Bundled skills
        if (bundledDir != null && Files.isDirectory(bundledDir)) {
            candidates.addAll(discoverRoot(bundledDir, SkillSource.BUNDLED, BUNDLED_RANK));
        }
        return candidates;
    }

    @Override
    public Optional<SkillDefinition> load(SkillCandidate candidate) {
        if (candidate.path().isEmpty()) return Optional.empty();
        Path skillFile = Path.of(candidate.path().get());
        try {
            String raw = Files.readString(skillFile);
            ParsedSkill parsed = parseSkill(raw);
            return Optional.of(new SkillDefinition(
                    candidate.name(),
                    parsed.description,
                    Optional.ofNullable(parsed.whenToUse),
                    candidate.invocation(),
                    candidate.source(),
                    candidate.provider(),
                    parsed.content,
                    candidate.path()
            ));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private List<SkillCandidate> discoverRoot(Path root, SkillSource source, int rank) {
        if (!Files.isDirectory(root)) return List.of();
        List<SkillCandidate> result = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
            for (Path entry : stream) {
                String name = entry.getFileName().toString();
                // Directory bundle: <name>/SKILL.md
                if (Files.isDirectory(entry)) {
                    Path skillFile = entry.resolve("SKILL.md");
                    if (Files.isRegularFile(skillFile)) {
                        addCandidate(result, name, skillFile, source, rank);
                    }
                } else if (name.endsWith(".md")) {
                    // Flat markdown: <name>.md
                    String skillName = name.substring(0, name.length() - 3);
                    addCandidate(result, skillName, entry, source, rank);
                }
            }
        } catch (IOException e) {
            // non-fatal
        }
        return result;
    }

    private void addCandidate(List<SkillCandidate> result, String name, Path file,
                              SkillSource source, int rank) {
        try {
            String raw = Files.readString(file);
            ParsedSkill parsed = parseSkill(raw);
            result.add(new SkillCandidate(
                    name, parsed.description, Optional.ofNullable(parsed.whenToUse),
                    SkillInvocationPolicy.both(), source, providerName,
                    Optional.of(file.toAbsolutePath().toString()), rank,
                    file.toAbsolutePath().toString()
            ));
        } catch (IOException e) {
            // skip unreadable
        }
    }

    private ParsedSkill parseSkill(String raw) {
        String description = "";
        String whenToUse = null;
        String content = raw;
        Matcher m = FRONTMATTER.matcher(raw);
        if (m.matches()) {
            String frontmatter = m.group(1);
            content = m.group(2);
            for (String line : frontmatter.split("\n")) {
                int colon = line.indexOf(':');
                if (colon <= 0) continue;
                String key = line.substring(0, colon).trim();
                String val = line.substring(colon + 1).trim();
                switch (key) {
                    case "description" -> description = val;
                    case "when_to_use" -> whenToUse = val;
                }
            }
        }
        // If no frontmatter, use first line as description
        if (description.isEmpty()) {
            String firstLine = raw.lines().findFirst().orElse("");
            if (firstLine.startsWith("#")) {
                description = firstLine.substring(1).trim();
            } else {
                description = firstLine;
            }
        }
        return new ParsedSkill(description, whenToUse, content);
    }

    private record ParsedSkill(String description, String whenToUse, String content) {}
}
