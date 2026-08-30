package com.missingpieces.content;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import dev.architectury.registry.CreativeTabOutput;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;

import com.missingpieces.MissingPieces;

public final class VanillaBlockVariants {
    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(MissingPieces.MOD_ID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(MissingPieces.MOD_ID, Registries.ITEM);

    private static final Set<String> VARIANT_SUFFIXES = Set.of(
            "stairs", "slab", "wall", "fence", "button", "pressure_plate");
    private static final Set<String> WOOD_TYPES = Set.of(
            "oak", "spruce", "birch", "jungle", "acacia", "cherry", "dark_oak", "pale_oak",
            "mangrove", "bamboo", "crimson", "warped");
    private static final Set<String> MINERAL_TYPES = Set.of(
            "coal", "copper", "iron", "gold", "diamond", "emerald", "lapis", "redstone",
            "netherite", "amethyst", "quartz", "raw_iron", "raw_copper", "raw_gold");

    private static final List<VariantSet> VARIANTS = new ArrayList<>();
    private static boolean registered;

    private VanillaBlockVariants() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        for (Block base : findBuildingBlocks()) {
            registerVariants(base);
        }

        BLOCKS.register();
        ITEMS.register();
        addToBuildingTab();
    }

    private static Set<Block> findBuildingBlocks() {
        Set<Block> result = new HashSet<>();

        try {
            var buildingTab = BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(CreativeModeTabs.BUILDING_BLOCKS);
            Field generatorField = CreativeModeTab.class.getDeclaredField("displayItemsGenerator");
            generatorField.setAccessible(true);
            var generator = (CreativeModeTab.DisplayItemsGenerator) generatorField.get(buildingTab);
            var parameters = new CreativeModeTab.ItemDisplayParameters(FeatureFlags.DEFAULT_FLAGS, true,
                    net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
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
                    if (block != Blocks.AIR) {
                        result.add(block);
                    }
                }
            });
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Retain a deterministic fallback if a loader changes the creative-tab implementation.
        }

        if (result.isEmpty()) {
            for (var entry : BuiltInRegistries.BLOCK.entrySet()) {
                Identifier id = entry.getKey().identifier();
                if (id.getNamespace().equals("minecraft") && isBuildingBlockCandidate(entry.getValue(), id)) {
                    result.add(entry.getValue());
                }
            }
        }
        return result;
    }

    private static boolean isBuildingBlockCandidate(Block block, Identifier id) {
        String path = id.getPath();
        return block.asItem() != Items.AIR
                && !(block instanceof StairBlock)
                && !(block instanceof SlabBlock)
                && !(block instanceof WallBlock)
                && !(block instanceof FenceBlock)
                && !(block instanceof ButtonBlock)
                && !(block instanceof PressurePlateBlock)
                && !path.contains("ore")
                && !path.contains("leaves")
                && !path.contains("sapling")
                && !path.contains("coral")
                && !path.contains("candle")
                && !path.contains("torch")
                && !path.contains("flower")
                && !path.contains("mushroom")
                && !path.contains("potted")
                && !path.contains("sign")
                && !path.contains("shelf")
                && !path.contains("door")
                && !path.contains("trapdoor")
                && !path.contains("chest")
                && !path.contains("shulker")
                && !path.contains("banner")
                && !path.contains("bed")
                && !path.contains("glass_pane")
                && !path.contains("carpet")
                && !path.contains("concrete_powder")
                && !path.contains("wool")
                && !path.contains("command")
                && !path.contains("structure")
                && !path.contains("jigsaw")
                && !path.contains("spawner")
                && !path.contains("vault")
                && !path.contains("trial")
                && !path.contains("air");
    }
    private static void registerVariants(Block base) {
        Identifier baseId = BuiltInRegistries.BLOCK.getKey(base);
        if (baseId == null || !baseId.getNamespace().equals("minecraft") || !shouldGenerate(base, baseId)) {
            return;
        }

        VariantSet variant = new VariantSet(base, baseId);
        variant.stairs = registerVariant(variant, "stairs",
                () -> new StairBlock(base.defaultBlockState(), properties(base, variant.id("stairs"))));
        variant.slab = registerVariant(variant, "slab",
                () -> new SlabBlock(properties(base, variant.id("slab"))));
        variant.wall = registerVariant(variant, "wall",
                () -> new WallBlock(properties(base, variant.id("wall")).forceSolidOn()));
        variant.fence = registerVariant(variant, "fence",
                () -> new FenceBlock(properties(base, variant.id("fence"))));
        variant.button = registerVariant(variant, "button",
                () -> new ButtonBlock(BlockSetType.STONE, 20, properties(base, variant.id("button"))));
        variant.pressurePlate = registerVariant(variant, "pressure_plate",
                () -> new PressurePlateBlock(BlockSetType.STONE, properties(base, variant.id("pressure_plate"))));

        variant.stairsItem = registerItem(variant.id("stairs"), variant.stairs);
        variant.slabItem = registerItem(variant.id("slab"), variant.slab);
        variant.wallItem = registerItem(variant.id("wall"), variant.wall);
        variant.fenceItem = registerItem(variant.id("fence"), variant.fence);
        variant.buttonItem = registerItem(variant.id("button"), variant.button);
        variant.pressurePlateItem = registerItem(variant.id("pressure_plate"), variant.pressurePlate);
        if (variant.hasEntries()) {
            VARIANTS.add(variant);
        }
    }

    private static boolean shouldGenerate(Block block, Identifier id) {
        if (block == Blocks.AIR || block.asItem() == Items.AIR) {
            return false;
        }
        if (block instanceof StairBlock || block instanceof SlabBlock || block instanceof WallBlock
                || block instanceof FenceBlock || block instanceof ButtonBlock || block instanceof PressurePlateBlock
                || block instanceof TrapDoorBlock || block instanceof DoorBlock || block instanceof IronBarsBlock
                || block instanceof ChainBlock) {
            return false;
        }

        String path = id.getPath();
        for (String suffix : VARIANT_SUFFIXES) {
            if (path.endsWith("_" + suffix)) {
                return false;
            }
        }
        return !isWood(path) && !isMineral(path) && !path.equals("reinforced_deepslate");
    }

    private static boolean isWood(String path) {
        for (String woodType : WOOD_TYPES) {
            if (path.equals(woodType) || path.startsWith(woodType + "_") || path.contains("_" + woodType + "_")) {
                return true;
            }
        }
        return path.contains("_log") || path.endsWith("_wood") || path.contains("_hyphae")
                || path.contains("_stem") || path.contains("_leaves") || path.contains("_planks");
    }

    private static boolean isMineral(String path) {
        if (path.contains("copper")) {
            return true;
        }
        boolean mineralName = MINERAL_TYPES.stream().anyMatch(path::contains);
        return mineralName && (path.endsWith("_block") || path.endsWith("_ore") || path.startsWith("raw_"));
    }

    @SuppressWarnings("deprecation")
    private static BlockBehaviour.Properties properties(Block base, Identifier id) {
        var state = base.defaultBlockState();
        var properties = BlockBehaviour.Properties.of()
                .mapColor(base.defaultMapColor())
                .sound(state.getSoundType())
                .strength(base.defaultDestroyTime(), base.getExplosionResistance())
                .friction(base.getFriction())
                .speedFactor(base.getSpeedFactor())
                .jumpFactor(base.getJumpFactor())
                .bounceRestitution(base.getBounceRestitution())
                .lightLevel(ignored -> state.getLightEmission())
                .pushReaction(state.getPistonPushReaction())
                .instrument(state.instrument())
                .setId(ResourceKey.create(Registries.BLOCK, id));

        if (state.requiresCorrectToolForDrops()) {
            properties.requiresCorrectToolForDrops();
        }
        if (!state.canOcclude()) {
            properties.noOcclusion();
        }
        if (!state.blocksMotion()) {
            properties.noCollision();
        }
        if (state.ignitedByLava()) {
            properties.ignitedByLava();
        }
        return properties;
    }

    private static RegistrySupplier<Block> registerVariant(VariantSet variant, String suffix,
            Supplier<? extends Block> supplier) {
        if (hasVanillaVariant(variant.baseId, suffix)) {
            return null;
        }
        return registerBlock(variant.id(suffix), supplier);
    }

    private static boolean hasVanillaVariant(Identifier baseId, String suffix) {
        String path = baseId.getPath();
        if (hasVanillaBlock(path + "_" + suffix)) {
            return true;
        }
        if (path.endsWith("s") && hasVanillaBlock(path.substring(0, path.length() - 1) + "_" + suffix)) {
            return true;
        }
        return path.endsWith("_block")
                && hasVanillaBlock(path.substring(0, path.length() - "_block".length()) + "_" + suffix);
    }

    private static boolean hasVanillaBlock(String path) {
        return BuiltInRegistries.BLOCK.containsKey(Identifier.fromNamespaceAndPath("minecraft", path));
    }

    private static RegistrySupplier<Block> registerBlock(Identifier id, Supplier<? extends Block> supplier) {
        return BLOCKS.register(id, supplier);
    }

    private static RegistrySupplier<Item> registerItem(Identifier id, RegistrySupplier<Block> block) {
        if (block == null) {
            return null;
        }
        return ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, id))
                .useBlockDescriptionPrefix()));
    }

    private static void addToBuildingTab() {
        CreativeTabRegistry.modify(CreativeTabRegistry.defer(CreativeModeTabs.BUILDING_BLOCKS),
                (features, output, hasPermissions) -> VARIANTS.forEach(variant -> variant.addTo(output)));
    }

    private static final class VariantSet {
        private final Block base;
        private final Identifier baseId;
        private RegistrySupplier<Block> stairs;
        private RegistrySupplier<Block> slab;
        private RegistrySupplier<Block> wall;
        private RegistrySupplier<Block> fence;
        private RegistrySupplier<Block> button;
        private RegistrySupplier<Block> pressurePlate;
        private RegistrySupplier<Item> stairsItem;
        private RegistrySupplier<Item> slabItem;
        private RegistrySupplier<Item> wallItem;
        private RegistrySupplier<Item> fenceItem;
        private RegistrySupplier<Item> buttonItem;
        private RegistrySupplier<Item> pressurePlateItem;

        private VariantSet(Block base, Identifier baseId) {
            this.base = base;
            this.baseId = baseId;
        }

        private boolean hasEntries() {
            return stairs != null || slab != null || wall != null || fence != null
                    || button != null || pressurePlate != null;
        }

        private void addTo(CreativeTabOutput output) {
            ItemStack anchor = new ItemStack(base);
            anchor = insertAfter(output, anchor, stairsItem);
            anchor = insertAfter(output, anchor, slabItem);
            anchor = insertAfter(output, anchor, wallItem);
            anchor = insertAfter(output, anchor, fenceItem);
            anchor = insertAfter(output, anchor, buttonItem);
            insertAfter(output, anchor, pressurePlateItem);
        }

        private ItemStack insertAfter(CreativeTabOutput output, ItemStack anchor, RegistrySupplier<Item> item) {
            if (item == null) {
                return anchor;
            }
            ItemStack next = new ItemStack(item.get());
            output.acceptAfter(anchor, next);
            return next;
        }

        private Identifier id(String suffix) {
            return Identifier.fromNamespaceAndPath(MissingPieces.MOD_ID, baseId.getPath() + "_" + suffix);
        }
    }
}
