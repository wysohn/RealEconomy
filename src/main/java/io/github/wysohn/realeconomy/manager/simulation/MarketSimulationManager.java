package io.github.wysohn.realeconomy.manager.simulation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.wysohn.rapidframework3.core.inject.annotations.PluginDirectory;
import io.github.wysohn.rapidframework3.core.inject.annotations.PluginLogger;
import io.github.wysohn.rapidframework3.core.inject.factory.IStorageFactory;
import io.github.wysohn.rapidframework3.core.main.Manager;
import io.github.wysohn.rapidframework3.interfaces.store.IKeyValueStorage;
import io.github.wysohn.rapidframework3.utils.Pair;
import io.github.wysohn.rapidframework3.utils.Validation;
import io.github.wysohn.realeconomy.interfaces.banking.IBankUserProvider;
import io.github.wysohn.realeconomy.interfaces.listing.IListingInfoProvider;
import io.github.wysohn.realeconomy.interfaces.simulation.IAgentReloadObserver;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import io.github.wysohn.realeconomy.manager.asset.signature.ItemStackSignature;
import io.github.wysohn.realeconomy.manager.listing.AssetListingManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

@Singleton
public class MarketSimulationManager extends Manager {
    public static final String SIMULATOR = "simulator";
    private static final Random RAND = new Random();
    private static final Set<Material> WOODS = new HashSet<>();
    private static final Set<Material> LOGS = new HashSet<>();

    private final Map<UUID, Agent> agentList = new HashMap<>();

    private final Logger logger;
    private final IListingInfoProvider assetInfoProvider;

    private final Set<IAgentReloadObserver> agentReloadObservers = new HashSet<>();
    private final IKeyValueStorage config;
    private final IBankUserProvider provider = agentList::get;

    @Inject
    public MarketSimulationManager(@PluginLogger Logger logger,
                                   @PluginDirectory File pluginDir,
                                   IListingInfoProvider assetInfoProvider,
                                   IStorageFactory storageFactory) {
        this.logger = logger;
        this.assetInfoProvider = assetInfoProvider;

        dependsOn(AssetListingManager.class);

        config = storageFactory.create(pluginDir, "agents.yml");

        for (Material value : Material.values()) {
            if (value.name().startsWith("LEGACY"))
                continue;

            if (value.name().endsWith("_WOOD"))
                WOODS.add(value);
        }
        for (Material value : Material.values()) {
            if (value.name().startsWith("LEGACY"))
                continue;

            if (value.name().endsWith("_LOG"))
                LOGS.add(value);
        }
    }

    @Override
    public void enable() throws Exception {

    }

    private void addAgent(Agent agent) {
        agentList.put(agent.getUuid(), agent);
    }

