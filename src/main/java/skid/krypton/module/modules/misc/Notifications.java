package skid.krypton.module.modules.misc;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.math.Vec3d;
import skid.krypton.Krypton;
import skid.krypton.event.EventListener;
import skid.krypton.event.events.TickEvent;
import skid.krypton.module.Category;
import skid.krypton.module.Module;
import skid.krypton.module.setting.BooleanSetting;
import skid.krypton.utils.ChatUtils;
import skid.krypton.utils.EncryptedString;

import java.util.ArrayList;

public final class Notifications extends Module {
    private final BooleanSetting totemPops = new BooleanSetting(EncryptedString.of("Totem Pops"), true)
            .setDescription(EncryptedString.of("Notifies you in chat whenever a player pops a totem"));
    private final BooleanSetting visualRange = new BooleanSetting(EncryptedString.of("Visual Range"), false)
            .setDescription(EncryptedString.of("Notifies you in chat whenever a player enters your render distance"));
    private final BooleanSetting pearlThrows = new BooleanSetting(EncryptedString.of("Pearl Throws"), true)
            .setDescription(EncryptedString.of("Notifies you in chat whenever a player throws a pearl"));

    private final ArrayList<PlayerEntity> loadedPlayers = new ArrayList<>();
    private final ArrayList<Integer> thrownPearls = new ArrayList<>();

    public Notifications() {
        super(EncryptedString.of("Notifications"), EncryptedString.of("Notifies you in chat whenever something significant happens"), -1, Category.MISC);
        this.addSettings(totemPops, visualRange, pearlThrows);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        loadedPlayers.clear();
        thrownPearls.clear();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        loadedPlayers.clear();
        thrownPearls.clear();
    }

    @EventListener
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (visualRange.getValue()) {
            for (Entity entity : mc.world.getEntities()) {
                if (!(entity instanceof PlayerEntity player) || entity == mc.player) continue;

                if (!loadedPlayers.contains(player)) {
                    loadedPlayers.add(player);
                    ChatUtils.info(player.getName().getString() + " has entered your visual range.");
                }
            }

            if (!loadedPlayers.isEmpty()) {
                for (PlayerEntity player : new ArrayList<>(loadedPlayers)) {
                    if (!mc.world.getPlayers().contains(player)) {
                        loadedPlayers.remove(player);
                        ChatUtils.info(player.getName().getString() + " has left your visual range.");
                    }
                }
            }
        }

        if (pearlThrows.getValue()) {
            for (Entity e : mc.world.getEntities()) {
                if (!(e instanceof EnderPearlEntity pearl)) continue;
                if (pearl.getOwner() == null || thrownPearls.contains(pearl.getId())) continue;

                String name = pearl.getOwner().getName().getString();
                String direction = getPearlDirection(pearl);
                ChatUtils.info(name + " threw a pearl towards " + direction + ".");
                thrownPearls.add(pearl.getId());
            }

            thrownPearls.removeIf(id -> !(mc.world.getEntityById(id) instanceof EnderPearlEntity));
        }
    }

    private String getPearlDirection(EnderPearlEntity pearl) {
        Vec3d velocity = pearl.getVelocity();
        double x = velocity.x;
        double z = velocity.z;
        
        if (Math.abs(x) > Math.abs(z)) {
            return x > 0 ? "East" : "West";
        } else {
            return z > 0 ? "South" : "North";
        }
    }

    // Method to be called when a player pops a totem (you'll need to integrate this with your totem detection)
    public void onPlayerPop(PlayerEntity player, int pops) {
        if (totemPops.getValue()) {
            ChatUtils.info(player.getName().getString() + " has popped " + pops + " totem" + (pops > 1 ? "s" : "") + ".");
        }
    }

    // Method to be called when a player dies after popping totems
    public void onPlayerDeath(PlayerEntity player, int pops) {
        if (totemPops.getValue() && pops > 0) {
            ChatUtils.info(player.getName().getString() + " has died after popping " + pops + " totem" + (pops > 1 ? "s" : "") + ".");
        }
    }

    // Method to be called when connecting to a server
    public void onClientConnect() {
        loadedPlayers.clear();
        thrownPearls.clear();
    }
} 