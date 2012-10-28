package com.amoebaman.engrave;

import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.material.Dye;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class Engraving {

	private Location loc;
	private Material block, tool;
	private String pureMessage, message;
	private int damage;
	private String color;
	
	public Engraving(Block block, ItemStack item, String message){
		loc = block.getLocation();
		this.block = block.getType();
		pureMessage = message;
		this.message = message;
		damage = 0;
		tool = item.getType();
		if(tool == Material.INK_SACK)
			color = Engrave.dyeToChatColor(((Dye) item.getData()).getColor()).toString();
		else
			color = ChatColor.GRAY.toString();
	}
	
	public Engraving(ConfigurationSection section){
		if(section == null)
			return;
		loc = S_Location.stringLoad(section.getName());
		block = Material.matchMaterial(section.getString("block"));
		tool = Material.matchMaterial(section.getString("tool"));
		pureMessage = section.getString("pureMessage");
		message = section.getString("message").replaceAll("'", "");
		damage = section.getInt("damage");
		color = section.getString("color");
	}
	
	public void save(ConfigurationSection container){
		if(message.trim().equals("")){
			container.set(S_Location.stringSave(loc), null);
			return;
		}
		ConfigurationSection section = container.createSection(S_Location.stringSave(loc));
		section.set("block", block.name());
		section.set("tool", tool.name());
		section.set("pureMessage", pureMessage);
		section.set("message", "'" + message.replaceAll("'", "`") + "'");
		section.set("damage", damage);
		section.set("color", color);
	}
	
	public Block getBlock(){ return loc.getBlock(); }
	public Material getMat(){ return block; }
	public Material getTool(){ return tool; }
	public double getDurability(){ return Math.pow(ConfigHandler.getBlockDurability(block), 1.5) * ConfigHandler.getToolQuality(tool); }
	public int getDamage(){ return damage; }
	public String getPureMessage(){ return pureMessage; }
	public String getMessage(){ return color + message; }
	public boolean isNull(){ return loc == null; }
	
	public boolean degrade(int damage){
		double chance = damage / getDurability();
		String newMessage = "";
		for(char letter : message.toCharArray()){
			if(Math.random() < chance){
				if(Math.random() > 0.3 && letter != ' ')
					newMessage += (char)(new Random().nextInt(93) + 33);
				else
					newMessage += " ";
			}
			else
				newMessage += letter;
		}
		boolean toReturn = !message.equals(newMessage);
		message = newMessage;
		return toReturn;
	}
	
}
