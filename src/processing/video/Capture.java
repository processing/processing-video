/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-19 The Processing Foundation
  Copyright (c) 2004-12 Ben Fry and Casey Reas
  The previous version of this code was developed by Hernando Barragan

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

import processing.core.*;

import java.nio.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.lang.reflect.*;

import org.freedesktop.gstreamer.*;
import org.freedesktop.gstreamer.Buffer;
import org.freedesktop.gstreamer.device.*;
import org.freedesktop.gstreamer.elements.*;
import org.freedesktop.gstreamer.event.SeekFlags;
import org.freedesktop.gstreamer.event.SeekType;


/**
   * ( begin auto-generated from Movie.xml )
   *
   * Datatype for storing and playing movies in Apple's QuickTime format.
   * Movies must be located in the sketch's data directory or an accessible
   * place on the network to load without an error.
   *
   * ( end auto-generated )
 *
 * @webref video
 * @usage application
 */
public class Capture extends PImage implements PConstants {
  public static String[] supportedProtocols = { "http" };
  public float frameRate;
  public Pipeline pipeline;
  
  protected boolean playing = false;
  protected boolean paused = false;
  protected boolean repeat = false;

  protected float rate;
  protected int bufWidth;
  protected int bufHeight;
  protected float volume;

  protected Method movieEventMethod;
  protected Object eventHandler;

  protected boolean available;
  protected boolean sinkReady;
  protected boolean newFrame;

  protected AppSink rgbSink = null;
  protected int[] copyPixels = null;

  protected boolean firstFrame = true;
  protected boolean seeking = false;

  protected boolean useBufferSink = false;
  protected boolean outdatedPixels = true;
  protected Object bufferSink;
  protected Method sinkCopyMethod;
  protected Method sinkSetMethod;
  protected Method sinkDisposeMethod;
  protected Method sinkGetMethod;
  
  protected String device;
  protected static List<Device> devices;    // we're caching this list for speed reasons

  NewSampleListener newSampleListener;
  NewPrerollListener newPrerollListener;
  private final Lock bufferLock = new ReentrantLock();

  /**
   *  Open the default capture device
   *  @param parent PApplet, typically "this"
   */
  public Capture(PApplet parent) {
    // attemt to use a default resolution
    this(parent, 640, 480, null, 0);
  }

  /**
   *  Open a specific capture device
   *  @param parent PApplet, typically "this"
   *  @param device device name
   *  @see Capture#list()
   *  @see Capture#listRawNames()
   */
  public Capture(PApplet parent, String device) {
    // attemt to use a default resolution
    this(parent, 640, 480, device, 0);
  }

  /**
   *  Open the default capture device with a given resolution
   *  @param parent PApplet, typically "this"
   *  @param width width in pixels
   *  @param height height in pixels
   */
  public Capture(PApplet parent, int width, int height) {
    this(parent, width, height, null, 0);
  }

  /**
   *  Open the default capture device with a given resolution and framerate
   *  @param parent PApplet, typically "this"
   *  @param width width in pixels
   *  @param height height in pixels
   *  @param fps frames per second
   */
  public Capture(PApplet parent, int width, int height, float fps) {
    this(parent, width, height, null, fps);
  }

  /**
   *  Open a specific capture device with a given resolution
   *  @param parent PApplet, typically "this"
   *  @param width width in pixels
   *  @param height height in pixels
   *  @param device device name
   *  @see Capture#list()
   */
  public Capture(PApplet parent, int width, int height, String device) {
    this(parent, width, height, device, 0);
  }

  /**
   *  Open a specific capture device with a given resolution and framerate
   *  @param parent PApplet, typically "this"
   *  @param width width in pixels
   *  @param height height in pixels
   *  @param device device name (null opens the default device)
   *  @param fps frames per second (0 uses the default framerate)
   *  @see Capture#list()
   */
  public Capture(PApplet parent, int width, int height, String device, float fps) {
    super(width, height, RGB);
    this.device = device;
    this.frameRate = fps;
    initGStreamer(parent);
  }

