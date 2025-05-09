# class-redefiner
Library to modify classes at runtime using [Instrumentation](https://docs.oracle.com/en/java/javase/24/docs/api/java.instrument/java/lang/instrument/Instrumentation.html)

## Limitations
- Require Java 11+ to work
- Default use limitations: (you can create your own agent to apply mappings at any moment)
    - [CDS](https://openjdk.org/jeps/310) will ignore modified classes
      (see [this](https://github.com/openjdk/jdk/blob/9def4538ab5456d689fd289bdef66fd1655773bc/src/hotspot/share/classfile/systemDictionaryShared.cpp#L272-L274) and [this](https://github.com/openjdk/jdk/blob/9def4538ab5456d689fd289bdef66fd1655773bc/src/hotspot/share/classfile/systemDictionaryShared.cpp#L545-L560))
    - Doesn't allow adding or removing methods (unless using special JVM)
    - Doesn't allow changing method or field descriptor (unless using special JVM)
    - Doesn't allow changing interfaces or superclass for defined classes

## Built-in features
- Mappings can interact with different classloaders
    - You can make almost any code (except exceptions, object initialization or private/protected classes) use methods and fields from different classloaders
- You can inject bytecode on top or bottom of the existing method
- You can entirely replace methods
- You can wrap field setters and getters
- You can directly modify bytecode
- You can inject bytecode into platform classes

## Getting started

1) Add dependency
    + Using Gradle
      ```groovy
      implementation("io.github.usernugget:class-redefiner:2.1.1")
      ```

    + Using Maven
      ```xml
      <dependency>
        <groupId>io.github.usernugget</groupId>
        <artifactId>class-redefiner</artifactId>
        <version>2.1.1</version>
      </dependency>
      ```

## Basic usage

1) Create and initialize `ClassRedefiner`

```java
// Create new redefiner
ClassRedefiner redefiner = new ClassRedefiner(
  new StandardAttachTypes(),
  new StandardHandlerTypes()
);

// Initialize javaagent
try {
  redefiner.initializeAgent();
} catch (Throwable throwable) {
  throw new IllegalStateException("unable to initialize javaagent", throwable);
}
```

2) Create new mapping
```java
// Mapping that will modify ClassValue behavior
@Mapping(targetClass = ClassValue.class)
public static final class ClassValueMapping {
  // Inject code on top of ClassValue::alwaysTrue
  @Head
  public static void alwaysTrue() {
    Op.returnOp(false);
  }
}

public static final class ClassValue {
  // Basic method that always returns true
  public static boolean alwaysTrue() {
    return true;
  }
}
```

3) Apply created mapping
```java
// Redefine ClassValue using ClassValueMapping
try {
  redefiner.transformClass(ClassValueMapping.class);
} catch (Throwable throwable) {
  throw new IllegalStateException("unable to apply mapping", throwable);
}
```

## Examples

There are no proper examples yet, but you can use tests as examples.