#!/bin/sh

for f in *.java; do
    g=`echo $f | perl -ne 's/.java$// && print'`
    echo "Simulating $g..."
    java $g > $g.res
done