  /**
   * Disposes all the native resources associated to this movie.
   *
   * NOTE: This is not official API and may/will be removed at any time.
   */
  public void dispose() {
    if (pipeline != null) {
//      try {
//        if (playbin.isPlaying()) {
//          playbin.stop();
//          playbin.getState();
//        }
//      } catch (Exception e) {
//        e.printStackTrace();
//      }

      pixels = null;

      rgbSink.disconnect(newSampleListener);
      rgbSink.disconnect(newPrerollListener);
      rgbSink.dispose();
      pipeline.setState(org.freedesktop.gstreamer.State.NULL);
      pipeline.getState();
      pipeline.getBus().dispose();
      pipeline.dispose();

      parent.g.removeCache(this);
      parent.unregisterMethod("dispose", this);
      parent.unregisterMethod("post", this);
    }
  }


  /**
   * Finalizer of the class.
   */
  protected void finalize() throws Throwable {
    try {
      dispose();
    } finally {
      super.finalize();
    }
  }


  /**
   * ( begin auto-generated from Movie_frameRate.xml )
   *
   * Sets how often frames are read from the movie. Setting the <b>fps</b>
   * parameter to 4, for example, will cause 4 frames to be read per second.
   *
   * ( end auto-generated )
   *
   * @webref movie
   * @usage web_application
   * @param ifps speed of the movie in frames per second
   * @brief Sets the target frame rate
   */
  public void frameRate(float ifps) {
    if (seeking) return;

    // We calculate the target ratio in the case both the
    // current and target framerates are valid (greater than
    // zero), otherwise we leave it as 1.
    float f = (0 < ifps && 0 < frameRate) ? ifps / frameRate : 1;

    if (playing) {
      pipeline.pause();
      pipeline.getState();
    }

    long t = pipeline.queryPosition(TimeUnit.NANOSECONDS);

    boolean res;
    long start, stop;
    if (rate > 0) {
      start = t;
      stop = -1;
    } else {
      start = 0;
      stop = t;
    } 

    res = pipeline.seek(rate * f, Format.TIME, EnumSet.of(SeekFlags.FLUSH, SeekFlags.ACCURATE), SeekType.SET, start, SeekType.SET, stop);
    pipeline.getState();

    if (!res) {
      PGraphics.showWarning("Seek operation failed.");
    }

    if (playing) {
      pipeline.play();
    }


    // getState() will wait until any async state change
    // (like seek in this case) has completed
    seeking = true;
    pipeline.getState();
    seeking = false;
  }


  /**
   * ( begin auto-generated from Movie_speed.xml )
   *
   * Sets the relative playback speed of the movie. The <b>rate</b>
   * parameters sets the speed where 2.0 will play the movie twice as fast,
   * 0.5 will play at half the speed, and -1 will play the movie in normal
   * speed in reverse.
   *
   * ( end auto-generated )
   *
   * @webref movie
   * @usage web_application
   * @param irate speed multiplier for movie playback
   * @brief Sets the relative playback speed
   */
  public void speed(float irate) {
    // If the frameRate() method is called continuously with very similar
    // rate values, playback might become sluggish. This condition attempts
    // to take care of that.
    if (PApplet.abs(rate - irate) > 0.1) {
      rate = irate;
      frameRate(frameRate); // The framerate is the same, but the rate (speed) could be different.
    }
  }


  /**
   * ( begin auto-generated from Movie_duration.xml )
   *
   * Returns the length of the movie in seconds. If the movie is 1 minute and
   * 20 seconds long the value returned will be 80.0.
   *
   * ( end auto-generated )
   *
   * @webref movie
   * @usage web_application
   * @brief Returns length of movie in seconds
   */
  public float duration() {
    long nanosec = pipeline.queryDuration(TimeUnit.NANOSECONDS);
    return Video.nanoSecToSecFrac(nanosec); 
    
  }


  /**
   * ( begin auto-generated from Movie_time.xml )
   *
   * Returns the location of the playback head in seconds. For example, if
   * the movie has been playing for 4 seconds, the number 4.0 will be returned.
   *
   * ( end auto-generated )
   *
   * @webref movie
   * @usage web_application
   * @brief Returns location of playback head in units of seconds
   */
  public float time() {
    long nanosec = pipeline.queryPosition(TimeUnit.NANOSECONDS);
    return Video.nanoSecToSecFrac(nanosec);
  }


