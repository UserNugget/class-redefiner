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
import io.github.usernugget.redefiner.util.asm.instruction.Insns;
import io.github.usernugget.redefiner.util.asm.instruction.immutable.Injected;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;

public class TailHandler implements Handler {
  @Override
  public void handleMethod(MethodChange change) {
    ClassMethod mapping = change.getMappingMethod();
    ClassMethod target = change.findTargetMethod();

    Type returnType = target.returnType();
    int returnVariable = target.newVariable();

    LabelNode mappingBase = new LabelNode();

    Insns targetCode = target.getInstructions();
    for (AbstractInsnNode instruction : targetCode) {
      // TODO: RET?
      if (instruction.getOpcode() >= Opcodes.IRETURN &&
          instruction.getOpcode() <= Opcodes.RETURN &&
          !(instruction instanceof Injected)) {
        Insns jump = new Insns();
        if (returnType.getSort() != Type.VOID) {
          jump.storeOp(returnType, returnVariable);
        }
        jump.jumpOp(Opcodes.GOTO, mappingBase);

        targetCode.insert(instruction, jump);
        targetCode.remove(instruction);
      }
    }

    mapping.increaseVariableIndex(target.maxVariable());

    Insns mappingCode = mapping.getInstructions();
    for (AbstractInsnNode instruction : mappingCode) {
      int opcode = instruction.getOpcode();
      if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN && !(instruction instanceof Injected)) {
        Insns exit = new Insns();
        if (returnType.getSort() != Type.VOID) {
          exit.loadOp(returnType, returnVariable);
        }
        exit.returnOp(returnType);

        mappingCode.insert(instruction, exit);
        mappingCode.remove(instruction);
      }
    }

    targetCode.add(mappingBase);
    targetCode.add(mappingCode);

    target.addTryCatchBlocks(mapping.tryCatchBlocks);
  }
}
