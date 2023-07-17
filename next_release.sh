#!/bin/bash
# This simple script gets the current release and increments the release number.
# Abort on an error
set -e

die () {
    echo >&2 "$@"
    exit 1
}
if [ "$1" != "Major" ] && [ "$1" != "Minor" ] && [ "$1" != "Patch" ]; then
  die "Only accepts Major, Minor or Patch as argument."
fi

### Increments the part of the string
## $1: version itself
## $2: number of part: 0 – major, 1 – minor, 2 – patch
increment_version() {

  major=`echo "$1" | cut -d "." -f 1`
  minor=`echo "$1" | cut -d "." -f 2`
  patch=`echo "$1" | cut -d "." -f 3`

  case "$2" in
    "Major")
      major=$((major + 2))
      minor=0
      patch=0
      ;;
    "Minor")
      minor=$((minor + 2))
      patch=0
      ;;
    "Patch")
      patch=$((patch + 2))
      ;;
  esac
  echo "$major.$minor.$patch-SNAPSHOT"
}

LATEST_RELEASE=`git fetch --prune --unshallow && git describe --abbrev=0 --tags`
NEW_RELEASE=`increment_version $LATEST_RELEASE $1`
echo $NEW_RELEASE

