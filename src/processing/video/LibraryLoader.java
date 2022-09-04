/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-22 The Processing Foundation
  Copyright (c) 2011-12 Ben Fry and Casey Reas
  GStreamer implementation ported from GSVideo library by Andres Colubri
  Library loader based on code by Tal Shalif

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
*/

package processing.video;

import java.util.HashMap;
import java.util.Map;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;

/**
 * This class loads the gstreamer native libraries.
 *
 */
public class LibraryLoader {

  public interface DummyLibrary extends Library {
  }

  private static LibraryLoader instance;

  static final Object[][] WINDOWS_MINGW_DEPENDENCIES = {
      // Base GStreamer native libraries for a COMPLETE MINGW installation 
      { "avcodec-58", new String[] {}, false },
      { "avfilter-7", new String[] {}, false },
      { "avformat-58", new String[] {}, false },
      { "avutil-56", new String[] {}, false },
      { "libass-9", new String[] {}, false },
      { "libbz2", new String[] {}, false },
      { "libcairo-2", new String[] {}, false },
      { "libcairo-gobject-2", new String[] {}, false },
      { "libcairo-script-interpreter-2", new String[] {}, false },
      { "libcharset-1", new String[] {}, false },
      { "libcroco-0.6-3", new String[] {}, false },
      { "libcrypto-1_1-x64", new String[] {}, false },
      { "libdca-0", new String[] {}, false },
      { "libdv-4", new String[] {}, false },
      { "libexpat-1", new String[] {}, false },
      { "libffi-7", new String[] {}, false },
      { "libFLAC-8", new String[] {}, false },
      { "libfontconfig-1", new String[] {}, false },
      { "libfreetype-6", new String[] {}, false },
      { "libfribidi-0", new String[] {}, false },
      { "libgcc_s_seh-1", new String[] {}, false },
      { "libgdk_pixbuf-2.0-0", new String[] {}, false },
      { "libges-1.0-0", new String[] {}, false },
      { "libgio-2.0-0", new String[] {}, false },
      { "libglib-2.0-0", new String[] {}, false },
      { "libgmodule-2.0-0", new String[] {}, false },
      { "libgobject-2.0-0", new String[] {}, false },
      { "libgraphene-1.0-0", new String[] {}, false },
      { "libgstadaptivedemux-1.0-0", new String[] {}, false },
      { "libgstallocators-1.0-0", new String[] {}, false },
      { "libgstapp-1.0-0", new String[] {}, false },
      { "libgstaudio-1.0-0", new String[] {}, false },
      { "libgstbadaudio-1.0-0", new String[] {}, false },
      { "libgstbase-1.0-0", new String[] {}, false },
      { "libgstbasecamerabinsrc-1.0-0", new String[] {}, false },
      { "libgstcheck-1.0-0", new String[] {}, false },
      { "libgstcodecparsers-1.0-0", new String[] {}, false },
      { "libgstcodecs-1.0-0", new String[] {}, false },
      { "libgstcontroller-1.0-0", new String[] {}, false },
      { "libgstd3d11-1.0-0", new String[] {}, false },
      { "libgstfft-1.0-0", new String[] {}, false },
      { "libgstgl-1.0-0", new String[] {}, false },
      { "libgstinsertbin-1.0-0", new String[] {}, false },
      { "libgstisoff-1.0-0", new String[] {}, false },
      { "libgstmpegts-1.0-0", new String[] {}, false },
      { "libgstnet-1.0-0", new String[] {}, false },
      { "libgstpbutils-1.0-0", new String[] {}, false },
      { "libgstphotography-1.0-0", new String[] {}, false },
      { "libgstplay-1.0-0", new String[] {}, false },
      { "libgstplayer-1.0-0", new String[] {}, false },
      { "libgstreamer-1.0-0", new String[] {}, false },
      { "libgstriff-1.0-0", new String[] {}, false },
      { "libgstrtp-1.0-0", new String[] {}, false },
      { "libgstrtsp-1.0-0", new String[] {}, false },
      { "libgstrtspserver-1.0-0", new String[] {}, false },
      { "libgstsctp-1.0-0", new String[] {}, false },
      { "libgstsdp-1.0-0", new String[] {}, false },
      { "libgsttag-1.0-0", new String[] {}, false },
      { "libgsttranscoder-1.0-0", new String[] {}, false },
      { "libgsturidownloader-1.0-0", new String[] {}, false },
      { "libgstvalidate-1.0-0", new String[] {}, false },
      { "libgstvideo-1.0-0", new String[] {}, false },
      { "libgstwebrtc-1.0-0", new String[] {}, false },
      { "libgthread-2.0-0", new String[] {}, false },
      { "libharfbuzz-0", new String[] {}, false },
      { "libiconv-2", new String[] {}, false },
      { "libintl-8", new String[] {}, false },
      { "libjpeg-8", new String[] {}, false },
      { "libjson-glib-1.0-0", new String[] {}, false },
      { "libkate-1", new String[] {}, false },
      { "libmp3lame-0", new String[] {}, false },
      { "libmpg123-0", new String[] {}, false },
      { "libnice-10", new String[] {}, false },
      { "libogg-0", new String[] {}, false },
      { "liboggkate-1", new String[] {}, false },
      { "libopencore-amrnb-0", new String[] {}, false },
      { "libopencore-amrwb-0", new String[] {}, false },
      { "libopenh264-6", new String[] {}, false },
      { "libopenjp2", new String[] {}, false },
      { "libopus-0", new String[] {}, false },
      { "liborc-0.4-0", new String[] {}, false },
      { "liborc-test-0.4-0", new String[] {}, false },
      { "libpango-1.0-0", new String[] {}, false },
      { "libpangocairo-1.0-0", new String[] {}, false },
      { "libpangoft2-1.0-0", new String[] {}, false },
      { "libpangowin32-1.0-0", new String[] {}, false },
      { "libpixman-1-0", new String[] {}, false },
      { "libpng16-16", new String[] {}, false },
      { "libpsl-5", new String[] {}, false },
      { "librsvg-2-2", new String[] {}, false },
      { "librtmp-1", new String[] {}, false },
      { "libsbc-1", new String[] {}, false },
      { "libSoundTouch-1", new String[] {}, false },
      { "libsoup-2.4-1", new String[] {}, false },
      { "libspandsp-2", new String[] {}, false },
      { "libspeex-1", new String[] {}, false },
      { "libsqlite3-0", new String[] {}, false },
      { "libsrt", new String[] {}, false },
      { "libsrtp2-1", new String[] {}, false },
      { "libssl-1_1-x64", new String[] {}, false },
      { "libstdc++-6", new String[] {}, false },
      { "libtag", new String[] {}, false },
      { "libtheora-0", new String[] {}, false },
      { "libtheoradec-1", new String[] {}, false },
      { "libtheoraenc-1", new String[] {}, false },
      { "libtiff-5", new String[] {}, false },
      { "libturbojpeg-0", new String[] {}, false },
      { "libvo-aacenc-0", new String[] {}, false },
      { "libvorbis-0", new String[] {}, false },
      { "libvorbisenc-2", new String[] {}, false },
      { "libvorbisfile-3", new String[] {}, false },
      { "libwavpack", new String[] {}, false },
      { "libwebrtc_audio_processing-0", new String[] {}, false },
      { "libwinpthread-1", new String[] {}, false },
      { "libx264-157", new String[] {}, false },
      { "libxml2-2", new String[] {}, false },
      { "libz-1", new String[] {}, false },
      { "libzbar-0", new String[] {}, false },
      { "swresample-3", new String[] {}, false }  
  };

