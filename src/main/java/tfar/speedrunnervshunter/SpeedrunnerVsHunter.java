package tfar.speedrunnervshunter;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import tfar.speedrunnervshunter.commands.MainCommand;

import java.util.*;

public class SpeedrunnerVsHunter implements ModInitializer {
    public static final String MODID = "speedrunnervshunter";

    public static UUID speedrunnerID;
    public static List<TrophyLocation> TROPHY_LOCATIONS = new ArrayList<>();

    //static ForgeConfigSpec serverSpec;

    public SpeedrunnerVsHunter() {
        ModConfig.compasses_work_in_end = true;
        ModConfig.minimum_height = 64;
        //final Pair<tfar.speedrunnervshunter.SpeedrunnerVsHunter, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(this::spec);
        //serverSpec = specPair.getRight();
       // ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, serverSpec);

       /* IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        MinecraftForge.EVENT_BUS.addListener(this::onServerStart);
        MinecraftForge.EVENT_BUS.addListener(this::commands);
        MinecraftForge.EVENT_BUS.addListener(this::knockback);
        MinecraftForge.EVENT_BUS.addListener(this::fallDamage);*/
    }

    // private static ServerBossInfo bossInfo;

    private static final Random rand = new Random();

    public static void start(MinecraftServer server, int distance, ServerPlayer speedrunner) {

        // bossInfo = new ServerBossInfo(new StringTextComponent("Timer"), BossInfo.Color.WHITE, BossInfo.Overlay.PROGRESS);
        // bossInfo.setPercent(1);

        speedrunnerID = speedrunner.getGameProfile().getId();
        TROPHY_LOCATIONS.clear();
        for (ServerPlayer playerMP : server.getPlayerList().getPlayers()) {

            //    bossInfo.addPlayer(playerMP);

            ItemStack stack = new ItemStack(Items.COMPASS);
            if (!isSpeedrunner(playerMP)) {
                stack.setHoverName(new TextComponent("Hunter's Compass"));
            } else {
                stack.setHoverName(new TextComponent("Speedrunner's Compass"));
            }
            playerMP.addItem(stack);
        }

        BlockPos center = speedrunner.blockPosition();

        double rot = rand.nextInt(360);

        for (int i = 0; i < TROPHY_COUNT; i++) {
            double offset = i * 360d / TROPHY_COUNT;
            int x = (int) (center.getX() + distance * Math.cos((Math.PI / 180) * (rot + offset)));
            int z = (int) (center.getZ() + distance * Math.sin((Math.PI / 180) * (rot + offset)));
            TrophyLocation trophyLocation = new TrophyLocation(x, z);
            TROPHY_LOCATIONS.add(trophyLocation);
        }

        for (TrophyLocation location : TROPHY_LOCATIONS) {
            LevelChunk chunk = speedrunner.level.getChunk(location.getPos().getX() >> 4, location.getPos().getZ() >> 4);
            int y = Math.max(chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, location.getPos().getX() & 15, location.getPos().getZ() & 15) + 1,64);
            location.setY(y);
            speedrunner.level.setBlock(location.getPos(), Blocks.GOLD_BLOCK.defaultBlockState(),3);
        }
    }

    private static int TROPHY_COUNT = 3;

    public void serverTick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.level.isClientSide && speedrunnerID != null) {
                BlockPos nearest;
                if (isSpeedrunner(player)) {
                    nearest = findNearestTrophy(player);
                } else {
                    //   if (HUNTERS_BLIND.get() && e.player.world.getGameTime() % 20 == 0) {
                    //      e.player.addPotionEffect(new EffectInstance(Effects.BLINDNESS, 80, 0, false, false));
                    //  }
                    nearest = new BlockPos(player.getServer().getPlayerList().getPlayer(speedrunnerID).position());
                }
                player.connection.send(new ClientboundSetDefaultSpawnPositionPacket(nearest, player.getLevel().getSharedSpawnAngle()));
            }
        }
    }

    private static boolean isSpeedrunner(Player player) {
        return player.getGameProfile().getId().equals(speedrunnerID);
    }

    public static int TIME_LIMIT = 1200;

    /*
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (speedrunnerID != null && event.phase == TickEvent.Phase.START) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();


            final long scaledTime = server.getWorld(World.OVERWORLD).getGameTime() % TIME_LIMIT;
            //  bossInfo.setPercent(1 - (scaledTime / (float)TIME_LIMIT));
            if (scaledTime == TIME_LIMIT - 200) {
                final List<ServerPlayer> players = server.getPlayerList().getPlayers().stream().filter(playerMP -> !(playerMP instanceof FakePlayer)).collect(Collectors.toList());
                //if (players.size() > 1) {
                //          server.getPlayerList().func_232641_a_(new StringTextComponent("Swapping in 10 seconds!"),ChatType.CHAT,Util.DUMMY_UUID);
                //  }
            }

            if (scaledTime == 0 && false) {
                List<ServerPlayer> players = Lists.newArrayList(server.getPlayerList().getPlayers());
                players.removeIf(serverPlayerEntity -> serverPlayerEntity.getGameProfile().getId().equals(speedrunnerID));
                if (!players.isEmpty()) {
                    ServerPlayer newSpeedrunner = players.get(rand.nextInt(players.size()));
                    UUID oldSpeedrunnerID = speedrunnerID;
                    ServerPlayer oldspeedrunner = server.getPlayerList().getPlayerByUUID(oldSpeedrunnerID);
                    speedrunnerID = newSpeedrunner.getGameProfile().getId();

                    PlayerInventory oldinventory = oldspeedrunner.inventory;
                    for (ItemStack stack : Stream.of(oldinventory.mainInventory, oldinventory.armorInventory, oldinventory.offHandInventory).flatMap(Collection::stream).collect(Collectors.toList())) {
                        if (stack.getItem() == Items.COMPASS) {
                            stack.setDisplayName(new StringTextComponent("Hunter's Compass"));
                        }
                    }

                    PlayerInventory newinventory = newSpeedrunner.inventory;
                    for (ItemStack stack : Stream.of(newinventory.mainInventory, newinventory.armorInventory, newinventory.offHandInventory).flatMap(Collection::stream).collect(Collectors.toList())) {
                        if (stack.getItem() == Items.COMPASS) {
                            stack.setDisplayName(new StringTextComponent("Speedrunner's Compass"));
                        }
                    }
                    TranslationTextComponent translationTextComponent =
                            new TranslationTextComponent("commands.speedrunnervshunter.speedrunner.success", newSpeedrunner.getDisplayName());
                    server.getPlayerList().func_232641_a_(translationTextComponent, ChatType.CHAT, Util.DUMMY_UUID);
                }
            }
        }
    }*/

    /*public static void updateStatus(ServerPlayer playerEntity) {
        if (isSpeedrunner(playerEntity)) {
            playerEntity.connection.sendPacket(new STitlePacket(STitlePacket.Type.ACTIONBAR, new StringTextComponent("You're the Speedrunner"), 0, TIME_LIMIT, 0));
        } else {
            playerEntity.connection.sendPacket(new STitlePacket(STitlePacket.Type.ACTIONBAR, new StringTextComponent("You're a Hunter"), 0, TIME_LIMIT, 0));
        }
    }*/

    /*private void knockback(LivingKnockBackEvent event) {
        if (MOBS_KNOCKBACK_10000.get()) {
            LivingEntity target = event.getEntityLiving();
            // DamageSource source = target.getLastDamageSource();
            LivingEntity attacker = target.getRevengeTarget();
            if (attacker instanceof MobEntity) {
                event.setStrength(event.getOriginalStrength() + 5000);
            }
        }
    }*/

  /*  private void fallDamage(LivingHurtEvent e) {
        if (FALL_DAMAGE_HEALS_SPEEDRUNNER.get()) {
            LivingEntity living = e.getEntityLiving();
            if (e.getSource() == DamageSource.FALL && living instanceof PlayerEntity && isSpeedrunner((PlayerEntity) living)) {
                living.heal(e.getAmount());
                e.setCanceled(true);
            }
        }
    }*/

   // public static ForgeConfigSpec.IntValue TROPHY_COUNT;
  //  public static ForgeConfigSpec.BooleanValue HUNTERS_BLIND;
   // public static ForgeConfigSpec.BooleanValue MOBS_KNOCKBACK_10000;
   // public static ForgeConfigSpec.BooleanValue FALL_DAMAGE_HEALS_SPEEDRUNNER;


   /* private tfar.speedrunnervshunter.SpeedrunnerVsHunter spec(ForgeConfigSpec.Builder builder) {
        builder.push("general");
        TROPHY_COUNT = builder.defineInRange("trophy_count", 3, 1, 10000);
        HUNTERS_BLIND = builder.define("hunters_blind", false);
        MOBS_KNOCKBACK_10000 = builder.define("mobs_knockback_10000", false);
        FALL_DAMAGE_HEALS_SPEEDRUNNER = builder.define("fall_damage_heals_speedrunner",false);
        return null;
    }*/

    //todo
    /*public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (isSpeedrunner(event.getPlayer())) {
            stop(event.getPlayer().getServer(), true);
        }
    }*/

    public void blockBreak(Level event, Player player, BlockPos pos, BlockState event4, BlockEntity event5) {
        for (Iterator<TrophyLocation> iterator = TROPHY_LOCATIONS.iterator(); iterator.hasNext(); ) {
            TrophyLocation trophyLocation = iterator.next();
            if (trophyLocation.getPos().equals(pos)) {
                player.sendMessage(new TranslatableComponent("text.speedrunnervshunter.trophy_get"), Util.NIL_UUID);
                iterator.remove();
            }
        }

        if (TROPHY_LOCATIONS.isEmpty() && speedrunnerID != null) {
            stop(player.getServer(), false);
        }
    }

    private static BlockPos findNearestTrophy(ServerPlayer playerMP) {
        double dist = Double.MAX_VALUE;
        BlockPos near = null;
        BlockPos playerPos = playerMP.blockPosition();
        for (TrophyLocation pos : TROPHY_LOCATIONS) {
            double dist1 = playerPos.distSqr(pos.getPos());
            if (dist1 < dist) {
                dist = dist1;
                near = pos.getPos();
            }
        }
        return near;
    }

    private static ServerPlayer getOtherPlayer(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        List<ServerPlayer> otherPlayers = new ArrayList<>(server.getPlayerList().getPlayers());
        otherPlayers.removeIf(serverPlayerEntity -> serverPlayerEntity.getGameProfile().getId().equals(player.getGameProfile().getId()));
        return !otherPlayers.isEmpty() ? otherPlayers.get(0) : null;
    }

    public static void stop(MinecraftServer server, boolean abort) {
        //bossInfo.removeAllPlayers();
        if (!abort)
            server.getPlayerList().broadcastMessage(new TranslatableComponent("text.speedrunnervshunter.speedrunner_win"), ChatType.CHAT, Util.NIL_UUID);
        speedrunnerID = null;
    }

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((t,a) -> MainCommand.registerCommands(t));
        ServerTickEvents.END_SERVER_TICK.register(this::serverTick);
        PlayerBlockBreakEvents.AFTER.register(this::blockBreak);

    }
}
