#!/bin/sh -l

set -e # End immediately if a command fails
set -u # End if trying to access an undefined variable

SOURCE_FILE_NAME="commands.md"
SOURCE_FILE_PATH="./docs/$SOURCE_FILE_NAME"
TARGET_FOLDER="docs/bots/lily"
TARGET_BRANCH="$1"
GIT_EMAIL="nocomment1105@outlook.com"
GIT_NAME="NoComment1105"
API_TOKEN="$2"
COMMIT_MESSAGE="Update LilyBot docs"

echo "Using API_TOKEN"
GIT_REPOSITORY="https://HyacinthBots:$API_TOKEN@github.com/HyacinthBots/website.git"

echo "Making a temporary directory to work from"
DIR=$(mktemp -d)

echo "Cloning target git repository 'website' on branch $TARGET_BRANCH"
git config --global user.email "$GIT_EMAIL"
git config --global user.name "$GIT_NAME"

{
    git clone --single-branch --depth 1 --branch "$TARGET_BRANCH" "$GIT_REPOSITORY" "$DIR"
} || {
	echo "::error::Could not clone the website repository."
	exit 1
}
ls -la "$DIR"

echo "Checking the source file exists"
if [ ! -f "$SOURCE_FILE_PATH" ]
then
    echo "::error::$SOURCE_FILE_PATH does not exist!"
    echo "Please verify the path to the file and try again!"
    exit 1
fi

echo "Getting contents of $SOURCE_FILE_NAME"
FILE_CONTENTS=`cat $SOURCE_FILE_PATH`
FRONT_MATTER="---
title: Commands List
permalink: /bots/lily/commands
---
"

echo "Prepending front matter for website"
WEB_READY_CONTENT="$FRONT_MATTER$FILE_CONTENTS"

echo "Checking contents of files are different"
EXISTING_CONTENT=`cat $DIR/$TARGET_FOLDER/$SOURCE_FILE_NAME`

if [ "$EXISTING_CONTENT" = "$WEB_READY_CONTENT" ]
then
    echo "The contents of the files are identical. Ending task!"
else
    echo "Removing old file"
    rm -f "$DIR/$TARGET_FOLDER/$SOURCE_FILE_NAME"

    echo "Writing new contents to $TARGET_FOLDER/$SOURCE_FILE_NAME"
    echo "$WEB_READY_CONTENT" | tee -a "$DIR/$TARGET_FOLDER/$SOURCE_FILE_NAME"

    echo "Changing into cloned directory"
    cd "$DIR"

    echo "Adding files to git"
    git add .

    echo "Committing the changes"
    git diff-index --quiet HEAD || git commit --message "$COMMIT_MESSAGE"

    echo "Pushing commit to remote"
    git push "$GIT_REPOSITORY" --set-upstream "$TARGET_BRANCH"
fi

echo "Cleaning up"
{
    rm -rf "$DIR"
} || {
    echo "Unable to clean working repository. The files will remain on this system."
    echo "If this is a GitHub actions runner, you can safely ignore this message."
}
