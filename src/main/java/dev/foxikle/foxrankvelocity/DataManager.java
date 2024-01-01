package dev.foxikle.foxrankvelocity;

import javax.annotation.Nullable;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class DataManager {
    private final FoxRankVelocity plugin;

    private final String host;
    private final String port;
    private final String database;
    private final String username;
    private final String password;
    private Connection connection;

    public DataManager(FoxRankVelocity plugin) {
        this.plugin = plugin;
        this.host = plugin.getConfig().getString("sqlHost");
        this.port = plugin.getConfig().getString("sqlPort");
        this.database = plugin.getConfig().getString("sqlName");
        this.username = plugin.getConfig().getString("sqlUsername");
        this.password = plugin.getConfig().getString("sqlPassword");
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            // Or "com.mysql.jdbc.Driver" for some old mysql driver
        } catch(ClassNotFoundException e) {
            plugin.getLogger().error("Failed to load driver");
            e.printStackTrace();
        }
    }


    protected boolean isConnected() {
        return (connection != null);
    }

    protected void connect() throws ClassNotFoundException, SQLException {
        if (!isConnected())
            connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false", username, password);
        plugin.getProxy().getScheduler().buildTask(plugin, () -> {
            if(isConnected()){
                try {
                    getConnection().prepareStatement("SELECT * FROM foxrankranks").executeQuery();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }).repeat(3, TimeUnit.MINUTES).schedule();
    }

    protected void disconnect() {
        if (isConnected()) {
            try {
                getConnection().close();
            } catch (SQLException e) {
                plugin.getLogger().error("An error occoured whilst disconnecting from the database. Please report the following stacktrace to Foxikle: \n");
                e.printStackTrace();
            }
        }
    }

    protected Connection getConnection() {
        try {
            if (connection.isClosed()){
                connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false", username, password);
                plugin.getLogger().info("Database connection restablished!");
                return connection;
            }
        } catch (SQLException e) {
            plugin.getLogger().error("An error occoured whilst connecting from the database. Please report the following stacktrace to Foxikle: \n");
            e.printStackTrace();
        }
        return connection;
    }

    protected List<String> getRanks(){
        List<String> returnme = new ArrayList<>();
        try {
            PreparedStatement ps = getConnection().prepareStatement("SELECT data FROM foxrankranks");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String str = rs.getString("data");
                if (str != null) {
                    returnme.add(str);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().error("An error occoured whilst fetching data from the database. Please report the following stacktrace to Foxikle: \n");
            Arrays.stream(e.getStackTrace()).forEach(stackTraceElement -> plugin.getLogger().error(stackTraceElement.toString()));
        }
        return returnme;
    }

    public void setupPlayer(UUID uuid, String rankID, String name) {
        try {
            PreparedStatement ps = getConnection().prepareStatement("INSERT IGNORE INTO foxrankplayerdata (name, uuid, rankid) VALUES (?,?,?)");
            ps.setString(1, name);
            ps.setString(2, uuid.toString());
            ps.setString(3, rankID);
            ps.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().error("An error occoured whilst fetching data from the database. Please report the following stacktrace to Foxikle: \n", e);
        }
    }

    public void setupRanks() {
        List<String> ranks = getRanks();
        Map<String, Integer> powerlevelMappy = new HashMap<>();
        Map<Rank, Integer> rankMappy = new HashMap<>();
        for (String str : ranks) {
            Rank rank = Rank.of(str);
            if(rank.getId() != null) {
                powerlevelMappy.put(rank.getId(), rank.getPowerlevel());
                rankMappy.put(rank, rank.getPowerlevel());
            }
        }
        plugin.ranks = getSortedRanks(rankMappy);
        plugin.powerLevels = sortByValue(powerlevelMappy);
    }

    private Map<String, Rank> getSortedRanks(Map<Rank, Integer> unsortMap) {
        List<Map.Entry<Rank, Integer>> list = new LinkedList<>(unsortMap.entrySet());
        list.sort(Map.Entry.comparingByValue());
        Collections.reverse(list);
        Map<String, Rank> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<Rank, Integer> entry : list) {
            sortedMap.put(entry.getKey().getId(), entry.getKey());
        }
        return sortedMap;
    }

    private Map<String, Integer> sortByValue(Map<String, Integer> unsortMap) {
        List<Map.Entry<String, Integer>> list = new LinkedList<>(unsortMap.entrySet());
        list.sort(Map.Entry.comparingByValue());
        Collections.reverse(list);
        Map<String, Integer> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }

    protected void addRank(String id, String data) {
        try {
            PreparedStatement ps = getConnection().prepareStatement("INSERT IGNORE INTO foxrankranks (data, id) VALUES (?,?)");
            ps.setString(1, data);
            ps.setString(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().error("An error occoured whilst fetching data from the database. Please report the following stacktrace to Foxikle: \n");
            Arrays.stream(e.getStackTrace()).forEach(stackTraceElement -> plugin.getLogger().error(stackTraceElement.toString()));
        }
    }

    protected void removeRank(String id) {
        try {
            PreparedStatement ps = getConnection().prepareStatement("DELETE FROM foxrankranks WHERE id = ?");
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().error("An error occoured whilst fetching data from the database. Please report the following stacktrace to Foxikle: \n", e);
        }
    }

    protected void updateRank(String id, String data) {
        try {
            PreparedStatement ps = getConnection().prepareStatement("UPDATE foxrankranks SET data = ? WHERE id = ?");
            ps.setString(1, data);
            ps.setString(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().error("An error occoured whilst fetching data from the database. Please report the following stacktrace to Foxikle: \n");
            e.printStackTrace();
        }
    }


    protected Set<UUID> getStoredBannedPlayers() {
        Set<UUID> returnme = new HashSet<>();
        try {
            PreparedStatement ps = getConnection().prepareStatement("SELECT uuids FROM foxrankbannedplayers WHERE id=?");
            ps.setString(1, "bannedPlayers");
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String str = rs.getString("uuids");
                if (!str.isEmpty()) {
                    List<String> list1 = List.of(str.split(":"));

                    for (String s : list1) {
                        returnme.add(UUID.fromString(s));
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().error("An error occoured whilst fetching data from the database. Please report the following stacktrace to Foxikle: \n" + Arrays.toString(e.getStackTrace()));
        }
        return returnme;
    }

    protected void setStoredBannedPlayers(Set<UUID> players) {
        List<String> uuids = new ArrayList<>();
        try {
            for (UUID u : players) {
                uuids.add(u.toString());
            }
            String str = String.join(":", uuids);
            PreparedStatement ps = getConnection().prepareStatement("UPDATE foxrankbannedplayers SET uuids = ? WHERE id = ?");
            ps.setString(1, str);
            ps.setString(2, "bannedPlayers");
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().error("An error occoured whilst fetching data from the database. Please report the following stacktrace to Foxikle: \n" + Arrays.toString(e.getStackTrace()));
        }
    }

    protected void setStoredRank(UUID uuid, Rank rank) {
        try {
            PreparedStatement ps = getConnection().prepareStatement("UPDATE foxrankplayerdata SET rankid = ? WHERE uuid=?");
            ps.setString(1, rank.getId());
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().error("An error occoured whilst fetching data from the database. Please report the following stacktrace to Foxikle: \n" + Arrays.toString(e.getStackTrace()));
        }
    }

    protected void setStoredVanishedState(UUID uuid, boolean isVanished) {
        try {
            PreparedStatement ps = getConnection().prepareStatement("UPDATE foxrankplayerdata SET isvanished = ? WHERE uuid = ?");
            ps.setBoolean(1, isVanished);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().error("An error occoured whilst fetching data from the database. Please report the following stacktrace to Foxikle: \n" + Arrays.toString(e.getStackTrace()));
        }
    }

    protected void setStoredNicknameState(UUID uuid, boolean isNicked) {
        try {
            PreparedStatement ps = getConnection().prepareStatement("UPDATE foxrankplayerdata SET isnicked = ? WHERE uuid = ?");
            ps.setBoolean(1, isNicked);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().error("An error occoured whilst fetching data from the database. Please report the following stacktrace to Foxikle: \n" + Arrays.toString(e.getStackTrace()));
        }
    }

    protected void setStoredMuteState(UUID uuid) {
        try {
            PreparedStatement ps = getConnection().prepareStatement("UPDATE foxrankplayerdata SET ismuted = ? WHERE uuid = ?");
            ps.setBoolean(1, false);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().error("An error occoured whilst fetching data from the database. Please report the following stacktrace to Foxikle: \n" + Arrays.toString(e.getStackTrace()));
        }
    }

    protected void setStoredBanState(UUID uuid) {
        try {
            PreparedStatement ps = getConnection().prepareStatement("UPDATE foxrankplayerdata SET isbanned = ? WHERE uuid = ?");
            ps.setBoolean(1, false);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().error("An error occoured whilst fetching data from the database. Please report the following stacktrace to Foxikle: \n" + Arrays.toString(e.getStackTrace()));
        }
    }

    protected void setStoredMuteData(UUID uuid, String reason, Instant duration) {
        try {
            PreparedStatement ps = getConnection().prepareStatement("UPDATE foxrankplayerdata SET ismuted = ?, muteduration = ?, mutereason = ? WHERE uuid=?");
            ps.setBoolean(1, true);
            ps.setString(2, duration == null ? null : duration.toString());
            ps.setString(3, reason);
            ps.setString(4, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().error("An error occoured whilst fetching data from the database. Please report the following stacktrace to Foxikle: \n" + Arrays.toString(e.getStackTrace()));
        }
    }

    protected void setStoredBanData(UUID uuid, String reason, @Nullable Instant duration, String ID) {
        try {
            PreparedStatement ps = getConnection().prepareStatement("UPDATE foxrankplayerdata SET isbanned = ?, banduration = ?, banreason = ?, banid = ? WHERE uuid = ?");
            ps.setBoolean(1, true);
            ps.setString(2, (duration == null) ? null : duration.toString());
            ps.setString(3, reason);
            ps.setString(4, ID);
            ps.setString(5, uuid.toString());

            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().error("An error occoured whilst fetching data from the database. Please report the following stacktrace to Foxikle: \n" + Arrays.toString(e.getStackTrace()));
        }
    }

    protected void setStoredNicknameData(UUID uuid, boolean isNicked, Rank rank, String newNick, String skin) {
        try {
            PreparedStatement ps = getConnection().prepareStatement("UPDATE foxrankplayerdata SET isnicked = ?, nickname = ?, nicknamerank = ?, nicknameskin = ? WHERE uuid = ?");
            ps.setBoolean(1, isNicked);
            ps.setString(2, newNick);
            ps.setString(3, rank.getId());
            ps.setString(4, skin);
            ps.setString(5, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().error("An error occoured whilst fetching data from the database. Please report the following stacktrace to Foxikle: \n" + Arrays.toString(e.getStackTrace()));
        }
    }

    protected Rank getStoredRank(UUID uuid) {
        try {
            PreparedStatement ps = getConnection().prepareStatement("SELECT rankid FROM foxrankplayerdata WHERE uuid=?");
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Rank.fromID(rs.getString("rankid"));
            }
        } catch (SQLException e) {
            plugin.getLogger().error("An error occoured whilst fetching data from the database. Please report the following stacktrace to Foxikle: \n" + Arrays.toString(e.getStackTrace()));
        }
        return plugin.getDefaultRank();
    }

    protected boolean getStoredMuteStatus(UUID uuid) {
        try {
            PreparedStatement ps = getConnection().prepareStatement("SELECT ismuted FROM foxrankplayerdata WHERE uuid=?");
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("ismuted");
            }
        } catch (SQLException e) {
            plugin.getLogger().error("An error occoured whilst fetching data from the database. Please report the following stacktrace to Foxikle: \n" + Arrays.toString(e.getStackTrace()));
        }
        return false;
    }

    protected boolean getStoredBanStatus(UUID uuid) {
        try {
            PreparedStatement ps = getConnection().prepareStatement("SELECT isbanned FROM foxrankplayerdata WHERE uuid = ?");
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("isbanned");
            }
        } catch (SQLException e) {
            plugin.getLogger().error("An error occoured whilst fetching data from the database. Please report the following stacktrace to Foxikle: \n" + Arrays.toString(e.getStackTrace()));
        }
        return false;
    }

    protected boolean getStoredNicknameStatus(UUID uuid) {
        try {
            PreparedStatement ps = getConnection().prepareStatement("SELECT isnicked FROM foxrankplayerdata WHERE uuid=?");
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("isnicked");
            }
        } catch (SQLException e) {
            plugin.getLogger().error("An error occoured whilst fetching data from the database. Please report the following stacktrace to Foxikle: \n" + Arrays.toString(e.getStackTrace()));
        }
        return false;
    }

    protected boolean getStoredVanishStatus(UUID uuid) {
        try {
            PreparedStatement ps = getConnection().prepareStatement("SELECT isvanished FROM foxrankplayerdata WHERE uuid = ?");
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("isvanished");
            }
        } catch (SQLException e) {
            plugin.getLogger().error("An error occoured whilst fetching data from the database. Please report the following stacktrace to Foxikle: \n" + Arrays.toString(e.getStackTrace()));
        }
        return false;
    }

    protected String getStoredNickname(UUID uuid) {
        try {
            PreparedStatement ps = getConnection().prepareStatement("SELECT nickname FROM foxrankplayerdata WHERE uuid = ?");
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("nickname");
            }
        } catch (SQLException e) {
            plugin.getLogger().error("An error occoured whilst fetching data from the database. Please report the following stacktrace to Foxikle: \n" + Arrays.toString(e.getStackTrace()));
        }
        return "";
    }

    protected String getStoredNicknameSkin(UUID uuid) {
        try {
            PreparedStatement ps = getConnection().prepareStatement("SELECT nicknameskin FROM foxrankplayerdata WHERE uuid = ?");
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("nicknameskin");
            }
        } catch (SQLException e) {
            plugin.getLogger().error("An error occoured whilst fetching data from the database. Please report the following stacktrace to Foxikle: \n" + Arrays.toString(e.getStackTrace()));
        }
        return "";
    }

    protected Rank getStoredNicknameRank(UUID uuid) {
        try {
            PreparedStatement ps = getConnection().prepareStatement("SELECT nicknamerank FROM foxrankplayerdata WHERE uuid = ?");
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Rank.fromID(rs.getString("nicknamerank"));
            }
        } catch (SQLException e) {
            plugin.getLogger().error("An error occoured whilst fetching data from the database. Please report the following stacktrace to Foxikle: \n" + Arrays.toString(e.getStackTrace()));
        }
        return plugin.getDefaultRank();
    }

    protected String getStoredMuteDuration(UUID uuid) {
        try {
            PreparedStatement ps = getConnection().prepareStatement("SELECT muteduration FROM foxrankplayerdata WHERE uuid = ?");
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("muteduration");
            }
        } catch (SQLException e) {
            plugin.getLogger().error("An error occoured whilst fetching data from the database. Please report the following stacktrace to Foxikle: \n" + Arrays.toString(e.getStackTrace()));
        }
        return null;
    }

    protected String getStoredMuteReason(UUID uuid) {
        try {
            PreparedStatement ps = getConnection().prepareStatement("SELECT mutereason FROM foxrankplayerdata WHERE uuid = ?");
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("mutereason");
            }
        } catch (SQLException e) {
            plugin.getLogger().error("An error occoured whilst fetching data from the database. Please report the following stacktrace to Foxikle: \n" + Arrays.toString(e.getStackTrace()));
        }
        return null;
    }

    protected String getStoredBanID(UUID uuid) {
        try {
            PreparedStatement ps = getConnection().prepareStatement("SELECT banid FROM foxrankplayerdata WHERE uuid = ?");
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("banid");
            }
        } catch (SQLException e) {
            plugin.getLogger().error("An error occoured whilst fetching data from the database. Please report the following stacktrace to Foxikle: \n" + Arrays.toString(e.getStackTrace()));
        }
        return null;
    }

    protected String getStoredBanDuration(UUID uuid) {
        try {
            PreparedStatement ps = getConnection().prepareStatement("SELECT banduration FROM foxrankplayerdata WHERE uuid = ?");
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("banduration");
            }
        } catch (SQLException e) {
            plugin.getLogger().error("An error occoured whilst fetching data from the database. Please report the following stacktrace to Foxikle: \n" + Arrays.toString(e.getStackTrace()));
        }
        return null;
    }

    protected String getStoredBanReason(UUID uuid) {
        try {
            PreparedStatement ps = getConnection().prepareStatement("SELECT banreason FROM foxrankplayerdata WHERE uuid = ?");
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("banreason");
            }
        } catch (SQLException e) {
            plugin.getLogger().error("An error occoured whilst fetching data from the database. Please report the following stacktrace to Foxikle: \n" + Arrays.toString(e.getStackTrace()));
        }
        return null;
    }

    protected List<String> getNames() {
        List<String> returnme = new ArrayList<>();
        try {
            PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM foxrankplayerdata");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if (rs.getString("name") != null) {
                    returnme.add(rs.getString("name"));
                }
            }
            return returnme;
        } catch (SQLException e) {
            plugin.getLogger().error("An error occoured whilst fetching data from the database. Please report the following stacktrace to Foxikle: \n" + Arrays.toString(e.getStackTrace()));
        }
        return new ArrayList<>();
    }

    protected List<UUID> getUUIDs() {
        List<UUID> returnme = new ArrayList<>();
        try {
            PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM foxrankplayerdata");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if (rs.getString("uuid") != null) {
                    returnme.add(UUID.fromString(rs.getString("uuid")));
                }
            }
            return returnme;
        } catch (SQLException e) {
            plugin.getLogger().error("An error occoured whilst fetching data from the database. Please report the following stacktrace to Foxikle: \n" + Arrays.toString(e.getStackTrace()));
        }
        return new ArrayList<>();
    }
}
