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

package io.github.usernugget.redefiner.changes;

import io.github.usernugget.redefiner.ClassRedefiner;
import io.github.usernugget.redefiner.annotation.ParsedAnnotation;
import io.github.usernugget.redefiner.util.asm.ClassField;
import io.github.usernugget.redefiner.util.asm.ClassFile;

public class FieldChange extends ClassChange {
  private ClassField mappingField;
  private ParsedAnnotation annotation;

  public FieldChange(
    ClassRedefiner redefiner, ClassLoader classLoader,
    Class<?> mappingJavaClass, Class<?> targetJavaClass,
    ClassFile mappingClass, ClassFile targetClass, ClassField mappingField,
    ParsedAnnotation annotation
  ) {
    super(redefiner, classLoader, mappingJavaClass, targetJavaClass, mappingClass, targetClass);
    this.mappingField = mappingField;
    this.annotation = annotation;
  }

  public ClassField getMappingField() {
    return this.mappingField;
  }

  public void setMappingField(ClassField mappingField) {
    this.mappingField = mappingField;
  }

  public ParsedAnnotation annotation() {
    return this.annotation;
  }

  public FieldChange annotation(
    ParsedAnnotation annotation
  ) {
    this.annotation = annotation;
    return this;
  }
}