  static final Object[][] WINDOWS_MSVC_DEPENDENCIES = {
      // Base GStreamer native libraries for a COMPLETE MSVC installation
      { "avcodec-58", new String[] {}, false },
      { "avfilter-7", new String[] {}, false },
      { "avformat-58", new String[] {}, false },
      { "avutil-56", new String[] {}, false },
      { "bz2", new String[] {}, false },
      { "cairo-2", new String[] {}, false },
      { "cairo-gobject-2", new String[] {}, false },
      { "cairo-script-interpreter-2", new String[] {}, false },
      { "dv-4", new String[] {}, false },
      { "ffi-7", new String[] {}, false },
      { "fontconfig-1", new String[] {}, false },
      { "fribidi-0", new String[] {}, false },
      { "gdk_pixbuf-2.0-0", new String[] {}, false },
      { "ges-1.0-0", new String[] {}, false },
      { "gio-2.0-0", new String[] {}, false },
      { "glib-2.0-0", new String[] {}, false },
      { "gmodule-2.0-0", new String[] {}, false },
      { "gobject-2.0-0", new String[] {}, false },
      { "graphene-1.0-0", new String[] {}, false },
      { "gstadaptivedemux-1.0-0", new String[] {}, false },
      { "gstallocators-1.0-0", new String[] {}, false },
      { "gstapp-1.0-0", new String[] {}, false },
      { "gstaudio-1.0-0", new String[] {}, false },
      { "gstbadaudio-1.0-0", new String[] {}, false },
      { "gstbase-1.0-0", new String[] {}, false },
      { "gstbasecamerabinsrc-1.0-0", new String[] {}, false },
      { "gstcheck-1.0-0", new String[] {}, false },
      { "gstcodecparsers-1.0-0", new String[] {}, false },
      { "gstcodecs-1.0-0", new String[] {}, false },
      { "gstcontroller-1.0-0", new String[] {}, false },
      { "gstd3d11-1.0-0", new String[] {}, false },
      { "gstfft-1.0-0", new String[] {}, false },
      { "gstgl-1.0-0", new String[] {}, false },
      { "gstinsertbin-1.0-0", new String[] {}, false },
      { "gstisoff-1.0-0", new String[] {}, false },
      { "gstmpegts-1.0-0", new String[] {}, false },
      { "gstnet-1.0-0", new String[] {}, false },
      { "gstpbutils-1.0-0", new String[] {}, false },
      { "gstphotography-1.0-0", new String[] {}, false },
      { "gstplay-1.0-0", new String[] {}, false },
      { "gstplayer-1.0-0", new String[] {}, false },
      { "gstreamer-1.0-0", new String[] {}, false },
      { "gstriff-1.0-0", new String[] {}, false },
      { "gstrtp-1.0-0", new String[] {}, false },
      { "gstrtsp-1.0-0", new String[] {}, false },
      { "gstrtspserver-1.0-0", new String[] {}, false },
      { "gstsctp-1.0-0", new String[] {}, false },
      { "gstsdp-1.0-0", new String[] {}, false },
      { "gsttag-1.0-0", new String[] {}, false },
      { "gsttranscoder-1.0-0", new String[] {}, false },
      { "gsturidownloader-1.0-0", new String[] {}, false },
      { "gstvalidate-1.0-0", new String[] {}, false },
      { "gstvideo-1.0-0", new String[] {}, false },
      { "gstwebrtc-1.0-0", new String[] {}, false },
      { "gstwinrt-1.0-0", new String[] {}, false },
      { "gthread-2.0-0", new String[] {}, false },
      { "harfbuzz", new String[] {}, false },
      { "intl-8", new String[] {}, false },
      { "json-glib-1.0-0", new String[] {}, false },
      { "libass-9", new String[] {}, false },
      { "libcharset-1", new String[] {}, false },
      { "libcroco-0.6-3", new String[] {}, false },
      { "libcrypto-1_1-x64", new String[] {}, false },
      { "libdca-0", new String[] {}, false },
      { "libexpat-1", new String[] {}, false },
      { "libFLAC-8", new String[] {}, false },
      { "libfreetype-6", new String[] {}, false },
      { "libgcc_s_seh-1", new String[] {}, false },
      { "libiconv-2", new String[] {}, false },
      { "libjpeg-8", new String[] {}, false },
      { "libkate-1", new String[] {}, false },
      { "libmp3lame-0", new String[] {}, false },
      { "libmpg123-0", new String[] {}, false },
      { "libogg-0", new String[] {}, false },
      { "liboggkate-1", new String[] {}, false },
      { "libopencore-amrnb-0", new String[] {}, false },
      { "libopencore-amrwb-0", new String[] {}, false },
      { "libpng16-16", new String[] {}, false },
      { "librsvg-2-2", new String[] {}, false },
      { "librtmp-1", new String[] {}, false },
      { "libsbc-1", new String[] {}, false },
      { "libspandsp-2", new String[] {}, false },
      { "libspeex-1", new String[] {}, false },
      { "libsrt", new String[] {}, false },
      { "libssl-1_1-x64", new String[] {}, false },
      { "libstdc++-6", new String[] {}, false },
      { "libtheora-0", new String[] {}, false },
      { "libtheoradec-1", new String[] {}, false },
      { "libtheoraenc-1", new String[] {}, false },
      { "libtiff-5", new String[] {}, false },
      { "libturbojpeg-0", new String[] {}, false },
      { "libvo-aacenc-0", new String[] {}, false },
      { "libvorbis-0", new String[] {}, false },
      { "libvorbisenc-2", new String[] {}, false },
      { "libvorbisfile-3", new String[] {}, false },
      { "libwinpthread-1", new String[] {}, false },
      { "libx264-157", new String[] {}, false },
      { "libxml2-2", new String[] {}, false },
      { "libzbar-0", new String[] {}, false },
      { "nice-10", new String[] {}, false },
      { "openh264-6", new String[] {}, false },
      { "openjp2", new String[] {}, false },
      { "opus-0", new String[] {}, false },
      { "orc-0.4-0", new String[] {}, false },
      { "orc-test-0.4-0", new String[] {}, false },
      { "pango-1.0-0", new String[] {}, false },
      { "pangocairo-1.0-0", new String[] {}, false },
      { "pangoft2-1.0-0", new String[] {}, false },
      { "pangowin32-1.0-0", new String[] {}, false },
      { "pixman-1-0", new String[] {}, false },
      { "psl-5", new String[] {}, false },
      { "soup-2.4-1", new String[] {}, false },
      { "sqlite3-0", new String[] {}, false },
      { "srtp2-1", new String[] {}, false },
      { "swresample-3", new String[] {}, false },
      { "wavpack", new String[] {}, false },
      { "z-1", new String[] {}, false }      
  };

