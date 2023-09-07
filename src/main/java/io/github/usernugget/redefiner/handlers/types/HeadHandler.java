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

package io.github.usernugget.redefiner.handlers.types;

import io.github.usernugget.redefiner.changes.MethodChange;
import io.github.usernugget.redefiner.handlers.Handler;
import io.github.usernugget.redefiner.util.asm.ClassMethod;
import io.github.usernugget.redefiner.util.asm.Ops;
import io.github.usernugget.redefiner.util.asm.instruction.Insns;
import io.github.usernugget.redefiner.util.asm.instruction.immutable.Injected;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;

public class HeadHandler implements Handler {
  @Override
  public void handleMethod(MethodChange change) {
    ClassMethod mapping = change.getMappingMethod();
    if (mapping.returnType().getSort() != Type.VOID) {
      throw new IllegalStateException("mapping method " + mapping + " must be void");
    }

    ClassMethod target = change.findTargetMethod();

    LabelNode targetBase = new LabelNode();

    Insns mappingCode = mapping.getInstructions();
    for (AbstractInsnNode instruction : mappingCode) {
      if (instruction.getOpcode() == Opcodes.RETURN &&
          !(instruction instanceof Injected)) {
        mappingCode.set(instruction, Ops.jumpOp(Opcodes.GOTO, targetBase));
      }
    }

    mapping.increaseVariableIndex(target.maxVariable());

    mappingCode.add(targetBase);
    target.getInstructions().insert(mappingCode);

    target.addTryCatchBlocks(mapping.tryCatchBlocks);
  }
}
