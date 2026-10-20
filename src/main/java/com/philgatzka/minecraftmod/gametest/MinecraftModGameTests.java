package com.philgatzka.minecraftmod.gametest;

import com.mojang.authlib.GameProfile;
import com.philgatzka.minecraftmod.Config;
import com.philgatzka.minecraftmod.MinecraftMod;
import com.philgatzka.minecraftmod.block.entity.BuildersHutBlockEntity;
import com.philgatzka.minecraftmod.block.entity.TownHallBlockEntity;
import com.philgatzka.minecraftmod.blueprint.Blueprint;
import com.philgatzka.minecraftmod.blueprint.BlueprintBlocks;
import com.philgatzka.minecraftmod.blueprint.BlueprintPlacement;
import com.philgatzka.minecraftmod.blueprint.BlueprintSelection;
import com.philgatzka.minecraftmod.blueprint.Blueprints;
import com.philgatzka.minecraftmod.construction.ConstructionSite;
import com.philgatzka.minecraftmod.construction.ConstructionSiteData;
import com.philgatzka.minecraftmod.construction.ConstructionSites;
import com.philgatzka.minecraftmod.entity.SettlerEntity;
import com.philgatzka.minecraftmod.entity.SettlerNames;
import com.philgatzka.minecraftmod.menu.SettlerInventoryMenu;
import com.philgatzka.minecraftmod.network.TerritoryPacket;
import com.philgatzka.minecraftmod.settlement.Settlement;
import com.philgatzka.minecraftmod.settlement.SettlementData;
import com.philgatzka.minecraftmod.settlement.Settlements;
import com.philgatzka.minecraftmod.settlement.SettlerJob;
import com.philgatzka.minecraftmod.settlement.SettlerRecord;
import com.philgatzka.minecraftmod.settlement.Territory;
import io.netty.buffer.Unpooled;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// Only registered when GameTests are enabled (the gameTestServer run, or /test in the dev client/server)
// Run them all with: ./gradlew runGameTestServer
@GameTestHolder(MinecraftMod.MODID)
@PrefixGameTestTemplate(false)
public class MinecraftModGameTests
{
    // data/minecraftmod/structures/test_platform.nbt: 5x3x5 with a stone floor at its y=0.
    // GameTest positions are relative to one block below the structure, so the floor is at relative y=1.
    private static final String PLATFORM = "test_platform";
    private static final BlockPos FLOOR = new BlockPos(2, 1, 2);
    private static final BlockPos ABOVE_FLOOR = new BlockPos(2, 2, 2);

    // Settlement tests run in their own batch (one at a time) and start from no settlements or sites at all:
    // a settlement's territory is many chunks wide, so it would otherwise reach into neighbouring tests.
    // Settlers never despawn, so the ones earlier tests spawned are removed too, or they'd pile up in the kept test world.
    private static void clearSettlementsAndSites(GameTestHelper helper)
    {
        helper.getLevel().getEntities(MinecraftMod.SETTLER.get(), settler -> true).forEach(Entity::discard);
        SettlementData settlements = SettlementData.get(helper.getLevel().getServer());
        List.copyOf(settlements.all()).forEach(settlement -> settlements.remove(settlement.id()));
        ConstructionSiteData sites = ConstructionSiteData.get(helper.getLevel().getServer());
        sites.all().forEach(site -> sites.remove(site.id()));
    }

    @GameTest(template = PLATFORM)
    public static void settlerGetsNameFromLists(GameTestHelper helper)
    {
        SettlerEntity settler = helper.spawn(MinecraftMod.SETTLER.get(), ABOVE_FLOOR);

        helper.assertTrue(SettlerNames.FIRST_NAMES.contains(settler.getFirstName()), "Unexpected first name: " + settler.getFirstName());
        helper.assertTrue(SettlerNames.LAST_NAMES.contains(settler.getLastName()), "Unexpected last name: " + settler.getLastName());
        helper.assertTrue(settler.getName().getString().equals(settler.getFullName()), "Displayed name should be the full name, was: " + settler.getName().getString());
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void settlerHasPlayerHealth(GameTestHelper helper)
    {
        SettlerEntity settler = helper.spawn(MinecraftMod.SETTLER.get(), ABOVE_FLOOR);
        double playerHealth = Player.createAttributes().build().getBaseValue(Attributes.MAX_HEALTH);

        helper.assertTrue(settler.getMaxHealth() == playerHealth, "Max health should be " + playerHealth + ", was " + settler.getMaxHealth());
        helper.assertTrue(settler.getHealth() == playerHealth, "Should spawn at full health, was " + settler.getHealth());
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void settlerNameSurvivesSaveAndLoad(GameTestHelper helper)
    {
        SettlerEntity original = helper.spawn(MinecraftMod.SETTLER.get(), ABOVE_FLOOR);
        original.setFirstName("Testy");
        original.setLastName("McTestface");
        CompoundTag saved = original.saveWithoutId(new CompoundTag());

        SettlerEntity loaded = MinecraftMod.SETTLER.get().create(helper.getLevel());
        helper.assertTrue(loaded != null, "Could not create a settler");
        loaded.load(saved);

        helper.assertTrue(loaded.getFullName().equals("Testy McTestface"), "Name not restored after load, was: " + loaded.getFullName());
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void settlerDoesNotDespawn(GameTestHelper helper)
    {
        SettlerEntity settler = helper.spawn(MinecraftMod.SETTLER.get(), ABOVE_FLOOR);

        helper.assertFalse(settler.removeWhenFarAway(1000.0), "Settlers should not despawn when far from players");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void spawnEggSpawnsNamedSettler(GameTestHelper helper)
    {
        Player player = helper.makeMockPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MinecraftMod.SETTLER_SPAWN_EGG.get()));
        BlockPos floor = helper.absolutePos(FLOOR);
        AABB area = new AABB(helper.absolutePos(BlockPos.ZERO), helper.absolutePos(new BlockPos(5, 4, 5)));
        // Settlers never despawn, so ones from other tests (or earlier runs) may wander in; only count new ones
        List<SettlerEntity> before = helper.getLevel().getEntities(MinecraftMod.SETTLER.get(), area, settler -> true);

        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(floor), Direction.UP, floor, false)));

