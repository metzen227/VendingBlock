package info.jbcs.minecraft.vending.gui.hud;

import com.kamildanak.minecraft.enderpay.api.EnderPayApi;
import com.kamildanak.minecraft.foamflower.gui.elements.GuiElement;
import com.kamildanak.minecraft.foamflower.gui.elements.GuiItemsList;
import com.kamildanak.minecraft.foamflower.gui.elements.GuiLabel;
import com.kamildanak.minecraft.foamflower.gui.elements.GuiLabelCarousel;
import com.kamildanak.minecraft.foamflower.gui.hud.HUD;
import com.kamildanak.minecraft.foamflower.gui.layouts.CenteredLayout;
import com.kamildanak.minecraft.foamflower.gui.layouts.LinearLayout;
import info.jbcs.minecraft.vending.Utils;
import info.jbcs.minecraft.vending.Vending;
import info.jbcs.minecraft.vending.inventory.InventoryVendingMachineEnderPay;
import info.jbcs.minecraft.vending.tileentity.TileEntityVendingMachine;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.Vector;

import static info.jbcs.minecraft.vending.General.countNotNull;


public class HintHUD extends HUD {
    private Minecraft mc;
    private GuiElement root;
    private LinearLayout layout;
    private LinearLayout boughtAndSold;
    private LinearLayout bought;
    private LinearLayout sold;
    private LinearLayout boughtItems;
    private LinearLayout soldItems;
    private GuiLabel labelBoughtCredits;
    private GuiLabel labelSoldCredits;
    private GuiLabel labelSeller;
    private GuiLabel labelClosed;
    private GuiLabel labelSelling;
    private GuiLabel labelFor;
    private GuiLabelCarousel labelBoughtDesc;
    private GuiLabelCarousel labelSoldDesc;
    private GuiItemsList soldItemList;
    private GuiItemsList boughtItemList;

    public HintHUD(Minecraft mc) {
        super(mc);
        this.mc = mc;
        ScaledResolution resolution = new ScaledResolution(mc);
        root = new CenteredLayout(0, 0, resolution.getScaledWidth(), resolution.getScaledHeight(), true, false);
        root.gui = this;
        root.addChild(layout = new LinearLayout(0, 0, false));
        layout.addChild(labelSeller = new GuiLabel(0, 0, "<seller>", 0xffffff));
        layout.addChild(labelClosed = new GuiLabel(0, 0, "gui.vendingBlock.closed", 0xa0a0a0));

        layout.addChild(boughtAndSold = new LinearLayout(0, 0, false));
        boughtAndSold.addChild(sold = new LinearLayout(0, 0, false));
        boughtAndSold.addChild(bought = new LinearLayout(0, 0, false));

        sold.addChild(soldItems = new LinearLayout(0, 0, true));
        soldItems.addChild(labelSelling = new GuiLabel(0, 0, "gui.vendingBlock.isSelling", 0xa0a0a0));
        soldItems.addChild(soldItemList = new GuiItemsList(0, 0, 0, 0));
        sold.addChild(labelSoldDesc = new GuiLabelCarousel(0, 0, "", 0xa0a0a0));
        sold.addChild(labelSoldCredits = new GuiLabel(0, 0, "<sCredits>", 0xa0a0a0));
        bought.addChild(boughtItems = new LinearLayout(0, 0, true));
        boughtItems.addChild(labelFor = new GuiLabel(0, 0, "gui.vendingBlock.for", 0xa0a0a0));
        boughtItems.addChild(boughtItemList = new GuiItemsList(0, 0, 0, 0));
        bought.addChild(labelBoughtDesc = new GuiLabelCarousel(0, 0, "", 0xa0a0a0));
        bought.addChild(labelBoughtCredits = new GuiLabel(0, 0, "<bCredits>", 0xa0a0a0));
    }

