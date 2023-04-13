# class-redefiner
Experimental library to modify classes at runtime using [ASM](https://asm.ow2.io/) and Java Agent (Instrumentation)

## Current issues
- [CDS](https://openjdk.org/jeps/310) will ignore modified classes
- Injected code will use modified class classloader and so can be accessed only from it
- Currently working only with Java 11+

## Getting started

1) Add Maven repository
   + Using Gradle
     ```groovy
     maven { url = uri("https://maven.pkg.github.com/UserNugget/class-redefiner") }
     ```

   + Using Maven
     ```xml
     <repository>
       <id>class-redefiner</id>
       <name>class-redefiner repo</name>
       <url>https://maven.pkg.github.com/UserNugget/class-redefiner</url>
     </repository>
     ```

2) Add dependency
   + Using Gradle
     ```groovy
     implementation("kk:class-redefiner:0.0.1")
     ```

   + Using Maven
     ```xml
     <dependency>
       <groupId>kk</groupId>
       <artifactId>class-redefiner</artifactId>
       <version>0.0.1</version>
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

You can find more examples [here](https://github.com/UserNugget/class-redefiner/tree/main/examples/src/main/java/kk/examples)