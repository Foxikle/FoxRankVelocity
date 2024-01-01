package dev.foxikle.foxrankvelocity;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;

@Plugin(
        id = "foxrankvelocity",
        name = "FoxRankVelocity",
        version = BuildConstants.VERSION,
        url = "https://foxikle.dev/plugins",
        authors = {"Foxikle"}
)
public class FoxRankVelocity {

    private Logger logger;
    private final Path dataDirectory;
    private final ProxyServer server;
    private YamlDocument config;
    public DataManager dataManager;
    public Map<String, Rank> ranks = new HashMap<>();
    public Map<String, Integer> powerLevels = new HashMap<>();
    protected static FoxRankVelocity INSTANCE;
    public MessageManager messageManager;
    public static final MinecraftChannelIdentifier UPDATE_PLAYER_RANK_IDENTIFIER = MinecraftChannelIdentifier.from("foxrank:updateranks");

    @Inject
    public FoxRankVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;

        try {
            config = YamlDocument.create(new File(dataDirectory.toFile(), "config.yml"),
                    Objects.requireNonNull(getClass().getResourceAsStream("/config.yml")),
                    GeneralSettings.DEFAULT,
                    LoaderSettings.builder().setAutoUpdate(true).build(),
                    DumperSettings.DEFAULT,
                    UpdaterSettings.builder().setVersioning(new BasicVersioning("file-version"))
                            .setOptionSorting(UpdaterSettings.OptionSorting.SORT_BY_DEFAULTS).build()
            );
            config.update();
            config.save();
        } catch (IOException e) {
            logger.error("Config couldn't be loaded! Shutting down plugin.");
            server.shutdown();
        }
        messageManager = new MessageManager(this);
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        INSTANCE = this;
        server.getChannelRegistrar().register(UPDATE_PLAYER_RANK_IDENTIFIER);
        dataManager = new DataManager(this);
        try {
            dataManager.connect();
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
        dataManager.setupRanks();
        CommandManager cm = getServer().getCommandManager();
        cm.register(cm.metaBuilder("reloadproxyranks").plugin(this).aliases("rpr").build(), new ReloadRanksCommand(this));
        cm.register(cm.metaBuilder("setrank").plugin(this).build(), new SetRankCommand(this));
    }

    @Subscribe
    public void inProxyShutdown(ProxyShutdownEvent event) {
        dataManager.disconnect();
    }

    public Logger getLogger() {
        return logger;
    }

    public ProxyServer getServer() {
        return server;
    }

    public YamlDocument getConfig() {
        return config;
    }
    public Rank getDefaultRank() {
        return Iterables.getLast(ranks.values());
    }
    public ProxyServer getProxy(){
        return server;
    }
}
