
package com.m2ps.interchange;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import org.jpos.core.Configuration;
import org.jpos.core.ConfigurationException;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOSource;

import com.m2ps.message.M2PSIsoMsg;
import com.m2ps.message.M2PSIsoMsg.MsgType;

/**
 * 
 * Performs translation to the NIBS ISO8583 specification.
 * 
 */
public class IsoMessageProcessorNibs extends IsoMessageProcessor
{
	private boolean initSignOn = false;
	private boolean signedOn = false;
	private Timer callHomeNetworkManagmentTimer = null;
	
	/*-----------------------------------------------------------------------------------------------------------------*/
	/**
	 * Constructs a new <code>IsoSrcRequestListener</code> instance.
	 */
	public IsoMessageProcessorNibs()
	{
	}
	
	/**
	 * Retrieves and loads the configuration as defined in the QBean.
	 * 
	 * @param cfg
	 * @throws ConfigurationException
	 * @see org.jpos.core.Configurable#setConfiguration(org.jpos.core.Configuration)
	 */
	@Override
	public void setConfiguration(Configuration cfg) throws ConfigurationException
	{
		super.setConfiguration(cfg);
		
		/* Param1 - Init signon */
		String paramInitSignon = cfg.get(Params.Key.INIT_SIGNON);
		if (Params.Value.INIT_SIGNON_TRUE.equals(paramInitSignon))
		{
			initSignOn = true;
			info("'init_signon' is set to true");
			/* Initiate the network timer */
			callHomeNetworkManagmentTimer = new Timer();
			callHomeNetworkManagmentTimer.schedule(new TimerTask()
				{
					@Override
					public void run()
					{
						try
						{
							processNwrkManagementRequest();
						}
						catch (ISOException e)
						{
							warn(e);
						}
					}
				}, 0, 10000);
		}
		else
		{
			/* If we are not using 0800's for signon, then assume signed on.*/
			signedOn = true;
		}
	}
	
	/**
	 * This function performs translation of a request message from one zone to another.
	 * 
	 * @param request
	 *           This is the input or source message. i.e. The translation function will use this as
	 *           the reference when translating.
	 * @param translatedRequest
	 *           This is the output of the translation operation. The translatedRequest will be the
	 *           final output of the request translation.
	 * @throws ISOException
	 * @see com.m2ps.interchange.IsoMessageProcessor#performRequestTranslation(org.jpos.iso.ISOMsg,
	 *      org.jpos.iso.ISOMsg)
	 */
	@Override
	public void performRequestTranslation(ISOMsg request, ISOMsg translatedRequest)
		throws ISOException,
			IOException
	{
		info("[IsoMessageProcessor] Performing request translation");
		
		/* Not signed on. So want to send message to source declining with response code 91 */
		if (!signedOn)
		{
			ISOSource isoSource = request.getSource();
			ISOMsg response = request;
			response.setMTI(M2PSIsoMsg.MsgType.getResponseMsgType(request.getMTI()));
			response.set(M2PSIsoMsg.Bit._039_RESPONSE_CODE, M2PSIsoMsg.RspCode._91_ISSUER_SWITCH_INOPERATIVE);
			isoSource.send(response);
			return;
		}
		
		/* Signed on so process request message. */
		String msgType = request.getMTI();
		
		if (MsgType._0200_TRAN_REQ.equals(msgType))
		{
			processTranRequest(request, translatedRequest);
		}
	}
	
	/**
	 * 
	 * This function performs translation of a response message from one zone to another.
	 * 
	 * @param response
	 *           This is the input or source message. i.e. The translation function will use this as
	 *           the reference when translating.
	 * @param translatedResponse
	 *           This is the output of the translation operation. The translatedResponse will be the
	 *           final output of the response translation.
	 * @throws ISOException
	 * @see com.m2ps.interchange.IsoMessageProcessor#performResponseTranslation(org.jpos.iso.ISOMsg,
	 *      org.jpos.iso.ISOMsg)
	 */
	@Override
	public void performResponseTranslation(ISOMsg response, ISOMsg translatedResponse)
		throws ISOException
	{
		info("[IsoMessageProcessor] Performing response translation");
		String msgType = response.getMTI();
		
		if (MsgType._0210_TRAN_REQ_RSP.equals(msgType))
		{
			processTranRequestResponse(response, translatedResponse);
		}
		if (MsgType._0810_NETWRK_MANAGEMENT_REQ_RSP.equals(msgType))
		{
			processNwrkManagementRequestResponse(response, translatedResponse);
		}
	}
	
