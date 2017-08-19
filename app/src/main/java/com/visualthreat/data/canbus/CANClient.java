package com.visualthreat.data.canbus;

/**
 *  An instance of a class implementing this interface has to be passed to the
 * constructor of {@link  CANBus)}. It will be used by {@link CANBus} to
 * forward received CAN messages and take action when the connection
 * is closed
 */
public interface CANClient {

  /**
   * Is called when the network has been disconnected. This call can e.g. be
   * used to show the connection status in a GUI or inform the user using
   * other means.
   *
   * @param id
   */
  public void disconnected(int id);

  /**
   * Is called to write connection information to the log. The information can
   * either be ignored, directed to stdout or written out to a specialized
   * field or file in the program.
   *
   * @param id
   *            The <b>int</b> passed to
   *            {@link CANBus(int, CANClient, int)} in the
   *            constructor. It can be used to identify which instance (which
   *            connection) a message comes from, when several instances of
   *            {@link CANBus} are connected to the same instance of a
   *            class implementing this interface.
   * @param text
   *            The text to be written into the log in human readable form.
   *            Corresponds to information about the connection or ports.
   */
  public void writeLog(int id, String text);
  public void writeLog(int id, String text, Throwable t);
}
