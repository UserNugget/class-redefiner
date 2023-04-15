# class-redefiner
Library to modify classes at runtime using [ASM](https://asm.ow2.io/) and Java Agent (Instrumentation)

## Limitations
- [CDS](https://openjdk.org/jeps/310) will ignore modified classes

## Getting started

1) Add dependency
   + Using Gradle
     ```groovy
     implementation("io.github.usernugget:class-redefiner:1.1.0")
     ```

   + Using Maven
     ```xml
     <dependency>
       <groupId>io.github.usernugget</groupId>
       <artifactId>class-redefiner</artifactId>
       <version>1.1.0</version>
     </dependency>
     ```

## Examples

1) Basic:
```java
// Mapping that will modify ClassValue behavior
@Mapping(ClassValue.class)
public static final class ClassValueMapping {
  // Inject code on top of ClassValue::alwaysTrue
  @Head
  public static void alwaysTrue() {
    Op.returnValue(false);
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
   new DefaultAnnotationRegistry()
);

// Redefine ClassValue using ClassValueMapping
redefiner.applyMapping(ClassValueMapping.class);

// Verify that ClassValue::alwaysTrue now return false
System.out.println("alwaysTrue is " + ClassValue.alwaysTrue());
```

You can find more examples [here](https://github.com/UserNugget/class-redefiner/tree/main/examples/src/main/java/io/github/usernugget/redefiner/examples)