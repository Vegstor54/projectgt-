package com.vgst54.projectgt.datagen;

import java.util.concurrent.CompletableFuture;

import com.vgst54.projectgt.ProjectGT;
import com.vgst54.projectgt.material.Material;
import com.vgst54.projectgt.material.MaterialForm;
import com.vgst54.projectgt.material.Materials;
import com.vgst54.projectgt.registry.ModCreativeTabs;
import com.vgst54.projectgt.registry.ModItems;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
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
        generator.addProvider(event.includeClient(), new ModLang(output));

        var blockTags = generator.addProvider(event.includeServer(), new ModBlockTags(output, lookup, helper));
        generator.addProvider(event.includeServer(), new ModItemTags(output, lookup, blockTags.contentsGetter(), helper));
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

    static final class ModLang extends LanguageProvider {
        ModLang(PackOutput output) {
            super(output, ProjectGT.MODID, "en_us");
        }

        @Override
        protected void addTranslations() {
            add(ModCreativeTabs.MATERIALS_TITLE_KEY, "Project-GT+++ Materials");
            add(ModCreativeTabs.ORES_TITLE_KEY, "Project-GT+++ Ores");
            ModItems.byMaterial().forEach(entry -> entry.getValue().forEach((form, item) ->
                    addItem(item, form.displayName(entry.getKey()))));
        }
    }

    // Пустой провайдер блок-тегов нужен, чтобы ItemTagsProvider мог ссылаться на него.
    static final class ModBlockTags extends BlockTagsProvider {
        ModBlockTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper helper) {
            super(output, lookup, ProjectGT.MODID, helper);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {}
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
            }
        }
    }
}
