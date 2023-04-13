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

package io.github.usernugget.redefiner.util.asm.node;

import io.github.usernugget.redefiner.util.asm.node.immutable.ImmutableVarInsnNode;
import io.github.usernugget.redefiner.util.asm.node.immutable.ImmutableIincInsnNode;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;
import java.io.PrintWriter;
import java.io.StringWriter;
import static org.objectweb.asm.Opcodes.ASM9;
import static org.objectweb.asm.Opcodes.DLOAD;
import static org.objectweb.asm.Opcodes.DSTORE;
import static org.objectweb.asm.Opcodes.LLOAD;
import static org.objectweb.asm.Opcodes.LSTORE;

public class ClassMethod extends MethodNode {
  // Hide superclass fields
  private byte instructions;

  public ClassMethod() {
    super(ASM9);
    super.instructions = new Insts();
  }

  public ClassMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
    super(ASM9, access, name, descriptor, signature, exceptions);
    super.instructions = new Insts();
  }

  public Insts insts() {
    return (Insts) super.instructions;
  }

  public void insts(Insts insts) {
    super.instructions = insts;
  }

  public ClassMethod copy() {
    ClassMethod newMethod = new ClassMethod(
       this.access, this.name, this.desc, this.signature,
       this.exceptions == null ? null : this.exceptions.toArray(new String[0])
    );
    accept(newMethod);
    return newMethod;
  }

  public int findDescLocals() {
    return Type.getArgumentsAndReturnSizes(this.desc) >> 2;
  }

  public int findLocals() {
    int locals = findDescLocals();
    for (AbstractInsnNode i : super.instructions) {
      if (i instanceof VarInsnNode) {
        int varSize = i.getOpcode() != LLOAD && i.getOpcode() != DLOAD &&
                      i.getOpcode() != LSTORE && i.getOpcode() != DSTORE ? 1 : 2;
        locals = Math.max(locals, ((VarInsnNode) i).var + varSize);
      } else if (i instanceof IincInsnNode) {
        locals = Math.max(locals, ((IincInsnNode) i).var + 1);
      }
    }

    return locals;
  }

  public int newLocal() {
    return findLocals() + 1;
  }

  public void incrementLocals(Insts insts) {
    int newLocal = newLocal();
    int descLocals = findDescLocals();

    for (AbstractInsnNode i : insts) {
      if (i instanceof VarInsnNode && !(i instanceof ImmutableVarInsnNode)) {
        VarInsnNode varNode = (VarInsnNode) i;
        if (varNode.var <= descLocals) {
          continue;
        }

        varNode.var += newLocal;
      } else if (i instanceof IincInsnNode && !(i instanceof ImmutableIincInsnNode)) {
        IincInsnNode iincNode = (IincInsnNode) i;
        if (iincNode.var <= descLocals) {
          continue;
        }

        iincNode.var += newLocal;
      }
    }
  }

  public boolean isInitializer() {
    return "<init>".equals(this.name) || "<clinit>".equals(this.name);
  }

  public boolean isPackagePrivate() {
    return (this.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE)) == 0;
  }

  public boolean isPublic() {
    return (this.access & Opcodes.ACC_PUBLIC) != 0;
  }

  public boolean isPrivate() {
    return (this.access & Opcodes.ACC_PRIVATE) != 0;
  }

  public boolean isProtected() {
    return (this.access & Opcodes.ACC_PROTECTED) != 0;
  }

  public boolean isStatic() {
    return (this.access & Opcodes.ACC_STATIC) != 0;
  }

  public boolean isFinal() {
    return (this.access & Opcodes.ACC_FINAL) != 0;
  }

  public boolean isSynchronized() {
    return (this.access & Opcodes.ACC_SYNCHRONIZED) != 0;
  }

  public boolean isBridge() {
    return (this.access & Opcodes.ACC_BRIDGE) != 0;
  }

  public boolean isVarargs() {
    return (this.access & Opcodes.ACC_VARARGS) != 0;
  }

  public boolean isNative() {
    return (this.access & Opcodes.ACC_NATIVE) != 0;
  }

  public boolean isAbstract() {
    return (this.access & Opcodes.ACC_ABSTRACT) != 0;
  }

  public boolean isStrict() {
    return (this.access & Opcodes.ACC_STRICT) != 0;
  }

  public boolean isSynthetic() {
    return (this.access & Opcodes.ACC_SYNTHETIC) != 0;
  }

  public boolean isMandated() {
    return (this.access & Opcodes.ACC_MANDATED) != 0;
  }

  public boolean isDeprecated() {
    return (this.access & Opcodes.ACC_DEPRECATED) != 0;
  }

  public String toBytecodeString() {
    StringWriter writer = new StringWriter();
    Textifier textifier = new Textifier();

    accept(new TraceMethodVisitor(textifier.visitMethod(
       this.access, this.name, this.desc, this.signature,
       this.exceptions == null ? null : this.exceptions.toArray(new String[0])
    )));

    PrintWriter printWriter = new PrintWriter(writer);
    textifier.print(new PrintWriter(writer));
    printWriter.flush();

    return writer.toString();
  }

  @Override
  public String toString() {
    return this.name + this.desc;
  }
}
