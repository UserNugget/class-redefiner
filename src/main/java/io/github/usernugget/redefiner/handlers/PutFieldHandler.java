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

import io.github.usernugget.redefiner.annotation.PutField;
import io.github.usernugget.redefiner.registry.AnnotationHandler;
import io.github.usernugget.redefiner.registry.HandlerPriority;
import io.github.usernugget.redefiner.util.asm.AnnotationValues;
import io.github.usernugget.redefiner.util.asm.CodeGenerator;
import io.github.usernugget.redefiner.util.asm.info.FieldInfo;
import io.github.usernugget.redefiner.util.asm.info.MethodInfo;
import io.github.usernugget.redefiner.util.asm.node.ClassField;
import io.github.usernugget.redefiner.util.asm.node.ClassFile;
import io.github.usernugget.redefiner.util.asm.node.ClassMethod;
import io.github.usernugget.redefiner.util.asm.node.Insts;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.PUTSTATIC;

public class PutFieldHandler implements AnnotationHandler {
  private static final String DESC = Type.getDescriptor(PutField.class);

  @Override
  public void handleMethod(
     CodeGenerator codeGenerator, AnnotationValues args,
     Class<?> targetJavaClass, ClassFile targetClass,
     Class<?> mappingJavaClass, ClassFile mappingClass, ClassMethod mappingMethod
  ) {
    ClassMethod targetMethod = MethodInfo.parse(mappingMethod, args)
       .findMethodOrThrow(targetClass);

    int offset = args.getOrDefault("offset", -1);

    FieldInfo fieldInfo = FieldInfo.parse(args);
    ClassField field = fieldInfo.findFieldOrThrow(targetClass);

    Type fieldDesc = Type.getType(field.desc);
    if(!Type.getReturnType(mappingMethod.desc).equals(fieldDesc)) {
      throw new IllegalStateException(
         "mapping method " + mappingMethod.name + " should return " + fieldDesc.getClassName()
      );
    }

    ClassMethod injectedMethod = InjectHandler.injectMethod(
       codeGenerator, targetClass, mappingClass, mappingMethod
    );

    Insts targetInsts = targetMethod.insts();
    for (AbstractInsnNode i : targetInsts) {
      int opcode = i.getOpcode();
      if ((opcode != PUTFIELD && opcode != PUTSTATIC) ||
          !fieldInfo.matches((FieldInsnNode) i)) {
        continue;
      }

      if (offset == -1 || --offset == 0) {
        targetInsts.insertBefore(i, codeGenerator.invokeOp(injectedMethod));
        if (offset == 0) {
          break;
        }
      }
    }
  }

  @Override
  public String annotationDesc() {
    return DESC;
  }

  @Override
  public short priority() {
    return HandlerPriority.NORMAL;
  }
}