  /**
   * ( begin auto-generated from Movie_jump.xml )
   *
   * Jumps to a specific location within a movie. The parameter <b>where</b>
   * is in terms of seconds. For example, if the movie is 12.2 seconds long,
   * calling <b>jump(6.1)</b> would go to the middle of the movie.
   *
   * ( end auto-generated )
   *
   * @webref movie
   * @usage web_application
   * @param where position to jump to specified in seconds
   * @brief Jumps to a specific location
   */
  public void jump(float where) {

    if (seeking) return;

    if (!sinkReady) {
      initSink();
    }

    // Round the time to a multiple of the source framerate, in
    // order to eliminate stutter. Suggested by Daniel Shiffman
    //float fps =  getSourceFrameRate();
    //int frame = (int)(where * fps);
    //where = frame / fps;
    
    final float position = where;
    
    Gst.invokeLater(() -> {
        long dur = pipeline.queryDuration(TimeUnit.NANOSECONDS);
        if (dur > 0) {
            long pos = (long) (position * dur);
            seek(false, pos);
        }
    });
//
//    boolean res;
//    long pos = Video.secToNanoLong(where);
//
//    res = pipe.seek(rate, Format.TIME, SeekFlags.FLUSH,
//                       SeekType.SET, pos, SeekType.NONE, -1);
//
//    if (!res) {
//      PGraphics.showWarning("Seek operation failed.");
//    }

    // getState() will wait until any async state change
    // (like seek in this case) has completed
//    seeking = true;
//    pipe.getState();
//    seeking = false;
    /*
    if (seeking) return; // don't seek again until the current seek operation is done.

    if (!sinkReady) {
      initSink();
    }

    // Round the time to a multiple of the source framerate, in
    // order to eliminate stutter. Suggested by Daniel Shiffman
    float fps = getSourceFrameRate();
    int frame = (int)(where * fps);
    final float seconds = frame / fps;

    // Put the seek operation inside a thread to avoid blocking the main
    // animation thread
    Thread seeker = new Thread() {
      @Override
      public void run() {
        long pos = Video.secToNanoLong(seconds);
        boolean res = playbin.seek(rate, Format.TIME, SeekFlags.FLUSH,
                                   SeekType.SET, pos, SeekType.NONE, -1);
        if (!res) {
          PGraphics.showWarning("Seek operation failed.");
        }

        // getState() will wait until any async state change
        // (like seek in this case) has completed
        seeking = true;
        playbin.getState();
        seeking = false;
      }
    };
    seeker.start();
    */
  }


  /**
   * ( begin auto-generated from Movie_available.xml )
   *
   * Returns "true" when a new movie frame is available to read.
   *
   * ( end auto-generated )
   *
   * @webref movie
   * @usage web_application
   * @brief Returns "true" when a new movie frame is available to read.
   */
  public boolean available() {
    return available;
  }


  /**
   * ( begin auto-generated from Movie_play.xml )
   *
   * Plays a movie one time and stops at the last frame.
   *
   * ( end auto-generated )
   *
   * @webref movie
   * @usage web_application
   * @brief Plays movie one time and stops at the last frame
   */
  public void start() {
    if (seeking) return;

    if (!sinkReady) {
      initSink();
    }

    playing = true;
    paused = false;
    pipeline.play();
    pipeline.getState();
  }


  /**
   * ( begin auto-generated from Movie_loop.xml )
   *
   * Plays a movie continuously, restarting it when it's over.
   *
   * ( end auto-generated )
   *
   * @webref movie
   * @usage web_application
   * @brief Plays a movie continuously, restarting it when it's over.
   */
//  public void loop() {
//    if (seeking) return;
//
//    repeat = true;
//    play();
//  }


  /**
   * ( begin auto-generated from Movie_noLoop.xml )
   *
   * If a movie is looping, calling noLoop() will cause it to play until the
   * end and then stop on the last frame.
   *
   * ( end auto-generated )
   *
   * @webref movie
   * @usage web_application
   * @brief Stops the movie from looping
   */
  public void noLoop() {
    if (seeking) return;

    if (!sinkReady) {
      initSink();
    }

    repeat = false;
  }


  /**
   * ( begin auto-generated from Movie_pause.xml )
   *
   * Pauses a movie during playback. If a movie is started again with play(),
   * it will continue from where it was paused.
   *
   * ( end auto-generated )
   *
   * @webref movie
   * @usage web_application
   * @brief Pauses the movie
   */
  public void pause() {
    if (seeking) return;

    if (!sinkReady) {
      initSink();
    }

    playing = false;
    paused = true;
    pipeline.pause();
    pipeline.getState();
  }


