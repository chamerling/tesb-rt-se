/*
 * #%L
 * Service Activity Monitoring :: Agent
 * %%
 * Copyright (C) 2011 Talend Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.talend.esb.sam.agent.wiretap;

import java.io.OutputStream;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.io.CacheAndWriteOutputStream;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.io.CachedOutputStreamCallback;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

/**
 * Create a CachedOutputStream on the message that can be used to Wiretap the
 * content. Additionally it registers an optional interceptor that is called as
 * soon as the output stream is closed
 */
public class WireTapOut extends AbstractPhaseInterceptor<Message> {
	private Interceptor<Message> wireTap;
	private boolean logMessageContent;

	public WireTapOut(Interceptor<Message> wireTap, boolean logMessageContent) {
		super(Phase.PRE_STREAM);
		this.wireTap = wireTap;
		this.logMessageContent = logMessageContent;
	}

	@Override
	public void handleMessage(final Message message) throws Fault {
		final OutputStream os = message.getContent(OutputStream.class);
		
		final CacheAndWriteOutputStream newOut = new CacheAndWriteOutputStream(
				os);
		message.setContent(OutputStream.class, newOut);
		
		if (os != null && logMessageContent) {
			message.setContent(CachedOutputStream.class, newOut);
		}
		
		if (wireTap != null) {
			newOut.registerCallback(new CallBack(message));
		}
	}

	private final class CallBack implements CachedOutputStreamCallback {
		private final Message message;

		private CallBack(Message message) {
			this.message = message;
		}

		@Override
		public void onFlush(CachedOutputStream os) {
		}

		@Override
		public void onClose(CachedOutputStream os) {
			wireTap.handleMessage(message);
		}
	}
}
