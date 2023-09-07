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

import java.util.function.Consumer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FieldNode;

public class ClassField extends FieldNode implements AccessFlags {
  public ClassFile owner;

  public ClassField(ClassFile owner, int access, String name, String descriptor) {
    this(owner, access, name, descriptor, null, null);
  }

  public ClassField(ClassFile owner, int access, String name, String descriptor, Object value) {
    this(owner, access, name, descriptor, null, value);
  }

  public ClassField(ClassFile owner, int access, String name, String descriptor, String signature, Object value) {
    super(Opcodes.ASM9, access, name, descriptor, signature, value);
    this.owner = owner;
  }

  public void eachAnnotation(Consumer<AnnotationNode> function) {
    Ops.eachAnnotation(function, this.visibleAnnotations, this.invisibleAnnotations);
  }

  @Override
  public int access() {
    return this.access;
  }

  @Override
  public String toString() {
    return this.desc + " " + this.owner + "." +this.name;
  }
}