  /**
   * ( begin auto-generated from Movie_stop.xml )
   *
   * Stops a movie from continuing. The playback returns to the beginning so
   * when a movie is played, it will begin from the beginning.
   *
   * ( end auto-generated )
   *
   * @webref movie
   * @usage web_application
   * @brief Stops the movie
   */
  public void stop() {
    if (seeking) return;

    if (!sinkReady) {
      initSink();
    }

    if (playing) {
      jump(0);
      playing = false;
    }
    paused = false;
    pipeline.stop();
    pipeline.getState();
  }


  /**
   * ( begin auto-generated from Movie_read.xml )
   *
   * Reads the current frame of the movie.
   *
   * ( end auto-generated )
   *
   * @webref movie
   * @usage web_application
   * @brief Reads the current frame
   */
  public synchronized void read() {
//    if (frameRate < 0) {
//      // Framerate not set yet, so we obtain from stream,
//      // which is already playing since we are in read().
//      frameRate = getSourceFrameRate();
//    }
//    if (volume < 0) {
//      // Idem for volume
//      volume = (float)playbin.getVolume();
//    }
//
//    if (copyPixels == null) {
//      return;
//    }

    if (firstFrame) {
      super.init(bufWidth, bufHeight, RGB, 1);
      firstFrame = false;
    }

    if (useBufferSink) {
      
      if (bufferSink == null) {
        Object cache = parent.g.getCache(Capture.this);
        if (cache != null) {
          setBufferSink(cache);
          getSinkMethods();
        }        
      }

    } else {
      int[] temp = pixels;
      pixels = copyPixels;
      updatePixels();
      copyPixels = temp;      
    }

    available = false;
    newFrame = true;
  }


  /**
   * Change the volume. Values are from 0 to 1.
   *
   * @param float v
   */
//  public void volume(float v) {
//    if (playing && PApplet.abs(volume - v) > 0.001f) {
//      pipe.setVolume(v);
//      volume = v;
//    }
//  }


  public synchronized void loadPixels() {
    super.loadPixels();

    if (useBufferSink && bufferSink != null) {
      /*
      if (natBuffer != null) {
        // This means that the OpenGL texture hasn't been created so far (the
        // video frame not drawn using image()), but the user wants to use the
        // pixel array, which we can just get from natBuffer.
        IntBuffer buf = natBuffer.getByteBuffer().asIntBuffer();
        buf.rewind();
        buf.get(pixels);
        Video.convertToARGB(pixels, width, height);
      } else if (sinkGetMethod != null) {
        try {
          // sinkGetMethod will copy the latest buffer to the pixels array,
          // and the pixels will be copied to the texture when the OpenGL
          // renderer needs to draw it.
          sinkGetMethod.invoke(bufferSink, new Object[] { pixels });
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      */

      try {
        // sinkGetMethod will copy the latest buffer to the pixels array,
        // and the pixels will be copied to the texture when the OpenGL
        // renderer needs to draw it.
        sinkGetMethod.invoke(bufferSink, new Object[] { pixels });
      } catch (Exception e) {
        e.printStackTrace();
      }

      outdatedPixels = false;
    }
  }


  public int get(int x, int y) {
    if (outdatedPixels) loadPixels();
    return super.get(x, y);
  }


  protected void getImpl(int sourceX, int sourceY,
                         int sourceWidth, int sourceHeight,
                         PImage target, int targetX, int targetY) {
    if (outdatedPixels) loadPixels();
    super.getImpl(sourceX, sourceY, sourceWidth, sourceHeight,
                  target, targetX, targetY);
  }


  ////////////////////////////////////////////////////////////

  // Initialization methods.


