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

import io.github.usernugget.redefiner.registry.AnnotationHandler;
import io.github.usernugget.redefiner.util.asm.AnnotationValues;
import io.github.usernugget.redefiner.util.asm.CodeGenerator;
import io.github.usernugget.redefiner.util.asm.info.FieldInfo;
import io.github.usernugget.redefiner.util.asm.info.MethodInfo;
import io.github.usernugget.redefiner.util.asm.node.ClassFile;
import io.github.usernugget.redefiner.util.asm.node.ClassMethod;
import io.github.usernugget.redefiner.util.asm.node.Insts;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GETSTATIC;

public class GetFieldHandler implements AnnotationHandler {
  @Override
  public void handleMethod(
     CodeGenerator code, AnnotationValues args,
     Class<?> targetJavaClass, ClassFile targetClass,
     Class<?> mappingJavaClass, ClassFile mappingClass, ClassMethod mappingMethod
  ) {
    ClassMethod method = MethodInfo.parse(mappingMethod, args)
       .findMethodOrThrow(targetClass);

    int offset = args.getOrDefault("offset", -1);

    code.ensureGenerating();

    ClassNode interfaceKlass = code.getGeneratedInterface();
    FieldInfo fieldInfo = FieldInfo.parse(args);
    FieldNode field = fieldInfo.findFieldOrThrow(targetClass);

    Type fieldDesc = Type.getType(field.desc);
    if(!Type.getReturnType(mappingMethod.desc).equals(fieldDesc)) {
      throw new IllegalStateException(
         "mapping method " + mappingMethod.name + " should return " + fieldDesc.getClassName()
      );
    }

    ClassMethod injectedMethod = InjectHandler.injectMethod(
       code, targetClass, mappingClass, mappingMethod
    );

    Insts targetInsts = method.insts();
    for (AbstractInsnNode i : targetInsts) {
      int opcode = i.getOpcode();
      if (opcode != GETFIELD && opcode != GETSTATIC ||
          !fieldInfo.matches((FieldInsnNode) i)) {
        continue;
      }

      if (offset == -1 || --offset == 0) {
        targetInsts.insert(i, code.invokeOp(injectedMethod));
        if (offset == 0) {
          break;
        }
      }
    }
  }
}
