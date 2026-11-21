package com.vgst54.projectgt.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.vgst54.projectgt.ProjectGT;
import com.vgst54.projectgt.block.StoneType;
import com.vgst54.projectgt.boiler.BoilerType;
import com.vgst54.projectgt.boiler.SteamBoilerBlock;
import com.vgst54.projectgt.machine.SteamMachineBlock;
import com.vgst54.projectgt.machine.SteamPressure;
import com.vgst54.projectgt.machine.recipe.MachineType;
import com.vgst54.projectgt.material.Material;
import com.vgst54.projectgt.material.Materials;
import com.vgst54.projectgt.multiblock.MultiblockControllerBlock;
import com.vgst54.projectgt.multiblock.Multiblocks;
import com.vgst54.projectgt.multiblock.PartAbility;
import com.vgst54.projectgt.multiblock.part.MultiblockPartBlock;
import com.vgst54.projectgt.pipe.FluidPipeBlock;
import com.vgst54.projectgt.pipe.FluidPipeMaterials;
import com.vgst54.projectgt.pipe.FluidPipeType;
import com.vgst54.projectgt.pipe.ItemPipeBlock;
import com.vgst54.projectgt.pipe.ItemPipeMaterials;
import com.vgst54.projectgt.pipe.ItemPipeType;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ProjectGT.MODID);

    /** Блок руды: материал × тип камня, вместе с предметом-блоком. */
    public record OreBlock(Material material, StoneType stone, DeferredBlock<Block> block, DeferredItem<BlockItem> item) {
        public String displayName() {
            return stone.namePrefix() + material.displayName() + " Ore";
        }
    }

    /** Жидкостная труба: вид (материал × размер), блок и предмет-блок. */
    public record FluidPipe(FluidPipeType type, DeferredBlock<FluidPipeBlock> block, DeferredItem<BlockItem> item) {}

    /** Предметная труба: вид (материал × размер), блок и предмет-блок. */
    public record ItemPipe(ItemPipeType type, DeferredBlock<ItemPipeBlock> block, DeferredItem<BlockItem> item) {}

    /** Паровой котёл: вид (чем греется × давление), блок и предмет-блок. */
    public record Boiler(BoilerType type, DeferredBlock<SteamBoilerBlock> block, DeferredItem<BlockItem> item) {}

    /** Паровая машина: вид (дробитель, экстрактор...) × давление, блок и предмет-блок. */
    public record SteamMachine(MachineType type, SteamPressure pressure, DeferredBlock<SteamMachineBlock> block,
                               DeferredItem<BlockItem> item) {
        /** id блока, например low_pressure_steam_forge_hammer. */
        public static String id(MachineType type, SteamPressure pressure) {
            return (pressure == SteamPressure.HIGH ? "high_pressure_steam_" : "low_pressure_steam_") + type.id();
        }

        public String displayName() {
            return (pressure == SteamPressure.HIGH ? "High Pressure Steam " : "Low Pressure Steam ") + type.displayName();
        }
    }

    /** Корпус парового мультиблока (бронзовые кирпичи). */
    public static final DeferredBlock<Block> BRONZE_BRICKS_CASING = BLOCKS.registerSimpleBlock("bronze_bricks_casing",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(3.0f, 6.0f)
                    .sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> BRONZE_BRICKS_CASING_ITEM =
            ModItems.ITEMS.registerSimpleBlockItem("bronze_bricks_casing", BRONZE_BRICKS_CASING);

    /** Контроллер парового измельчителя (форма в Multiblocks.steamGrinder). */
    public static final DeferredBlock<MultiblockControllerBlock> STEAM_GRINDER = BLOCKS.register("steam_grinder",
            () -> new MultiblockControllerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE)
                    .strength(3.0f, 6.0f).sound(SoundType.METAL).requiresCorrectToolForDrops(),
                    Multiblocks::steamGrinder));
    public static final DeferredItem<BlockItem> STEAM_GRINDER_ITEM =
            ModItems.ITEMS.registerSimpleBlockItem("steam_grinder", STEAM_GRINDER);

    /** Люки парового мультиблока. */
    public static final DeferredBlock<MultiblockPartBlock> STEAM_INPUT_BUS = part("steam_input_bus", PartAbility.STEAM_IMPORT_ITEMS);
    public static final DeferredBlock<MultiblockPartBlock> STEAM_OUTPUT_BUS = part("steam_output_bus", PartAbility.STEAM_EXPORT_ITEMS);
    public static final DeferredBlock<MultiblockPartBlock> STEAM_HATCH = part("steam_input_hatch", PartAbility.STEAM);
    public static final DeferredItem<BlockItem> STEAM_INPUT_BUS_ITEM = ModItems.ITEMS.registerSimpleBlockItem("steam_input_bus", STEAM_INPUT_BUS);
    public static final DeferredItem<BlockItem> STEAM_OUTPUT_BUS_ITEM = ModItems.ITEMS.registerSimpleBlockItem("steam_output_bus", STEAM_OUTPUT_BUS);
    public static final DeferredItem<BlockItem> STEAM_HATCH_ITEM = ModItems.ITEMS.registerSimpleBlockItem("steam_input_hatch", STEAM_HATCH);

    private static DeferredBlock<MultiblockPartBlock> part(String id, PartAbility ability) {
        return BLOCKS.register(id, () -> new MultiblockPartBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_ORANGE).strength(3.0f, 6.0f).sound(SoundType.METAL)
                .requiresCorrectToolForDrops(), ability));
    }

    /** Люки мультиблоков (для BE-типа, лута, тегов). */
    public static Stream<DeferredBlock<MultiblockPartBlock>> multiblockParts() {
        return Stream.of(STEAM_INPUT_BUS, STEAM_OUTPUT_BUS, STEAM_HATCH);
    }

    /** Блоки мультиблоков (корпуса, контроллеры и люки) для лута, тегов и вкладки. */
    public static Stream<Block> multiblockBlocks() {
        return Stream.concat(Stream.of(BRONZE_BRICKS_CASING.get(), STEAM_GRINDER.get()),
                multiblockParts().map(DeferredBlock::get));
    }

    private static final List<SteamMachine> STEAM_MACHINES = new ArrayList<>();
    private static final List<Boiler> BOILERS = new ArrayList<>();
    private static final List<ItemPipe> ITEM_PIPES = new ArrayList<>();
    private static final List<OreBlock> ORES = new ArrayList<>();
    private static final List<FluidPipe> FLUID_PIPES = new ArrayList<>();

    static {
        for (Material material : Materials.all()) {
            if (!material.hasOre()) {
                continue;
            }
            for (StoneType stone : StoneType.values()) {
                String id = stone.id() + "_" + material.id() + "_ore";
                DeferredBlock<Block> block = BLOCKS.register(id, () -> new Block(
                        BlockBehaviour.Properties.ofFullCopy(stone.base())
                                .strength(stone.hardness(), 3.0f)
                                .requiresCorrectToolForDrops()));
                DeferredItem<BlockItem> item = ModItems.ITEMS.registerSimpleBlockItem(id, block);
                ORES.add(new OreBlock(material, stone, block, item));
            }
        }
    }

    static {
        for (FluidPipeType type : FluidPipeMaterials.all()) {
            DeferredBlock<FluidPipeBlock> block = BLOCKS.register(type.id(), () -> new FluidPipeBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.0f, 6.0f)
                            .sound(SoundType.METAL)
                            .noOcclusion(),
                    type));
            DeferredItem<BlockItem> item = ModItems.ITEMS.registerSimpleBlockItem(type.id(), block);
            FLUID_PIPES.add(new FluidPipe(type, block, item));
        }
    }

    static {
        for (ItemPipeType type : ItemPipeMaterials.all()) {
            DeferredBlock<ItemPipeBlock> block = BLOCKS.register(type.id(), () -> new ItemPipeBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.0f, 6.0f)
                            .sound(SoundType.METAL)
                            .noOcclusion(),
                    type));
            DeferredItem<BlockItem> item = ModItems.ITEMS.registerSimpleBlockItem(type.id(), block);
            ITEM_PIPES.add(new ItemPipe(type, block, item));
        }
    }

    static {
        for (BoilerType type : BoilerType.all()) {
            DeferredBlock<SteamBoilerBlock> block = BLOCKS.register(type.id(), () -> new SteamBoilerBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.0f, 6.0f)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops(),
                    type));
            DeferredItem<BlockItem> item = ModItems.ITEMS.registerSimpleBlockItem(type.id(), block);
            BOILERS.add(new Boiler(type, block, item));
        }
    }

    static {
        for (SteamPressure pressure : SteamPressure.values()) {
            for (MachineType type : MachineType.values()) {
                String id = SteamMachine.id(type, pressure);
                DeferredBlock<SteamMachineBlock> block = BLOCKS.register(id, () -> new SteamMachineBlock(
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.METAL)
                                .strength(3.0f, 6.0f)
                                .sound(SoundType.METAL)
                                .requiresCorrectToolForDrops(),
                        type, pressure));
                DeferredItem<BlockItem> item = ModItems.ITEMS.registerSimpleBlockItem(id, block);
                STEAM_MACHINES.add(new SteamMachine(type, pressure, block, item));
            }
        }
    }

    private ModBlocks() {}

    /** Все паровые машины: сначала низкое давление, затем высокое, внутри по порядку {@link MachineType}. */
    public static Stream<SteamMachine> steamMachines() {
        return Collections.unmodifiableList(STEAM_MACHINES).stream();
    }

    /** Все паровые котлы: сначала низкое давление, затем высокое. */
    public static Stream<Boiler> boilers() {
        return Collections.unmodifiableList(BOILERS).stream();
    }

    /** Все блоки руды в порядке: материал, затем тип камня. */
    public static Stream<OreBlock> ores() {
        return Collections.unmodifiableList(ORES).stream();
    }

    /** Все предметные трубы в порядке: материал, размер. */
    public static Stream<ItemPipe> itemPipes() {
        return Collections.unmodifiableList(ITEM_PIPES).stream();
    }

    /** Все жидкостные трубы в порядке: материал, затем размер. */
    public static Stream<FluidPipe> fluidPipes() {
        return Collections.unmodifiableList(FLUID_PIPES).stream();
    }
}
