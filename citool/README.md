# KeY: ci-tool 1.3.0

ci-tool is a utility for supporting Java and JML contracts in Continuous Integration pipelines 
by providing support for checking the proofability of JML with [KeY](https://key-project.org).

ci-tool is licensed under GPLv2+.

If you any suggestion feel to contact: [Alexander Weigl](https://formal.iti.kit.edu/weigl).

## Usage:

To use the ci-tool add the following lines to the ci config.

1. Get the latest version from the server:
   ```bash
   $ wget -O ci-tool.jar  https://formal.iti.kit.edu/weigl/ci-tool/latest.php?type=all
   ```

  New with `1.3.0`, we also offer a minified version without an included KeY version.
  You can get this version with
  
  ```bash
  $ wget -O ci-tool.jar  https://formal.iti.kit.edu/weigl/ci-tool/latest.php?type=mini
  ```

  Remember, that you need to add `KeY-2.X.Y-exe.jar` to your classpath. 
  
2. Call ci-tool with your key-files or java file (or folder).
   ci-tool tries to verify all proofs automatically and uses found proofs or script files.
   ``` 
   $ java -jar <jarfile> [files]
   ```

3. Find more parameters with `-h`
  
   ``` 
   $ java -jar <jarfile> -h 
   ```


### Examples

For travis-ci:

```
jdk:
  - openjdk11

language: java
install:
  - wget -O ci-tool.jar  https://formal.iti.kit.edu/weigl/ci-tool/latest.php?type=all
script:
  - javac simplified/Keyserver.java
  - java -jar ci-tool.jar simplified/Keyserver.java
```

## Changelog

* [1.3.0 (2021-03-03)](https://formal.iti.kit.edu/ci-tool/ci-tool-1.3.0-all.jar): 
  - Added supporting for writing Junit xml files  (still beta)
    - A new option `--xml-output [FILE]` was added. If specified, an XML file is written in the JUnit format.
  - ci-tool lives now in a standalone repository.
  - Bundled with KeY-2.8.0 `a3cd916f85e4a52092723a6bb05c97d98b531763` from (Feb 15 2021)
  - `key.ui` is not included anymore.
  - The bug fix for model fields in set statements is revoked, and ci-tool build on KeY's `master` branch.
  - [Minimized Version](https://formal.iti.kit.edu/ci-tool/ci-tool-1.3.0-mini.jar)

* [1.3.0](https://formal.iti.kit.edu/ci-tool/keyext.citool-1.3.0--alpha-all.jar): Adding junit xml output
  - ci-tool lives now in a standalone repository.
  - The bug fix for model fields in set statements is revoked, and ci-tool build on KeY's `master` branch.
  - A new option `--xml-output [FILE]` was added. If specified, an XML file is written in the JUnit format.
  

* [1.2.0](https://formal.iti.kit.edu/ci-tool/keyext.citool-1.2.0-all.jar): Hot-fix for KeY
   - repairing model fields in set statements

* [1.1.0](https://formal.iti.kit.edu/ci-tool/keyext.citool-1.1.0-all.jar): bug fixes and support for proofs in key-files
  - Change of console output 
  - Fix loading of proof files
  - Add download URL and a website
  - Known Bug: Something with the proof (search) path is wrong.

* [1.0.0](https://formal.iti.kit.edu/ci-tool/keyext.citool-1.0.0-all.jar) initial working version (*24.01.2020*)
  - first release of this project after positive of the a small `jshell` utility.
  - deploy on VerifyThis LTC 2020 repository  
  
  
## Future features

* Accumulate statistics
* If proof remains open, 
* Calculate a coverage based on statements in the 
