package com.sijobe.spc.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * replaces a method call with another static call
 * @author aucguy
 * @version 1.0
 */
public class CallReplacer extends MethodTransformer {
   
   /**
    * the method to be replaced
    */
   String oldMethod;
   
   /**
    * the method to be added in
    */
   String newMethod;
   
   
   CallReplacer(String id, String oldMethod, String newMethod) {
      super(id);
      this.oldMethod = oldMethod;
      this.newMethod = newMethod;
   }

   @Override
   void injectMethodWriter(MethodVisitor mv) {
      this.mv = mv;
   }
   
   @Override
   public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      if((owner+":"+name+":"+desc).equals(this.oldMethod)) {
         String[] parts = this.newMethod.split(":");
         super.visitMethodInsn(Opcodes.INVOKESTATIC, parts[0], parts[1], parts[2]);
      }
      else {
         super.visitMethodInsn(opcode, owner, name, desc);
      }
   }
}
