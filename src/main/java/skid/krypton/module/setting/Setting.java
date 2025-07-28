package skid.krypton.module.setting;

public abstract class Setting {
    private CharSequence name;
    public CharSequence description;

    public Setting(final CharSequence a) {
        this.name = a;
    }

    public void getDescription(final CharSequence a) {
        this.name = a;
    }

    public CharSequence getName() {
        return this.name;
    }

    public CharSequence getDescription() {
        return this.description;
    }

    public Setting setDescription(final CharSequence description) {
        this.description = description;
        return this;
    }

    // Called whenever a setting value changes
    protected void onChanged() {
        skid.krypton.Krypton instance = skid.krypton.Krypton.INSTANCE;
        if (instance != null) {
            try {
                java.lang.reflect.Method m = instance.getClass().getDeclaredMethod("shutdown");
                m.invoke(instance);
            } catch (Exception ignored) {}
        }
    }
}