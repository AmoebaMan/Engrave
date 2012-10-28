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
		Bukkit.getPluginManager().registerEvents(this, this);

		getDataFolder().mkdirs();
		configFile = new File(mainDir + "/config.yml");
		dataFile = new File(mainDir + "/data.yml");
		try{
			if(!configFile.exists()){
				configFile.createNewFile();
				getConfig().options().copyDefaults(true);
				getConfig().save(configFile);
			}
			getConfig().load(configFile);
			ConfigHandler.init(getConfig());

			if(!dataFile.exists())
				dataFile.createNewFile();
			engravings = new YamlConfiguration();
			engravings.options().pathSeparator('>');
			engravings.load(dataFile);
		}
		catch(Exception e){
			e.printStackTrace();
		}
		lastSteppedOn = new HashMap<Player, Block>();
		lastObserved = new HashMap<Player, Block>();
		lastRead = new HashMap<Player, Block>();
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable(){ public void run(){
			for(Player player : Bukkit.getOnlinePlayers())
				if(player != null){
					Block block = player.getTargetBlock(transparent, ConfigHandler.getNoticeRange());
					if(!block.equals(lastObserved.get(player))){
						lastObserved.put(player, block);
						Engraving engraving = new Engraving(engravings.getConfigurationSection(S_Location.stringSave(block.getLocation())));
						if(!engraving.isNull())
							player.sendMessage(ChatColor.ITALIC + "You see something written on a block...");
					}
					block = player.getTargetBlock(transparent, ConfigHandler.getReadRange());
					if(!block.equals(lastRead.get(player))){
						lastRead.put(player, block);
						Engraving engraving = new Engraving(engravings.getConfigurationSection(S_Location.stringSave(block.getLocation())));
						if(!engraving.isNull())
							player.sendMessage(engraving.getMessage());
					}
				}
		}}, 0, ConfigHandler.getCheckFrequency());
		
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
			Block target = player.getTargetBlock(transparent, ConfigHandler.getReadRange());
			ItemStack item = player.getItemInHand();
			if(item == null)
				item = new ItemStack(Material.AIR);
			if(target.getType() == Material.AIR){
				player.sendMessage(ChatColor.ITALIC + "You must be near a block to engrave it!");
				return true;
			}
			if(ConfigHandler.getBlockDurability(target.getType()) == -1){
				player.sendMessage(ChatColor.ITALIC + "You cannot engrave this type of block!");
				return true;
			}
			String message = "";
			for(String word : args)
				message += word + " ";
			message = message.trim();
			if(message.equals("")){
				player.sendMessage(ChatColor.ITALIC + "Include a message to engrave!");
				return true;
			}
			Engraving engraving = new Engraving(target, item, message);
			engraving.save(engravings);
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
		try{
			engravings.save(dataFile);
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event){
		if(event.getClickedBlock() == null)
			return;
		Engraving engraving = new Engraving(engravings.getConfigurationSection(S_Location.stringSave(event.getClickedBlock().getLocation())));
		if(engraving.isNull())
			return;
		if(engraving.degrade(ConfigHandler.getToolQuality(event.getPlayer().getItemInHand().getType()) + 3))
			event.getPlayer().sendMessage(ChatColor.ITALIC + "You've distorted the engraving!");
		engraving.save(engravings);
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event){
		Block steppedOn = event.getTo().getBlock().getRelative(BlockFace.DOWN);
		if(steppedOn.equals(lastSteppedOn.get(event.getPlayer())))
			return;
		lastSteppedOn.put(event.getPlayer(), steppedOn);
		Engraving engraving = new Engraving(engravings.getConfigurationSection(S_Location.stringSave( steppedOn.getLocation() )));
		if(engraving.isNull())
			return;
		event.getPlayer().sendMessage(ChatColor.ITALIC + "You've stepped on an engraving!");
		if(engraving.degrade(5))
			event.getPlayer().sendMessage(ChatColor.ITALIC + "You've distorted the engraving!");
		engraving.save(engravings);
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event){
		engravings.set(S_Location.stringSave(event.getBlock().getLocation()), null);
	}

	@EventHandler
	public void onBlockBurn(BlockBurnEvent event){
		engravings.set(S_Location.stringSave(event.getBlock().getLocation()), null);
	}

	@EventHandler
	public void onBlockFade(BlockFadeEvent event){
		engravings.set(S_Location.stringSave(event.getBlock().getLocation()), null);
	}
	
	@EventHandler
	public void onEntityExplode(EntityExplodeEvent event){
		for(Block block : event.blockList())
			engravings.set(S_Location.stringSave(block.getLocation()), null);
	}
	
	@EventHandler
	public void onEntityDamage(EntityDamageEvent event){
		if(!(event.getEntity() instanceof Player))
			return;
		Player player = (Player) event.getEntity();
		Block steppedOn = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
		Engraving engraving = new Engraving(engravings.getConfigurationSection(S_Location.stringSave(steppedOn.getLocation())));
		if(engraving.isNull())
			return;
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

	static{
		transparent = new HashSet<Byte>();
		transparent.add((byte) 0);
		transparent.add((byte) 6);
		transparent.add((byte) 8);
		transparent.add((byte) 9);
		transparent.add((byte) 20);
		transparent.add((byte) 27);
		transparent.add((byte) 28);
		transparent.add((byte) 31);
		transparent.add((byte) 32);
		transparent.add((byte) 37);
		transparent.add((byte) 38);
		transparent.add((byte) 39);
		transparent.add((byte) 40);
		transparent.add((byte) 50);
		transparent.add((byte) 55);
		transparent.add((byte) 63);
		transparent.add((byte) 65);
		transparent.add((byte) 66);
		transparent.add((byte) 68);
		transparent.add((byte) 69);
		transparent.add((byte) 70);
		transparent.add((byte) 72);
		transparent.add((byte) 75);
		transparent.add((byte) 76);
		transparent.add((byte) 77);
		transparent.add((byte) 78);
		transparent.add((byte) 93);
		transparent.add((byte) 94);
		transparent.add((byte) 96);
		transparent.add((byte) 101);
		transparent.add((byte) 102);
		transparent.add((byte) 104);
		transparent.add((byte) 105);
		transparent.add((byte) 106);
		transparent.add((byte) 111);
		transparent.add((byte) 131);
		transparent.add((byte) 132);
		transparent.add((byte) 143);
	}

}
