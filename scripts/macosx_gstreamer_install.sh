#!/bin/bash

# This script downloads the latest GStreamer universal runtime package

GST_VERSION=${1:-1.20.3}
GST_PKG_URL="https://gstreamer.freedesktop.org/data/pkg/osx"
DOWNLOAD_PATH="."
TARGET_PATH="/"
CURRENT_PATH=`pwd`

echo "FULL INSTALL..."

SRC_FILE="$GST_PKG_URL/$GST_VERSION/gstreamer-1.0-$GST_VERSION-universal.pkg"
DEST_FILE="$DOWNLOAD_PATH/gstreamer-1.0-$GST_VERSION-universal.pkg"

curl $SRC_FILE --output $DEST_FILE

sudo installer -pkg $DOWNLOAD_PATH/gstreamer-1.0-$GST_VERSION-universal.pkg -target $TARGET_PATH

rm $DOWNLOAD_PATH/gstreamer-1.0-$GST_VERSION-universal.pkg

echo "DONE..."