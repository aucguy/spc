package com.sijobe.spc.asm;

import net.minecraft.launchwrapper.IClassTransformer;

/**
 * the ASMtransformer
 * 
 * @author aucguy
 * @version 1.0
 */
public class Transformer implements IClassTransformer {
   static Transformer instance;
   
   public Transformer() {
      instance = this;
   }
   
   void hookTransformers() {
      Processor processor = Processor.getInstance();
      processor.mappings.shouldWarn = true;
      processor.registerMethodTransformers(MethodTransformer.generateFromFunctions(SimpleHooked.class));
      processor.registerMethodTransformer(new SlashPrefixer());
      processor.registerMethodTransformer(new EntityReacherClient());
      processor.registerMethodTransformer(new EntityReacherServer());
      processor.registerMethodTransformer(new CallReplacer("net.minecraft.world.World:updateEntities:()V",
            "net/minecraft/tileentity/TileEntity:updateEntity:()V",
            "com.sijobe.spc.command.Pause:updateTileEntity:(Lnet/minecraft/tileentity/TileEntity;)V"));
      processor.mappings.shouldWarn = false;
   }
   
   @Override
   public byte[] transform(String name, String transformedName, byte[] basicClass) {
      return Processor.getInstance().process(name, basicClass);
   }
}
