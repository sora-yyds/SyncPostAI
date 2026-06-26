package run.halo.starter.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.infra.ExternalLinkProcessor;
import run.halo.app.plugin.ReactiveSettingFetcher;
import run.halo.app.core.extension.content.Category;
import run.halo.app.core.extension.content.Category.CategorySpec;
import run.halo.app.core.extension.content.Post;
import run.halo.app.core.extension.content.Post.Excerpt;
import run.halo.app.core.extension.content.Post.PostPhase;
import run.halo.app.core.extension.content.Post.PostSpec;
import run.halo.app.core.extension.content.Post.PostStatus;
import run.halo.app.core.extension.content.Post.VisibleEnum;
import run.halo.app.core.extension.content.Snapshot;
import run.halo.app.core.extension.content.Snapshot.SnapShotSpec;
import run.halo.app.core.extension.content.Tag;
import run.halo.app.core.extension.content.Tag.TagSpec;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.MetadataUtil;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.Ref;
import run.halo.app.extension.GroupVersionKind;
import run.halo.starter.config.AiPublishSetting;
import run.halo.starter.config.AiSourceSetting;
import run.halo.starter.extension.ArticleReview;
import run.halo.starter.model.AiArticlePushRequest;
import run.halo.starter.model.AiArticlePushResponse;

@Service
public class AiArticlePublishService {

    private static final String RAW_TYPE_HTML = "html";

    private static final String CONTENT_TYPE_HTML = "html";

    private static final String CONTENT_TYPE_MARKDOWN = "markdown";

    private static final String CONTENT_TYPE_TEXT = "text";

    private final ReactiveExtensionClient client;

    private final ReactiveSettingFetcher settingFetcher;

    private final ExternalLinkProcessor externalLinkProcessor;

    private final Parser markdownParser = Parser.builder().build();

    private final HtmlRenderer htmlRenderer = HtmlRenderer.builder()
        .escapeHtml(true)
        .build();

    public AiArticlePublishService(ReactiveExtensionClient client, ReactiveSettingFetcher settingFetcher,
        ExternalLinkProcessor externalLinkProcessor) {
        this.client = client;
        this.settingFetcher = settingFetcher;
        this.externalLinkProcessor = externalLinkProcessor;
    }

    public Mono<AiArticlePushResponse> publish(AiArticlePushRequest request) {
        var validationMessage = validate(request);
        if (validationMessage != null) {
            return Mono.just(AiArticlePushResponse.failed(validationMessage));
        }

        var content = articleContent(request);
        var title = resolveTitle(request, content.frontMatter());
        var articleName = StringUtils.hasText(request.slug())
            ? normalizeName(request.slug())
            : normalizeName(title);
        var finalArticleName = StringUtils.hasText(articleName)
            ? articleName
            : "ai-article-" + shortHash(title + System.currentTimeMillis());
        return publishSetting()
            .flatMap(setting -> shouldPublish(request, setting).flatMap(published -> {
                if (!published) {
                    return createReview(request, content, title);
                }

                return client.fetch(Post.class, finalArticleName)
                    .flatMap(existingPost -> Mono.just(AiArticlePushResponse.failed(
                        "Article already exists: " + finalArticleName
                    )))
                    .switchIfEmpty(Mono.defer(() -> createArticle(request, finalArticleName, "published",
                        true, setting, content, title)));
            }));
    }

    public Mono<List<ArticleReview>> listReviews(String status) {
        return client.listAll(ArticleReview.class, ListOptions.builder().build(),
                Sort.by(Sort.Order.desc("metadata.creationTimestamp")))
            .filter(review -> !StringUtils.hasText(status) || status.equals(review.getSpec().getStatus()))
            .collectList();
    }

