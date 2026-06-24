package run.halo.starter.model;

public record AiArticlePushResponse(
    boolean success,
    String message,
    String articleName,
    String snapshotName,
    String status,
    String articleUrl
) {

    public static AiArticlePushResponse accepted(String articleName, String snapshotName, String status,
        String articleUrl) {
        return new AiArticlePushResponse(true, "Article published to Halo.", articleName, snapshotName, status,
            articleUrl);
    }

    public static AiArticlePushResponse failed(String message) {
        return new AiArticlePushResponse(false, message, null, null, null, null);
    }
}
