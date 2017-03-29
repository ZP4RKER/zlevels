package com.zp4rker.dscrd.zlevels.commands;

import com.zp4rker.dscrd.core.cmd.CommandExecutor;
import com.zp4rker.dscrd.core.cmd.RegisterCommand;
import com.zp4rker.dscrd.zlevels.core.db.UserData;
import net.dv8tion.jda.core.entities.Message;

/**
 * @author ZP4RKER
 */
public class InactiveCommand implements CommandExecutor {

    private int count;

    @RegisterCommand(aliases = "inactive",
                    usage = "{prefix}inactive",
                    description = "Displays count of inactive members on the server.")
    public String onCommand(Message message) {
        // Create count
        count = 0;
        // Loop through
        message.getGuild().getMembers().forEach(member -> {
            // Check if bot
            if (member.getUser().isBot()) return;
            // Get data
            UserData data = UserData.fromId(member.getUser().getId());
            // Check if exists
            if (data == null) count++;
        });
        // Send message
        message.getTextChannel().sendMessage("There are " + count + " inactive " + (count == 1 ? "user" : "users") +
                " in this server.").queue();
        // Return null
        return null;
    }

}