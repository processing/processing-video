/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-19 The Processing Foundation
  Copyright (c) 2011-12 Ben Fry and Casey Reas
  GStreamer implementation ported from GSVideo library by Andres Colubri

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

import org.freedesktop.gstreamer.*;
import processing.core.PApplet;
import processing.core.PConstants;

import java.io.File;
import java.nio.ByteOrder;
import java.nio.file.Paths;
import java.util.List;

/**
 * This class contains some basic functions used by the rest of the classes in
 * this library.
 */
public class Video implements PConstants {
  // Allows to set the amount of desired debug output from GStreamer, according to the following table:
  // https://gstreamer.freedesktop.org/documentation/tutorials/basic/debugging-tools.html?gi-language=c#printing-debug-information
  public static int DEBUG_LEVEL = 1;

  // Path that the video library will use to load the GStreamer base libraries 
  // and plugins from. They can be passed from the application using the 
  // gstreamer.library.path and gstreamer.plugin.path system variables (see
  // comments in initImpl() below).
  public static String gstreamerLibPath = "";
  public static String gstreamerPluginPath = "";  
  
  protected static boolean usingGStreamerSystemInstall = false;

  // OpenGL texture used as buffer sink by default, when the renderer is 
  // GL-based. This can improve performance significantly, since the video 
  // frames are automatically copied into the texture without passing through 
  // the pixels arrays, as well as having the color conversion into RGBA handled 
  // natively by GStreamer.
  protected static boolean useGLBufferSink = true;    

  protected static boolean defaultGLibContext = false;
  
  protected static long INSTANCES_COUNT = 0;
  
  protected static int bitsJVM;
  static {
    bitsJVM = PApplet.parseInt(System.getProperty("sun.arch.data.model"));
  }  
  
  
  static protected void init() {
    if (INSTANCES_COUNT == 0) {
      initImpl();
    }
    INSTANCES_COUNT++;
  }
  
  
  static protected void restart() {
    removePlugins();
    Gst.deinit();
    initImpl();
  }
   
  
  static protected void initImpl() {
    // The video library loads the GStreamer libraries according to the following
    // priority:
    // 1) If the VM argument "gstreamer.library.path" exists, it will use it as the
    //    root location of the libraries. This is typically the case when running 
    //    the library from Eclipse.
    // 2) If the environmental variable is GSTREAMER_1_0_ROOT_X86(_64) is defined then 
    //    will try to use its contents as the root path of the system install of GStreamer.
    // 3) If GSTREAMER_1_0_ROOT_X86(_64) is not defined, then will try to use default 
    //    install locations of GStreamer on Windows and Mac, if they exist.
    // 4) If none of the above works, the bundled version of GStreamer will be used.
    // In this way, priority is given to the system installation of GStreamer. 
    String libPath = System.getProperty("gstreamer.library.path");
    if (libPath != null) {
      gstreamerLibPath = libPath;
      
      // If the GStreamer installation referred by gstreamer.library.path is not
      // a system installation, then the path containing the plugins needs to be
      // specified separately, otherwise the plugins will be automatically 
      // loaded from the default location. The system property for the plugin
      // path is "gstreamer.plugin.path"
      String pluginPath = System.getProperty("gstreamer.plugin.path");
      if (pluginPath != null) {
        gstreamerPluginPath = pluginPath;
      }
      
      usingGStreamerSystemInstall = false;
    } else {
      String rootPath = "";
      if (bitsJVM == 64 && System.getenv("GSTREAMER_1_0_ROOT_X86_64") != null) {
        // Get 64-bit root of GStreamer install
        rootPath = System.getenv("GSTREAMER_1_0_ROOT_X86_64");
      } else if (bitsJVM == 32 && System.getenv("GSTREAMER_1_0_ROOT_X86") != null) {
        // Get 32-bit root of GStreamer install
        rootPath = System.getenv("GSTREAMER_1_0_ROOT_X86");  
      }
      
      if (!rootPath.equals("")) {
        if (PApplet.platform == MACOSX) {
          gstreamerLibPath = Paths.get(rootPath, "lib").toString();
        } else {
          gstreamerLibPath = Paths.get(rootPath, "bin").toString();
        }
        File path = new File(gstreamerLibPath);
        if (path.exists()) {
          // We have a system install of GStreamer
          usingGStreamerSystemInstall = true;
        } else {
          // The environmental variables contain invalid paths...
          gstreamerLibPath = "";
        }
      } else {
        // No environmental variables defined, will try some default locations
        if (PApplet.platform == MACOSX) {
          rootPath = "/Library/Frameworks/GStreamer.framework/Versions/1.0";
          gstreamerLibPath = Paths.get(rootPath, "lib").toString(); 
        } else if (PApplet.platform == WINDOWS) {
          // We are on Windows.
          if (bitsJVM == 64) {
            rootPath = "C:\\gstreamer\\1.0\\x86_64";
          } else {
            rootPath = "C:\\gstreamer\\1.0\\x86";
          }
          gstreamerLibPath = Paths.get(rootPath, "bin").toString();
        }
        
        File path = new File(gstreamerLibPath);
        if (path.exists()) {
          // We have a system install of GStreamer
          usingGStreamerSystemInstall = true;
          if (bitsJVM == 64) {
            Environment.libc.setenv("GSTREAMER_1_0_ROOT_X86_64", gstreamerLibPath, 1);  
          } else {
            Environment.libc.setenv("GSTREAMER_1_0_ROOT_X86", gstreamerLibPath, 1); 
          }          
        }        
      }
      
      if (usingGStreamerSystemInstall) {
        if (System.getenv("GST_PLUGIN_SYSTEM_PATH") != null) {
          gstreamerPluginPath = System.getenv("GST_PLUGIN_SYSTEM_PATH");
        } else {
          if (PApplet.platform == WINDOWS) {
            gstreamerPluginPath = Paths.get(rootPath, "lib", "gstreamer-1.0").toString();
          } else {
            gstreamerPluginPath = Paths.get(gstreamerLibPath, "gstreamer-1.0").toString();          }
        }
        File path = new File(gstreamerPluginPath);
        if (!path.exists()) {
          gstreamerPluginPath = "";
        }        
      }
    } 
      
    if (libPath == null && !usingGStreamerSystemInstall) {
      // Paths are build automatically from the current location of the video
      // library.
      if (PApplet.platform == LINUX) {    
        buildLinuxPaths();
      } else if (PApplet.platform == WINDOWS) {
        buildWindowsPaths();
      } else if (PApplet.platform == MACOSX) {
        buildMacOSXPaths();
      }      
    }
    
    if (!gstreamerLibPath.equals("")) {
      System.setProperty("jna.library.path", gstreamerLibPath);
    }
    
    Environment.libc.setenv("GST_DEBUG", String.valueOf(DEBUG_LEVEL), 1);    

    if (!usingGStreamerSystemInstall) {
      // Disable the use of gst-plugin-scanner on environments where we're
      // not using the host system's installation of GStreamer
      // the problem with gst-plugin-scanner is that the library expects it
      // to exist at a specific location determined at build time
      if (PApplet.platform != LINUX) {
        Environment.libc.setenv("GST_REGISTRY_FORK", "no", 1);
      }

      // Prevent globally installed libraries from being used on platforms
      // where we ship GStreamer
      if (!gstreamerPluginPath.equals("")) {
        Environment.libc.setenv("GST_PLUGIN_SYSTEM_PATH_1_0", "", 1);
      }
    }

    if (PApplet.platform == WINDOWS) {
      // Pre-loading base GStreamer libraries on Windows, otherwise nothing works
      LibraryLoader loader = LibraryLoader.getInstance();
      if (loader == null) {
        System.err.println("Cannot load GStreamer libraries.");
      }
    }    

    String[] args = { "" };
    Gst.setUseDefaultContext(defaultGLibContext);
    Gst.init("Processing core video", args);
   
    if (!usingGStreamerSystemInstall) {
      // Plugins are scanned explicitly from the bindings if using the
      // local GStreamer
      addPlugins();
    }
  
    // output GStreamer version, lib path, plugin path
    // and whether a system install is being used
    printGStreamerInfo();
  }

