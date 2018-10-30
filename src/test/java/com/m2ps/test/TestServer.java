
package com.m2ps.test;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.ServerSocket;

import junit.framework.Assert;

import org.jpos.iso.ISOChannel;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.channel.XMLChannel;
import org.jpos.iso.packager.XMLPackager;
import org.jpos.util.LogSource;
import org.jpos.util.Logger;
import org.jpos.util.SimpleLogListener;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test general request flows.
 * 
 */
public class TestServer
{
	public static Logger loggerSink = new Logger();
	public static ISOChannel channelSink;
	
	public static void main(String args[])
	{
		try
		{
			/* Create Sink Node server */
			loggerSink.addListener(new SimpleLogListener(System.out));
			channelSink = new XMLChannel(new XMLPackager(), new ServerSocket(10006));
			((LogSource)channelSink).setLogger(loggerSink, "SinkSimulator-channel");
			/*
			 * Note don't connect here. Connect waits for incoming client connection. Using one-shot
			 * channel adapter so we connect when we receive connection from nibs sink.
			 */
		}
		catch (ISOException ex)
		{
			ex.printStackTrace();
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}
		while (true)
		{
			try
			{
				/* Receive at Sink node */
				System.out.println("Waiting for connection");
				channelSink.connect();
				ISOMsg r = channelSink.receive();
				
				/* Send response from sink node */
				// Get network managment req
				r.setMTI("0810");
				r.set(39, "00");
				channelSink.send(r);
				System.out.println("Sent 0810 back");
			}
			catch (Exception e)
			{
				fail("Unexpected exception: " + e.toString());
			}
		}
	}
}
