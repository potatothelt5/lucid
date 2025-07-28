package skid.krypton.module.modules.donut;

import net.minecraft.block.Blocks;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import skid.krypton.Krypton;
import skid.krypton.event.EventListener;
import skid.krypton.event.events.TickEvent;
import skid.krypton.module.Category;
import skid.krypton.module.Module;
import skid.krypton.module.setting.BooleanSetting;
import skid.krypton.module.setting.NumberSetting;
import skid.krypton.module.setting.StringSetting;
import skid.krypton.utils.ChatUtils;
import skid.krypton.utils.EncryptedString;

import java.time.Instant;

public class SpawnerProtect extends Module {
    // Webhook settings
    private final BooleanSetting webhook = new BooleanSetting(EncryptedString.of("Webhook"), false)
            .setDescription(EncryptedString.of("Enable webhook notifications"));
    private final StringSetting webhookUrl = new StringSetting(EncryptedString.of("Webhook URL"), "")
            .setDescription(EncryptedString.of("Discord webhook URL for notifications"));
    private final BooleanSetting selfPing = new BooleanSetting(EncryptedString.of("Self Ping"), false)
            .setDescription(EncryptedString.of("Ping yourself in the webhook message"));
    private final StringSetting discordId = new StringSetting(EncryptedString.of("Discord ID"), "")
            .setDescription(EncryptedString.of("Your Discord user ID for pinging"));

    // General settings
    private final NumberSetting spawnerRange = new NumberSetting(EncryptedString.of("Spawner Range"), 1.0, 50.0, 16.0, 1.0);
    private final NumberSetting delaySeconds = new NumberSetting(EncryptedString.of("Recheck Delay Seconds"), 1.0, 10.0, 1.0, 1.0);
    private final BooleanSetting disableAutoReconnect = new BooleanSetting(EncryptedString.of("Disable AutoReconnect"), true)
            .setDescription(EncryptedString.of("Disables AutoReconnect"));

    // Auto-walk settings
    private final BooleanSetting autoWalk = new BooleanSetting(EncryptedString.of("Auto Walk"), true)
            .setDescription(EncryptedString.of("Automatically walk to spawners"));
    private final NumberSetting walkSpeed = new NumberSetting(EncryptedString.of("Walk Speed"), 0.1, 1.0, 0.3, 0.1);
    private final NumberSetting walkDistance = new NumberSetting(EncryptedString.of("Walk Distance"), 1.0, 10.0, 3.0, 0.5);

    // Mining settings
    private final BooleanSetting autoShift = new BooleanSetting(EncryptedString.of("Auto Shift"), true)
            .setDescription(EncryptedString.of("Automatically shift when mining spawners"));

    // Ender chest settings
    private final BooleanSetting autoEnderChest = new BooleanSetting(EncryptedString.of("Auto Ender Chest"), true)
            .setDescription(EncryptedString.of("Automatically walk to and open ender chest"));
    private final NumberSetting enderChestRange = new NumberSetting(EncryptedString.of("Ender Chest Range"), 5.0, 50.0, 20.0, 1.0);

    // Whitelist settings
    private final BooleanSetting enableWhitelist = new BooleanSetting(EncryptedString.of("Enable Whitelist"), false)
            .setDescription(EncryptedString.of("Enable player whitelist (whitelisted players won't trigger protection)"));
    private final StringSetting whitelistPlayers = new StringSetting(EncryptedString.of("Whitelisted Players"), "")
            .setDescription(EncryptedString.of("Comma-separated list of player names to ignore"));

    private enum State {
        IDLE,
        GOING_TO_SPAWNERS,
        GOING_TO_CHEST,
        DEPOSITING_ITEMS,
        DISCONNECTING
    }

    private State currentState = State.IDLE;
    private String detectedPlayer = "";
    private long detectionTime = 0;
    private boolean spawnersMinedSuccessfully = false;
    private boolean itemsDepositedSuccessfully = false;
    private int tickCounter = 0;
    private boolean chestOpened = false;
    private int transferDelayCounter = 0;
    private int lastProcessedSlot = -1;

    private boolean sneaking = false;
    private BlockPos currentTarget = null;
    private int recheckDelay = 0;
    private int confirmDelay = 0;
    private boolean waiting = false;
    private boolean walkingToSpawner = false;
    private Vec3d walkTarget = null;

