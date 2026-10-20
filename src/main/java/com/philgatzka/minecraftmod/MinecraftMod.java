package com.philgatzka.minecraftmod;

import com.mojang.logging.LogUtils;
import com.philgatzka.minecraftmod.blueprint.BlueprintCommands;
import com.philgatzka.minecraftmod.blueprint.PlayerSelections;
import com.philgatzka.minecraftmod.block.BuildersHutBlock;
import com.philgatzka.minecraftmod.block.TownHallBlock;
import com.philgatzka.minecraftmod.block.entity.BuildersHutBlockEntity;
import com.philgatzka.minecraftmod.block.entity.TownHallBlockEntity;
import com.philgatzka.minecraftmod.client.ConstructionRenderer;
import com.philgatzka.minecraftmod.client.renderer.SettlerRenderer;
import com.philgatzka.minecraftmod.client.screen.BlueprintPickerScreen;
import com.philgatzka.minecraftmod.client.screen.BlueprintScreen;
import com.philgatzka.minecraftmod.client.screen.TownHallScreen;
import com.philgatzka.minecraftmod.client.screen.WorkplaceScreen;
import com.philgatzka.minecraftmod.construction.ConstructionSites;
import com.philgatzka.minecraftmod.entity.SettlerEntity;
import com.philgatzka.minecraftmod.item.BlueprintWandItem;
import com.philgatzka.minecraftmod.item.BuildersWandItem;
import com.philgatzka.minecraftmod.menu.BlueprintMenu;
import com.philgatzka.minecraftmod.menu.BlueprintPickerMenu;
import com.philgatzka.minecraftmod.menu.TownHallMenu;
import com.philgatzka.minecraftmod.menu.WorkplaceMenu;
import com.philgatzka.minecraftmod.network.ModNetwork;
import com.philgatzka.minecraftmod.settlement.Territory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(MinecraftMod.MODID)
public class MinecraftMod
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "minecraftmod";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "minecraftmod" namespace
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "minecraftmod" namespace
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "minecraftmod" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    // Create a Deferred Register to hold BlockEntityTypes which will all be registered under the "minecraftmod" namespace
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    // Create a Deferred Register to hold EntityTypes which will all be registered under the "minecraftmod" namespace
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    // Create a Deferred Register to hold MenuTypes which will all be registered under the "minecraftmod" namespace
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);

    // Creates a new Block with the id "minecraftmod:example_block", combining the namespace and path
    public static final RegistryObject<Block> EXAMPLE_BLOCK = BLOCKS.register("example_block", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)));
    // Creates a new BlockItem with the id "minecraftmod:example_block", combining the namespace and path
    public static final RegistryObject<Item> EXAMPLE_BLOCK_ITEM = ITEMS.register("example_block", () -> new BlockItem(EXAMPLE_BLOCK.get(), new Item.Properties()));

    // Creates the Town Hall block with the id "minecraftmod:town_hall", plus its BlockItem and BlockEntityType
    public static final RegistryObject<Block> TOWN_HALL = BLOCKS.register("town_hall", () -> new TownHallBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.QUARTZ).requiresCorrectToolForDrops().strength(2.0f, 6.0f).sound(SoundType.STONE)));
    public static final RegistryObject<Item> TOWN_HALL_ITEM = ITEMS.register("town_hall", () -> new BlockItem(TOWN_HALL.get(), new Item.Properties()));
    @SuppressWarnings("DataFlowIssue") // null is fine here, the data fixer type is only needed for vanilla
    public static final RegistryObject<BlockEntityType<TownHallBlockEntity>> TOWN_HALL_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("town_hall",
            () -> BlockEntityType.Builder.of(TownHallBlockEntity::new, TOWN_HALL.get()).build(null));
    // The Town Hall screen's menu; IForgeMenuType lets us send extra data (TownHallInfo) to the client when it opens
    public static final RegistryObject<MenuType<TownHallMenu>> TOWN_HALL_MENU = MENU_TYPES.register("town_hall", () -> IForgeMenuType.create(TownHallMenu::new));

    // Creates the Builder's Hut block with the id "minecraftmod:builders_hut": a workplace that employs one settler as builder
    public static final RegistryObject<Block> BUILDERS_HUT = BLOCKS.register("builders_hut", () -> new BuildersHutBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD).strength(2.0f, 3.0f).sound(SoundType.WOOD)));
    public static final RegistryObject<Item> BUILDERS_HUT_ITEM = ITEMS.register("builders_hut", () -> new BlockItem(BUILDERS_HUT.get(), new Item.Properties()));
    @SuppressWarnings("DataFlowIssue") // null is fine here, the data fixer type is only needed for vanilla
    public static final RegistryObject<BlockEntityType<BuildersHutBlockEntity>> BUILDERS_HUT_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("builders_hut",
            () -> BlockEntityType.Builder.of(BuildersHutBlockEntity::new, BUILDERS_HUT.get()).build(null));
    // Shared by all workplaces (Builder's Hut, ...): pick which settler works there
    public static final RegistryObject<MenuType<WorkplaceMenu>> WORKPLACE_MENU = MENU_TYPES.register("workplace", () -> IForgeMenuType.create(WorkplaceMenu::new));

    // The Blueprint Wand marks an area around a city block to save as a blueprint (see blueprint/Blueprints)
    public static final RegistryObject<Item> BLUEPRINT_WAND = ITEMS.register("blueprint_wand", () -> new BlueprintWandItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<MenuType<BlueprintMenu>> BLUEPRINT_MENU = MENU_TYPES.register("blueprint", () -> IForgeMenuType.create(BlueprintMenu::new));
    // The Builder's Wand places blueprints as construction sites (see construction/ConstructionSites)
    public static final RegistryObject<Item> BUILDERS_WAND = ITEMS.register("builders_wand", () -> new BuildersWandItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<MenuType<BlueprintPickerMenu>> BLUEPRINT_PICKER_MENU = MENU_TYPES.register("blueprint_picker",
            () -> IForgeMenuType.create(BlueprintPickerMenu::new));

    // Creates the Settler entity with the id "minecraftmod:settler", sized like a player, plus its spawn egg
    public static final RegistryObject<EntityType<SettlerEntity>> SETTLER = ENTITY_TYPES.register("settler", () -> EntityType.Builder.of(SettlerEntity::new, MobCategory.CREATURE)
            .sized(0.6f, 1.8f).build("settler"));
    public static final RegistryObject<Item> SETTLER_SPAWN_EGG = ITEMS.register("settler_spawn_egg", () -> new ForgeSpawnEggItem(SETTLER, 0x00AFAF, 0x463AA5, new Item.Properties()));

    // Creates a new food item with the id "minecraftmod:example_id", nutrition 1 and saturation 2
    public static final RegistryObject<Item> EXAMPLE_ITEM = ITEMS.register("example_item", () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
            .alwaysEat().nutrition(1).saturationMod(2f).build())));

    // Creates a creative tab with the id "minecraftmod:example_tab" for the example item, that is placed after the combat tab
    public static final RegistryObject<CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.minecraftmod"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> EXAMPLE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(EXAMPLE_ITEM.get()); // Add the example item to the tab. For your own tabs, this method is preferred over the event
                output.accept(TOWN_HALL_ITEM.get());
                output.accept(BUILDERS_HUT_ITEM.get());
                output.accept(BLUEPRINT_WAND.get());
                output.accept(BUILDERS_WAND.get());
                output.accept(SETTLER_SPAWN_EGG.get());
            }).build());

    public MinecraftMod(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so block entity types get registered
        BLOCK_ENTITY_TYPES.register(modEventBus);
        // Register the Deferred Register to the mod event bus so entity types get registered
        ENTITY_TYPES.register(modEventBus);
        // Register the Deferred Register to the mod event bus so menu types get registered
        MENU_TYPES.register(modEventBus);

        // Register our network packets
        ModNetwork.register();

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // /blueprint commands, and forgetting blueprint selections when players leave
        MinecraftForge.EVENT_BUS.addListener(BlueprintCommands::register);
        MinecraftForge.EVENT_BUS.addListener(PlayerSelections::onPlayerLoggedOut);
        MinecraftForge.EVENT_BUS.addListener(PlayerSelections::onServerStopped);
        // Tell clients about construction sites so they can outline them
        MinecraftForge.EVENT_BUS.addListener(ConstructionSites::onPlayerLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(ConstructionSites::onPlayerChangedDimension);
        // Settlement territory: show it to leaders, and keep hand-placed buildings inside it
        MinecraftForge.EVENT_BUS.addListener(Territory::onPlayerLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(Territory::onPlayerChangedDimension);
        MinecraftForge.EVENT_BUS.addListener(Territory::onBlockPlaced);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register the attributes (health, speed, ...) for our entities
        modEventBus.addListener(this::registerAttributes);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.logDirtBlock)
            LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
            event.accept(EXAMPLE_BLOCK_ITEM);
    }

    private void registerAttributes(EntityAttributeCreationEvent event)
    {
        event.put(SETTLER.get(), SettlerEntity.createAttributes().build());
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());

            // MenuScreens isn't thread-safe, so register on the main thread
            event.enqueueWork(() -> {
                MenuScreens.register(TOWN_HALL_MENU.get(), TownHallScreen::new);
                MenuScreens.register(WORKPLACE_MENU.get(), WorkplaceScreen::new);
                MenuScreens.register(BLUEPRINT_MENU.get(), BlueprintScreen::new);
                MenuScreens.register(BLUEPRINT_PICKER_MENU.get(), BlueprintPickerScreen::new);
            });
        }

        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event)
        {
            ConstructionRenderer.registerKeys(event);
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event)
        {
            event.registerEntityRenderer(SETTLER.get(), SettlerRenderer::new);
        }
    }
}
