package org.asylum_media.afkSync;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
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

    // How long (ms) before we mark a player AFK
    private long afkAfterMillis;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Load config (default 300 seconds = 5 minutes)
        afkAfterMillis = getConfig().getLong("afk.after-seconds", 300) * 1000L;

        // Register event listener
        getServer().getPluginManager().registerEvents(this, this);

        // Seed already-online players (reload safety)
        long now = System.currentTimeMillis();
        for (Player player : getServer().getOnlinePlayers()) {
            lastActivity.put(player.getUniqueId(), now);
        }

        // Start AFK detection loop
        startAfkScheduler();

        getLogger().info("AfkSync enabled (AFK after " + (afkAfterMillis / 1000) + "s).");
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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!command.getName().equalsIgnoreCase("afksync")) {
            return false;
        }

        if (args.length != 2 || !args[0].equalsIgnoreCase("status")) {
            sender.sendMessage("§cUsage: /afksync status <player>");
            return true;
        }

        Player target = getServer().getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found or not online.");
            return true;
        }

        UUID uuid = target.getUniqueId();

        boolean isAfk = afkSince.containsKey(uuid);
        long now = System.currentTimeMillis();

        if (!isAfk) {
            sender.sendMessage("§a" + target.getName() + " is not AFK.");
            return true;
        }

        long since = afkSince.get(uuid);
        long seconds = (now - since) / 1000;

        sender.sendMessage(
                "§e" + target.getName() + " is AFK (§f" + seconds + "s§e)"
        );

        return true;
    }

    private void startAfkScheduler() {
        getServer().getScheduler().runTaskTimer(
                this,
                () -> {
                    long now = System.currentTimeMillis();

                    for (Player player : getServer().getOnlinePlayers()) {
                        UUID uuid = player.getUniqueId();

                        Long last = lastActivity.get(uuid);
                        if (last == null) {
                            lastActivity.put(uuid, now);
                            continue;
                        }

                        // Already AFK → nothing to do
                        if (afkSince.containsKey(uuid)) {
                            continue;
                        }

                        long idleTime = now - last;
                        if (idleTime >= afkAfterMillis) {
                            afkSince.put(uuid, now);
                            // Optional: keep this as fine so it doesn't spam console
                            getLogger().fine(player.getName() + " is now AFK");
                        }
                    }
                },
                40L, // initial delay (2 seconds)
                40L  // repeat every 2 seconds
        );
    }
}
