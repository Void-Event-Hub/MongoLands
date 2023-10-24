package net.ollycodes.mongolands;

import com.mongodb.client.MongoDatabase;
import me.angeschossen.lands.api.LandsIntegration;
import net.ollycodes.mongolands.commands.MLCommand;
import net.ollycodes.mongolands.commands.TabComplete;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.util.Objects;

public final class MongoLands extends JavaPlugin {

    public final FileConfiguration config = getConfig();
    public final MongoDB mongo = new MongoDB(this);
    public final LandsDB landsDB = new LandsDB(this);
    public MongoDatabase database;
    public Connection landsDBConnection;
    public LandsIntegration landsAPI = LandsIntegration.of(this);
    public Logger logger = new Logger(this);
    public int syncTask;

    @Override
    public void onEnable() {
        Config.handleConfig(this);

        database = mongo.getDatabase();
        landsDBConnection = landsDB.getLandsDBConnection();

        Objects.requireNonNull(getCommand("ml")).setExecutor(new MLCommand(this));
        Objects.requireNonNull(getCommand("ml")).setTabCompleter(new TabComplete(this));

        syncTask = this.getServer().getScheduler().scheduleSyncRepeatingTask(this,() -> {
                if (landsDB.syncAll()) {
                    logger.info(
                        "Synced all tables, next full sync in " + config.getInt("sync_interval") + " seconds"
                    );
                } else {
                    logger.error("Failed to sync all tables");
                }
            }, 0L, config.getInt("sync_interval") * 20L
        );

        getServer().getPluginManager().registerEvents(new EventListener(this), this);
    }

    @Override
    public void onDisable() {
        try {
            landsDB.syncAll();
            landsDBConnection.close();
        } catch (Exception e) {
            logger.error("Failed to close connection to lands db: " + e.getMessage());
        }
        try {
            mongo.mongoClient.close();
        } catch (Exception e) {
            logger.error("Failed to close connection to mongo db: " + e.getMessage());
        }
    }
}
