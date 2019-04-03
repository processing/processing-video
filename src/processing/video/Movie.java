/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

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

import java.awt.Dimension;
import java.io.*;
import java.net.URI;
import java.nio.*;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.lang.reflect.*;

import org.freedesktop.gstreamer.*;
import org.freedesktop.gstreamer.Buffer;
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
public class Movie extends PImage implements PConstants {
  public static String[] supportedProtocols = { "http" };

  protected int nativeWidth;
  protected int nativeHeight;
  protected float nativeFrameRate;    // the file's native fps
  public float frameRate;             // the current playback fps
  protected float rate;               // speed multiplier (1.0: frameRate = nativeFrameRate)

  public String filename;
  public PlayBin playbin;

  protected boolean playing = false;
  protected boolean paused = false;
  protected boolean repeat = false;

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
  protected String copyMask;
  protected Buffer natBuffer = null;
//  protected BufferDataAppSink natSink = null;

  NewSampleListener newSampleListener;
//  NewPrerollListener newPrerollListener;
  private final Lock bufferLock = new ReentrantLock();
  

  /**
   * Creates an instance of GSMovie loading the movie from filename.
   *
   * @param parent PApplet
   * @param filename String
   */
  public Movie(PApplet parent, String filename) {
    super(0, 0, RGB);
    initGStreamer(parent, filename);
  }