    public Mono<AiArticlePushResponse> approveReview(String reviewName, String reviewer) {
        return client.fetch(ArticleReview.class, reviewName)
            .flatMap(review -> {
                if ("approved".equals(review.getSpec().getStatus())) {
                    return Mono.just(AiArticlePushResponse.accepted(
                        review.getSpec().getArticleName(),
                        review.getSpec().getSnapshotName(),
                        "published",
                        review.getSpec().getArticleUrl()
                    ));
                }
                var request = requestFromReview(review, true);
                var attempts = review.getSpec().getAttempts() == null ? 0 : review.getSpec().getAttempts();
                review.getSpec().setAttempts(attempts + 1);
                return publishDirect(request)
                    .flatMap(response -> {
                        review.getSpec().setReviewedAt(Instant.now());
                        review.getSpec().setReviewedBy(reviewer);
                        if (response.success()) {
                            review.getSpec().setStatus("approved");
                            review.getSpec().setArticleName(response.articleName());
                            review.getSpec().setSnapshotName(response.snapshotName());
                            review.getSpec().setArticleUrl(response.articleUrl());
                            review.getSpec().setFailureReason("");
                        } else {
                            review.getSpec().setStatus("failed");
                            review.getSpec().setFailureReason(response.message());
                        }
                        return client.update(review).thenReturn(response);
                    });
            })
            .switchIfEmpty(Mono.just(AiArticlePushResponse.failed("Review item not found: " + reviewName)));
    }

    public Mono<ArticleReview> rejectReview(String reviewName, String reason, String reviewer) {
        return client.fetch(ArticleReview.class, reviewName)
            .flatMap(review -> {
                review.getSpec().setStatus("rejected");
                review.getSpec().setRejectReason(StringUtils.hasText(reason) ? reason.trim() : "");
                review.getSpec().setReviewedAt(Instant.now());
                review.getSpec().setReviewedBy(reviewer);
                return client.update(review);
            });
    }

    public Mono<Boolean> deleteReview(String reviewName) {
        return client.fetch(ArticleReview.class, reviewName)
            .flatMap(review -> client.delete(review).thenReturn(true))
            .defaultIfEmpty(false);
    }

    public Mono<Integer> cleanupReviews(Integer retentionDays) {
        return publishSetting()
            .map(setting -> retentionDays == null || retentionDays < 1
                ? setting.reviewRetentionDaysOrDefault()
                : retentionDays)
            .flatMap(days -> {
                var cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
                return client.listAll(ArticleReview.class, ListOptions.builder().build(),
                        Sort.by(Sort.Order.asc("metadata.creationTimestamp")))
                    .filter(review -> shouldCleanup(review, cutoff))
                    .concatMap(review -> client.delete(review).thenReturn(1))
                    .reduce(0, Integer::sum);
            });
    }

    private Mono<AiArticlePushResponse> publishDirect(AiArticlePushRequest request) {
        var validationMessage = validate(request);
        if (validationMessage != null) {
            return Mono.just(AiArticlePushResponse.failed(validationMessage));
        }
        var content = articleContent(request);
        var title = resolveTitle(request, content.frontMatter());
        var articleName = StringUtils.hasText(request.slug())
            ? normalizeName(request.slug())
            : normalizeName(title);
        var finalArticleName = StringUtils.hasText(articleName)
            ? articleName
            : "ai-article-" + shortHash(title + System.currentTimeMillis());
        return publishSetting()
            .flatMap(setting -> client.fetch(Post.class, finalArticleName)
                .flatMap(existingPost -> Mono.just(AiArticlePushResponse.failed(
                    "Article already exists: " + finalArticleName
                )))
                .switchIfEmpty(Mono.defer(() -> createArticle(request, finalArticleName, "published",
                    true, setting, content, title))));
    }

    private boolean shouldCleanup(ArticleReview review, Instant cutoff) {
        if (review == null || review.getSpec() == null) {
            return false;
        }
        var status = review.getSpec().getStatus();
        if (!"approved".equals(status) && !"rejected".equals(status) && !"failed".equals(status)) {
            return false;
        }
        var time = review.getSpec().getReviewedAt() == null
            ? review.getSpec().getReceivedAt()
            : review.getSpec().getReviewedAt();
        return time != null && time.isBefore(cutoff);
    }

