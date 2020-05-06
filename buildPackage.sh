#!/bin/bash
dir=`dirname "$0"`
cd "$dir"
rm -rf project/obj
haxelib run lime rebuild . ios || exit
haxelib run lime rebuild . ios -debug || exit
rm -rf project/obj
#rm -f extension-admob.zip
#zip -r extension-admob.zip extension haxelib.json include.xml project ndll dependencies frameworks
