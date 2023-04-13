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

package io.github.usernugget.redefiner.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class Java8Tools {
  public static Class<?> vmClass;

  static {
    if(JavaInternals.CLASS_MAJOR_VERSION <= 52) {
      String javaHome = System.getProperty("java.home");

      File foundTools = null;
      for(String path : new String[] { "/../lib/tools.jar", "/lib/tools.jar" }) {
        File file = new File(javaHome + path);
        if(file.exists()) {
          foundTools = file;
        }
      }

      if(foundTools == null) {
        throw new IllegalStateException("tools.jar not found");
      }

      try {
        URLClassLoader tools = new URLClassLoader(new URL[] { foundTools.toURL() });
        vmClass = tools.loadClass("com.sun.tools.attach.VirtualMachine");
      } catch (MalformedURLException e) {
        throw new Error("should not reach here", e);
      } catch (ClassNotFoundException e) {
        throw new ExceptionInInitializerError(e);
      }
    }
  }
}