    private Mono<AiArticlePushResponse> createReview(AiArticlePushRequest request, ArticleContent content,
        String title) {
        var now = Instant.now();
        var review = new ArticleReview();
        review.groupVersionKind(GroupVersionKind.fromExtension(ArticleReview.class));
        review.setMetadata(metadata("review-" + shortHash(title + content.body() + now.toString())));
        var spec = new ArticleReview.Spec();
        spec.setTitle(title);
        spec.setContent(request.content());
        spec.setContentType(normalizeContentType(request.contentType()));
        spec.setExcerpt(request.excerpt());
        spec.setAuthor(request.author());
        spec.setCover(request.cover());
        spec.setSlug(request.slug());
        spec.setTags(request.tags());
        spec.setCategories(request.categories());
        spec.setPublish(false);
        spec.setSource(StringUtils.hasText(request.source()) ? request.source().trim() : "external");
        spec.setStatus("pending");
        spec.setReceivedAt(now);
        spec.setAttempts(0);
        review.setSpec(spec);
        return client.create(review)
            .map(created -> AiArticlePushResponse.queued(created.getMetadata().getName()));
    }

    private AiArticlePushRequest requestFromReview(ArticleReview review, boolean publish) {
        var spec = review.getSpec();
        return new AiArticlePushRequest(
            spec.getTitle(),
            spec.getContent(),
            spec.getContentType(),
            spec.getExcerpt(),
            spec.getAuthor(),
            spec.getCover(),
            spec.getSlug(),
            spec.getTags(),
            spec.getCategories(),
            publish,
            spec.getSource()
        );
    }

    private Mono<AiArticlePushResponse> createArticle(AiArticlePushRequest request, String postName,
        String status, boolean published, AiPublishSetting setting, ArticleContent content, String title) {
        var now = Instant.now();
        var snapshotName = postName + "-base-" + shortHash(content.body());
        var htmlContent = renderContent(request, content.body());
        var owner = resolveOwner(request, content.frontMatter(), setting);
        var categories = configuredValues(content.frontMatter().list("categories"), setting.categoriesOrDefault());
        var tags = configuredValues(content.frontMatter().list("tags"), setting.tagsOrDefault());
        categories = configuredValues(request.categories(), categories);
        tags = configuredValues(request.tags(), tags);

        return ensureCategories(categories)
            .zipWith(ensureTags(tags))
            .flatMap(tuple -> {
                var categoryNames = tuple.getT1();
                var tagNames = tuple.getT2();
                var snapshot = newSnapshot(snapshotName, postName, htmlContent, owner, now);
                var post = newPost(postName, snapshotName, request, content.frontMatter(), title,
                    categoryNames, tagNames, published, owner, resolveCover(request, content.frontMatter(),
                        setting), now);

                return client.create(snapshot)
                    .then(client.create(post))
                    .map(createdPost -> AiArticlePushResponse.accepted(
                        createdPost.getMetadata().getName(), snapshotName, status, articleUrl(postName)
                    ));
            });
    }

    private Mono<List<String>> ensureCategories(List<String> displayNames) {
        return Flux.fromIterable(nonBlankValues(displayNames))
            .concatMap(this::ensureCategory)
            .collectList();
    }

    private Mono<String> ensureCategory(String displayName) {
        var name = resourceName("category", displayName);
        return client.fetch(Category.class, name)
            .map(category -> category.getMetadata().getName())
            .switchIfEmpty(Mono.defer(() -> client.create(newCategory(name, displayName))
                .map(category -> category.getMetadata().getName())));
    }

    private Mono<List<String>> ensureTags(List<String> displayNames) {
        return Flux.fromIterable(nonBlankValues(displayNames))
            .concatMap(this::ensureTag)
            .collectList();
    }

    private Mono<String> ensureTag(String displayName) {
        var name = resourceName("tag", displayName);
        return client.fetch(Tag.class, name)
            .map(tag -> tag.getMetadata().getName())
            .switchIfEmpty(Mono.defer(() -> client.create(newTag(name, displayName))
                .map(tag -> tag.getMetadata().getName())));
    }

    private Snapshot newSnapshot(String snapshotName, String postName, String content, String owner,
        Instant now) {
        var snapshot = new Snapshot();
        snapshot.groupVersionKind(GroupVersionKind.fromExtension(Snapshot.class));
        snapshot.setMetadata(metadata(snapshotName));
        MetadataUtil.nullSafeAnnotations(snapshot).put(Snapshot.KEEP_RAW_ANNO, "true");

        var spec = new SnapShotSpec();
        spec.setSubjectRef(Ref.of(postName, Post.GVK));
        spec.setRawType(RAW_TYPE_HTML);
        spec.setRawPatch(content);
        spec.setContentPatch(content);
        spec.setLastModifyTime(now);
        spec.setOwner(owner);
        spec.setContributors(new HashSet<>(List.of(owner)));
        snapshot.setSpec(spec);
        return snapshot;
    }

