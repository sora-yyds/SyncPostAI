package run.halo.starter.config;

public record AiSecuritySetting(
    Boolean enabled,
    String pushToken
) {

    public static final String GROUP = "security";

    public boolean enabledOrDefault() {
        return enabled == null || enabled;
    }

    public String tokenOrBlank() {
        return pushToken == null ? "" : pushToken.trim();
    }

    public static AiSecuritySetting defaults() {
        return new AiSecuritySetting(true, "");
    }
}
