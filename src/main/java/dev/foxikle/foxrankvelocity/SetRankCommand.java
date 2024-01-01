package dev.foxikle.foxrankvelocity;

import com.google.gson.JsonParser;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.UUID;

public class SetRankCommand implements SimpleCommand {

    private final FoxRankVelocity plugin;

    public SetRankCommand(FoxRankVelocity plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        if (invocation.source() instanceof Player player) {
            if (plugin.dataManager.getStoredRank(player.getUniqueId()).getPermissionNodes().contains("foxrank.ranks.setrank") ||
                    plugin.dataManager.getStoredRank(player.getUniqueId()).getPermissionNodes().contains("foxrank.ranks.*") ||
                    plugin.dataManager.getStoredRank(player.getUniqueId()).getPermissionNodes().contains("foxrank.*")) {
                if (invocation.arguments().length >= 2) {
                    boolean force = false;
                    if (invocation.arguments().length >= 3) {
                        force = invocation.arguments()[2].equals("--force");
                    }

                    String rankID;
                    UUID target;
                    boolean sendMessage = true;

                    if (plugin.ranks.keySet().contains(invocation.arguments()[0])) {
                        rankID = invocation.arguments()[0];
                    } else {
                        player.sendMessage(Component.text("'" + invocation.arguments()[0] + "' is not a valid rank!", NamedTextColor.RED));
                        return;
                    }

                    if (force) {
                        String id = invocation.arguments()[0];
                        String name = invocation.arguments()[1];
                        UUID uuid;
                        try {
                            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + invocation.arguments()[1]);
                            InputStreamReader reader;
                            reader = new InputStreamReader(url.openStream());
                            String rawUuid = JsonParser.parseReader(reader).getAsJsonObject().get("id").getAsString();
                            uuid = UUID.fromString(
                                    rawUuid.replaceFirst(
                                            "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5"
                                    )
                            );
                        } catch (IOException e) {
                            plugin.getLogger().error("An error occoured whilst fetching " + invocation.arguments()[1] + "'s uuid from the Mojang API");
                            return;
                        }

                        plugin.dataManager.setupPlayer(uuid, id, name);
                        return;
                    }

                    if (plugin.dataManager.getNames().contains(invocation.arguments()[1])) {
                        if (plugin.getServer().getPlayer(invocation.arguments()[1]).isPresent()) {
                            target = plugin.getServer().getPlayer(invocation.arguments()[1]).get().getUniqueId();
                        } else {
                            // go out to mojang's api
                            try {
                                URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + invocation.arguments()[1]);
                                InputStreamReader reader;
                                reader = new InputStreamReader(url.openStream());
                                String uuid = JsonParser.parseReader(reader).getAsJsonObject().get("id").getAsString();
                                target = UUID.fromString(
                                        uuid.replaceFirst(
                                                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5"
                                        )
                                );
                            } catch (IOException e) {
                                plugin.getLogger().error("An error occoured whilst fetching " + invocation.arguments()[1] + "'s uuid from the Mojang API");
                                return;
                            }
                            sendMessage = false;
                        }
                    } else {
                        player.sendMessage(Component.text("'" + invocation.arguments()[1] + "' is not a valid player! Run '/setrank " + invocation.arguments()[0] + " " + invocation.arguments()[1] + " --force' to create a database entry for this player.", NamedTextColor.RED));
                        return;

                    }
                    plugin.dataManager.setStoredRank(target, Rank.fromID(rankID));
                    if (sendMessage)
                        plugin.messageManager.sendPlayerRankUpdateMessage(plugin.getProxy().getPlayer(target).get());

                } else {
                    player.sendMessage(Component.text("Insufficient arguments!", NamedTextColor.RED));
                }


            } else {
                player.sendMessage(Component.text("You cannot do this!", NamedTextColor.RED));
            }
        }
    }
}
