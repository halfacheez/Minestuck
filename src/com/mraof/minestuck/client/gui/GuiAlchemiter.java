package com.mraof.minestuck.client.gui;

import java.util.List;

import com.mraof.minestuck.MinestuckConfig;
import com.mraof.minestuck.block.MinestuckBlocks;
import com.mraof.minestuck.client.util.GuiUtil;
import com.mraof.minestuck.item.MinestuckItems;
import com.mraof.minestuck.network.MinestuckChannelHandler;
import com.mraof.minestuck.network.MinestuckPacket;
import com.mraof.minestuck.network.MinestuckPacket.Type;
import com.mraof.minestuck.tileentity.TileEntityAlchemiter;
import com.mraof.minestuck.util.AlchemyRecipeHandler;
import com.mraof.minestuck.util.GristAmount;
import com.mraof.minestuck.util.GristRegistry;
import com.mraof.minestuck.util.GristSet;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class GuiAlchemiter extends GuiScreen  {
	
	
		private static final ResourceLocation guiBackground = new ResourceLocation("minestuck", "textures/gui/large_alchemiter.png");
		private static final int guiWidth = 159, guiHeight = 102;
		private TileEntityAlchemiter alchemiter;
		private int itemQuantity;
		
		public GuiAlchemiter(TileEntityAlchemiter te) {
			alchemiter = te;
			itemQuantity=1;
		}

		
		@Override
		public void initGui()
		{
			GuiButton alchemize = new GuiButton(0, (width-100)/2,(height-guiHeight)/2+110, 100, 20, "ALCHEMIZE");

			GuiButton hundrids_up = new GuiButton(1,(width-guiWidth)/2+52,(height-guiHeight)/2+10,18,18,"^");
			GuiButton tens_up = new GuiButton(2,(width-guiWidth)/2+31,(height-guiHeight)/2+10,18,18,"^");
			GuiButton ones_up = new GuiButton(3,(width-guiWidth)/2+10,(height-guiHeight)/2+10,18,18,"^");
			GuiButton hundrids_down = new GuiButton(4,(width-guiWidth)/2+52,(height-guiHeight)/2+74,18,18,"v");
			GuiButton tens_down =new GuiButton(5,(width-guiWidth)/2+31,(height-guiHeight)/2+74,18,18,"v");
			GuiButton ones_down = new GuiButton(6,(width-guiWidth)/2+10,(height-guiHeight)/2+74,18,18,"v");
			//GuiLabel ones = new GuiLabel(fontRendererObj, p_i45540_2_, p_i45540_3_, p_i45540_4_, p_i45540_5_, p_i45540_6_, p_i45540_7_)
			
			buttonList.add(alchemize);
			buttonList.add(ones_up);
			buttonList.add(tens_up);
			buttonList.add(hundrids_up);
			buttonList.add(ones_down);
			buttonList.add(tens_down);
			buttonList.add(hundrids_down);
			
			
		}
		
		@Override
		public void drawScreen(int mouseX, int mouseY, float partialTicks)
		{
			int xOffset = (width - guiWidth)/2;
			int yOffset = (height - guiHeight)/2;
			
			this.drawDefaultBackground();	
			
			
			
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

			this.mc.getTextureManager().bindTexture(guiBackground);
			this.drawTexturedModalRect(xOffset, yOffset, 0, 0, guiWidth, guiHeight);
			
			mc.fontRenderer.drawString(Integer.toString(((int)(itemQuantity/Math.pow(10,2))%10)), (width-guiWidth)/2+15,(height-guiHeight)/2+46, 16777215);
			mc.fontRenderer.drawString(Integer.toString(((int)(itemQuantity/Math.pow(10,1))%10)), (width-guiWidth)/2+36,(height-guiHeight)/2+46, 16777215);
			mc.fontRenderer.drawString(Integer.toString(((int)(itemQuantity/Math.pow(10,0))%10)), (width-guiWidth)/2+57,(height-guiHeight)/2+46, 16777215);
			
			//Render grist requirements
			ItemStack stack = AlchemyRecipeHandler.getDecodedItem(alchemiter.getDowel());
			if( !(alchemiter.getDowel().hasTagCompound() && alchemiter.getDowel().getTagCompound().hasKey("contentID")))
				stack = new ItemStack(MinestuckBlocks.genericObject);

			GristSet set = GristRegistry.getGristConversion(stack);
			boolean useSelectedType = stack.getItem() == MinestuckItems.captchaCard;
			if (useSelectedType)
				set = new GristSet(alchemiter.selectedGrist, MinestuckConfig.clientCardCost);
			if (set != null && stack.isItemDamaged())
			{
				float multiplier = 1 - stack.getItem().getDamage(stack) / ((float) stack.getMaxDamage());
				for (GristAmount amount : set.getArray())
				{
					set.setGrist(amount.getType(), (int)( Math.ceil(amount.getAmount() * multiplier)));
				}
				
			}
			for (GristAmount amount : set.getArray())
			{
				set.setGrist(amount.getType(), amount.getAmount()*itemQuantity);
			}
			
			GuiUtil.drawGristBoard(set, useSelectedType ? GuiUtil.GristboardMode.LARGE_ALCHEMITER_SELECT : GuiUtil.GristboardMode.LARGE_ALCHEMITER, (width-guiWidth)/2+88,(height-guiHeight)/2+13, fontRenderer);
			
			List<String> tooltip = GuiUtil.getGristboardTooltip(set, mouseX , mouseY , 9, 45, fontRenderer);
			if (tooltip != null)
				this.drawHoveringText(tooltip, mouseX , mouseY , fontRenderer);
			super.drawScreen(mouseX, mouseY, partialTicks);
		}
		
		
		@Override
		public boolean doesGuiPauseGame() {
			return false;
		}
		

		
		
		@Override
		protected void actionPerformed(GuiButton button)
		{
		
			if(button.id==0) {

				MinestuckPacket packet = MinestuckPacket.makePacket(Type.ALCHEMITER_PACKET,alchemiter,itemQuantity);
				MinestuckChannelHandler.sendToServer(packet);
				this.mc.displayGuiScreen(null);

			}
			else if(button.id<=3){
				if((int)(itemQuantity/Math.pow(10, button.id-1))%10!=9) {
					itemQuantity+=Math.pow(10,button.id-1);
				}
			}
			else {
				if((int)(itemQuantity/Math.pow(10, button.id-4))%10!=0) {
					itemQuantity-=Math.pow(10,button.id-4);
				}
			}
			
			
		}
		/*
		@Override
		public void onGuiClosed()
		{
			if(firstTime && mc != null && mc.player != null)
			{
				ITextComponent message;
				if(ColorCollector.playerColor == -1)
					message = new TextComponentTranslation("message.selectDefaultColor");
				else message = new TextComponentTranslation("message.selectColor");
				this.mc.player.sendMessage(new TextComponentString("[Minestuck] ").appendSibling(message));
			}
		}
		
	}*/

}