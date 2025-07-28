package skid.krypton.event.events;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import skid.krypton.event.CancellableEvent;

public class AttackBlockEvent extends CancellableEvent {
    public BlockPos pos;
    public Direction direction;

    public AttackBlockEvent(final BlockPos pos, final Direction direction) {
        this.pos = pos;
        this.direction = direction;
    }
}