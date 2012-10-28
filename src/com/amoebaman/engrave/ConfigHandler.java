package com.amoebaman.engrave;

import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

public class ConfigHandler {

	private static ConfigurationSection root;
	private static HashMap<Material, Integer> blockDurability;
	private static HashMap<Material, Integer> toolQuality;
	public static void init(ConfigurationSection instance){
		root = instance;
		
		if(root.getConfigurationSection("block-durability") == null)
			root.createSection("block-durability");
		ConfigurationSection temp = root.getConfigurationSection("block-durability");
		blockDurability = new HashMap<Material, Integer>();
		for(String key : temp.getKeys(false))
			blockDurability.put(getMat(key), temp.getInt(key));
		
		if(root.getConfigurationSection("tool-quality") == null)
			root.createSection("tool-quality");
		temp = root.getConfigurationSection("tool-quality");
		toolQuality = new HashMap<Material, Integer>();
		for(String key : temp.getKeys(false))
			toolQuality.put(getMat(key), temp.getInt(key));
	}
	
	public static int getBlockDurability(Material mat){
		if(blockDurability.containsKey(mat))
			return blockDurability.get(mat);
		return -1;
	}
	
	public static int getToolQuality(Material mat){
		if(toolQuality.containsKey(mat))
			return toolQuality.get(mat);
		return 0;
	}
	
	public static int getCheckFrequency(){
		return 20 / root.getInt("visibility-checks-per-second", 2);
	}
	
	public static int getEngravingDamage(){
		return root.getInt("engraving-tool-damage", 75);
	}
	
	public static int getNoticeRange(){
		return root.getInt("notice-range", 10);
	}
	
	public static int getReadRange(){
		return root.getInt("read-range", 10);
	}
	
	public static String getShieldWord(){
		return root.getString("shield-word", "elbereth");
	}
	
	public static Material getMat(String name) {
		name = name.toUpperCase().replace(' ', '_');
		Material mat = null;
		if(mat == null){
			try{ mat = Material.getMaterial(Integer.parseInt(name)); }
			catch(NumberFormatException e){}
		}
		if(mat == null)
			mat = Material.matchMaterial(name);
		return mat;
	}
	
}
