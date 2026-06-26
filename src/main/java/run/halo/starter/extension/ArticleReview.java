package run.halo.starter.extension;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

@Data
@EqualsAndHashCode(callSuper = true)
@GVK(
    group = "syncpostai.sora.run",
    version = "v1alpha1",
    kind = "ArticleReview",
    plural = "articlereviews",
    singular = "articlereview"
)
public class ArticleReview extends AbstractExtension {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Spec spec;

    @Data
    @Schema(name = "ArticleReviewSpec")
    public static class Spec {
        private String title;
        private String content;
        private String contentType;
        private String excerpt;
        private String author;
        private String cover;
        private String slug;
        private List<String> tags;
        private List<String> categories;
        private Boolean publish;
        private String source;
        private String status;
        private String articleName;
        private String snapshotName;
        private String articleUrl;
        private String failureReason;
        private String rejectReason;
        private Instant receivedAt;
        private Instant reviewedAt;
        private String reviewedBy;
        private Integer attempts;
    }
}
