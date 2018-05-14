package mcjty.rftoolsdim.dimensions;

import mcjty.lib.worlddata.AbstractWorldData;
import mcjty.rftoolsdim.config.PowerConfiguration;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.Constants;

import java.util.HashMap;
import java.util.Map;

public class DimensionStorage extends AbstractWorldData<DimensionStorage> {

    private static final String DIMSTORAGE_NAME = "RFToolsDimensionStorage";

    private final Map<Integer,Integer> energy = new HashMap<>();

    public DimensionStorage(String name) {
        super(name);
    }

    @Override
    public void clear() {
        energy.clear();
    }

    public static DimensionStorage getDimensionStorage(World world) {
        return getData(world, DimensionStorage.class, DIMSTORAGE_NAME);
    }

    public int getEnergyLevel(int id) {
        if (energy.containsKey(id)) {
            return energy.get(id);
        } else {
            return 0;
        }
    }

    public void setEnergyLevel(int id, int energyLevel) {
        int old = getEnergyLevel(id);
        energy.put(id, energyLevel);
        if (PowerConfiguration.freezeUnpowered) {
            World world = DimensionManager.getWorld(id);
            if (world != null) {
                if (old == 0 && energyLevel > 0) {
                    RfToolsDimensionManager.unfreezeDimension(world);
                } else if (energyLevel == 0) {
                    RfToolsDimensionManager.freezeDimension(world);
                }
            }
        }
    }

    public void removeDimension(int id) {
        energy.remove(id);
    }

    @Override
    public void readFromNBT(NBTTagCompound tagCompound) {
        energy.clear();
        NBTTagList lst = tagCompound.getTagList("dimensions", Constants.NBT.TAG_COMPOUND);
        for (int i = 0 ; i < lst.tagCount() ; i++) {
            NBTTagCompound tc = lst.getCompoundTagAt(i);
            int id = tc.getInteger("id");
            int rf = tc.getInteger("energy");
            energy.put(id, rf);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound) {
        NBTTagList lst = new NBTTagList();
        for (Map.Entry<Integer,Integer> me : energy.entrySet()) {
            NBTTagCompound tc = new NBTTagCompound();
            tc.setInteger("id", me.getKey());
            tc.setInteger("energy", me.getValue());
            lst.appendTag(tc);
        }
        tagCompound.setTag("dimensions", lst);
        return tagCompound;
    }
}
