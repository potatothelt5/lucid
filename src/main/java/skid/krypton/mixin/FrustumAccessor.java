package skid.krypton.mixin;

import net.minecraft.client.render.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({Frustum.class})
public interface FrustumAccessor {
    @Accessor("x")
    double getX();

    @Accessor("x")
    void setX(final double x);

    @Accessor("y")
    double getY();

    @Accessor("y")
    void setY(final double y);

    @Accessor("z")
    double getZ();

    @Accessor("z")
    void setZ(final double z);
}