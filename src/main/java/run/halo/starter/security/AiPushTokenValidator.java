package run.halo.starter.security;

import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.Secret;
import run.halo.app.plugin.ReactiveSettingFetcher;
import run.halo.starter.config.AiSourceSetting;

@Slf4j
@Component
public class AiPushTokenValidator {

    public static final String TOKEN_HEADER = "X-SyncPost-Token";

    private final ReactiveSettingFetcher settingFetcher;

    private final ReactiveExtensionClient client;

    public AiPushTokenValidator(ReactiveSettingFetcher settingFetcher, ReactiveExtensionClient client) {
        this.settingFetcher = settingFetcher;
        this.client = client;
    }

    public Mono<Boolean> isEnabled() {
        return sourceSetting()
            .map(AiSourceSetting::enabledOrDefault);
    }

    public Mono<Boolean> isConfigured(String source) {
        return configuredToken(source)
            .map(StringUtils::hasText);
    }

    public Mono<Boolean> isSourceEnabled(String source) {
        return sourceSetting()
            .map(setting -> setting.profile(source))
            .map(profile -> profile.known() && profile.enabled());
    }

    public Mono<Boolean> isValid(String token, String source) {
        return configuredToken(source)
            .map(expectedToken -> StringUtils.hasText(expectedToken) && expectedToken.equals(token));
    }

    private Mono<String> configuredToken(String source) {
        return sourceSetting()
            .map(setting -> setting.profile(source))
            .flatMap(profile -> {
                if (!profile.known() || !profile.enabled() || !profile.hasSecret()) {
                    return Mono.just("");
                }
                return tokenFromSecret(profile.secretName());
            });
    }

    private Mono<String> tokenFromSecret(String secretName) {
        return client.fetch(Secret.class, secretName)
            .map(this::tokenValue)
            .doOnNext(token -> {
                if (!StringUtils.hasText(token)) {
                    log.warn("Source push token is empty or missing `token` key in Secret [{}].", secretName);
                }
            })
            .defaultIfEmpty("")
            .doOnError(error -> log.warn("Failed to read source push token from Secret [{}]: {}",
                secretName, error.getMessage()))
            .onErrorReturn("");
    }

    private String tokenValue(Secret secret) {
        if (secret.getData() != null && secret.getData().containsKey("token")) {
            var tokenBytes = secret.getData().get("token");
            return tokenBytes == null ? "" : new String(tokenBytes, StandardCharsets.UTF_8).trim();
        }
        if (secret.getStringData() != null && secret.getStringData().containsKey("token")) {
            var token = secret.getStringData().get("token");
            return token == null ? "" : token.trim();
        }
        return "";
    }

    private Mono<AiSourceSetting> sourceSetting() {
        return settingFetcher.fetch(AiSourceSetting.GROUP, AiSourceSetting.class)
            .defaultIfEmpty(AiSourceSetting.defaults())
            .onErrorReturn(AiSourceSetting.defaults());
    }

}
