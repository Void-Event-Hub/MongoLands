package net.ollycodes.mongolands;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Updates;
import me.angeschossen.lands.api.land.Land;
import me.angeschossen.lands.api.nation.Nation;
import me.angeschossen.lands.api.player.OfflinePlayer;
import org.bson.BsonArray;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.sqlite.SQLiteConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static com.mongodb.client.model.Filters.*;


public class LandsDB {

    public final MongoLands plugin;
    public LandsDB(MongoLands plugin) { this.plugin = plugin; }
    public Connection landsDBConnection;


    public interface OverrideFunction {
        Object run(Object id, ResultSet result, String[][] fields) throws ExecutionException, InterruptedException;
    }

    public Connection getLandsDBConnection() {
        if (landsDBConnection != null) {
            return landsDBConnection;
        }
        try {
            plugin.logger.info("Connecting to LandsDB at " + plugin.getConfig().getString("lands_db_path"));
            String connectionString = plugin.getConfig().getString("lands_db_path");
            SQLiteConfig config = new SQLiteConfig();
            config.setReadOnly(true);
            config.setSharedCache(true);
            landsDBConnection = DriverManager.getConnection("jdbc:sqlite:" + connectionString, config.toProperties());
            assert testLands();
            plugin.logger.success("Connected to LandsDB");
        } catch (Exception e) {
            plugin.logger.error("Error connecting to LandsDB: " + e.getMessage());
            throw new RuntimeException("Error connecting to LandsDB: " + e.getMessage());
        }
        return landsDBConnection;
    }

    public boolean testLands() {
        try {
            String query = "SELECT name FROM sqlite_master WHERE type='table';";
            ResultSet result = landsDBConnection.createStatement().executeQuery(query);
            while (result.next()) {
                plugin.logger.info("Tables: " + result.getString("name"));
            }
            plugin.logger.success("Connected to LandsDB");
            return true;
        } catch (Exception e) {
            plugin.logger.error("Error connecting to LandsDB: " + e.getMessage());
            return false;
        }
    }

    public Document buildDocument(
            Object id,
            ResultSet result,
            String[][] fields,
            @Nullable Map<String, OverrideFunction> overrides
    ) throws SQLException, ExecutionException, InterruptedException {
        Document document = new Document();
        for (String[] field : fields) {
            Object data = null;
            if (overrides != null && overrides.containsKey(field[1])) {
                data = overrides.get(field[1]).run(id, result, fields);
                if (data != null) {
                    document.append(field[1], data);
                }
            }
            if (data == null) {
                if (result.getString(field[1]) == null) {
                    document.append(field[1], null);
                } else if (field[0].equalsIgnoreCase("string")) {
                    document.append(field[1], result.getString(field[1]));
                } else if (field[0].equalsIgnoreCase("int")) {
                    document.append(field[1], result.getInt(field[1]));
                } else if (field[0].equalsIgnoreCase("float")) {
                    document.append(field[1], result.getFloat(field[1]));
                } else if (field[0].equalsIgnoreCase("long")) {
                    document.append(field[1], result.getLong(field[1]));
                } else if (field[0].equalsIgnoreCase("document")) {
                    document.append(field[1], Document.parse(result.getString(field[1])));
                } else if (field[0].equalsIgnoreCase("array")) {
                    document.append(field[1], BsonArray.parse(result.getString(field[1])));
                } else if (field[0].equalsIgnoreCase("bool")) {
                    document.append(field[1], result.getBoolean(field[1]));
                } else {
                    plugin.logger.error("Unknown field type: " + field[0]);
                }
            }
        }
        return document;
    }