        List<SettlerEntity> settlers = helper.getLevel().getEntities(MinecraftMod.SETTLER.get(), area, settler -> !before.contains(settler));
        helper.assertTrue(settlers.size() == 1, "Expected 1 settler from the spawn egg, found " + settlers.size());
        helper.assertFalse(settlers.get(0).getFirstName().isEmpty(), "Settler from the spawn egg has no name");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void townHallHasBlockEntity(GameTestHelper helper)
    {
        helper.setBlock(ABOVE_FLOOR, MinecraftMod.TOWN_HALL.get());

        helper.assertTrue(helper.getBlockEntity(ABOVE_FLOOR) instanceof TownHallBlockEntity, "Town Hall should have a TownHallBlockEntity");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void townHallDropsItself(GameTestHelper helper)
    {
        helper.setBlock(ABOVE_FLOOR, MinecraftMod.TOWN_HALL.get());
        BlockPos pos = helper.absolutePos(ABOVE_FLOOR);

        // Ask the loot table what mining it with a pickaxe drops
        List<ItemStack> drops = Block.getDrops(helper.getBlockState(ABOVE_FLOOR), helper.getLevel(), pos, helper.getBlockEntity(ABOVE_FLOOR),
                null, new ItemStack(Items.IRON_PICKAXE));

        helper.assertTrue(drops.size() == 1 && drops.get(0).is(MinecraftMod.TOWN_HALL_ITEM.get()) && drops.get(0).getCount() == 1,
                "Town Hall should drop exactly one Town Hall item, dropped: " + drops);
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void townHallNeedsPickaxe(GameTestHelper helper)
    {
        BlockState townHall = MinecraftMod.TOWN_HALL.get().defaultBlockState();

        helper.assertTrue(townHall.requiresCorrectToolForDrops(), "Town Hall should require the correct tool to drop");
        helper.assertTrue(new ItemStack(Items.WOODEN_PICKAXE).isCorrectToolForDrops(townHall), "A pickaxe should harvest the Town Hall");
        helper.assertFalse(new ItemStack(Items.IRON_AXE).isCorrectToolForDrops(townHall), "An axe should not harvest the Town Hall");
        helper.succeed();
    }

    // --- Settlements ---

    private static final BlockPos TOWN_HALL_A = new BlockPos(1, 2, 1);
    private static final BlockPos TOWN_HALL_B = new BlockPos(3, 2, 3);

    private static TownHallBlockEntity placeTownHall(GameTestHelper helper, BlockPos pos)
    {
        helper.setBlock(pos, MinecraftMod.TOWN_HALL.get());
        if (!(helper.getBlockEntity(pos) instanceof TownHallBlockEntity townHall))
            throw new IllegalStateException("Town Hall at " + pos + " has no block entity");
        return townHall;
    }

    private static SettlementData settlements(GameTestHelper helper)
    {
        return SettlementData.get(helper.getLevel().getServer());
    }

    @GameTest(template = PLATFORM, batch = "settlementCanBeFoundedAtTownHall")
    public static void settlementCanBeFoundedAtTownHall(GameTestHelper helper)
    {
        clearSettlementsAndSites(helper);
        TownHallBlockEntity townHall = placeTownHall(helper, TOWN_HALL_A);
        Player player = helper.makeMockPlayer();

        Settlements.CreateResult result = Settlements.tryCreate(player, townHall, "Rivertown");

        helper.assertTrue(result.isSuccess(), "Founding should succeed, got: " + result.error());
        helper.assertTrue(result.settlement().id().equals(townHall.getSettlementId()), "Town Hall should point at the new settlement");
        helper.assertTrue(settlements(helper).getByLeader(player.getUUID()).map(s -> s.name().equals("Rivertown")).orElse(false),
                "Player should lead Rivertown");
        helper.assertTrue(result.settlement().townHallPos().equals(townHall.getBlockPos()), "Settlement should remember its Town Hall");
        helper.succeed();
    }

    @GameTest(template = PLATFORM, batch = "playerCanOnlyLeadOneSettlement")
    public static void playerCanOnlyLeadOneSettlement(GameTestHelper helper)
    {
        clearSettlementsAndSites(helper);
        TownHallBlockEntity first = placeTownHall(helper, TOWN_HALL_A);
        TownHallBlockEntity second = placeTownHall(helper, TOWN_HALL_B);
        Player player = helper.makeMockPlayer();

        helper.assertTrue(Settlements.tryCreate(player, first, "Rivertown").isSuccess(), "First settlement should succeed");
        Settlements.CreateResult again = Settlements.tryCreate(player, second, "Hilltop");

        helper.assertFalse(again.isSuccess(), "A player must not found a second settlement");
        helper.assertTrue(second.getSettlementId() == null, "The second Town Hall should stay free");
        helper.succeed();
    }

    @GameTest(template = PLATFORM, batch = "townHallHostsOnlyOneSettlement")
    public static void townHallHostsOnlyOneSettlement(GameTestHelper helper)
    {
        clearSettlementsAndSites(helper);
        TownHallBlockEntity townHall = placeTownHall(helper, TOWN_HALL_A);
        Player founder = helper.makeMockPlayer();
        Player other = helper.makeMockPlayer();

        helper.assertTrue(Settlements.tryCreate(founder, townHall, "Rivertown").isSuccess(), "First settlement should succeed");
        Settlements.CreateResult taken = Settlements.tryCreate(other, townHall, "Hilltop");

        helper.assertFalse(taken.isSuccess(), "A Town Hall must not host a second settlement");
        helper.assertTrue(settlements(helper).getByLeader(other.getUUID()).isEmpty(), "The other player should not lead anything");
        helper.succeed();
    }

    @GameTest(template = PLATFORM, batch = "settlementNameIsValidated")
    public static void settlementNameIsValidated(GameTestHelper helper)
    {
        clearSettlementsAndSites(helper);
        TownHallBlockEntity townHall = placeTownHall(helper, TOWN_HALL_A);
        Player player = helper.makeMockPlayer();

        helper.assertFalse(Settlements.tryCreate(player, townHall, "   ").isSuccess(), "Blank names must be rejected");
        helper.assertFalse(Settlements.tryCreate(player, townHall, "x".repeat(Settlements.MAX_NAME_LENGTH + 1)).isSuccess(), "Too long names must be rejected");
        helper.assertTrue(townHall.getSettlementId() == null, "Rejected names must not create a settlement");

        Settlements.CreateResult trimmed = Settlements.tryCreate(player, townHall, "  Rivertown  ");
        helper.assertTrue(trimmed.isSuccess() && trimmed.settlement().name().equals("Rivertown"), "Names should be trimmed, got: " + trimmed);
        helper.succeed();
    }

    @GameTest(template = PLATFORM, batch = "removingTownHallKeepsSettlement")
    public static void removingTownHallKeepsSettlement(GameTestHelper helper)
    {
        clearSettlementsAndSites(helper);
        TownHallBlockEntity townHall = placeTownHall(helper, TOWN_HALL_A);
        Player player = helper.makeMockPlayer();
        helper.assertTrue(Settlements.tryCreate(player, townHall, "Rivertown").isSuccess(), "Founding should succeed");

        helper.destroyBlock(TOWN_HALL_A);

        Settlement settlement = settlements(helper).getByLeader(player.getUUID()).orElse(null);
        helper.assertTrue(settlement != null && settlement.name().equals("Rivertown"), "Settlement should survive losing its Town Hall");
        helper.assertTrue(settlement.townHallPos() == null, "Settlement should no longer have a Town Hall");
        helper.assertTrue(settlement.settlers().size() == Config.settlersOnFounding, "Settlers should stay in the settlement");
        helper.assertFalse(Settlements.tryCreate(player, placeTownHall(helper, TOWN_HALL_B), "Hilltop").isSuccess(),
                "The leader still leads Rivertown, so can't found another settlement");
        helper.succeed();
    }

    @GameTest(template = PLATFORM, batch = "townHallRemembersSettlementAfterSaveAndLoad")
    public static void townHallRemembersSettlementAfterSaveAndLoad(GameTestHelper helper)
    {
        clearSettlementsAndSites(helper);
        TownHallBlockEntity townHall = placeTownHall(helper, TOWN_HALL_A);
        helper.assertTrue(Settlements.tryCreate(helper.makeMockPlayer(), townHall, "Rivertown").isSuccess(), "Founding should succeed");

        CompoundTag saved = townHall.saveWithoutMetadata();
        TownHallBlockEntity loaded = new TownHallBlockEntity(townHall.getBlockPos(), townHall.getBlockState());
        loaded.load(saved);

        helper.assertTrue(townHall.getSettlementId().equals(loaded.getSettlementId()), "Settlement id should survive save and load");
        helper.succeed();
    }

    @GameTest(template = PLATFORM, batch = "settlementMovesToNewTownHall")
    public static void settlementMovesToNewTownHall(GameTestHelper helper)
    {
        clearSettlementsAndSites(helper);
        Player leader = helper.makeMockPlayer();
        helper.assertTrue(Settlements.tryCreate(leader, placeTownHall(helper, TOWN_HALL_A), "Rivertown").isSuccess(), "Founding should succeed");
        TownHallBlockEntity newTownHall = placeTownHall(helper, TOWN_HALL_B);
        helper.assertFalse(Settlements.tryMoveTo(leader, newTownHall).isSuccess(), "A settlement that still has its Town Hall must not move");

        helper.destroyBlock(TOWN_HALL_A);
        Settlements.CreateResult moved = Settlements.tryMoveTo(leader, newTownHall);

        helper.assertTrue(moved.isSuccess(), "Moving to a free Town Hall should succeed, got: " + moved.error());
        Settlement settlement = moved.settlement();
        helper.assertTrue(newTownHall.getBlockPos().equals(settlement.townHallPos()), "The settlement should have its new Town Hall");
        helper.assertTrue(settlement.id().equals(newTownHall.getSettlementId()), "The new Town Hall should point at the settlement");
        helper.assertTrue(settlement.settlers().size() == Config.settlersOnFounding, "The settlers should stay in the settlement");
        helper.assertTrue(Territory.contains(settlement, newTownHall.getBlockPos()), "The territory should be around the new Town Hall");
        helper.succeed();
    }

    @GameTest(template = PLATFORM, batch = "settlementCannotMoveToTakenTownHallOrForeignTerritory")
    public static void settlementCannotMoveToTakenTownHallOrForeignTerritory(GameTestHelper helper)
    {
        clearSettlementsAndSites(helper);
        Player leader = helper.makeMockPlayer();
        helper.assertTrue(Settlements.tryCreate(leader, placeTownHall(helper, TOWN_HALL_A), "Rivertown").isSuccess(), "Founding should succeed");
        helper.destroyBlock(TOWN_HALL_A);
        // Another settlement is founded right next to where Rivertown's Town Hall was
        TownHallBlockEntity taken = placeTownHall(helper, TOWN_HALL_B);
        helper.assertTrue(Settlements.tryCreate(helper.makeMockPlayer(), taken, "Hilltop").isSuccess(), "Founding Hilltop should succeed");

        helper.assertFalse(Settlements.tryMoveTo(leader, taken).isSuccess(), "A Town Hall of another settlement must not be taken over");
        TownHallBlockEntity inForeignTerritory = placeTownHall(helper, TOWN_HALL_A);
        helper.assertFalse(Settlements.tryMoveTo(leader, inForeignTerritory).isSuccess(), "Moving into another settlement's territory must be rejected");
        helper.assertTrue(inForeignTerritory.getSettlementId() == null, "The rejected Town Hall should stay free");
        helper.assertFalse(Settlements.tryMoveTo(helper.makeMockPlayer(), inForeignTerritory).isSuccess(), "A player without a settlement can't move one");
        helper.succeed();
    }

    // --- Founding settlers ---

    @GameTest(template = PLATFORM, batch = "foundingSpawnsConfiguredSettlers")
    public static void foundingSpawnsConfiguredSettlers(GameTestHelper helper)
    {
        clearSettlementsAndSites(helper);
        Settlement settlement = foundSettlement(helper, helper.makeMockPlayer());

        helper.assertTrue(settlement.settlers().size() == Config.settlersOnFounding,
                "Expected " + Config.settlersOnFounding + " settlers in the settlement, got " + settlement.settlers().size());
        for (SettlerRecord record : settlement.settlers())
        {
            if (!(helper.getLevel().getEntity(record.entityId()) instanceof SettlerEntity settler))
                throw new IllegalStateException("No settler entity for " + record.fullName());
            helper.assertTrue(settlement.id().equals(settler.getSettlementId()), record.fullName() + " should belong to the settlement");
            helper.assertTrue(settler.getFullName().equals(record.fullName()), "Settlement should know the settler's name " + settler.getFullName());
            helper.assertTrue(record.job() == null, record.fullName() + " should start without a job");
        }
        helper.succeed();
    }

    @GameTest(template = PLATFORM, batch = "deadSettlerLeavesSettlement")
    public static void deadSettlerLeavesSettlement(GameTestHelper helper)
    {
        clearSettlementsAndSites(helper);
        Settlement settlement = foundSettlement(helper, helper.makeMockPlayer());
        UUID victim = firstSettlers(helper, settlement, 1).get(0);

        helper.getLevel().getEntity(victim).kill();

        helper.assertTrue(settlement.settler(victim).isEmpty(), "A dead settler should leave the settlement");
        helper.assertTrue(settlement.settlers().size() == Config.settlersOnFounding - 1, "The other settlers should stay");
        helper.succeed();
    }

    // --- Workplaces (Builder's Hut) ---

    // --- Settler inventory ---

    private static SettlerEntity spawnSettler(GameTestHelper helper)
    {
        return helper.spawn(MinecraftMod.SETTLER.get(), ABOVE_FLOOR);
    }

    @GameTest(template = PLATFORM)
    public static void settlerInventorySurvivesSaveAndLoad(GameTestHelper helper)
    {
        SettlerEntity settler = spawnSettler(helper);
        settler.getStorage().setItem(5, new ItemStack(Items.DIRT, 12));
        settler.getStorage().setItem(SettlerEntity.STORAGE_SIZE - 1, new ItemStack(Items.BREAD, 3));
        settler.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));

        CompoundTag saved = settler.saveWithoutId(new CompoundTag());
        settler.discard();
        SettlerEntity loaded = MinecraftMod.SETTLER.get().create(helper.getLevel());
        loaded.load(saved);

        helper.assertTrue(ItemStack.matches(loaded.getStorage().getItem(5), new ItemStack(Items.DIRT, 12)), "Storage slot 5 should keep its dirt");
        helper.assertTrue(ItemStack.matches(loaded.getStorage().getItem(SettlerEntity.STORAGE_SIZE - 1), new ItemStack(Items.BREAD, 3)),
                "The last storage slot should keep its bread");
        helper.assertTrue(loaded.getStorage().getItem(0).isEmpty(), "Items must stay in their slots, not move to the front");
        helper.assertTrue(loaded.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "The helmet should still be worn");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void settlerInventoryMenuEquipsArmorAndStoresItems(GameTestHelper helper)
    {
        SettlerEntity settler = spawnSettler(helper);
        try
        {
            Player player = helper.makeMockPlayer();
            SettlerInventoryMenu menu = SettlerInventoryMenu.forSettler(0, player.getInventory(), settler);
            // The menu's first player slot is the player's inventory slot 9 (the top row above the hotbar)
            player.getInventory().setItem(9, new ItemStack(Items.IRON_CHESTPLATE));
            player.getInventory().setItem(10, new ItemStack(Items.DIRT, 64));

            menu.quickMoveStack(player, SettlerInventoryMenu.PLAYER_START);
            menu.quickMoveStack(player, SettlerInventoryMenu.PLAYER_START + 1);

            helper.assertTrue(settler.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE), "Shift-clicking a chestplate should put it on the settler");
            helper.assertTrue(ItemStack.matches(settler.getStorage().getItem(0), new ItemStack(Items.DIRT, 64)), "Shift-clicking dirt should put it in storage");
            helper.assertTrue(player.getInventory().getItem(9).isEmpty() && player.getInventory().getItem(10).isEmpty(),
                    "Both items should have left the player's inventory");
            helper.assertFalse(menu.getSlot(SettlerInventoryMenu.ARMOR_START).mayPlace(new ItemStack(Items.DIRT)), "Armor slots only take armor");
            helper.assertFalse(menu.getSlot(SettlerInventoryMenu.ARMOR_START).mayPlace(new ItemStack(Items.IRON_BOOTS)), "The head slot must not take boots");

            menu.quickMoveStack(player, SettlerInventoryMenu.ARMOR_START + 1);
            helper.assertTrue(settler.getItemBySlot(EquipmentSlot.CHEST).isEmpty(), "Shift-clicking the worn chestplate should take it off");
            helper.assertTrue(player.getInventory().contains(new ItemStack(Items.IRON_CHESTPLATE)), "The chestplate should go back to the player");
        }
        finally
        {
            settler.discard();
        }
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void deadSettlerDropsInventory(GameTestHelper helper)
    {
        SettlerEntity settler = spawnSettler(helper);
        settler.getStorage().setItem(7, new ItemStack(Items.BREAD, 5));
        settler.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));

