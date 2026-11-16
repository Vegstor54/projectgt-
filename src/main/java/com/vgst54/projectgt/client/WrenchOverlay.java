package com.vgst54.projectgt.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.vgst54.projectgt.item.WrenchItem;
import com.vgst54.projectgt.pipe.FluidPipeBlock;
import com.vgst54.projectgt.pipe.FluidPipeBlockEntity;
import com.vgst54.projectgt.pipe.ItemPipeBlock;
import com.vgst54.projectgt.pipe.PipeWrenchGrid;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;

/**
 * Оверлей ключа: сетка 3×3 на грани трубы, на которую смотрит игрок. Показывается только при ключе в руке.
 * Центр переключает саму грань, боковые клетки соседние стороны, угловые обратную грань.
 * На грани под курсором сетка яркая, клетка под курсором подсвечена; на остальных пяти гранях
 * та же сетка рисуется приглушённой, чтобы было видно состояние всех сторон сразу.
 * Обычный режим: крестик в клетке значит, что рука в эту сторону торчит.
 * С shift (режим стрелок): в клетках, где стоит стрелка (выход), рисуется стрелка; стрелок может быть до шести.
 * Раскладку клеток даёт PipeWrenchGrid, ту же, что использует клик на сервере.
 */
public final class WrenchOverlay {
    /** Сдвиг плоскости сетки от грани блока, чтобы линии не сливались с геометрией. */
    private static final double OFFSET = 0.003;
    /** Отступ крестика от краёв клетки. */
    private static final double MARGIN = 0.02;

    private static final double[] EDGES = {
            0.0, 0.5 - PipeWrenchGrid.CENTER_HALF, 0.5 + PipeWrenchGrid.CENTER_HALF, 1.0};

    /** Цвета: активная грань яркая, остальные обесцвеченные и полупрозрачные. */
    private record Style(int[] grid, int[] cross, int[] arrow) {}

    private static final Style ACTIVE = new Style(new int[]{230, 230, 230, 255}, new int[]{96, 255, 128, 255}, new int[]{255, 160, 40, 255});
    private static final Style DIM = new Style(new int[]{150, 150, 150, 90}, new int[]{125, 150, 130, 110}, new int[]{170, 135, 95, 110});
    private static final int[] HOVER = {255, 215, 64, 255};

    private WrenchOverlay() {}

    public static void render(RenderHighlightEvent.Block event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        // Только главная рука: именно с ней труба целится как полный блок (см. FluidPipeBlock.getShape).
        if (!(mc.player.getMainHandItem().getItem() instanceof WrenchItem)) {
            return;
        }
        BlockHitResult hit = event.getTarget();
        BlockPos pos = hit.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        boolean fluidPipe = state.getBlock() instanceof FluidPipeBlock;
        if (!fluidPipe && !(state.getBlock() instanceof ItemPipeBlock)) {
            return;
        }

        // Стрелки (выходы) есть только у жидкостных труб.
        boolean arrowMode = fluidPipe && mc.player.isShiftKeyDown();
        int flowMask = mc.level.getBlockEntity(pos) instanceof FluidPipeBlockEntity pipeEntity ? pipeEntity.flowMask() : 0;

        Direction hitFace = hit.getDirection();
        Vec3 local = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        int hoverU = PipeWrenchGrid.band(PipeWrenchGrid.along(PipeWrenchGrid.tangentU(hitFace), local.x, local.y, local.z));
        int hoverV = PipeWrenchGrid.band(PipeWrenchGrid.along(PipeWrenchGrid.tangentV(hitFace), local.x, local.y, local.z));

        Vec3 cam = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        VertexConsumer lines = event.getMultiBufferSource().getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(pos.getX() - cam.x, pos.getY() - cam.y, pos.getZ() - cam.z);
        PoseStack.Pose pose = poseStack.last();

        // Сначала приглушённые грани, потом яркая поверх.
        for (Direction face : Direction.values()) {
            if (face != hitFace) {
                drawFace(lines, pose, state, face, DIM, arrowMode, flowMask, false, 0, 0);
            }
        }
        drawFace(lines, pose, state, hitFace, ACTIVE, arrowMode, flowMask, true, hoverU, hoverV);
        poseStack.popPose();
    }