  static final Object[][] LINUX_DEPENDENCIES = {
       // Base GStreamer native libraries from a meson build
  
      // GLib libraries
      { "glib-2.0", new String[] {}, false },
      { "gobject-2.0", new String[] {}, false },
      { "gio-2.0", new String[] {}, false },
      { "gmodule-2.0", new String[] {}, false },
      { "gthread-2.0", new String[] {}, false },

      // Core GStreamer libraries... the order of these libraries is important (while it does
      // not seem to matter for Windows. For example, if gstbase comes before gstreamer, then
      // plugin scanning crashes with "cannot register existing type 'GstObject'" error
      { "gstreamer-1.0", new String[] {}, false },
      { "gstbase-1.0", new String[] {}, false },
      { "gsturidownloader-1.0", new String[] {}, false },
      { "gstadaptivedemux-1.0", new String[] {}, false },
      { "gstapp-1.0", new String[] {}, false },
      { "gsttag-1.0", new String[] {}, false },
      { "gstvideo-1.0", new String[] {}, false },
      { "gstaudio-1.0", new String[] {}, false },
      { "gstpbutils-1.0", new String[] {}, false },
      { "gstplay-1.0", new String[] {}, false },
      { "gstplayer-1.0", new String[] {}, false },
      { "gstbadaudio-1.0", new String[] {}, false },
      { "gstbasecamerabinsrc-1.0", new String[] {}, false },
      { "gstcheck-1.0", new String[] {}, false },
      { "gstcodecparsers-1.0", new String[] {}, false },
      { "gstcontroller-1.0", new String[] {}, false },
      { "gstfft-1.0", new String[] {}, false },
      { "gstinsertbin-1.0", new String[] {}, false },
      { "gstisoff-1.0", new String[] {}, false },
      { "gstmpegts-1.0", new String[] {}, false },
      { "gstnet-1.0", new String[] {}, false },
      { "gstphotography-1.0", new String[] {}, false },
      { "gstallocators-1.0", new String[] {}, false },
      { "libgstcodecs-1.0", new String[] {}, false },
      { "gstriff-1.0", new String[] {}, false },
      { "gstrtp-1.0", new String[] {}, false },
      { "gstrtsp-1.0", new String[] {}, false },
      { "gstsdp-1.0", new String[] {}, false },
      { "gstsctp-1.0", new String[] {}, false },
      { "gstrtspserver-1.0", new String[] {}, false },
      { "gstvalidate-1.0", new String[] {}, false },
      { "gstvalidate-default-overrides-1.0", new String[] {}, false },
      { "gstwebrtc-1.0", new String[] {}, false },
      { "gsttranscoder-1.0", new String[] {}, false },

      // External libraries      
      { "xml2", new String[] {}, false },
      { "avutil", new String[] {}, false },
      { "swresample", new String[] {}, false },
      { "swscale", new String[] {}, false },
      { "avcodec", new String[] {}, false },
      { "avformat", new String[] {}, false },
      { "avresample", new String[] {}, false },
      { "avfilter", new String[] {}, false },
      { "avdevice", new String[] {}, false },
      { "avtp", new String[] {}, false },
      { "cairo-gobject", new String[] {}, false },
      { "cairo-script-interpreter", new String[] {}, false },
      { "cairo", new String[] {}, false },
      { "dv", new String[] {}, false },
      { "fdk_aac", new String[] {}, false },
      { "fontconfig", new String[] {}, false },
      { "freetype", new String[] {}, false },
      { "fribidi", new String[] {}, false },
      { "ges-1.0", new String[] {}, false },
      { "harfbuzz-gobject", new String[] {}, false },
      { "harfbuzz", new String[] {}, false },
      { "harfbuzz-subset", new String[] {}, false },
      { "jpeg", new String[] {}, false },
      { "json-glib-1.0", new String[] {}, false },
      { "microdns", new String[] {}, false },
      { "mp3lame", new String[] {}, false },
      { "nice", new String[] {}, false },
      { "ogg", new String[] {}, false },
      { "openh264", new String[] {}, false },
      { "openjp2", new String[] {}, false },
      { "opus", new String[] {}, false },
      { "orc-0.4", new String[] {}, false },
      { "orc-test-0.4", new String[] {}, false },
      { "pango-1.0", new String[] {}, false },
      // { "pangocairo-1.0", new String[] {}, false }, // Seems broken in 1.20.3
      { "pangoft2-1.0", new String[] {}, false },
      { "pixman-1", new String[] {}, false },
      { "png16", new String[] {}, false },
      { "postproc", new String[] {}, false },
      { "psl", new String[] {}, false },
      { "soup-2.4", new String[] {}, false },
      { "soup-gnome-2.4", new String[] {}, false },
      { "sqlite3", new String[] {}, false },
      { "vorbisenc", new String[] {}, false },
      { "vorbisfile", new String[] {}, false },
      { "vorbis", new String[] {}, false }      
};

