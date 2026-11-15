package com.vgst54.projectgt.datagen;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import com.vgst54.projectgt.ProjectGT;
import com.vgst54.projectgt.material.Material;
import com.vgst54.projectgt.material.MaterialForm;
import com.vgst54.projectgt.material.Materials;
import com.vgst54.projectgt.material.OreTags;
import com.vgst54.projectgt.registry.ModBlocks;
import com.vgst54.projectgt.registry.ModCreativeTabs;
import com.vgst54.projectgt.registry.ModItems;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
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
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return ModBlocks.ores().map(ore -> ore.block().get()).toList();
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

            // Экран настроек (ключи: <modid>.configuration.<путь опции>)
            add("projectgt.configuration.title", "Project-GT+++ Configs");
            add("projectgt.configuration.section.projectgt.common.toml", "Project-GT+++ Configs");
            add("projectgt.configuration.section.projectgt.common.toml.title", "Project-GT+++ Configs");
            add("projectgt.configuration.worldgen", "World Generation");
            add("projectgt.configuration.enableVanillaOreGeneration", "Enable Vanilla Ore Generation");

            ModItems.byMaterial().forEach(entry -> entry.getValue().forEach((form, item) ->
                    addItem(item, form.displayName(entry.getKey()))));
            ModBlocks.ores().forEach(ore -> addBlock(ore.block(), ore.displayName()));
        }
    }

    /**
     * Плавка сырой руды в слитки: печь (200 тиков) и плавильная печь (100 тиков).
     * Это простая переплавка для начальных стадий; для высоких стадий будет отдельная продвинутая переработка.
     * Минералы без своего слитка в моде (свинец, цинк, серебро и т.д.) пока не плавятся.
     * Сюда же входит сырьё алмаза и графита (графит даёт пыль). Уголь (2 штуки с сырья) задан вручную в JSON.
     * Ванадиевый магнетит тоже не плавится: его надо сначала разделить (для получения ванадия), это будет в продвинутой переработке.
     */
    static final class ModRecipes extends RecipeProvider {
        ModRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
            super(output, lookup);
        }

        private static Map<Material, Supplier<ItemLike>> smeltingTable() {
            Supplier<ItemLike> iron = () -> Items.IRON_INGOT;
            Supplier<ItemLike> copper = () -> Items.COPPER_INGOT;
            Supplier<ItemLike> gold = () -> Items.GOLD_INGOT;
            Supplier<ItemLike> tin = () -> ModItems.get(Materials.TIN, MaterialForm.INGOT).get();
            Supplier<ItemLike> diamond = () -> Items.DIAMOND;
            Supplier<ItemLike> graphiteDust = () -> ModItems.get(Materials.GRAPHITE, MaterialForm.DUST).get();

            Map<Material, Supplier<ItemLike>> table = new LinkedHashMap<>();
            // Железо
            table.put(Materials.PYRITE, iron);
            table.put(Materials.HEMATITE, iron);
            table.put(Materials.MAGNETITE, iron);
            table.put(Materials.LIMONITE, iron);
            table.put(Materials.SIDERITE, iron);
            // Медь, олово, золото
            table.put(Materials.CHALCOPYRITE, copper);
            table.put(Materials.MALACHITE, copper);
            table.put(Materials.CASSITERITE, tin);
            table.put(Materials.TIN, tin);    // сырая оловянная руда самого олова
            table.put(Materials.NATIVE_GOLD, gold);
            // Алмаз и графит (графит плавится в пыль). Сырой уголь даёт 2 угля, см. recipe/smelting/coal_double.json
            table.put(Materials.DIAMOND, diamond);
            table.put(Materials.GRAPHITE, graphiteDust);
            return table;
        }

        /**
         * Пыль плавится в те же слитки, что и сырьё этого минерала, плюс пыль самих металлов и сплавов.
         * Алмаз и графит не входят: у них результат не слиток.
         */
        private static Map<Material, Supplier<ItemLike>> dustTable() {
            Map<Material, Supplier<ItemLike>> table = new LinkedHashMap<>(smeltingTable());
            table.remove(Materials.DIAMOND);
            table.remove(Materials.GRAPHITE);
            table.put(Materials.IRON, () -> Items.IRON_INGOT);
            table.put(Materials.COPPER, () -> Items.COPPER_INGOT);
            table.put(Materials.GOLD, () -> Items.GOLD_INGOT);
            table.put(Materials.BRONZE, () -> ModItems.get(Materials.BRONZE, MaterialForm.INGOT).get());
            table.put(Materials.STEEL, () -> ModItems.get(Materials.STEEL, MaterialForm.INGOT).get());
            return table;
        }

        @Override
        protected void buildRecipes(RecipeOutput output) {
            smeltingTable().forEach((material, result) -> cook(output, material, MaterialForm.RAW_ORE, result.get()));
            dustTable().forEach((material, result) -> cook(output, material, MaterialForm.DUST, result.get()));
        }

        /** Печь (200 тиков) и плавильная печь (100 тиков): форма материала (сырьё или пыль) в результат. */
        private void cook(RecipeOutput output, Material material, MaterialForm form, ItemLike out) {
            var tag = form.tag(material);
            Ingredient input = Ingredient.of(tag);
            String from = form == MaterialForm.DUST ? material.id() + "_dust" : material.id();
            String name = from + "_to_" + BuiltInRegistries.ITEM.getKey(out.asItem()).getPath();
            String trigger = "has_" + material.id() + "_" + form.suffix();

            SimpleCookingRecipeBuilder.smelting(input, RecipeCategory.MISC, out, 0.7f, 200)
                    .unlockedBy(trigger, has(tag))
                    .save(output, ResourceLocation.fromNamespaceAndPath(ProjectGT.MODID, "smelting/" + name));
            SimpleCookingRecipeBuilder.blasting(input, RecipeCategory.MISC, out, 0.7f, 100)
                    .unlockedBy(trigger, has(tag))
                    .save(output, ResourceLocation.fromNamespaceAndPath(ProjectGT.MODID, "blasting/" + name));
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
        }
    }
}
