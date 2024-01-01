package dev.foxikle.foxrankvelocity;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;

public class MessageManager {
    private final FoxRankVelocity plugin;

    public MessageManager(FoxRankVelocity plugin) {
        this.plugin = plugin;
    }

    public void sendPlayerRankUpdateMessage(Player player){
        RegisteredServer server = player.getCurrentServer().get().getServer();
        server.sendPluginMessage(FoxRankVelocity.UPDATE_PLAYER_RANK_IDENTIFIER, new byte[]{});
    }

    //public void sendReloadRanksMessage(Player player) {
    //    RegisteredServer server = player.getCurrentServer().get().getServer();
    //    server.sendPluginMessage(FoxRankVelocity.UPDATE_PLAYER_RANK_IDENTIFIER, new byte[]{});
    //}
}
