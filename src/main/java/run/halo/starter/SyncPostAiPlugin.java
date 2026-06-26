package run.halo.starter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import run.halo.app.extension.Scheme;
import run.halo.app.extension.SchemeManager;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;
import run.halo.starter.extension.ArticleReview;

/**
 * Plugin lifecycle entry.
 */
@Slf4j
@Component
public class SyncPostAiPlugin extends BasePlugin {

    private final SchemeManager schemeManager;

    public SyncPostAiPlugin(PluginContext pluginContext, SchemeManager schemeManager) {
        super(pluginContext);
        this.schemeManager = schemeManager;
    }

    @Override
    public void start() {
        schemeManager.register(ArticleReview.class);
        log.info("SyncPostAI plugin started.");
    }

    @Override
    public void stop() {
        schemeManager.unregister(Scheme.buildFromType(ArticleReview.class));
        log.info("SyncPostAI plugin stopped.");
    }
}