    private Post newPost(String postName, String snapshotName, AiArticlePushRequest request,
        FrontMatter frontMatter, String title, List<String> categories, List<String> tags,
        boolean published, String owner, String cover, Instant now) {
        var post = new Post();
        post.groupVersionKind(Post.GVK);
        post.setMetadata(metadata(postName));

        var labels = MetadataUtil.nullSafeLabels(post);
        labels.put(Post.DELETED_LABEL, "false");
        labels.put(Post.PUBLISHED_LABEL, Boolean.toString(published));
        labels.put(Post.OWNER_LABEL, owner);
        labels.put(Post.VISIBLE_LABEL, VisibleEnum.PUBLIC.name());

        var annotations = MetadataUtil.nullSafeAnnotations(post);
        annotations.put(Post.LAST_RELEASED_SNAPSHOT_ANNO, published ? snapshotName : "");

        var spec = new PostSpec();
        spec.setTitle(title);
        spec.setSlug(postName);
        spec.setBaseSnapshot(snapshotName);
        spec.setHeadSnapshot(snapshotName);
        spec.setReleaseSnapshot(published ? snapshotName : null);
        spec.setOwner(owner);
        spec.setDeleted(false);
        spec.setPublish(published);
        spec.setPublishTime(published ? now : null);
        spec.setPinned(false);
        spec.setAllowComment(true);
        spec.setVisible(VisibleEnum.PUBLIC);
        spec.setPriority(0);
        spec.setCategories(categories);
        spec.setTags(tags);
        spec.setCover(cover);
        spec.setHtmlMetas(List.of());

        var excerpt = new Excerpt();
        var rawExcerpt = resolveExcerpt(request, frontMatter);
        if (StringUtils.hasText(rawExcerpt)) {
            excerpt.setAutoGenerate(false);
            excerpt.setRaw(rawExcerpt);
        } else {
            excerpt.setAutoGenerate(true);
            excerpt.setRaw(null);
        }
        spec.setExcerpt(excerpt);

        post.setSpec(spec);

        var postStatus = new PostStatus();
        postStatus.setPhase(published ? PostPhase.PUBLISHED.name() : PostPhase.DRAFT.name());
        postStatus.setContributors(List.of(owner));
        postStatus.setLastModifyTime(now);
        post.setStatus(postStatus);
        return post;
    }

    private Mono<AiPublishSetting> publishSetting() {
        return settingFetcher.fetch(AiPublishSetting.GROUP, AiPublishSetting.class)
            .defaultIfEmpty(AiPublishSetting.defaults())
            .onErrorReturn(AiPublishSetting.defaults());
    }

    private Mono<Boolean> shouldPublish(AiArticlePushRequest request, AiPublishSetting publishSetting) {
        if (!publishSetting.shouldPublishByDefault()) {
            return Mono.just(false);
        }
        if (request.publish() != null) {
            return Mono.just(Boolean.TRUE.equals(request.publish()));
        }
        return settingFetcher.fetch(AiSourceSetting.GROUP, AiSourceSetting.class)
            .defaultIfEmpty(AiSourceSetting.defaults())
            .onErrorReturn(AiSourceSetting.defaults())
            .map(sourceSetting -> sourceSetting.profile(request.source()))
            .map(profile -> profile.known() ? profile.defaultPublish() : publishSetting.shouldPublishByDefault());
    }

    private Category newCategory(String name, String displayName) {
        var category = new Category();
        category.groupVersionKind(Category.GVK);
        category.setMetadata(metadata(name));

        var spec = new CategorySpec();
        spec.setDisplayName(displayName);
        spec.setSlug(name);
        spec.setPriority(0);
        spec.setChildren(new ArrayList<>());
        spec.setHideFromList(false);
        spec.setPreventParentPostCascadeQuery(false);
        category.setSpec(spec);
        return category;
    }

    private Tag newTag(String name, String displayName) {
        var tag = new Tag();
        tag.groupVersionKind(Tag.GVK);
        tag.setMetadata(metadata(name));

        var spec = new TagSpec();
        spec.setDisplayName(displayName);
        spec.setSlug(name);
        tag.setSpec(spec);
        return tag;
    }

