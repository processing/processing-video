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

  static final Object[][] WINDOWS_DEPENDENCIES = {
      // Core GStreamer libraries
      { "libgstadaptivedemux-1.0-0", new String[] {}, false },
      { "libgstallocators-1.0-0", new String[] {}, false },
      { "libgstapp-1.0-0", new String[] {}, false },
      { "libgstaudio-1.0-0", new String[] {}, false },
      { "libgstbadaudio-1.0-0", new String[] {}, false },
      { "libgstbase-1.0-0", new String[] {}, false },
      { "libgstbasecamerabinsrc-1.0-0", new String[] {}, false },
      { "libgstcheck-1.0-0", new String[] {}, false },
      { "libgstcodecparsers-1.0-0", new String[] {}, false },
      { "libgstcontroller-1.0-0", new String[] {}, false },
      { "libgstfft-1.0-0", new String[] {}, false },
      { "libgstgl-1.0-0", new String[] {}, false },
      { "libgstinsertbin-1.0-0", new String[] {}, false },
      { "libgstisoff-1.0-0", new String[] {}, false },
      { "libgstmpegts-1.0-0", new String[] {}, false },
      { "libgstnet-1.0-0", new String[] {}, false },
      { "libgstpbutils-1.0-0", new String[] {}, false },
      { "libgstphotography-1.0-0", new String[] {}, false },
      { "libgstplayer-1.0-0", new String[] {}, false },
      { "libgstreamer-1.0-0", new String[] {}, false },
      { "libgstriff-1.0-0", new String[] {}, false },
      { "libgstrtp-1.0-0", new String[] {}, false },
      { "libgstrtsp-1.0-0", new String[] {}, false },
      { "libgstrtspserver-1.0-0", new String[] {}, false },
      { "libgstsctp-1.0-0", new String[] {}, false },
      { "libgstsdp-1.0-0", new String[] {}, false },
      { "libgsttag-1.0-0", new String[] {}, false },
      { "libgsturidownloader-1.0-0", new String[] {}, false },
      { "libgstvideo-1.0-0", new String[] {}, false },
      { "libgstwebrtc-1.0-0", new String[] {}, false },

      // External libraries
      { "libbz2", new String[] {}, false },
      { "libcairo-2", new String[] {}, false },
      { "libcairo-gobject-2", new String[] {}, false },
      { "libcairo-script-interpreter-2", new String[] {}, false },
      { "libcroco-0.6-3", new String[] {}, false },
      { "libcrypto-1_1-x64", new String[] {}, false },
      { "libdv-4", new String[] {}, false },
      { "libexpat-1", new String[] {}, false },
      { "libffi-7", new String[] {}, false },
      { "libFLAC-8", new String[] {}, false },
      { "libfontconfig-1", new String[] {}, false },
      { "libfreetype-6", new String[] {}, false },
      { "libfribidi-0", new String[] {}, false },
      { "libgcc_s_sjlj-1", new String[] {}, false },
      { "libgdk_pixbuf-2.0-0", new String[] {}, false },
      { "libgio-2.0-0", new String[] {}, false },
      { "libglib-2.0-0", new String[] {}, false },
      { "libgmodule-2.0-0", new String[] {}, false },
      { "libgmp-10", new String[] {}, false },
      { "libgnutls-30", new String[] {}, false },
      { "libgnutlsxx-28", new String[] {}, false },
      { "libgobject-2.0-0", new String[] {}, false },
      { "libgomp-1", new String[] {}, false },
      { "libgraphene-1.0-0", new String[] {}, false },
      { "libgthread-2.0-0", new String[] {}, false },
      { "libharfbuzz-0", new String[] {}, false },
      { "libhogweed-4", new String[] {}, false },
      { "libintl-8", new String[] {}, false },
      { "libjpeg-8", new String[] {}, false },
      { "libjson-glib-1.0-0", new String[] {}, false },
      { "libkate-1", new String[] {}, false },
      { "libmp3lame-0", new String[] {}, false },
      { "libmpg123-0", new String[] {}, false },
      { "libnettle-6", new String[] {}, false },
      { "libnice-10", new String[] {}, false },
      { "libogg-0", new String[] {}, false },
      { "liboggkate-1", new String[] {}, false },
      { "libopenh264", new String[] {}, false },
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
      { "librsvg-2-2", new String[] {}, false },
      { "libsbc-1", new String[] {}, false },
      { "libsoup-2.4-1", new String[] {}, false },
      { "libspandsp-2", new String[] {}, false },
      { "libspeex-1", new String[] {}, false },
      { "libsrt", new String[] {}, false },
      { "libsrtp", new String[] {}, false },
      { "libssl-1_1-x64", new String[] {}, false },
      { "libstdc++-6", new String[] {}, false },
      { "libtag", new String[] {}, false },
      { "libtasn1-6", new String[] {}, false },
      { "libtheora-0", new String[] {}, false },
      { "libtheoradec-1", new String[] {}, false },
      { "libtheoraenc-1", new String[] {}, false },
      { "libtiff-5", new String[] {}, false },
      { "libturbojpeg-0", new String[] {}, false },
      { "libusrsctp-1", new String[] {}, false },
      { "libvorbis-0", new String[] {}, false },
      { "libvorbisenc-2", new String[] {}, false },
      { "libvorbisfile-3", new String[] {}, false },
      { "libwavpack-1", new String[] {}, false },
      { "libwinpthread-1", new String[] {}, false },
      { "libxml2-2", new String[] {}, false },
      { "libz-1", new String[] {}, false },
      { "avcodec-58", new String[] {}, false },
      { "avfilter-7", new String[] {}, false },
      { "avformat-58", new String[] {}, false },
      { "avutil-56", new String[] {}, false },
      { "swresample-3", new String[] {}, false }
    };

  static final Object[][] LINUX_DEPENDENCIES = {
      // GLib libraries
      { "glib-2.0", new String[] {}, false },
      { "gobject-2.0", new String[] {}, false },
      { "gio-2.0", new String[] {}, false },
      { "gmodule-2.0", new String[] {}, false },
      { "gthread-2.0", new String[] {}, false },

      // Core GStreamer libraries
      { "gstreamer-1.0", new String[] {}, false },
      { "gstbase-1.0", new String[] {}, false },
      { "gsturidownloader-1.0", new String[] {}, false },
      { "gstadaptivedemux-1.0", new String[] {}, false },
      { "gstapp-1.0", new String[] {}, false },
      { "gsttag-1.0", new String[] {}, false },
      { "gstvideo-1.0", new String[] {}, false },
      { "gstaudio-1.0", new String[] {}, false },
      { "gstpbutils-1.0", new String[] {}, false },
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
      { "gstriff-1.0", new String[] {}, false },
      { "gstrtp-1.0", new String[] {}, false },
      { "gstrtsp-1.0", new String[] {}, false },
      { "gstsdp-1.0", new String[] {}, false },
      { "gstsctp-1.0", new String[] {}, false },
      { "gstrtspserver-1.0", new String[] {}, false },
      { "gstvalidate-1.0", new String[] {}, false },
      { "gstvalidate-default-overrides-1.0", new String[] {}, false },
      { "gstwebrtc-1.0", new String[] {}, false },

      // External libraries
      { "avutil", new String[] {}, false },
      { "swresample", new String[] {}, false },
      { "swscale", new String[] {}, false },
      { "avcodec", new String[] {}, false },
      { "avformat", new String[] {}, false },
      { "avresample", new String[] {}, false },
      { "avfilter", new String[] {}, false },
      { "avdevice", new String[] {}, false },
      { "ges-1.0", new String[] {}, false },
      { "json-glib-1.0", new String[] {}, false },
      { "nice", new String[] {}, false },
      { "x264", new String[] {}, false },
      { "openh264", new String[] {}, false },
      { "orc-0.4", new String[] {}, false },
      { "orc-test-0.4", new String[] {}, false },
      { "postproc", new String[] {}, false }
    };

  static final Object[][] MACOS_DEPENDENCIES = { };

  static final Object[][] DEFAULT_DEPENDENCIES = { };

  static final Object[][] dependencies =
    Platform.isWindows() ? WINDOWS_DEPENDENCIES :
    Platform.isLinux() ? LINUX_DEPENDENCIES :
    Platform.isMac() ? MACOS_DEPENDENCIES : DEFAULT_DEPENDENCIES;


  private static final Map<String, Object> loadedMap =
    new HashMap<>();


  private static final int RECURSIVE_LOAD_MAX_DEPTH = 5;


  private LibraryLoader() {
  }


  private void preLoadLibs() {
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


  public static synchronized LibraryLoader getInstance() {
    if (null == instance) {
      instance = new LibraryLoader();
      instance.preLoadLibs();
    }
    return instance;
  }
}
