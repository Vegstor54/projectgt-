package com.vgst54.projectgt.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.vgst54.projectgt.item.WrenchItem;
import com.vgst54.projectgt.pipe.FluidPipeBlock;
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
 * Клетка, в которую целится курсор, подсвечена; крестик в клетке значит, что рука в эту сторону торчит.
 * Рисуем только линиями. Раскладку клеток даёт PipeWrenchGrid, ту же, что использует клик на сервере.
 */
public final class WrenchOverlay {
    /** Сдвиг плоскости сетки от грани блока, чтобы линии не сливались с геометрией. */
    private static final double OFFSET = 0.003;
    /** Отступ крестика от краёв клетки. */
    private static final double MARGIN = 0.02;

    private WrenchOverlay() {}

    public static void render(RenderHighlightEvent.Block event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (!(mc.player.getMainHandItem().getItem() instanceof WrenchItem)
                && !(mc.player.getOffhandItem().getItem() instanceof WrenchItem)) {
            return;
        }
        BlockHitResult hit = event.getTarget();
        BlockPos pos = hit.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        if (!(state.getBlock() instanceof FluidPipeBlock pipe)) {
            return;
        }

        Direction face = hit.getDirection();
        double half = pipe.type().size().halfWidth();
        double lo = 0.5 - half;
        double hi = 0.5 + half;
        double[] edges = {0.0, lo, hi, 1.0};

        Vec3 local = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        int hoverU = PipeWrenchGrid.band(PipeWrenchGrid.along(PipeWrenchGrid.tangentU(face), local.x, local.y, local.z), half);
        int hoverV = PipeWrenchGrid.band(PipeWrenchGrid.along(PipeWrenchGrid.tangentV(face), local.x, local.y, local.z), half);

        Vec3 cam = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        VertexConsumer lines = event.getMultiBufferSource().getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(pos.getX() - cam.x, pos.getY() - cam.y, pos.getZ() - cam.z);
        PoseStack.Pose pose = poseStack.last();

        // Сетка: рамка грани и линии по границам ядра.
        rect(lines, pose, face, 0, 0, 1, 1, 230, 230, 230);
        for (int i = 1; i <= 2; i++) {
            line(lines, pose, face, edges[i], 0, edges[i], 1, 230, 230, 230);
            line(lines, pose, face, 0, edges[i], 1, edges[i], 230, 230, 230);
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                double u0 = edges[i];
                double u1 = edges[i + 1];
                double v0 = edges[j];
                double v1 = edges[j + 1];
                Direction dir = PipeWrenchGrid.directionOf(face, i - 1, j - 1);

                if (state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(dir))) {
                    // Рука торчит в эту сторону: перечёркиваем клетку.
                    line(lines, pose, face, u0 + MARGIN, v0 + MARGIN, u1 - MARGIN, v1 - MARGIN, 96, 255, 128);
                    line(lines, pose, face, u0 + MARGIN, v1 - MARGIN, u1 - MARGIN, v0 + MARGIN, 96, 255, 128);
                }
                if (i - 1 == hoverU && j - 1 == hoverV) {
                    // Клетка под курсором: рамка внутри, чтобы выделялась на фоне сетки.
                    rect(lines, pose, face, u0 + MARGIN / 2, v0 + MARGIN / 2, u1 - MARGIN / 2, v1 - MARGIN / 2, 255, 215, 64);
                }
            }
        }
        poseStack.popPose();
    }

    private static void rect(VertexConsumer vc, PoseStack.Pose pose, Direction face,
                             double u0, double v0, double u1, double v1, int r, int g, int b) {
        line(vc, pose, face, u0, v0, u1, v0, r, g, b);
        line(vc, pose, face, u1, v0, u1, v1, r, g, b);
        line(vc, pose, face, u1, v1, u0, v1, r, g, b);
        line(vc, pose, face, u0, v1, u0, v0, r, g, b);
    }

    /** Отрезок в плоскости грани: (u, v) это координаты вдоль касательных осей грани (0..1). */
    private static void line(VertexConsumer vc, PoseStack.Pose pose, Direction face,
                             double u0, double v0, double u1, double v1, int r, int g, int b) {
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
        vc.addVertex(pose, (float) a[0], (float) a[1], (float) a[2]).setColor(r, g, b, 255).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose, (float) c[0], (float) c[1], (float) c[2]).setColor(r, g, b, 255).setNormal(pose, nx, ny, nz);
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
