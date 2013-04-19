#!/bin/sh

GROUP=com/threerings/nexus

if [ -z "$1" ]; then
    echo "Usage: $0 M.N"
    echo "Where M.N is the version of the just performed release."
    exit 255
fi

CORE=nexus-core
COREDIR=$HOME/.m2/repository/$GROUP/$CORE/$1
if [ ! -d $COREDIR ]; then
    echo "Can't find: $COREDIR"
    echo "Is $1 the correct version?"
    exit 255
fi

echo "Unpacking $CORE-$1-javadoc.jar..."
cd javadoc/core
jar xf $COREDIR/$CORE-$1-javadoc.jar
rm -rf META-INF

SERVER=nexus-server
SERVERDIR=$HOME/.m2/repository/$GROUP/$SERVER/$1
echo "Unpacking $SERVER-$1-javadoc.jar..."
cd ../server
jar xf $SERVERDIR/$SERVER-$1-javadoc.jar
rm -rf META-INF

cd ..
echo "Adding and committing updated docs..."
git add core server
git commit -m "Updated docs for $1 release." .
cd ..
git push

echo "Tagging docs..."
git tag -a v$1 -m "Tagged docs for $1 release."
git push origin v$1

echo "Thank you, please drive through."
