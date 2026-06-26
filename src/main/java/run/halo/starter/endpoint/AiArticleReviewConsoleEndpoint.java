package run.halo.starter.endpoint;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;
import run.halo.starter.model.AiArticlePushResponse;
import run.halo.starter.service.AiArticlePublishService;

@Component
@RequiredArgsConstructor
public class AiArticleReviewConsoleEndpoint implements CustomEndpoint {

    private final AiArticlePublishService articlePublishService;

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return route()
            .GET("/article-reviews", this::listReviews)
            .POST("/article-reviews/{name}/approve", this::approveReview)
            .POST("/article-reviews/{name}/reject", this::rejectReview)
            .DELETE("/article-reviews/{name}", this::deleteReview)
            .POST("/article-reviews/cleanup", this::cleanupReviews)
            .build();
    }

    @Override
    public GroupVersion groupVersion() {
        return new GroupVersion("console.api.syncpostai.sora.run", "v1alpha1");
    }

    private Mono<ServerResponse> listReviews(ServerRequest request) {
        var status = request.queryParam("status").orElse("");
        return articlePublishService.listReviews(status)
            .flatMap(reviews -> ServerResponse.ok()
                .contentType(APPLICATION_JSON)
                .bodyValue(reviews));
    }

    private Mono<ServerResponse> approveReview(ServerRequest request) {
        var name = request.pathVariable("name");
        return currentUser()
            .flatMap(reviewer -> articlePublishService.approveReview(name, reviewer))
            .flatMap(this::response);
    }

    private Mono<ServerResponse> rejectReview(ServerRequest request) {
        var name = request.pathVariable("name");
        return request.bodyToMono(RejectRequest.class)
            .defaultIfEmpty(new RejectRequest(""))
            .zipWith(currentUser())
            .flatMap(tuple -> articlePublishService.rejectReview(name, tuple.getT1().reason(), tuple.getT2()))
            .flatMap(review -> ServerResponse.ok()
                .contentType(APPLICATION_JSON)
                .bodyValue(review))
            .switchIfEmpty(ServerResponse.notFound().build());
    }

    private Mono<ServerResponse> deleteReview(ServerRequest request) {
        var name = request.pathVariable("name");
        return articlePublishService.deleteReview(name)
            .flatMap(deleted -> deleted
                ? ServerResponse.noContent().build()
                : ServerResponse.notFound().build());
    }

    private Mono<ServerResponse> cleanupReviews(ServerRequest request) {
        return request.bodyToMono(CleanupRequest.class)
            .defaultIfEmpty(new CleanupRequest(null))
            .flatMap(body -> articlePublishService.cleanupReviews(body.retentionDays()))
            .flatMap(count -> ServerResponse.ok()
                .contentType(APPLICATION_JSON)
                .bodyValue(new CleanupResponse(count)));
    }

    private Mono<ServerResponse> response(AiArticlePushResponse response) {
        var builder = response.success() ? ServerResponse.ok() : ServerResponse.badRequest();
        return builder.contentType(APPLICATION_JSON)
            .bodyValue(response);
    }

    private Mono<String> currentUser() {
        return ReactiveSecurityContextHolder.getContext()
            .map(context -> context.getAuthentication().getName())
            .defaultIfEmpty("unknown");
    }

    private record RejectRequest(String reason) {
    }

    private record CleanupRequest(Integer retentionDays) {
    }

    private record CleanupResponse(Integer deletedCount) {
    }
}
