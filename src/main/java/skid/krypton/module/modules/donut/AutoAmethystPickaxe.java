package skid.krypton.module.modules.donut;

import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import skid.krypton.event.EventListener;
import skid.krypton.event.events.TickEvent;
import skid.krypton.module.Category;
import skid.krypton.module.Module;
import skid.krypton.module.setting.BooleanSetting;
import skid.krypton.module.setting.NumberSetting;
import skid.krypton.module.setting.StringSetting;
import skid.krypton.utils.ChatUtils;
import skid.krypton.utils.EnchantmentUtil;
import skid.krypton.utils.EncryptedString;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoAmethystPickaxe extends Module {
    
    private final BooleanSetting autoBuy = new BooleanSetting(EncryptedString.of("Auto Buy"), true);
    private final BooleanSetting notify = new BooleanSetting(EncryptedString.of("Notify"), true);
    private final NumberSetting refreshDelay = new NumberSetting(EncryptedString.of("Refresh Delay"), 1.0, 10.0, 3.0, 0.5);
    private final StringSetting maxPrice = new StringSetting(EncryptedString.of("Max Price"), "50k");
    private final NumberSetting buyDelay = new NumberSetting(EncryptedString.of("Buy Delay"), 0.0, 10.0, 1.0, 0.5);
    
    private int tickCounter = 0;
    private int delayCounter = 0;
    private boolean hasOpenedAH = false;
    private boolean isProcessing = false;
    
    public AutoAmethystPickaxe() {
        super(EncryptedString.of("Auto Amethyst Drill"), EncryptedString.of("Automatically searches and buys Amethyst Drills with Unbreaking III, Efficiency V, and Mending"), -1, Category.DONUT);
        this.addSettings(autoBuy, notify, refreshDelay, maxPrice, buyDelay);
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        hasOpenedAH = false;
        tickCounter = 0;
        delayCounter = 0;
        isProcessing = false;
        if (notify.getValue()) {
            ChatUtils.info("Auto Amethyst Drill enabled! Will open auction house and search for drills.");
        }
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        hasOpenedAH = false;
        isProcessing = false;
        if (notify.getValue()) {
            ChatUtils.info("Auto Amethyst Drill disabled!");
        }
    }
    
    @EventListener
    public void onTick(TickEvent event) {
        if (!isEnabled()) return;
        
        ClientPlayerEntity player = mc.player;
        if (player == null) return;
        
        if (delayCounter > 0) {
            delayCounter--;
            return;
        }
        
        tickCounter++;
        
        // Open auction house if not already opened
        if (!hasOpenedAH) {
            openAuctionHouse();
            hasOpenedAH = true;
            delayCounter = 20;
            return;
        }
        
        // Check if we're in auction house GUI
        if (mc.currentScreen instanceof GenericContainerScreen) {
            GenericContainerScreen screen = (GenericContainerScreen) mc.currentScreen;
            GenericContainerScreenHandler handler = screen.getScreenHandler();
            
            // Process the auction house
            processAuctionHouse(handler);
        }
    }
    
    private void openAuctionHouse() {
        ClientPlayerEntity player = mc.player;
        if (player == null) return;
        
        // Open auction house with Amethyst Drill search
        String command = "/ah ᴀᴍᴇᴛʜʏѕᴛ ᴅʀɪʟʟ";
        player.networkHandler.sendCommand(command.substring(1));
        
        if (notify.getValue()) {
            ChatUtils.info("Opening auction house for Amethyst Drill search...");
        }
    }
    
    private void processAuctionHouse(GenericContainerScreenHandler handler) {
        // Check for 6-row auction house layout
        if (handler.getRows() == 6) {
            processSixRowAuction(handler);
        } else if (handler.getRows() == 3) {
            processThreeRowAuction(handler);
        }
    }
    
    private void processSixRowAuction(GenericContainerScreenHandler handler) {
        // Check the "Recently Listed" slot (usually slot 47)
        ItemStack recentItem = handler.getSlot(47).getStack();
        if (!recentItem.isOf(Items.AIR)) {
            // Check if it's a recently listed item
            List<Text> tooltip = recentItem.getTooltip(Item.TooltipContext.DEFAULT, mc.player, TooltipType.BASIC);
            for (Text text : tooltip) {
                String tooltipText = text.getString();
                if (tooltipText.contains("Recently Listed") && 
                    (text.getStyle().toString().contains("white") || tooltipText.contains("white"))) {
                    // Click to buy the recently listed item
                    mc.interactionManager.clickSlot(handler.syncId, 47, 1, SlotActionType.QUICK_MOVE, mc.player);
                    delayCounter = 5;
                    if (notify.getValue()) {
                        ChatUtils.info("Bought recently listed Amethyst Drill!");
                    }
                    return;
                }
            }
        }
        
        // Check all slots for valid Amethyst Drills
        for (int i = 0; i < 44; i++) {
            ItemStack itemStack = handler.getSlot(i).getStack();
            if (isValidAmethystDrill(itemStack)) {
                if (isProcessing) {
                    // Buy the item
                    mc.interactionManager.clickSlot(handler.syncId, i, 1, SlotActionType.QUICK_MOVE, mc.player);
                    isProcessing = false;
                    if (notify.getValue()) {
                        ChatUtils.info("Bought Amethyst Drill with Unbreaking III, Efficiency V, and Mending!");
                    }
                    return;
                }
                isProcessing = true;
                delayCounter = (int)buyDelay.getValue();
                return;
            }
        }
        
        // If no valid items found, refresh
        if (tickCounter % (int)(refreshDelay.getValue() * 20) == 0) {
            // Click refresh button (usually slot 49)
            mc.interactionManager.clickSlot(handler.syncId, 49, 1, SlotActionType.QUICK_MOVE, mc.player);
            delayCounter = (int)refreshDelay.getValue();
            if (notify.getValue()) {
                ChatUtils.info("Refreshing auction house search...");
            }
        }
    }
    
    private void processThreeRowAuction(GenericContainerScreenHandler handler) {
        // For 3-row layout, check the main item slot (usually slot 13)
        ItemStack itemStack = handler.getSlot(13).getStack();
        if (isValidAmethystDrill(itemStack)) {
            // Click buy button (usually slot 15)
            mc.interactionManager.clickSlot(handler.syncId, 15, 1, SlotActionType.QUICK_MOVE, mc.player);
            delayCounter = 20;
            if (notify.getValue()) {
                ChatUtils.info("Bought Amethyst Drill from 3-row auction!");
            }
        }
    }
    
    private boolean isValidAmethystDrill(ItemStack itemStack) {
        if (itemStack.isOf(Items.AIR)) {
            return false;
        }
        
        // Check if it's a pickaxe (Amethyst Drills are pickaxes)
        if (!itemStack.getItem().toString().contains("pickaxe")) {
            return false;
        }
        
        // Check price
        double itemPrice = parseTooltipPrice(itemStack.getTooltip(Item.TooltipContext.DEFAULT, mc.player, TooltipType.BASIC));
        double maxPriceValue = parsePrice(this.maxPrice.getValue());
        
        if (itemPrice == -1.0 || maxPriceValue == -1.0) {
            return false;
        }
        
        if (itemPrice > maxPriceValue) {
            return false;
        }
        
        // Check enchantments
        return hasRequiredEnchantments(itemStack);
    }
    
    private boolean hasRequiredEnchantments(ItemStack itemStack) {
        try {
            // Get item enchantments
            Object2IntMap<RegistryKey<Enchantment>> enchantmentMap = new Object2IntArrayMap<>();
            EnchantmentUtil.populateEnchantmentMap(itemStack, enchantmentMap);
            
            // Check for required enchantments: Unbreaking III, Efficiency V, Mending
            boolean hasUnbreaking = false;
            boolean hasEfficiency = false;
            boolean hasMending = false;
            
            for (Object2IntMap.Entry<RegistryKey<Enchantment>> entry : enchantmentMap.object2IntEntrySet()) {
                RegistryKey<Enchantment> enchantment = entry.getKey();
                int level = entry.getIntValue();
                
                if (enchantment.getValue().toString().contains("unbreaking") && level >= 3) {
                    hasUnbreaking = true;
                } else if (enchantment.getValue().toString().contains("efficiency") && level >= 5) {
                    hasEfficiency = true;
                } else if (enchantment.getValue().toString().contains("mending") && level >= 1) {
                    hasMending = true;
                }
            }
            
            return hasUnbreaking && hasEfficiency && hasMending;
        } catch (Exception e) {
            return false;
        }
    }
    
    private double parseTooltipPrice(List<Text> tooltip) {
        if (tooltip == null || tooltip.isEmpty()) {
            return -1.0;
        }
        
        for (Text text : tooltip) {
            String tooltipText = text.getString();
            if (tooltipText.matches("(?i).*price\\s*:\\s*\\$.*")) {
                String priceText = tooltipText.replaceAll("[,$]", "");
                Matcher matcher = Pattern.compile("([\\d]+(?:\\.[\\d]+)?)\\s*([KMB])?", Pattern.CASE_INSENSITIVE).matcher(priceText);
                if (matcher.find()) {
                    String number = matcher.group(1);
                    String suffix = matcher.group(2) != null ? matcher.group(2).toUpperCase() : "";
                    return parsePrice(number + suffix);
                }
            }
        }
        return -1.0;
    }
    
    private double parsePrice(String price) {
        if (price == null || price.isEmpty()) {
            return -1.0;
        }
        
        String cleanPrice = price.trim().toUpperCase();
        double multiplier = 1.0;
        
        if (cleanPrice.endsWith("B")) {
            multiplier = 1.0E9;
            cleanPrice = cleanPrice.substring(0, cleanPrice.length() - 1);
        } else if (cleanPrice.endsWith("M")) {
            multiplier = 1000000.0;
            cleanPrice = cleanPrice.substring(0, cleanPrice.length() - 1);
        } else if (cleanPrice.endsWith("K")) {
            multiplier = 1000.0;
            cleanPrice = cleanPrice.substring(0, cleanPrice.length() - 1);
        }
        
        try {
            return Double.parseDouble(cleanPrice) * multiplier;
        } catch (NumberFormatException e) {
            return -1.0;
        }
    }
    
    public void buyAmethystDrill() {
        if (!autoBuy.getValue()) return;
        
        ClientPlayerEntity player = mc.player;
        if (player == null) return;
        
        // Create the command to buy an Amethyst Drill with the specified enchantments
        String command = "/ah ᴀᴍᴇᴛʜʏѕᴛ ᴘɪᴄᴋᴀxᴇ{Enchantments:[{id:\"minecraft:unbreaking\",lvl:3s},{id:\"minecraft:efficiency\",lvl:5s},{id:\"minecraft:mending\",lvl:1s}]}";
        
        // Send the command
        player.networkHandler.sendCommand(command.substring(1));
        
        if (notify.getValue()) {
            ChatUtils.info("Attempting to buy Amethyst Drill with Unbreaking III, Efficiency V, and Mending...");
        }
    }
    
    public void checkAndBuyDrill() {
        if (!isEnabled() || !autoBuy.getValue()) return;
        
        ClientPlayerEntity player = mc.player;
        if (player == null) return;
        
        // Check if player has a drill in their inventory
        boolean hasDrill = false;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == Items.DIAMOND_PICKAXE || stack.getItem() == Items.NETHERITE_PICKAXE) {
                hasDrill = true;
                break;
            }
        }
        
        // If no drill found, buy one
        if (!hasDrill) {
            buyAmethystDrill();
        }
    }
} 