    private static void drawFace(VertexConsumer lines, PoseStack.Pose pose, BlockState state, Direction face,
                                 Style style, boolean arrowMode, int flowMask, boolean hover, int hoverU, int hoverV) {
        // Сетка: рамка грани и линии по границам центральной клетки.
        rect(lines, pose, face, 0, 0, 1, 1, style.grid());
        for (int i = 1; i <= 2; i++) {
            line(lines, pose, face, EDGES[i], 0, EDGES[i], 1, style.grid());
            line(lines, pose, face, 0, EDGES[i], 1, EDGES[i], style.grid());
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                double u0 = EDGES[i];
                double u1 = EDGES[i + 1];
                double v0 = EDGES[j];
                double v1 = EDGES[j + 1];
                Direction dir = PipeWrenchGrid.directionOf(face, i - 1, j - 1);

                if (arrowMode) {
                    if ((flowMask & (1 << dir.ordinal())) != 0) {
                        arrow(lines, pose, face, dir, u0, v0, u1, v1, style.arrow());
                    }
                } else if (state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(dir))) {
                    // Рука торчит в эту сторону: перечёркиваем клетку.
                    line(lines, pose, face, u0 + MARGIN, v0 + MARGIN, u1 - MARGIN, v1 - MARGIN, style.cross());
                    line(lines, pose, face, u0 + MARGIN, v1 - MARGIN, u1 - MARGIN, v0 + MARGIN, style.cross());
                }
                if (hover && i - 1 == hoverU && j - 1 == hoverV) {
                    // Клетка под курсором: рамка внутри, чтобы выделялась на фоне сетки.
                    rect(lines, pose, face, u0 + MARGIN / 2, v0 + MARGIN / 2, u1 - MARGIN / 2, v1 - MARGIN / 2, HOVER);
                }
            }
        }
    }

    /**
     * Стрелка в клетке. Для сторон вдоль грани: стрелка в плоскости, указывает на ту сторону.
     * Для сторон по нормали к грани (центр и углы): ромб, потому что стрелка смотрела бы в экран.
     */
    private static void arrow(VertexConsumer vc, PoseStack.Pose pose, Direction face, Direction dir,
                              double u0, double v0, double u1, double v1, int[] rgba) {
        double cu = (u0 + u1) / 2;
        double cv = (v0 + v1) / 2;
        double len = Math.min(u1 - u0, v1 - v0) * 0.35;

        double du;
        double dv;
        if (dir == PipeWrenchGrid.tangentU(face)) {
            du = 1;
            dv = 0;
        } else if (dir == PipeWrenchGrid.tangentU(face).getOpposite()) {
            du = -1;
            dv = 0;
        } else if (dir == PipeWrenchGrid.tangentV(face)) {
            du = 0;
            dv = 1;
        } else if (dir == PipeWrenchGrid.tangentV(face).getOpposite()) {
            du = 0;
            dv = -1;
        } else {
            line(vc, pose, face, cu - len, cv, cu, cv + len, rgba);
            line(vc, pose, face, cu, cv + len, cu + len, cv, rgba);
            line(vc, pose, face, cu + len, cv, cu, cv - len, rgba);
            line(vc, pose, face, cu, cv - len, cu - len, cv, rgba);
            return;
        }
        double tipU = cu + du * len;
        double tipV = cv + dv * len;
        double tailU = cu - du * len;
        double tailV = cv - dv * len;
        double baseU = tipU - du * len * 0.6;
        double baseV = tipV - dv * len * 0.6;
        line(vc, pose, face, tailU, tailV, tipU, tipV, rgba);
        line(vc, pose, face, tipU, tipV, baseU - dv * len * 0.5, baseV + du * len * 0.5, rgba);
        line(vc, pose, face, tipU, tipV, baseU + dv * len * 0.5, baseV - du * len * 0.5, rgba);
    }

    private static void rect(VertexConsumer vc, PoseStack.Pose pose, Direction face,
                             double u0, double v0, double u1, double v1, int[] rgba) {
        line(vc, pose, face, u0, v0, u1, v0, rgba);
        line(vc, pose, face, u1, v0, u1, v1, rgba);
        line(vc, pose, face, u1, v1, u0, v1, rgba);
        line(vc, pose, face, u0, v1, u0, v0, rgba);
    }

    /** Отрезок в плоскости грани: (u, v) это координаты вдоль касательных осей грани (0..1). */
    private static void line(VertexConsumer vc, PoseStack.Pose pose, Direction face,
                             double u0, double v0, double u1, double v1, int[] rgba) {
        double[] a = point(face, u0, v0);
        double[] c = point(face, u1, v1);
        float nx = (float) (c[0] - a[0]);
        float ny = (float) (c[1] - a[1]);
        float nz = (float) (c[2] - a[2]);
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 0) {
            nx /= len;
            ny /= len;
            nz /= len;
        }
        vc.addVertex(pose, (float) a[0], (float) a[1], (float) a[2])
                .setColor(rgba[0], rgba[1], rgba[2], rgba[3]).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose, (float) c[0], (float) c[1], (float) c[2])
                .setColor(rgba[0], rgba[1], rgba[2], rgba[3]).setNormal(pose, nx, ny, nz);
    }

    /** Точка на плоскости грани (чуть снаружи блока) в локальных координатах блока. */
    private static double[] point(Direction face, double u, double v) {
        double[] p = new double[3];
        p[face.getAxis().ordinal()] = face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 + OFFSET : -OFFSET;
        p[PipeWrenchGrid.tangentU(face).getAxis().ordinal()] = u;
        p[PipeWrenchGrid.tangentV(face).getAxis().ordinal()] = v;
        return p;
    }
}