  static protected void printGStreamerInfo() {
    System.out.println("Processing video library using GStreamer " + Gst.getVersion());
  }

  
  static protected void addPlugins() {
    if (!gstreamerPluginPath.equals("")) {
      Registry reg = Registry.get();
      boolean res;
      res = reg.scanPath(gstreamerPluginPath);
      if (!res) {
        System.err.println("Cannot load GStreamer plugins from " + gstreamerPluginPath);
      }
    }
  }
  
  
  static protected void removePlugins() {
    Registry reg = Registry.get();
    List<Plugin> list = reg.getPluginList();
    for (Plugin plg : list) {
      reg.removePlugin(plg);
    }    
  }
  
  
  static protected void buildLinuxPaths() {
    // JNA automatically tries all library paths known to the host system's
    // ldconfig, so we'd even catch locations like /usr/local/lib etc
    gstreamerLibPath = "";
    gstreamerPluginPath = "";
  }

  
  static protected void buildWindowsPaths() {
    LibraryPath libPath = new LibraryPath();
    String path = libPath.get();
    gstreamerLibPath = buildGStreamerLibPath(path, "windows" + bitsJVM);
    if (!gstreamerLibPath.equals("")) {
      gstreamerPluginPath = Paths.get(gstreamerLibPath, "gstreamer-1.0").toString();
    }
  }

  
  static protected void buildMacOSXPaths() {
    LibraryPath libPath = new LibraryPath();
    String path = libPath.get();
    gstreamerLibPath = buildGStreamerLibPath(path, "macosx");
    if (!gstreamerLibPath.equals("")) {
      gstreamerPluginPath = Paths.get(gstreamerLibPath, "gstreamer-1.0").toString();
    }    
  }

  
  static protected String buildGStreamerLibPath(String base, String os) {        
    File path = new File(base + os);
    if (path.exists()) {
      return base + os; 
    } else {     	
    	usingGStreamerSystemInstall = true;  
    	return "";  
    }
  }

  
  static protected float nanoSecToSecFrac(long nanosec) {
    return (float)(nanosec / 1E9);
  }

  
  static protected long secToNanoLong(float sec) {
    Double f = Double.valueOf(sec * 1E9);
    return f.longValue();
  }
  
  
  /**
   * Reorders an OpenGL pixel array (RGBA) into ARGB. The array must be
   * of size width * height.
   * @param pixels int[]
   */
  static protected void convertToARGB(int[] pixels, int width, int height) {
    int t = 0;
    int p = 0;
    if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
      // RGBA to ARGB conversion: shifting RGB 8 bits to the right,
      // and placing A 24 bits to the left.
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          int pixel = pixels[p++];
          pixels[t++] = (pixel >>> 8) | ((pixel << 24) & 0xFF000000);
        }
      }
    } else {
      // We have to convert ABGR into ARGB, so R and B must be swapped,
      // A and G just brought back in.
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          int pixel = pixels[p++];
          pixels[t++] = ((pixel & 0xFF) << 16) | ((pixel & 0xFF0000) >> 16) |
                        (pixel & 0xFF00FF00);
        }
      }
    }
  }  
}
