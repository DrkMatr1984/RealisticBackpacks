package org.fatecrafters.plugins.versions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import net.minecraft.server.v1_4_R1.NBTBase;
import net.minecraft.server.v1_4_R1.NBTTagCompound;
import net.minecraft.server.v1_4_R1.NBTTagList;

import org.bukkit.craftbukkit.v1_4_R1.inventory.CraftInventoryCustom;
import org.bukkit.craftbukkit.v1_4_R1.inventory.CraftItemStack;
import org.bukkit.inventory.Inventory;
import org.fatecrafters.plugins.RBInterface;
import org.fatecrafters.plugins.RealisticBackpacks;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

public class v1_4_R1 implements RBInterface {

	RealisticBackpacks plugin;

	public v1_4_R1(final RealisticBackpacks rb) {
		this.plugin = rb;
	}

	@Override
	public String inventoryToString(final Inventory inventory) {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		final DataOutputStream dataOutput = new DataOutputStream(outputStream);
		final NBTTagList itemList = new NBTTagList();
		for (int i = 0; i < inventory.getSize(); i++) {
			final NBTTagCompound outputObject = new NBTTagCompound();
			CraftItemStack craft = null;
			final org.bukkit.inventory.ItemStack is = inventory.getItem(i);
			if (is instanceof CraftItemStack) {
				craft = (CraftItemStack) is;
			} else if (is != null) {
				craft = CraftItemStack.asCraftCopy(is);
			} else {
				craft = null;
			}
			if (craft != null) {
				CraftItemStack.asNMSCopy(craft).save(outputObject);
			}
			itemList.add(outputObject);
		}
		NBTBase.a(itemList, dataOutput);
		return Base64Coder.encodeLines(outputStream.toByteArray());
	}

	@Override
	public Inventory stringToInventory(final String data, final String name) {
		final ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
		final NBTTagList itemList = (NBTTagList) NBTBase.b(new DataInputStream(inputStream));
		final Inventory inventory = new CraftInventoryCustom(null, itemList.size());

		for (int i = 0; i < itemList.size(); i++) {
			final NBTTagCompound inputObject = (NBTTagCompound) itemList.get(i);
			if (!inputObject.isEmpty()) {
				inventory.setItem(i, CraftItemStack.asCraftMirror(net.minecraft.server.v1_4_R1.ItemStack.createStack(inputObject)));
			}
		}
		return inventory;
	}

}
