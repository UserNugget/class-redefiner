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
import io.github.usernugget.redefiner.util.asm.ClassMethod;

public class MethodChange extends ClassChange {
  private ClassMethod mappingMethod;
  private ParsedAnnotation annotation;

  public MethodChange(
     ClassRedefiner redefiner,
     ClassLoader classLoader,
     Class<?> mappingJavaClass, Class<?> targetJavaClass,
     ClassFile mappingClass, ClassFile targetClass,
     ClassMethod mappingMethod, ParsedAnnotation annotation
  ) {
    super(redefiner, classLoader, mappingJavaClass, targetJavaClass, mappingClass, targetClass);
    this.mappingMethod = mappingMethod;
    this.annotation = annotation;
  }

  public ClassMethod findTargetMethod() {
    if (this.annotation.get("method") == null) {
      return this.getTargetClass().findMethod(this.getMappingMethod().name, null);
    }

    return this.getTargetClass().findMethod(this.annotation.getMethod("method"));
  }

  public ClassField findTargetField(String fieldName) {
    return this.getTargetClass().findField(this.annotation.getField(fieldName));
  }

  public ClassMethod getMappingMethod() {
    return this.mappingMethod;
  }

  public void setMappingMethod(ClassMethod mappingMethod) {
    this.mappingMethod = mappingMethod;
  }

  public ParsedAnnotation getAnnotation() {
    return this.annotation;
  }

  public void setAnnotation(ParsedAnnotation annotation) {
    this.annotation = annotation;
  }
}
