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

package io.github.usernugget.redefiner.tests.other;

import io.github.usernugget.redefiner.annotation.Head;
import io.github.usernugget.redefiner.annotation.Mapping;
import io.github.usernugget.redefiner.op.Op;
import io.github.usernugget.redefiner.tests.AbstractTest;
import io.github.usernugget.redefiner.tests.other.classes.ParentClassLoaderTestTmp1;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ParentClassLoaderTest extends AbstractTest {
  public static final class ParentClassLoader extends URLClassLoader {
    public ParentClassLoader(ClassLoader parent) {
      super(new URL[] {
         ParentClassLoader.class.getProtectionDomain().getCodeSource().getLocation()
      }, parent);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
      if (name.startsWith("ParentClassLoaderTest")) {
        return super.findClass(
           // 50 chars {:
           "io.github.usernugget.redefiner.tests.other.classes." + name
        );
      }

      return super.loadClass(name);
    }
  }

  @Test
  void testParent() throws Throwable {
    ParentClassLoader firstClassLoader = new ParentClassLoader(ParentClassLoaderTest.class.getClassLoader());

    ParentClassLoader secondClassLoader = new ParentClassLoader(firstClassLoader);
    ParentClassLoader thirdClassLoader = new ParentClassLoader(firstClassLoader);

    Class<?> secondClass = thirdClassLoader.loadClass("ParentClassLoaderTestTmp1");
    Class<?> firstClass = secondClassLoader.loadClass("ParentClassLoaderTestTmp");
    Method firstMethod = firstClass.getMethod("invoke");

    assertEquals(thirdClassLoader, secondClass.getClassLoader());
    assertEquals(secondClassLoader, firstClass.getClassLoader());

    assertTrue((Boolean) firstMethod.invoke(null));
    REDEFINER.applyMapping(TmpMapping.class, secondClassLoader);
    assertFalse((Boolean) firstMethod.invoke(null));
  }

  public static boolean falseValue() {
    return false;
  }

  @Mapping(raw = "io.github.usernugget.redefiner.tests.other.classes.ParentClassLoaderTestTmp")
  public static final class TmpMapping {
    @Head
    public static void invoke() {
      if (TmpMapping.class.getClassLoader() == ParentClassLoaderTestTmp1.class.getClassLoader()) {
        throw new Error("should not reach here");
      }

      Op.returnValue(!ParentClassLoaderTestTmp1.FIELD || !ParentClassLoaderTestTmp1.invoke());
    }
  }
}
