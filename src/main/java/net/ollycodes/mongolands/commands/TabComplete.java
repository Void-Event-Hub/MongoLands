package net.ollycodes.mongolands.commands;

import me.angeschossen.lands.api.land.Land;
import me.angeschossen.lands.api.nation.Nation;
import net.ollycodes.mongolands.MongoLands;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TabComplete implements TabCompleter {

    public final MongoLands plugin;
    public TabComplete(MongoLands plugin) { this.plugin = plugin; }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (!command.getName().equalsIgnoreCase("ml")) {
            return null;
        }

        if (strings.length == 1) {
            return new ArrayList<>(Arrays.asList("sync", "syncrecord", "reload", "testdb"));
        } else if (strings.length == 2 && strings[0].equals("testdb")) {
            return new ArrayList<>(Arrays.asList("mongo", "lands"));
        } else if (strings.length == 2 && strings[0].equals("sync")) {
            return Arrays.asList(this.plugin.mongo.collectionNames);
        } else if (strings.length == 2 && strings[0].equals("syncrecord")) {
            return List.of("land", "nation", "player");
        } else if (strings.length == 3 && strings[0].equals("syncrecord")) {
            switch (strings[1]) {
                case "land":
                    return plugin.landsAPI.getLands().stream().map(Land::getName).toList();
                case "nation":
                    return plugin.landsAPI.getNations().stream().map(Nation::getName).toList();
                case "player":
                    return plugin.getServer().getOnlinePlayers().stream().map(Player::getName).toList();
            }
        }
        return null;
    }
}
