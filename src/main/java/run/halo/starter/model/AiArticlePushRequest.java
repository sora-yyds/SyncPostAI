package run.halo.starter.model;

import java.util.List;

public record AiArticlePushRequest(
    String title,
    String content,
    String contentType,
    String excerpt,
    String author,
    String cover,
    String slug,
    List<String> tags,
    List<String> categories,
    Boolean publish
) {
}
