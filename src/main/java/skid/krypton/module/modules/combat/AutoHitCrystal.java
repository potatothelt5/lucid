package skid.krypton.module.modules.combat;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;
import skid.krypton.Krypton;
import skid.krypton.event.EventListener;
import skid.krypton.event.events.AttackEvent;
import skid.krypton.event.events.PostItemUseEvent;
import skid.krypton.event.events.TickEvent;
import skid.krypton.module.Category;
import skid.krypton.module.Module;
import skid.krypton.module.setting.BindSetting;
import skid.krypton.module.setting.BooleanSetting;
import skid.krypton.module.setting.NumberSetting;
import skid.krypton.utils.*;

import java.util.Random;

public final class AutoHitCrystal extends Module {
    private final BindSetting activateKey = new BindSetting(EncryptedString.of("Activate Key"), GLFW.GLFW_MOUSE_BUTTON_RIGHT, false);
    private final BooleanSetting checkPlace = new BooleanSetting(EncryptedString.of("Check Place"), false);
    private final NumberSetting switchDelay = new NumberSetting(EncryptedString.of("Switch Delay"), 0, 20, 0, 1);
    private final NumberSetting switchChance = new NumberSetting(EncryptedString.of("Switch Chance"), 0, 100, 100, 1);
    private final NumberSetting placeDelay = new NumberSetting(EncryptedString.of("Place Delay"), 0, 20, 0, 1);
    private final NumberSetting placeChance = new NumberSetting(EncryptedString.of("Place Chance"), 0, 100, 100, 1);
    private final BooleanSetting workWithTotem = new BooleanSetting(EncryptedString.of("Work With Totem"), false);
    private final BooleanSetting workWithCrystal = new BooleanSetting(EncryptedString.of("Work With Crystal"), false);
    private final BooleanSetting clickSimulation = new BooleanSetting(EncryptedString.of("Click Simulation"), false);
    private final BooleanSetting swordSwap = new BooleanSetting(EncryptedString.of("Sword Swap"), true);

    private int placeClock = 0;
    private int switchClock = 0;
    private boolean active;
    private boolean crystalling;
    private boolean crystalSelected;
    private final Random random = new Random();

    public AutoHitCrystal() {
        super(EncryptedString.of("Auto Hit Crystal"),
                EncryptedString.of("Automatically hit-crystals for you"),
                -1,
                Category.COMBAT);
        addSettings(activateKey, checkPlace, switchDelay, switchChance, placeDelay, placeChance, workWithTotem, workWithCrystal, clickSimulation, swordSwap);
    }

    @EventListener
    public void onTick(TickEvent event) {
        int randomNum = random.nextInt(100) + 1;

        if (mc.currentScreen != null)
            return;

        if (KeyUtils.isKeyPressed(activateKey.getValue())) {
            if (mc.crosshairTarget instanceof BlockHitResult hitResult && mc.crosshairTarget.getType() == HitResult.Type.BLOCK)
                if (!active && !canPlaceBlockClient(hitResult.getBlockPos()) && checkPlace.getValue())
                    return;

            ItemStack mainHandStack = mc.player.getMainHandStack();

            if (!(mainHandStack.getItem() instanceof SwordItem || (workWithTotem.getValue() && mainHandStack.isOf(Items.TOTEM_OF_UNDYING)) || workWithCrystal.getValue() && mainHandStack.isOf(Items.END_CRYSTAL)) && !active)
                return;
            else if (mc.crosshairTarget instanceof BlockHitResult hitResult && !active) {
                if (swordSwap.getValue()) {
                    if (mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
                        Block block = mc.world.getBlockState(hitResult.getBlockPos()).getBlock();

                        crystalling = block == Blocks.OBSIDIAN || block == Blocks.BEDROCK;
                    }
                }
            }

            active = true;

            if (!crystalling) {
                if (mc.crosshairTarget instanceof BlockHitResult hit) {
                    if (hit.getType() == HitResult.Type.MISS)
                        return;

                    if (!BlockUtil.isBlockAtPosition(hit.getBlockPos(), Blocks.OBSIDIAN)) {
                        if (BlockUtil.isBlockAtPosition(hit.getBlockPos(), Blocks.RESPAWN_ANCHOR) && BlockUtil.isRespawnAnchorCharged(hit.getBlockPos()))
                            return;

                        mc.options.useKey.setPressed(false);

                        if (!mc.player.isHolding(Items.OBSIDIAN)) {
                            if (switchClock > 0) {
                                switchClock--;
                                return;
                            }

                            if (randomNum <= switchChance.getValue()) {
                                switchClock = (int) switchDelay.getValue();
                                InventoryUtil.swap(Items.OBSIDIAN);
                            }
                        }

                        if (mc.player.isHolding(Items.OBSIDIAN)) {
                            if (placeClock > 0) {
                                placeClock--;
                                return;
                            }

                            if (clickSimulation.getValue())
                                mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);

                            randomNum = random.nextInt(100) + 1;

                            if (randomNum <= placeChance.getValue()) {
                                WorldUtils.placeBlock(hit, true);

                                placeClock = (int) placeDelay.getValue();
                                crystalling = true;
                            }
                        }
                    }
                }
            }

            if (crystalling) {
                if (!mc.player.isHolding(Items.END_CRYSTAL) && !crystalSelected) {
                    if (switchClock > 0) {
                        switchClock--;
                        return;
                    }

                    randomNum = random.nextInt(100) + 1;

                    if (randomNum <= switchChance.getValue()) {
                        crystalSelected = InventoryUtil.swap(Items.END_CRYSTAL);
                        switchClock = (int) switchDelay.getValue();
                    }
                }

                if (mc.player.isHolding(Items.END_CRYSTAL)) {
                    AutoCrystal autoCrystal = (AutoCrystal) Krypton.INSTANCE.getModuleManager().getModule(AutoCrystal.class);

                    if (!autoCrystal.isEnabled())
                        autoCrystal.onEnable();
                }
            }
        } else reset();
    }

    @EventListener
    public void onItemUse(PostItemUseEvent event) {
        ItemStack mainHandStack = mc.player.getMainHandStack();
        if ((mainHandStack.isOf(Items.END_CRYSTAL) || mainHandStack.isOf(Items.OBSIDIAN)) && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) != GLFW.GLFW_PRESS)
            event.cancel();
    }

    public void reset() {
        placeClock = (int) placeDelay.getValue();
        switchClock = (int) switchDelay.getValue();
        active = false;
        crystalling = false;
        crystalSelected = false;
    }

    @EventListener
    public void onAttack(AttackEvent event) {
        if (mc.player.getMainHandStack().isOf(Items.END_CRYSTAL) && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS)
            event.cancel();
    }

    private boolean canPlaceBlockClient(net.minecraft.util.math.BlockPos pos) {
        return mc.world.getBlockState(pos).isAir();
    }
} 