package dev.foxikle.foxrankvelocity;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

public class ReloadRanksCommand implements SimpleCommand {
    private final FoxRankVelocity plugin;

    public ReloadRanksCommand(FoxRankVelocity plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        if(invocation.source() instanceof Player player){
            Rank rank = plugin.dataManager.getStoredRank(player.getUniqueId());
            List<String> nodes = rank.getPermissionNodes();
            if(nodes.contains("foxrank.*") || nodes.contains("foxrank.commands.reload") || nodes.contains("foxrank.commands.*")){
                plugin.dataManager.setupRanks();
                player.sendMessage(Component.text("Successfully reloaded ranks.", NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("You cannot do this!", NamedTextColor.RED));
            }
        }
    }
}