	/**
	 * 
	 * Mappings for the 0200 Transaction Request
	 * 
	 * @param request
	 * @param translatedRequest
	 */
	public void processTranRequest(ISOMsg request, ISOMsg translatedRequest) throws ISOException
	{
		translatedRequest = new ISOMsg();
		translatedRequest.setMTI(MsgType._0200_TRAN_REQ);
		
		/* Straight copy fields */
		M2PSIsoMsg.copyField(translatedRequest, request, M2PSIsoMsg.Bit._002_PAN);
		M2PSIsoMsg.copyField(translatedRequest, request, M2PSIsoMsg.Bit._003_PROCESING_CODE);
		M2PSIsoMsg.copyField(translatedRequest, request, M2PSIsoMsg.Bit._004_AMOUNT_TRAN);
		M2PSIsoMsg.copyField(translatedRequest, request, M2PSIsoMsg.Bit._011_SYSTEM_TRACE_AUDIT_NR);
		M2PSIsoMsg.copyField(
			translatedRequest,
			request,
			M2PSIsoMsg.Bit._041_CARD_ACCEPTOR_TERMINAL_ID);
		
		/* Constructed fields */
	}
	
	/**
	 * 
	 * Mappings for the 0210 Transaction Request Response
	 * 
	 * @param response
	 * @param translatedResponse
	 */
	public void processTranRequestResponse(ISOMsg response, ISOMsg translatedResponse)
		throws ISOException
	{
		translatedResponse = new ISOMsg();
		translatedResponse.setMTI(MsgType._0210_TRAN_REQ_RSP);
		
		/* Straight copy fields */
		M2PSIsoMsg.copyField(translatedResponse, response, M2PSIsoMsg.Bit._002_PAN);
		M2PSIsoMsg.copyField(translatedResponse, response, M2PSIsoMsg.Bit._003_PROCESING_CODE);
		M2PSIsoMsg.copyField(translatedResponse, response, M2PSIsoMsg.Bit._004_AMOUNT_TRAN);
		M2PSIsoMsg.copyField(translatedResponse, response, M2PSIsoMsg.Bit._011_SYSTEM_TRACE_AUDIT_NR);
		M2PSIsoMsg.copyField(translatedResponse, response, M2PSIsoMsg.Bit._039_RESPONSE_CODE);
		M2PSIsoMsg.copyField(
			translatedResponse,
			response,
			M2PSIsoMsg.Bit._041_CARD_ACCEPTOR_TERMINAL_ID);
		
		/* Constructed fields */
	}
	
	/**
	 * 
	 * Sends a new Network management request
	 * 
	 * @throws ISOException
	 */
	public void processNwrkManagementRequest() throws ISOException
	{
		if (initSignOn)
		{
			/* Send 0800 sign-on */
			ISOMsg msg = new ISOMsg();
			msg.setMTI(MsgType._0800_NETWRK_MANAGEMENT_REQ);
			
			msg.set(3, "000000");
			msg.set(41, "00000001");
			destQmux.request(msg, 10000, this, msg);
		}
	}
	
	/**
	 * 
	 * TODO
	 * 
	 * @param response
	 * @param translatedResponse
	 * @throws ISOException
	 */
	public void processNwrkManagementRequestResponse(ISOMsg response, ISOMsg translatedResponse)
		throws ISOException
	{
		/* Don't want to forward this message. Swallow it here. */
		translatedResponse = null;
		info("Signon was successful.");
	}
	
	/* Used to access configuration parameters */
	public static class Params
	{
		public static class Key
		{
			public static String INIT_SIGNON = "init_signon";
		}
		
		public static class Value
		{
			public static String INIT_SIGNON_TRUE = "true";
		}
	}
}
