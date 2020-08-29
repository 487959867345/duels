package io.github.best.duel.game;


import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.best.duel.game.map.DUELMapConfig;
import xyz.nucleoid.plasmid.game.config.PlayerConfig;
import io.github.best.duel.game.map.DUELMapConfig;

public class DUELConfig {
    public static final Codec<DUELConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PlayerConfig.CODEC.fieldOf("players").forGetter(config -> config.playerConfig),
            DUELMapConfig.CODEC.fieldOf("map").forGetter(config -> config.mapConfig),
            Codec.INT.fieldOf("time_limit_secs").forGetter(config -> config.timeLimitSecs)
    ).apply(instance, DUELConfig::new));

    public final PlayerConfig playerConfig;
    public final DUELMapConfig mapConfig;
    public final int timeLimitSecs;

    public DUELConfig(PlayerConfig players, DUELMapConfig mapConfig, int timeLimitSecs) {
        this.playerConfig = players;
        this.mapConfig = mapConfig;
        this.timeLimitSecs = timeLimitSecs;
    }
}
