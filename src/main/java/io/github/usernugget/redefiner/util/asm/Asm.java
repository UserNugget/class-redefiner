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

package io.github.usernugget.redefiner.util.asm;

import io.github.usernugget.redefiner.util.asm.node.immutable.ImmutableInsnNode;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Type.VOID;

public class Asm {
  public static int varOp(Type type, int opcode) {
    return type.getSort() == VOID ? -1 : type.getOpcode(opcode);
  }

  public static int loadToStore(int loadOpcode) {
    return ISTORE + (loadOpcode - ILOAD);
  }

  public static int storeToLoad(int storeOpcode) {
    return ILOAD + (storeOpcode - ISTORE);
  }

  public static boolean isReturn(int opcode) {
    return opcode >= IRETURN && opcode <= RETURN;
  }

  public static boolean isUnmoddedReturn(AbstractInsnNode insnNode) {
    return isReturn(insnNode.getOpcode()) && !(insnNode instanceof ImmutableInsnNode);
  }
}