    private Metadata metadata(String name) {
        var metadata = new Metadata();
        metadata.setName(name);
        metadata.setLabels(new HashMap<>());
        metadata.setAnnotations(new HashMap<>());
        return metadata;
    }

    private String validate(AiArticlePushRequest request) {
        if (request == null) {
            return "Request body is required.";
        }
        if (!StringUtils.hasText(request.content())) {
            return "Content is required.";
        }
        return null;
    }

    private String resolveTitle(AiArticlePushRequest request, FrontMatter frontMatter) {
        if (request != null && StringUtils.hasText(request.title())) {
            return request.title().trim();
        }
        var frontMatterTitle = frontMatter.value("title");
        if (StringUtils.hasText(frontMatterTitle)) {
            return frontMatterTitle;
        }
        if (request == null || !StringUtils.hasText(request.content())) {
            return "未命名文章";
        }
        var heading = extractFirstMarkdownHeading(frontMatter.body());
        return StringUtils.hasText(heading) ? heading : fallbackTitle(request.content());
    }

    private String extractFirstMarkdownHeading(String content) {
        var lines = content.split("\\R");
        for (var line : lines) {
            var trimmed = line.trim();
            if (trimmed.startsWith("# ") && trimmed.length() > 2) {
                return trimmed.substring(2).trim();
            }
        }
        return "";
    }

    private String fallbackTitle(String content) {
        var text = content.replaceAll("(?s)^---\\s*\\R.*?\\R---\\s*\\R?", "")
            .replaceAll("[#>*_`\\-\\[\\]()]", " ")
            .replaceAll("\\s+", " ")
            .trim();
        if (StringUtils.hasText(text)) {
            return text.length() > 30 ? text.substring(0, 30) : text;
        }
        return "未命名文章";
    }

    private String articleUrl(String postName) {
        return externalLinkProcessor.processLink("/archives/" + postName);
    }

