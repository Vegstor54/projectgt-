package com.vgst54.projectgt.datagen;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.vgst54.projectgt.ProjectGT;
import com.vgst54.projectgt.material.Material;
import com.vgst54.projectgt.material.MaterialForm;
import com.vgst54.projectgt.material.Materials;
import com.vgst54.projectgt.material.OreTags;
import com.vgst54.projectgt.registry.ModBlocks;
import com.vgst54.projectgt.registry.ModCreativeTabs;
import com.vgst54.projectgt.registry.ModItems;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
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
