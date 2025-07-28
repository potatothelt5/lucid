package skid.krypton.event.events;

import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import skid.krypton.event.CancellableEvent;

public class ChunkDataEvent extends CancellableEvent {
    public ChunkDataS2CPacket packet;

    public ChunkDataEvent(final ChunkDataS2CPacket packet) {
        this.packet = packet;
    }
}