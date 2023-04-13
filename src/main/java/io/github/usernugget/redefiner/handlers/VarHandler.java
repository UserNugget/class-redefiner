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
import io.github.usernugget.redefiner.util.asm.node.ClassFile;
import io.github.usernugget.redefiner.util.asm.node.ClassMethod;
import io.github.usernugget.redefiner.util.asm.node.Insts;
import io.github.usernugget.redefiner.util.asm.node.immutable.ImmutableIincInsnNode;
import io.github.usernugget.redefiner.util.asm.node.immutable.ImmutableVarInsnNode;
import io.github.usernugget.redefiner.registry.AnnotationHandler;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.VarInsnNode;

public class VarHandler implements AnnotationHandler {
  @Override
  public void handleParameter(
     ClassFile targetClass, ClassMethod targetMethod,
     ClassFile mappingClass, ClassMethod mappingMethod,
     AnnotationValues args, int argIndex, Type[] descArgs
  ) {
    int argMapping = args.getOrDefault("raw", -1);
    if (argMapping == -1) {
      String expectedName = args.getString("name");
      if (expectedName == null || expectedName.isEmpty()) {
        if(mappingMethod.localVariables == null) {
          throw new IllegalStateException(
             "missing local variables in mapping method " + desc(mappingClass, mappingMethod)
          );
        }

        expectedName = mappingMethod.localVariables.get(argIndex).name;
      }

      if (targetMethod.localVariables == null) {
        throw new IllegalStateException(
           "missing local variables in target method " + targetMethod
        );
      }

      for (LocalVariableNode localVariable : targetMethod.localVariables) {
        if (localVariable.name.equals(expectedName)) {
          argMapping = localVariable.index;
        }
      }

      if (argMapping == -1) {
        throw new IllegalStateException(
           "local variable " + expectedName + " not found in method " + desc(targetClass, targetMethod)
        );
      }
    }

    int argValue = -1;
    int locals = !mappingMethod.isStatic() ? 1 : 0;
    for (int i = 0; i < descArgs.length; i++) {
      if (argIndex == i) {
        argValue = locals;
        break;
      }

      locals += descArgs[i].getSize();
    }

    Insts mappingInsts = mappingMethod.insts();
    for (AbstractInsnNode i : mappingInsts) {
      if (i instanceof VarInsnNode) {
        VarInsnNode varNode = (VarInsnNode) i;
        if (varNode.var != argValue) {
          continue;
        }

        mappingInsts.set(varNode, new ImmutableVarInsnNode(varNode.getOpcode(), argMapping));
      } else if (i instanceof IincInsnNode) {
        IincInsnNode iincNode = (IincInsnNode) i;
        if (iincNode.var != argValue) {
          continue;
        }

        mappingInsts.set(iincNode, new ImmutableIincInsnNode(iincNode.getOpcode(), argMapping));
      }
    }
  }
}
