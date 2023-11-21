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
import io.github.usernugget.redefiner.util.Checks;
import io.github.usernugget.redefiner.util.asm.ClassField;
import io.github.usernugget.redefiner.util.asm.ClassMethod;
import io.github.usernugget.redefiner.util.asm.Ops;
import io.github.usernugget.redefiner.util.asm.instruction.Insns;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.VarInsnNode;

public abstract class FieldHandler implements Handler {
  public abstract boolean instructionMatches(AbstractInsnNode instruction);

  public abstract void insertCode(
    ClassMethod target, ClassMethod mapping,
    FieldInsnNode field, Insns wrapper
  );

  @Override
  public void handleMethod(MethodChange change) {
    ClassMethod mapping = change.getMappingMethod();
    ClassMethod target = change.findTargetMethod();

    int offset = change.getAnnotation().getOrDefault("offset", -1);
    if (offset >= 0) {
      offset += 1;
    }

    ClassField field = change.getTargetClass().findField(
      change.getAnnotation().getField("field")
    );

    if (field == null) {
      throw new IllegalStateException(
        "field " + change.getAnnotation().get("field") + " not found"
      );
    }

    // Check if mapping has both return and argument same as the field
    Checks.checkFieldFormat(mapping, Type.getType(field.desc));

    // Inject wrapper to specified setfield
    Insns targetCode = target.getInstructions();
    for (AbstractInsnNode node : targetCode) {
      if (this.instructionMatches(node)) {
        FieldInsnNode fieldNode = (FieldInsnNode) node;
        if (fieldNode.owner.equals(field.owner.name) &&
            fieldNode.name.equals(field.name) &&
            fieldNode.desc.equals(field.desc)) {
          if (offset == -1 || offset-- == 2) {
            this.insertWrapper(fieldNode, target, mapping.copy());

            if (offset == 0) {
              break;
            }
          }
        }
      }
    }
  }

  private void insertWrapper(
    FieldInsnNode fieldNode,
    ClassMethod target,
    ClassMethod mapping
  ) {
    LabelNode exitNode = new LabelNode();

    mapping.increaseVariableIndex(target.maxVariable());
    int wrapperArg = target.newVariable(mapping);

    Type fieldDesc = Type.getType(fieldNode.desc);
    Insns wrapper = mapping.getInstructions();
    wrapper.insert(Ops.storeOp(fieldDesc, wrapperArg));
    wrapper.add(exitNode);

    int firstArg = mapping.isStatic() ? 0 : 1;
    for (AbstractInsnNode node : wrapper) {
      // Replace first argument with wrapper argument
      if (node instanceof VarInsnNode) {
        VarInsnNode var = (VarInsnNode) node;
        if (var.var == firstArg) {
          var.var = wrapperArg;
        }
      } else if (node instanceof IincInsnNode) {
        IincInsnNode iinc = (IincInsnNode) node;
        if (iinc.var == firstArg) {
          iinc.var = wrapperArg;
        }
      }

      // Replace returns with jumps
      if (node.getOpcode() >= Opcodes.IRETURN &&
          node.getOpcode() <= Opcodes.RETURN) {
        wrapper.set(node, Ops.jumpOp(Opcodes.GOTO, exitNode)); // jump to the end of the method
      }
    }

    this.insertCode(target, mapping, fieldNode, wrapper);
  }
}
