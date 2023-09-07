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
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

// Converts Op.returnOp(*) to plain returns
public class OpHandler implements Handler {
  private static final String OP_NAME = Type.getInternalName(Op.class);

  @Override
  public void handleMethod(MethodChange change) {
    Type targetReturn = change.findTargetMethod().returnType();

    Insns mappingCode = change.getMappingMethod().getInstructions();
    for (AbstractInsnNode instruction : mappingCode) {
      if (instruction instanceof MethodInsnNode) {
        MethodInsnNode method = (MethodInsnNode) instruction;
        // TODO: think about method comparsion under obfuscation
        if (method.owner.equals(OP_NAME)) {
          Type[] arguments = Type.getArgumentTypes(method.desc);
          Type opReturn = arguments.length == 0 ? Type.VOID_TYPE : arguments[0];

          // type != type && !(object || object)
          if (opReturn.getSort() != targetReturn.getSort() &&
              ((opReturn.getSort() != Type.OBJECT &&
                opReturn.getSort() != Type.ARRAY) ||
               (targetReturn.getSort() != Type.OBJECT &&
                targetReturn.getSort() != Type.ARRAY))) {
            // Not a number, so can't convert
            if ((opReturn.getSort() < Type.BOOLEAN ||
                 opReturn.getSort() > Type.DOUBLE ||
                 targetReturn.getSort() < Type.BOOLEAN ||
                 targetReturn.getSort() > Type.DOUBLE)) {
              throw new IllegalStateException(
                "Op::returnOp type " + opReturn + " is not compatible with " + targetReturn
              );
            }

            Insns conversion = new Insns();

            switch (opReturn.getSort()) {
              case Type.INT: {
                switch (targetReturn.getSort()) {
                  case Type.BYTE:    { conversion.op(Opcodes.I2B); break; }
                  case Type.CHAR:    { conversion.op(Opcodes.I2C); break; }
                  case Type.SHORT:   { conversion.op(Opcodes.I2S); break; }
                  case Type.FLOAT:   { conversion.op(Opcodes.I2F); break; }
                  case Type.LONG:    { conversion.op(Opcodes.I2L); break; }
                  case Type.DOUBLE:  { conversion.op(Opcodes.I2D); break; }
                }
                break;
              }
              case Type.FLOAT: {
                switch (targetReturn.getSort()) {
                  case Type.CHAR:
                  case Type.BYTE:
                  case Type.SHORT:
                  case Type.INT:
                  case Type.BOOLEAN: {
                    conversion.op(Opcodes.F2I);
                    switch (targetReturn.getSort()) {
                      case Type.CHAR:  { conversion.op(Opcodes.I2C); break; }
                      case Type.BYTE:  { conversion.op(Opcodes.I2B); break; }
                      case Type.SHORT: { conversion.op(Opcodes.I2S); break; }
                    }
                  }
                  case Type.LONG:    { conversion.op(Opcodes.F2L); break; }
                  case Type.DOUBLE:  { conversion.op(Opcodes.F2D); break; }
                }
                break;
              }
              case Type.LONG: {
                switch (targetReturn.getSort()) {
                  case Type.CHAR:
                  case Type.BYTE:
                  case Type.SHORT:
                  case Type.INT:
                  case Type.BOOLEAN: {
                    conversion.op(Opcodes.L2I);
                    switch (targetReturn.getSort()) {
                      case Type.CHAR:  { conversion.op(Opcodes.I2C); break; }
                      case Type.BYTE:  { conversion.op(Opcodes.I2B); break; }
                      case Type.SHORT: { conversion.op(Opcodes.I2S); break; }
                    }
                  }
                  case Type.FLOAT:   { conversion.op(Opcodes.L2F); break; }
                  case Type.DOUBLE:  { conversion.op(Opcodes.F2D); break; }
                }
                break;
              }
              case Type.DOUBLE: {
                switch (targetReturn.getSort()) {
                  case Type.CHAR:
                  case Type.BYTE:
                  case Type.SHORT:
                  case Type.INT:
                  case Type.BOOLEAN: {
                    conversion.op(Opcodes.D2I);
                    switch (targetReturn.getSort()) {
                      case Type.CHAR:  { conversion.op(Opcodes.I2C); break; }
                      case Type.BYTE:  { conversion.op(Opcodes.I2B); break; }
                      case Type.SHORT: { conversion.op(Opcodes.I2S); break; }
                    }
                  }
                  case Type.FLOAT:   { conversion.op(Opcodes.D2F); break; }
                  case Type.LONG:    { conversion.op(Opcodes.D2L); break; }
                }
                break;
              }
            }

            conversion.add(Ops.injectedReturnOp(targetReturn));

            mappingCode.insert(instruction, conversion);
            mappingCode.remove(instruction);
          } else {
            mappingCode.set(instruction, Ops.injectedReturnOp(opReturn));
          }
        }
      }
    }
  }
}