    public SpawnerProtect() {
        super(EncryptedString.of("Spawner Protect"), EncryptedString.of("Breaks spawners and puts them in your inv when a player is detected"), -1, Category.DONUT);
        this.addSettings(webhook, webhookUrl, selfPing, discordId, spawnerRange, delaySeconds, disableAutoReconnect, autoWalk, walkSpeed, walkDistance, autoShift, autoEnderChest, enderChestRange, enableWhitelist, whitelistPlayers);
    }

    @Override
    public void onEnable() {
        currentState = State.IDLE;
        detectedPlayer = "";
        detectionTime = 0;
        spawnersMinedSuccessfully = false;
        itemsDepositedSuccessfully = false;
        tickCounter = 0;
        chestOpened = false;
        transferDelayCounter = 0;
        lastProcessedSlot = -1;

        sneaking = false;
        currentTarget = null;
        recheckDelay = 0;
        confirmDelay = 0;
        waiting = false;
        walkingToSpawner = false;
        walkTarget = null;

        ChatUtils.info("SpawnerProtect activated - monitoring for players...");
        ChatUtils.warning("Make sure to have an empty inventory with only a silk touch pickaxe and an ender chest nearby!");
    }

    private void toggleModule(Class<? extends Module> moduleClass, boolean disable) {
        Module module = Krypton.INSTANCE.getModuleManager().getModule(moduleClass);
        if (module != null) {
            if (disable && module.isEnabled()) module.toggle();
            else if (!disable && !module.isEnabled()) module.toggle();
        }
    }

    @EventListener
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        tickCounter++;

        if (transferDelayCounter > 0) {
            transferDelayCounter--;
            return;
        }

        switch (currentState) {
            case IDLE:
                checkForPlayers();
                break;
            case GOING_TO_SPAWNERS:
                handleGoingToSpawners();
                break;
            case GOING_TO_CHEST:
                handleGoingToChest();
                break;
            case DEPOSITING_ITEMS:
                handleDepositingItems();
                break;
            case DISCONNECTING:
                handleDisconnecting();
                break;
        }

