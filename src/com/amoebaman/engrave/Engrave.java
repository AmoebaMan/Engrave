package com.amoebaman.engrave;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Dye;
import org.bukkit.plugin.java.JavaPlugin;

public class Engrave extends JavaPlugin implements Listener{

	public final static String mainDir = "plugins/Engrave";
	private static File configFile, dataFile;
	private static YamlConfiguration engravings;
	private static HashSet<Byte> transparent;
	private static HashMap<Player, Block> lastSteppedOn, lastObserved, lastRead;

	public void onEnable(){
		//Register the few events that we need to listen for
		Bukkit.getPluginManager().registerEvents(this, this);
		
		//Ensure that the files we need exist, and then load from them
		getDataFolder().mkdirs();
		configFile = new File(mainDir + "/config.yml");
		dataFile = new File(mainDir + "/data.yml");
		try{
			if(!configFile.exists()){
				configFile.createNewFile();
				
				//If the config file didn't exist, load up the defaults and save them
				getConfig().options().copyDefaults(true);
				getConfig().save(configFile);
			}
			getConfig().load(configFile);
			ConfigHandler.init(getConfig());

			if(!dataFile.exists())
				dataFile.createNewFile();
			engravings = new YamlConfiguration();
			
			//Our system of saving these engravings uses decimals as strings, which doesn't mesh with the default YAML path separator
			engravings.options().pathSeparator('>');
			engravings.load(dataFile);
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		//Initialize these maps
		lastSteppedOn = new HashMap<Player, Block>();
		lastObserved = new HashMap<Player, Block>();
		lastRead = new HashMap<Player, Block>();
		
		//Every so often check every player to see if they're looking at an engraving
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable(){ public void run(){
			for(Player player : Bukkit.getOnlinePlayers())
				if(player != null && player.hasPermission("engrave.read")){
					
					//Check for engravings far away and notify the player that they've seen them
					Block block = player.getTargetBlock(transparent, ConfigHandler.getNoticeRange());
					if(!block.equals(lastObserved.get(player))){
						lastObserved.put(player, block);
						Engraving engraving = new Engraving(engravings.getConfigurationSection(S_Location.stringSave(block.getLocation())));
						if(!engraving.isNull())
							player.sendMessage(ChatColor.ITALIC + "You see something written on a block...");
					}
					
					//Check for engravings close up and read them to the player
					block = player.getTargetBlock(transparent, ConfigHandler.getReadRange());
					if(!block.equals(lastRead.get(player))){
						lastRead.put(player, block);
						Engraving engraving = new Engraving(engravings.getConfigurationSection(S_Location.stringSave(block.getLocation())));
						if(!engraving.isNull())
							player.sendMessage(engraving.getMessage());
					}
				}
		}}, 0, ConfigHandler.getCheckFrequency()); //Configurable to allow large servers to decrease the frequency of these checks if they affect performance
		
		//Every so often, check to make sure that destroyed engravings are removed (if one of the listeners fails)
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable(){ public void run(){
			for(String key : engravings.getKeys(false)){
				Engraving engraving = new Engraving(engravings.getConfigurationSection(key));
				if(engraving.getMat() != engraving.getBlock().getType())
					engravings.set(key, null);
			}
		}}, 0, 1200);
	}

	public boolean onCommand(CommandSender sender, Command command, String string, String[] args){

		Player player = null;
		if(sender instanceof Player)
			player = (Player) sender;

		if(command.getName().equals("engrave") && player != null){
			
			//Gets the block the player is looking at within a certain range
			Block target = player.getTargetBlock(transparent, ConfigHandler.getReadRange());
			ItemStack item = player.getItemInHand();
			if(item == null)
				item = new ItemStack(Material.AIR);
			
			//Either the player is looking at space, or they're not close enough, either scenario isn't allowable
			if(target.getType() == Material.AIR){
				player.sendMessage(ChatColor.ITALIC + "You must be near a block to engrave it!");
				return true;
			}
			
			//If the config doesn't mention a durability for the block, we can't let them engrave on it
			if(ConfigHandler.getBlockDurability(target.getType()) == -1){
				player.sendMessage(ChatColor.ITALIC + "You cannot engrave this type of block!");
				return true;
			}
			
			//Concatenate the remainder of the words in the command
			String message = "";
			for(String word : args)
				message += word + " ";
			message = message.trim();
			
			//Obviously you need to actually type a message...
			if(message.equals("")){
				player.sendMessage(ChatColor.ITALIC + "Include a message to engrave!");
				return true;
			}
			
			//Create and save the engraving
			Engraving engraving = new Engraving(target, item, message);
			engraving.save(engravings);
			
			//Send the player a nicely formatted message letting them know the engraving was successful
			String block = target.getType().name().toLowerCase().replaceAll("_", " ");
			String tool = ConfigHandler.getToolQuality(item.getType()) == 0 ? "hand" : item.getType().name().toLowerCase().replaceAll("_", " ");
			if(item.getType() == Material.INK_SACK){
				tool = ((Dye) item.getData()).getColor().name().toLowerCase().replaceAll("_", " ") + " dye";
				player.setItemInHand(null);
			}
			else
				item.setDurability((short) (item.getDurability() + ConfigHandler.getEngravingDamage()));
			player.sendMessage(ChatColor.ITALIC + "You engraved in the " + block + " with your " + tool + ":");
			player.sendMessage(engraving.getMessage());
		}

		//Send the player various information about an engraving for debugging purposes
		if(command.getName().equals("debug-engrave") && player != null){
			Block target = player.getTargetBlock(transparent, ConfigHandler.getNoticeRange());
			if(target.getType() == Material.AIR){
				player.sendMessage(ChatColor.ITALIC + "You must be near a block to debug it!");
				return true;
			}
			Engraving engraving = new Engraving(engravings.getConfigurationSection(S_Location.stringSave(target.getLocation())));
			if(engraving.isNull())
				return true;
			player.sendMessage("Location: " + S_Location.stringSave(engraving.getBlock().getLocation()));
			player.sendMessage("Tool: " + engraving.getTool().name().toLowerCase().replaceAll("_", " "));
			player.sendMessage("Durability: " + engraving.getDurability());
			player.sendMessage("Damage: " + engraving.getDamage());
			player.sendMessage("Pure message: " + engraving.getPureMessage());
			player.sendMessage("Message: " + engraving.getMessage());
		}

		return true;
	}

	public void onDisable(){
		
		//Save the engravings to the file
		try{ engravings.save(dataFile); }
		catch(Exception e){ e.printStackTrace(); }
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event){
		if(event.getClickedBlock() == null)
			return;

		//Degrade the engraving if it's hit, according to what tool was used to hit it
		if(event.getAction() == Action.LEFT_CLICK_BLOCK && event.getPlayer().hasPermission("engrave.degrade")){
			Engraving engraving = new Engraving(engravings.getConfigurationSection(S_Location.stringSave(event.getClickedBlock().getLocation())));
			if(engraving.isNull())
				return;
			if(engraving.degrade(ConfigHandler.getToolQuality(event.getPlayer().getItemInHand().getType()) + 3))
				event.getPlayer().sendMessage(ChatColor.ITALIC + "You've degraded the engraving!");
			engraving.save(engravings);
		}
		
		//Read the engraving if it's examined
		if(event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getPlayer().hasPermission("engrave.read")){
			Engraving engraving = new Engraving(engravings.getConfigurationSection(S_Location.stringSave(event.getClickedBlock().getLocation())));
			if(engraving.isNull())
				return;
			event.getPlayer().sendMessage(engraving.getMessage());
		}
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event){
		
		//Get the block that was stepped on
		Block steppedOn = event.getTo().getBlock().getRelative(BlockFace.DOWN);
		if(steppedOn.equals(lastSteppedOn.get(event.getPlayer())))
			return;
		lastSteppedOn.put(event.getPlayer(), steppedOn);
		Engraving engraving = new Engraving(engravings.getConfigurationSection(S_Location.stringSave( steppedOn.getLocation() )));
		if(engraving.isNull())
			return;
		
		//Let the player know that they've stepped on an engraving
		event.getPlayer().sendMessage(ChatColor.ITALIC + "You've stepped on an engraving!");
		
		//Alert them if they've degraded it
		if(event.getPlayer().hasPermission("engrave.degrade") && engraving.degrade(5))
			event.getPlayer().sendMessage(ChatColor.ITALIC + "You've degraded the engraving!");
		engraving.save(engravings);
	}

	//Remove records of engravings if their blocks are destroyed
	@EventHandler public void onBlockBreak(BlockBreakEvent event){ engravings.set(S_Location.stringSave(event.getBlock().getLocation()), null); }
	@EventHandler public void onBlockBurn(BlockBurnEvent event){ engravings.set(S_Location.stringSave(event.getBlock().getLocation()), null); }
	@EventHandler public void onBlockFade(BlockFadeEvent event){ engravings.set(S_Location.stringSave(event.getBlock().getLocation()), null); }	
	@EventHandler public void onEntityExplode(EntityExplodeEvent event){ for(Block block : event.blockList()) engravings.set(S_Location.stringSave(block.getLocation()), null); }
	
	@EventHandler
	public void onEntityDamage(EntityDamageEvent event){
		if(!(event.getEntity() instanceof Player))
			return;
		
		//Get the engraving they're stepping on
		Player player = (Player) event.getEntity();
		Block steppedOn = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
		Engraving engraving = new Engraving(engravings.getConfigurationSection(S_Location.stringSave(steppedOn.getLocation())));
		if(engraving.isNull())
			return;
		
		//Shield words only affect certain sources of damage
		if(engraving.getMessage().toLowerCase().contains(ConfigHandler.getShieldWord())){
			switch(event.getCause()){
			case BLOCK_EXPLOSION:
			case ENTITY_ATTACK:
			case ENTITY_EXPLOSION:
			case FIRE:
			case FIRE_TICK:
			case LIGHTNING:
			case MAGIC:
			case POISON:
			case PROJECTILE:
				
				//Cancel the damage, then give them a very brief bit of invulnerability
				event.setCancelled(true);
				if(player.getNoDamageTicks() == 0){
					player.sendMessage(ChatColor.ITALIC + "The engraving's shield word protects you from harm!");
					player.setNoDamageTicks(60);
				}
				break;
			default:
			}
		}
		
	}

	//For conversion between dye colors (engraving with dyes) and chat colors (displaying the engravings)
	public static ChatColor dyeToChatColor(DyeColor color){
		if(color == null) return ChatColor.RESET;
		switch(color){
		case BLACK: return ChatColor.BLACK;
		case BLUE: return ChatColor.DARK_BLUE;
		case BROWN: return ChatColor.BOLD;
		case CYAN: return ChatColor.DARK_AQUA;
		case GRAY: return ChatColor.DARK_GRAY;
		case GREEN: return ChatColor.DARK_GREEN;
		case LIGHT_BLUE: return ChatColor.BLUE;
		case LIME: return ChatColor.GREEN;
		case MAGENTA: return ChatColor.LIGHT_PURPLE;
		case ORANGE: return ChatColor.GOLD;
		case PINK: return ChatColor.RED;
		case PURPLE: return ChatColor.DARK_PURPLE;
		case RED: return ChatColor.DARK_RED;
		case SILVER: return ChatColor.GRAY;
		case WHITE: return ChatColor.WHITE;
		case YELLOW: return ChatColor.YELLOW;
		default: return ChatColor.RESET;
		}
	}

	//Statically define all the blocks that players can see through
	static{
		transparent = new HashSet<Byte>(); transparent.add((byte) 0); transparent.add((byte) 6); transparent.add((byte) 8);
		transparent.add((byte) 9); transparent.add((byte) 20); transparent.add((byte) 27); transparent.add((byte) 28);
		transparent.add((byte) 31); transparent.add((byte) 32); transparent.add((byte) 37); transparent.add((byte) 38);
		transparent.add((byte) 39); transparent.add((byte) 40); transparent.add((byte) 50); transparent.add((byte) 55);
		transparent.add((byte) 63); transparent.add((byte) 65); transparent.add((byte) 66); transparent.add((byte) 68);
		transparent.add((byte) 69); transparent.add((byte) 70); transparent.add((byte) 72); transparent.add((byte) 75);
		transparent.add((byte) 76); transparent.add((byte) 77); transparent.add((byte) 78); transparent.add((byte) 93);
		transparent.add((byte) 94); transparent.add((byte) 96); transparent.add((byte) 101); transparent.add((byte) 102);
		transparent.add((byte) 104); transparent.add((byte) 105); transparent.add((byte) 106); transparent.add((byte) 111);
		transparent.add((byte) 131); transparent.add((byte) 132); transparent.add((byte) 143);
	}

}