    @Override
    public void load() throws Exception {
        agentReloadObservers.forEach(observer -> observer.beforeAgentReload(getAgents()));

        /*
        simulator:
          <agentName>:
            uuid: <UUID>
            resourcesNeeded:
              <UUID>: 22
              <UUID>: 5
            production:
              <UUID>: 2
         */

        synchronized (agentList) {
            agentList.clear();
            if (!config.get(SIMULATOR).isPresent()) {
//            addAgent(new AgentConfigBuilder("")
//                    .build(logger, config, assetInfoProvider));

                addAgent(new AgentConfigBuilder("Pastry_1")
                        .addNeededResource(Material.WHEAT, 300)
                        .addOutput(Material.BREAD, 100)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Pastry_2")
                        .addNeededResource(Material.WHEAT, 200)
                        .addNeededResource(Material.COCOA_BEANS, 100)
                        .addOutput(Material.COOKIE, 800)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Pastry_3")
                        .addNeededResource(Material.PUMPKIN, 100)
                        .addNeededResource(Material.SUGAR, 100)
                        .addNeededResource(Material.EGG, 100)
                        .addOutput(Material.PUMPKIN_PIE, 100)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Pastry_4")
                        .addNeededResource(Material.SUGAR, 200)
                        .addNeededResource(Material.EGG, 100)
                        .addNeededResource(Material.WHEAT, 300)
                        .addNeededResource(Material.IRON_INGOT, 9) // represents 3 buckets
                        .addOutput(Material.CAKE, 100)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Pastry_5")
                        .addNeededResource(Material.GOLD_NUGGET, 800)
                        .addNeededResource(Material.CARROT, 100)
                        .addOutput(Material.GOLDEN_CARROT, 100)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Pastry_6")
                        .addNeededResource(Material.GOLD_NUGGET, 800)
                        .addNeededResource(Material.MELON_SLICE, 100)
                        .addOutput(Material.GLISTERING_MELON_SLICE, 100)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Pastry_7")
                        .addNeededResource(Material.POTATO, 100)
                        .addOutput(Material.BAKED_POTATO, 100)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Pastry_8")
                        .addNeededResource(Material.BEETROOT, 600)
                        .addNeededResource(Material.BOWL, 100) // this is made of easy to get material
                        .addOutput(Material.BEETROOT_SOUP, 100)
                        .build(logger, config, assetInfoProvider));

                addAgent(new AgentConfigBuilder("Farmer_1")
                        .addNeededResource(Material.WHEAT_SEEDS, 100)
                        .addOutput(Material.WHEAT, 100)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Farmer_1_2")
                        .addNeededResource(Material.WHEAT_SEEDS, 100)
                        .addOutput(Material.WHEAT_SEEDS, 150)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Farmer_2")
                        .addNeededResource(Material.CARROT, 100)
                        .addOutput(Material.CARROT, 150)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Farmer_3")
                        .addNeededResource(Material.POTATO, 100)
                        .addOutput(Material.POTATO, 150)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Farmer_4")
                        .addNeededResource(Material.BEETROOT_SEEDS, 100)
                        .addOutput(Material.BEETROOT, 100)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Farmer_4_2")
                        .addNeededResource(Material.BEETROOT_SEEDS, 100)
                        .addOutput(Material.BEETROOT_SEEDS, 150)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Farmer_5")
                        .addNeededResource(Material.SWEET_BERRIES, 100)
                        .addOutput(Material.SWEET_BERRIES, 125)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Farmer_6")
                        .addNeededResource(Material.MELON_SEEDS, 100)
                        .addOutput(Material.MELON_SLICE, 500)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Farmer_7")
                        .addNeededResource(Material.PUMPKIN_SEEDS, 100)
                        .addOutput(Material.PUMPKIN, 500)
                        .build(logger, config, assetInfoProvider));

                addAgent(new AgentConfigBuilder("Librarian_1")
                        .addNeededResource(Material.PAPER, 300)
                        .addNeededResource(Material.LEATHER, 100)
                        .addOutput(Material.BOOK, 100)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Librarian_2")
                        .addNeededResource(Material.PAPER, 100)
                        .addNeededResource(Material.DIAMOND, 200)
                        .addNeededResource(Material.OBSIDIAN, 400)
                        .addOutput(Material.ENCHANTING_TABLE, 100)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Librarian_3")
                        .addNeededResource(Material.PAPER, 300)
                        .addNeededResource(Material.LEATHER, 100)
                        .addOutput(Material.BOOK, 100)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Librarian_4")
                        .addNeededResource(Material.IRON_INGOT, 40)
                        .addNeededResource(Material.REDSTONE, 10)
                        .addOutput(Material.COMPASS, 10)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Librarian_5")
                        .addNeededResource(Material.GOLD_INGOT, 40)
                        .addNeededResource(Material.REDSTONE, 10)
                        .addOutput(Material.CLOCK, 10)
                        .build(logger, config, assetInfoProvider));
                for (Material wood : WOODS) {
                    addAgent(new AgentConfigBuilder("Librarian_BookShelf_" + wood)
                            .addNeededResource(wood, 60)
                            .addNeededResource(Material.BOOK, 30)
                            .addOutput(Material.BOOKSHELF, 10)
                            .build(logger, config, assetInfoProvider));
                    addAgent(new AgentConfigBuilder("Librarian_NameTag_" + wood)
                            .addNeededResource(wood, 20)
                            .addNeededResource(Material.STRING, 10)
                            .addNeededResource(Material.DIAMOND, 1)
                            .addOutput(Material.NAME_TAG, 10)
                            .build(logger, config, assetInfoProvider));
                }
                for (Enchantment ench : Enchantment.values()) {
                    ItemStack enchBook = new ItemStack(Material.ENCHANTED_BOOK);
                    EnchantmentStorageMeta esm = (EnchantmentStorageMeta) enchBook.getItemMeta();
                    for (int level = ench.getStartLevel(); level <= ench.getMaxLevel(); level++) {
                        esm.addStoredEnchant(ench, level, false);
                        enchBook.setItemMeta(esm);

                        addAgent(new AgentConfigBuilder("Librarian_E_" + ench + "_lv" + level)
                                .addNeededResource(Material.BOOK, 2)
                                .addNeededResource(Material.EMERALD, 1)
                                .addNeededResource(Material.LAPIS_LAZULI, 1)
                                .addOutput(enchBook, 2)
                                .build(logger, config, assetInfoProvider));
                    }
                }

                addAgent(new AgentConfigBuilder("Toolsmith_1")
                        .addNeededResource(Material.GOLD_INGOT, 25)
                        .addNeededResource(Material.STICK, 10)
                        .addNeededResource(Material.DIAMOND, 5)
                        .addOutput(Material.BELL, 5)
                        .build(logger, config, assetInfoProvider));

                addAgentsFromRecipe("Toolsmith", Material.IRON_HELMET, 10);
                addAgentsFromRecipe("Toolsmith", Material.IRON_CHESTPLATE, 10);
                addAgentsFromRecipe("Toolsmith", Material.IRON_LEGGINGS, 10);
                addAgentsFromRecipe("Toolsmith", Material.IRON_BOOTS, 10);
                addAgentsFromRecipe("Toolsmith", Material.IRON_SHOVEL, 10);
                addAgentsFromRecipe("Toolsmith", Material.IRON_AXE, 10);
                addAgentsFromRecipe("Toolsmith", Material.IRON_PICKAXE, 10);
                addAgentsFromRecipe("Toolsmith", Material.IRON_HOE, 10);

                addAgentsFromRecipe("Toolsmith", Material.GOLDEN_HELMET, 10);
                addAgentsFromRecipe("Toolsmith", Material.GOLDEN_CHESTPLATE, 10);
                addAgentsFromRecipe("Toolsmith", Material.GOLDEN_LEGGINGS, 10);
                addAgentsFromRecipe("Toolsmith", Material.GOLDEN_BOOTS, 10);
                addAgentsFromRecipe("Toolsmith", Material.GOLDEN_SHOVEL, 10);
                addAgentsFromRecipe("Toolsmith", Material.GOLDEN_AXE, 10);
                addAgentsFromRecipe("Toolsmith", Material.GOLDEN_PICKAXE, 10);
                addAgentsFromRecipe("Toolsmith", Material.GOLDEN_HOE, 10);

                addAgentsFromRecipe("Toolsmith", Material.DIAMOND_HELMET, 10);
                addAgentsFromRecipe("Toolsmith", Material.DIAMOND_CHESTPLATE, 10);
                addAgentsFromRecipe("Toolsmith", Material.DIAMOND_LEGGINGS, 10);
                addAgentsFromRecipe("Toolsmith", Material.DIAMOND_BOOTS, 10);
                addAgentsFromRecipe("Toolsmith", Material.DIAMOND_SHOVEL, 10);
                addAgentsFromRecipe("Toolsmith", Material.DIAMOND_AXE, 10);
                addAgentsFromRecipe("Toolsmith", Material.DIAMOND_PICKAXE, 10);
                addAgentsFromRecipe("Toolsmith", Material.DIAMOND_HOE, 10);

                for (Material material : Material.values()) {
                    addAgentsFromRecipe("Auto", material, 10);
                }

                addAgent(new AgentConfigBuilder("Miner_Ancient")
                        .addOutput(Material.ANCIENT_DEBRIS, (int)(2.89 * 4))
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Miner_Emerald")
                        .addOutput(Material.EMERALD, (int)(3.48 * 4))
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Miner_Lapis")
                        .addOutput(Material.LAPIS_LAZULI, (int)(4.11 * 4))
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Miner_Diamond")
                        .addOutput(Material.DIAMOND, (int)(4.32 * 4))
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Miner_Gold")
                        .addOutput(Material.GOLD_INGOT, (int)(5.4 * 4))
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Miner_Redstone")
                        .addOutput(Material.REDSTONE, (int)(35.64 * 4))
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Miner_Iron")
                        .addOutput(Material.IRON_INGOT, (int)(26.64 * 4))
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Miner_Coal")
                        .addOutput(Material.COAL, (int)(45.0 * 4))
                        .build(logger, config, assetInfoProvider));

                addAgent(new AgentConfigBuilder("Dye_Red_1")
                        .addNeededResource(Material.POPPY, 10)
                        .addOutput(Material.RED_DYE, 10)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Dye_Red_1")
                        .addNeededResource(Material.RED_TULIP, 10)
                        .addOutput(Material.RED_DYE, 10)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Dye_Red_1")
                        .addNeededResource(Material.ROSE_BUSH, 10)
                        .addOutput(Material.RED_DYE, 10)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Dye_Red_1")
                        .addNeededResource(Material.BEETROOT, 10)
                        .addOutput(Material.RED_DYE, 10)
                        .build(logger, config, assetInfoProvider));

                addAgent(new AgentConfigBuilder("Dye_Green")
                        .addNeededResource(Material.CACTUS, 10)
                        .addOutput(Material.GREEN_DYE, 10)
                        .build(logger, config, assetInfoProvider));

                addAgent(new AgentConfigBuilder("Dye_Purple")
                        .addNeededResource(Material.RED_DYE, 10)
                        .addNeededResource(Material.BLUE_DYE, 10)
                        .addOutput(Material.PURPLE_DYE, 20)
                        .build(logger, config, assetInfoProvider));

                addAgent(new AgentConfigBuilder("Dye_Cyan")
                        .addNeededResource(Material.GREEN_DYE, 10)
                        .addNeededResource(Material.BLUE_DYE, 10)
                        .addOutput(Material.CYAN_DYE, 20)
                        .build(logger, config, assetInfoProvider));

                addAgent(new AgentConfigBuilder("Dye_LightGray_1")
                        .addNeededResource(Material.AZURE_BLUET, 10)
                        .addOutput(Material.LIGHT_GRAY_DYE, 10)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Dye_LightGray_2")
                        .addNeededResource(Material.WHITE_TULIP, 10)
                        .addOutput(Material.LIGHT_GRAY_DYE, 10)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Dye_LightGray_3")
                        .addNeededResource(Material.OXEYE_DAISY, 10)
                        .addOutput(Material.LIGHT_GRAY_DYE, 10)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Dye_LightGray_4")
                        .addNeededResource(Material.GRAY_DYE, 10)
                        .addNeededResource(Material.WHITE_DYE, 10)
                        .addOutput(Material.LIGHT_GRAY_DYE, 20)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Dye_LightGray_5")
                        .addNeededResource(Material.BLACK_DYE, 10)
                        .addNeededResource(Material.WHITE_DYE, 20)
                        .addOutput(Material.LIGHT_GRAY_DYE, 30)
                        .build(logger, config, assetInfoProvider));

                addAgent(new AgentConfigBuilder("Dye_Gray")
                        .addNeededResource(Material.WHITE_DYE, 10)
                        .addNeededResource(Material.BLACK_DYE, 10)
                        .addOutput(Material.GRAY_DYE, 20)
                        .build(logger, config, assetInfoProvider));

                addAgent(new AgentConfigBuilder("Dye_Pink_1")
                        .addNeededResource(Material.RED_DYE, 10)
                        .addNeededResource(Material.WHITE_DYE, 10)
                        .addOutput(Material.PINK_DYE, 20)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Dye_Pink_2")
                        .addNeededResource(Material.PINK_TULIP, 10)
                        .addOutput(Material.PINK_DYE, 10)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Dye_Pink_3")
                        .addNeededResource(Material.PEONY, 10)
                        .addOutput(Material.PINK_DYE, 10)
                        .build(logger, config, assetInfoProvider));

                addAgent(new AgentConfigBuilder("Dye_Lime")
                        .addNeededResource(Material.GREEN_DYE, 10)
                        .addNeededResource(Material.WHITE_DYE, 10)
                        .addOutput(Material.LIME_DYE, 20)
                        .build(logger, config, assetInfoProvider));

                addAgent(new AgentConfigBuilder("Dye_Yellow_1")
                        .addNeededResource(Material.DANDELION, 10)
                        .addOutput(Material.YELLOW_DYE, 10)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Dye_Yellow_2")
                        .addNeededResource(Material.SUNFLOWER, 10)
                        .addOutput(Material.YELLOW_DYE, 20)
                        .build(logger, config, assetInfoProvider));

                addAgent(new AgentConfigBuilder("Dye_LightBlue_1")
                        .addNeededResource(Material.BLUE_DYE, 10)
                        .addNeededResource(Material.WHITE_DYE, 10)
                        .addOutput(Material.LIGHT_BLUE_DYE, 20)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Dye_LightBlue_2")
                        .addNeededResource(Material.BLUE_ORCHID, 10)
                        .addOutput(Material.LIGHT_BLUE_DYE, 10)
                        .build(logger, config, assetInfoProvider));

                addAgent(new AgentConfigBuilder("Dye_Magenta_1")
                        .addNeededResource(Material.PURPLE_DYE, 10)
                        .addNeededResource(Material.PINK_DYE, 10)
                        .addOutput(Material.MAGENTA_DYE, 20)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Dye_Magenta_2")
                        .addNeededResource(Material.BLUE_DYE, 10)
                        .addNeededResource(Material.RED_DYE, 10)
                        .addNeededResource(Material.PINK_DYE, 10)
                        .addOutput(Material.MAGENTA_DYE, 30)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Dye_Magenta_3")
                        .addNeededResource(Material.BLUE_DYE, 10)
                        .addNeededResource(Material.RED_DYE, 20)
                        .addNeededResource(Material.WHITE_DYE, 10)
                        .addOutput(Material.MAGENTA_DYE, 40)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Dye_Magenta_4")
                        .addNeededResource(Material.ALLIUM, 10)
                        .addOutput(Material.MAGENTA_DYE, 10)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Dye_Magenta_5")
                        .addNeededResource(Material.LILAC, 10)
                        .addOutput(Material.MAGENTA_DYE, 20)
                        .build(logger, config, assetInfoProvider));

                addAgent(new AgentConfigBuilder("Dye_Orange_1")
                        .addNeededResource(Material.RED_DYE, 10)
                        .addNeededResource(Material.YELLOW_DYE, 10)
                        .addOutput(Material.ORANGE_DYE, 20)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Dye_Orange_2")
                        .addNeededResource(Material.ORANGE_TULIP, 10)
                        .addOutput(Material.ORANGE_DYE, 10)
                        .build(logger, config, assetInfoProvider));

                addAgent(new AgentConfigBuilder("Dye_Blue_1")
                        .addNeededResource(Material.CORNFLOWER, 10)
                        .addOutput(Material.BLUE_DYE, 10)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Dye_Blue_2")
                        .addNeededResource(Material.LAPIS_LAZULI, 10)
                        .addOutput(Material.BLUE_DYE, 10)
                        .build(logger, config, assetInfoProvider));

                addAgent(new AgentConfigBuilder("Dye_Brown")
                        .addNeededResource(Material.COCOA_BEANS, 10)
                        .addOutput(Material.BROWN_DYE, 20)
                        .build(logger, config, assetInfoProvider));

                addAgent(new AgentConfigBuilder("Dye_Black_1")
                        .addNeededResource(Material.INK_SAC, 10)
                        .addOutput(Material.BLACK_DYE, 10)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Dye_Black_2")
                        .addNeededResource(Material.WITHER_ROSE, 10)
                        .addOutput(Material.BLACK_DYE, 10)
                        .build(logger, config, assetInfoProvider));

                addAgent(new AgentConfigBuilder("Dye_White_1")
                        .addNeededResource(Material.LILY_OF_THE_VALLEY, 10)
                        .addOutput(Material.WHITE_DYE, 10)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Dye_White_2")
                        .addNeededResource(Material.BONE_MEAL, 10)
                        .addOutput(Material.WHITE_DYE, 10)
                        .build(logger, config, assetInfoProvider));

                addAgent(new AgentConfigBuilder("Mason_1")
                        .addNeededResource(Material.SAND, 100)
                        .addOutput(Material.CLAY_BALL, 10)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Mason_2")
                        .addNeededResource(Material.CLAY_BALL, 10)
                        .addOutput(Material.BRICK, 10)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Mason_3")
                        .addNeededResource(Material.COBBLESTONE, 100)
                        .addOutput(Material.STONE, 100)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Mason_4")
                        .addNeededResource(Material.STONE, 100)
                        .addOutput(Material.STONE_BRICKS, 100)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Mason_5")
                        .addNeededResource(Material.DIORITE, 100)
                        .addNeededResource(Material.NETHER_QUARTZ_ORE, 100)
                        .addOutput(Material.GRANITE, 100)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Mason_6")
                        .addNeededResource(Material.GRANITE, 100)
                        .addOutput(Material.POLISHED_GRANITE, 100)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Mason_7")
                        .addNeededResource(Material.DIORITE, 100)
                        .addNeededResource(Material.COBBLESTONE, 100)
                        .addOutput(Material.ANDESITE, 200)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Mason_8")
                        .addNeededResource(Material.ANDESITE, 100)
                        .addOutput(Material.POLISHED_GRANITE, 100)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Mason_9")
                        .addNeededResource(Material.COBBLESTONE, 100)
                        .addNeededResource(Material.NETHER_QUARTZ_ORE, 100)
                        .addOutput(Material.DIORITE, 100)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Mason_10")
                        .addNeededResource(Material.DIORITE, 100)
                        .addOutput(Material.POLISHED_DIORITE, 100)
                        .build(logger, config, assetInfoProvider));

            } else {
                config.get(SIMULATOR)
                        .filter(config::isSection)
                        .map(section -> Agent.readAll(config,
                                logger,
                                assetInfoProvider,
                                section))
                        .ifPresent(agentCollection -> agentCollection.forEach(this::addAgent));
            }

            logger.info(agentList.size() + " Market Simulation Agents are active");
        }
    }

