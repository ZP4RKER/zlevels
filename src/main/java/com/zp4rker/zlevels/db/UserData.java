package com.zp4rker.zlevels.db;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTable;
import com.zp4rker.zlevels.ZLevels;
import com.zp4rker.zlevels.util.AutoRole;
import com.zp4rker.zlevels.util.Config;
import com.zp4rker.zlevels.util.ZLogger;
import com.zp4rker.zlevels.util.LevelsUtil;
import net.dv8tion.jda.core.entities.User;

import java.util.List;
import java.util.concurrent.Executors;

/**
 * @author ZP4RKER
 */
@DatabaseTable(tableName = "USER_DATA")
public class UserData {

    @DatabaseField(generatedId = true, unique = true) private int id;

    @DatabaseField(canBeNull = false, unique = true) private String userId;

    @DatabaseField(canBeNull = false) private String avatarUrl;

    @DatabaseField(canBeNull = false) private long totalXp = 0;

    @DatabaseField(canBeNull = false) private int level = 0;

    // Temp data
    private static UserData data = null;

    public int getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAvatarUrl() {
        // Get User
        User user = ZLevels.jda.getUserById(getUserId());
        // Return avatar url
        return user.getEffectiveAvatarUrl();
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public long getTotalXp() {
        return totalXp;
    }

    public void setTotalXp(long totalXp) {
        // Set to field
        this.totalXp = totalXp;
        // Get and set levels
        setLevel(LevelsUtil.xpToLevels(totalXp));
    }

    public int getLevel() {
        return level;
    }

    private void setLevel(int level) {
        // Check if new level
        if (level > this.level) {
            // Send info
            ZLogger.info(getUserId() + " just levelled up to " + level + "!");
            // Get User
            User user = ZLevels.jda.getUserById(getUserId());
            // Catch errors
            try {
                // Open DM
                user.openPrivateChannel().complete();
            } catch (Exception e) {
                // Send warning
                ZLogger.warn("Couldn't open DM or already open!");
            }
            // Send DM
            user.getPrivateChannel().sendMessage("Congratulations, you are now level " + level + "!").queue();
            // Check if autorole enabled
            if (Config.AUTOROLE_ENABLED) {
                // Auto-assign role
                AutoRole.assignRole(this);
            }
        }
        // Set level
        this.level = level;
    }

    public void save() {
        // Get current data
        UserData current = this;
        // Update avatar url
        current.setAvatarUrl(current.getAvatarUrl());
        // Run asynchronously
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                // Get the connection
                ConnectionSource source = Database.getConnection();
                // Get the Dao
                Dao<UserData, String> db = DaoManager.createDao(source, UserData.class);
                // Save the record
                db.createOrUpdate(current);
                // Close the connection
                source.close();
            } catch (Exception e) {
                // Send warning
                ZLogger.warn("Could not save UserData for " + getUserId() + "!");
            }
        });
    }

    public void delete() {
        // Get current data
        UserData current = this;
        // Run asynchronously
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                // Get the connection
                ConnectionSource source = Database.getConnection();
                // Get the Dao
                Dao<UserData, String> db = DaoManager.createDao(source, UserData.class);
                // Delete the record
                db.delete(current);
                // Close the connection
                source.close();
            } catch (Exception e) {
                // Send warning
                ZLogger.warn("Colud not delete UserData for " + getUserId() + "!");
            }
        });
    }

    public static UserData fromId(String userId) {
        // Get data
        UserData data = byId(userId);
        // Set data to null
        UserData.data = null;
        // Return data
        return data;
    }

    private static UserData byId(String id) {
        try {
            // Get the connection
            ConnectionSource source = Database.getConnection();
            // Get the Dao
            Dao<UserData, String> db = DaoManager.createDao(source, UserData.class);
            // Search
            data = db.queryForEq("userId", id).get(0);
            // Close connection
            source.close();
        } catch (Exception e) {
            // Send warning
            ZLogger.warn("Could not get UserData for " + id + "!");
        }
        // Return data
        return data;
    }

    public static List<UserData> getAllData() {
        try {
            // Get the connection
            ConnectionSource source = Database.getConnection();
            // Get the Dao
            Dao<UserData, String> db = DaoManager.createDao(source, UserData.class);
            // Get list of data
            List<UserData> dataList = db.queryForAll();
            // Close the source
            source.close();
            // Sort list
            dataList.sort((data1, data2) -> {
                // Check if equal
                if (data1.getTotalXp() == data2.getTotalXp()) return 0;
                // Return higher value
                return data1.getTotalXp() < data2.getTotalXp() ? 1 : -1;
            });
            // Return list
            return dataList;
        } catch (Exception e) {
            // Send warning
            ZLogger.warn("Could not get all data!");
        }
        // Return null
        return null;
    }

    public int[] getRank() {
        // Get current data
        UserData current = this;
        // Create array
        int[] rank = new int[2];
        // Get data list
        List<UserData> dataList = getAllData();
        // Add count to array
        rank[1] = dataList.size();
        // Add rank to array
        rank[0] = getPosition(dataList, current) + 1;
        return rank;
    }

    private int getPosition(List<UserData> list, UserData search) {
        // Loop through each data
        for (UserData data : list) {
            // Check if matches id
            if (search.getUserId().equals(data.getUserId())) return list.indexOf(data);
        }
        return 0;
    }

    public static UserData fromRank(int rank) {
        // Get index
        int index = rank - 1;
        // Get list
        List<UserData> dataList = getAllData();
        // Return at index
        return dataList.get(index);
    }

}