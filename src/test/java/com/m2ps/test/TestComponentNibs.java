
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
public class TestComponentNibs
{
	public Logger loggerSource = new Logger();
	public Logger loggerSink = new Logger();
	public ISOChannel channelSource;
	public ISOChannel channelSink;
	
	/**
	 * Connect channel and logger
	 *
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception
	{
		try
		{
			/* Create core server connection */
			loggerSource.addListener(new SimpleLogListener(System.out));
			channelSource = new XMLChannel(new XMLPackager(), new ServerSocket(10004));
			((LogSource)channelSource).setLogger(loggerSource, "SourceSimulator-channel");
			channelSource.connect();
			
			/* Create Sink Node server */
			loggerSink.addListener(new SimpleLogListener(System.out));
			channelSink = new XMLChannel(new XMLPackager(), new ServerSocket(10006));
			((LogSource)channelSink).setLogger(loggerSink, "SinkSimulator-channel");
			/* Note don't connect here. Connect waits for incoming client connection. Using one-shot channel adapter
			 * so we connect when we receive connection from nibs sink. 
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
	}
	
	/**
	 * Disconnects channel
	 *
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception
	{
		/* Disconnect */
		try
		{
			channelSource.disconnect();
			channelSink.disconnect();
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}
	}
	
	/**
	 * Test an approved 0200 request and a 0210 response. 
	 *
	 */
	@Test
	public void requestResponseApproved()
	{
		try
		{
			/* Send message to Source node */
			ISOMsg m = getRequestMsg();
			channelSource.send(m);
			
			/* Receive at Sink node */
			//channelSink.connect();
			//ISOMsg r = channelSink.receive();
			
			/* Send response from sink node */
			// Get network managment req
//			r.setMTI("0810");
//			r.set(39, "00");
//			channelSink.send(r);
//
//			// Get tran
//			channelSink.connect();
//			r = channelSink.receive();
//			r.setMTI("0210");
//			r.set(39, "00");
//			channelSink.send(r);
			
			/* Receive response at source node */
			ISOMsg r = channelSource.receive();
			
			Assert.assertEquals(r.getString("39"), "91");
		}
		catch (Exception e)
		{
			fail("Unexpected exception: " + e.toString());
		}
	}
	
	public ISOMsg getRequestMsg()
	{
		ISOMsg m = new ISOMsg();
		try
		{
			m.setMTI("0200");
			m.set(3, "000000");
			m.set(4, "000000111224");
			m.set(11, "000000987123");
			m.set(41, "00000001");
		}
		catch (ISOException ex)
		{
			ex.printStackTrace();
		}
		return m;
	}
}

