package io.github.andrewwwwwwwwwwwwwww.holograms;

import io.github.andrewwwwwwwwwwwwwww.holograms.command.HologramCommands;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Interaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Holograms implements ModInitializer {
    public static final String MOD_ID = "holograms";
    public static final Logger LOGGER = LoggerFactory.getLogger("Holograms");

    public static MinecraftServer server;
    public static final HologramManager MANAGER = new HologramManager();

    @Override
    public void onInitialize() {
        LOGGER.info("Holograms initializing");

        ServerLifecycleEvents.SERVER_STARTED.register(srv -> {
            server = srv;
            MANAGER.load(srv);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(srv -> {
            MANAGER.save(srv);
            server = null;
        });

        // Right-click a hologram's interaction entity → run its click actions.
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            if (!(entity instanceof Interaction)) return InteractionResult.PASS;
            if (!entity.entityTags().contains(HologramRenderer.GENERIC_TAG)) return InteractionResult.PASS;
            if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

            for (Hologram h : MANAGER.all()) {
                if (entity.entityTags().contains(HologramRenderer.nameTag(h.name))) {
                    runClickActions(sp, h);
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.PASS;
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                HologramCommands.register(dispatcher, registryAccess));
    }

    private static void runClickActions(ServerPlayer sp, Hologram h) {
        if (h.clickMessage != null) {
            sp.sendSystemMessage(Colors.parse(h.clickMessage));
        }
        if (h.clickSound != null) {
            Identifier id = Identifier.tryParse(h.clickSound);
            Holder<SoundEvent> holder = id == null
                    ? null
                    : BuiltInRegistries.SOUND_EVENT.get(id).map(r -> (Holder<SoundEvent>) r).orElse(null);
            if (holder != null) {
                // Send straight to the clicking player so it always plays for them.
                sp.connection.send(new ClientboundSoundPacket(holder, SoundSource.MASTER,
                        sp.getX(), sp.getY(), sp.getZ(), 1.0f, 1.0f, sp.level().getRandom().nextLong()));
            }
        }
        if (!h.clickCommands.isEmpty()) {
            CommandSourceStack source = sp.createCommandSourceStack()
                    .withPermission(PermissionSet.ALL_PERMISSIONS)
                    .withSuppressedOutput();
            for (String cmd : h.clickCommands) {
                String c = cmd.startsWith("/") ? cmd.substring(1) : cmd;
                server.getCommands().performPrefixedCommand(source, c);
            }
        }
    }
}
