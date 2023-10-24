package net.ollycodes.mongolands.commands;

import me.angeschossen.lands.api.land.Land;
import me.angeschossen.lands.api.nation.Nation;
import me.angeschossen.lands.api.player.OfflinePlayer;
import net.ollycodes.mongolands.MongoLands;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;

public class MLCommand implements CommandExecutor {

    public final MongoLands plugin;
    public MLCommand(MongoLands plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("ml.admin")) {
            plugin.logger.warn("Insufficient permissions");
            sender.sendMessage("[MongoLands] You don't have permission to use this command.");
        }
        if (args.length == 0) {
            plugin.logger.warn("No args provided.");
            sender.sendMessage("[MongoLands] Usage: /ml <sync|reload|testdb>");

        } else if (args[0].equals("reload")) {
            plugin.reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "[MongoLands] Reloaded config");

        } else if (args[0].equals("testdb") && args.length == 1) {
            sender.sendMessage(ChatColor.RED + "[MongoLands] Usage: /ml testdb <mongo|lands>");
        } else if (args[0].equals("testdb") && args.length == 2) {
            if (args[1].equalsIgnoreCase("mongo")) {
                plugin.logger.info("Testing MongoDB connection");
                boolean result = plugin.mongo.testMongo();
                if (result) {
                    sender.sendMessage(ChatColor.GREEN + "[MongoLands] MongoDB connection successful");
                } else {
                    sender.sendMessage(ChatColor.RED + "[MongoLands] MongoDB connection failed");
                }
            } else if (args[1].equalsIgnoreCase("lands")) {
                plugin.logger.info("Testing LandsDB connection");
                boolean result = plugin.landsDB.testLands();
                if (result) {
                    sender.sendMessage(ChatColor.GREEN + "[MongoLands] LandsDB connection successful");
                } else {
                    sender.sendMessage(ChatColor.RED + "[MongoLands] LandsDB connection failed");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "[MongoLands] Usage: /ml debug <testmongo|testlands>");
            }

        } else if (args[0].equals("sync") && args.length == 2) {
            String table = args[1].toLowerCase();
            if (table.equals("all")) {
                if (plugin.landsDB.syncAll()) {
                    sender.sendMessage(ChatColor.GREEN + "[MongoLands] Synced all tables");
                } else {
                    sender.sendMessage(ChatColor.RED + "[MongoLands] Failed to sync all tables");
                }
            } else if (Arrays.asList(plugin.mongo.collectionNames).contains(table)) {
                boolean success = switch (table) {
                    case "containers_world" -> plugin.landsDB.syncContainersWorld();
                    case "declarations" -> plugin.landsDB.syncDeclarations();
                    case "lands" -> plugin.landsDB.syncLands();
                    case "nations" -> plugin.landsDB.syncNations();
                    case "players" -> plugin.landsDB.syncPlayers();
                    case "relations" -> plugin.landsDB.syncRelations();
                    case "wars" -> plugin.landsDB.syncWars();
                    case "worlds" -> plugin.landsDB.syncWorlds();
                    default -> false;
                };
                if (success) {
                    sender.sendMessage(ChatColor.GREEN + "[MongoLands] Synced " + table);
                } else {
                    sender.sendMessage(ChatColor.RED + "[MongoLands] Failed to sync " + table);
                }
            }
        } else if (args[0].equals("sync")) {
            sender.sendMessage(ChatColor.RED + "[MongoLands] Usage: /ml sync <all|containers_world|declarations|lands|nations|players|relations|wars|worlds>");

        } else if (args[0].equals("syncrecord") && args.length == 3) {
            String table = args[1].toLowerCase();
            String target = args[2];
            boolean success = false;
            switch (table) {
                case "land" -> {
                    Land land = plugin.landsAPI.getLandByName(target);
                    if (land == null) {
                        sender.sendMessage(ChatColor.RED + "[MongoLands] Land not found");
                        return true;
                    }
                    success = plugin.landsDB.syncLandRecord(land);
                }
                case "nation" -> {
                    Nation nation = plugin.landsAPI.getNationByName(target);
                    if (nation == null) {
                        sender.sendMessage(ChatColor.RED + "[MongoLands] Nation not found");
                        return true;
                    }
                    success = plugin.landsDB.syncNationRecord(nation);
                }
                case "player" -> {
                    org.bukkit.OfflinePlayer player = plugin.getServer().getPlayer(target);
                    if (player == null) {
                        sender.sendMessage(ChatColor.RED + "[MongoLands] Player not found");
                        return true;
                    }
                    OfflinePlayer landsPlayer;
                    try {
                        landsPlayer = plugin.landsAPI.getOfflineLandPlayer(player.getUniqueId()).get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                    if (landsPlayer == null) {
                        sender.sendMessage(ChatColor.RED + "[MongoLands] Player not found");
                        return true;
                    }
                    success = plugin.landsDB.syncPlayerRecord(landsPlayer);
                }
                default ->
                        sender.sendMessage(ChatColor.RED + "[MongoLands] Usage: /ml syncrecord <land|nation|player>");
            }
            if (success) {
                sender.sendMessage(ChatColor.GREEN + "[MongoLands] Synced " + table + " " + target);
            } else {
                sender.sendMessage(ChatColor.RED + "[MongoLands] Failed to sync " + table + " " + target);
            }
        } else if (args[0].equals("syncrecord") && args.length == 2) {
            String table = args[1].toLowerCase();
            switch (table) {
                case "land":
                    sender.sendMessage(ChatColor.RED + "[MongoLands] Usage: /ml syncrecord land <land_name>");
                    break;
                case "nation":
                    sender.sendMessage(ChatColor.RED + "[MongoLands] Usage: /ml syncrecord nation <nation_name>");
                    break;
                case "player":
                    sender.sendMessage(ChatColor.RED + "[MongoLands] Usage: /ml syncrecord player <username>");
                    break;
                default:
                    sender.sendMessage(ChatColor.RED + "[MongoLands] Usage: /ml syncrecord <land|nation|player>");
                    break;
            }
        } else if (args[0].equals("syncrecord")) {
            sender.sendMessage(ChatColor.RED + "[MongoLands] Usage: /ml syncrecord <land|nation|player>");

        } else {
            sender.sendMessage(ChatColor.RED + "[MongoLands] Usage: /ml <sync|syncrecord|reload|testdb>");
        }
        return true;
    }
}