  static Object[][] dependencies;


  private static final Map<String, Object> loadedMap =
    new HashMap<>();


  private static final int RECURSIVE_LOAD_MAX_DEPTH = 5;


  private LibraryLoader() {
  }


  private void preLoadLibs(int winBuildType) {
    if (Platform.isWindows()) {
      if (winBuildType == 0) {
        System.err.println("Seems like you are trying to use GStreamer native libraries older than 1.20, which are not supported.");
        return;
      } else if (winBuildType == 1) {
        dependencies = WINDOWS_MINGW_DEPENDENCIES;
      } else if (winBuildType == 2) {
        dependencies = WINDOWS_MSVC_DEPENDENCIES;
      }

    } else if (Platform.isLinux()) {
      dependencies = LINUX_DEPENDENCIES;
    } else {
      // No need for dependencies pre-loading on MacOS
      return;
    }

    for (Object[] a : dependencies) {
      load(a[0].toString(), DummyLibrary.class, true, 0, (Boolean) a[2]);
    }
  }


  static private String[] findDeps(String name) {
    for (Object[] a : dependencies) {
      if (name.equals(a[0])) {
        return (String[]) a[1];
      }
    }

    // library dependency load chain unspecified - probably client call
    return new String[] { };
  }


  public Object load(String name, Class<?> clazz, boolean reqLib) {
    return load(name, clazz, true, 0, reqLib);
  }


