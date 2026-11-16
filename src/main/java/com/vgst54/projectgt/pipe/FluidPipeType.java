package com.vgst54.projectgt.pipe;

import com.vgst54.projectgt.material.Material;

/**
 * Один вид жидкостной трубы: материал × размер.
 * throughput (L/s) равна внутренней ёмкости трубы, как в GTNH. heatLimit (K): жидкость горячее
 * уничтожает трубу.
 */
public record FluidPipeType(Material material, PipeSize size, int throughput, int heatLimit) {
    public String id() {
        return material.id() + "_" + size.id() + "_fluid_pipe";
    }

    public String displayName() {
        return size.displayName() + " " + material.displayName() + " Fluid Pipe";
    }
}
