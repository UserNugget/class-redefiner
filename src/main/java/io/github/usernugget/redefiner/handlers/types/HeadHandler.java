/*
 * Copyright (C) 2024 UserNugget/class-redefiner
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
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;

public class HeadHandler implements Handler {
  @Override
  public void handleMethod(MethodChange change) {
    ClassMethod mapping = change.getMappingMethod();
    ClassMethod target = change.findTargetMethod();

    LabelNode targetBase = new LabelNode();

    Insns mappingCode = mapping.getInstructions();
    for (AbstractInsnNode instruction : mappingCode) {
      int opcode = instruction.getOpcode();
      if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN && !(instruction instanceof Injected)) {
        if (opcode == Opcodes.RETURN) {
          mappingCode.set(instruction, Ops.jumpOp(Opcodes.GOTO, targetBase));
        } else {
          Insns injection = new Insns();
          if (opcode == Opcodes.DRETURN || opcode == Opcodes.LRETURN) {
            injection.op(Opcodes.POP2);
          } else {
            injection.op(Opcodes.POP);
          }

          injection.jumpOp(Opcodes.GOTO, targetBase);

          mappingCode.insert(instruction, injection);
          mappingCode.remove(instruction);
        }
      }
    }

    mapping.increaseVariableIndex(target.maxVariable());

    mappingCode.add(targetBase);
    target.getInstructions().insert(mappingCode);

    target.addTryCatchBlocks(mapping.tryCatchBlocks);
  }
}
