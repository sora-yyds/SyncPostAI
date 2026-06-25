package run.halo.starter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;

/**
 * Plugin lifecycle entry.
 */
@Slf4j
@Component
public class SyncPostAiPlugin extends BasePlugin {

    public SyncPostAiPlugin(PluginContext pluginContext) {
        super(pluginContext);
    }

    @Override
    public void start() {
        log.info("SyncPostAI plugin started.");
    }

    @Override
    public void stop() {
        log.info("SyncPostAI plugin stopped.");
    }
}
