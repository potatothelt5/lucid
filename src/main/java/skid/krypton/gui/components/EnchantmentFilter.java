package skid.krypton.gui.components;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import skid.krypton.module.modules.client.Krypton;
import skid.krypton.module.setting.EnchantmentSetting;
import skid.krypton.utils.RenderUtils;
import skid.krypton.utils.TextRenderer;
import skid.krypton.utils.Utils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class EnchantmentFilter extends Screen {
    private final EnchantmentSetting setting;
    private String searchQuery;
    private final List<RegistryKey<Enchantment>> allEnchantments;
    private List<RegistryKey<Enchantment>> filteredEnchantments;
    private int scrollOffset;
    private final int ENCHANTMENTS_PER_ROW = 8;
    private final int MAX_ROWS_VISIBLE = 5;
    private int selectedIndex;
    private final int ENCHANTMENT_SIZE = 60;
    private final int ENCHANTMENT_SPACING = 10;
    final /* synthetic */ EnchantmentBox this$0;

    public EnchantmentFilter(final EnchantmentBox this$0, final EnchantmentSetting setting) {
        super(Text.empty());
        this.this$0 = this$0;
        this.searchQuery = "";
        this.scrollOffset = 0;
        this.selectedIndex = -1;
        this.setting = setting;
        this.allEnchantments = new ArrayList<>();
        this.getAllEnchantments();
        this.filteredEnchantments = new ArrayList<>(this.allEnchantments);
    }

    @Override
    public void render(final DrawContext drawContext, final int n, final int n2, final float n3) {
        RenderUtils.unscaledProjection();
        final int n4 = n * (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
        final int n5 = n2 * (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
        super.render(drawContext, n4, n5, n3);
        final int width = this.this$0.mc.getWindow().getWidth();
        final int height = this.this$0.mc.getWindow().getHeight();
        int a;
        if (Krypton.renderBackground.getValue()) {
            a = 180;
        } else {
            a = 0;
        }
        drawContext.fill(0, 0, width, height, new Color(0, 0, 0, a).getRGB());
        final int n6 = (this.this$0.mc.getWindow().getWidth() - 800) / 2;
        final int n7 = (this.this$0.mc.getWindow().getHeight() - 600) / 2;
        RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(30, 30, 35, 240), n6, n7, n6 + 800, n7 + 600, 8.0, 8.0, 8.0, 8.0, 20.0);
        RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(40, 40, 45, 255), n6, n7, n6 + 800, n7 + 30, 8.0, 8.0, 0.0, 0.0, 20.0);
        drawContext.fill(n6, n7 + 30, n6 + 800, n7 + 31, Utils.getMainColor(255, 1).getRGB());
        TextRenderer.drawCenteredString("Select Enchantments: " + this.setting.getName(), drawContext, n6 + 400, n7 + 8, new Color(245, 245, 245, 255).getRGB());
        final int n8 = n6 + 20;
        final int n9 = n7 + 50;
        RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(20, 20, 25, 255), n8, n9, n8 + 760, n9 + 30, 5.0, 5.0, 5.0, 5.0, 20.0);
        RenderUtils.renderRoundedOutline(drawContext, new Color(60, 60, 65, 255), n8, n9, n8 + 760, n9 + 30, 5.0, 5.0, 5.0, 5.0, 1.0, 20.0);
        final String searchQuery = this.searchQuery;
        String s;
        if (System.currentTimeMillis() % 1000L > 500L) {
            s = "|";
        } else {
            s = "";
        }
        TextRenderer.drawString("Search: " + searchQuery + s, drawContext, n8 + 10, n9 + 9, new Color(200, 200, 200, 255).getRGB());
        final int n10 = n6 + 20;
        final int n11 = n9 + 30 + 15;
        final int n12 = 600 - (n11 - n7) - 60;
        RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(25, 25, 30, 255), n10, n11, n10 + 760, n11 + n12, 5.0, 5.0, 5.0, 5.0, 20.0);
        final double ceil = Math.ceil(this.filteredEnchantments.size() / 8.0);
        final int max = Math.max(0, (int) ceil - 7);
        this.scrollOffset = Math.min(this.scrollOffset, max);
        if ((int) ceil > 7) {
            final int n13 = n10 + 760 - 6 - 5;
            final int n14 = n11 + 5;
            final int n15 = n12 - 10;
            RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(20, 20, 25, 150), n13, n14, n13 + 6, n14 + n15, 3.0, 3.0, 3.0, 3.0, 20.0);
            final float n16 = this.scrollOffset / (float) max;
            final float max2 = Math.max(40.0f, n15 * (7.0f / (int) ceil));
            final int n17 = n14 + (int) ((n15 - max2) * n16);
            RenderUtils.renderRoundedQuad(drawContext.getMatrices(), Utils.getMainColor(255, 1), n13, n17, n13 + 6, n17 + max2, 3.0, 3.0, 3.0, 3.0, 20.0);
        }
        int i;
        for (int n18 = i = this.scrollOffset * 8; i < Math.min(n18 + Math.min(this.filteredEnchantments.size(), 56), this.filteredEnchantments.size()); ++i) {
            final int n19 = n10 + 5 + (i - n18) % 8 * 95;
            final int n20 = n11 + 5 + (i - n18) / 8 * 85;
            Color mainColor;
            if (i == this.selectedIndex) {
                mainColor = Utils.getMainColor(100, 1);
            } else {
                mainColor = new Color(35, 35, 40, 255);
            }
            RenderUtils.renderRoundedQuad(drawContext.getMatrices(), mainColor, n19, n20, n19 + 90, n20 + 80, 4.0, 4.0, 4.0, 4.0, 20.0);
            
            // Draw enchantment name only, no book icon
            RegistryKey<Enchantment> enchantment = this.filteredEnchantments.get(i);
            String enchantmentName = getEnchantmentDisplayName(enchantment);
            
            // Special color for Amethyst Drill
            int textColor = (enchantment == null) ? new Color(255, 215, 0).getRGB() : new Color(255, 255, 255, 255).getRGB();
            
            // Split long names into multiple lines if needed
            String[] lines = splitEnchantmentName(enchantmentName);
            for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
                String line = lines[lineIndex];
                int yOffset = n20 + 20 + (lineIndex * 15);
                if (yOffset + 15 <= n20 + 80) {
                    TextRenderer.drawCenteredString(line, drawContext, n19 + 45, yOffset, textColor);
                }
            }
            
            // Draw level text for Amethyst Drill
            if (enchantment == null) {
                String levelText = getEnchantmentLevelText(enchantment);
                TextRenderer.drawCenteredString(levelText, drawContext, n19 + 45, n20 + 55, new Color(150, 150, 150).getRGB());
            }
            
            if (n4 >= n19 && n4 <= n19 + 90 && n5 >= n20 && n5 <= n20 + 80) {
                RenderUtils.renderRoundedOutline(drawContext, Utils.getMainColor(200, 1), n19, n20, n19 + 90, n20 + 80, 4.0, 4.0, 4.0, 4.0, 1.0, 20.0);
            }
        }
        if (this.filteredEnchantments.isEmpty()) {
            TextRenderer.drawCenteredString("No enchantments found", drawContext, n10 + 380, n11 + n12 / 2 - 10, new Color(150, 150, 150, 200).getRGB());
        }
        final int n21 = n7 + 600 - 45;
        final int n22 = n6 + 800 - 80 - 20;
        final int n23 = n22 - 80 - 10;
        final int n24 = n23 - 80 - 10;
        RenderUtils.renderRoundedQuad(drawContext.getMatrices(), Utils.getMainColor(255, 1), n22, n21, n22 + 80, n21 + 30, 5.0, 5.0, 5.0, 5.0, 20.0);
        TextRenderer.drawCenteredString("Save", drawContext, n22 + 40, n21 + 8, new Color(245, 245, 245, 255).getRGB());
        RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(60, 60, 65, 255), n23, n21, n23 + 80, n21 + 30, 5.0, 5.0, 5.0, 5.0, 20.0);
        TextRenderer.drawCenteredString("Cancel", drawContext, n23 + 40, n21 + 8, new Color(245, 245, 245, 255).getRGB());
        RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(70, 40, 40, 255), n24, n21, n24 + 80, n21 + 30, 5.0, 5.0, 5.0, 5.0, 20.0);
        TextRenderer.drawCenteredString("Reset", drawContext, n24 + 40, n21 + 8, new Color(245, 245, 245, 255).getRGB());
        RenderUtils.scaledProjection();
    }

    public boolean mouseClicked(final double n, final double n2, final int n3) {
        final double n4 = n * MinecraftClient.getInstance().getWindow().getScaleFactor();
        final double n5 = n2 * MinecraftClient.getInstance().getWindow().getScaleFactor();
        final int n6 = (this.this$0.mc.getWindow().getWidth() - 800) / 2;
        final int n7 = (this.this$0.mc.getWindow().getHeight() - 600) / 2;
        final int n8 = n7 + 600 - 45;
        final int n9 = n6 + 800 - 80 - 20;
        final int n10 = n9 - 80 - 10;
        if (this.isInBounds(n4, n5, n9, n8, 80, 30)) {
            if (this.selectedIndex >= 0 && this.selectedIndex < this.filteredEnchantments.size()) {
                RegistryKey<Enchantment> selectedEnchantment = this.filteredEnchantments.get(this.selectedIndex);
                List<RegistryKey<Enchantment>> currentEnchantments = new ArrayList<>(this.setting.getEnchantments());
                if (currentEnchantments.contains(selectedEnchantment)) {
                    currentEnchantments.remove(selectedEnchantment);
                    } else {
                    currentEnchantments.add(selectedEnchantment);
                }
                this.setting.setEnchantments(currentEnchantments);
            }
            this.this$0.mc.setScreen(skid.krypton.Krypton.INSTANCE.GUI);
            return true;
        }
        if (this.isInBounds(n4, n5, n10, n8, 80, 30)) {
            this.this$0.mc.setScreen(skid.krypton.Krypton.INSTANCE.GUI);
            return true;
        }
        if (this.isInBounds(n4, n5, n10 - 80 - 10, n8, 80, 30)) {
            this.setting.setEnchantments(new ArrayList<>());
            this.selectedIndex = -1;
            return true;
        }
        final int n11 = n6 + 20;
        final int n12 = n7 + 50 + 30 + 15;
        if (this.isInBounds(n4, n5, n11, n12, 760, 600 - (n12 - n7) - 60)) {
            final int n13 = this.scrollOffset * 8;
            final int n14 = (int) (n4 - n11 - 5.0) / 95;
            if (n14 >= 0 && n14 < 8) {
                final int selectedIndex = n13 + (int) (n5 - n12 - 5.0) / 85 * 8 + n14;
                if (selectedIndex >= 0 && selectedIndex < this.filteredEnchantments.size()) {
                    this.selectedIndex = selectedIndex;
                    return true;
                }
            }
        }
        return super.mouseClicked(n4, n5, n3);
    }

    public boolean mouseScrolled(final double n, final double n2, final double n3, final double n4) {
        final double n5 = n * MinecraftClient.getInstance().getWindow().getScaleFactor();
        final double n6 = n2 * MinecraftClient.getInstance().getWindow().getScaleFactor();
        final int width = this.this$0.mc.getWindow().getWidth();
        final int n7 = (this.this$0.mc.getWindow().getHeight() - 600) / 2;
        final int n8 = n7 + 50 + 30 + 15;
        if (this.isInBounds(n5, n6, (width - 800) / 2 + 20, n8, 760, 600 - (n8 - n7) - 60)) {
            final int max = Math.max(0, (int) Math.ceil(this.filteredEnchantments.size() / 8.0) - 7);
            if (n4 > 0.0) {
                this.scrollOffset = Math.max(0, this.scrollOffset - 1);
            } else if (n4 < 0.0) {
                this.scrollOffset = Math.min(max, this.scrollOffset + 1);
            }
            return true;
        }
        return super.mouseScrolled(n5, n6, n3, n4);
    }

    public boolean keyPressed(final int n, final int n2, final int n3) {
        if (n == 256) {
            if (this.selectedIndex >= 0 && this.selectedIndex < this.filteredEnchantments.size()) {
                RegistryKey<Enchantment> selectedEnchantment = this.filteredEnchantments.get(this.selectedIndex);
                List<RegistryKey<Enchantment>> currentEnchantments = new ArrayList<>(this.setting.getEnchantments());
                if (currentEnchantments.contains(selectedEnchantment)) {
                    currentEnchantments.remove(selectedEnchantment);
                } else {
                    currentEnchantments.add(selectedEnchantment);
                }
                this.setting.setEnchantments(currentEnchantments);
            }
            this.this$0.mc.setScreen(skid.krypton.Krypton.INSTANCE.GUI);
            return true;
        }
        if (n == 259) {
            if (!this.searchQuery.isEmpty()) {
                this.searchQuery = this.searchQuery.substring(0, this.searchQuery.length() - 1);
                this.updateFilteredEnchantments();
            }
            return true;
        }
        if (n == 265) {
            if (this.selectedIndex >= 8) {
                this.selectedIndex -= 8;
                this.ensureSelectedEnchantmentVisible();
            }
            return true;
        }
        if (n == 264) {
            if (this.selectedIndex + 8 < this.filteredEnchantments.size()) {
                this.selectedIndex += 8;
                this.ensureSelectedEnchantmentVisible();
            }
            return true;
        }
        if (n == 263) {
            if (this.selectedIndex > 0) {
                --this.selectedIndex;
                this.ensureSelectedEnchantmentVisible();
            }
            return true;
        }
        if (n == 262) {
            if (this.selectedIndex < this.filteredEnchantments.size() - 1) {
                ++this.selectedIndex;
                this.ensureSelectedEnchantmentVisible();
            }
            return true;
        }
        if (n == 257) {
            if (this.selectedIndex >= 0 && this.selectedIndex < this.filteredEnchantments.size()) {
                RegistryKey<Enchantment> selectedEnchantment = this.filteredEnchantments.get(this.selectedIndex);
                List<RegistryKey<Enchantment>> currentEnchantments = new ArrayList<>(this.setting.getEnchantments());
                if (currentEnchantments.contains(selectedEnchantment)) {
                    currentEnchantments.remove(selectedEnchantment);
                } else {
                    currentEnchantments.add(selectedEnchantment);
                }
                this.setting.setEnchantments(currentEnchantments);
                this.this$0.mc.setScreen(skid.krypton.Krypton.INSTANCE.GUI);
            }
            return true;
        }
        return super.keyPressed(n, n2, n3);
        }

    public boolean charTyped(final char c, final int n) {
        this.searchQuery += c;
        this.updateFilteredEnchantments();
        return true;
    }

    private void updateFilteredEnchantments() {
        if (this.searchQuery.isEmpty()) {
            this.filteredEnchantments = new ArrayList<>(this.allEnchantments);
        } else {
            this.filteredEnchantments = this.allEnchantments.stream()
                .filter(enchantment -> getEnchantmentDisplayName(enchantment).toLowerCase().contains(this.searchQuery.toLowerCase()))
                .collect(Collectors.toList());
        }
        this.scrollOffset = 0;
        this.selectedIndex = -1;
    }

    private void ensureSelectedEnchantmentVisible() {
        if (this.selectedIndex < 0) {
            return;
        }
        final int scrollOffset = this.selectedIndex / 8;
        if (scrollOffset < this.scrollOffset) {
            this.scrollOffset = scrollOffset;
        } else if (scrollOffset >= this.scrollOffset + 7) {
            this.scrollOffset = scrollOffset - 7 + 1;
        }
    }

    private boolean isInBounds(final double n, final double n2, final int n3, final int n4, final int n5, final int n6) {
        return n >= n3 && n <= n3 + n5 && n2 >= n4 && n2 <= n4 + n6;
    }

    public void renderBackground(final DrawContext drawContext, final int n, final int n2, final float n3) {
    }

    public boolean shouldCloseOnEsc() {
        return false;
    }

    private void getAllEnchantments() {
        // Add special Amethyst Drill option first
        this.allEnchantments.add(null); // Special marker for Amethyst Drill
        
        // Add common enchantments manually using registry keys
        this.allEnchantments.add(Enchantments.SHARPNESS);
        this.allEnchantments.add(Enchantments.SMITE);
        this.allEnchantments.add(Enchantments.BANE_OF_ARTHROPODS);
        this.allEnchantments.add(Enchantments.KNOCKBACK);
        this.allEnchantments.add(Enchantments.FIRE_ASPECT);
        this.allEnchantments.add(Enchantments.LOOTING);
        this.allEnchantments.add(Enchantments.SWEEPING_EDGE);
        this.allEnchantments.add(Enchantments.EFFICIENCY);
        this.allEnchantments.add(Enchantments.SILK_TOUCH);
        this.allEnchantments.add(Enchantments.FORTUNE);
        this.allEnchantments.add(Enchantments.UNBREAKING);
        this.allEnchantments.add(Enchantments.PROTECTION);
        this.allEnchantments.add(Enchantments.FIRE_PROTECTION);
        this.allEnchantments.add(Enchantments.BLAST_PROTECTION);
        this.allEnchantments.add(Enchantments.PROJECTILE_PROTECTION);
        this.allEnchantments.add(Enchantments.THORNS);
        this.allEnchantments.add(Enchantments.RESPIRATION);
        this.allEnchantments.add(Enchantments.AQUA_AFFINITY);
        this.allEnchantments.add(Enchantments.DEPTH_STRIDER);
        this.allEnchantments.add(Enchantments.FROST_WALKER);
        this.allEnchantments.add(Enchantments.FEATHER_FALLING);
        this.allEnchantments.add(Enchantments.POWER);
        this.allEnchantments.add(Enchantments.PUNCH);
        this.allEnchantments.add(Enchantments.FLAME);
        this.allEnchantments.add(Enchantments.INFINITY);
        this.allEnchantments.add(Enchantments.LUCK_OF_THE_SEA);
        this.allEnchantments.add(Enchantments.LURE);
        this.allEnchantments.add(Enchantments.LOYALTY);
        this.allEnchantments.add(Enchantments.IMPALING);
        this.allEnchantments.add(Enchantments.RIPTIDE);
        this.allEnchantments.add(Enchantments.CHANNELING);
        this.allEnchantments.add(Enchantments.MULTISHOT);
        this.allEnchantments.add(Enchantments.PIERCING);
        this.allEnchantments.add(Enchantments.QUICK_CHARGE);
        this.allEnchantments.add(Enchantments.MENDING);
        this.allEnchantments.add(Enchantments.VANISHING_CURSE);
        this.allEnchantments.add(Enchantments.BINDING_CURSE);
    }

    private String getEnchantmentDisplayName(RegistryKey<Enchantment> enchantment) {
        // Special case for Amethyst Drill
        if (enchantment == null) {
            return "AMETHYST DRILL";
        }
        
        String name = enchantment.getValue().toString();
        if (name.contains("sharpness")) return "SHARPNESS";
        if (name.contains("smite")) return "SMITE";
        if (name.contains("bane_of_arthropods")) return "BANE OF ARTHROPODS";
        if (name.contains("knockback")) return "KNOCKBACK";
        if (name.contains("fire_aspect")) return "FIRE ASPECT";
        if (name.contains("looting")) return "LOOTING";
        if (name.contains("sweeping_edge")) return "SWEEPING EDGE";
        if (name.contains("efficiency")) return "EFFICIENCY";
        if (name.contains("silk_touch")) return "SILK TOUCH";
        if (name.contains("fortune")) return "FORTUNE";
        if (name.contains("unbreaking")) return "UNBREAKING";
        if (name.contains("protection")) return "PROTECTION";
        if (name.contains("fire_protection")) return "FIRE PROTECTION";
        if (name.contains("blast_protection")) return "BLAST PROTECTION";
        if (name.contains("projectile_protection")) return "PROJECTILE PROTECTION";
        if (name.contains("thorns")) return "THORNS";
        if (name.contains("respiration")) return "RESPIRATION";
        if (name.contains("aqua_affinity")) return "AQUA AFFINITY";
        if (name.contains("depth_strider")) return "DEPTH STRIDER";
        if (name.contains("frost_walker")) return "FROST WALKER";
        if (name.contains("feather_falling")) return "FEATHER FALLING";
        if (name.contains("power")) return "POWER";
        if (name.contains("punch")) return "PUNCH";
        if (name.contains("flame")) return "FLAME";
        if (name.contains("infinity")) return "INFINITY";
        if (name.contains("luck_of_the_sea")) return "LUCK OF THE SEA";
        if (name.contains("lure")) return "LURE";
        if (name.contains("loyalty")) return "LOYALTY";
        if (name.contains("impaling")) return "IMPALING";
        if (name.contains("riptide")) return "RIPTIDE";
        if (name.contains("channeling")) return "CHANNELING";
        if (name.contains("multishot")) return "MULTISHOT";
        if (name.contains("piercing")) return "PIERCING";
        if (name.contains("quick_charge")) return "QUICK CHARGE";
        if (name.contains("mending")) return "MENDING";
        if (name.contains("vanishing_curse")) return "VANISHING CURSE";
        if (name.contains("binding_curse")) return "BINDING CURSE";
        
        return name.toUpperCase().replace("_", " ");
    }

    private String getEnchantmentLevelText(RegistryKey<Enchantment> enchantment) {
        // Special case for Amethyst Pickaxe
        if (enchantment == null) {
            return "UNB III + EFF V + MEND";
        }
        
        // Return level range for enchantments
        if (enchantment == Enchantments.SHARPNESS || enchantment == Enchantments.SMITE || 
            enchantment == Enchantments.BANE_OF_ARTHROPODS || enchantment == Enchantments.EFFICIENCY ||
            enchantment == Enchantments.FORTUNE || enchantment == Enchantments.PROTECTION ||
            enchantment == Enchantments.FIRE_PROTECTION || enchantment == Enchantments.BLAST_PROTECTION ||
            enchantment == Enchantments.PROJECTILE_PROTECTION || enchantment == Enchantments.POWER ||
            enchantment == Enchantments.PUNCH || enchantment == Enchantments.LUCK_OF_THE_SEA ||
            enchantment == Enchantments.LURE || enchantment == Enchantments.LOYALTY ||
            enchantment == Enchantments.IMPALING || enchantment == Enchantments.MULTISHOT ||
            enchantment == Enchantments.PIERCING || enchantment == Enchantments.QUICK_CHARGE) {
            return "I-V";
        } else if (enchantment == Enchantments.KNOCKBACK || enchantment == Enchantments.FIRE_ASPECT ||
                   enchantment == Enchantments.LOOTING || enchantment == Enchantments.SWEEPING_EDGE ||
                   enchantment == Enchantments.SILK_TOUCH || enchantment == Enchantments.UNBREAKING ||
                   enchantment == Enchantments.THORNS || enchantment == Enchantments.RESPIRATION ||
                   enchantment == Enchantments.AQUA_AFFINITY || enchantment == Enchantments.DEPTH_STRIDER ||
                   enchantment == Enchantments.FROST_WALKER || enchantment == Enchantments.FEATHER_FALLING ||
                   enchantment == Enchantments.FLAME || enchantment == Enchantments.INFINITY ||
                   enchantment == Enchantments.RIPTIDE || enchantment == Enchantments.CHANNELING ||
                   enchantment == Enchantments.MENDING || enchantment == Enchantments.VANISHING_CURSE ||
                   enchantment == Enchantments.BINDING_CURSE) {
            return "I-II";
        }
        return "";
    }

    private String[] splitEnchantmentName(String name) {
        if (name.length() <= 8) {
            return new String[]{name};
        }
        
        // Split by spaces first
        String[] words = name.split(" ");
        if (words.length > 1) {
            // Try to split by words
            StringBuilder line1 = new StringBuilder();
            StringBuilder line2 = new StringBuilder();
            
            for (int i = 0; i < words.length; i++) {
                if (line1.length() + words[i].length() <= 8) {
                    if (line1.length() > 0) line1.append(" ");
                    line1.append(words[i]);
                } else {
                    if (line2.length() > 0) line2.append(" ");
                    line2.append(words[i]);
                }
            }
            
            if (line2.length() == 0) {
                // If we can't split by words, split by characters
                return new String[]{
                    name.substring(0, Math.min(8, name.length())),
                    name.length() > 8 ? name.substring(8, Math.min(16, name.length())) : ""
                };
            } else {
                return new String[]{line1.toString(), line2.toString()};
            }
        } else {
            // Single word, split by characters
            return new String[]{
                name.substring(0, Math.min(8, name.length())),
                name.length() > 8 ? name.substring(8, Math.min(16, name.length())) : ""
            };
        }
    }
} 