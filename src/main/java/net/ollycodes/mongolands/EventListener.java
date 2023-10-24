package net.ollycodes.mongolands;

import me.angeschossen.lands.api.events.LandCreateEvent;
import me.angeschossen.lands.api.events.LandRenameEvent;
import me.angeschossen.lands.api.events.land.bank.LandBankBalanceChangedEvent;
import me.angeschossen.lands.api.events.land.spawn.LandSpawnRemoveEvent;
import me.angeschossen.lands.api.events.land.spawn.LandSpawnSetEvent;
import me.angeschossen.lands.api.events.nation.edit.NationCreateEvent;
import me.angeschossen.lands.api.events.nation.edit.NationRenameEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.concurrent.ExecutionException;

public class EventListener implements Listener {
    public final MongoLands plugin;
    public EventListener(MongoLands plugin) { this.plugin = plugin; }

    @EventHandler
    public void onLandBankBalanceChangedEvent(LandBankBalanceChangedEvent event) {
        plugin.landsDB.syncLandRecord(event.getLand());
    }

    @EventHandler
    public void onLandCreateEvent(LandCreateEvent event) {
        plugin.landsDB.syncLandRecord(event.getLand());
    }

    @EventHandler
    public void onLandRenameEvent(LandRenameEvent event) {
        plugin.landsDB.syncLandRecord(event.getLand());
    }

    @EventHandler
    public void onLandSpawnSetEvent(LandSpawnSetEvent event) {
        plugin.landsDB.syncLandRecord(event.getLand());
    }

    @EventHandler
    public void onLandSpawnRemoveEvent(LandSpawnRemoveEvent event) {
        plugin.landsDB.syncLandRecord(event.getLand());
    }

    @EventHandler
    public void onNationCreateEvent(NationCreateEvent event) {
        plugin.landsDB.syncNationRecord(event.getNation());
    }

    @EventHandler
    public void onNationRenameEvent(NationRenameEvent event) {
        plugin.landsDB.syncNationRecord(event.getNation());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) throws ExecutionException, InterruptedException {
        plugin.landsDB.syncPlayerRecord(plugin.landsAPI.getOfflineLandPlayer(event.getPlayer().getUniqueId()).get());
    }

}
