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

package kk.redefiner.handlers;

import kk.redefiner.registry.AnnotationHandler;
import kk.redefiner.util.asm.AnnotationValues;
import kk.redefiner.util.asm.Asm;
import kk.redefiner.util.asm.CodeGenerator;
import kk.redefiner.util.asm.info.MethodInfo;
import kk.redefiner.util.asm.node.ClassFile;
import kk.redefiner.util.asm.node.ClassMethod;
import kk.redefiner.util.asm.node.Insts;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;

public class HeadHandler implements AnnotationHandler {
  @Override
  public void handleMethod(
     CodeGenerator codeGenerator, AnnotationValues args,
     Class<?> targetJavaClass, ClassFile targetClass,
     Class<?> mappingJavaClass, ClassFile mappingClass, ClassMethod mappingMethod
  ) {
    if(Type.getReturnType(mappingMethod.desc).getSort() != Type.VOID) {
      throw new IllegalStateException(
         "mapping method " + mappingMethod.name + " should be void"
      );
    }

    ClassMethod method = MethodInfo.parse(mappingMethod, args)
       .findMethodOrThrow(targetClass);

    LabelNode returnLabel = new LabelNode();
    Insts mappingInsts = mappingMethod.insts();
    for(AbstractInsnNode i : mappingInsts) {
      if(Asm.isUnmoddedReturn(i)) {
        mappingInsts.set(i, new JumpInsnNode(Opcodes.GOTO, returnLabel));
      }
    }

    Insts insns = method.insts();
    insns.insert(returnLabel);
    insns.insertBefore(returnLabel, mappingInsts);
  }
}