  protected void initGStreamer(PApplet parent) {
    this.parent = parent;
    pipeline = null;

    Video.init();


    Element srcElement = null;
    if (device == null) {

      // use the default device from GStreamer
      srcElement = ElementFactory.make("autovideosrc", null);

    } else {
    	
      // look for device
      if (devices == null) {
        DeviceMonitor monitor = new DeviceMonitor();
        monitor.addFilter("Video/Source", null);
        devices = monitor.getDevices();
        monitor.close();
      }
      
      
      for (int i=0; i < devices.size(); i++) {
    	String deviceName = devices.get(i).getDisplayName() + " #" + Integer.toString(i + 1);
    	  
        if (devices.get(i).getDisplayName().equals(device) || devices.get(i).getName().equals(device) || deviceName.equals(device)) {
          // found device
          srcElement = devices.get(i).createElement(null);
          break;
        }
      }

      // error out if we got passed an invalid device name
      if (srcElement == null) {
        throw new RuntimeException("Could not find device " + device);
      }

    }

    pipeline = new Pipeline();
    useBufferSink = Video.useGLBufferSink && parent.g.isGL();

    Element videoscale = ElementFactory.make("videoscale", null);
    Element videoconvert = ElementFactory.make("videoconvert", null);
    Element capsfilter = ElementFactory.make("capsfilter", null);

    String frameRateString;
    if (frameRate != 0.0) {
      frameRateString = ", framerate=" + fpsToFramerate(frameRate);
    } else {
      frameRateString = "";
    }
    capsfilter.set("caps", Caps.fromString("video/x-raw, width=" + width + ", height=" + height + frameRateString));

    rgbSink = new AppSink("sink");
    rgbSink.set("emit-signals", true);
    newSampleListener = new NewSampleListener();
    newPrerollListener = new NewPrerollListener();        
    rgbSink.connect(newSampleListener);
    rgbSink.connect(newPrerollListener);

    if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
      if (useBufferSink) rgbSink.setCaps(Caps.fromString("video/x-raw, format=RGBx"));
      else rgbSink.setCaps(Caps.fromString("video/x-raw, format=BGRx"));
    } else {
      rgbSink.setCaps(Caps.fromString("video/x-raw, format=xRGB"));
    }

    pipeline.addMany(srcElement, videoscale, videoconvert, capsfilter, rgbSink);
    Pipeline.linkMany(srcElement, videoscale, videoconvert, capsfilter, rgbSink);

    makeBusConnections(pipeline.getBus());


