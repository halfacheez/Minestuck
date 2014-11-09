package com.mraof.minestuck.inventory.captchalouge;

import java.util.HashMap;
import java.util.Random;

import com.mraof.minestuck.Minestuck;
import com.mraof.minestuck.client.ClientProxy;
import com.mraof.minestuck.network.CaptchaDeckPacket;
import com.mraof.minestuck.network.MinestuckChannelHandler;
import com.mraof.minestuck.network.MinestuckPacket;
import com.mraof.minestuck.util.AlchemyRecipeHandler;
import com.mraof.minestuck.util.Debug;
import com.mraof.minestuck.util.UsernameHandler;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class CaptchaDeckHandler
{
	
	public static enum ModusType
	{
		STACK(StackModus.class),
		QUEUE(QueueModus.class);
		
		private final Class<? extends Modus> c;
		ModusType(Class<? extends Modus> c)
		{
			this.c = c;
		}
		
		public Modus createInstance()
		{
			try
			{
				return c.newInstance();
			} catch (Exception e)
			{
				e.printStackTrace();
				return null;
			}
		}
		
		public static ModusType getType(Modus modus)
		{
			for(ModusType type : values())
				if(type.c == modus.getClass())
					return type;
			return null;
		}
		
	}
	
	public static Random rand;
	
	@SideOnly(Side.CLIENT)
	public static Modus clientSideModus;
	
	public static HashMap<String, Modus> playerMap = new HashMap<String, Modus>();
	
	public static void launchItem(EntityPlayer player, ItemStack item)
	{
		boolean b = true;
		if(item.getItem().equals(Minestuck.captchaCard) && (!item.hasTagCompound() || !item.getTagCompound().hasKey("contentID")))
			b = !playerMap.get(UsernameHandler.encode(player.getCommandSenderName())).increaseSize();
		if(b)
			launchAnyItem(player, item);
	}
	
	public static void launchAnyItem(EntityPlayer player, ItemStack item)
	{
		EntityItem entity = new EntityItem(player.worldObj, player.posX, player.posY+1, player.posZ, item);
		entity.motionX = rand.nextDouble() - 0.5;
		entity.motionZ = rand.nextDouble() - 0.5;
		entity.delayBeforeCanPickup = 10;
		player.worldObj.spawnEntityInWorld(entity);
	}
	
	public static void useItem(EntityPlayerMP player)
	{
		if(!(player.openContainer instanceof ContainerCaptchaDeck))
			return;
		ContainerCaptchaDeck container = (ContainerCaptchaDeck) player.openContainer;
		if(container.inventory.getStackInSlot(0) == null)
			return;
		ItemStack item = container.inventory.getStackInSlot(0);
		Modus modus = playerMap.get(UsernameHandler.encode(player.getCommandSenderName()));
		if(item.getItem().equals(Minestuck.captchaModus) && ModusType.values().length > item.getItemDamage())
		{
			if(modus == null)
			{
				modus = ModusType.values()[item.getItemDamage()].createInstance();
				modus.player = player;
				modus.initModus(null);
				playerMap.put(UsernameHandler.encode(player.getCommandSenderName()), modus);
				container.inventory.setInventorySlotContents(0, null);
			}
			else
			{
				Modus oldModus = modus;
				ModusType oldType = ModusType.getType(oldModus);
				if(oldType.ordinal() == item.getItemDamage())
					return;
				modus = ModusType.values()[item.getItemDamage()].createInstance();
				modus.player = player;
				if(modus.canSwitchFrom(oldType))
					modus.initModus(oldModus.getItems());
				else
				{
					for(ItemStack content : oldModus.getItems())
						launchAnyItem(player, content);
					modus.initModus(null);
				}
				
				playerMap.put(UsernameHandler.encode(player.getCommandSenderName()), modus);
				item.setItemDamage(oldType.ordinal());
			}
			
		}
		else if(item.getItem().equals(Minestuck.captchaCard) && (!item.hasTagCompound() || !item.getTagCompound().getBoolean("punched"))
				&& modus != null)
		{
			ItemStack content = AlchemyRecipeHandler.getDecodedItem(item, false);
			if(!modus.increaseSize())
				return;
			container.inventory.setInventorySlotContents(0, null);
			if(content != null && !modus.putItemStack(content))
				launchItem(player, content);
		}
		
		container.detectAndSendChanges();
		MinestuckPacket packet = MinestuckPacket.makePacket(MinestuckPacket.Type.CAPTCHA, CaptchaDeckPacket.DATA, modus.writeToNBT(new NBTTagCompound()));
		MinestuckChannelHandler.sendToPlayer(packet, player);
	}
	
	public static void captchalougeItem(EntityPlayerMP player)
	{
		ItemStack item = player.getCurrentEquippedItem();
		Modus modus = playerMap.get(UsernameHandler.encode(player.getCommandSenderName()));
		if(modus != null && item != null && modus.putItemStack(item))
		{
			player.setCurrentItemOrArmor(0, null);
			player.inventoryContainer.detectAndSendChanges();
		}
	}
	
	public static void getItem(EntityPlayerMP player, int index)
	{
		Modus modus = playerMap.get(UsernameHandler.encode(player.getCommandSenderName()));
		if(modus == null)
			return;
		ItemStack stack = modus.getItem(index);
		if(stack != null)
		{
			if(player.getCurrentEquippedItem() == null)
				player.setCurrentItemOrArmor(0, stack);
			else
			{
				boolean placed = false;
				for(int i = 0; i < player.inventory.mainInventory.length; i++)
				{
					if(player.inventory.mainInventory[i] == null)
					{
						player.inventory.mainInventory[i] = stack;
						placed = true;
						break;
					}
				}
				if(!placed)
					launchAnyItem(player, stack);
			}
			player.inventoryContainer.detectAndSendChanges();
		}
	}
	
	public static NBTTagCompound writeToNBT(Modus modus)
	{
		int index = ModusType.getType(modus).ordinal();
		NBTTagCompound nbt = modus.writeToNBT(new NBTTagCompound());
		nbt.setInteger("type", index);
		return nbt;
	}
	
	public static Modus readFromNBT(NBTTagCompound nbt, boolean clientSide)
	{
		Modus modus;
		if(clientSide && clientSideModus != null && nbt.getInteger("type") == ModusType.getType(clientSideModus).ordinal())
			modus = clientSideModus;
		else
		{
			modus = ModusType.values()[nbt.getInteger("type")].createInstance();
			if(clientSide)
				modus.player = ClientProxy.getPlayer();
		}
		modus.readFromNBT(nbt);
		return modus;
	}
	
}
