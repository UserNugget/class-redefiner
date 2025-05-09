package io.github.usernugget.tests.loader;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class FakeClassLoader extends URLClassLoader {
  public FakeClassLoader(String name, URL klass, ClassLoader parent) throws Throwable {
    super(makeJar(name, klass), parent);
  }

  private static URL[] makeJar(String name, URL klass) throws Throwable {
    Path path = Files.createTempFile("class-redefiner-test", ".jar");
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        Files.deleteIfExists(path);
      } catch (IOException e) {
        throw new IllegalStateException("failed to remove temp file", e);
      }
    }));

    try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(path))) {
      jos.putNextEntry(new JarEntry(name.replace('.', '/') + ".class"));
      klass.openConnection().getInputStream().transferTo(jos);
    }

    return new URL[] { path.toUri().toURL() };
  }
}