    public Bson buildUpdates(
            Object id,
            ResultSet result,
            String[][] fields,
            @Nullable Map<String, OverrideFunction> overrides
    ) throws SQLException, ExecutionException, InterruptedException {
        ArrayList<Bson> updates = new ArrayList<>();
        for (String[] field : fields) {
            Object data = null;
            if (overrides != null && overrides.containsKey(field[1])) {
                data = overrides.get(field[1]).run(id, result, fields);
                if (data != null) {
                    updates.add(Updates.set(field[1], data));
                }
            }
            if (data == null) {
                if (result.getString(field[1]) == null) {
                    updates.add(Updates.set(field[1], null));
                } else if (field[0].equalsIgnoreCase("string")) {
                    updates.add(Updates.set(field[1], result.getString(field[1])));
                } else if (field[0].equalsIgnoreCase("int")) {
                    updates.add(Updates.set(field[1], result.getInt(field[1])));
                } else if (field[0].equalsIgnoreCase("float")) {
                    updates.add(Updates.set(field[1], result.getFloat(field[1])));
                } else if (field[0].equalsIgnoreCase("long")) {
                    updates.add(Updates.set(field[1], result.getLong(field[1])));
                } else if (field[0].equalsIgnoreCase("document")) {
                    updates.add(Updates.set(field[1], Document.parse(result.getString(field[1]))));
                } else if (field[0].equalsIgnoreCase("array")) {
                    updates.add(Updates.set(field[1], BsonArray.parse(result.getString(field[1]))));
                } else if (field[0].equalsIgnoreCase("bool")) {
                    updates.add(Updates.set(field[1], result.getBoolean(field[1])));
                } else {
                    plugin.logger.error("Unknown field type: " + field[0]);
                }
            }
        }
        return Updates.combine(updates);
    }


    public boolean syncRecord(
            String tableName,
            @NotNull MongoCollection<Document> collection,
            String[][] fields,
            Object id
    ) {
        return syncRecord(tableName, collection, fields, id, null);
    }

    public boolean syncRecord(
            String tableName,
            @NotNull MongoCollection<Document> collection,
            String[][] fields,
            Object id,
            @Nullable Map<String, OverrideFunction> overrides
    ) {
        try {
            String query = String.format("SELECT * FROM lands_%s WHERE %s = \"%s\"", tableName, fields[0][1], id);
            ResultSet result = landsDBConnection.createStatement().executeQuery(query);
            Document document;
            document = collection.find(eq(fields[0][1], id)).first();
            if (document == null) {
                document = buildDocument(id, result, fields, overrides);
                collection.insertOne(document);
            } else {
                Bson updates = buildUpdates(id, result, fields, overrides);
                collection.updateOne(document, updates);
            }
        } catch (Exception e) {
            plugin.logger.error("Error syncing " + tableName + ": " + e.getMessage());
            return false;
        }
        plugin.logger.success(
            "Updated record in " + tableName + " in MongoDB"
        );
        return true;
    }

    public boolean syncLandRecord(Land land) {
        String[][] fields = {
            {"int", "id"},
            {"string", "world"},
            {"string", "nuid"},
            {"int", "type"},
            {"string", "category"},
            {"string", "name"},
            {"document", "area"},
            {"document", "members"},
            {"long", "created"},
            {"float", "balance"},
            {"string", "title"},
            {"document", "spawn"},
            {"array", "inbox"},
            {"document", "vs"},
            {"string", "shield"},
            {"document", "stats"},
            {"string", "icon"},
            {"string", "storage"}
        };

        Map<String, OverrideFunction> overrides = Map.of(
            "type", (id, result, field) -> land.getLandType().id,
            "category", (id, result, field) -> {
                if (land.getCategory() == null) {
                    return null;
                }
                return land.getCategory().getName();
            },
            "name", (id, result, field) -> land.getName(),
            "balance", (id, result, field) -> land.getBalance()
        );

        return syncRecord("lands", plugin.mongo.lands, fields, land.getId(), overrides);
    }


    public boolean syncNationRecord(Nation nation) {
        String[][] fields = {
            {"int", "id"},
            {"string", "name"},
            {"int", "capital"},
            {"long", "created"},
            {"float", "taxes"},
            {"array", "lands"},
            {"array", "inbox"},
            {"long", "shield"},
            {"document", "stats"},
            {"document", "effects"}
        };

        Map<String, OverrideFunction> overrides = Map.of(
            "name", (id, result, field) -> nation.getName(),
            "capital", (id, result, field) -> nation.getCapital().getId(),
            "lands", (id, result, field) -> nation.getLands().stream().map(Land::getId).toList()
        );
        return syncRecord("nations", plugin.mongo.nations, fields, nation.getId(), overrides);
    }