  /**
   * Disposes all the native resources associated to this movie.
   * 
   * NOTE: This is not official API and may/will be removed at any time.
   */
  public void dispose() {
    if (playbin != null) {
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
//      sink.disconnect(newPrerollListener);
      rgbSink.dispose();
      playbin.setState(org.freedesktop.gstreamer.State.NULL);
      playbin.getState();
      playbin.getBus().dispose();
      playbin.dispose();
      
      
//      copyPixels = null;
//      if (rgbSink != null) {
//        rgbSink.removeListener();
//        rgbSink.dispose();
//        rgbSink = null;
//      }
      
//      natBuffer = null;
//      if (natSink != null) {
//        natSink.removeListener();
//        natSink.dispose();
//        natSink = null;
//      }

//      playbin.dispose();
//      playbin = null;
      
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
      playbin.pause();
      playbin.getState();
    }

    long t = playbin.queryPosition(TimeUnit.NANOSECONDS);

    boolean res;
    long start, stop;
    if (rate > 0) {
      start = t;
      stop = -1;
    } else {
      start = 0;
      stop = t;
    }

    res = playbin.seek(rate * f, Format.TIME, EnumSet.of(SeekFlags.FLUSH, SeekFlags.ACCURATE), SeekType.SET, start, SeekType.SET, stop);
    
    
    
    playbin.getState();

    if (!res) {
      PGraphics.showWarning("Seek operation failed.");
    }

    if (playing) {
      playbin.play();
    }

    frameRate = ifps;
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
    float sec = playbin.queryDuration(TimeUnit.SECONDS);
    float nanosec = playbin.queryDuration(TimeUnit.NANOSECONDS);
    return sec + Video.nanoSecToSecFrac(nanosec);
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
    float sec = playbin.queryDuration(TimeUnit.SECONDS);
    float nanosec = playbin.queryDuration(TimeUnit.NANOSECONDS);
    return sec + Video.nanoSecToSecFrac(nanosec);
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
    if (nativeFrameRate != -1) {
      int frame = (int)(where * nativeFrameRate);
      where = frame / nativeFrameRate;
    }

    boolean res;
    long pos = Video.secToNanoLong(where);

    res = playbin.seek(rate, Format.TIME, EnumSet.of(SeekFlags.FLUSH, SeekFlags.ACCURATE), SeekType.SET, pos, SeekType.NONE, -1);

    if (!res) {
      PGraphics.showWarning("Seek operation failed.");
    }

    // getState() will wait until any async state change
    // (like seek in this case) has completed
    seeking = true;
    playbin.getState();
    seeking = false;    
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
  public void play() {
    if (seeking) return;

    if (!sinkReady) {
      initSink();
    }

    playing = true;
    paused = false;
    playbin.play();
    playbin.getState();
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
  public void loop() {
    if (seeking) return;

    repeat = true;
    play();
  }


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
    playbin.pause();
    playbin.getState();
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
    playbin.stop();
    playbin.getState();
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

    int[] temp = pixels;
    pixels = copyPixels;
    updatePixels();
    copyPixels = temp;    
    
/*
    if (useBufferSink) { // The native buffer from gstreamer is copied to the buffer sink.
      outdatedPixels = true;
      if (natBuffer == null) {
        return;
      }

      if (firstFrame) {
        super.init(bufWidth, bufHeight, ARGB, 1);
        firstFrame = false;
      }

      if (bufferSink == null) {
        Object cache = parent.g.getCache(this);
        if (cache == null) {
          return;
        }
        setBufferSink(cache);
        getSinkMethods();
      }

      ByteBuffer byteBuffer = natBuffer.getByteBuffer();

      try {
        sinkCopyMethod.invoke(bufferSink, new Object[] { natBuffer, byteBuffer, bufWidth, bufHeight });
      } catch (Exception e) {
        e.printStackTrace();
      }

      natBuffer = null;
    } else { // The pixels just read from gstreamer are copied to the pixels array.
      if (copyPixels == null) {
        return;
      }

      if (firstFrame) {
        super.init(bufWidth, bufHeight, RGB, 1);
        firstFrame = false;
      }

      int[] temp = pixels;
      pixels = copyPixels;
      updatePixels();
      copyPixels = temp;
    }
    */

    available = false;
    newFrame = true;
  }


  /**
   * Change the volume. Values are from 0 to 1.
   *
   * @param float v
   */
  public void volume(float v) {
    if (playing && PApplet.abs(volume - v) > 0.001f) {
      playbin.setVolume(v);
      volume = v;
    }
  }


  public synchronized void loadPixels() {
    super.loadPixels();
    
    if (useBufferSink) {
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


  protected void initGStreamer(PApplet parent, String filename) {
    this.parent = parent;
    playbin = null;

    File file;

    Video.init();

    // first check to see if this can be read locally from a file.
    try {
      try {
        // first try a local file using the dataPath. usually this will
        // work ok, but sometimes the dataPath is inside a jar file,
        // which is less fun, so this will crap out.
        file = new File(parent.dataPath(filename));
        if (file.exists()) {
          playbin = new PlayBin("Movie Player");
          playbin.setInputFile(file);
        }
      } catch (Exception e) {
        // XXX: we want to see exceptions here such as
        // "java.lang.IllegalArgumentException: No such Gstreamer factory: playbin"
        // just hide the file-not-found ones
      }

      // read from a file just hanging out in the local folder.
      // this might happen when the video library is used with some
      // other application, or the person enters a full path name
      if (playbin == null) {
        try {
          file = new File(filename);
          if (file.exists()) {
            playbin = new PlayBin("Movie Player");
            playbin.setInputFile(file);
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      if (playbin == null) {
        // Try network read...
        for (int i = 0; i < supportedProtocols.length; i++) {
          if (filename.startsWith(supportedProtocols[i] + "://")) {
            try {
              playbin = new PlayBin("Movie Player");
              playbin.setURI(URI.create(filename));
              break;
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }
      }
    } catch (SecurityException se) {
      // online, whups. catch the security exception out here rather than
      // doing it three times (or whatever) for each of the cases above.
    }

    if (playbin == null) {
      parent.die("Could not load movie file " + filename, null);
    }

    // we've got a valid movie! let's rock.
    try {
      this.filename = filename; // for error messages

      // register methods
      parent.registerMethod("dispose", this);
      parent.registerMethod("post", this);

      setEventHandlerObject(parent);

      nativeFrameRate = -1;
      frameRate = -1;
      rate = 1.0f;
      volume = -1;
      sinkReady = false;
      bufWidth = bufHeight = 0;
    } catch (Exception e) {
      e.printStackTrace();
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
      movieEventMethod = eventHandler.getClass().getMethod("movieEvent", Movie.class);
      return;
    } catch (Exception e) {
      // no such method, or an error... which is fine, just ignore
    }

    // movieEvent can alternatively be defined as receiving an Object, to allow
    // Processing mode implementors to support the video library without linking
    // to it at build-time.
    try {
      movieEventMethod = eventHandler.getClass().getMethod("movieEvent", Object.class);
    } catch (Exception e) {
      // no such method, or an error... which is fine, just ignore
    }
  }


  protected void initSink() {
    rgbSink = new AppSink("sink");
    rgbSink.set("emit-signals", true);
    newSampleListener = new NewSampleListener();
//    newPrerollListener = new NewPrerollListener();
    rgbSink.connect(newSampleListener);
//    rgbSink.connect(newPrerollListener);
    if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
      rgbSink.setCaps(Caps.fromString("video/x-raw, format=BGRx"));
    } else {
      rgbSink.setCaps(Caps.fromString("video/x-raw, format=xRGB"));
    }
    playbin.setVideoSink(rgbSink);
    
    makeBusConnections(playbin.getBus());
    playbin.setState(org.freedesktop.gstreamer.State.READY);

    
    


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
          if (repeat) {
            if (0 < rate) {
              // Playing forward, so we return to the beginning
              jump(0);
            } else {
              // Playing backwards, so we go to the end.
              jump(duration());
            }

            // The rate is set automatically to 1 when restarting the
            // stream, so we need to call frameRate in order to reset
            // to the latest fps rate.
            frameRate(frameRate);
          } else {
            playing = false;
          }
        }
    });
  }

  

  ////////////////////////////////////////////////////////////

  // Stream event handling.


  protected synchronized void invokeEvent(int w, int h, IntBuffer buffer) {
    available = true;
    bufWidth = w;
    bufHeight = h;

    if (copyPixels == null) {
      copyPixels = new int[w * h];
    }
    buffer.rewind();
    try {
      buffer.get(copyPixels);
    } catch (BufferUnderflowException e) {
      e.printStackTrace();
      copyPixels = null;
      return;
    }

    if (playing) {
      fireMovieEvent();
    }
  }

  protected synchronized void invokeEvent(int w, int h, Buffer buffer) {
    available = true;
    bufWidth = w;
    bufHeight = h;
    if (natBuffer != null) {
      // To handle the situation where read() is not called in the sketch, so 
      // that the native buffers are not being sent to the sinke, and therefore, not disposed
      // by it.
      natBuffer.dispose(); 
    }
    natBuffer = buffer;

    if (playing) {
      fireMovieEvent();
    }
  }

  private void fireMovieEvent() {
    // Creates a movieEvent.
    if (movieEventMethod != null) {
      try {
        movieEventMethod.invoke(eventHandler, this);
      } catch (Exception e) {
        System.err.println("error, disabling movieEvent() for " + filename);
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
  protected int getSourceHeight() {
//    Dimension dim = playbin.getVideoSize();
//    if (dim != null) {
//      return dim.height;
//    } else {
//      return 0;
//    }
    return nativeWidth;
  }


  /**
   * Get the original framerate of the source video. Note: calling this method
   * repeatedly can slow down playback performance.
   *
   * @return float
   */
  protected float getSourceFrameRate() {
    // This doesn't work any longer with GStreamer 1.x
    //return (float)playbin.getVideoSinkFrameRate();
    // so use the field extracted from the caps instead:
    return nativeFrameRate;
  }


  /**
   * Get the width of the source video. Note: calling this method repeatedly
   * can slow down playback performance.
   *
   * @return int
   */
  protected int getSourceWidth() {
//    Dimension dim = playbin.getVideoSize();
//    if (dim != null) {
//      return dim.width;
//    } else {
//      return 0;
//    }
    return nativeHeight;
  }


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
    initCopyMask();
  }


  /**
   * Sets the object to use as destination for the frames read from the stream.
   *
   * NOTE: This is not official API and may/will be removed at any time.
   *
   * @param Object dest
   * @param String mask
   */
  public void setBufferSink(Object sink, String mask) {
    bufferSink = sink;
    copyMask = mask;
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


  protected void initCopyMask() {
    if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
      copyMask = "red_mask=(int)0xFF000000, green_mask=(int)0xFF0000, blue_mask=(int)0xFF00";
    } else {
      copyMask = "red_mask=(int)0xFF, green_mask=(int)0xFF00, blue_mask=(int)0xFF0000";
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
  
  private class NewSampleListener implements AppSink.NEW_SAMPLE {

    @Override
    public FlowReturn newSample(AppSink sink) {
      Sample sample = sink.pullSample();

      // pull out metadata from caps
      Structure capsStruct = sample.getCaps().getStructure(0);
      nativeWidth = capsStruct.getInteger("width");
      nativeHeight = capsStruct.getInteger("height");
      Fraction fps = capsStruct.getFraction("framerate");
      nativeFrameRate = (float)fps.numerator / fps.denominator;

      // set the playback rate to the file's native framerate
      // unless the user has already set a custom one
      if (frameRate == -1.0) {
        frameRate = nativeFrameRate;
      }

      Buffer buffer = sample.getBuffer();
      ByteBuffer bb = buffer.map(false);
      if (bb != null) {
        // If the EDT is still copying data from the buffer, just drop this frame
        if (!bufferLock.tryLock()) {
          return FlowReturn.OK;
        }
        IntBuffer rgb = bb.asIntBuffer();
        
        available = true;
        bufWidth = nativeWidth;
        bufHeight = nativeHeight;
        if (copyPixels == null) {
          copyPixels = new int[nativeWidth * nativeHeight];
        }
        
        try {
          rgb.get(copyPixels, 0, width * height);
          if (playing) {
            fireMovieEvent();
          }          
          
        } finally {
          bufferLock.unlock();
        }        
        
        buffer.unmap();
      }
      sample.dispose();
      return FlowReturn.OK;
    }
  }

  /*
  private class NewPrerollListener implements AppSink.NEW_PREROLL {
    @Override
    public void newPreroll(AppSink sink) {
        surfaceLock.lock();
        Sample sample = sink.pullPreroll();
        Structure capsStruct = sample.getCaps().getStructure(0);
        int width = capsStruct.getInteger("width");
        int height = capsStruct.getInteger("height");
        try {
            if (surface == null || surface.getWidth() != width || surface.getHeight() != height) {
                if (surface != null && surface.sample != null) {
                    surface.sample.dispose();
                }
                surface = new GStreamerSurface(width, height);
            } else {
                if (surface.sample != null) {
                    surface.sample.dispose();
                }
            }
            surface.sample = sample;
            surface.modCount++;
        } finally {
            surfaceLock.unlock();
        }
    }
  }
  */
}