        toggleModule(skid.krypton.module.modules.misc.AutoReconnect.class, disableAutoReconnect.getValue());
    }

    private void checkForPlayers() {
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (!(player instanceof AbstractClientPlayerEntity)) continue;

            String playerName = player.getGameProfile().getName();

            if (enableWhitelist.getValue() && isPlayerWhitelisted(playerName)) {
                continue;
            }

            detectedPlayer = playerName;
            detectionTime = System.currentTimeMillis();

            ChatUtils.info("SpawnerProtect: Player detected - " + detectedPlayer);

            currentState = State.GOING_TO_SPAWNERS;
            ChatUtils.info("Player detected! Starting protection sequence...");

            // Always start sneaking immediately
            if (!sneaking) {
                mc.player.setSneaking(true);
                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
                sneaking = true;
            }

            break;
        }
    }

    private boolean isPlayerWhitelisted(String playerName) {
        if (!enableWhitelist.getValue() || whitelistPlayers.getValue().isEmpty()) {
            return false;
        }

        String[] whitelistedNames = whitelistPlayers.getValue().split(",");
        for (String whitelistedName : whitelistedNames) {
            if (whitelistedName.trim().equalsIgnoreCase(playerName)) {
                return true;
            }
        }

        return false;
    }

    private void handleGoingToSpawners() {
        // Always ensure sneaking is enabled when auto-shift is on
        if (autoShift.getValue()) {
            if (!sneaking) {
                mc.player.setSneaking(true);
                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
                sneaking = true;
            }

            // Force sneaking every tick to ensure it stays active
            if (!mc.player.isSneaking()) {
                mc.player.setSneaking(true);
                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
            }
        }

        if (currentTarget == null) {
            currentTarget = findNearestSpawner();

            if (currentTarget == null && !waiting) {
                waiting = true;
                recheckDelay = 0;
                confirmDelay = 0;
                ChatUtils.info("No more spawners found, waiting to confirm...");
            }
        } else {
            // Auto-walk to spawner if enabled
            if (autoWalk.getValue() && !walkingToSpawner) {
                walkToSpawner(currentTarget);
            }

            lookAtBlock(currentTarget);

            // Always mine the spawner while sneaking (if auto-shift is enabled)
            mc.interactionManager.updateBlockBreakingProgress(currentTarget, Direction.UP);
            KeyBinding.setKeyPressed(mc.options.attackKey.getDefaultKey(), true);

            // Check if spawner is broken
            if (mc.world.getBlockState(currentTarget).isAir()) {
                ChatUtils.info("Spawner at " + currentTarget + " broken! Looking for next spawner...");
                currentTarget = null;
                KeyBinding.setKeyPressed(mc.options.attackKey.getDefaultKey(), false);
                walkingToSpawner = false;
                walkTarget = null;

                transferDelayCounter = 5;
            }
        }

        if (waiting) {
            recheckDelay++;
            if (recheckDelay == delaySeconds.getIntValue() * 20) {
                BlockPos foundSpawner = findNearestSpawner();

                if (foundSpawner != null) {
                    waiting = false;
                    currentTarget = foundSpawner;
                    ChatUtils.info("Found additional spawner at " + foundSpawner);
                    return;
                }
            }

            if (recheckDelay > delaySeconds.getIntValue() * 20) {
                confirmDelay++;
                if (confirmDelay >= 5) {
                    KeyBinding.setKeyPressed(mc.options.attackKey.getDefaultKey(), false);
                    spawnersMinedSuccessfully = true;
                    if (sneaking && mc.player.isSneaking()) {
                        mc.player.setSneaking(false);
                        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
                        sneaking = false;
                    }
                    currentState = State.GOING_TO_CHEST;
                    ChatUtils.info("All spawners mined successfully. Going to ender chest...");
                    tickCounter = 0;
                }
            }
        }
    }

    private void walkToSpawner(BlockPos spawnerPos) {
        Vec3d spawnerCenter = Vec3d.ofCenter(spawnerPos);
        Vec3d playerPos = mc.player.getPos();
        double distance = playerPos.distanceTo(spawnerCenter);
        
        // Only update movement every few ticks to reduce lag
        if (tickCounter % 3 != 0) {
            return;
        }
        
        if (distance > walkDistance.getValue()) {
            walkingToSpawner = true;
            
            // Look towards spawner (rotation) - only update rotation occasionally
            if (tickCounter % 5 == 0) {
                lookAtBlock(spawnerPos);
            }
            
            // Only use W key (forward) to move
            KeyBinding.setKeyPressed(mc.options.forwardKey.getDefaultKey(), true);
            KeyBinding.setKeyPressed(mc.options.backKey.getDefaultKey(), false);
            KeyBinding.setKeyPressed(mc.options.leftKey.getDefaultKey(), false);
            KeyBinding.setKeyPressed(mc.options.rightKey.getDefaultKey(), false);
            
        } else {
            // Stop walking when close enough
            walkingToSpawner = false;
            walkTarget = null;
            KeyBinding.setKeyPressed(mc.options.forwardKey.getDefaultKey(), false);
            KeyBinding.setKeyPressed(mc.options.backKey.getDefaultKey(), false);
            KeyBinding.setKeyPressed(mc.options.leftKey.getDefaultKey(), false);
            KeyBinding.setKeyPressed(mc.options.rightKey.getDefaultKey(), false);
        }
    }

    private BlockPos findNearestSpawner() {
        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos nearestSpawner = null;
        double nearestDistance = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.iterate(
            playerPos.add(-spawnerRange.getIntValue(), -spawnerRange.getIntValue(), -spawnerRange.getIntValue()),
            playerPos.add(spawnerRange.getIntValue(), spawnerRange.getIntValue(), spawnerRange.getIntValue()))) {

            if (mc.world.getBlockState(pos).getBlock() == Blocks.SPAWNER) {
                double distance = pos.getSquaredDistance(mc.player.getPos());
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestSpawner = pos.toImmutable();
                }
            }
        }

        if (nearestSpawner != null) {
            ChatUtils.info("Found spawner at " + nearestSpawner + " (distance: " + Math.sqrt(nearestDistance) + ")");
        }

        return nearestSpawner;
    }

    private void lookAtBlock(BlockPos pos) {
        Vec3d targetPos = Vec3d.ofCenter(pos);
        Vec3d playerPos = mc.player.getEyePos();

        Vec3d direction = targetPos.subtract(playerPos).normalize();

        double yaw = Math.toDegrees(Math.atan2(-direction.x, direction.z));
        double pitch = Math.toDegrees(-Math.asin(direction.y));

        mc.player.setYaw((float) yaw);
        mc.player.setPitch((float) pitch);
    }

    private void handleGoingToChest() {
        if (autoEnderChest.getValue()) {
            BlockPos nearestEnderChest = findNearestEnderChest();
            
            if (nearestEnderChest != null) {
                // Walk to ender chest
                walkToEnderChest(nearestEnderChest);
                
                // Check if we're close enough to open it
                double distance = mc.player.getPos().distanceTo(Vec3d.ofCenter(nearestEnderChest));
                if (distance <= 4.0) {
                    // Look at the ender chest
                    lookAtBlock(nearestEnderChest);
                    
                    // Try to open it
                    if (tickCounter % 10 == 0) {
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, 
                            new BlockHitResult(Vec3d.ofCenter(nearestEnderChest), Direction.UP, nearestEnderChest, false));
                    }
                    
                    // Check if chest is opened
                    if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler) {
                        currentState = State.DEPOSITING_ITEMS;
                        tickCounter = 0;
                        ChatUtils.info("Ender chest opened! Starting item deposit...");
                        return;
                    }
                }
            } else {
                ChatUtils.error("No ender chest found within range!");
                currentState = State.DISCONNECTING;
                return;
            }
        } else {
            // Original logic for manual ender chest
            boolean nearEnderChest = false;
            BlockPos playerPos = mc.player.getBlockPos();

            for (int x = -3; x <= 3; x++) {
                for (int y = -3; y <= 3; y++) {
                    for (int z = -3; z <= 3; z++) {
                        BlockPos pos = playerPos.add(x, y, z);
                        if (mc.world.getBlockState(pos).getBlock() == Blocks.ENDER_CHEST) {
                            nearEnderChest = true;
                            break;
                        }
                    }
                }
            }

            if (nearEnderChest) {
                currentState = State.DEPOSITING_ITEMS;
                tickCounter = 0;
                ChatUtils.info("Reached ender chest area. Opening and depositing items...");
            }
        }

        if (tickCounter > 600) {
            ChatUtils.error("Timed out trying to reach ender chest!");
            currentState = State.DISCONNECTING;
        }
    }

    private BlockPos findNearestEnderChest() {
        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos nearestChest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.iterate(
            playerPos.add(-enderChestRange.getIntValue(), -enderChestRange.getIntValue(), -enderChestRange.getIntValue()),
            playerPos.add(enderChestRange.getIntValue(), enderChestRange.getIntValue(), enderChestRange.getIntValue()))) {

            if (mc.world.getBlockState(pos).getBlock() == Blocks.ENDER_CHEST) {
                double distance = pos.getSquaredDistance(mc.player.getPos());
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestChest = pos.toImmutable();
                }
            }
        }

        return nearestChest;
    }

    private void walkToEnderChest(BlockPos chestPos) {
        Vec3d chestCenter = Vec3d.ofCenter(chestPos);
        Vec3d playerPos = mc.player.getPos();
        double distance = playerPos.distanceTo(chestCenter);
        
        // Only update movement every few ticks to reduce lag
        if (tickCounter % 3 != 0) {
            return;
        }
        
        if (distance > 4.5) {
            // Look towards chest (rotation) - only update rotation occasionally
            if (tickCounter % 5 == 0) {
                lookAtBlock(chestPos);
            }
            
            // Only use W key (forward) to move
            KeyBinding.setKeyPressed(mc.options.forwardKey.getDefaultKey(), true);
            KeyBinding.setKeyPressed(mc.options.backKey.getDefaultKey(), false);
            KeyBinding.setKeyPressed(mc.options.leftKey.getDefaultKey(), false);
            KeyBinding.setKeyPressed(mc.options.rightKey.getDefaultKey(), false);
            
        } else {
            // Stop walking when close enough
            KeyBinding.setKeyPressed(mc.options.forwardKey.getDefaultKey(), false);
            KeyBinding.setKeyPressed(mc.options.backKey.getDefaultKey(), false);
            KeyBinding.setKeyPressed(mc.options.leftKey.getDefaultKey(), false);
            KeyBinding.setKeyPressed(mc.options.rightKey.getDefaultKey(), false);
        }
    }

    private void handleDepositingItems() {
        if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler) {
            GenericContainerScreenHandler handler = (GenericContainerScreenHandler) mc.player.currentScreenHandler;

            if (!chestOpened) {
                chestOpened = true;
                lastProcessedSlot = -1;
                ChatUtils.info("Ender chest opened, starting item transfer...");
            }

            if (!hasItemsToDeposit()) {
                itemsDepositedSuccessfully = true;
                ChatUtils.info("All items deposited successfully!");
                mc.player.closeHandledScreen();
                transferDelayCounter = 10;
                currentState = State.DISCONNECTING;
                return;
            }

            transferItemsToChest(handler);

        } else {
            if (tickCounter % 20 == 0) {
                // Try to open ender chest
                BlockPos playerPos = mc.player.getBlockPos();
                for (int x = -3; x <= 3; x++) {
                    for (int y = -3; y <= 3; y++) {
                        for (int z = -3; z <= 3; z++) {
                            BlockPos pos = playerPos.add(x, y, z);
                            if (mc.world.getBlockState(pos).getBlock() == Blocks.ENDER_CHEST) {
                                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false));
                                return;
                            }
                        }
                    }
                }
            }
        }

        if (tickCounter > 900) {
            ChatUtils.error("Timed out depositing items!");
            currentState = State.DISCONNECTING;
        }
    }

    private boolean hasItemsToDeposit() {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() != Items.AIR) {
                return true;
            }
        }
        return false;
    }

    private void transferItemsToChest(GenericContainerScreenHandler handler) {
        int totalSlots = handler.slots.size();
        int chestSlots = totalSlots - 36;
        int playerInventoryStart = chestSlots;
        int startSlot = Math.max(lastProcessedSlot + 1, playerInventoryStart);

        for (int i = 0; i < 36; i++) {
            int slotId = playerInventoryStart + ((startSlot - playerInventoryStart + i) % 36);
            ItemStack stack = handler.getSlot(slotId).getStack();

            if (stack.isEmpty() || stack.getItem() == Items.AIR) {
                continue;
            }

            ChatUtils.info("Transferring item from slot " + slotId + ": " + stack.getItem().toString());

            mc.interactionManager.clickSlot(
                handler.syncId,
                slotId,
                0,
                SlotActionType.QUICK_MOVE,
                mc.player
            );

            lastProcessedSlot = slotId;
            transferDelayCounter = 2;
            return;
        }

        if (lastProcessedSlot >= playerInventoryStart) {
            lastProcessedSlot = playerInventoryStart - 1;
            transferDelayCounter = 3;
        }
    }

    private void handleDisconnecting() {
        sendWebhookNotification();

        ChatUtils.info("SpawnerProtect: Player detected - " + detectedPlayer);

        if (mc.world != null) {
            mc.world.disconnect();
        }

        ChatUtils.info("Disconnected due to player detection.");
        toggle();
    }

    private void sendWebhookNotification() {
        if (!webhook.getValue() || webhookUrl.getValue().isEmpty()) {
            ChatUtils.info("Webhook disabled or URL not configured.");
            return;
        }

        long discordTimestamp = detectionTime / 1000L;

        String messageContent = "";
        if (selfPing.getValue() && !discordId.getValue().trim().isEmpty()) {
            messageContent = String.format("<@%s>", discordId.getValue().trim());
        }

        String embedJson = String.format("""
            {
                "username": "Krypton Webhook",
                "avatar_url": "https://i.imgur.com/OL2y1cr.png",
                "content": "%s",
                "embeds": [{
                    "title": "SpawnerProtect Alert",
                    "description": "**Player Detected:** %s\\n**Detection Time:** <t:%d:R>\\n**Spawners Mined:** %s\\n**Items Deposited:** %s\\n**Disconnected:** Yes",
                    "color": 16766720,
                    "timestamp": "%s",
                    "footer": {
                        "text": "Sent by Krypton"
                    }
                }]
            }""",
            messageContent.replace("\"", "\\\""),
            detectedPlayer,
            discordTimestamp,
            spawnersMinedSuccessfully ? "✅ Success" : "❌ Failed",
            itemsDepositedSuccessfully ? "✅ Success" : "❌ Failed",
            Instant.now().toString()
        );

        new Thread(() -> {
            try {
                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(webhookUrl.getValue()))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(embedJson))
                    .build();

                client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
                ChatUtils.info("Webhook notification sent successfully!");
            } catch (Exception e) {
                ChatUtils.error("Failed to send webhook notification: " + e.getMessage());
            }
        }).start();
    }

    @Override
    public void onDisable() {
        KeyBinding.setKeyPressed(mc.options.attackKey.getDefaultKey(), false);
        KeyBinding.setKeyPressed(mc.options.forwardKey.getDefaultKey(), false);
        KeyBinding.setKeyPressed(mc.options.backKey.getDefaultKey(), false);
        KeyBinding.setKeyPressed(mc.options.leftKey.getDefaultKey(), false);
        KeyBinding.setKeyPressed(mc.options.rightKey.getDefaultKey(), false);
        if (sneaking && mc.player != null && mc.player.isSneaking()) {
            mc.player.setSneaking(false);
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
        }
    }
} 