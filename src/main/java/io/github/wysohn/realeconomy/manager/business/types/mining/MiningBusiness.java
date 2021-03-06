package io.github.wysohn.realeconomy.manager.business.types.mining;

import io.github.wysohn.rapidframework3.core.language.ManagerLanguage;
import io.github.wysohn.rapidframework3.data.SimpleLocation;
import io.github.wysohn.rapidframework3.interfaces.ICommandSender;
import io.github.wysohn.rapidframework3.interfaces.IMemento;
import io.github.wysohn.realeconomy.interfaces.business.IBusinessContextHandler;
import io.github.wysohn.realeconomy.interfaces.business.types.mining.IBlockGenerator;
import io.github.wysohn.realeconomy.main.RealEconomyLangs;
import io.github.wysohn.realeconomy.manager.asset.signature.LabourSignature;
import io.github.wysohn.realeconomy.manager.business.types.AbstractBusiness;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.*;

public class MiningBusiness extends AbstractBusiness {
    // special blocks that re-generates and also yield more labour points.
    private static final Map<Material, Double> SPECIAL_BLOCKS = new EnumMap<>(Material.class);

    static {
        SPECIAL_BLOCKS.put(Material.COBBLESTONE, 0.5);
        SPECIAL_BLOCKS.put(Material.COAL_ORE, 5.0);
        SPECIAL_BLOCKS.put(Material.DIAMOND_ORE, 240.0);
        SPECIAL_BLOCKS.put(Material.EMERALD_ORE, 300.0);
        SPECIAL_BLOCKS.put(Material.GOLD_ORE, 80.0);
        SPECIAL_BLOCKS.put(Material.IRON_ORE, 10.0);
        SPECIAL_BLOCKS.put(Material.LAPIS_ORE, 120.0);
        SPECIAL_BLOCKS.put(Material.REDSTONE_ORE, 30.0);
    }

    @Inject
    private IBlockGenerator blockGenerator;

    @Inject
    @Named("oreRegenDelay")
    private long oreRegenDelay;

    private final Map<SimpleLocation, OreInfo> regenQueue = Collections.synchronizedMap(new LinkedHashMap<>());

    private MiningBusiness() {
        super(null);
    }

    public MiningBusiness(UUID key) {
        super(key);
    }

    @Override
    public Map<Object, Object> properties(ManagerLanguage lang, ICommandSender sender) {
        Map<Object, Object> map = super.properties(lang, sender);

        if (isEstablished()) {
            Map<Object, Object> labourPoints = new LinkedHashMap<>();
            map.put(RealEconomyLangs.MiningBusiness_LabourSources, labourPoints);
            SPECIAL_BLOCKS.forEach((material, amount) -> labourPoints.put(material, String.format("&8- &f%.2flp", amount)));
        }

        return map;
    }

    @Override
    public void update() {
        super.update();

        SimpleLocation location = null;
        OreInfo oreInfo = null;
        synchronized (regenQueue) {
            Iterator<Map.Entry<SimpleLocation, OreInfo>> iter = regenQueue.entrySet().iterator();
            if (!iter.hasNext())
                return;

            Map.Entry<SimpleLocation, OreInfo> oldestEntry = iter.next();
            location = oldestEntry.getKey();
            oreInfo = oldestEntry.getValue();
        }

        if (location != null && oreInfo != null) {
            // check if regen time is passed.
            if (System.currentTimeMillis() < oreInfo.breakAt + oreRegenDelay)
                return;

            // delete from queue
            regenQueue.remove(location);
            // regen ore
            blockGenerator.generateBlockAt(location, oreInfo.material);
        }
    }

    /**
     * Main thread
     *
     * @param event
     */
    void blockBreak(BlockBreakEvent event, IBusinessContextHandler handler) {
        Block block = event.getBlock();
        Location location = block.getLocation();

        event.setCancelled(true);

        if (handler.isInBusiness(this, event.getPlayer().getUniqueId())) {
            // get labour point
            double labourPoints = SPECIAL_BLOCKS.getOrDefault(block.getType(), 0.1);

            // queue ore regen and delete the block manually
            if (SPECIAL_BLOCKS.containsKey(block.getType())) {
                regenQueue.put(new SimpleLocation(location.getWorld().getName(),
                        location.getBlockX(),
                        location.getBlockY(),
                        location.getBlockZ()), new OreInfo(block.getType(), System.currentTimeMillis()));
                event.getBlock().setType(Material.AIR);
            }

            // increase labour points
            addAsset(new LabourSignature().asset(labourPoints));
        }
    }

    @Override
    public IMemento saveState() {
        return new Memento(this);
    }

    @Override
    public void restoreState(IMemento iMemento) {
        super.restoreState(iMemento);


    }

    private class Memento extends ParentMemento {
        public Memento(AbstractBusiness business) {
            super(business);
        }
    }

//    public static void main(String[] ar){
//        Map<String, Integer> test = new LinkedHashMap<>();
//        test.put("test1", 1);
//        test.put("test2", 2);
//        test.put("test3", 3);
//
//        Iterator<Map.Entry<String, Integer>> iter = test.entrySet().stream().iterator();
//        while(iter.hasNext()){
//            System.out.println(iter.next());
//        }
//    }
}
