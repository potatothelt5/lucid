package skid.krypton.module.setting;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryKey;

import java.util.ArrayList;
import java.util.List;

public final class EnchantmentSetting extends Setting {
    private List<RegistryKey<Enchantment>> enchantments;

    public EnchantmentSetting(final CharSequence name) {
        super(name);
        this.enchantments = new ArrayList<>();
    }

    public EnchantmentSetting(final CharSequence name, final List<RegistryKey<Enchantment>> enchantments) {
        super(name);
        this.enchantments = enchantments;
    }

    public List<RegistryKey<Enchantment>> getEnchantments() {
        return this.enchantments;
    }

    public void setEnchantments(final List<RegistryKey<Enchantment>> enchantments) {
        this.enchantments = enchantments;
    }

    public void addEnchantment(final RegistryKey<Enchantment> enchantment) {
        if (!this.enchantments.contains(enchantment)) {
            this.enchantments.add(enchantment);
        }
    }

    public void removeEnchantment(final RegistryKey<Enchantment> enchantment) {
        this.enchantments.remove(enchantment);
    }

    public void clearEnchantments() {
        this.enchantments.clear();
    }

    public boolean hasEnchantment(final RegistryKey<Enchantment> enchantment) {
        return this.enchantments.contains(enchantment);
    }

    public boolean isEmpty() {
        return this.enchantments.isEmpty();
    }

    public int size() {
        return this.enchantments.size();
    }

    public EnchantmentSetting setDescription(final CharSequence charSequence) {
        super.setDescription(charSequence);
        return this;
    }
} 