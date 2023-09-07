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
import io.github.usernugget.redefiner.handlers.Op;
import io.github.usernugget.redefiner.util.asm.Ops;
import io.github.usernugget.redefiner.util.asm.instruction.Insns;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

// Converts Op.returnOp(*) to plain returns
public class OpHandler implements Handler {
  private static final String OP_NAME = Type.getInternalName(Op.class);

  @Override
  public void handleMethod(MethodChange change) {
    Insns mappingCode = change.getMappingMethod().getInstructions();
    for (AbstractInsnNode instruction : mappingCode) {
      if (instruction instanceof MethodInsnNode) {
        MethodInsnNode method = (MethodInsnNode) instruction;
        // TODO: think about method comparsion under obfuscation
        if (method.owner.equals(OP_NAME)) {
          mappingCode.set(instruction, Ops.returnOp(Type.getArgumentTypes(method.desc)[0]));
        }
      }
    }
  }
}
