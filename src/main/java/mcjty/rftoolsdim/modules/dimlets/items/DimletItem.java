package mcjty.rftoolsdim.modules.dimlets.items;

import mcjty.lib.builder.TooltipBuilder;
import mcjty.lib.tooltips.ITooltipSettings;
import mcjty.rftoolsdim.setup.Registration;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

import java.util.List;

import static mcjty.lib.builder.TooltipBuilder.*;

public class DimletItem extends Item implements ITooltipSettings {

    private final TooltipBuilder tooltipBuilder = new TooltipBuilder()
            .info(key("message.rftoolsdim.shiftmessage"))
            .infoShift(header(), gold(), TooltipBuilder.parameter("key", DimletItem::isReady, DimletItem::getDescription));

    public DimletItem() {
        super(Registration.createStandardProperties());
    }

    public static boolean isReady(ItemStack stack) {
        // @todo return true if this dimlet is ready
        return false;
    }

    public static String getDescription(ItemStack stack) {
        // @todo
        return "";
    }

    @Override
    public void addInformation(ItemStack itemStack, World world, List<ITextComponent> list, ITooltipFlag flags) {
        super.addInformation(itemStack, world, list, flags);
        tooltipBuilder.makeTooltip(getRegistryName(), itemStack, list, flags);
    }
}
