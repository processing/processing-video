/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org
  Copyright (c) 2004-17 Ben Fry and Casey Reas

  Originally from:
  http://blog.quirk.es/2009/11/setting-environment-variables-in-java.html

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

import com.sun.jna.Library;
import com.sun.jna.Native;

public class Environment {

  public interface WinLibC extends Library {
    public int _putenv(String name);
  }

  public interface LinuxLibC extends Library {
    public int setenv(String name, String value, int overwrite);
    public int unsetenv(String name);
  }

  static public class POSIX {
    static Object libc;
    static {
      if (System.getProperty("os.name").equals("Linux")) {
        libc = Native.loadLibrary("c", LinuxLibC.class);
      } else {
        libc = Native.loadLibrary("msvcrt", WinLibC.class);
      }
    }

    public int setenv(String name, String value, int overwrite) {
      if (libc instanceof LinuxLibC) {
        return ((LinuxLibC)libc).setenv(name, value, overwrite);
      }
      else {
        return ((WinLibC)libc)._putenv(name + "=" + value);
      }
    }

    public int unsetenv(String name) {
      if (libc instanceof LinuxLibC) {
        return ((LinuxLibC)libc).unsetenv(name);
      }
      else {
        return ((WinLibC)libc)._putenv(name + "=");
      }
    }
  }

  static POSIX libc = new POSIX();
}
