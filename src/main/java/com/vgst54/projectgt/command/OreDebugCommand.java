package com.vgst54.projectgt.command;

import java.util.Comparator;
import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.vgst54.projectgt.ProjectGT;
import com.vgst54.projectgt.worldgen.OreNodes;
import com.vgst54.projectgt.worldgen.Vein;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Отладка генерации руд (нужны права оператора):
 *   /projectgt nodes - ближайшие рудные узлы (координаты, вид, радиус)
 *   /projectgt veins - ближайшие жилы (минерал и координаты центра)
 */
@EventBusSubscriber(modid = ProjectGT.MODID)
public final class OreDebugCommand {
    private static final int SEARCH_CELLS = 2;  // +-2 ячейки = до ~1200 блоков
    private static final int LIMIT = 6;

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("projectgt")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("nodes").executes(OreDebugCommand::nodes))
                .then(Commands.literal("veins").executes(OreDebugCommand::veins)));
    }

    private static int nodes(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        Vec3 p = src.getPosition();
        List<OreNodes.Node> list = OreNodes.nodesNear(src.getLevel().getSeed(), p.x, p.z, SEARCH_CELLS);
        if (list.isEmpty()) {
            src.sendSuccess(() -> Component.literal("Рудных узлов рядом нет"), false);
            return 0;
        }
        src.sendSuccess(() -> Component.literal("Ближайшие рудные узлы:"), false);
        for (OreNodes.Node n : list.stream().limit(LIMIT).toList()) {
            double dist = Math.hypot(n.x() - p.x, n.z() - p.z);
            String kind = n.layered() ? "слоистый" : "гидротермальный";
            String line = String.format("  X %d, Z %d | %s | радиус %d | до центра %d | жил %d",
                    Math.round(n.x()), Math.round(n.z()), kind, Math.round(n.radius()), Math.round(dist), n.veins().size());
            src.sendSuccess(() -> Component.literal(line), false);
        }
        return list.size();
    }

    private static int veins(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        Vec3 p = src.getPosition();
        List<Vein> all = OreNodes.nodesNear(src.getLevel().getSeed(), p.x, p.z, SEARCH_CELLS).stream()
                .flatMap(n -> n.veins().stream())
                .sorted(Comparator.comparingDouble(v -> dist3(v, p)))
                .limit(LIMIT)
                .toList();
        if (all.isEmpty()) {
            src.sendSuccess(() -> Component.literal("Жил рядом нет"), false);
            return 0;
        }
        src.sendSuccess(() -> Component.literal("Ближайшие жилы:"), false);
        for (Vein v : all) {
            String line = String.format("  %s | X %d, Y %d, Z %d | до центра %d",
                    v.material().displayName(), Math.round(v.cx()), Math.round(v.cy()), Math.round(v.cz()),
                    Math.round(dist3(v, p)));
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
