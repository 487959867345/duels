package io.github.best.duel;

import io.github.best.duel.game.DUELConfig;
import io.github.best.duel.game.DUELWaiting;
import net.fabricmc.api.ModInitializer;
import xyz.nucleoid.plasmid.game.GameType;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.github.best.duel.game.DUELConfig;
import io.github.best.duel.game.DUELWaiting;

public class DUEL implements ModInitializer {

    public static final String ID = "duel";
    public static final Logger LOGGER = LogManager.getLogger(ID);

    public static final GameType<DUELConfig> TYPE = GameType.register(
            new Identifier(ID, "duel"),
            DUELWaiting::open,
            DUELConfig.CODEC
    );

    @Override
    public void onInitialize() {
    }
}
