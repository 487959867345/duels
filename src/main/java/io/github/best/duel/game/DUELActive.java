package io.github.best.duel.game;

import io.github.best.duel.game.map.DUELMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import org.lwjgl.system.CallbackI;
import xyz.nucleoid.plasmid.game.GameWorld;
import xyz.nucleoid.plasmid.game.event.*;
import xyz.nucleoid.plasmid.game.player.JoinResult;
import xyz.nucleoid.plasmid.game.rule.GameRule;
import xyz.nucleoid.plasmid.game.rule.RuleResult;
import xyz.nucleoid.plasmid.util.PlayerRef;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;
import io.github.best.duel.DUEL;
import io.github.best.duel.game.map.DUELMap;

import java.util.*;
import java.util.stream.Collectors;

public class DUELActive {
    private final DUELConfig config;

    public final GameWorld gameWorld;
    private final DUELMap gameMap;

    // TODO replace with ServerPlayerEntity if players are removed upon leaving
    private final Object2ObjectMap<PlayerRef, DUELPlayer> participants;
    private final DUELSpawnLogic spawnLogic;
    private final DUELIdle idle;
    private final boolean ignoreWinState;
    private final DUELTimerBar timerBar;

    private DUELActive(GameWorld gameWorld, DUELMap map, DUELConfig config, Set<PlayerRef> participants) {
        this.gameWorld = gameWorld;
        this.config = config;
        this.gameMap = map;
        this.spawnLogic = new DUELSpawnLogic(gameWorld, map);
        this.participants = new Object2ObjectOpenHashMap<>();

        for (PlayerRef player : participants) {
            this.participants.put(player, new DUELPlayer());
        }

        this.idle = new DUELIdle();
        this.ignoreWinState = this.participants.size() <= 1;
        this.timerBar = new DUELTimerBar();
    }

    public static void open(GameWorld gameWorld, DUELMap map, DUELConfig config) {
        Set<PlayerRef> participants = gameWorld.getPlayers().stream()
                .map(PlayerRef::of)
                .collect(Collectors.toSet());
        DUELActive active = new DUELActive(gameWorld, map, config, participants);

        gameWorld.openGame(builder -> {
            builder.setRule(GameRule.CRAFTING, RuleResult.DENY);
            builder.setRule(GameRule.PORTALS, RuleResult.DENY);
            builder.setRule(GameRule.PVP, RuleResult.ALLOW);
            builder.setRule(GameRule.HUNGER, RuleResult.DENY);
            builder.setRule(GameRule.FALL_DAMAGE, RuleResult.DENY);
            builder.setRule(GameRule.INTERACTION, RuleResult.DENY);
            builder.setRule(GameRule.BLOCK_DROPS, RuleResult.DENY);
            builder.setRule(GameRule.THROW_ITEMS, RuleResult.DENY);
            builder.setRule(GameRule.UNSTABLE_TNT, RuleResult.DENY);

            builder.on(GameOpenListener.EVENT, active::onOpen);
            builder.on(GameCloseListener.EVENT, active::onClose);

            builder.on(OfferPlayerListener.EVENT, player -> JoinResult.ok());
            builder.on(PlayerAddListener.EVENT, active::addPlayer);
            builder.on(PlayerRemoveListener.EVENT, active::removePlayer);

            builder.on(GameTickListener.EVENT, active::tick);
            builder.on(PlayerDamageListener.EVENT, active::onPlayerDamage);
            builder.on(PlayerDeathListener.EVENT, active::onPlayerDeath);
        });
    }

    private void onOpen() {
        ServerWorld world = this.gameWorld.getWorld();
        for (PlayerRef ref : this.participants.keySet()) {
            ref.ifOnline(world, this::spawnParticipant);
        }
        this.idle.onOpen(world.getTime(), this.config);
        // TODO setup logic
    }

    private void onClose() {
        this.timerBar.close();
        // TODO teardown logic
    }

    private void addPlayer(ServerPlayerEntity player) {
        if (!this.participants.containsKey(PlayerRef.of(player))) {
            this.spawnSpectator(player);
        }
        this.timerBar.addPlayer(player);
    }

    private void removePlayer(ServerPlayerEntity player) {
        this.participants.remove(PlayerRef.of(player));
        this.timerBar.removePlayer(player);
    }

