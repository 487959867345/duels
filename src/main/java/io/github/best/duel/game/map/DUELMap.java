package io.github.best.duel.game.map;

import net.minecraft.server.MinecraftServer;
import xyz.nucleoid.plasmid.game.map.template.MapTemplate;
import xyz.nucleoid.plasmid.game.map.template.TemplateChunkGenerator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.chunk.ChunkGenerator;

public class DUELMap {
    private final MapTemplate template;
    private final DUELMapConfig config;
    public BlockPos spawn;

    public DUELMap(MapTemplate template, DUELMapConfig config) {
        this.template = template;
        this.config = config;
    }

    public ChunkGenerator asGenerator(MinecraftServer server) {
        return new TemplateChunkGenerator(server, this.template, BlockPos.ORIGIN);
    }
}
