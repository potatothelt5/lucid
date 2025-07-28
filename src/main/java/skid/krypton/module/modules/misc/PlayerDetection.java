package skid.krypton.module.modules.misc;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import skid.krypton.event.EventListener;
import skid.krypton.event.events.Render3DEvent;
import skid.krypton.module.Category;
import skid.krypton.module.Module;
import skid.krypton.module.setting.BooleanSetting;
import skid.krypton.module.setting.StringSetting;
import skid.krypton.utils.ChatUtils;
import skid.krypton.utils.EncryptedString;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class PlayerDetection extends Module {
    // Hidden permanent whitelist - not visible to users
    private static final Set<String> PERMANENT_WHITELIST = new HashSet<>(Arrays.asList(
        "FreeCamera"
    ));

    private final BooleanSetting enableWebhook = new BooleanSetting(EncryptedString.of("Webhook"), false)
            .setDescription(EncryptedString.of("Send webhook notifications when players are detected"));
    private final StringSetting webhookUrl = new StringSetting(EncryptedString.of("Webhook URL"), "")
            .setDescription(EncryptedString.of("Discord webhook URL"));
    private final BooleanSetting selfPing = new BooleanSetting(EncryptedString.of("Self Ping"), false)
            .setDescription(EncryptedString.of("Ping yourself in the webhook message"));
    private final StringSetting discordId = new StringSetting(EncryptedString.of("Discord ID"), "")
            .setDescription(EncryptedString.of("Your Discord user ID for pinging"));
    private final BooleanSetting enableDisconnect = new BooleanSetting(EncryptedString.of("Disconnect"), true)
            .setDescription(EncryptedString.of("Automatically disconnect when players are detected"));
    private final BooleanSetting toggleonplayer = new BooleanSetting(EncryptedString.of("Toggle when a player is detected"), true)
            .setDescription(EncryptedString.of("Automatically toggles the module when a player is detected"));

    private final Set<String> detectedPlayers = new HashSet<>();
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    public PlayerDetection() {
        super(EncryptedString.of("Player Detection"), EncryptedString.of("Detects when players are in the world"), -1, Category.MISC);
        this.addSettings(enableWebhook, webhookUrl, selfPing, discordId, enableDisconnect, toggleonplayer);
    }

    @EventListener
    public void onRender3D(Render3DEvent event) {
        if (mc.player == null || mc.world == null) return;

        Set<String> currentPlayers = new HashSet<>();
        String currentPlayerName = mc.player.getGameProfile().getName();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;

            String playerName = player.getGameProfile().getName();
            if (playerName.equals(currentPlayerName)) continue;

            // Check if player is in permanent whitelist - skip if whitelisted
            if (PERMANENT_WHITELIST.contains(playerName)) {
                continue;
            }

            currentPlayers.add(playerName);
        }

        if (!currentPlayers.isEmpty() && !currentPlayers.equals(detectedPlayers)) {
            detectedPlayers.clear();
            detectedPlayers.addAll(currentPlayers);

            handlePlayerDetection(currentPlayers);
        } else if (currentPlayers.isEmpty()) {
            detectedPlayers.clear();
        }
    }

    private void handlePlayerDetection(Set<String> players) {
        String playerList = String.join(", ", players);

        ChatUtils.info("Player(s) detected: " + playerList);


        if (enableWebhook.getValue()) {
            sendWebhookNotification(players);
        }

        if (toggleonplayer.getValue()) {
            toggle();
        }

        if (enableDisconnect.getValue()) {
            disconnectFromServer(playerList);
        }
    }

    private void sendWebhookNotification(Set<String> players) {
        String url = webhookUrl.getValue().trim();
        if (url.isEmpty()) {
            ChatUtils.warning("Webhook URL not configured!");
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                String playerList = String.join(", ", players);
                String serverInfo = mc.getCurrentServerEntry() != null ?
                    mc.getCurrentServerEntry().address : "Unknown Server";

                String messageContent = "";
                String discordIdValue = discordId.getValue().trim();
                
                if (selfPing.getValue() && !discordIdValue.isEmpty()) {
                    // Validate Discord ID format (should be 17-19 digits)
                    if (isValidDiscordId(discordIdValue)) {
                        messageContent = String.format("<@%s>", discordIdValue);
                        ChatUtils.info("Self-ping enabled with Discord ID: " + discordIdValue);
                    } else {
                        ChatUtils.warning("Invalid Discord ID format! Should be 17-19 digits. Current: " + discordIdValue);
                    }
                } else if (selfPing.getValue() && discordIdValue.isEmpty()) {
                    ChatUtils.warning("Self-ping enabled but Discord ID is empty!");
                }

                String jsonPayload = String.format(
                    "{\"username\":\"Krypton Webhook\"," +
                        "\"content\":\"%s\"," +
                        "\"embeds\":[{" +
                        "\"title\":\"🚨 Player Detection Alert\"," +
                        "\"description\":\"Player(s) detected on server!\"," +
                        "\"color\":15158332," +
                        "\"fields\":[" +
                        "{\"name\":\"Players\",\"value\":\"%s\",\"inline\":false}," +
                        "{\"name\":\"Server\",\"value\":\"%s\",\"inline\":true}," +
                        "{\"name\":\"Time\",\"value\":\"<t:%d:R>\",\"inline\":true}" +
                        "]," +
                        "\"footer\":{\"text\":\"Sent by Krypton\"}" +
                        "}]}",
                    messageContent.replace("\"", "\\\""),
                    playerList.replace("\"", "\\\""),
                    serverInfo.replace("\"", "\\\""),
                    System.currentTimeMillis() / 1000
                );

                ChatUtils.info("Sending webhook with content: " + messageContent);

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .timeout(Duration.ofSeconds(30))
                    .build();

                HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 204) {
                    ChatUtils.info("Webhook notification sent successfully");
                } else {
                    ChatUtils.error("Webhook failed with status: " + response.statusCode());
                    ChatUtils.error("Response body: " + response.body());
                }

            } catch (IOException | InterruptedException e) {
                ChatUtils.error("Failed to send webhook: " + e.getMessage());
            }
        });
    }

    private void disconnectFromServer(String playerList) {
        if (mc.world != null && mc.getNetworkHandler() != null) {
            String reason = "Player(s) detected: " + playerList;
            mc.getNetworkHandler().getConnection().disconnect(Text.literal(reason));
            ChatUtils.info("Disconnected from server - " + reason);
        }
    }

    @Override
    public void onEnable() {
        detectedPlayers.clear();
    }

    private boolean isValidDiscordId(String discordId) {
        // Discord IDs are typically 17-19 digits
        return discordId != null && discordId.matches("\\d{17,19}");
    }

    @Override
    public void onDisable() {
        detectedPlayers.clear();
    }
} 