package com.vgst54.projectgt.datagen;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.vgst54.projectgt.ProjectGT;
import com.vgst54.projectgt.item.ToolType;
import com.vgst54.projectgt.material.Heat;
import com.vgst54.projectgt.material.Material;
import com.vgst54.projectgt.material.MaterialForm;
import com.vgst54.projectgt.material.Materials;
import com.vgst54.projectgt.material.OreTags;
import com.vgst54.projectgt.pipe.FluidPipeBlock;
import com.vgst54.projectgt.pipe.ItemPipeBlock;
import com.vgst54.projectgt.machine.SteamPressure;
import com.vgst54.projectgt.machine.recipe.MachineRecipe;
import com.vgst54.projectgt.machine.recipe.MachineType;
import com.vgst54.projectgt.pipe.PipeSize;
import com.vgst54.projectgt.registry.ModBlocks;
import com.vgst54.projectgt.registry.ModCreativeTabs;
import com.vgst54.projectgt.registry.ModItems;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = ProjectGT.MODID)
public final class DataGenerators {
    private DataGenerators() {}

    @SubscribeEvent
    static void gather(GatherDataEvent event) {
        var generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper helper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();

        generator.addProvider(event.includeClient(), new ModItemModels(output, helper));
        generator.addProvider(event.includeClient(), new ModBlockStates(output, helper));
        generator.addProvider(event.includeClient(), new ModLang(output));

        var blockTags = generator.addProvider(event.includeServer(), new ModBlockTags(output, lookup, helper));
        generator.addProvider(event.includeServer(), new ModItemTags(output, lookup, blockTags.contentsGetter(), helper));
        generator.addProvider(event.includeServer(), new ModRecipes(output, lookup));
        generator.addProvider(event.includeServer(), new LootTableProvider(output, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(ModBlockLoot::new, LootContextParamSets.BLOCK)),
                lookup));
    }

    static final class ModItemModels extends ItemModelProvider {
        ModItemModels(PackOutput output, ExistingFileHelper helper) {
            super(output, ProjectGT.MODID, helper);
        }

        @Override
        protected void registerModels() {
            ModItems.byMaterial().forEach(entry -> entry.getValue().forEach((form, item) ->
                    withExistingParent(item.getId().getPath(), "item/generated").texture("layer0", form.texture(entry.getKey()))));
            // Инструменты: layer0 рукоять без тонировки, layer1 оголовье (тонируется материалом, см. ProjectGTClient).
            // Монтировка и ключ целиком из металла: один слой layer0, тонируется весь
            ModItems.byMaterialTools().forEach(entry -> entry.getValue().forEach((type, item) ->
                    {
                        var model = withExistingParent(item.getId().getPath(), "item/handheld");
                        if (type.isFullMetal()) {
                            model.texture("layer0", type.fullTexture());
                            if (type.hasDyedGrip()) {
                                model.texture("layer1", type.gripTexture());
                            }
                        } else {
                            model.texture("layer0", type.handleTexture()).texture("layer1", type.headTexture());
                        }
                    }));
        }
    }

    /** Блоки руды: камень снизу + тонированная накладка с жилами сверху (tintindex 0). */
    static final class ModBlockStates extends BlockStateProvider {
        ModBlockStates(PackOutput output, ExistingFileHelper helper) {
            super(output, ProjectGT.MODID, helper);
        }

        @Override
        protected void registerStatesAndModels() {
            ModBlocks.ores().forEach(ore -> {
                Block block = ore.block().get();
                var base = ore.stone().texture();
                var overlay = ore.material().iconSet().oreOverlay();

                var model = models().getBuilder(ore.block().getId().getPath())
                        .parent(new ModelFile.UncheckedModelFile("minecraft:block/block"))
                        .texture("particle", base)
                        .texture("base", base)
                        .texture("overlay", overlay);
                model.element().from(0, 0, 0).to(16, 16, 16)
                        .allFaces((dir, face) -> face.texture("#base").cullface(dir)).end();
                model.element().from(0, 0, 0).to(16, 16, 16)
                        .allFaces((dir, face) -> face.texture("#overlay").tintindex(0).cullface(dir)).end();
                model.renderType("minecraft:cutout");

                simpleBlock(block, model);
                simpleBlockItem(block, model);
            });
            registerPipes();
            registerBoilers();
            registerSteamMachines();
        }

        /** Паровые машины: лицо block/machine/<id>_front, бок и верх по давлению (low_pressure_side, low_pressure_top). */
        private void registerSteamMachines() {
            ModBlocks.steamMachines().forEach(machine -> {
                String pressure = machine.pressure() == SteamPressure.HIGH ? "high" : "low";
                String id = machine.block().getId().getPath();
                var model = models().orientable(id,
                        ResourceLocation.fromNamespaceAndPath(ProjectGT.MODID, "block/machine/" + pressure + "_pressure_side"),
                        ResourceLocation.fromNamespaceAndPath(ProjectGT.MODID, "block/machine/" + id + "_front"),
                        ResourceLocation.fromNamespaceAndPath(ProjectGT.MODID, "block/machine/" + pressure + "_pressure_top"));
                horizontalBlock(machine.block().get(), model);
                simpleBlockItem(machine.block().get(), model);
            });
        }

        /** Котлы: куб с одной текстурой на вид (block/boiler/<id>). */
        private void registerBoilers() {
            ModBlocks.boilers().forEach(boiler -> {
                Block block = boiler.block().get();
                var model = models().cubeAll(boiler.block().getId().getPath(),
                        ResourceLocation.fromNamespaceAndPath(ProjectGT.MODID, "block/boiler/" + boiler.type().id()));
                simpleBlock(block, model);
                simpleBlockItem(block, model);
            });
        }

        /**
         * Трубы: ядро + «руки» по сторонам (multipart). Руку делаем смотрящей на север и поворачиваем.
         * Одна серая текстура block/pipe, цвет материала даёт тонировка (tintindex 0).
         * Модели ядра, руки и инвентарная общие для всех материалов одного размера.
         */
        private void registerPipes() {
            var texture = modLoc("block/pipe");
            Map<PipeSize, ModelFile> cores = new EnumMap<>(PipeSize.class);
            Map<PipeSize, ModelFile> arms = new EnumMap<>(PipeSize.class);
            Map<PipeSize, ModelFile> inventory = new EnumMap<>(PipeSize.class);
            Map<PipeSize, ModelFile> arrowModels = new EnumMap<>(PipeSize.class);
            for (PipeSize size : PipeSize.values()) {
                float lo = 8 - size.thickness() / 2.0f;
                float hi = 8 + size.thickness() / 2.0f;
                cores.put(size, pipeBox("pipe/core_" + size.id(), texture, lo, lo, lo, hi, hi, hi));
                arms.put(size, pipeBox("pipe/arm_" + size.id(), texture, lo, lo, 0, hi, hi, lo));
                inventory.put(size, pipeBox("pipe/inventory_" + size.id(), texture, lo, lo, 0, hi, hi, 16));
                arrowModels.put(size, pipeArrow("pipe/arrow_" + size.id(), size));
            }
            ModBlocks.fluidPipes().forEach(pipe -> {
                PipeSize size = pipe.type().size();
                Block block = pipe.block().get();
                var builder = getMultipartBuilder(block);
                builder.part().modelFile(cores.get(size)).addModel().end();
                for (Direction dir : Direction.values()) {
                    builder.part().modelFile(arms.get(size))
                            .rotationX(dir == Direction.DOWN ? 90 : dir == Direction.UP ? 270 : 0)
                            .rotationY(switch (dir) {
                                case EAST -> 90;
                                case SOUTH -> 180;
                                case WEST -> 270;
                                default -> 0;
                            })
                            .addModel()
                            .condition(PipeBlock.PROPERTY_BY_DIRECTION.get(dir), true)
                            .end();
                }
                // Стрелки (выходы): мини-стрелка на конце трубы, указывает наружу; поворот как у руки.
                for (Direction dir : Direction.values()) {
                    builder.part().modelFile(arrowModels.get(size))
                            .rotationX(dir == Direction.DOWN ? 90 : dir == Direction.UP ? 270 : 0)
                            .rotationY(switch (dir) {
                                case EAST -> 90;
                                case SOUTH -> 180;
                                case WEST -> 270;
                                default -> 0;
                            })
                            .addModel()
                            .condition(FluidPipeBlock.ARROW.get(dir), true)
                            .end();
                }
                simpleBlockItem(block, inventory.get(size));
            });

            // Предметные трубы: режим restrictive отображается стрелками на открытых руках.
            ModBlocks.itemPipes().forEach(pipe -> {
                PipeSize size = pipe.type().size();
                Block block = pipe.block().get();
                var builder = getMultipartBuilder(block);
                builder.part().modelFile(cores.get(size)).addModel().end();
                for (Direction dir : Direction.values()) {
                    int rotX = dir == Direction.DOWN ? 90 : dir == Direction.UP ? 270 : 0;
                    int rotY = switch (dir) {
                        case EAST -> 90;
                        case SOUTH -> 180;
                        case WEST -> 270;
                        default -> 0;
                    };
                    builder.part().modelFile(arms.get(size)).rotationX(rotX).rotationY(rotY).addModel()
                            .condition(PipeBlock.PROPERTY_BY_DIRECTION.get(dir), true).end();
                    builder.part().modelFile(arrowModels.get(size)).rotationX(rotX).rotationY(rotY).addModel()
                            .condition(ItemPipeBlock.RESTRICTIVE.get(dir), true)
                            .condition(PipeBlock.PROPERTY_BY_DIRECTION.get(dir), true).end();
                }
                simpleBlockItem(block, inventory.get(size));
            });
        }

        /**
         * Мини-стрелка на северную сторону: светло-серая 2D-стрелка (block/pipe_arrow) на конце трубы,
         * на всех четырёх гранях трубы вокруг оси (верх, низ, восток, запад), острие на север.
         * Размер зависит от толщины трубы (2..4 пикселя), стрелки разных сторон лежат у своих краёв блока
         * и не налезают друг на друга. Повороты граней подобраны под UV-раскладку ванили:
         * верх 0, низ 180, восток 90, запад 270.
         */
        private ModelFile pipeArrow(String name, PipeSize size) {
            float t = size.thickness();
            float lo = 8 - t / 2.0f;
            float hi = 8 + t / 2.0f;
            float s = Math.max(2f, Math.min(t / 2.0f, 4f));
            float x1 = 8 - s / 2;
            float x2 = 8 + s / 2;
            float z1 = 0.5f;
            float z2 = 0.5f + s;
            float e = 0.02f;
            var model = models().getBuilder(name)
                    .parent(new ModelFile.UncheckedModelFile("minecraft:block/block"))
                    .texture("particle", modLoc("block/pipe"))
                    .texture("arrow", modLoc("block/pipe_arrow"));
            decal(model, Direction.UP, x1, hi + e, z1, x2, hi + e, z2, ModelBuilder.FaceRotation.ZERO);
            decal(model, Direction.DOWN, x1, lo - e, z1, x2, lo - e, z2, ModelBuilder.FaceRotation.UPSIDE_DOWN);
            decal(model, Direction.EAST, hi + e, x1, z1, hi + e, x2, z2, ModelBuilder.FaceRotation.CLOCKWISE_90);
            decal(model, Direction.WEST, lo - e, x1, z1, lo - e, x2, z2, ModelBuilder.FaceRotation.COUNTERCLOCKWISE_90);
            model.renderType("minecraft:cutout");
            return model;
        }

        private void decal(BlockModelBuilder model, Direction face, float x1, float y1, float z1,
                           float x2, float y2, float z2, ModelBuilder.FaceRotation rotation) {
            model.element().from(x1, y1, z1).to(x2, y2, z2)
                    .face(face).texture("#arrow").uvs(0, 0, 16, 16).rotation(rotation).end()
                    .end();
        }

        private ModelFile pipeBox(String name, net.minecraft.resources.ResourceLocation texture,
                                  float x1, float y1, float z1, float x2, float y2, float z2) {
            var model = models().getBuilder(name)
                    .parent(new ModelFile.UncheckedModelFile("minecraft:block/block"))
                    .texture("particle", texture)
                    .texture("pipe", texture);
            model.element().from(x1, y1, z1).to(x2, y2, z2)
                    .allFaces((dir, face) -> face.texture("#pipe").tintindex(0)).end();
            return model;
        }
    }

    static final class ModBlockLoot extends BlockLootSubProvider {
        ModBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
        }

        @Override
        protected void generate() {
            ModBlocks.ores().forEach(ore -> {
                Block block = ore.block().get();
                add(block, createOreDrop(block, ModItems.rawOre(ore.material())));
            });
            ModBlocks.fluidPipes().forEach(pipe -> dropSelf(pipe.block().get()));
            ModBlocks.itemPipes().forEach(pipe -> dropSelf(pipe.block().get()));
            ModBlocks.boilers().forEach(boiler -> dropSelf(boiler.block().get()));
            ModBlocks.steamMachines().forEach(machine -> dropSelf(machine.block().get()));
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return Stream.of(
                    ModBlocks.ores().map(ore -> ore.block().get()),
                    ModBlocks.fluidPipes().map(pipe -> (Block) pipe.block().get()),
                    ModBlocks.itemPipes().map(pipe -> (Block) pipe.block().get()),
                    ModBlocks.boilers().map(boiler -> (Block) boiler.block().get()),
                    ModBlocks.steamMachines().map(machine -> (Block) machine.block().get()))
                    .flatMap(blocks -> blocks).toList();
        }
    }

    static final class ModLang extends LanguageProvider {
        ModLang(PackOutput output) {
            super(output, ProjectGT.MODID, "en_us");
        }

        @Override
        protected void addTranslations() {
            add(ModCreativeTabs.MATERIALS_TITLE_KEY, "Project-GT+++ Materials");
            add(ModCreativeTabs.ORES_TITLE_KEY, "Project-GT+++ Ores");
            add(ModCreativeTabs.PIPES_TITLE_KEY, "Project-GT+++ Pipes");
            add(ModCreativeTabs.TOOLS_TITLE_KEY, "Project-GT+++ Tools");
            add(ModCreativeTabs.MACHINES_TITLE_KEY, "Project-GT+++ Machines");

            // Описания инструментов
            for (ToolType type : ToolType.values()) {
                add(type.descriptionKey(), type.description());
            }
            add("tooltip.projectgt.tool.material", "Material: %s");
            add("tooltip.projectgt.tool.durability", "Durability: %s / %s");
            add("tooltip.projectgt.tool.mining_speed", "Mining Speed: %s");
            add("tooltip.projectgt.tool.mining_level", "Mining Level: %s");
            add("tooltip.projectgt.tool.crafting_wear", "Loses 1 durability per craft");
            add("tooltip.projectgt.tool.dyeable_grip", "Combine with dye in crafting to color the grip");
            add("tooltip.projectgt.tier.wood", "Wood");
            add("tooltip.projectgt.tier.stone", "Stone");
            add("tooltip.projectgt.tier.iron", "Iron");
            add("tooltip.projectgt.tier.diamond", "Diamond");

            // Трубы
            add("tooltip.projectgt.pipe.throughput", "Throughput: %s L/s");
            add("tooltip.projectgt.pipe.heat_limit", "Heat limit: %s K");
            add("tooltip.projectgt.item_pipe.throughput", "Throughput: %s stacks/s");
            add("tooltip.projectgt.item_pipe.routing_value", "Routing value: %s");
            add("message.projectgt.item_pipe.restrictive_on", "Restrictive side %s: on");
            add("message.projectgt.item_pipe.restrictive_off", "Restrictive side %s: off");
            add("message.projectgt.pipe.side_enabled", "Side %s: enabled");
            add("message.projectgt.pipe.side_disabled", "Side %s: disabled");
            add("message.projectgt.pipe.arrow_on", "Arrow %s: on");
            add("message.projectgt.pipe.arrow_off", "Arrow %s: off");

            // Котлы и пар
            add("fluid_type.projectgt.steam", "Steam");
            add("tooltip.projectgt.boiler.steam_output", "Steam output: %s mB/t at full heat");
            add("tooltip.projectgt.boiler.warmup", "Warm-up: %s s");
            add("message.projectgt.boiler.status", "Heat: %s%% | Steam: %s mB | Water: %s mB");
            add("gui.projectgt.machine.steam", "Steam: %s / %s mB");

            // Экран настроек (ключи: <modid>.configuration.<путь опции>)
            add("projectgt.configuration.title", "Project-GT+++ Configs");
            add("projectgt.configuration.section.projectgt.common.toml", "Project-GT+++ Configs");
            add("projectgt.configuration.section.projectgt.common.toml.title", "Project-GT+++ Configs");
            add("projectgt.configuration.worldgen", "World Generation");
            add("projectgt.configuration.enableVanillaOreGeneration", "Enable Vanilla Ore Generation");

            ModItems.byMaterial().forEach(entry -> entry.getValue().forEach((form, item) ->
                    addItem(item, form.displayName(entry.getKey()))));
            ModItems.byMaterialTools().forEach(entry -> entry.getValue().forEach((type, item) ->
                    addItem(item, type.displayName(entry.getKey()))));
            ModBlocks.ores().forEach(ore -> addBlock(ore.block(), ore.displayName()));
            ModBlocks.fluidPipes().forEach(pipe -> addBlock(pipe.block(), pipe.type().displayName()));
            ModBlocks.itemPipes().forEach(pipe -> addBlock(pipe.block(), pipe.type().displayName()));
            ModBlocks.boilers().forEach(boiler -> addBlock(boiler.block(), boiler.type().displayName()));
            ModBlocks.steamMachines().forEach(machine -> addBlock(machine.block(), machine.displayName()));
        }
    }

    /**
     * Плавка сырой руды в слитки: печь (200 тиков) и плавильная печь (100 тиков).
     * Это простая переплавка для начальных стадий; для высоких стадий будет отдельная продвинутая переработка.
    * Минералы без своего слитка в моде (свинец, серебро и т.д.) пока не плавятся.
     * Сюда же входит сырьё алмаза, угля и графита (графит даёт пыль).
     * Ванадиевый магнетит тоже не плавится: его надо сначала разделить (для получения ванадия), это будет в продвинутой переработке.
     */
    static final class ModRecipes extends RecipeProvider {
        /** Результат плавки. metal: материал металла в результате (по его meltingPoint решается, плавится ли в печи), null для неметаллов. */
        private record SmeltingOutput(Supplier<ItemLike> item, int count, Material metal) {
            SmeltingOutput(Supplier<ItemLike> item, int count) {
                this(item, count, null);
            }
        }

        ModRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
            super(output, lookup);
        }

        private static Map<Material, SmeltingOutput> smeltingTable() {
            Supplier<ItemLike> iron = () -> Items.IRON_INGOT;
            Supplier<ItemLike> copper = () -> Items.COPPER_INGOT;
            Supplier<ItemLike> gold = () -> Items.GOLD_INGOT;
            Supplier<ItemLike> tin = () -> ModItems.get(Materials.TIN, MaterialForm.INGOT).get();
            Supplier<ItemLike> nickel = () -> ModItems.get(Materials.NICKEL, MaterialForm.INGOT).get();
            Supplier<ItemLike> zinc = () -> ModItems.get(Materials.ZINC, MaterialForm.INGOT).get();
            Supplier<ItemLike> diamond = () -> Items.DIAMOND;
            Supplier<ItemLike> coal = () -> Items.COAL;
            Supplier<ItemLike> redstone = () -> Items.REDSTONE;
            Supplier<ItemLike> graphiteDust = () -> ModItems.get(Materials.GRAPHITE, MaterialForm.DUST).get();

            Map<Material, SmeltingOutput> table = new LinkedHashMap<>();
            // Железо
            table.put(Materials.PYRITE, new SmeltingOutput(iron, 1, Materials.IRON));
            table.put(Materials.HEMATITE, new SmeltingOutput(iron, 1, Materials.IRON));
            table.put(Materials.MAGNETITE, new SmeltingOutput(iron, 1, Materials.IRON));
            table.put(Materials.LIMONITE, new SmeltingOutput(iron, 1, Materials.IRON));
            table.put(Materials.SIDERITE, new SmeltingOutput(iron, 1, Materials.IRON));
            // Медь, олово, золото
            table.put(Materials.CHALCOPYRITE, new SmeltingOutput(copper, 1, Materials.COPPER));
            table.put(Materials.MALACHITE, new SmeltingOutput(copper, 1, Materials.COPPER));
            table.put(Materials.CASSITERITE, new SmeltingOutput(tin, 1, Materials.TIN));
            table.put(Materials.TIN, new SmeltingOutput(tin, 1, Materials.TIN));    // сырая оловянная руда самого олова
            table.put(Materials.NATIVE_GOLD, new SmeltingOutput(gold, 1, Materials.GOLD));
            // Никель и цинк
            table.put(Materials.PENTLANDITE, new SmeltingOutput(nickel, 1, Materials.NICKEL));
            table.put(Materials.SPHALERITE, new SmeltingOutput(zinc, 1, Materials.ZINC));
            // Алмаз, уголь, графит (графит плавится в пыль)
            table.put(Materials.DIAMOND, new SmeltingOutput(diamond, 1));
            table.put(Materials.COAL, new SmeltingOutput(coal, 2));
            table.put(Materials.GRAPHITE, new SmeltingOutput(graphiteDust, 1));
            // Сырой редстоун даёт 5 ванильной красной пыли
            table.put(Materials.REDSTONE, new SmeltingOutput(redstone, 5));
            return table;
        }

        private static Map<Material, SmeltingOutput> dustSmeltingTable() {
            Map<Material, SmeltingOutput> table = new LinkedHashMap<>(smeltingTable());
            table.remove(Materials.COAL);
            table.remove(Materials.GRAPHITE);
            table.remove(Materials.REDSTONE);   // у редстоуна нет своей пыли, результат и так пыль
            table.put(Materials.IRON, new SmeltingOutput(() -> Items.IRON_INGOT, 1, Materials.IRON));
            table.put(Materials.COPPER, new SmeltingOutput(() -> Items.COPPER_INGOT, 1, Materials.COPPER));
            table.put(Materials.GOLD, new SmeltingOutput(() -> Items.GOLD_INGOT, 1, Materials.GOLD));
            table.put(Materials.BRONZE, new SmeltingOutput(
                    () -> ModItems.get(Materials.BRONZE, MaterialForm.INGOT).get(), 1, Materials.BRONZE));
            table.put(Materials.NICKEL, new SmeltingOutput(
                    () -> ModItems.get(Materials.NICKEL, MaterialForm.INGOT).get(), 1, Materials.NICKEL));
            table.put(Materials.ZINC, new SmeltingOutput(
                    () -> ModItems.get(Materials.ZINC, MaterialForm.INGOT).get(), 1, Materials.ZINC));
            return table;
        }

        private static void addCookingRecipes(RecipeOutput output, Ingredient input, net.minecraft.tags.TagKey<Item> inputTag,
                                              ItemStack result, String trigger, String name) {
            SimpleCookingRecipeBuilder.smelting(input, RecipeCategory.MISC, result, 0.7f, 200)
                    .unlockedBy(trigger, has(inputTag))
                    .save(output, ResourceLocation.fromNamespaceAndPath(ProjectGT.MODID, "smelting/" + name));
            SimpleCookingRecipeBuilder.blasting(input, RecipeCategory.MISC, result, 0.7f, 100)
                    .unlockedBy(trigger, has(inputTag))
                    .save(output, ResourceLocation.fromNamespaceAndPath(ProjectGT.MODID, "blasting/" + name));
            }

        @Override
        protected void buildRecipes(RecipeOutput output) {
            smeltingTable().forEach((material, result) -> {
                if (!Heat.furnaceCanSmelt(result.metal())) return;   // слишком тугоплавкий для обычной печи
                ItemLike out = result.item().get();
                ItemStack resultStack = new ItemStack(out.asItem(), result.count());
                var rawTag = MaterialForm.RAW_ORE.tag(material);
                String name = material.id() + "_to_" + BuiltInRegistries.ITEM.getKey(out.asItem()).getPath();
                addCookingRecipes(output, Ingredient.of(rawTag), rawTag, resultStack,
                        "has_" + material.id() + "_raw_ore", name);
            });

            dustSmeltingTable().forEach((material, result) -> {
                if (!Heat.furnaceCanSmelt(result.metal())) return;
                ItemLike out = result.item().get();
                var dustTag = MaterialForm.DUST.tag(material);
                String name = "dust_" + material.id() + "_to_" + BuiltInRegistries.ITEM.getKey(out.asItem()).getPath();
                addCookingRecipes(output, Ingredient.of(dustTag), dustTag,
                        new ItemStack(out.asItem(), result.count()), "has_" + material.id() + "_dust", name);
            });

            buildAlloyRecipes(output);
            buildMachineRecipes(output);
        }

        /**
         * Сплавы: пыль смешивается крафтом. Бронза: 3 пыли меди и 1 пыль олова дают 4 пыли бронзы
         * (как в GT). Слиток бронзы получается плавкой пыли в печи (бронза плавится при 950).
         */
        private void buildAlloyRecipes(RecipeOutput output) {
            var copperDust = MaterialForm.DUST.tag(Materials.COPPER);
            var tinDust = MaterialForm.DUST.tag(Materials.TIN);
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.get(Materials.BRONZE, MaterialForm.DUST).get(), 4)
                    .requires(Ingredient.of(copperDust), 3)
                    .requires(Ingredient.of(tinDust))
                    .unlockedBy("has_copper_dust", has(copperDust))
                    .unlockedBy("has_tin_dust", has(tinDust))
                    .save(output, ResourceLocation.fromNamespaceAndPath(ProjectGT.MODID, "alloy/bronze_dust"));
        }

        private static void machineRecipe(RecipeOutput output, MachineType machine, String name,
                                          List<SizedIngredient> inputs, List<ItemStack> outputs, int eut, int ticks) {
            output.accept(ResourceLocation.fromNamespaceAndPath(ProjectGT.MODID, machine.id() + "/" + name),
                    new MachineRecipe(machine, inputs, outputs, eut, ticks), null);
        }

        /**
         * Рецепты машин. Значения черновые, баланс правится здесь.
         * Дробитель и компрессор тянут 2 EU/t, кузнечный молот 16 EU/t: так паровые версии тратят 2/4 и 16/32 mB/t,
         * как в GTCEu. Рецептов экстрактора пока нет: ему нужны жидкости или резина.
         * Ванильные слитки (железо, медь, золото) лежат в тегах c:ingots/&lt;материал&gt;, как и слитки мода.
         */
        private void buildMachineRecipes(RecipeOutput output) {
            for (Material material : List.of(Materials.IRON, Materials.COPPER, Materials.GOLD, Materials.TIN,
                    Materials.NICKEL, Materials.ZINC, Materials.BRONZE, Materials.STEEL)) {
                SizedIngredient ingot = SizedIngredient.of(MaterialForm.INGOT.tag(material), 1);
                if (material.has(MaterialForm.DUST)) {
                    machineRecipe(output, MachineType.MACERATOR, material.id() + "_ingot_to_dust", List.of(ingot),
                            List.of(new ItemStack(ModItems.get(material, MaterialForm.DUST).get())), 2, 400);
                }
                if (material.has(MaterialForm.PLATE)) {
                    machineRecipe(output, MachineType.FORGE_HAMMER, material.id() + "_ingot_to_plate", List.of(ingot),
                            List.of(new ItemStack(ModItems.get(material, MaterialForm.PLATE).get())), 16, 100);
                }
            }
            // Компрессор: девять штук в ванильный блок.
            machineRecipe(output, MachineType.COMPRESSOR, "iron_block", List.of(SizedIngredient.of(Items.IRON_INGOT, 9)),
                    List.of(new ItemStack(Items.IRON_BLOCK)), 2, 300);
            machineRecipe(output, MachineType.COMPRESSOR, "copper_block", List.of(SizedIngredient.of(Items.COPPER_INGOT, 9)),
                    List.of(new ItemStack(Items.COPPER_BLOCK)), 2, 300);
            machineRecipe(output, MachineType.COMPRESSOR, "gold_block", List.of(SizedIngredient.of(Items.GOLD_INGOT, 9)),
                    List.of(new ItemStack(Items.GOLD_BLOCK)), 2, 300);
            machineRecipe(output, MachineType.COMPRESSOR, "coal_block", List.of(SizedIngredient.of(Items.COAL, 9)),
                    List.of(new ItemStack(Items.COAL_BLOCK)), 2, 300);
            machineRecipe(output, MachineType.COMPRESSOR, "redstone_block", List.of(SizedIngredient.of(Items.REDSTONE, 9)),
                    List.of(new ItemStack(Items.REDSTONE_BLOCK)), 2, 300);
        }
    }

    static final class ModBlockTags extends BlockTagsProvider {
        ModBlockTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper helper) {
            super(output, lookup, ProjectGT.MODID, helper);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            ModBlocks.ores().forEach(ore -> {
                Block block = ore.block().get();
                tag(BlockTags.MINEABLE_WITH_PICKAXE).add(block);
                tag(BlockTags.NEEDS_STONE_TOOL).add(block);
                tag(OreTags.block(ore.material())).add(block);
            });
            ModBlocks.fluidPipes().forEach(pipe -> tag(BlockTags.MINEABLE_WITH_PICKAXE).add(pipe.block().get()));
            ModBlocks.itemPipes().forEach(pipe -> tag(BlockTags.MINEABLE_WITH_PICKAXE).add(pipe.block().get()));
            ModBlocks.boilers().forEach(boiler -> tag(BlockTags.MINEABLE_WITH_PICKAXE).add(boiler.block().get()));
            ModBlocks.steamMachines().forEach(machine -> tag(BlockTags.MINEABLE_WITH_PICKAXE).add(machine.block().get()));
            for (Material material : Materials.all()) {
                if (material.hasOre()) {
                    tag(OreTags.ALL_BLOCKS).addTag(OreTags.block(material));
                }
            }
        }
    }

    static final class ModItemTags extends ItemTagsProvider {
        ModItemTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
                    CompletableFuture<TagLookup<Block>> blockTags, ExistingFileHelper helper) {
            super(output, lookup, blockTags, ProjectGT.MODID, helper);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            for (Material material : Materials.all()) {
                for (MaterialForm form : material.forms()) {
                    Item item = ModItems.get(material, form).get();
                    tag(form.tag(material)).add(item);
                    tag(form.formTag()).addTag(form.tag(material));
                }
                if (material.hasOre()) {
                    copy(OreTags.block(material), OreTags.item(material));
                }
            }
            copy(OreTags.ALL_BLOCKS, OreTags.ALL_ITEMS);

            // Инструменты: projectgt:tools/<тип> и ванильные теги для кирок, топоров, лопат, мотыг и мечей
            ModItems.byMaterialTools().forEach(entry -> entry.getValue().forEach((type, holder) -> {
                Item item = holder.get();
                tag(type.tag()).add(item);
                switch (type) {
                    case PICKAXE -> tag(ItemTags.PICKAXES).add(item);
                    case AXE -> tag(ItemTags.AXES).add(item);
                    case SHOVEL -> tag(ItemTags.SHOVELS).add(item);
                    case HOE -> tag(ItemTags.HOES).add(item);
                    case CROWBAR -> tag(ItemTags.DYEABLE).add(item);   // хват красится ванильным рецептом покраски
                    case SWORD -> tag(ItemTags.SWORDS).add(item);
                    default -> { }
                }
            }));
        }
    }
}
