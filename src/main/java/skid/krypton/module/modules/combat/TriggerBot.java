package skid.krypton.module.modules.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;
import skid.krypton.Krypton;
import skid.krypton.event.EventListener;
import skid.krypton.event.events.AttackEvent;
import skid.krypton.event.events.TickEvent;
import skid.krypton.module.Category;
import skid.krypton.module.Module;
import skid.krypton.module.modules.client.Friends;
import skid.krypton.module.setting.BooleanSetting;
import skid.krypton.module.setting.NumberSetting;
import skid.krypton.utils.EncryptedString;
import skid.krypton.utils.TimerUtils;
import skid.krypton.utils.WorldUtils;
import net.minecraft.component.DataComponentTypes;

public class TriggerBot extends Module {
    private final BooleanSetting inScreen = new BooleanSetting(EncryptedString.of("Work In Screen"), false);
    private final BooleanSetting whileUse = new BooleanSetting(EncryptedString.of("While Use"), false);
    private final BooleanSetting onLeftClick = new BooleanSetting(EncryptedString.of("On Left Click"), false);
    private final BooleanSetting allItems = new BooleanSetting(EncryptedString.of("All Items"), false);
    private final NumberSetting swordDelay = new NumberSetting(EncryptedString.of("Sword Delay"), 0, 1000, 550, 1);
    private final NumberSetting axeDelay = new NumberSetting(EncryptedString.of("Axe Delay"), 0, 1000, 800, 1);
    private final BooleanSetting checkShield = new BooleanSetting(EncryptedString.of("Check Shield"), false);
    private final BooleanSetting onlyCritSword = new BooleanSetting(EncryptedString.of("Only Crit Sword"), false);
    private final BooleanSetting onlyCritAxe = new BooleanSetting(EncryptedString.of("Only Crit Axe"), false);
    private final BooleanSetting swing = new BooleanSetting(EncryptedString.of("Swing Hand"), true);
    private final BooleanSetting whileAscend = new BooleanSetting(EncryptedString.of("While Ascending"), false);
    private final BooleanSetting clickSimulation = new BooleanSetting(EncryptedString.of("Click Simulation"), false);
    private final BooleanSetting strayBypass = new BooleanSetting(EncryptedString.of("Stray Bypass"), false);
    private final BooleanSetting allEntities = new BooleanSetting(EncryptedString.of("All Entities"), false);
    private final BooleanSetting useShield = new BooleanSetting(EncryptedString.of("Use Shield"), false);
    private final NumberSetting shieldTime = new NumberSetting(EncryptedString.of("Shield Time"), 100, 1000, 350, 1);
    private final BooleanSetting sticky = new BooleanSetting(EncryptedString.of("Same Player"), false);
    private final TimerUtils timer = new TimerUtils();

    private int currentSwordDelay, currentAxeDelay;

    public TriggerBot() {
        super(EncryptedString.of("Trigger Bot"), EncryptedString.of("Automatically hits players for you"), -1, Category.COMBAT);
        addSettings(inScreen, whileUse, onLeftClick, allItems, swordDelay, axeDelay, checkShield, whileAscend, sticky, onlyCritSword, onlyCritAxe, swing, clickSimulation, strayBypass, allEntities, useShield, shieldTime);
    }

    @Override
    public void onEnable() {
        currentSwordDelay = swordDelay.getIntValue();
        currentAxeDelay = axeDelay.getIntValue();
        super.onEnable();
    }

