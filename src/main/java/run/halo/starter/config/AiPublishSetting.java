package run.halo.starter.config;

import java.util.List;

public record AiPublishSetting(
    Boolean defaultPublish,
    String defaultOwner,
    List<String> defaultCategories,
    List<String> defaultTags,
    Boolean randomCoverEnabled,
    List<String> coverImages
) {

    public static final String GROUP = "publishing";

    public boolean shouldPublishByDefault() {
        return Boolean.TRUE.equals(defaultPublish);
    }

    public String ownerOrDefault() {
        return hasText(defaultOwner) ? defaultOwner.trim() : "admin";
    }

    public List<String> categoriesOrDefault() {
        return defaultCategories == null ? List.of() : defaultCategories;
    }

    public List<String> tagsOrDefault() {
        return defaultTags == null ? List.of() : defaultTags;
    }

    public boolean shouldUseRandomCover() {
        return Boolean.TRUE.equals(randomCoverEnabled);
    }

    public List<String> coverImagesOrDefault() {
        return coverImages == null ? List.of() : coverImages;
    }

    public static AiPublishSetting defaults() {
        return new AiPublishSetting(true, "admin", List.of(), List.of(), false, List.of());
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