        settler.kill();

        helper.assertItemEntityPresent(Items.BREAD, ABOVE_FLOOR, 3.0);
        helper.assertItemEntityPresent(Items.IRON_BOOTS, ABOVE_FLOOR, 3.0);
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(ABOVE_FLOOR)).inflate(3.0)).forEach(Entity::discard);
        helper.succeed();
    }

    @GameTest(template = PLATFORM, batch = "onlyLeaderOpensSettlerInventory")
    public static void onlyLeaderOpensSettlerInventory(GameTestHelper helper)
    {
        clearSettlementsAndSites(helper);
        Player leader = helper.makeMockPlayer();
        Settlement settlement = foundSettlement(helper, leader);
        if (!(helper.getLevel().getEntity(firstSettlers(helper, settlement, 1).get(0)) instanceof SettlerEntity member))
            throw new IllegalStateException("The founding settler should exist");
        SettlerEntity loner = spawnSettler(helper);
        Player stranger = helper.makeMockPlayer();

        helper.assertTrue(member.canOpenInventory(leader), "The leader may open their settler's inventory");
        helper.assertFalse(member.canOpenInventory(stranger), "Other players must not open a settlement's settler");
        helper.assertTrue(loner.canOpenInventory(stranger), "A settler without a settlement is open to anyone");
        loner.discard();
        helper.succeed();
    }

    private static final BlockPos HUT_A = new BlockPos(3, 2, 1);
    private static final BlockPos HUT_B = new BlockPos(1, 2, 3);

    private static Settlement foundSettlement(GameTestHelper helper, Player leader)
    {
        Settlements.CreateResult result = Settlements.tryCreate(leader, placeTownHall(helper, TOWN_HALL_A), "Rivertown");
        if (!result.isSuccess())
            throw new IllegalStateException("Founding failed: " + result.error().getString());
        return result.settlement();
    }

    private static List<UUID> firstSettlers(GameTestHelper helper, Settlement settlement, int count)
    {
        helper.assertTrue(settlement.settlers().size() >= count, "This test needs settlersOnFounding >= " + count);
        return settlement.settlers().stream().limit(count).map(SettlerRecord::entityId).toList();
    }

    private static BuildersHutBlockEntity placeClaimedHut(GameTestHelper helper, BlockPos pos, Player leader)
    {
        helper.setBlock(pos, MinecraftMod.BUILDERS_HUT.get());
        if (!(helper.getBlockEntity(pos) instanceof BuildersHutBlockEntity hut))
            throw new IllegalStateException("Builder's Hut at " + pos + " has no block entity");
        Settlements.claimWorkplace(helper.getLevel(), leader, hut);
        return hut;
    }

    private static SettlerJob jobOf(Settlement settlement, UUID settler)
    {
        return settlement.settler(settler).map(SettlerRecord::job).orElse(null);
    }

    @GameTest(template = PLATFORM, batch = "buildersHutIsClaimedByLeadersSettlement")
    public static void buildersHutIsClaimedByLeadersSettlement(GameTestHelper helper)
    {
        clearSettlementsAndSites(helper);
        Player leader = helper.makeMockPlayer();
        Settlement settlement = foundSettlement(helper, leader);

        BuildersHutBlockEntity claimed = placeClaimedHut(helper, HUT_A, leader);
        BuildersHutBlockEntity unclaimed = placeClaimedHut(helper, HUT_B, helper.makeMockPlayer());

        helper.assertTrue(settlement.id().equals(claimed.getSettlementId()), "The leader's hut should join their settlement");
        helper.assertTrue(unclaimed.getSettlementId() == null, "A player without a settlement can't claim a hut");
        helper.succeed();
    }

    @GameTest(template = PLATFORM, batch = "buildersHutAssignsOneBuilder")
    public static void buildersHutAssignsOneBuilder(GameTestHelper helper)
    {
        clearSettlementsAndSites(helper);
        Player leader = helper.makeMockPlayer();
        Settlement settlement = foundSettlement(helper, leader);
        BuildersHutBlockEntity hut = placeClaimedHut(helper, HUT_A, leader);
        List<UUID> settlers = firstSettlers(helper, settlement, 2);

        helper.assertTrue(Settlements.assignWorker(leader, hut, settlers.get(0)).isSuccess(), "Assigning should succeed");
        helper.assertTrue(jobOf(settlement, settlers.get(0)) == SettlerJob.BUILDER, "First settler should be the builder");
        helper.assertTrue(settlers.get(0).equals(settlement.workerAt(hut.getBlockPos()).map(SettlerRecord::entityId).orElse(null)),
                "The hut's worker should be the first settler");

        helper.assertTrue(Settlements.assignWorker(leader, hut, settlers.get(1)).isSuccess(), "Reassigning should succeed");
        helper.assertTrue(jobOf(settlement, settlers.get(0)) == null, "The previous builder should be unemployed");
        helper.assertTrue(jobOf(settlement, settlers.get(1)) == SettlerJob.BUILDER, "Second settler should be the builder");

        helper.assertTrue(Settlements.assignWorker(leader, hut, null).isSuccess(), "Unassigning should succeed");
        helper.assertTrue(settlement.workerAt(hut.getBlockPos()).isEmpty(), "The hut should have no worker");
        helper.assertTrue(jobOf(settlement, settlers.get(1)) == null, "The unassigned builder should be unemployed");
        helper.succeed();
    }

    @GameTest(template = PLATFORM, batch = "settlerHoldsOnlyOneJob")
    public static void settlerHoldsOnlyOneJob(GameTestHelper helper)
    {
        clearSettlementsAndSites(helper);
        Player leader = helper.makeMockPlayer();
        Settlement settlement = foundSettlement(helper, leader);
        BuildersHutBlockEntity first = placeClaimedHut(helper, HUT_A, leader);
        BuildersHutBlockEntity second = placeClaimedHut(helper, HUT_B, leader);
        UUID settler = firstSettlers(helper, settlement, 1).get(0);

        Settlements.assignWorker(leader, first, settler);
        Settlements.assignWorker(leader, second, settler);

        helper.assertTrue(settlement.workerAt(first.getBlockPos()).isEmpty(), "Moving the settler should free the first hut");
        helper.assertTrue(settlement.settler(settler).map(record -> record.worksAt(second.getBlockPos())).orElse(false),
                "The settler should now work at the second hut");
        helper.succeed();
    }

    @GameTest(template = PLATFORM, batch = "onlyLeaderCanAssignWorkers")
    public static void onlyLeaderCanAssignWorkers(GameTestHelper helper)
    {
        clearSettlementsAndSites(helper);
        Player leader = helper.makeMockPlayer();
        Settlement settlement = foundSettlement(helper, leader);
        BuildersHutBlockEntity hut = placeClaimedHut(helper, HUT_A, leader);
        UUID settler = firstSettlers(helper, settlement, 1).get(0);

        helper.assertFalse(Settlements.assignWorker(helper.makeMockPlayer(), hut, settler).isSuccess(), "Other players must not assign jobs");
        helper.assertTrue(jobOf(settlement, settler) == null, "The settler should still be unemployed");
        helper.succeed();
    }

    @GameTest(template = PLATFORM, batch = "cannotAssignSettlerFromOtherSettlement")
    public static void cannotAssignSettlerFromOtherSettlement(GameTestHelper helper)
    {
        clearSettlementsAndSites(helper);
        Player leader = helper.makeMockPlayer();
        foundSettlement(helper, leader);
        BuildersHutBlockEntity hut = placeClaimedHut(helper, HUT_A, leader);

        helper.assertFalse(Settlements.assignWorker(leader, hut, UUID.randomUUID()).isSuccess(), "Unknown settlers must be rejected");
        helper.succeed();
    }

    @GameTest(template = PLATFORM, batch = "removingBuildersHutUnassignsBuilder")
    public static void removingBuildersHutUnassignsBuilder(GameTestHelper helper)
    {
        clearSettlementsAndSites(helper);
        Player leader = helper.makeMockPlayer();
        Settlement settlement = foundSettlement(helper, leader);
        BuildersHutBlockEntity hut = placeClaimedHut(helper, HUT_A, leader);
        UUID settler = firstSettlers(helper, settlement, 1).get(0);
        Settlements.assignWorker(leader, hut, settler);

        helper.destroyBlock(HUT_A);

        helper.assertTrue(jobOf(settlement, settler) == null, "The builder of a removed hut should be unemployed");
        helper.assertTrue(settlement.settler(settler).isPresent(), "The settler should stay in the settlement");
        helper.succeed();
    }

    // --- Blueprints ---

    // data/minecraftmod/structures/blueprint_platform.nbt: 12x5x5 with a stone floor at relative y=1;
    // the source structure goes at x=1..3, copies are placed at x=7..9
    private static final String BLUEPRINT_PLATFORM = "blueprint_platform";
    private static final BlockPos SOURCE_MIN = new BlockPos(1, 2, 1);
    private static final BlockPos SOURCE_MAX = new BlockPos(3, 4, 3);
    private static final BlockPos COPY_MIN = new BlockPos(7, 2, 1);

    private static BlueprintSelection sourceSelection(GameTestHelper helper)
    {
        return BlueprintSelection.of(helper.absolutePos(SOURCE_MIN), helper.absolutePos(SOURCE_MAX));
    }

    // A small structure around a Town Hall that still remembers a settlement
    private static void buildSourceStructure(GameTestHelper helper)
    {
        helper.setBlock(new BlockPos(1, 2, 1), Blocks.STONE_BRICKS);
        helper.setBlock(new BlockPos(3, 3, 3), Blocks.OAK_PLANKS);
        helper.setBlock(new BlockPos(2, 4, 2), Blocks.GLASS);
        placeTownHall(helper, new BlockPos(2, 2, 2)).setSettlementId(UUID.randomUUID());
    }

    private static String uniqueBlueprintName()
    {
        return "gametest_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private static void deleteBlueprint(GameTestHelper helper, Blueprints.SaveResult result)
    {
        if (result.isSuccess())
        {
            try
            {
                Files.deleteIfExists(result.file());
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        }
    }

    private static void assertCopyMatchesSource(GameTestHelper helper)
    {
        for (BlockPos offset : BlockPos.betweenClosed(BlockPos.ZERO, SOURCE_MAX.subtract(SOURCE_MIN)))
        {
            BlockState source = helper.getBlockState(SOURCE_MIN.offset(offset));
            BlockState copy = helper.getBlockState(COPY_MIN.offset(offset));
            helper.assertTrue(source.equals(copy), "Copy differs at offset " + offset + ": expected " + source + ", got " + copy);
        }
    }

    @GameTest(template = BLUEPRINT_PLATFORM)
    public static void blueprintAreaNeedsExactlyOneCityBlock(GameTestHelper helper)
    {
        helper.assertFalse(Blueprints.analyze(helper.getLevel(), sourceSelection(helper)).isValid(), "An area without a city block must be rejected");

        placeTownHall(helper, new BlockPos(2, 2, 2));
        Blueprints.Analysis one = Blueprints.analyze(helper.getLevel(), sourceSelection(helper));
        helper.assertTrue(one.isValid(), "An area with one city block should be accepted, got: " + one.error());
        helper.assertTrue(one.cityBlock() == MinecraftMod.TOWN_HALL.get(), "The city block should be the Town Hall");
        helper.assertTrue(one.anchor().equals(new BlockPos(1, 0, 1)), "The anchor should be relative to the area's min corner, got " + one.anchor());

        helper.setBlock(new BlockPos(1, 2, 1), MinecraftMod.BUILDERS_HUT.get());
        helper.assertFalse(Blueprints.analyze(helper.getLevel(), sourceSelection(helper)).isValid(), "An area with two city blocks must be rejected");
        helper.succeed();
    }

    @GameTest(template = BLUEPRINT_PLATFORM)
    public static void blueprintAreaHasSizeLimit(GameTestHelper helper)
    {
        BlockPos min = helper.absolutePos(SOURCE_MIN);
        BlueprintSelection tooLarge = BlueprintSelection.of(min, min.offset(Blueprints.MAX_SIZE, 0, 0));
        BlueprintSelection largest = BlueprintSelection.of(min, min.offset(Blueprints.MAX_SIZE - 1, 0, 0));

        helper.assertFalse(tooLarge.fitsMaxSize(), "Areas wider than " + Blueprints.MAX_SIZE + " must not fit");
        helper.assertTrue(largest.fitsMaxSize(), "Areas exactly " + Blueprints.MAX_SIZE + " wide should fit");
        helper.assertFalse(Blueprints.analyze(helper.getLevel(), tooLarge).isValid(), "Too large areas must be rejected");
        helper.succeed();
    }

    @GameTest(template = BLUEPRINT_PLATFORM)
    public static void blueprintNameIsValidated(GameTestHelper helper)
    {
        buildSourceStructure(helper);
        for (String name : List.of("", "Big House", "../escape", "x".repeat(Blueprints.MAX_NAME_LENGTH + 1)))
        {
            Blueprints.SaveResult result = Blueprints.save(helper.getLevel(), sourceSelection(helper), name, "gametest");
            deleteBlueprint(helper, result);
            helper.assertFalse(result.isSuccess(), "Name '" + name + "' must be rejected");
        }
        helper.succeed();
    }

    @GameTest(template = BLUEPRINT_PLATFORM)
    public static void blueprintSavesLoadsAndPlaces(GameTestHelper helper)
    {
        buildSourceStructure(helper);
        String name = uniqueBlueprintName();
        Blueprints.SaveResult saved = Blueprints.save(helper.getLevel(), sourceSelection(helper), name, "gametest");
        try
        {
            helper.assertTrue(saved.isSuccess(), "Saving should succeed, got: " + saved.error());
            helper.assertTrue(saved.id().equals(Blueprints.idFor(MinecraftMod.TOWN_HALL.get(), name)), "Unexpected id " + saved.id());
            helper.assertTrue(Files.exists(saved.file()), "The blueprint file should exist at " + saved.file());
            helper.assertFalse(Blueprints.save(helper.getLevel(), sourceSelection(helper), name, "gametest").isSuccess(),
                    "Saving the same name twice must be rejected");

            Blueprint blueprint = Blueprints.load(helper.getLevel().getServer(), saved.id()).orElse(null);
            helper.assertTrue(blueprint != null, "The saved blueprint should load");
            helper.assertTrue(blueprint.cityBlock() == MinecraftMod.TOWN_HALL.get(), "Loaded city block should be the Town Hall");
            helper.assertTrue(blueprint.anchor().equals(new BlockPos(1, 0, 1)), "Loaded anchor should be 1 0 1, got " + blueprint.anchor());
            helper.assertTrue(blueprint.template().getSize().equals(new Vec3i(3, 3, 3)), "Loaded size should be 3x3x3");
            helper.assertTrue(Blueprints.list(helper.getLevel().getServer()).stream().anyMatch(entry -> entry.id().equals(saved.id()) && entry.inWorld()),
                    "The saved blueprint should be listed");

            Blueprints.place(helper.getLevel(), blueprint, helper.absolutePos(COPY_MIN), Rotation.NONE);
            assertCopyMatchesSource(helper);
            if (!(helper.getBlockEntity(COPY_MIN.offset(1, 0, 1)) instanceof TownHallBlockEntity copiedTownHall))
                throw new IllegalStateException("The copy should contain a Town Hall with its block entity");
            helper.assertTrue(copiedTownHall.getSettlementId() == null, "Blueprints must not copy the settlement of the captured Town Hall");
        }
        finally
        {
            deleteBlueprint(helper, saved);
        }
        helper.succeed();
    }

    @GameTest(template = BLUEPRINT_PLATFORM)
    public static void blueprintLoadCommandPlacesBlueprint(GameTestHelper helper)
    {
        buildSourceStructure(helper);
        Blueprints.SaveResult saved = Blueprints.save(helper.getLevel(), sourceSelection(helper), uniqueBlueprintName(), "gametest");
        try
        {
            helper.assertTrue(saved.isSuccess(), "Saving should succeed, got: " + saved.error());
            MinecraftServer server = helper.getLevel().getServer();
            CommandSourceStack source = server.createCommandSourceStack().withLevel(helper.getLevel()).withSuppressedOutput();
            BlockPos at = helper.absolutePos(COPY_MIN);

            int result = server.getCommands().performPrefixedCommand(source,
                    "blueprint load " + saved.id() + " " + at.getX() + " " + at.getY() + " " + at.getZ());

            helper.assertTrue(result == 1, "/blueprint load should succeed, returned " + result);
            assertCopyMatchesSource(helper);
            helper.assertTrue(server.getCommands().performPrefixedCommand(source, "blueprint list") >= 1, "/blueprint list should list it");
        }
        finally
        {
            deleteBlueprint(helper, saved);
        }
        helper.succeed();
    }

    @GameTest(template = BLUEPRINT_PLATFORM)
    public static void blueprintLoadCommandRotatesBlueprint(GameTestHelper helper)
    {
        buildSourceStructure(helper);
        Blueprints.SaveResult saved = Blueprints.save(helper.getLevel(), sourceSelection(helper), uniqueBlueprintName(), "gametest");
        try
        {
            helper.assertTrue(saved.isSuccess(), "Saving should succeed, got: " + saved.error());
            MinecraftServer server = helper.getLevel().getServer();
            CommandSourceStack source = server.createCommandSourceStack().withLevel(helper.getLevel()).withSuppressedOutput();
            BlockPos at = helper.absolutePos(COPY_MIN);

            int result = server.getCommands().performPrefixedCommand(source,
                    "blueprint load " + saved.id() + " " + at.getX() + " " + at.getY() + " " + at.getZ() + " clockwise_90");

            // A quarter turn clockwise moves (x, y, z) inside the 3x3x3 area to (2 - z, y, x); the area still starts at COPY_MIN
            helper.assertTrue(result == 1, "/blueprint load with a rotation should succeed, returned " + result);
            helper.assertBlockPresent(Blocks.STONE_BRICKS, COPY_MIN.offset(2, 0, 0));
            helper.assertBlockPresent(Blocks.OAK_PLANKS, COPY_MIN.offset(0, 1, 2));
            helper.assertBlockPresent(MinecraftMod.TOWN_HALL.get(), COPY_MIN.offset(1, 0, 1));
            helper.assertBlockPresent(Blocks.AIR, COPY_MIN);
        }
        finally
        {
            deleteBlueprint(helper, saved);
        }
        helper.succeed();
    }

    // --- Construction sites (Builder's Wand) ---

    // Where placed blueprints go in BLUEPRINT_PLATFORM: the source structure's anchor is at 1 0 1 inside its 3x3x3,
    // so a city block at 8 2 2 makes the site cover x=7..9 like COPY_MIN
    private static final BlockPos SITE_CITY_BLOCK = new BlockPos(8, 2, 2);

    // Saves the source structure as a blueprint, runs the test, then deletes the file and any sites the test made
    private static void withSourceBlueprint(GameTestHelper helper, java.util.function.Consumer<Blueprint> test)
    {
        buildSourceStructure(helper);
        Blueprints.SaveResult saved = Blueprints.save(helper.getLevel(), sourceSelection(helper), uniqueBlueprintName(), "gametest");
        try
        {
            helper.assertTrue(saved.isSuccess(), "Saving should succeed, got: " + saved.error());
            test.accept(Blueprints.load(helper.getLevel().getServer(), saved.id()).orElseThrow());
        }
        finally
        {
            deleteBlueprint(helper, saved);
            ConstructionSiteData sites = ConstructionSiteData.get(helper.getLevel().getServer());
            BlueprintSelection platform = BlueprintSelection.of(helper.absolutePos(BlockPos.ZERO), helper.absolutePos(new BlockPos(11, 5, 4)));
            sites.in(helper.getLevel().dimension()).stream().filter(site -> site.area().intersects(platform)).forEach(site -> sites.remove(site.id()));
        }
    }

    // A player in survival mode, unlike GameTestHelper.makeMockPlayer() which is in creative
    private static Player makeSurvivalPlayer(GameTestHelper helper)
    {
        return new Player(helper.getLevel(), BlockPos.ZERO, 0.0f, new GameProfile(UUID.randomUUID(), "survival-player"))
        {
            @Override
            public boolean isSpectator()
            {
                return false;
            }

            @Override
            public boolean isCreative()
            {
                return false;
            }
        };
    }

    @GameTest(template = BLUEPRINT_PLATFORM)
    public static void placementKeepsCityBlockOnTargetWhenRotated(GameTestHelper helper)
    {
        BlockPos target = new BlockPos(100, 64, 100);
        // An off-centre anchor in a 5x2x3 blueprint, so rotation visibly moves the area
        for (Rotation rotation : Rotation.values())
        {
            BlueprintPlacement placement = new BlueprintPlacement(target, new BlockPos(1, 0, 0), new Vec3i(5, 2, 3), rotation);
            helper.assertTrue(placement.worldPos(new BlockPos(1, 0, 0)).equals(target), rotation + ": the city block should land on the target");
            Vec3i size = placement.area().size();
            boolean quarterTurn = rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90;
            helper.assertTrue(size.equals(quarterTurn ? new Vec3i(3, 2, 5) : new Vec3i(5, 2, 3)), rotation + ": unexpected area size " + size);
        }
        BlueprintSelection unrotated = new BlueprintPlacement(target, new BlockPos(1, 0, 0), new Vec3i(5, 2, 3), Rotation.NONE).area();
        helper.assertTrue(unrotated.min().equals(target.offset(-1, 0, 0)) && unrotated.max().equals(target.offset(3, 1, 2)),
                "Unrotated area should start one block before the city block, got " + unrotated);
        helper.succeed();
    }

    @GameTest(template = BLUEPRINT_PLATFORM, batch = "blueprintBlocksSurviveNetworkRoundTrip")
    public static void blueprintBlocksSurviveNetworkRoundTrip(GameTestHelper helper)
    {
        clearSettlementsAndSites(helper);
        withSourceBlueprint(helper, blueprint -> {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            blueprint.blocks().write(buf);
            BlueprintBlocks read = BlueprintBlocks.read(buf);

            helper.assertTrue(read.equals(blueprint.blocks()), "Blueprint blocks should be the same after sending them");
            helper.assertTrue(read.blocks().size() == 4, "Expected the 4 non-air blocks, got " + read.blocks().size());
            helper.assertTrue(read.blocks().get(new BlockPos(1, 0, 1)).is(MinecraftMod.TOWN_HALL.get()), "The anchor should hold the Town Hall");
        });
        helper.succeed();
    }

    @GameTest(template = BLUEPRINT_PLATFORM, batch = "placingBlueprintOnlyPlacesCityBlock")
    public static void placingBlueprintOnlyPlacesCityBlock(GameTestHelper helper)
    {
        clearSettlementsAndSites(helper);
        withSourceBlueprint(helper, blueprint -> {
            BlockPos target = helper.absolutePos(SITE_CITY_BLOCK);
            ConstructionSites.Result result = ConstructionSites.place(helper.makeMockPlayer(), helper.getLevel(), blueprint, target, Rotation.NONE);

            helper.assertTrue(result.isSuccess(), "Placing should succeed, got: " + result.error());
            helper.assertBlockPresent(MinecraftMod.TOWN_HALL.get(), SITE_CITY_BLOCK);
            for (BlockPos offset : BlockPos.betweenClosed(BlockPos.ZERO, new BlockPos(2, 2, 2)))
            {
                BlockPos pos = COPY_MIN.offset(offset);
                if (!pos.equals(SITE_CITY_BLOCK))
                    helper.assertBlockPresent(Blocks.AIR, pos);
            }
            ConstructionSite site = result.site();
            helper.assertTrue(site.blueprint().equals(blueprint.id()), "The site should remember its blueprint");
            helper.assertTrue(site.area().equals(BlueprintSelection.of(helper.absolutePos(COPY_MIN), helper.absolutePos(COPY_MIN.offset(2, 2, 2)))),
                    "The site should cover the blueprint's area, got " + site.area());
            helper.assertTrue(ConstructionSites.atCityBlock(helper.getLevel(), target).isPresent(), "The site should be stored");
            if (!(helper.getBlockEntity(SITE_CITY_BLOCK) instanceof TownHallBlockEntity townHall) || townHall.getSettlementId() != null)
                throw new IllegalStateException("The placed Town Hall should be new, without a settlement");
        });
        helper.succeed();
    }

    @GameTest(template = BLUEPRINT_PLATFORM, batch = "placingBlueprintCostsCityBlockInSurvival")
    public static void placingBlueprintCostsCityBlockInSurvival(GameTestHelper helper)
    {
        clearSettlementsAndSites(helper);
        withSourceBlueprint(helper, blueprint -> {
            Player player = makeSurvivalPlayer(helper);
            BlockPos target = helper.absolutePos(SITE_CITY_BLOCK);

            helper.assertFalse(ConstructionSites.place(player, helper.getLevel(), blueprint, target, Rotation.NONE).isSuccess(),
                    "Without a Town Hall item, placing must fail");
            helper.assertBlockPresent(Blocks.AIR, SITE_CITY_BLOCK);

            player.getInventory().add(new ItemStack(MinecraftMod.TOWN_HALL_ITEM.get(), 2));
            helper.assertTrue(ConstructionSites.place(player, helper.getLevel(), blueprint, target, Rotation.NONE).isSuccess(),
                    "With a Town Hall item, placing should succeed");
            helper.assertTrue(player.getInventory().countItem(MinecraftMod.TOWN_HALL_ITEM.get()) == 1, "Placing should use up one Town Hall");
        });
        helper.succeed();
    }

    @GameTest(template = BLUEPRINT_PLATFORM, batch = "constructionSitesMustNotOverlapOrBeBlocked")
    public static void constructionSitesMustNotOverlapOrBeBlocked(GameTestHelper helper)
    {
        clearSettlementsAndSites(helper);
        withSourceBlueprint(helper, blueprint -> {
            Player player = helper.makeMockPlayer();
            helper.setBlock(SITE_CITY_BLOCK, Blocks.STONE);
            helper.assertFalse(ConstructionSites.place(player, helper.getLevel(), blueprint, helper.absolutePos(SITE_CITY_BLOCK), Rotation.NONE).isSuccess(),
                    "A blocked city block spot must be rejected");

            helper.setBlock(SITE_CITY_BLOCK, Blocks.AIR);
            helper.assertTrue(ConstructionSites.place(player, helper.getLevel(), blueprint, helper.absolutePos(SITE_CITY_BLOCK), Rotation.NONE).isSuccess(),
                    "The first site should be placed");
            helper.assertFalse(ConstructionSites.place(player, helper.getLevel(), blueprint, helper.absolutePos(SITE_CITY_BLOCK.offset(1, 0, 1)),
                    Rotation.NONE).isSuccess(), "An overlapping site must be rejected");
        });
        helper.succeed();
    }

    @GameTest(template = BLUEPRINT_PLATFORM, batch = "removingCityBlockCancelsConstructionSite")
    public static void removingCityBlockCancelsConstructionSite(GameTestHelper helper)
    {
        clearSettlementsAndSites(helper);
        withSourceBlueprint(helper, blueprint -> {
            BlockPos target = helper.absolutePos(SITE_CITY_BLOCK);
            helper.assertTrue(ConstructionSites.place(helper.makeMockPlayer(), helper.getLevel(), blueprint, target, Rotation.NONE).isSuccess(),
                    "Placing should succeed");

            helper.destroyBlock(SITE_CITY_BLOCK);

            helper.assertTrue(ConstructionSites.atCityBlock(helper.getLevel(), target).isEmpty(), "Removing the city block should cancel the site");
        });
        helper.succeed();
    }

    @GameTest(template = BLUEPRINT_PLATFORM, batch = "placedBuildersHutJoinsLeadersSettlement")
    public static void placedBuildersHutJoinsLeadersSettlement(GameTestHelper helper)
    {
        clearSettlementsAndSites(helper);
        // A one-block blueprint of just a Builder's Hut
        helper.setBlock(SOURCE_MIN, MinecraftMod.BUILDERS_HUT.get());
        BlueprintSelection hutOnly = BlueprintSelection.of(helper.absolutePos(SOURCE_MIN), helper.absolutePos(SOURCE_MIN));
        Blueprints.SaveResult saved = Blueprints.save(helper.getLevel(), hutOnly, uniqueBlueprintName(), "gametest");
        ConstructionSite site = null;
        try
        {
            helper.assertTrue(saved.isSuccess(), "Saving should succeed, got: " + saved.error());
            Blueprint blueprint = Blueprints.load(helper.getLevel().getServer(), saved.id()).orElseThrow();
            Player leader = helper.makeMockPlayer();
            Settlement settlement = Settlements.tryCreate(leader, placeTownHall(helper, new BlockPos(2, 2, 3)), "Rivertown").settlement();

            ConstructionSites.Result result = ConstructionSites.place(leader, helper.getLevel(), blueprint, helper.absolutePos(SITE_CITY_BLOCK), Rotation.NONE);
            site = result.site();

            helper.assertTrue(result.isSuccess(), "Placing should succeed, got: " + result.error());
            helper.assertTrue(settlement.id().equals(site.settlementId()), "The site should belong to the leader's settlement");
            if (!(helper.getBlockEntity(SITE_CITY_BLOCK) instanceof BuildersHutBlockEntity hut) || !settlement.id().equals(hut.getSettlementId()))
                throw new IllegalStateException("The placed Builder's Hut should join the leader's settlement");
        }
        finally
        {
            deleteBlueprint(helper, saved);
            if (site != null)
                ConstructionSiteData.get(helper.getLevel().getServer()).remove(site.id());
        }
        helper.succeed();
    }

    // --- Territory ---

    private static Set<ChunkPos> chunkSquare(ChunkPos center, int radius)
    {
        Set<ChunkPos> chunks = new HashSet<>();
        for (int x = -radius; x <= radius; x++)
            for (int z = -radius; z <= radius; z++)
                chunks.add(new ChunkPos(center.x + x, center.z + z));
        return chunks;
    }

    // A one-block blueprint of just a Builder's Hut (built at SOURCE_MIN of the blueprint platform)
    private static Blueprints.SaveResult saveHutBlueprint(GameTestHelper helper)
    {
        helper.setBlock(SOURCE_MIN, MinecraftMod.BUILDERS_HUT.get());
        Blueprints.SaveResult saved = Blueprints.save(helper.getLevel(), BlueprintSelection.of(helper.absolutePos(SOURCE_MIN), helper.absolutePos(SOURCE_MIN)),
                uniqueBlueprintName(), "gametest");
        helper.assertTrue(saved.isSuccess(), "Saving the hut blueprint should succeed, got: " + saved.error());
        helper.setBlock(SOURCE_MIN, Blocks.AIR);
        return saved;
    }

    @GameTest(template = PLATFORM, batch = "territoryIsSquareAroundTownHall")
    public static void territoryIsSquareAroundTownHall(GameTestHelper helper)
    {
        clearSettlementsAndSites(helper);
        Settlement settlement = foundSettlement(helper, helper.makeMockPlayer());
        ChunkPos townHallChunk = new ChunkPos(settlement.townHallPos());
        int radius = Config.townHallTerritoryRadius;

        helper.assertTrue(Territory.of(settlement).equals(chunkSquare(townHallChunk, radius)),
                "Territory should be the " + (2 * radius + 1) + "x" + (2 * radius + 1) + " chunks around the Town Hall");
        helper.assertFalse(Territory.contains(settlement, settlement.townHallPos().offset(16 * (radius + 1), 0, 0)),
                "Chunks beyond the radius must not be in the territory");
        helper.succeed();
    }

    @GameTest(template = PLATFORM, batch = "buildingsExtendTerritory")
    public static void buildingsExtendTerritory(GameTestHelper helper)
    {
        clearSettlementsAndSites(helper);
        Settlement settlement = foundSettlement(helper, helper.makeMockPlayer());
        SettlementData data = settlements(helper);
        ChunkPos townHallChunk = new ChunkPos(settlement.townHallPos());
        // A building in the territory's edge chunk reaches one building radius further out
        BlockPos edgeBuilding = settlement.townHallPos().offset(16 * Config.townHallTerritoryRadius, 0, 0);
        BlockPos beyondEdge = edgeBuilding.offset(16 * Config.buildingTerritoryRadius, 0, 0);
        helper.assertFalse(Territory.contains(settlement, beyondEdge), "Before the building, the chunk beyond the edge is outside");

        data.addBuilding(settlement, edgeBuilding);
        Set<ChunkPos> expected = chunkSquare(townHallChunk, Config.townHallTerritoryRadius);
        expected.addAll(chunkSquare(new ChunkPos(edgeBuilding), Config.buildingTerritoryRadius));
        helper.assertTrue(Territory.of(settlement).equals(expected), "The building should add the chunks around it");
        helper.assertTrue(Territory.contains(settlement, beyondEdge), "After the building, the chunk beyond the edge is inside");

        data.removeBuilding(settlement, edgeBuilding);
        helper.assertFalse(Territory.contains(settlement, beyondEdge), "Removing the building should take its chunks away again");
        helper.succeed();
    }

    @GameTest(template = PLATFORM, batch = "claimedWorkplaceBecomesBuildingUntilRemoved")
    public static void claimedWorkplaceBecomesBuildingUntilRemoved(GameTestHelper helper)
    {
        clearSettlementsAndSites(helper);
        Player leader = helper.makeMockPlayer();
        Settlement settlement = foundSettlement(helper, leader);
        BuildersHutBlockEntity hut = placeClaimedHut(helper, HUT_A, leader);

        helper.assertTrue(settlement.buildings().contains(hut.getBlockPos()), "A claimed hut should count as a building of the settlement");
        helper.destroyBlock(HUT_A);
        helper.assertFalse(settlement.buildings().contains(helper.absolutePos(HUT_A)), "A removed hut should no longer count as a building");
        helper.succeed();
    }

    @GameTest(template = PLATFORM, batch = "settlementBuildingsSurviveSaveAndLoad")
    public static void settlementBuildingsSurviveSaveAndLoad(GameTestHelper helper)
    {
        clearSettlementsAndSites(helper);
        Settlement settlement = foundSettlement(helper, helper.makeMockPlayer());
        settlements(helper).addBuilding(settlement, settlement.townHallPos().offset(40, 0, 7));

        Settlement loaded = Settlement.load(settlement.save());

        helper.assertTrue(loaded.buildings().equals(settlement.buildings()), "Buildings should survive save and load");
        helper.assertTrue(Territory.of(loaded).equals(Territory.of(settlement)), "The territory should be the same after loading");
        helper.succeed();
    }

    @GameTest(template = BLUEPRINT_PLATFORM, batch = "workplaceSiteMustBeInsideTerritory")
    public static void workplaceSiteMustBeInsideTerritory(GameTestHelper helper)
    {
        clearSettlementsAndSites(helper);
        Blueprints.SaveResult saved = saveHutBlueprint(helper);
        ConstructionSite site = null;
        try
        {
            Blueprint hutBlueprint = Blueprints.load(helper.getLevel().getServer(), saved.id()).orElseThrow();
            Player leader = helper.makeMockPlayer();
            Settlements.tryCreate(leader, placeTownHall(helper, new BlockPos(2, 2, 3)), "Rivertown");
            BlockPos outside = helper.absolutePos(SITE_CITY_BLOCK).offset(16 * (Config.townHallTerritoryRadius + 1), 0, 0);

            ConstructionSites.Result farAway = ConstructionSites.place(leader, helper.getLevel(), hutBlueprint, outside, Rotation.NONE);
            helper.assertFalse(farAway.isSuccess(), "A Builder's Hut outside the territory must be rejected");
            helper.assertTrue(ConstructionSites.atCityBlock(helper.getLevel(), outside).isEmpty(), "No site should be made outside the territory");

            ConstructionSites.Result inside = ConstructionSites.place(leader, helper.getLevel(), hutBlueprint, helper.absolutePos(SITE_CITY_BLOCK), Rotation.NONE);
            site = inside.site();
            helper.assertTrue(inside.isSuccess(), "A Builder's Hut inside the territory should be placed, got: " + inside.error());
        }
        finally
        {
            deleteBlueprint(helper, saved);
            if (site != null)
                ConstructionSiteData.get(helper.getLevel().getServer()).remove(site.id());
        }
        helper.succeed();
    }

    @GameTest(template = BLUEPRINT_PLATFORM, batch = "workplaceSiteNeedsSettlement")
    public static void workplaceSiteNeedsSettlement(GameTestHelper helper)
    {
        clearSettlementsAndSites(helper);
        Blueprints.SaveResult saved = saveHutBlueprint(helper);
        try
        {
            Blueprint hutBlueprint = Blueprints.load(helper.getLevel().getServer(), saved.id()).orElseThrow();
            ConstructionSites.Result result = ConstructionSites.place(helper.makeMockPlayer(), helper.getLevel(), hutBlueprint,
                    helper.absolutePos(SITE_CITY_BLOCK), Rotation.NONE);

            helper.assertFalse(result.isSuccess(), "Without a settlement, a Builder's Hut can't be placed");
            helper.assertBlockPresent(Blocks.AIR, SITE_CITY_BLOCK);
        }
        finally
        {
            deleteBlueprint(helper, saved);
        }
        helper.succeed();
    }

    // --- Town Halls and other settlements' territory ---

    @GameTest(template = PLATFORM)
    public static void territoryPacketKeepsOwnAndOtherChunks(GameTestHelper helper)
    {
        TerritoryPacket packet = new TerritoryPacket(chunkSquare(new ChunkPos(0, 0), 1), chunkSquare(new ChunkPos(10, -4), 2));
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        packet.encode(buf);
        TerritoryPacket read = TerritoryPacket.decode(buf);

        helper.assertTrue(read.chunks().equals(packet.chunks()), "The own territory should be the same after sending it");
        helper.assertTrue(read.others().equals(packet.others()), "The other settlements' territory should be the same after sending it");
        helper.succeed();
    }

    @GameTest(template = PLATFORM, batch = "townHallCannotBeFoundedInOtherTerritory")
    public static void townHallCannotBeFoundedInOtherTerritory(GameTestHelper helper)
    {
        clearSettlementsAndSites(helper);
        foundSettlement(helper, helper.makeMockPlayer());
        Player newcomer = helper.makeMockPlayer();

        Settlements.CreateResult result = Settlements.tryCreate(newcomer, placeTownHall(helper, TOWN_HALL_B), "Hilltop");

        helper.assertFalse(result.isSuccess(), "Founding inside another settlement's territory must be rejected");
        helper.assertTrue(settlements(helper).getByLeader(newcomer.getUUID()).isEmpty(), "The newcomer should not lead a settlement");
        helper.succeed();
    }

    @GameTest(template = BLUEPRINT_PLATFORM, batch = "townHallSiteCannotBePlacedInOtherTerritory")
    public static void townHallSiteCannotBePlacedInOtherTerritory(GameTestHelper helper)
    {
        clearSettlementsAndSites(helper);
        withSourceBlueprint(helper, townHallBlueprint -> {
            Player leader = helper.makeMockPlayer();
            Settlements.tryCreate(leader, placeTownHall(helper, new BlockPos(2, 2, 3)), "Rivertown");
            BlockPos target = helper.absolutePos(SITE_CITY_BLOCK);

            ConstructionSites.Result stranger = ConstructionSites.place(helper.makeMockPlayer(), helper.getLevel(), townHallBlueprint, target, Rotation.NONE);
            helper.assertFalse(stranger.isSuccess(), "A Town Hall site inside another settlement's territory must be rejected");
            helper.assertBlockPresent(Blocks.AIR, SITE_CITY_BLOCK);

            ConstructionSites.Result own = ConstructionSites.place(leader, helper.getLevel(), townHallBlueprint, target, Rotation.NONE);
            helper.assertTrue(own.isSuccess(), "The leader may place a Town Hall site in their own territory, got: " + own.error());
        });
        helper.succeed();
    }
}