    private void addAgentsFromRecipe(String baseName, Material material, int amount) {
        new AgentConfigBuilder(baseName + "_" + material)
                .buildFromRecipe(logger, config, assetInfoProvider, material, amount)
                .forEach(this::addAgent);
    }

    @Override
    public void disable() throws Exception {

    }

    public void registerAgentReloadObserver(IAgentReloadObserver observer) {
        agentReloadObservers.add(observer);
    }

    /**
     * Get read-only collection of Agents.
     *
     * @return
     */
    public Collection<Agent> getAgents() {
        synchronized (agentList) {
            return new LinkedList<>(agentList.values());
        }
    }

    public IBankUserProvider getAgentProvider() {
        return provider;
    }

    private static class AgentConfigBuilder {
        private final String agentName;
        private final UUID uuid;
        private final List<Pair<AssetSignature, Double>> needed = new LinkedList<>();
        private final List<Pair<AssetSignature, Double>> production = new LinkedList<>();

        private AgentConfigBuilder(String agentName) {
            Validation.assertNotNull(agentName);
            Validation.validate(agentName, name -> name.length() > 0, "name cannot be empty.");

            this.agentName = agentName;
            this.uuid = UUID.randomUUID();
        }

        public static AgentConfigBuilder of(String agentName) {
            return new AgentConfigBuilder(agentName);
        }

