#!/bin/bash

gradle miniShadowJar shadowJar --parallel

echo "
<html>
<head>
<style>body {width:60em; margin:auto;}</style>
<title>ci-tool</title></head>
<body>" > build/libs/index.html
pandoc README.md >> build/libs/index.html
echo "</body></html>">> build/libs/index.html
rsync -v build/libs/* i57adm.ira.uka.de:htdocs/weigl/ci-tool
