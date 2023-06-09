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

package io.github.usernugget.redefiner.examples;

import io.github.usernugget.redefiner.ClassRedefiner;
import io.github.usernugget.redefiner.annotation.Head;
import io.github.usernugget.redefiner.annotation.Mapping;
import io.github.usernugget.redefiner.annotation.Tail;
import io.github.usernugget.redefiner.op.Op;
import io.github.usernugget.redefiner.registry.DefaultAnnotationRegistry;
import io.github.usernugget.redefiner.throwable.RedefineFailedException;

public class SimpleExample {
  @Mapping(ClassValue.class)
  public static final class ClassValueMapping {
    @Head
    public static void alwaysTrue() {
      Op.returnValue(false);
    }

    @Tail
    public static void alwaysFalse() {
      Op.returnValue(true);
    }
  }

  public static final class ClassValue {
    public static boolean alwaysTrue() {
      return true;
    }

    public static boolean alwaysFalse() {
      return false;
    }
  }

  public static void main(String[] args) throws RedefineFailedException {
    ClassRedefiner redefiner = new ClassRedefiner(
       new DefaultAnnotationRegistry()
    );

    redefiner.applyMapping(ClassValueMapping.class);

    System.out.println("alwaysTrue is " + ClassValue.alwaysTrue());
    System.out.println("alwaysFalse is " + ClassValue.alwaysFalse());
  }
}