    @Override
    public GuiElement getRoot() {
        return root;
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    @SuppressWarnings("unused")
    public void onRenderInfo(RenderGameOverlayEvent.Post event) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        if (event.isCancelable() || event.getType() != RenderGameOverlayEvent.ElementType.EXPERIENCE) return;

        if (mc == null || mc.player == null || mc.world == null) return;
        RayTraceResult mop = this.mc.objectMouseOver;
        if (mop == null || mop.typeOfHit != RayTraceResult.Type.BLOCK) return;
        IBlockState blockState = mc.world.getBlockState(mop.getBlockPos());
        if (!blockState.getBlock().hasTileEntity(blockState)) return;
        TileEntity te = mc.world.getTileEntity(mop.getBlockPos());
        if (te == null) return;
        if (!(mc.world.getTileEntity(mop.getBlockPos()) instanceof TileEntityVendingMachine)) return;

        ScaledResolution resolution = new ScaledResolution(mc);
        root.h = resolution.getScaledHeight();
        root.w = resolution.getScaledWidth();

        boughtAndSold.setHorizontal(mc.player.isSneaking());

        TileEntityVendingMachine tileEntity = (TileEntityVendingMachine) te;
        root.y = Vending.settings.getOffsetY();
        labelSeller.setCaption(tileEntity.getOwnerName());
        labelSeller.center = true;

        boolean isOpened = tileEntity.isOpen();
        if (Loader.isModLoaded("enderpay")) {
            if (isOpened && !tileEntity.isInfinite()) {
                if (tileEntity.inventory instanceof InventoryVendingMachineEnderPay) {
                    long soldSum = ((InventoryVendingMachineEnderPay) tileEntity.inventory).soldCreditsSum();
                    long realTotalSum = ((InventoryVendingMachineEnderPay) tileEntity.inventory).realTotalCreditsSum();
                    isOpened = soldSum <= realTotalSum;
                }
                if (!isOpened) labelClosed.setCaption("gui.vendingBlock.shopNotEnoughCredits");
                else if (tileEntity.inventory instanceof InventoryVendingMachineEnderPay)
                {
                    isOpened = !(((InventoryVendingMachineEnderPay) tileEntity.inventory).boughtCreditsSum() > 0
                            && !((InventoryVendingMachineEnderPay) tileEntity.inventory).hasBanknoteInStorage());
                    if (!isOpened) labelClosed.setCaption("gui.vendingBlock.banknoteInStorageRequiredToAcceptPayments");
                }
            } else {
                labelClosed.setCaption("gui.vendingBlock.closed");
            }
        }
        labelClosed.hidden = isOpened;
        boughtAndSold.hidden = !isOpened;


        //labelClosed.hidden = true;
        //boughtAndSold.hidden = true;

        NonNullList<ItemStack> soldItemStacks = NonNullList.create();
        NonNullList<ItemStack> boughtItemStacks = NonNullList.create();
        soldItemStacks.addAll(tileEntity.inventory.getSoldItems());
        boughtItemStacks.addAll(tileEntity.inventory.getBoughtItems());
        if (Loader.isModLoaded("enderpay")) {
            long amountSold = 0;
            long amountBought = 0;
            if (tileEntity.inventory instanceof InventoryVendingMachineEnderPay) {
                amountSold = ((InventoryVendingMachineEnderPay) tileEntity.inventory).soldCreditsSum();
                amountBought = ((InventoryVendingMachineEnderPay) tileEntity.inventory).boughtCreditsSum();
            }

            String label = countNotNull(soldItemStacks) == 0 ?
                    (amountBought == 0 && countNotNull(boughtItemStacks) == 0 ?
                            "gui.vendingBlock.isGivingAway" : "gui.vendingBlock.isSelling") : "gui.vendingBlock.and";
            String amountStr = I18n.format(label).trim() + " " + Utils.format(amountSold) + " " + getCurrencyName(amountSold);
            labelSoldCredits.setCaption(amountStr);
            labelSoldCredits.hidden = amountSold == 0;

            label = countNotNull(boughtItemStacks) == 0 ?
                    (amountSold == 0 && countNotNull(soldItemStacks) == 0 ?
                            "gui.vendingBlock.isAccepting" : "gui.vendingBlock.for") : "gui.vendingBlock.and";
            amountStr = I18n.format(label).trim() + " " + Utils.format(amountBought) + " " + getCurrencyName(amountBought);
            labelBoughtCredits.setCaption(amountStr);
            labelBoughtCredits.hidden = amountBought == 0;
        }

        soldItems.hidden = countNotNull(soldItemStacks) == 0;
        boughtItems.hidden = countNotNull(boughtItemStacks) == 0;
        layout.hidden = soldItems.hidden && boughtItems.hidden && labelBoughtCredits.hidden && labelSoldCredits.hidden;
        soldItemList.setItems(soldItemStacks);
        boughtItemList.setItems(boughtItemStacks);
        String tooltip;
        labelSoldDesc.hidden = !mc.player.isSneaking();
        labelBoughtDesc.hidden = !mc.player.isSneaking();
        if (mc.player.isSneaking()) {
            labelBoughtDesc.setCaption(getTooltips(boughtItemStacks));
            labelSoldDesc.setCaption(getTooltips(soldItemStacks));
        }

        labelSelling.setCaption(boughtItems.hidden && labelBoughtCredits.hidden ? "gui.vendingBlock.isGivingAway" : "gui.vendingBlock.isSelling");
        labelFor.setCaption(soldItems.hidden && labelSoldCredits.hidden ? "gui.vendingBlock.isAccepting" : "gui.vendingBlock.for");

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        if (!layout.hidden)
            drawGradientRect(layout.x - 6, layout.y - 5,
                    layout.x + layout.getWidth() + 6, layout.y + layout.getHeight() + 5 - 2,
                    0xc0101010, 0xd0101010);


        if (!Loader.isModLoaded("enderpay")) {
            labelSoldCredits.hidden = true;
            labelBoughtCredits.hidden = true;
        }
        super.render();

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        Utils.bind("textures/gui/icons.png");
    }

    private String[] getTooltips(NonNullList<ItemStack> itemStacks) {
        Vector<String> tooltips = new Vector<>();
        for (ItemStack stack : itemStacks) {
            if (stack.isEmpty()) continue;
            StringBuilder tooltip = new StringBuilder();
            for (int i = 0; i < stack.getTooltip(mc.player, () -> false).size(); i++) {
                if (i != 0) tooltip.append("\n");
                tooltip.append(stack.getTooltip(mc.player, () -> false).get(i));
            }
            tooltips.add(tooltip.toString());
        }
        return tooltips.toArray(new String[tooltips.size()]);
    }

    @Optional.Method(modid = "enderpay")
    private String getCurrencyName(long amount) {
        return EnderPayApi.getCurrencyName(amount);
    }
}