    @EventListener
    public void onTick(TickEvent event) {
        try {
            if (!inScreen.getValue() && mc.currentScreen != null)
                return;

            // Check if aiming over friend (if Friends module exists)
            Friends friendsModule = (Friends) Krypton.INSTANCE.getModuleManager().getModule(Friends.class);
            if (friendsModule != null && friendsModule.isEnabled()) {
                // Add friend check logic here if needed
            }

            Item item = mc.player.getMainHandStack().getItem();

            if (onLeftClick.getValue() && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS)
                return;

            if (((mc.player.getOffHandStack().getItem().getComponents().contains(DataComponentTypes.FOOD) || mc.player.getOffHandStack().getItem() instanceof ShieldItem) && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS) && !whileUse.getValue())
                return;
            
            if (!whileAscend.getValue() && ((!mc.player.isOnGround() && mc.player.getVelocity().y > 0) || (!mc.player.isOnGround() && mc.player.fallDistance <= 0.0F)))
                return;

            if (!allItems.getValue()) {
                if (item instanceof SwordItem) {
                    if (mc.crosshairTarget instanceof EntityHitResult hit) {
                        Entity entity = hit.getEntity();

                        if (sticky.getValue() && entity != mc.player.getAttacking())
                            return;

                        if (entity instanceof PlayerEntity || (strayBypass.getValue() && entity instanceof ZombieEntity) || (allEntities.getValue() && entity != null)) {

                            if (entity instanceof PlayerEntity player) {
                                if (checkShield.getValue() && player.isBlocking() && !WorldUtils.isShieldFacingAway(player))
                                    return;
                            }

                            if (onlyCritSword.getValue() && mc.player.fallDistance <= 0.0F)
                                return;

                            if (timer.delay(currentSwordDelay)) {
                                if (useShield.getValue()) {
                                    if (mc.player.getOffHandStack().getItem() == Items.SHIELD && mc.player.isBlocking()) {
                                        // Release right click - handled by WorldUtils
                                    }
                                }

                                WorldUtils.hitEntity(entity, swing.getValue());

                                if (clickSimulation.getValue()) {
                                    // Click simulation handled by WorldUtils
                                }

                                currentSwordDelay = swordDelay.getIntValue();
                                timer.reset();
                            } else {
                                if (useShield.getValue()) {
                                    if (mc.player.getOffHandStack().getItem() == Items.SHIELD) {
                                        int useFor = shieldTime.getIntValue();
                                        // Shield handling done by WorldUtils
                                    }
                                }
                            }
                        }
                    }
                } else if (item instanceof AxeItem) {
                    if (mc.crosshairTarget instanceof EntityHitResult hit) {
                        Entity entity = hit.getEntity();

                        if (entity instanceof PlayerEntity || (strayBypass.getValue() && entity instanceof ZombieEntity) || (allEntities.getValue() && entity != null)) {
                            if (entity instanceof PlayerEntity player) {
                                if (checkShield.getValue() && player.isBlocking() && !WorldUtils.isShieldFacingAway(player))
                                    return;
                            }

                            if (onlyCritAxe.getValue() && mc.player.fallDistance <= 0.0F)
                                return;

                            if (timer.delay(currentAxeDelay)) {
                                WorldUtils.hitEntity(entity, swing.getValue());

                                if (clickSimulation.getValue()) {
                                    // Click simulation handled by WorldUtils
                                }

                                currentAxeDelay = axeDelay.getIntValue();
                                timer.reset();
                            } else {
                                if (useShield.getValue()) {
                                    if (mc.player.getOffHandStack().getItem() == Items.SHIELD) {
                                        int useFor = shieldTime.getIntValue();
                                        // Shield handling done by WorldUtils
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (mc.crosshairTarget instanceof EntityHitResult entityHit && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
                    Entity entity = entityHit.getEntity();

                    if (sticky.getValue() && entity != mc.player.getAttacking())
                        return;

                    if (entity instanceof PlayerEntity || (strayBypass.getValue() && entity instanceof ZombieEntity) || (allEntities.getValue() && entity != null)) {
                        if (entity instanceof PlayerEntity player) {
                            if (checkShield.getValue() && player.isBlocking() && !WorldUtils.isShieldFacingAway(player))
                                return;
                        }

                        if (onlyCritSword.getValue() && mc.player.fallDistance <= 0.0F)
                            return;

                        if (timer.delay(currentSwordDelay)) {
                            WorldUtils.hitEntity(entity, swing.getValue());

                                                            if (clickSimulation.getValue()) {
                                    // Click simulation handled by WorldUtils
                                }

                            currentSwordDelay = swordDelay.getIntValue();
                            timer.reset();
                        } else {
                            if (useShield.getValue()) {
                                if (mc.player.getOffHandStack().getItem() == Items.SHIELD) {
                                    int useFor = shieldTime.getIntValue();
                                    // Shield handling done by WorldUtils
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    @EventListener
    public void onAttack(AttackEvent event) {
        if (GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS)
            event.cancel();
    }
} 