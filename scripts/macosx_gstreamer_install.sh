#!/bin/bash

# This script downloads the GStreamer image with separate package to install
# only those needed by Processing:
# https://gstreamer.freedesktop.org/documentation/deploying/mac-osx.html?gi-language=c#deploy-only-necessary-packages-using-the-provided-ones

GST_VERSION=${1:-1.16.0}
INSTALL_PACKAGES=0 # version 1.16.0 does not yet have a pacakages image
GST_PKG_URL="https://gstreamer.freedesktop.org/data/pkg/osx"
DOWNLOAD_PATH="."
TARGET_PATH="/"
CURRENT_PATH=`pwd`

if [ $INSTALL_PACKAGES -eq 1 ]
then
  echo "PACKAGES INSTALL..."

  SRC_FILE="$GST_PKG_URL/$GST_VERSION/gstreamer-1.0-$GST_VERSION-x86_64-packages.dmg"
  DEST_FILE="$DOWNLOAD_PATH/gstreamer-1.0-$GST_VERSION-x86_64-packages.dmg"

  curl $SRC_FILE --output $DEST_FILE

  sudo hdiutil attach $DOWNLOAD_PATH/gstreamer-1.0-$GST_VERSION-x86_64-packages.dmg
  cd /Volumes/hdidir

  sudo installer -pkg base-system-1.0-${GST_VERSION}-x86_64.pkg -target $TARGET_PATH
  sudo installer -pkg base-crypto-${GST_VERSION}-x86_64.pkg -target $TARGET_PATH
  sudo installer -pkg gstreamer-1.0-system-${GST_VERSION}-x86_64.pkg -target $TARGET_PATH
  sudo installer -pkg gstreamer-1.0-core-${GST_VERSION}-x86_64.pkg -target $TARGET_PATH  
  sudo installer -pkg gstreamer-1.0-playback-${GST_VERSION}-x86_64.pkg -target $TARGET_PATH
  sudo installer -pkg gstreamer-1.0-capture-${GST_VERSION}-x86_64.pkg -target $TARGET_PATH
  sudo installer -pkg gstreamer-1.0-codecs-${GST_VERSION}-x86_64.pkg -target $TARGET_PATH
  sudo installer -pkg gstreamer-1.0-libav-${GST_VERSION}-x86_64.pkg -target $TARGET_PATH
  sudo installer -pkg gstreamer-1.0-net-${GST_VERSION}-x86_64.pkg -target $TARGET_PATH
  sudo installer -pkg gstreamer-1.0-editing-${GST_VERSION}-x86_64.pkg -target $TARGET_PATH

  cd $CURRENT_PATH
  hdiutil detach /Volumes/hdidir
  rm $DOWNLOAD_PATH/gstreamer-1.0-$GST_VERSION-x86_64-packages.dmg

  echo "DONE..."
else 
  echo "FULL INSTALL..."

  SRC_FILE="$GST_PKG_URL/$GST_VERSION/gstreamer-1.0-$GST_VERSION-x86_64.pkg"
  DEST_FILE="$DOWNLOAD_PATH/gstreamer-1.0-$GST_VERSION-x86_64.pkg"

  # curl $SRC_FILE --output $DEST_FILE

  sudo installer -pkg $DOWNLOAD_PATH/gstreamer-1.0-$GST_VERSION-x86_64.pkg -target $TARGET_PATH

  # rm $DOWNLOAD_PATH/gstreamer-1.0-$GST_VERSION-x86_64.pkg

  echo "DONE..."  
fi