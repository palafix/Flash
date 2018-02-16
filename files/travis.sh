#!/usr/bin/env bash

# Add appropriate files for encryption

rm phase.tar.enc
cd ..
tar cvf phase.tar files/gplay-keys.json files/play.keystore files/play.properties files/test.keystore app/fabric.properties
travis encrypt-file phase.tar --add
rm phase.tar
mv phase.tar.enc files/