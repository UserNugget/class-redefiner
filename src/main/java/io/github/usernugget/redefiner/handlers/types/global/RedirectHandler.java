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

package io.github.usernugget.redefiner.handlers.types.global;

import io.github.usernugget.redefiner.changes.MethodChange;
import io.github.usernugget.redefiner.handlers.Handler;
import io.github.usernugget.redefiner.util.asm.instruction.Insns;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

// Redirect mapping calls to itself into calls to target
public class RedirectHandler implements Handler {
  @Override
  public void handleMethod(MethodChange change) {
    String mappingName = change.getMappingClass().name;
    String targetName = change.getTargetClass().name;

    Insns mappingCode = change.getMappingMethod().getInstructions();
    for (AbstractInsnNode instruction : mappingCode) {
      if (instruction instanceof MethodInsnNode) {
        MethodInsnNode method = (MethodInsnNode) instruction;
        if (method.owner.equals(mappingName)) {
          method.owner = targetName;
        }
      } else if (instruction instanceof FieldInsnNode) {
        FieldInsnNode field = (FieldInsnNode) instruction;
        if (field.owner.equals(mappingName)) {
          field.owner = targetName;
        }
      } else if (instruction instanceof LdcInsnNode) {
        LdcInsnNode ldc = (LdcInsnNode) instruction;
        if (ldc.cst instanceof Type) {
          if (((Type) ldc.cst).getInternalName().equals(mappingName)) {
            ldc.cst = Type.getObjectType(targetName);
          }
        }

        // FIXME
        //  ldc.cst instanceof Handle
        //  ldc.cst instanceof ConstantDynamic
      } else if (instruction instanceof TypeInsnNode) {
        TypeInsnNode type = (TypeInsnNode) instruction;
        if (type.desc.equals(mappingName)) {
          type.desc = targetName;
        }
      } else if (instruction instanceof MultiANewArrayInsnNode) {
        MultiANewArrayInsnNode array = (MultiANewArrayInsnNode) instruction;
        Type type = Type.getType(array.desc);
        Type element = type.getElementType();

        if (element.getInternalName().equals(mappingName)) {
          array.desc = "]".repeat(type.getDimensions()) + element.getDescriptor();
        }
      }
      // FIXME
      //  instruction instanceof InvokeDynamicInsnNode
    }

  }
}
