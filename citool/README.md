# KeY: ci-tool 1.4.0

ci-tool is a utility for supporting Java and JML contracts in Continuous Integration pipelines 
by providing support for checking the proofability of JML with [KeY](https://key-project.org).

ci-tool is licensed under GPLv3.

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

  If you are using the `mini` version, you also need to add `KeY-2.X.Y-exe.jar` 
  to your classpath. 
  
2. Call ci-tool with your key-files or java file (or folder).
   ci-tool tries to verify all proofs automatically and uses found proofs or script files.

   ```bash 
   $ java -jar <jarfile> [files]
   ```

4. Find more parameters with `-h`
  
   ```bash
   $ java -jar <jarfile> -h 
   ```


### Examples

For travis-ci:

```yaml
jdk:
  - openjdk11

language: java
install:
  - wget -O ci-tool.jar  https://formal.iti.kit.edu/weigl/ci-tool/latest.php?type=all
script:
  - javac simplified/Keyserver.java
  - java -jar ci-tool.jar simplified/Keyserver.java
```

[Check out this file for Github workflows.](https://github.com/KeYProject/verification-project-template/blob/main/.github/workflows/KeY-check.yml)


## Changelog

* [1.5.0-pre (2022-XX-XX)](https://formal.iti.kit.edu/ci-tool/ci-tool-XXX.jar):
  - [Minimized Version for the use with different KeY version](https://formal.iti.kit.edu/ci-tool/ci-tool-XXX-mini.jar)
  - [ADD] If you store your proof files under the non-default filename, you can enable 
    `--read-contract-names-from-file` in ci-tool, for detection based on content. 
  - [ADD] `--append-statistics` gives you the opportunity to accumulate the statistics of different runs into one file.
  - [ADD] `--color={auto,yes,no}` allows you to configure the use of colors
  - [ENH] more robustness on loading KeY proofs


* [1.4.0 (2022-03-23)](https://formal.iti.kit.edu/ci-tool/ci-tool-1.4.0-all.jar):
  - [Minimized Version for the use with different KeY version](https://formal.iti.kit.edu/ci-tool/ci-tool-1.4.0-mini.jar)
  - Added support for writing out proof statistic file into a JSON file, see option `-s`.  
    If you want to gather statistics you can use [script like this.](https://github.com/KeYProject/verification-project-template/blob/main/tools/statistics.py)
  - Bug-fix: Proof-path are now treated correctly and recursively.
  - Bundled with KeY-2.10.0 from KeY's maven repository.

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

## Usage hints

ci-tool uses the KeY's default contract names and filenames to find proof files. 
With the following script, you can restore the filenames based on the stored contract names inside the file. 

```bash
#!/bin/bash -x

line=$(zcat "$1" | grep 'name=' | head -n 1 | tr -d '\r\n')
name=${line:5}
name=$(echo $name | tr '\\$?|<>:*"/[]' "_______+'-()")
mv "$1" "${name}.proof.gz"
```
  
  
## Future features

* Accumulate statistics
* If proof remains open, 
* Calculate a coverage based on statements 