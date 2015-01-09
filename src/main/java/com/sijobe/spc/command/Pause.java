package com.sijobe.spc.command;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.sijobe.spc.validation.Parameter;
import com.sijobe.spc.validation.ParameterBoolean;
import com.sijobe.spc.validation.ParameterString;
import com.sijobe.spc.validation.Parameters;
import com.sijobe.spc.wrapper.CommandBase;
import com.sijobe.spc.wrapper.CommandException;
import com.sijobe.spc.wrapper.CommandSender;

/**
 * Pauses different routines in the game like mobs, redstone, etc.
 * @author aucguy
 * @version 1.0
 */
@Command (
      name = "pause",
      description = "pauses things in the game",
      example = "mob",
      videoURL = "",
      version = "1.0"
   )
public class Pause extends StandardCommand {
   
   /**
    * stores whether or not a the certain update type is paused
    */
   
   public static boolean mobUpdatesPaused = false; 
   public static boolean tileEntityUpdatesPaused = false;
   public static boolean blockUpdatesPaused = false;
   
   /**
    * The parameters of the command
    */
   private static final Parameters PARAMETERS = new Parameters(
      new Parameter[] {
            new ParameterString("<mob|tileentity|blockupdate>", false, new String[] {"mob", "tileentity", "blockupdate"}),
            new ParameterBoolean("<enable|disable>", true)
      }
   );

   /**
    * @throws CommandException 
    * @see com.sijobe.spc.wrapper.CommandBase#execute(net.minecraft.src.ICommandSender, java.util.List)
    */
   @Override
   public void execute(CommandSender sender, List<?> params) throws CommandException {
      String update = (String) params.get(0);
      if(!updateTypeExists(update)) {
         throw(new CommandException("Unknown update type"));
      }
      
      if(params.size() == 1) {
         setUpdatePaused(update, !getUpdatePaused((String) params.get(0)));
      }
      else {
         setUpdatePaused(update, (Boolean) params.get(1));
      }
   }
   
   /**
    * returns whether or not the update type is supported
    */
   public boolean updateTypeExists(String type) {
      return type.equals("mob") || type.equals("tileentity") || type.equals("blockupdate");
   }
   
   /**
    * returns whether or not a certain update is paused
    */
   public boolean getUpdatePaused(String type) {
      if(type.equals("mob")) {
         return mobUpdatesPaused;
      }
      else if(type.equals("tileentity")) {
         return tileEntityUpdatesPaused;
      }
      else if(type.equals("blockupdate")) {
         return blockUpdatesPaused;
      }
      return false;
   }
   
   /**
    * sets the whether or not to pause a certain type
    */
   public void setUpdatePaused(String type, boolean paused) {
      if(type.equals("mob")) {
         mobUpdatesPaused = paused;
      }
      else if(type.equals("tileentity")) {
         tileEntityUpdatesPaused = paused;
      }
      else if(type.equals("blockupdate")) {
         blockUpdatesPaused = paused;
      }
   }
   
   /**
    * returns whether or not to update the given entity. Called through ASM modifications
    */
   public static boolean shouldNotUpdateEntity(World world, Entity entity) {
      return mobUpdatesPaused && entity instanceof EntityLiving;
   }
   
   /**
    * returns whether or not to update the given tileentity. Called through ASM modifications
    */
   public static void updateTileEntity(TileEntity tileentity) {
      if(!tileEntityUpdatesPaused) {
         tileentity.updateEntity();
      }
   }
   
   public static int modifyFlags(int flags) {
      if(blockUpdatesPaused) {
         return flags & ~1;
      }
      return flags;
   }
   
   /**
    * @see com.sijobe.spc.wrapper.CommandBase#getParameters()
    */
   @Override
   public Parameters getParameters() {
      return PARAMETERS;
   }
}
