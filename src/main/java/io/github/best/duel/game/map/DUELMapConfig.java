package io.github.best.duel.game.map;


import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;

public class DUELMapConfig {
    public static final Codec<DUELMapConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockState.CODEC.fieldOf("spawn_block").forGetter(map -> map.spawnBlock)
    ).apply(instance, DUELMapConfig::new));

    public final BlockState spawnBlock;

    public DUELMapConfig(BlockState spawnBlock) {
        this.spawnBlock = spawnBlock;
    }
}
