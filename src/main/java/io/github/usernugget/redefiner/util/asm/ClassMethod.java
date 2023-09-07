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

import io.github.usernugget.redefiner.util.asm.instruction.Insns;
import io.github.usernugget.redefiner.util.asm.instruction.immutable.Injected;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

public class ClassMethod extends MethodNode implements AccessFlags {
  public ClassFile owner;

  public ClassMethod(ClassFile owner, int access, String name, String descriptor) {
    this(owner, access, name, descriptor, null, null);
  }

  public ClassMethod(
    ClassFile owner, int access, String name,
    String descriptor, String signature, String[] exceptions
  ) {
    super(Opcodes.ASM9, access, name, descriptor, signature, exceptions);
    this.owner = owner;
    this.instructions = new Insns();
  }

  public Insns getInstructions() {
    return (Insns) this.instructions;
  }

  public Type returnType() {
    return Type.getReturnType(this.desc);
  }

  public boolean isSpecial() {
    return this.name.equals("<init>") || this.name.equals("<clinit>");
  }

  public int descSize() {
    return Type.getArgumentsAndReturnSizes(this.desc) >> 2;
  }

  public int newVariable() {
    return maxVariable() + 1;
  }

  public int maxVariable() {
    int size = this.descSize();
    for (AbstractInsnNode inst : this.instructions) {
      if (inst instanceof VarInsnNode) {
        VarInsnNode var = (VarInsnNode) inst;
        int nodeSize = var.getOpcode() == Opcodes.DLOAD ||
                       var.getOpcode() == Opcodes.DSTORE ||
                       var.getOpcode() == Opcodes.LLOAD ||
                       var.getOpcode() == Opcodes.LSTORE ? 2 : 1;

        size = Math.max(size, var.var + nodeSize);
      } else if (inst instanceof IincInsnNode) {
        size = Math.max(size, ((IincInsnNode) inst).var + 1);
      }
    }

    return size;
  }

  public void increaseVariableIndex(int idx) {
    int size = this.descSize();
    for (AbstractInsnNode inst : this.instructions) {
      if (inst instanceof VarInsnNode && !(inst instanceof Injected)) {
        VarInsnNode var = (VarInsnNode) inst;
        if (var.var > size) {
          var.var += idx;
        }
      } else if (inst instanceof IincInsnNode && !(inst instanceof Injected)) {
        IincInsnNode iinc = (IincInsnNode) inst;
        if (iinc.var > size) {
          iinc.var += idx;
        }
      }
    }
  }

  public void eachAnnotation(Consumer<AnnotationNode> consumer) {
    Ops.eachAnnotation(consumer, this.visibleAnnotations, this.invisibleAnnotations);
  }

  public void addTryCatchBlocks(List<TryCatchBlockNode> tryCatchBlocks) {
    if (tryCatchBlocks == null) return;

    if (this.tryCatchBlocks == null) {
      this.tryCatchBlocks = new ArrayList<>(tryCatchBlocks);
    } else {
      this.tryCatchBlocks.addAll(tryCatchBlocks);
    }
  }

  public String toReadableBytecode() {
    StringWriter writer = new StringWriter();
    Textifier textifier = new Textifier();

    accept(new TraceMethodVisitor(null, textifier));

    textifier.print(new PrintWriter(writer));
    return writer.toString();
  }

  @Override
  public int access() {
    return this.access;
  }

  @Override
  public String toString() {
    return this.owner.name + "::" + this.name + this.desc;
  }
}
