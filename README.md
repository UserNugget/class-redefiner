# class-redefiner
Library to modify classes at runtime using [Instrumentation](https://docs.oracle.com/en/java/javase/20/docs/api/java.instrument/java/lang/instrument/Instrumentation.html)

## Limitations
- Require Java 11+ to work
- [CDS](https://openjdk.org/jeps/310) will ignore modified classes
  (see [this](https://github.com/openjdk/jdk/blob/9def4538ab5456d689fd289bdef66fd1655773bc/src/hotspot/share/classfile/systemDictionaryShared.cpp#L272-L274) and [this](https://github.com/openjdk/jdk/blob/9def4538ab5456d689fd289bdef66fd1655773bc/src/hotspot/share/classfile/systemDictionaryShared.cpp#L545-L560))

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

## Examples

1) Basic:
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
  // Basic method that always return true
  public static boolean alwaysTrue() {
    return true;
  }
}

// Create new redefiner
ClassRedefiner redefiner = new ClassRedefiner(
  new StandardAttachTypes(),
  new StandardHandlerTypes()
);

// Redefine ClassValue using ClassValueMapping
try {
  redefiner.transformClass(ClassValueMapping.class);
} catch (Throwable throwable) {
  throw new IllegalStateException("unable to apply mapping", throwable);
}

// Verify that ClassValue::alwaysTrue now return false
System.out.println("alwaysTrue is " + ClassValue.alwaysTrue());
```