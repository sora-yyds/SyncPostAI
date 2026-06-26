package run.halo.starter.model;

public record AiArticlePushResponse(
    boolean success,
    String message,
    String articleName,
    String snapshotName,
    String status,
    String articleUrl,
    String reviewName
) {

    public static AiArticlePushResponse accepted(String articleName, String snapshotName, String status,
        String articleUrl) {
        return new AiArticlePushResponse(true, "Article published to Halo.", articleName, snapshotName, status,
            articleUrl, null);
    }

    public static AiArticlePushResponse queued(String reviewName) {
        return new AiArticlePushResponse(true, "Article saved to review queue.", null, null, "pending_review",
            null, reviewName);
    }

    public static AiArticlePushResponse failed(String message) {
        return new AiArticlePushResponse(false, message, null, null, null, null, null);
    }
}
