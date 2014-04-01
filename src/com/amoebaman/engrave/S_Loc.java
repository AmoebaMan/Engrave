package com.amoebaman.engrave;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Contains static methods for converting and reverting {@link Location}s to and from easily serializable
 * and saveable forms.
 * 
 * @author AmoebaMan
 */
public class S_Loc{
	
	/**
	 * Saves a location into a {@link ConfigurationSection}, optionally including the rotational data.
	 * <br><br>
	 * Rotational data can take up quite a bit more room than just coordinates, and isn't even
	 * used unless you're teleporting a player to a location.  If you're not storing a location
	 * with teleportation in mind, you should probably not include rotation data.
	 * <br><br>
	 * After saving a location using this method, it can be loaded by using {@link #configLoad(ConfigurationSection)}.
	 * 
	 * @param loc a location
	 * @param config the configuration section to store the location in
	 * @param includeRotation whether or not to include the yaw and pitch data
	 */
	public static void configSave(Location loc, ConfigurationSection config, boolean includeRotation){
		config.set("world", loc.getWorld().getName());
		config.set("x", loc.getX());
		config.set("y", loc.getY());
		config.set("z", loc.getZ());
		if(includeRotation){
			config.set("yaw", loc.getYaw());
			config.set("pitch", loc.getPitch());
		}
	}
	
	/**
	 * Loads a previously-saved location from a {@link ConfigurationSection}.  If any portions of the location's
	 * data are missing from the config, they'll be replaced by their respective components from the location
	 * world@(0.5, 64.5, 0.5, 0.0, 0.0).
	 * 
	 * @param config a configuration section
	 * @return the location previously stored
	 */
	public static Location configLoad(ConfigurationSection config){
		if(config == null)
			return new Location(Bukkit.getWorlds().get(0), 0.5, 64.5, 0.5);
		return new Location(Bukkit.getWorld(config.getString("world", "world")), config.getDouble("x", 0.5), config.getDouble("y", 64.5), config.getDouble("z", 0.5), (float)config.getDouble("yaw", 0.0), (float)config.getDouble("pitch", 0.0));
	}
	
	/**
	 * Compresses a {@link Location} into a String, allowing it to be easily saved and recalled later, optionally including the rotational data.
	 * <br><br>
	 * Rotational data can take up quite a bit more room than just coordinates, and isn't even
	 * used unless you're teleporting a player to a location.  If you're not storing a location
	 * with teleportation in mind, you should probably not include rotation data.
	 * <br><br>
	 * The saved string will be of the format <code>[world]@([x],[y],[z],[yaw],[pitch])</code>.
	 * <br><br>
	 * After saving a location using this method, it can be loaded by using {@link #stringLoad(Location)}
	 * 
	 * @param loc a location
	 * @param round whether or not to snap to even coordinates
	 * @param rotation whether or not to include the yaw and pitch data
	 * @return a string containing the location's data
	 */
	public static String stringSave(Location loc, boolean round, boolean rotation){
		if(loc == null)
			return null;
		String str = loc.getWorld().getName() + "@" + (round ? loc.getBlockX() + 0.5 : loc.getX()) + "," + (round ? loc.getBlockY() : loc.getY()) + "," + (round ? loc.getBlockZ() + 0.5 : loc.getZ());
		if(rotation)
			str += "," + (round ? Math.round(loc.getYaw() * 22.5) / 22.5 : loc.getYaw()) + "," + (round ? Math.round(loc.getPitch() * 22.5) / 22.5 : loc.getPitch());
		return str;
	}
	
	/**
	 * Loads a previously-saved location from a string saved using {@link #stringSave(Location, boolean)}.  If the string
	 * is improperly formatted, this method will return null;
	 * 
	 * @param str a location string
	 * @param round whether or not to automatically snap the location to block centers (usually for nice teleportation)
	 * @return the location saved, or null if the string is improperly formatted
	 */
	public static Location stringLoad(String str){
		if(str == null)
			return null;
		try{
			String[] split = str.split("@");
			World world = Bukkit.getWorld(split[0]);
			String[] coords = split[1].split(",");
			Location toReturn = new Location(world, Double.parseDouble(coords[0]), Double.parseDouble(coords[1]), Double.parseDouble(coords[2]));
			if(coords.length > 3){
				toReturn.setYaw(Float.parseFloat(coords[3]));
				toReturn.setPitch(Float.parseFloat(coords[4]));
			}
			return toReturn;	
		}
		catch(Exception e){
			Bukkit.getLogger().severe("Was unable to parse Location from String: " + str);
			return null;
		}
	}
	
	/**
	 * Gets a nice, friendly bit of text to represent a {@link Location}.  The string is formatted as
	 * "([x], [y], [z]) in [world]".
	 * 
	 * @param loc a location
	 * @return a friendly string
	 */
	public static String toString(Location loc){
		return "(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ") in " + loc.getWorld().getName();
	}
}
