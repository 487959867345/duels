package io.github.best.duel.game.map;

import xyz.nucleoid.plasmid.game.map.template.MapTemplate;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import io.github.best.duel.game.DUELConfig;

import java.util.concurrent.CompletableFuture;

public class DUELMapGenerator {

    private final DUELMapConfig config;

    public DUELMapGenerator(DUELMapConfig config) {
        this.config = config;
    }

    public CompletableFuture<DUELMap> create() {
        return CompletableFuture.supplyAsync(this::build, Util.getMainWorkerExecutor());
    }

    private DUELMap build() {
        MapTemplate template = MapTemplate.createEmpty();
        DUELMap map = new DUELMap(template, this.config);

        this.buildSpawn(template);
        map.spawn = new BlockPos(0,65,0);

        return map;
    }

    private void buildSpawn(MapTemplate builder) {
        BlockPos min = new BlockPos(-10, 64, -10);
        BlockPos max = new BlockPos(10, 64, 10);

        for (BlockPos pos : BlockPos.iterate(min, max)) {
            builder.setBlockState(pos, this.config.spawnBlock);
        }
    }
}
