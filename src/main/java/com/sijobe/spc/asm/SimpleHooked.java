package com.sijobe.spc.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * simple ASM hooks that either replace or prefix methos
 * 
 * @author aucguy
 * @version 1.0
 */
public class SimpleHooked {
   /**
    * Allows spc to have control over client-sided block reach
    * effectively makes the method this:
    *    return com.sijobe.spc.command.BlockReach.reachDistance;
    * 
    * @param mv - the MethodWriter instance
    */
   @MethodReplacer.Hook("net.minecraft.client.multiplayer.PlayerControllerMP:getBlockReachDistance:()F")
   public static void getBlockReachDistance(MethodVisitor mv) {
      mv.visitCode();
      mv.visitFieldInsn(Opcodes.GETSTATIC, "com/sijobe/spc/command/BlockReach", "reachDistance", "F");
      mv.visitInsn(Opcodes.FRETURN);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
   }
   
   /**
    * Allows spc to have control over whether or not the inside of blocks are rendered
    * effectively prefixes the method with this
    *    if(com.sijobe.spc.ModSpc.instance.proxy.shouldNotRenderInsideOfBlock()) {
    *       return;
    *    }
    * 
    * @param mv - the MethodWriter
    */
   @MethodPrefixer.Hook("net.minecraft.client.renderer.ItemRenderer:renderInsideOfBlock:(FLnet/minecraft/util/IIcon;)V")
   public static void renderInsideOfBlock(MethodVisitor mv) {
      mv.visitCode();
      mv.visitFieldInsn(Opcodes.GETSTATIC, "com/sijobe/spc/ModSpc", "instance", "Lcom/sijobe/spc/ModSpc;");
      mv.visitFieldInsn(Opcodes.GETFIELD, "com/sijobe/spc/ModSpc", "proxy", "Lcom/sijobe/spc/proxy/Proxy;");
      mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "com/sijobe/spc/proxy/Proxy", "shouldNotRenderInsideOfBlock", "()Z");
      returnIfFalse(mv);
   }
   
   /**
    * Allows spc to have control over whether or to update an entity
    * effectively prefixes the method with this
    *    if(com.sijobe.spc.command.Pause.shouldNotUpdateEntity(this, entity))  {
    *       return;
    *    }
    */
   @MethodPrefixer.Hook("net.minecraft.world.World:updateEntityWithOptionalForce:(Lnet/minecraft/entity/Entity;Z)V")
   public static void updateEntites(MethodVisitor mv) {
      mv.visitCode();
      mv.visitVarInsn(Opcodes.ALOAD, 0);
      mv.visitVarInsn(Opcodes.ALOAD, 1);
      mv.visitMethodInsn(Opcodes.INVOKESTATIC, "com/sijobe/spc/command/Pause", "shouldNotUpdateEntity", "(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;)Z");
      returnIfFalse(mv);
   }
   
   @MethodPrefixer.Hook("net.minecraft.world.World:setBlock:(IIILnet/minecraft/block/Block;II)Z")
   public static void modifyFlags(MethodVisitor mv) {
      mv.visitCode();
      mv.visitVarInsn(Opcodes.ILOAD, 6); //push flags
      mv.visitMethodInsn(Opcodes.INVOKESTATIC, "com/sijobe/spc/command/Pause", "modifyFlags", "(I)I");
      mv.visitVarInsn(Opcodes.ISTORE, 6); //pop flags
   }
   
   /**
    * adds the code
    * if(<something that was already called>) {
    *    return;
    * }
    */
   public static void returnIfFalse(MethodVisitor mv) {
      Label label = new Label();
      mv.visitJumpInsn(Opcodes.IFEQ, label);
      mv.visitInsn(Opcodes.RETURN);
      mv.visitLabel(label);
      mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
   }
}
