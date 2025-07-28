package skid.krypton.module.modules.donut;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import skid.krypton.event.EventListener;
import skid.krypton.event.events.EntitySpawnEvent;
import skid.krypton.module.Category;
import skid.krypton.module.Module;
import skid.krypton.utils.EncryptedString;

import java.util.ArrayList;

public final class AntiTrap extends Module {
    public AntiTrap() {
        super(EncryptedString.of("Anti Trap"), EncryptedString.of("Module that helps you escape Polish traps"), -1, Category.DONUT);
        this.addSettings();
    }

    @Override
    public void onEnable() {
        this.removeTrapEntities();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @EventListener
    public void onEntitySpawn(final EntitySpawnEvent entitySpawnEvent) {
        if (this.isTrapEntity(entitySpawnEvent.packet.getEntityType())) {
            entitySpawnEvent.cancel();
        }
    }

    private void removeTrapEntities() {
        if (this.mc.world == null) {
            return;
        }
        final ArrayList<Entity> trapEntities = new ArrayList<>();
        this.mc.world.getEntities().forEach(entity -> {
            if (entity != null && this.isTrapEntity(entity.getType())) {
                trapEntities.add(entity);
            }
        });
        trapEntities.forEach(trapEntity -> {
            if (!trapEntity.isRemoved()) {
                trapEntity.remove(Entity.RemovalReason.DISCARDED);
            }
        });
    }

    private boolean isTrapEntity(final EntityType<?> entityType) {
        return entityType != null && (entityType.equals(EntityType.ARMOR_STAND) || entityType.equals(EntityType.CHEST_MINECART));
    }
}