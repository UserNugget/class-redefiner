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
import io.github.usernugget.redefiner.util.asm.ClassFile;

public class ClassChange {
  private ClassRedefiner redefiner;

  private ClassLoader classLoader;

  private Class<?> mappingJavaClass;
  private Class<?> targetJavaClass;

  private ClassFile mappingClass;
  private ClassFile targetClass;

  public ClassChange(
    ClassRedefiner redefiner, ClassLoader classLoader, Class<?> mappingJavaClass,
    Class<?> targetJavaClass, ClassFile mappingClass, ClassFile targetClass
  ) {
    this.redefiner = redefiner;
    this.classLoader = classLoader;
    this.mappingJavaClass = mappingJavaClass;
    this.targetJavaClass = targetJavaClass;
    this.mappingClass = mappingClass;
    this.targetClass = targetClass;
  }

  public ClassRedefiner getRedefiner() {
    return this.redefiner;
  }

  public void setRedefiner(ClassRedefiner redefiner) {
    this.redefiner = redefiner;
  }

  public ClassLoader getClassLoader() {
    return this.classLoader;
  }

  public void setClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  public Class<?> getMappingJavaClass() {
    return this.mappingJavaClass;
  }

  public void setMappingJavaClass(Class<?> mappingJavaClass) {
    this.mappingJavaClass = mappingJavaClass;
  }

  public Class<?> getTargetJavaClass() {
    return this.targetJavaClass;
  }

  public void setTargetJavaClass(Class<?> targetJavaClass) {
    this.targetJavaClass = targetJavaClass;
  }

  public ClassFile getMappingClass() {
    return this.mappingClass;
  }

  public void setMappingClass(ClassFile mappingClass) {
    this.mappingClass = mappingClass;
  }

  public ClassFile getTargetClass() {
    return this.targetClass;
  }

  public void setTargetClass(ClassFile targetClass) {
    this.targetClass = targetClass;
  }
}
