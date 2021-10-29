#!/bin/bash
echo "Building Lily..."
chmod +x gradlew
./gradlew --no-daemon build
echo "Starting Lily..."
cp "lily.properties" "./build/libs/lily.properties"
FILE=.env
if test -f "$FILE"; then
	cp ".env" "./build/libs/.env"
	cd ./build/libs/
	java -Xms1G -Xmx1G -XX:+UseShenandoahGC -jar LilyBot-1.0-all.jar
else
	echo "$FILE does not exist - Cannot start Lily. Make sure you have read the documentation to allow for proper setup."
fi
