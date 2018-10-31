# What's jBATIS
The jBATIS persistence framework is a succeeding framework for iBATIS Sql Map framework which was stopped at version 2.3.4.726.

# Features & Improvements

 * cglib and asm refined and embedded.
 * jdbc null value unboxed as 0.
 * improved batch: auto tx, auto batch size and call procedure in batch
 * SIMPLE datasource fixed and refined for industrial-scale applications.
 * classpath:/ibatis.ini for global properties
 * 2.4 dtd and dynamic sql tag alias added
 * new APIs in org.ibatis.client.SqlMapClient and its super interfaces.
 * new Spring integration support.
 * jBATIS Persistence APIs
 * cache support refined and 3rd lib refreshed.
 * dialect and paging query support for modern databases.
 
# Maven artifact
```xml
  <dependency>
    <groupId>com.dukeware</groupId>
    <artifactId>jbatis</artifactId>
    <version>2.4.8</version>
  </dependency>
```

# User Guide

 see doc/jbatis-guide.md