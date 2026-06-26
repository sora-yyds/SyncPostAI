package run.halo.starter.endpoint;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;
import run.halo.starter.model.AiArticlePushRequest;
import run.halo.starter.model.AiArticlePushResponse;
import run.halo.starter.security.AiPushTokenValidator;
import run.halo.starter.service.AiArticlePublishService;

@Component
@RequiredArgsConstructor
public class AiArticleEndpoint implements CustomEndpoint {

    private final AiArticlePublishService articlePublishService;

    private final AiPushTokenValidator tokenValidator;

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return route()
            .POST("/articles", this::pushArticle)
            .build();
    }

    @Override
    public GroupVersion groupVersion() {
        return new GroupVersion("api.syncpostai.sora.run", "v1alpha1");
    }

    private Mono<ServerResponse> pushArticle(ServerRequest request) {
        var token = request.headers().firstHeader(AiPushTokenValidator.TOKEN_HEADER);
        return tokenValidator.isEnabled()
            .flatMap(enabled -> enabled ? parseAndValidate(request, token) : forbidden());
    }

    private Mono<ServerResponse> parseAndValidate(ServerRequest request, String token) {
        return request.bodyToMono(AiArticlePushRequest.class)
            .switchIfEmpty(Mono.just(new AiArticlePushRequest(null, null, null, null, null, null,
                null, null, null, null, null)))
            .flatMap(body -> ensureSourceEnabled(body, token));
    }

    private Mono<ServerResponse> ensureSourceEnabled(AiArticlePushRequest body, String token) {
        return tokenValidator.isSourceEnabled(body.source())
            .flatMap(enabled -> enabled ? ensureConfigured(body, token) : sourceDisabled());
    }

    private Mono<ServerResponse> ensureConfigured(AiArticlePushRequest body, String token) {
        return tokenValidator.isConfigured(body.source())
            .flatMap(configured -> configured ? ensureValid(body, token) : notConfigured());
    }

    private Mono<ServerResponse> ensureValid(AiArticlePushRequest body, String token) {
        return tokenValidator.isValid(token, body.source())
            .flatMap(valid -> valid ? publishArticle(body) : unauthorized());
    }

    private Mono<ServerResponse> publishArticle(AiArticlePushRequest body) {
        return articlePublishService.publish(body)
            .flatMap(response -> {
                if (response.success()) {
                    return ServerResponse.ok()
                        .contentType(APPLICATION_JSON)
                        .bodyValue(response);
                }
                return ServerResponse.badRequest()
                    .contentType(APPLICATION_JSON)
                    .bodyValue(response);
            });
    }

    private Mono<ServerResponse> forbidden() {
        return ServerResponse.status(403)
            .contentType(APPLICATION_JSON)
            .bodyValue(AiArticlePushResponse.failed("External article push is disabled."));
    }

    private Mono<ServerResponse> sourceDisabled() {
        return ServerResponse.status(403)
            .contentType(APPLICATION_JSON)
            .bodyValue(AiArticlePushResponse.failed("Source is disabled."));
    }

    private Mono<ServerResponse> notConfigured() {
        return ServerResponse.status(503)
            .contentType(APPLICATION_JSON)
            .bodyValue(AiArticlePushResponse.failed("Push token is not configured."));
    }

    private Mono<ServerResponse> unauthorized() {
        return ServerResponse.status(401)
            .contentType(APPLICATION_JSON)
            .bodyValue(AiArticlePushResponse.failed("Invalid push token."));
    }
}
