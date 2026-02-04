package org.asylum_media.afkSync;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AfkSync extends JavaPlugin implements Listener {

    private final Map<UUID, Long> lastActivity = new HashMap<>();
    private final Map<UUID, Long> afkSince = new HashMap<>();

    @Override
    public void onEnable() {
        // Register event listener
        getServer().getPluginManager().registerEvents(this, this);

        // Seed already-online players (reload safety)
        long now = System.currentTimeMillis();
        for (Player player : getServer().getOnlinePlayers()) {
            lastActivity.put(player.getUniqueId(), now);
        }

        getLogger().info("AfkSync enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("AfkSync disabled.");
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        // Ignore head-only movement
        if (event.getFrom().getX() == event.getTo().getX()
                && event.getFrom().getZ() == event.getTo().getZ()) {
            return;
        }

        UUID uuid = event.getPlayer().getUniqueId();

        // Update activity timestamp
        lastActivity.put(uuid, System.currentTimeMillis());

        // If they were AFK, mark them as back
        afkSince.remove(uuid);
    }
}
