if [ -n "$TRAVIS_TAG" ]
then
    export BUILD_TYPE=release/$TRAVIS_TAG
    mvn -B versions:set -DnewVersion="$TRAVIS_TAG" --file pom.xml
fi

echo BUILD_TYPE: "$BUILD_TYPE"

echo "Importing GPG keys"
openssl aes-256-cbc -K $encrypted_2d46c2ddc73e_key -iv $encrypted_2d46c2ddc73e_iv -in codesigning.asc.enc -out codesigning.asc -d
gpg --fast-import codesigning.asc

mvn -B -U -P ci,docs deploy -DskipTests=true --settings settings.xml
