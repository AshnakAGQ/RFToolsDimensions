package mcjty.rftoolsdim.blocks.editor;

import mcjty.lib.container.DefaultSidedInventory;
import mcjty.lib.container.InventoryHelper;
import mcjty.lib.entity.GenericEnergyReceiverTileEntity;
import mcjty.lib.network.Argument;
import mcjty.lib.network.PacketRequestIntegerFromServer;
import mcjty.lib.varia.BlockTools;
import mcjty.lib.varia.Broadcaster;
import mcjty.rftoolsdim.RFToolsDim;
import mcjty.rftoolsdim.config.GeneralConfiguration;
import mcjty.rftoolsdim.config.MachineConfiguration;
import mcjty.rftoolsdim.config.Settings;
import mcjty.rftoolsdim.dimensions.DimensionInformation;
import mcjty.rftoolsdim.dimensions.DimensionStorage;
import mcjty.rftoolsdim.dimensions.RfToolsDimensionManager;
import mcjty.rftoolsdim.dimensions.dimlets.DimletCosts;
import mcjty.rftoolsdim.dimensions.dimlets.DimletKey;
import mcjty.rftoolsdim.dimensions.dimlets.KnownDimletConfiguration;
import mcjty.rftoolsdim.dimensions.dimlets.types.DimletType;
import mcjty.rftoolsdim.dimensions.dimlets.types.IDimletType;
import mcjty.rftoolsdim.dimensions.world.WorldGenerationTools;
import mcjty.rftoolsdim.network.RFToolsDimMessages;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class DimensionEditorTileEntity extends GenericEnergyReceiverTileEntity implements ITickable, DefaultSidedInventory {

    public static final String CMD_GETEDITING = "getEditing";
    public static final String CLIENTCMD_GETEDITING = "getEditing";

    private static int editPercentage = 0;
    private int ticksLeft = -1;
    private int ticksCost = -1;
    private int rfPerTick = -1;
    private int state = 0;          // For front state

    private InventoryHelper inventoryHelper = new InventoryHelper(this, DimensionEditorContainer.factory, 2);

    public DimensionEditorTileEntity() {
        super(MachineConfiguration.EDITOR_MAXENERGY, MachineConfiguration.EDITOR_RECEIVEPERTICK);
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        int oldstate = state;
        super.onDataPacket(net, packet);
        if (oldstate != state) {
            getWorld().markBlockRangeForRenderUpdate(getPos(), getPos());
        }
    }

    @Override
    protected boolean needsCustomInvWrapper() {
        return true;
    }

    @Override
    public InventoryHelper getInventoryHelper() {
        return inventoryHelper;
    }

    @Override
    public void update() {
        if (getWorld().isRemote) {
            return;
        }
        ItemStack injectableItemStack = validateInjectableItemStack();
        if (injectableItemStack.isEmpty()) {
            return;
        }

        ItemStack dimensionItemStack = validateDimensionItemStack();
        if (dimensionItemStack.isEmpty()) {
            return;
        }

        if (ticksLeft == -1) {
            // We were not injecting. Start now.
            if (isMatterReceiver(injectableItemStack)) {
                ticksCost = DimletCosts.baseDimensionTickCost + 1000;
                ticksLeft = ticksCost;
                rfPerTick = DimletCosts.baseDimensionCreationCost + 200;
            } else if (isTNT(injectableItemStack)) {
                ticksCost = 600;
                ticksLeft = ticksCost;
                rfPerTick = 10;
            } else {
                DimletKey key = KnownDimletConfiguration.getDimletKey(injectableItemStack);
                Settings settings = KnownDimletConfiguration.getSettings(key);
                ticksCost = DimletCosts.baseDimensionTickCost + settings.getTickCost();
                ticksLeft = ticksCost;
                rfPerTick = DimletCosts.baseDimensionCreationCost + settings.getCreateCost();
            }
        } else {
            int rf = getEnergyStored();
            int rfpt = rfPerTick;
            rfpt = (int) (rfpt * (2.0f - getInfusedFactor()) / 2.0f);

            if (rf >= rfpt) {
                // Enough energy.
                consumeEnergy(rfpt);

                ticksLeft--;
                if (ticksLeft <= 0) {
                    RfToolsDimensionManager dimensionManager = RfToolsDimensionManager.getDimensionManager(getWorld());

                    ItemStack dimensionTab = validateDimensionItemStack();
                    NBTTagCompound tagCompound = dimensionTab.getTagCompound();
                    int id = tagCompound.getInteger("id");

                    injectableItemStack = validateInjectableItemStack();
                    if (isMatterReceiver(injectableItemStack)) {
                        World dimWorld = RfToolsDimensionManager.getWorldForDimension(getWorld(), id);
                        int y = findGoodReceiverLocation(dimWorld);
                        if (y == -1) {
                            y = dimWorld.getHeight() / 2;
                        }
                        Item item = injectableItemStack.getItem();
                        if (item instanceof ItemBlock) {
                            ItemBlock itemBlock = (ItemBlock) item;
                            IBlockState state = itemBlock.getBlock().getStateFromMeta(itemBlock.getMetadata(injectableItemStack));
                            BlockPos pos = new BlockPos(8, y, 8);
                            dimWorld.setBlockState(pos, state, 2);
                            Block block = dimWorld.getBlockState(pos).getBlock();
                            // @todo @@@@@@@@@@@@@@ check if right?
                            block.onBlockActivated(dimWorld, pos, state, FakePlayerFactory.getMinecraft((WorldServer) dimWorld), EnumHand.MAIN_HAND, EnumFacing.DOWN, 0.0F, 0.0F, 0.0F);
                            //                            block.onBlockPlaced(dimWorld, pos, EnumFacing.DOWN, 0, 0, 0, 0, null);
                            block.onBlockPlacedBy(dimWorld, pos, state, null, injectableItemStack);
                            dimWorld.setBlockToAir(pos.up());
                            dimWorld.setBlockToAir(pos.up(2));

                        }
                    } else if (isTNT(injectableItemStack)) {
                        safeDeleteDimension(id, dimensionTab);
                    } else {
                        DimletKey key = KnownDimletConfiguration.getDimletKey(injectableItemStack);

                        DimensionInformation information = dimensionManager.getDimensionInformation(id);
                        information.injectDimlet(key);
                        dimensionManager.save(getWorld());
                    }

                    inventoryHelper.decrStackSize(DimensionEditorContainer.SLOT_INJECTINPUT, 1);

                    stopInjecting();
                }
            }
        }
        markDirty();

        setState();
    }

    private void safeDeleteDimension(int id, ItemStack dimensionTab) {
        World w = DimensionManager.getWorld(id);
        if (w != null) {
            // Dimension is still loaded. Do nothing.
            Broadcaster.broadcast(getWorld(), pos.getX(), pos.getY(), pos.getZ(), "Dimension cannot be deleted. It is still in use!", 10);
            return;
        }
        RfToolsDimensionManager dimensionManager = RfToolsDimensionManager.getDimensionManager(getWorld());
        DimensionInformation information = dimensionManager.getDimensionInformation(id);
        if (information.getOwner() == null) {
            Broadcaster.broadcast(getWorld(), pos.getX(), pos.getY(), pos.getZ(), "You cannot delete a dimension without an owner!", 10);
            return;
        }
        if (getOwnerUUID() == null) {
            Broadcaster.broadcast(getWorld(), pos.getX(), pos.getY(), pos.getZ(), "This machine has no proper owner and cannot delete dimensions!", 10);
            return;
        }
        if (!getOwnerUUID().equals(information.getOwner())) {
            Broadcaster.broadcast(getWorld(), pos.getX(), pos.getY(), pos.getZ(), "This machine's owner differs from the dimensions owner!", 10);
            return;
        }

        RFToolsDim.teleportationManager.removeReceiverDestinations(getWorld(), id);

        dimensionManager.removeDimension(id);
        dimensionManager.reclaimId(id);
        dimensionManager.save(getWorld());

        DimensionStorage dimensionStorage = DimensionStorage.getDimensionStorage(getWorld());
        dimensionStorage.removeDimension(id);
        dimensionStorage.save(getWorld());

        if (GeneralConfiguration.dimensionFolderIsDeletedWithSafeDel) {
            File rootDirectory = DimensionManager.getCurrentSaveRootDirectory();
            try {
                FileUtils.deleteDirectory(new File(rootDirectory.getPath() + File.separator + "RFTOOLS" + id));
                Broadcaster.broadcast(getWorld(), pos.getX(), pos.getY(), pos.getZ(), "Dimension deleted and dimension folder succesfully wiped!", 10);
            } catch (IOException e) {
                Broadcaster.broadcast(getWorld(), pos.getX(), pos.getY(), pos.getZ(), "Dimension deleted but dimension folder could not be completely wiped!", 10);
            }
        } else {
            Broadcaster.broadcast(getWorld(), pos.getX(), pos.getY(), pos.getZ(), "Dimension deleted. Please remove the dimension folder from disk!", 10);
        }

        dimensionTab.getTagCompound().removeTag("id");
        int tickCost = dimensionTab.getTagCompound().getInteger("tickCost");
        dimensionTab.getTagCompound().setInteger("ticksLeft", tickCost);
    }

    private int findGoodReceiverLocation(World dimWorld) {
        int y = WorldGenerationTools.findSuitableEmptySpot(dimWorld, 8, 8);
        y++;
        return y;
    }

    private ItemStack validateInjectableItemStack() {
        ItemStack itemStack = inventoryHelper.getStackInSlot(DimensionEditorContainer.SLOT_INJECTINPUT);
        if (itemStack.isEmpty()) {
            stopInjecting();
            return ItemStack.EMPTY;
        }

        if (isMatterReceiver(itemStack)) {
            return itemStack;
        }
        if (isTNT(itemStack)) {
            return canDeleteDimension(itemStack);
        }

        DimletKey key = KnownDimletConfiguration.getDimletKey(itemStack);
        DimletType type = key.getType();
        IDimletType itype = type.dimletType;
        if (itype.isInjectable()) {
            return itemStack;
        } else {
            return ItemStack.EMPTY;
        }
    }

    private ItemStack canDeleteDimension(ItemStack itemStack) {
        if (!GeneralConfiguration.playersCanDeleteDimensions) {
            Broadcaster.broadcast(getWorld(), pos.getX(), pos.getY(), pos.getZ(), "Players cannot delete dimensions!", 10);
            return ItemStack.EMPTY;
        }
        if (!GeneralConfiguration.editorCanDeleteDimensions) {
            Broadcaster.broadcast(getWorld(), pos.getX(), pos.getY(), pos.getZ(), "Dimension deletion with the editor is not enabled!", 10);
            return ItemStack.EMPTY;
        }
        ItemStack dimensionStack = inventoryHelper.getStackInSlot(DimensionEditorContainer.SLOT_DIMENSIONTARGET);
        if (dimensionStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        NBTTagCompound tagCompound = dimensionStack.getTagCompound();
        int id = tagCompound.getInteger("id");
        if (id == 0) {
            return ItemStack.EMPTY;
        }
        DimensionInformation information = RfToolsDimensionManager.getDimensionManager(getWorld()).getDimensionInformation(id);

        if (getOwnerUUID() != null && getOwnerUUID().equals(information.getOwner())) {
            return itemStack;
        }

        Broadcaster.broadcast(getWorld(), pos.getX(), pos.getY(), pos.getZ(), "This machine's owner differs from the dimensions owner!", 10);
        return ItemStack.EMPTY;
    }

    private boolean isMatterReceiver(ItemStack itemStack) {
        Block block = BlockTools.getBlock(itemStack);
        Block receiver = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("rftools", "matter_receiver"));
        if (block == receiver) {
            // We can inject matter receivers too.
            return true;
        }
        return false;
    }

    private boolean isTNT(ItemStack itemStack) {
        Block block = BlockTools.getBlock(itemStack);
        if (block == Blocks.TNT) {
            // We can inject TNT to destroy a dimension.
            return true;
        }
        return false;
    }

    private ItemStack validateDimensionItemStack() {
        ItemStack itemStack = inventoryHelper.getStackInSlot(DimensionEditorContainer.SLOT_DIMENSIONTARGET);
        if (itemStack.isEmpty()) {
            stopInjecting();
            return ItemStack.EMPTY;
        }

        NBTTagCompound tagCompound = itemStack.getTagCompound();
        int id = tagCompound.getInteger("id");
        if (id == 0) {
            // Not a valid dimension.
            stopInjecting();
            return ItemStack.EMPTY;
        }

        return itemStack;
    }

    private void stopInjecting() {
        setState();
        ticksLeft = -1;
        ticksCost = -1;
        rfPerTick = -1;
        markDirty();
    }

    public DimensionEditorBlock.OperationType getState() {
        return DimensionEditorBlock.OperationType.values()[state];
    }

    private void setState() {
        int oldstate = state;
        state = 0;
        if (ticksLeft == 0) {
            state = 0;
        } else if (ticksLeft == -1) {
            state = 1;
        } else if (((ticksLeft >> 2) & 1) == 0) {
            state = 2;
        } else {
            state = 3;
        }
        if (oldstate != state) {
            markDirtyClient();
        }
    }

    @Override
    public int[] getSlotsForFace(EnumFacing side) {
        return DimensionEditorContainer.factory.getAccessibleSlots();
    }

    @Override
    public boolean canInsertItem(int index, ItemStack itemStackIn, EnumFacing direction) {
        return DimensionEditorContainer.factory.isInputSlot(index);
    }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction) {
        return DimensionEditorContainer.factory.isOutputSlot(index);
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return canPlayerAccess(player);
    }


    // Request the building percentage from the server. This has to be called on the client side.
    public void requestBuildingPercentage() {
        RFToolsDimMessages.INSTANCE.sendToServer(new PacketRequestIntegerFromServer(RFToolsDim.MODID, getPos(),
                CMD_GETEDITING,
                CLIENTCMD_GETEDITING));
    }

    @Override
    public Integer executeWithResultInteger(String command, Map<String, Argument> args) {
        Integer rc = super.executeWithResultInteger(command, args);
        if (rc != null) {
            return rc;
        }
        if (CMD_GETEDITING.equals(command)) {
            if (ticksLeft == -1) {
                return 0;
            } else {
                return (ticksCost - ticksLeft) * 100 / ticksCost;
            }
        }
        return null;
    }

    @Override
    public boolean execute(String command, Integer result) {
        boolean rc = super.execute(command, result);
        if (rc) {
            return true;
        }
        if (CLIENTCMD_GETEDITING.equals(command)) {
            editPercentage = result;
            return true;
        }
        return false;
    }

    public static int getEditPercentage() {
        return editPercentage;
    }

    @Override
    public void readFromNBT(NBTTagCompound tagCompound) {
        super.readFromNBT(tagCompound);
        state = tagCompound.getByte("state");
    }

    @Override
    public void readRestorableFromNBT(NBTTagCompound tagCompound) {
        super.readRestorableFromNBT(tagCompound);
        readBufferFromNBT(tagCompound, inventoryHelper);
        ticksLeft = tagCompound.getInteger("ticksLeft");
        ticksCost = tagCompound.getInteger("ticksCost");
        rfPerTick = tagCompound.getInteger("rfPerTick");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound) {
        super.writeToNBT(tagCompound);
        tagCompound.setByte("state", (byte) state);
        return tagCompound;
    }

    @Override
    public void writeRestorableToNBT(NBTTagCompound tagCompound) {
        super.writeRestorableToNBT(tagCompound);
        writeBufferToNBT(tagCompound, inventoryHelper);
        tagCompound.setInteger("ticksLeft", ticksLeft);
        tagCompound.setInteger("ticksCost", ticksCost);
        tagCompound.setInteger("rfPerTick", rfPerTick);
    }
}
