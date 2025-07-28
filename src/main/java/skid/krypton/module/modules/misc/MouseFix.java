package skid.krypton.module.modules.misc;

import skid.krypton.module.Category;
import skid.krypton.module.Module;
import skid.krypton.module.setting.BooleanSetting;
import skid.krypton.utils.EncryptedString;

public final class MouseFix extends Module {
    private final BooleanSetting customDebounce = new BooleanSetting(EncryptedString.of("Custom Debounce"), true)
            .setDescription(EncryptedString.of("Implements a custom debounce timer on mouse inputs"));

    public MouseFix() {
        super(EncryptedString.of("Mouse Fix"), EncryptedString.of("Fixes multiple mouse issues"), -1, Category.MISC);
        this.addSettings(customDebounce);
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
} 