  private Object load(String name, Class<?> clazz, boolean forceReload,
      int depth, boolean reqLib) {

    assert depth < RECURSIVE_LOAD_MAX_DEPTH : String.format(
        "recursive max load depth %s has been exceeded", depth);

    Object library = loadedMap.get(name);

    if (null == library || forceReload) {

      // Logger.getAnonymousLogger().info(String.format("%" + ((depth + 1) * 2)
      // + "sloading %s", "->", name));

      try {
        String[] deps = findDeps(name);

        for (String lib : deps) {
          load(lib, DummyLibrary.class, false, depth + 1, reqLib);
        }

        library = loadLibrary(name, clazz, reqLib);

        if (library != null) {
          loadedMap.put(name, library);
        }
      } catch (Exception e) {
        if (reqLib)
          throw new RuntimeException(String.format(
            "can not load required library %s", name, e));
        else
          System.out.println(String.format("can not load library %s", name, e));
      }
    }

    return library;
  }


  private static Object loadLibrary(String name, Class<?> clazz,
    boolean reqLib) {

    // Logger.getAnonymousLogger().info(String.format("loading %s", name));

    String[] nameFormats;
    nameFormats = Platform.isWindows() ? new String[] { "lib%s", "lib%s-0",
        "%s" } : new String[] { "%s-0", "%s" };

    UnsatisfiedLinkError linkError = null;

    for (String fmt : nameFormats) {
      try {      
        String s = String.format(fmt, name);
        //System.out.println("Trying to load library file " + s);
        Object obj = Native.loadLibrary(s, clazz);
        //System.out.println("Loaded library " + s + " successfully!");
        return obj;
      } catch (UnsatisfiedLinkError ex) {
        linkError = ex;
      }
    }

    if (reqLib)
      throw new UnsatisfiedLinkError(
        String.format(
          "can't load library %s (%1$s|lib%1$s|lib%1$s-0) with " +
          "-Djna.library.path=%s. Last error:%s",
          name, System.getProperty("jna.library.path"), linkError));
    else {
      System.out.println(String.format(
        "can't load library %s (%1$s|lib%1$s|lib%1$s-0) with " +
        "-Djna.library.path=%s. Last error:%s",
        name, System.getProperty("jna.library.path"), linkError));
      return null;
    }
  }


  public static synchronized LibraryLoader getInstance(int winBuildType) {
    if (null == instance) {
      instance = new LibraryLoader();
      instance.preLoadLibs(winBuildType);
    }
    return instance;
  }
}
