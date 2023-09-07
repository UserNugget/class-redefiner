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

package io.github.usernugget.redefiner.util.asm.instruction;

import io.github.usernugget.redefiner.util.asm.ClassField;
import io.github.usernugget.redefiner.util.asm.ClassMethod;
import io.github.usernugget.redefiner.util.asm.Ops;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class Insns extends InsnList {
  protected <T extends AbstractInsnNode> T addInternal(T node) {
    assert !this.contains(node);

    this.add(node);
    return node;
  }

  public InsnNode op(int op) { return this.addInternal(Ops.op(op)); }
  public IntInsnNode intOp(int op, int value) { return this.addInternal(Ops.intOp(op, value)); }
  public VarInsnNode varOp(int op, int var) { return this.addInternal(Ops.varOp(op, var)); }
  public VarInsnNode varOp(Type type, int op, int var) { return this.addInternal(Ops.varOp(type, op, var)); }
  public IincInsnNode iincOp(int var, int increment) { return this.addInternal(Ops.iincOp(var, increment)); }
  public JumpInsnNode jumpOp(int op, LabelNode label) { return this.addInternal(Ops.jumpOp(op, label)); }
  public TypeInsnNode typeOp(int op, String type) { return this.addInternal(Ops.typeOp(op, type)); }
  public FieldInsnNode fieldOp(int op, ClassField field) { return this.addInternal(Ops.fieldOp(op, field)); }
  public FieldInsnNode fieldOp(int op, String owner, String field, String desc) { return this.addInternal(Ops.fieldOp(op, owner, field, desc)); }
  public MethodInsnNode methodOp(int op, String owner, String method, String desc, boolean interface_) { return this.addInternal(Ops.methodOp(op, owner, method, desc, interface_)); }
  public AbstractInsnNode ldc(Object value) { return this.addInternal(Ops.ldc(value)); }
  public InsnNode ldc(boolean value) { return this.addInternal(Ops.ldc(value)); }
  public AbstractInsnNode ldc(int value) { return this.addInternal(Ops.ldc(value)); }
  public AbstractInsnNode ldc(float value) { return this.addInternal(Ops.ldc(value)); }
  public AbstractInsnNode ldc(long value) { return this.addInternal(Ops.ldc(value)); }
  public AbstractInsnNode ldc(double value) { return this.addInternal(Ops.ldc(value)); }
  public VarInsnNode loadOp(Type type, int var) { return this.addInternal(Ops.loadOp(type, var)); }
  public VarInsnNode storeOp(Type type, int var) { return this.addInternal(Ops.storeOp(type, var)); }
  public FieldInsnNode fieldGetter(ClassField field) { return this.addInternal(Ops.fieldGetter(field)); }
  public FieldInsnNode fieldSetter(ClassField field) { return this.addInternal(Ops.fieldSetter(field)); }
  public MethodInsnNode invoke(ClassMethod method) { return this.addInternal(Ops.invoke(method)); }
  public InsnNode returnOp(String desc) { return this.addInternal(Ops.returnOp(desc)); }
  public InsnNode returnOp(Type type) { return this.addInternal(Ops.returnOp(type)); }
}