    private String normalizeName(String value) {
        var normalized = Normalizer.normalize(value, Normalizer.Form.NFKD)
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-+|-+$)", "");
        if (StringUtils.hasText(normalized)) {
            return normalized;
        }
        return "";
    }

    private String resourceName(String prefix, String displayName) {
        var normalized = normalizeName(displayName);
        if (StringUtils.hasText(normalized)) {
            return normalized;
        }
        return prefix + "-" + shortHash(displayName);
    }

    private List<String> nonBlankValues(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .distinct()
            .toList();
    }

    private List<String> configuredValues(List<String> requestValues, List<String> settingValues) {
        var cleanedRequestValues = nonBlankValues(requestValues);
        if (!cleanedRequestValues.isEmpty()) {
            return cleanedRequestValues;
        }
        return nonBlankValues(settingValues);
    }

    private String toHtmlContent(String content) {
        var trimmed = content.trim();
        if (trimmed.contains("<") && trimmed.contains(">")) {
            return trimmed;
        }
        var paragraphs = trimmed.split("\\R{2,}");
        var html = new StringBuilder();
        for (var paragraph : paragraphs) {
            if (StringUtils.hasText(paragraph)) {
                html.append("<p>")
                    .append(escapeHtml(paragraph).replace("\r\n", "<br>").replace("\n", "<br>"))
                    .append("</p>");
            }
        }
        return html.toString();
    }

    private String renderContent(AiArticlePushRequest request, String content) {
        var contentType = normalizeContentType(request.contentType());
        if (CONTENT_TYPE_MARKDOWN.equals(contentType)) {
            return renderMarkdown(content);
        }
        if (CONTENT_TYPE_HTML.equals(contentType)) {
            return content.trim();
        }
        return toHtmlContent(content);
    }

    private String normalizeContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return CONTENT_TYPE_TEXT;
        }
        var normalized = contentType.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case CONTENT_TYPE_HTML, CONTENT_TYPE_MARKDOWN, CONTENT_TYPE_TEXT -> normalized;
            default -> CONTENT_TYPE_TEXT;
        };
    }

    private String renderMarkdown(String content) {
        var document = markdownParser.parse(content.trim());
        return htmlRenderer.render(document);
    }

    private String resolveOwner(AiArticlePushRequest request, FrontMatter frontMatter,
        AiPublishSetting setting) {
        if (request != null && StringUtils.hasText(request.author())) {
            return request.author().trim();
        }
        var author = frontMatter.value("author");
        if (!StringUtils.hasText(author)) {
            author = frontMatter.value("auther");
        }
        return StringUtils.hasText(author) ? author : setting.ownerOrDefault();
    }

    private String resolveExcerpt(AiArticlePushRequest request, FrontMatter frontMatter) {
        if (request != null && StringUtils.hasText(request.excerpt())) {
            return request.excerpt().trim();
        }
        return frontMatter.value("excerpt");
    }

    private String resolveCover(AiArticlePushRequest request, FrontMatter frontMatter,
        AiPublishSetting setting) {
        if (request != null && StringUtils.hasText(request.cover())) {
            return request.cover().trim();
        }
        var cover = frontMatter.value("cover");
        if (StringUtils.hasText(cover)) {
            return cover;
        }
        var coverImages = nonBlankValues(setting.coverImagesOrDefault());
        if (setting.shouldUseRandomCover() && !coverImages.isEmpty()) {
            return coverImages.get(ThreadLocalRandom.current().nextInt(coverImages.size()));
        }
        return "";
    }

    private ArticleContent articleContent(AiArticlePushRequest request) {
        var parsed = FrontMatter.parse(request.content());
        return new ArticleContent(parsed.body(), parsed);
    }

    private record ArticleContent(String body, FrontMatter frontMatter) {
    }

    private record FrontMatter(String body, Map<String, List<String>> values) {

        static FrontMatter parse(String content) {
            if (!StringUtils.hasText(content)) {
                return new FrontMatter("", Map.of());
            }
            var normalized = content.replace("\r\n", "\n").replace('\r', '\n');
            if (!normalized.startsWith("---\n")) {
                return new FrontMatter(content, Map.of());
            }
            var end = normalized.indexOf("\n---", 4);
            if (end < 0) {
                return new FrontMatter(content, Map.of());
            }
            var metadata = normalized.substring(4, end).trim();
            var bodyStart = end + "\n---".length();
            if (bodyStart < normalized.length() && normalized.charAt(bodyStart) == '\n') {
                bodyStart++;
            }
            return new FrontMatter(normalized.substring(bodyStart), parseValues(metadata));
        }

        String value(String key) {
            var valuesForKey = values.get(normalizeKey(key));
            if (valuesForKey == null || valuesForKey.isEmpty()) {
                return "";
            }
            return valuesForKey.get(0);
        }

        List<String> list(String key) {
            return values.getOrDefault(normalizeKey(key), List.of());
        }

        private static Map<String, List<String>> parseValues(String metadata) {
            var values = new LinkedHashMap<String, List<String>>();
            String currentListKey = null;
            for (var line : metadata.split("\\R")) {
                if (!StringUtils.hasText(line)) {
                    continue;
                }
                var trimmed = line.trim();
                if (trimmed.startsWith("- ") && currentListKey != null) {
                    values.computeIfAbsent(currentListKey, ignored -> new ArrayList<>())
                        .add(cleanValue(trimmed.substring(2)));
                    continue;
                }
                var separator = line.indexOf(':');
                if (separator < 0) {
                    currentListKey = null;
                    continue;
                }
                var key = normalizeKey(line.substring(0, separator));
                var value = cleanValue(line.substring(separator + 1));
                if (StringUtils.hasText(value)) {
                    values.put(key, new ArrayList<>(List.of(value)));
                    currentListKey = null;
                } else {
                    values.putIfAbsent(key, new ArrayList<>());
                    currentListKey = key;
                }
            }
            return values;
        }

        private static String normalizeKey(String key) {
            return key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
        }

        private static String cleanValue(String value) {
            if (value == null) {
                return "";
            }
            var cleaned = value.trim();
            if ((cleaned.startsWith("\"") && cleaned.endsWith("\""))
                || (cleaned.startsWith("'") && cleaned.endsWith("'"))) {
                cleaned = cleaned.substring(1, cleaned.length() - 1);
            }
            return cleaned.trim();
        }
    }

    private String escapeHtml(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    private String shortHash(String value) {
        try {
            var digest = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 6);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available.", e);
        }
    }
}
