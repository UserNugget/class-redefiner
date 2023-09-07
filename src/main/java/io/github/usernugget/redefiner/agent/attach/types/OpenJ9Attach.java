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

package io.github.usernugget.redefiner.agent.attach.types;

import io.github.usernugget.redefiner.ClassRedefiner;
import io.github.usernugget.redefiner.throwables.InitializationException;
import io.github.usernugget.redefiner.util.JavaInternals;
import java.lang.invoke.MethodType;
import java.nio.file.Path;

public class OpenJ9Attach extends HotspotAttach {
  @Override
  public boolean isSupported(ClassRedefiner redefiner) {
    try {
      Class.forName("openj9.internal.tools.attach.target.Attachment");
      return true;
    } catch (ClassNotFoundException ignored) { }

    return false;
  }

  @Override
  protected void loadAgent(Path agentFile) throws InitializationException {
    try {
      Class<?> attachment = Class.forName("openj9.internal.tools.attach.target.Attachment");
      Object attachmentObject = JavaInternals.UNSAFE.allocateInstance(attachment);

      int ret = (int) JavaInternals.TRUSTED.findVirtual(attachment, "loadAgentLibraryImpl",
        MethodType.methodType(int.class, ClassLoader.class, String.class, String.class, boolean.class)
      ).invoke(attachmentObject, ClassLoader.getSystemClassLoader(), "instrument", agentFile + "=", true);

      if (ret == -1) {
        throw new IllegalStateException("internal OpenJ9 error");
      }

      if (ret != 0) {
        throw new IllegalStateException("unknown error: " + ret);
      }
    } catch (Throwable t) {
      throw new InitializationException("OpenJ9 failed to inject", t);
    }
  }
}
