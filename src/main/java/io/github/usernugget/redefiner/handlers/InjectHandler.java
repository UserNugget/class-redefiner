/*
 * Copyright (C) 2023 UserNugget/class-redefiner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.usernugget.redefiner.handlers;

import io.github.usernugget.redefiner.util.asm.AnnotationValues;
import io.github.usernugget.redefiner.util.asm.CodeGenerator;
import io.github.usernugget.redefiner.util.asm.info.MethodInfo;
import io.github.usernugget.redefiner.util.asm.node.ClassField;
import io.github.usernugget.redefiner.util.asm.node.ClassFile;
import io.github.usernugget.redefiner.util.asm.node.ClassMethod;
import io.github.usernugget.redefiner.util.asm.node.Insts;
import io.github.usernugget.redefiner.registry.AnnotationHandler;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.PUTSTATIC;

public class InjectHandler implements AnnotationHandler {
  public static ClassMethod injectMethod(
     CodeGenerator codeGenerator, ClassFile targetClass,
     ClassFile mappingClass, ClassMethod mappingMethod
  ) {
    ClassMethod method = mappingMethod.copy();
    method.name += "$Gen_" + Long.toString(System.nanoTime(), 36);

    if (!method.isStatic()) {
      method.access |= Opcodes.ACC_STATIC;
      method.desc = "(L" + targetClass.name + ";" + method.desc.substring(1);
    }

    codeGenerator.ensureGenerating();

    ClassNode interfaceClass = codeGenerator.getGeneratedInterface();
    interfaceClass.methods.add(method);

    Insts mappingInsts = method.insts();
    for (AbstractInsnNode i : mappingInsts) {
      if (i instanceof MethodInsnNode) {
        MethodInsnNode methodInst = (MethodInsnNode) i;
        if (!methodInst.owner.equals(targetClass.name)) {
          continue;
        }

        ClassMethod targetMethod = targetClass.findMethod(methodInst.name, methodInst.desc);
        if (targetMethod != null && targetMethod.isPrivate()) {
          codeGenerator.invoke(
             mappingInsts, i,
             codeGenerator.methodInvoker(targetClass, targetMethod)
          );
        }
      }

      if (i instanceof FieldInsnNode) {
        FieldInsnNode fieldInst = (FieldInsnNode) i;
        if (!fieldInst.owner.equals(targetClass.name)) {
          continue;
        }

        ClassField ownerField = targetClass.findField(fieldInst.name, fieldInst.desc);
        if (ownerField != null && ownerField.isPrivate()) {
          ClassMethod fieldOp;

          int opcode = fieldInst.getOpcode();
          if (opcode == PUTFIELD || opcode == PUTSTATIC) {
            fieldOp = codeGenerator.fieldSetter(targetClass, ownerField);
          } else if (opcode == GETFIELD || opcode == GETSTATIC) {
            fieldOp = codeGenerator.fieldGetter(targetClass, ownerField);
          } else {
            continue;
          }

          codeGenerator.invoke(mappingInsts, i, fieldOp);
        }
      }
    }

    for (ClassMethod targetMethod : targetClass.methods()) {
      Insts targetInsts = targetMethod.insts();
      for (AbstractInsnNode instruction : targetInsts) {
        if (instruction instanceof MethodInsnNode) {
          MethodInsnNode methodNode = (MethodInsnNode) instruction;
          if (!MethodInfo.matches(methodNode, mappingMethod)) {
            continue;
          }

          targetInsts.set(
             instruction, new MethodInsnNode(INVOKESTATIC, interfaceClass.name, method.name, method.desc, true)
          );
        }
      }
    }

    mappingClass.removeMethod(mappingMethod);
    return method;
  }

  @Override
  public void handleMethod(
     CodeGenerator codeGenerator, AnnotationValues args,
     Class<?> targetJavaClass, ClassFile targetClass,
     Class<?> mappingJavaClass, ClassFile mappingClass, ClassMethod mappingMethod
  ) {
    injectMethod(codeGenerator, targetClass, mappingClass, mappingMethod);
  }
}
