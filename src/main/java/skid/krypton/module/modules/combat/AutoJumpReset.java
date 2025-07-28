package skid.krypton.module.modules.combat;

import skid.krypton.event.EventListener;
import skid.krypton.event.events.TickEvent;
import skid.krypton.module.Category;
import skid.krypton.module.Module;
import skid.krypton.module.setting.NumberSetting;
import skid.krypton.utils.EncryptedString;

import java.util.Random;

public final class AutoJumpReset extends Module {
    private final NumberSetting chance = new NumberSetting(EncryptedString.of("Chance"), 0, 100, 100, 1);
    private final Random random = new Random();

    public AutoJumpReset() {
        super(EncryptedString.of("Auto Jump Reset"),
                EncryptedString.of("Automatically jumps for you when you get hit so you take less knockback (not good for crystal pvp)"),
                -1,
                Category.COMBAT);
        addSettings(chance);
    }

    @EventListener
    public void onTick(TickEvent event) {
        if (random.nextInt(100) + 1 <= chance.getIntValue()) {
            if (mc.currentScreen != null)
                return;

            if (mc.player.isUsingItem())
                return;

            if (mc.player.hurtTime == 0)
                return;

            if (mc.player.hurtTime == mc.player.maxHurtTime)
                return;

            if (!mc.player.isOnGround())
                return;

            if (mc.player.hurtTime == 9 && random.nextInt(100) + 1 <= chance.getIntValue())
                mc.player.jump();
        }
    }
} 