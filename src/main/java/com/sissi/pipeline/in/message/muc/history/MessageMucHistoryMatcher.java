package com.sissi.pipeline.in.message.muc.history;

import com.sissi.pipeline.in.ClassMatcher;
import com.sissi.protocol.Protocol;
import com.sissi.protocol.message.Message;

/**
 * 匹配Message.history
 * 
 * @author kim 2014年4月2日
 */
public class MessageMucHistoryMatcher extends ClassMatcher {

	public MessageMucHistoryMatcher() {
		super(Message.class);
	}

	public boolean match(Protocol protocol) {
		return super.match(protocol) && protocol.cast(Message.class).history();
	}
}
