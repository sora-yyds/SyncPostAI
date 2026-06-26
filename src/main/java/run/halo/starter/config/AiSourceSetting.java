package run.halo.starter.config;

import org.springframework.util.StringUtils;

public record AiSourceSetting(
    Boolean enabled,
    Boolean astrbotPulseEnabled,
    String astrbotPulseToken,
    Boolean astrbotPulseDefaultPublish,
    Boolean n8nEnabled,
    String n8nToken,
    Boolean n8nDefaultPublish,
    Boolean difyEnabled,
    String difyToken,
    Boolean difyDefaultPublish,
    Boolean cozeEnabled,
    String cozeToken,
    Boolean cozeDefaultPublish,
    Boolean githubActionsEnabled,
    String githubActionsToken,
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
                tokenOrBlank(astrbotPulseToken), publishOrDefault(astrbotPulseDefaultPublish));
            case "n8n" -> new SourceProfile("n8n", enabled(n8nEnabled), tokenOrBlank(n8nToken),
                publishOrDefault(n8nDefaultPublish));
            case "dify" -> new SourceProfile("dify", enabled(difyEnabled), tokenOrBlank(difyToken),
                publishOrDefault(difyDefaultPublish));
            case "coze" -> new SourceProfile("coze", enabled(cozeEnabled), tokenOrBlank(cozeToken),
                publishOrDefault(cozeDefaultPublish));
            case "github-actions" -> new SourceProfile("github-actions", enabled(githubActionsEnabled),
                tokenOrBlank(githubActionsToken), publishOrDefault(githubActionsDefaultPublish));
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

    private static String tokenOrBlank(String token) {
        return token == null ? "" : token.trim();
    }

    public record SourceProfile(
        String key,
        boolean known,
        boolean enabled,
        String token,
        boolean defaultPublish
    ) {
        static SourceProfile unknown(String key) {
            return new SourceProfile(key, false, false, "", false);
        }

        SourceProfile(String key, boolean enabled, String token, boolean defaultPublish) {
            this(key, true, enabled, token, defaultPublish);
        }

        public boolean hasToken() {
            return StringUtils.hasText(token);
        }
    }
}
