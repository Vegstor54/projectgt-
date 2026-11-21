package com.vgst54.projectgt.registry;

import com.vgst54.projectgt.ProjectGT;
import com.vgst54.projectgt.boiler.BoilerMenu;
import com.vgst54.projectgt.machine.SteamMachineMenu;
import com.vgst54.projectgt.multiblock.part.PartItemMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Типы меню: одно на все паровые машины и одно на котлы. */
public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ProjectGT.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<SteamMachineMenu>> STEAM_MACHINE =
            MENUS.register("steam_machine", () -> IMenuTypeExtension.create(SteamMachineMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<BoilerMenu>> BOILER =
            MENUS.register("boiler", () -> IMenuTypeExtension.create(BoilerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<PartItemMenu>> PART_ITEMS =
            MENUS.register("part_items", () -> IMenuTypeExtension.create(PartItemMenu::new));

    private ModMenus() {}
}
