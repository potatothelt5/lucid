package skid.krypton.module.modules.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import skid.krypton.event.EventListener;
import skid.krypton.event.events.PacketSendEvent;
import skid.krypton.module.Category;
import skid.krypton.module.Module;
import skid.krypton.utils.EncryptedString;
import skid.krypton.utils.WorldUtils;

public final class CrystalOptimizer extends Module {
    public CrystalOptimizer() {
        super(EncryptedString.of("Crystal Optimizer"),
                EncryptedString.of("Makes your crystals disappear faster client-side so you can place crystals faster"),
                -1,
                Category.COMBAT);
    }

    @EventListener
    public void onPacketSend(PacketSendEvent event) {
        if (event.packet instanceof PlayerInteractEntityC2SPacket interactPacket) {
            interactPacket.handle(new PlayerInteractEntityC2SPacket.Handler() {
                @Override
                public void interact(Hand hand) {

                }

                @Override
                public void interactAt(Hand hand, Vec3d pos) {

                }

                @Override
                public void attack() {
                    if (mc.crosshairTarget == null)
                        return;

                    if (mc.crosshairTarget.getType() == HitResult.Type.ENTITY && mc.crosshairTarget instanceof EntityHitResult hit) {
                        if (hit.getEntity() instanceof EndCrystalEntity) {
                            StatusEffectInstance weakness = mc.player.getStatusEffect(StatusEffects.WEAKNESS);
                            StatusEffectInstance strength = mc.player.getStatusEffect(StatusEffects.STRENGTH);
                            if (!(weakness == null || strength != null && strength.getAmplifier() > weakness.getAmplifier() || WorldUtils.isTool(mc.player.getMainHandStack())))
                                return;

                            hit.getEntity().kill();
                            hit.getEntity().setRemoved(Entity.RemovalReason.KILLED);
                            hit.getEntity().onRemoved();
                        }
                    }
                }
            });
        }
    }
} 