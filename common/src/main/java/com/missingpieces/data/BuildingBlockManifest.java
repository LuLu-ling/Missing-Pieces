package com.missingpieces.data;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

public final class BuildingBlockManifest {
    private BuildingBlockManifest() {
    }

    public static void main(String[] args) throws IOException, ReflectiveOperationException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected the output file path.");
        }

        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var buildingTab = BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(CreativeModeTabs.BUILDING_BLOCKS);
        Set<String> blockIds = new TreeSet<>();

        Field generatorField = CreativeModeTab.class.getDeclaredField("displayItemsGenerator");
        generatorField.setAccessible(true);
        var generator = (CreativeModeTab.DisplayItemsGenerator) generatorField.get(buildingTab);
        var parameters = new CreativeModeTab.ItemDisplayParameters(FeatureFlags.DEFAULT_FLAGS, true, registries);
        generator.accept(parameters, new CreativeModeTab.Output() {
            @Override
            public void accept(ItemStack stack, CreativeModeTab.TabVisibility visibility) {
                collect(Block.byItem(stack.getItem()));
            }

            @Override
            public void accept(ItemLike item) {
                collect(Block.byItem(item.asItem()));
            }

            @Override
            public void accept(ItemLike item, CreativeModeTab.TabVisibility visibility) {
                collect(Block.byItem(item.asItem()));
            }

            private void collect(Block block) {
                var id = BuiltInRegistries.BLOCK.getKey(block);
                if (id != null && id.getNamespace().equals("minecraft")) {
                    blockIds.add(id.getPath());
                }
            }
        });

        Path output = Path.of(args[0]);
        Files.createDirectories(output.getParent());
        Files.write(output, blockIds);
    }
}
