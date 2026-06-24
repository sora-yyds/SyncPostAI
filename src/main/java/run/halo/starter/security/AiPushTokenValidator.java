package run.halo.starter.security;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;
import run.halo.starter.config.AiSecuritySetting;

@Component
public class AiPushTokenValidator {

    public static final String TOKEN_HEADER = "X-SyncPost-Token";

    private static final String TOKEN_PROPERTY = "syncpostai.push-token";

    private static final String TOKEN_ENV = "SYNCPOSTAI_PUSH_TOKEN";

    private final ReactiveSettingFetcher settingFetcher;

    public AiPushTokenValidator(ReactiveSettingFetcher settingFetcher) {
        this.settingFetcher = settingFetcher;
    }

    public Mono<Boolean> isEnabled() {
        return securitySetting()
            .map(AiSecuritySetting::enabledOrDefault);
    }

    public Mono<Boolean> isConfigured() {
        return configuredToken()
            .map(StringUtils::hasText);
    }

    public Mono<Boolean> isValid(String token) {
        return configuredToken()
            .map(expectedToken -> StringUtils.hasText(expectedToken) && expectedToken.equals(token));
    }

    private Mono<String> configuredToken() {
        return securitySetting()
            .map(AiSecuritySetting::tokenOrBlank)
            .map(settingToken -> StringUtils.hasText(settingToken) ? settingToken : environmentToken());
    }

    private Mono<AiSecuritySetting> securitySetting() {
        return settingFetcher.fetch(AiSecuritySetting.GROUP, AiSecuritySetting.class)
            .defaultIfEmpty(AiSecuritySetting.defaults())
            .onErrorReturn(AiSecuritySetting.defaults());
    }

    private String environmentToken() {
        var propertyToken = System.getProperty(TOKEN_PROPERTY);
        if (StringUtils.hasText(propertyToken)) {
            return propertyToken;
        }
        return System.getenv(TOKEN_ENV);
    }
}