    private boolean onPlayerDamage(ServerPlayerEntity player, DamageSource source, float amount) {
        // TODO handle damage
        if (player.getHealth() < 5 ){
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION));
        }
        return false;
    }

    private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
        // TODO handle death

        for(ServerPlayerEntity ply: player.getServerWorld().getPlayers()) {
            if (ply != player){
                broadcastWin(new WinResult(ply, true));
                this.gameWorld.close();
            }
        }
        this.spawnParticipant(player);
        return ActionResult.FAIL;
    }
    private void gearPlayer(ServerPlayerEntity player){
        // giv sheild
        player.inventory.offHand.set(0,new ItemStack(Items.SHIELD));
        // main loot
        player.inventory.main.set(0, new ItemStack(Kits.Basic.MAIN));
        player.inventory.main.set(1, new ItemStack(Items.GOLDEN_APPLE)).increment(20);
        // armor
        player.inventory.armor.set(3, new ItemStack(Kits.Basic.HEAD));
        player.inventory.armor.set(2, new ItemStack(Kits.Basic.CHEST));
        player.inventory.armor.set(1, new ItemStack(Kits.Basic.LEGS));
        player.inventory.armor.set(0, new ItemStack(Kits.Basic.BOOTS));
    }

    private void spawnParticipant(ServerPlayerEntity player) {
        this.spawnLogic.resetPlayer(player, GameMode.ADVENTURE);
        gearPlayer(player);
        this.spawnLogic.spawnPlayer(player);
    }

    private void spawnSpectator(ServerPlayerEntity player) {
        this.spawnLogic.resetPlayer(player, GameMode.SPECTATOR);
        this.spawnLogic.spawnPlayer(player);
    }

    private void tick() {
        ServerWorld world = this.gameWorld.getWorld();
        long time = world.getTime();
        DUELIdle.IdleTickResult result = this.idle.tick(time, gameWorld);

        switch (result) {
            case CONTINUE_TICK:
                break;
            case TICK_FINISHED:
                return;
            case GAME_FINISHED:
                this.broadcastWin(this.checkWinResult());
                return;
            case GAME_CLOSED:
                this.gameWorld.close();
                return;
        }

        this.timerBar.update(this.idle.finishTime - time, this.config.timeLimitSecs * 20);

        // TODO tick logic
    }

    protected static void broadcastMessage(Text message, GameWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            player.sendMessage(message, false);
        }
    }

    protected static void broadcastSound(SoundEvent sound, float pitch, GameWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            player.playSound(sound, SoundCategory.PLAYERS, 1.0F, pitch);
        }
    }

    protected static void broadcastSound(SoundEvent sound,  GameWorld world) {
        broadcastSound(sound, 1.0f, world);
    }

    protected static void broadcastTitle(Text message, GameWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            TitleS2CPacket packet = new TitleS2CPacket(TitleS2CPacket.Action.TITLE, message, 1, 5,  3);
            player.networkHandler.sendPacket(packet);
        }
    }

    private void broadcastWin(WinResult result) {
        ServerPlayerEntity winningPlayer = result.getWinningPlayer();

        Text message;
        if (winningPlayer != null) {
            message = winningPlayer.getDisplayName().shallowCopy().append("Has won the Duel!").formatted(Formatting.GOLD);
        } else {
            message = new LiteralText(" The game ended, but nobody won!").formatted(Formatting.GOLD);
        }

        broadcastMessage(message, this.gameWorld);
        broadcastSound(SoundEvents.ENTITY_VILLAGER_YES, this.gameWorld);
    }

    private WinResult checkWinResult() {
        // for testing purposes: don't end the game if we only ever had one participant
        if (this.ignoreWinState) {
            return WinResult.no();
        }

        ServerWorld world = this.gameWorld.getWorld();
        ServerPlayerEntity winningPlayer = null;

        // TODO win result logic
        return WinResult.no();
    }

    static class WinResult {
        final ServerPlayerEntity winningPlayer;
        final boolean win;

        private WinResult(ServerPlayerEntity winningPlayer, boolean win) {
            this.winningPlayer = winningPlayer;
            this.win = win;
        }

        static WinResult no() {
            return new WinResult(null, false);
        }

        static WinResult win(ServerPlayerEntity player) {
            return new WinResult(player, true);
        }

        public boolean isWin() {
            return this.win;
        }

        public ServerPlayerEntity getWinningPlayer() {
            return this.winningPlayer;
        }
    }
}