        public AgentConfigBuilder addNeededResource(AssetSignature signature, double amount) {
            needed.add(Pair.of(signature, amount));
            return this;
        }

        public AgentConfigBuilder addNeededResource(Material material, int amount) {
            return addNeededResource(new ItemStackSignature(material), amount);
        }

        public AgentConfigBuilder addOutput(AssetSignature signature, double amount) {
            production.add(Pair.of(signature, amount));
            return this;
        }

        public AgentConfigBuilder addOutput(ItemStack itemStack, int amount) {
            return addOutput(new ItemStackSignature(itemStack), amount);
        }

        public AgentConfigBuilder addOutput(Material material, int amount) {
            return addOutput(new ItemStackSignature(material), amount);
        }

        public Agent build(Logger logger,
                           IKeyValueStorage config,
                           IListingInfoProvider assetInfoProvider) {
            Validation.validate(production, val -> val.size() > 0, "empty production outputs.");

            if(needed.size() < 1){
                logger.info(agentName+" has no needed input. It will keep producing output");
            }

            Agent agent = new Agent(logger,
                    uuid,
                    agentName,
                    needed,
                    production);

            ConfigurationSection section = config.get(SIMULATOR)
                    .map(ConfigurationSection.class::cast)
                    .orElseGet(YamlConfiguration::new);
            agent.write(section, assetInfoProvider);
            config.put(SIMULATOR, section);

            return agent;
        }