    public boolean syncPlayerRecord(OfflinePlayer player) {
        String[][] fields = {
            {"string", "id"},
            {"string", "username"},
            {"array", "lands"},
            {"int", "edit"},
            {"array", "invites"},
            {"document", "vs"},
            {"string", "flags"}
        };

        Map<String, OverrideFunction> overrides = Map.of(
            "username", (id, result, field) -> plugin.getServer().getOfflinePlayer(player.getUID()).getName()
        );
        return syncRecord("players", plugin.mongo.players, fields, player.getUID().toString(), overrides);
    }


    public boolean syncTable(String tableName, @NotNull MongoCollection<Document> collection, String[][] fields) {
        return syncTable(tableName, collection, fields, null);
    }


    public boolean syncTable(
            String tableName,
            @NotNull MongoCollection<Document> collection,
            String[][] fields,
            @Nullable Map<String, OverrideFunction> overrides
    ) {
        int created = 0;
        int updated = 0;
        int deleted;
        try {
            String query = "SELECT * FROM lands_" + tableName;
            ResultSet result = landsDBConnection.createStatement().executeQuery(query);
            ArrayList<Object> ids = new ArrayList<>();
            while (result.next()) {
                Document document;
                Object id;
                if (fields[0][0].equals("int")) {
                    id = result.getInt(fields[0][1]);
                } else {
                    id = result.getString(fields[0][1]);
                }
                document = collection.find(eq(fields[0][1], id)).first();
                ids.add(id);
                if (document == null) {
                    document = buildDocument(id, result, fields, overrides);
                    collection.insertOne(document);
                    created++;
                } else {
                    Bson updates = buildUpdates(id, result, fields, overrides);
                    collection.updateOne(document, updates);
                    updated++;
                }
            }
            deleted = (int) collection.countDocuments() - ids.size();
            collection.deleteMany(not(in(fields[0][1], ids)));
        } catch (Exception e) {
            plugin.logger.error("Error syncing " + tableName + ": " + e.getMessage());
            return false;
        }
        plugin.logger.success(
            "Created " + created + ", updated " + updated + ", and deleted " + deleted + " " + tableName + " in MongoDB"
        );
        return true;
    }


    public boolean syncContainersWorld() {
        String[][] fields = {
            {"int", "id"},
            {"array", "chunks"},
            {"document", "areas"},
            {"document", "blocks"}
        };
        return syncTable("containers_world", plugin.mongo.containers_world, fields);
    }


    public boolean syncDeclarations() {
        String[][] fields = {
            {"int", "id"},
            {"document", "teams"},
            {"float", "tribute"},
            {"long", "sent"},
            {"bool", "mutual"}
        };
        return syncTable("declarations", plugin.mongo.declarations, fields);
    }


    public boolean syncLands() {
        String[][] fields = {
            {"int", "id"},
            {"string", "world"},
            {"string", "nuid"},
            {"int", "type"},
            {"string", "category"},
            {"string", "name"},
            {"document", "area"},
            {"document", "members"},
            {"long", "created"},
            {"float", "balance"},
            {"string", "title"},
            {"document", "spawn"},
            {"array", "inbox"},
            {"document", "vs"},
            {"string", "shield"},
            {"document", "stats"},
            {"string", "icon"},
            {"string", "storage"}
        };

        Map<String, OverrideFunction> overrides = Map.of(
            "type", (id, result, field) -> {
                Land land = plugin.landsAPI.getLandById((int) id);
                if (land == null) {
                    return null;
                }
                return land.getLandType().id;
            },
            "category", (id, result, field) -> {
                Land land = plugin.landsAPI.getLandById((int) id);
                if (land == null || land.getCategory() == null) {
                    return null;
                }
                return land.getCategory().getName();
            },
            "name", (id, result, field) -> {
                Land land = plugin.landsAPI.getLandById((int) id);
                if (land == null) {
                    return null;
                }
                return land.getName();
            },
            "balance", (id, result, field) -> {
                Land land = plugin.landsAPI.getLandById((int) id);
                if (land == null) {
                    return null;
                }
                return land.getBalance();
            }
        );
        return syncTable("lands", plugin.mongo.lands, fields, overrides);
    }

