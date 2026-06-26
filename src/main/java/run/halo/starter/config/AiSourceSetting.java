package run.halo.starter.config;

import java.util.Map;
import org.springframework.util.StringUtils;

public record AiSourceSetting(
    Boolean enabled,
    Boolean astrbotPulseEnabled,
    Object astrbotPulseSecret,
    Boolean astrbotPulseDefaultPublish,
    Boolean n8nEnabled,
    Object n8nSecret,
    Boolean n8nDefaultPublish,
    Boolean difyEnabled,
    Object difySecret,
    Boolean difyDefaultPublish,
    Boolean cozeEnabled,
    Object cozeSecret,
    Boolean cozeDefaultPublish,
    Boolean githubActionsEnabled,
    Object githubActionsSecret,
    Boolean githubActionsDefaultPublish
) {

    public static final String GROUP = "sources";

    public static AiSourceSetting defaults() {
        return new AiSourceSetting(true, false, "", false, false, "", false, false, "", false, false, "", false,
            false, "", false);
    }

    public boolean enabledOrDefault() {
        return enabled == null || enabled;
    }

    public SourceProfile profile(String source) {
        return switch (normalizeSource(source)) {
            case "astrbot_plugin_pulse" -> new SourceProfile("astrbot_plugin_pulse", enabled(astrbotPulseEnabled),
                secretNameOrBlank(astrbotPulseSecret), publishOrDefault(astrbotPulseDefaultPublish));
            case "n8n" -> new SourceProfile("n8n", enabled(n8nEnabled), secretNameOrBlank(n8nSecret),
                publishOrDefault(n8nDefaultPublish));
            case "dify" -> new SourceProfile("dify", enabled(difyEnabled), secretNameOrBlank(difySecret),
                publishOrDefault(difyDefaultPublish));
            case "coze" -> new SourceProfile("coze", enabled(cozeEnabled), secretNameOrBlank(cozeSecret),
                publishOrDefault(cozeDefaultPublish));
            case "github-actions" -> new SourceProfile("github-actions", enabled(githubActionsEnabled),
                secretNameOrBlank(githubActionsSecret), publishOrDefault(githubActionsDefaultPublish));
            default -> SourceProfile.unknown(normalizeSource(source));
        };
    }

    public static String normalizeSource(String source) {
        if (!StringUtils.hasText(source)) {
            return "external";
        }
        var normalized = source.trim().toLowerCase()
            .replace("_", "-")
            .replace(" ", "-");
        if (normalized.contains("astrbot") || normalized.contains("pulse")) {
            return "astrbot_plugin_pulse";
        }
        if (normalized.contains("github")) {
            return "github-actions";
        }
        if (normalized.contains("dify")) {
            return "dify";
        }
        if (normalized.contains("coze")) {
            return "coze";
        }
        if (normalized.contains("n8n")) {
            return "n8n";
        }
        return normalized;
    }

    private static boolean enabledOrDefault(Boolean enabled) {
        return enabled == null || enabled;
    }

    private static boolean enabled(Boolean enabled) {
        return Boolean.TRUE.equals(enabled);
    }

    private static boolean publishOrDefault(Boolean publish) {
        return Boolean.TRUE.equals(publish);
    }

    @SuppressWarnings("unchecked")
    private static String secretNameOrBlank(Object secret) {
        if (secret == null) {
            return "";
        }
        if (secret instanceof String secretName) {
            return secretName.trim();
        }
        if (secret instanceof Map<?, ?> secretMap) {
            var directName = firstTextValue(secretMap, "name", "secretName", "value");
            if (StringUtils.hasText(directName)) {
                return directName;
            }
            var metadata = secretMap.get("metadata");
            if (metadata instanceof Map<?, ?> metadataMap) {
                return firstTextValue((Map<Object, Object>) metadataMap, "name");
            }
        }
        return "";
    }

    private static String firstTextValue(Map<?, ?> values, String... keys) {
        for (var key : keys) {
            var value = values.get(key);
            if (value instanceof String text && StringUtils.hasText(text)) {
                return text.trim();
            }
        }
        return "";
    }

    public record SourceProfile(
        String key,
        boolean known,
        boolean enabled,
        String secretName,
        boolean defaultPublish
    ) {
        static SourceProfile unknown(String key) {
            return new SourceProfile(key, false, false, "", false);
        }

        SourceProfile(String key, boolean enabled, String secretName, boolean defaultPublish) {
            this(key, true, enabled, secretName, defaultPublish);
        }

        public boolean hasSecret() {
            return StringUtils.hasText(secretName);
        }
    }
}