        /**
         * Create new agent from recipe setting. Can be multiple if multiple recipes found.
         *
         * @param logger
         * @param config
         * @param assetInfoProvide
         * @param output
         * @param amount
         * @return list of Agents; empty list if no recipe found
         */
        public List<Agent> buildFromRecipe(Logger logger,
                                           IKeyValueStorage config,
                                           IListingInfoProvider assetInfoProvide,
                                           Material output,
                                           int amount) {
            List<Agent> agents = new LinkedList<>();

            List<Recipe> recipes = Bukkit.getRecipesFor(new ItemStack(output));
            if (recipes.size() < 1)
                return agents;

            for (Recipe recipe : recipes) {
                if (recipe instanceof ShapedRecipe) {
                    ShapedRecipe shapedRecipe = (ShapedRecipe) recipe;
                    ItemStack result = recipe.getResult();

                    needed.clear();
                    production.clear();

                    shapedRecipe.getIngredientMap().forEach((c, need) -> {
                        if (need == null)
                            return;

                        ItemStackSignature sign = new ItemStackSignature(need);
                        assetInfoProvide.newListing(sign);
                        needed.add(Pair.of(sign, (double) (need.getAmount() * amount)));
                    });

                    if (!needed.isEmpty()) {
                        ItemStackSignature sign = new ItemStackSignature(result);
                        assetInfoProvide.newListing(sign);
                        production.add(Pair.of(sign, (double) (result.getAmount() * amount)));

                        agents.add(build(logger, config, assetInfoProvide));
                    }
                }
            }

            return agents;
        }
    }
}
