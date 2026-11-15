package com.vgst54.projectgt.command;

import java.util.Comparator;
import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.vgst54.projectgt.ProjectGT;
import com.vgst54.projectgt.worldgen.OreNodes;
import com.vgst54.projectgt.worldgen.TerrainProbe;
import com.vgst54.projectgt.worldgen.Vein;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Отладка генерации руд (нужны права оператора):
 *   /projectgt veins - ближайшие жилы с координатами центра и составом
 */
@EventBusSubscriber(modid = ProjectGT.MODID)
public final class OreDebugCommand {
    private static final int SEARCH_CELLS = 5;  // +-5 ячеек сетки, около 560 блоков
    private static final int LIMIT = 8;

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("projectgt")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("veins").executes(OreDebugCommand::veins)));
    }

    private static int veins(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        Vec3 p = src.getPosition();
        boolean nether = src.getLevel().dimension() == Level.NETHER;
        List<Vein> all = OreNodes.veinsNear(src.getLevel().getSeed(), p.x, p.z, SEARCH_CELLS, nether, TerrainProbe.of(src.getLevel())).stream()
                .sorted(Comparator.comparingDouble(v -> dist3(v, p)))
                .limit(LIMIT)
                .toList();
        if (all.isEmpty()) {
            src.sendSuccess(() -> Component.literal("Жил рядом нет"), false);
            return 0;
        }
        src.sendSuccess(() -> Component.literal("Ближайшие жилы:"), false);
        for (Vein v : all) {
            String line = String.format("  X %d, Y %d, Z %d | до центра %d | %s",
                    Math.round(v.cx()), Math.round(v.cy()), Math.round(v.cz()),
                    Math.round(dist3(v, p)), v.mix().describe());
            src.sendSuccess(() -> Component.literal(line), false);
        }
        return all.size();
    }

    private static double dist3(Vein v, Vec3 p) {
        double dx = v.cx() - p.x, dy = v.cy() - p.y, dz = v.cz() - p.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private OreDebugCommand() {}
}
