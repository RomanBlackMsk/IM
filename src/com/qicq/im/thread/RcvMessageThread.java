package com.qicq.im.thread;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import android.util.Log;

import com.qicq.im.api.APIManager;
import com.qicq.im.api.ChatMessage;
import com.qicq.im.msg.MsgRcvListener;

public class RcvMessageThread extends AbstractMessageThread{
	private Vector<MsgRcvListener> listeners = new Vector<MsgRcvListener>();
	//private boolean continueRcv = true;
	//private APIManager api = null;
	public final static long MAX_TIME = 128000;
	public final static long MIN_TIME = 2000;
	//private long sleeptime = 1000;
	//private boolean networkConnect = true;
	//private boolean needPause = false;

	public RcvMessageThread(APIManager api){
		super(api);
		this.api = api;
	}

	public synchronized void addMsgRcvListener(MsgRcvListener e)
	{
		listeners.addElement(e);
	}

	public synchronized void removeMsgRcvListener(MsgRcvListener e)
	{
		listeners.removeElement(e);
	}

	@SuppressWarnings("unchecked")
	protected void notifyAllEvent(List<ChatMessage> msgs)
	{
		List<ChatMessage> ms = new ArrayList<ChatMessage>();
		List<ChatMessage> hello_ms = new ArrayList<ChatMessage>();
		List<ChatMessage> req_ms = new ArrayList<ChatMessage>();
		for(ChatMessage m : msgs){
			if(m.type == ChatMessage.MESSAGE_TYPE_HELLO){
				hello_ms.add(m);
			}
			else if(m.type == ChatMessage.MESSAGE_TYPE_REQUEST){
				req_ms.add(m);
			}
			else{
				ms.add(m);
			}
		}

		if(ms.size() == 0)
			ms = null;
		if(hello_ms.size() == 0)
			hello_ms = null;
		if(req_ms.size() == 0)
			req_ms = null;

		Vector<MsgRcvListener> tempVector = null;
		synchronized(this)
		{
			tempVector=(Vector<MsgRcvListener>)listeners.clone();

			for(int i=0;i<tempVector.size();i++)
			{
				MsgRcvListener l = (MsgRcvListener)tempVector.elementAt(i);
				if(ms != null)
					l.onMsgRcved(ms);
				if(hello_ms != null)
					l.onHelloMsgRcved(hello_ms);
				if(req_ms != null)
					l.onRequestMsgRcved(req_ms);
			}
		}

	}

	@Override
	public void run() {
		Log.i("RcvMessageThread","Start");
		while(continuing){
			while(!networkConnect || needPause){
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					Log.e("RcvMessageThread",e.getMessage());
				}
			}

			List<ChatMessage> msgs = api.RcvMessage();

			if(msgs.size() != 0){
				notifyAllEvent(msgs);
				sleeptime /= 2;
				if(sleeptime < MIN_TIME)
					sleeptime = MIN_TIME;
			}else{
				sleeptime *= 2;
				if(sleeptime > MAX_TIME)
					sleeptime = MAX_TIME;
			}

			try {
				Thread.sleep(sleeptime);
			} catch (InterruptedException e) {
				Log.e("RcvMessageThread",e.getMessage());
			}
		}
	}
}
