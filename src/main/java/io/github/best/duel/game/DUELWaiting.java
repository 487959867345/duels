package io.github.best.duel.game;


import io.github.best.duel.game.map.DUELMap;
import io.github.best.duel.game.map.DUELMapGenerator;
import net.minecraft.util.ActionResult;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameWaitingLobby;
import xyz.nucleoid.plasmid.game.GameWorld;
import xyz.nucleoid.plasmid.game.StartResult;
import xyz.nucleoid.plasmid.game.event.PlayerAddListener;
import xyz.nucleoid.plasmid.game.event.PlayerDeathListener;
import xyz.nucleoid.plasmid.game.event.RequestStartListener;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;
import io.github.best.duel.game.map.DUELMap;
import io.github.best.duel.game.map.DUELMapGenerator;
import xyz.nucleoid.plasmid.world.bubble.BubbleWorldConfig;

import java.util.concurrent.CompletableFuture;

public class DUELWaiting {
    private final GameWorld gameWorld;
    private final DUELMap map;
    private final DUELConfig config;
    private final DUELSpawnLogic spawnLogic;

    private DUELWaiting(GameWorld gameWorld, DUELMap map, DUELConfig config) {
        this.gameWorld = gameWorld;
        this.map = map;
        this.config = config;
        this.spawnLogic = new DUELSpawnLogic(gameWorld, map);
    }

    public static CompletableFuture<GameWorld> open(GameOpenContext<DUELConfig> context) {
        DUELMapGenerator generator = new DUELMapGenerator(context.getConfig().mapConfig);

        return generator.create().thenCompose(map -> {
            BubbleWorldConfig worldConfig = new BubbleWorldConfig()
                    .setGenerator(map.asGenerator(context.getServer()))
                    .setDefaultGameMode(GameMode.SPECTATOR);

            return context.openWorld(worldConfig).thenApply(gameWorld -> {
                DUELWaiting waiting = new DUELWaiting(gameWorld, map, context.getConfig());

                GameWaitingLobby.open(gameWorld, context.getConfig().playerConfig, builder -> {
                    builder.on(RequestStartListener.EVENT, waiting::requestStart);
                    builder.on(PlayerAddListener.EVENT, waiting::addPlayer);
                    builder.on(PlayerDeathListener.EVENT, waiting::onPlayerDeath);
                });

                return gameWorld;
            });
        });
    }

    private StartResult requestStart() {
        DUELActive.open(this.gameWorld, this.map, this.config);
        return StartResult.OK;
    }

    private void addPlayer(ServerPlayerEntity player) {
        this.spawnPlayer(player);
    }

    private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
        player.setHealth(20.0f);
        this.spawnPlayer(player);
        return ActionResult.FAIL;
    }

    private void spawnPlayer(ServerPlayerEntity player) {
        this.spawnLogic.resetPlayer(player, GameMode.ADVENTURE);
        this.spawnLogic.spawnPlayer(player);
    }
}