    public boolean syncNations() {
        String[][] fields = {
            {"int", "id"},
            {"string", "name"},
            {"int", "capital"},
            {"long", "created"},
            {"float", "taxes"},
            {"array", "lands"},
            {"array", "inbox"},
            {"long", "shield"},
            {"document", "stats"},
            {"document", "effects"}
        };

        Map<String, OverrideFunction> overrides = Map.of(
            "name", (id, result, field) -> {
                Nation nation = plugin.landsAPI.getNationById((int) id);
                if (nation == null) {
                    return null;
                }
                return nation.getName();
            },
            "capital", (id, result, field) -> {
                Nation nation = plugin.landsAPI.getNationById((int) id);
                if (nation == null) {
                    return null;
                }
                return nation.getCapital().getId();
            },
            "lands", (id, result, field) -> {
                Nation nation = plugin.landsAPI.getNationById((int) id);
                if (nation == null) {
                    return null;
                }
                return nation.getLands().stream().map(Land::getId).toList();
            }
        );
        return syncTable("nations", plugin.mongo.nations, fields, overrides);
    }


    public boolean syncPlayers() {
        String[][] fields = {
            {"string", "id"},
            {"string", "username"},
            {"array", "lands"},
            {"int", "edit"},
            {"array", "invites"},
            {"document", "vs"},
            {"string", "flags"}
        };

        Map<String, OverrideFunction> overrides = Map.of(
            "username", (id, result, field) -> plugin.getServer().getOfflinePlayer(UUID.fromString((String) id)).getName()
        );
        return syncTable("players", plugin.mongo.players, fields, overrides);
    }


    public boolean syncRelations() {
        String[][] fields = {
            {"int", "holder_a"},
            {"int", "holder_b"},
            {"string", "type"}
        };

        int created = 0;
        int updated = 0;
        int deleted;
        try {
            String query = "SELECT * FROM lands_relations";
            ResultSet result = landsDBConnection.createStatement().executeQuery(query);
            ArrayList<String> holders = new ArrayList<>();
            while (result.next()) {
                String combinedHolder = result.getInt("holder_a") + ":" + result.getInt("holder_b");
                holders.add(combinedHolder);
                Document document = plugin.mongo.relations.find(eq("holder", combinedHolder)).first();
                if (document == null) {
                    plugin.mongo.relations.insertOne(
                        buildDocument(combinedHolder, result, fields, null).append("holder", combinedHolder)
                    );
                    created++;
                } else {
                    plugin.mongo.relations.updateOne(
                        document,
                        Updates.combine(
                            Updates.set("holder", combinedHolder),
                            buildUpdates(combinedHolder, result, fields, null)
                        )
                    );
                    updated++;
                }
            }
            deleted = (int) plugin.mongo.relations.countDocuments() - holders.size();
            plugin.mongo.relations.deleteMany(not(in("holder", holders)));
        } catch (Exception e) {
            plugin.logger.error("Error syncing relations: " + e.getMessage());
            return false;
        }
        plugin.logger.success(
            "Created " + created + ", updated " + updated + ", and deleted " + deleted + " relations in MongoDB"
        );
        return true;
    }


    public boolean syncWars() {
        String[][] fields = {
            {"int", "id"},
            {"document", "teams"},
            {"long", "started"},
            {"bool", "tribute"},
            {"string", "captures"}
        };
        return syncTable("wars", plugin.mongo.wars, fields);
    }


    public boolean syncWorlds() {
        String[][] fields = {
            {"string", "uid"},
            {"array", "roleflags"},
            {"array", "naturalflags"}
        };
        return syncTable("worlds", plugin.mongo.worlds, fields);
    }


    public boolean syncAll() {
        boolean result = syncContainersWorld();
        result = syncDeclarations() && result;
        result = syncLands() && result;
        result = syncNations() && result;
        result = syncPlayers() && result;
        result = syncRelations() && result;
        result = syncWars() && result;
        result = syncWorlds() && result;
        return result;
    }
}