    try {
      // register methods
      parent.registerMethod("dispose", this);
      parent.registerMethod("post", this);

      setEventHandlerObject(parent);

      rate = 1.0f;
      volume = -1;
      sinkReady = false;
      bufWidth = bufHeight = 0;
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  public static String fpsToFramerate(float fps) {
    String formatted = Float.toString(fps);
    // this presumes the delimitter is always a dot
    int i = formatted.indexOf('.');
    if (Math.floor(fps) != fps) {
      int denom = (int)Math.pow(10, formatted.length()-i-1);
      int num = (int)(fps * denom);
      return num + "/" + denom;
    } else {
      return (int)fps + "/1";
    }
  }


  /**
   * Uses a generic object as handler of the movie. This object should have a
   * movieEvent method that receives a GSMovie argument. This method will
   * be called upon a new frame read event.
   *
   */
  protected void setEventHandlerObject(Object obj) {
    eventHandler = obj;

    try {
      movieEventMethod = eventHandler.getClass().getMethod("captureEvent", Capture.class);
      return;
    } catch (Exception e) {
      // no such method, or an error... which is fine, just ignore
    }

    // movieEvent can alternatively be defined as receiving an Object, to allow
    // Processing mode implementors to support the video library without linking
    // to it at build-time.
    try {
      movieEventMethod = eventHandler.getClass().getMethod("captureEvent", Object.class);
    } catch (Exception e) {
      // no such method, or an error... which is fine, just ignore
    }
  }


  protected void initSink() {
    pipeline.setState(org.freedesktop.gstreamer.State.READY);
    sinkReady = true;
    newFrame = false;
  }


  private void makeBusConnections(Bus bus) {
    bus.connect(new Bus.ERROR() {

        public void errorMessage(GstObject arg0, int arg1, String arg2) {
            System.err.println(arg0 + " : " + arg2);
        }
    });
    bus.connect(new Bus.EOS() {

        public void endOfStream(GstObject arg0) {
            try {
                if (repeat) {
                  pipeline.seek(0, TimeUnit.NANOSECONDS);
                } else {
                    stop();
                }
            } catch (Exception ex) {
              ex.printStackTrace();
            }
        }
    });
  }

  private void seek(boolean eos, long position) {
      
      double rate = this.rate;
      if (rate == 0.0) {
          rate = 0.0000001;
      }
      long duration = pipeline.queryDuration(TimeUnit.NANOSECONDS);
      if (eos) {
          if (rate > 0) {
              position = 0;
          } else {
              position = duration;
          }
      } else if (position < 0) {
          position = pipeline.queryPosition(TimeUnit.NANOSECONDS);
      }

      if (rate > 0) {
          pipeline.seek(rate, Format.TIME, EnumSet.of(SeekFlags.FLUSH, SeekFlags.ACCURATE), SeekType.SET, position, SeekType.SET, duration);
      } else {
          pipeline.seek(rate, Format.TIME, EnumSet.of(SeekFlags.FLUSH, SeekFlags.ACCURATE), SeekType.SET, 0, SeekType.SET, position);
      }
      
      pipeline.getState(10, TimeUnit.MILLISECONDS);
  }


  ////////////////////////////////////////////////////////////

  // Stream event handling.

  
  private void fireMovieEvent() {
    // Creates a movieEvent.
    if (movieEventMethod != null) {
      try {
        movieEventMethod.invoke(eventHandler, this);
      } catch (Exception e) {
        System.err.println("error, disabling captureEvent()");
        e.printStackTrace();
        movieEventMethod = null;
      }
    }
  }


  ////////////////////////////////////////////////////////////

  // Stream query methods.


  /**
   * Get the height of the source video. Note: calling this method repeatedly
   * can slow down playback performance.
   *
   * @return int
   */
//  protected int getSourceHeight() {
//    Dimension dim = pipe.getVideoSize();
//    if (dim != null) {
//      return dim.height;
//    } else {
//      return 0;
//    }
//  }


  /**
   * Get the original framerate of the source video. Note: calling this method
   * repeatedly can slow down playback performance.
   *
   * @return float
   */
//  protected float getSourceFrameRate() {
//    return (float)pipeline.getVideoSinkFrameRate();
//  }


  /**
   * Get the width of the source video. Note: calling this method repeatedly
   * can slow down playback performance.
   *
   * @return int
   */
//  protected int getSourceWidth() {
//    Dimension dim = pipe.getVideoSize();
//    if (dim != null) {
//      return dim.width;
//    } else {
//      return 0;
//    }
//  }


  ////////////////////////////////////////////////////////////

  // Buffer source interface.


  /**
   * Sets the object to use as destination for the frames read from the stream.
   * The color conversion mask is automatically set to the one required to
   * copy the frames to OpenGL.
   *
   * NOTE: This is not official API and may/will be removed at any time.
   *
   * @param Object dest
   */
  public void setBufferSink(Object sink) {
    bufferSink = sink;
  }


  /**
   * NOTE: This is not official API and may/will be removed at any time.
   */
  public boolean hasBufferSink() {
    return bufferSink != null;
  }


  /**
   * NOTE: This is not official API and may/will be removed at any time.
   */
  public synchronized void disposeBuffer(Object buf) {
    ((Buffer)buf).dispose();
  }


  protected void getSinkMethods() {
    try {
      sinkCopyMethod = bufferSink.getClass().getMethod("copyBufferFromSource",
        new Class[] { Object.class, ByteBuffer.class, int.class, int.class });
    } catch (Exception e) {
      throw new RuntimeException("Movie: provided sink object doesn't have a " +
                                 "copyBufferFromSource method.");
    }

    try {
      sinkSetMethod = bufferSink.getClass().getMethod("setBufferSource",
        new Class[] { Object.class });
      sinkSetMethod.invoke(bufferSink, new Object[] { this });
    } catch (Exception e) {
      throw new RuntimeException("Movie: provided sink object doesn't have a " +
                                 "setBufferSource method.");
    }

    try {
      sinkDisposeMethod = bufferSink.getClass().getMethod("disposeSourceBuffer",
        new Class[] { });
    } catch (Exception e) {
      throw new RuntimeException("Movie: provided sink object doesn't have " +
                                 "a disposeSourceBuffer method.");
    }

    try {
      sinkGetMethod = bufferSink.getClass().getMethod("getBufferPixels",
        new Class[] { int[].class });
    } catch (Exception e) {
      throw new RuntimeException("Movie: provided sink object doesn't have " +
                                 "a getBufferPixels method.");
    }
  }


  public synchronized void post() {
    if (useBufferSink && sinkDisposeMethod != null) {
      try {
        sinkDisposeMethod.invoke(bufferSink, new Object[] {});
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   *  Returns a list of all capture devices, using the device's pretty display name.
   *  Multiple devices can have identical display names, appending ' #n' to devices
   *  with duplicate display names.
   *  @return array of device names
   */
  static public String[] list() {
	    Video.init();

	    String[] out;
	    
	    DeviceMonitor monitor = new DeviceMonitor();
	    monitor.addFilter("Video/Source", null);
	    devices = monitor.getDevices();
	    monitor.close();
   
	    out = new String[devices.size()];
	    for (int i = 0; i < devices.size(); i++) {
	    	Device dev = devices.get(i);	    		     	
	    	out[i] = checkCameraDuplicates(dev) > 1 ? assignDisplayName(dev, i) : dev.getDisplayName();
	    }

	    return out;	  	  	  
  }
  
  static private String assignDisplayName(Device d, int pos) {
	  String s = "";
	  int count = 1;
	  
	  for(int i = 0; i < devices.size(); i++) {
		  if(devices.get(i).getDisplayName().equals(d.getDisplayName())){
			  if(i == pos) {
				  s = d.getDisplayName() + " #" + Integer.toString(count);
			  }		
			  count++;
		  }		  
	  }
 
	  return s;
  }
  
  static private int checkCameraDuplicates(Device d) {
	  int count = 0;
	  for (int i = 0; i < devices.size(); i++) {
		  if(devices.get(i).getDisplayName().equals(d.getDisplayName())) {
			  count++;
		  }
	  }    
	  return count;
  }
 

  private class NewSampleListener implements AppSink.NEW_SAMPLE {

    @Override
    public FlowReturn newSample(AppSink sink) {
      Sample sample = sink.pullSample();
      Structure capsStruct = sample.getCaps().getStructure(0);
      int w = capsStruct.getInteger("width");
      int h = capsStruct.getInteger("height");
      
      
      Buffer buffer = sample.getBuffer();
      ByteBuffer bb = buffer.map(false);
      if (bb != null) {
        
        // If the EDT is still copying data from the buffer, just drop this frame
        if (!bufferLock.tryLock()) {
          return FlowReturn.OK;
        }

        available = true;
        bufWidth = w;
        bufHeight = h;
                
        if (useBufferSink && bufferSink != null) { // The native buffer from gstreamer is copied to the buffer sink.
          
          try {
            sinkCopyMethod.invoke(bufferSink, new Object[] { buffer, bb, bufWidth, bufHeight });
            if (playing) {
              fireMovieEvent();
            }             
          } catch (Exception e) {
            e.printStackTrace();
          } finally {
            bufferLock.unlock();
          }        
          
        } else {
          IntBuffer rgb = bb.asIntBuffer();

          if (copyPixels == null) {
            copyPixels = new int[w * h];
          }

          try {
            rgb.get(copyPixels, 0, width * height);
            if (playing) {
              fireMovieEvent();
            }
          } finally {
            bufferLock.unlock();
          }
          
        }

        buffer.unmap();
      }
      sample.dispose();
      return FlowReturn.OK;
    }
  }
  

  private class NewPrerollListener implements AppSink.NEW_PREROLL {
    @Override
    public FlowReturn newPreroll(AppSink sink) {
      Sample sample = sink.pullPreroll();
      Structure capsStruct = sample.getCaps().getStructure(0);
      int w = capsStruct.getInteger("width");
      int h = capsStruct.getInteger("height");
      
      
      Buffer buffer = sample.getBuffer();
      ByteBuffer bb = buffer.map(false);
      if (bb != null) {
        
        // If the EDT is still copying data from the buffer, just drop this frame
        if (!bufferLock.tryLock()) {
          return FlowReturn.OK;
        }

        available = true;
        bufWidth = w;
        bufHeight = h;
                
        if (useBufferSink && bufferSink != null) { // The native buffer from gstreamer is copied to the buffer sink.
          
          try {
            sinkCopyMethod.invoke(bufferSink, new Object[] { buffer, bb, bufWidth, bufHeight });
            if (playing) {
              fireMovieEvent();
            }             
          } catch (Exception e) {
            e.printStackTrace();
          } finally {
            bufferLock.unlock();
          }        
          
        } else {
          IntBuffer rgb = bb.asIntBuffer();

          if (copyPixels == null) {
            copyPixels = new int[w * h];
          }

          try {
            rgb.get(copyPixels, 0, width * height);
            if (playing) {
              fireMovieEvent();
            }
          } finally {
            bufferLock.unlock();
          }
          
        }

        buffer.unmap();
      }
      sample.dispose();
      return FlowReturn.OK;
    }
  }
}
