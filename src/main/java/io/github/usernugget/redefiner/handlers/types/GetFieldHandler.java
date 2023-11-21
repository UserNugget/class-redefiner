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

import io.github.usernugget.redefiner.util.asm.ClassMethod;
import io.github.usernugget.redefiner.util.asm.instruction.Insns;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;

public class GetFieldHandler extends FieldHandler {
  @Override
  public boolean instructionMatches(AbstractInsnNode instruction) {
    return instruction.getOpcode() == Opcodes.GETSTATIC ||
           instruction.getOpcode() == Opcodes.GETFIELD;
  }

  @Override
  public void insertCode(
    ClassMethod target, ClassMethod mapping,
    FieldInsnNode field, Insns wrapper
  ) {
    target.getInstructions().insert(field, wrapper);
    target.addTryCatchBlocks(mapping.tryCatchBlocks);
  }
}
