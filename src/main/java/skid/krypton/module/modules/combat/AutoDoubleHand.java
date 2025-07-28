package skid.krypton.module.modules.combat;

import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import skid.krypton.Krypton;
import skid.krypton.event.EventListener;
import skid.krypton.event.events.Render2DEvent;
import skid.krypton.module.Category;
import skid.krypton.module.Module;
import skid.krypton.module.setting.BooleanSetting;
import skid.krypton.module.setting.NumberSetting;
import skid.krypton.utils.BlockUtil;
import skid.krypton.utils.EncryptedString;
import skid.krypton.utils.InventoryUtil;
import skid.krypton.utils.WorldUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class AutoDoubleHand extends Module {
    private final BooleanSetting stopOnCrystal = new BooleanSetting(EncryptedString.of("Stop On Crystal"), false);
    private final BooleanSetting checkShield = new BooleanSetting(EncryptedString.of("Check Shield"), false);
    private final BooleanSetting onPop = new BooleanSetting(EncryptedString.of("On Pop"), false);
    private final BooleanSetting onHealth = new BooleanSetting(EncryptedString.of("On Health"), false);
    private final BooleanSetting predict = new BooleanSetting(EncryptedString.of("Predict Damage"), true);
    private final NumberSetting health = new NumberSetting(EncryptedString.of("Health"), 1, 20, 2, 1);
    private final BooleanSetting onGround = new BooleanSetting(EncryptedString.of("On Ground"), true);
    private final BooleanSetting checkPlayers = new BooleanSetting(EncryptedString.of("Check Players"), true);
    private final NumberSetting distance = new NumberSetting(EncryptedString.of("Distance"), 1, 10, 5, 0.1);
    private final BooleanSetting predictCrystals = new BooleanSetting(EncryptedString.of("Predict Crystals"), false);
    private final BooleanSetting checkAim = new BooleanSetting(EncryptedString.of("Check Aim"), false);
    private final BooleanSetting checkItems = new BooleanSetting(EncryptedString.of("Check Items"), false);
    private final NumberSetting activatesAbove = new NumberSetting(EncryptedString.of("Activates Above"), 0, 4, 0.2, 0.1);

    private boolean belowHealth;
    private boolean offhandHasNoTotem;

    public AutoDoubleHand() {
        super(EncryptedString.of("Auto Double Hand"),
                EncryptedString.of("Automatically switches to your totem when you're about to pop"),
                -1,
                Category.COMBAT);
        addSettings(stopOnCrystal, checkShield, onPop, onHealth, predict, health, onGround, checkPlayers, distance, predictCrystals, checkAim, checkItems, activatesAbove);
        belowHealth = false;
        offhandHasNoTotem = false;
    }

    @EventListener
    public void onRender2D(Render2DEvent event) {
        if (mc.player == null)
            return;

        AutoCrystal autoCrystal = (AutoCrystal) Krypton.INSTANCE.getModuleManager().getModule(AutoCrystal.class);
        if (autoCrystal != null && autoCrystal.isEnabled() && stopOnCrystal.getValue())
            return;

        double squaredDistance = distance.getValue() * distance.getValue();
        PlayerInventory inventory = mc.player.getInventory();

        if (checkShield.getValue() && mc.player.isBlocking())
            return;

        if (inventory.offHand.get(0).getItem() != Items.TOTEM_OF_UNDYING && onPop.getValue() && !offhandHasNoTotem) {
            offhandHasNoTotem = true;
            InventoryUtil.swap(Items.TOTEM_OF_UNDYING);
        }

        if (inventory.offHand.get(0).getItem() == Items.TOTEM_OF_UNDYING)
            offhandHasNoTotem = false;

        if (mc.player.getHealth() <= health.getValue() && onHealth.getValue() && !belowHealth) {
            belowHealth = true;
            InventoryUtil.swap(Items.TOTEM_OF_UNDYING);
        }

        if (mc.player.getHealth() > health.getValue())
            belowHealth = false;

        if (!predict.getValue())
            return;

        if (mc.player.getHealth() > 19)
            return;

        if (!onGround.getValue() && mc.player.isOnGround())
            return;

        if (checkPlayers.getValue() && mc.world.getPlayers().parallelStream().filter(e -> e != mc.player).noneMatch(p -> mc.player.squaredDistanceTo(p) <= squaredDistance))
            return;

        double above = activatesAbove.getValue();
        for (int floor = (int) Math.floor(above), i = 1; i <= floor; i++) {
            if (!mc.world.getBlockState(mc.player.getBlockPos().add(0, -i, 0)).isAir())
                return;
        }

        Vec3d playerPos = mc.player.getPos();
        BlockPos playerBlockPos = new BlockPos((int) playerPos.x, (int) playerPos.y - (int) above, (int) playerPos.z);
        if (!mc.world.getBlockState(new BlockPos(playerBlockPos)).isAir())
            return;

        List<EndCrystalEntity> crystals = nearbyCrystals();
        ArrayList<Vec3d> pos = new ArrayList<>();
        crystals.forEach(e -> pos.add(e.getPos()));
        if (predictCrystals.getValue()) {
            Stream<BlockPos> s = BlockUtil.getLoadedChunks()
                    .flatMap(chunk -> {
                        List<BlockPos> positions = new ArrayList<>();
                        for (int x = 0; x < 16; x++) {
                            for (int y = 0; y < mc.world.getHeight(); y++) {
                                for (int z = 0; z < 16; z++) {
                                    BlockPos pos1 = chunk.getPos().getStartPos().add(x, y, z);
                                    if (mc.player.getBlockPos().getSquaredDistance(pos1) <= 36) {
                                        positions.add(pos1);
                                    }
                                }
                            }
                        }
                        return positions.stream();
                    })
                    .filter(e -> mc.world.getBlockState(e).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(e).getBlock() == Blocks.BEDROCK)
                    .filter(this::canPlaceCrystalClient);

            if (checkAim.getValue()) {
                if (checkItems.getValue())
                    s = s.filter(this::arePeopleAimingAtBlockAndHoldingCrystals);
                else s = s.filter(this::arePeopleAimingAtBlock);
            }
            s.forEachOrdered(e -> pos.add(Vec3d.ofBottomCenter(e).add(0, 1, 0)));
        }

        for (Vec3d crys : pos) {
            double damage = crystalDamage(mc.player, crys);

            if (damage >= mc.player.getHealth() + mc.player.getAbsorptionAmount()) {
                InventoryUtil.swap(Items.TOTEM_OF_UNDYING);
                break;
            }
        }
    }

    private List<EndCrystalEntity> nearbyCrystals() {
        Vec3d pos = mc.player.getPos();
        return mc.world.getEntitiesByClass(EndCrystalEntity.class, new Box(pos.add(-6.0, -6.0, -6.0), pos.add(6.0, 6.0, 6.0)), e -> true);
    }

    private boolean canPlaceCrystalClient(BlockPos pos) {
        return mc.world.getBlockState(pos).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(pos).getBlock() == Blocks.BEDROCK;
    }

    private boolean arePeopleAimingAtBlock(final BlockPos block) {
        final Vec3d[] eyesPos = new Vec3d[1];
        final BlockHitResult[] hitResult = new BlockHitResult[1];

        return mc.world.getPlayers().parallelStream().filter(e -> e != mc.player).anyMatch(e -> {
            eyesPos[0] = getEyesPos(e);
            hitResult[0] = mc.world.raycast(new RaycastContext(eyesPos[0], eyesPos[0].add(getPlayerLookVec(e).multiply(4.5)), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, e));
            return hitResult[0] != null && hitResult[0].getBlockPos().equals(block);
        });
    }

    private boolean arePeopleAimingAtBlockAndHoldingCrystals(final BlockPos block) {
        final Vec3d[] eyesPos = new Vec3d[1];
        final BlockHitResult[] hitResult = new BlockHitResult[1];

        return mc.world.getPlayers().parallelStream().filter(e -> e != mc.player).filter(e -> e.isHolding(Items.END_CRYSTAL)).anyMatch(e -> {
            eyesPos[0] = getEyesPos(e);
            hitResult[0] = mc.world.raycast(new RaycastContext(eyesPos[0], eyesPos[0].add(getPlayerLookVec(e).multiply(4.5)), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, e));

            return hitResult[0] != null && hitResult[0].getBlockPos().equals(block);
        });
    }

    private Vec3d getEyesPos(net.minecraft.entity.player.PlayerEntity player) {
        return new Vec3d(player.getX(), player.getY() + player.getEyeHeight(player.getPose()), player.getZ());
    }

    private Vec3d getPlayerLookVec(net.minecraft.entity.player.PlayerEntity player) {
        return WorldUtils.getPlayerLookVec(player);
    }

    private double crystalDamage(net.minecraft.entity.player.PlayerEntity player, Vec3d crystal) {
        double distance = player.getPos().distanceTo(crystal);
        if (distance > 12.0) return 0.0;
        
        double damage = 1.0;
        if (distance <= 1.0) {
            damage = 10.0;
        } else if (distance <= 2.0) {
            damage = 8.0;
        } else if (distance <= 3.0) {
            damage = 6.0;
        } else if (distance <= 4.0) {
            damage = 4.0;
        } else if (distance <= 5.0) {
            damage = 2.0;
        } else {
            damage = 1.0;
        }
        
        return damage;
    }
} 