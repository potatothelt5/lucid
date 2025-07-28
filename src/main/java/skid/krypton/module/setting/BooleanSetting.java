package skid.krypton.module.setting;

public final class BooleanSetting extends Setting {
    private boolean value;
    private final boolean defaultValue;

    public BooleanSetting(final CharSequence charSequence, final boolean value) {
        super(charSequence);
        this.value = value;
        this.defaultValue = value;
    }

    public void toggle() {
        this.setValue(!this.value);
        this.onChanged();
    }

    public void setValue(final boolean a) {
        this.value = a;
        this.onChanged();
    }

    public boolean getDefaultValue() {
        return this.defaultValue;
    }

    public boolean getValue() {
        return this.value;
    }

    public BooleanSetting setDescription(final CharSequence charSequence) {
        super.setDescription(charSequence);
        return this;
    }
}