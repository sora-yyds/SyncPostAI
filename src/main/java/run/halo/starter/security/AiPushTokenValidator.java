package run.halo.starter.security;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;
import run.halo.starter.config.AiSourceSetting;

@Component
public class AiPushTokenValidator {

    public static final String TOKEN_HEADER = "X-SyncPost-Token";

    private final ReactiveSettingFetcher settingFetcher;

    public AiPushTokenValidator(ReactiveSettingFetcher settingFetcher) {
        this.settingFetcher = settingFetcher;
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
            .map(profile -> profile.known() && profile.enabled() ? profile.token() : "");
    }

    private Mono<AiSourceSetting> sourceSetting() {
        return settingFetcher.fetch(AiSourceSetting.GROUP, AiSourceSetting.class)
            .defaultIfEmpty(AiSourceSetting.defaults())
            .onErrorReturn(AiSourceSetting.defaults());
    }

}
