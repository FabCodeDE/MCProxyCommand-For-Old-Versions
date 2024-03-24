package de.michiruf.proxycommand.fabric;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.michiruf.proxycommand.common.ProxyCommandConstants;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Michael Ruf
 * @since 2022-12-15
 */
public class ProxyCommandMod implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("ProxyCommand");

    @Override
    public void onInitialize() {
        LOGGER.info("ProxyCommand is active");
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            if (dedicated) { // Only register command on dedicated server
                registerCommand(dispatcher);
            }
        });
    }

    private static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("proxycommand")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("command", StringArgumentType.string())
                        .executes(ProxyCommandMod::sendMessage))
        );
    }

    private static int sendMessage(CommandContext<ServerCommandSource> context) {
        String command = StringArgumentType.getString(context, "command");

        try {
            var player = context.getSource().getPlayer();
            // To communicate with the proxy, a S2C packet sent via the player's connection is needed
            ServerPlayNetworking.send(
                    player,
                    new Identifier(ProxyCommandConstants.COMMAND_PACKET_ID),
                    PacketByteBufs.create().writeString(command)
            );
        } catch (Exception e) {
            LOGGER.warn("Command \"" + command + "\" was executed without a player as source");
            context.getSource().sendError(Text.of("Command source must be a player"));
            return 0; // Return 0 to indicate that the command execution was not successful
        }
        return 1; // Return 1 to indicate successful command execution
    }
}
