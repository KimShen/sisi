package com.sissi.pipeline.in.iq.vcard;

import com.sissi.context.JIDContext;
import com.sissi.pipeline.in.ProxyProcessor;
import com.sissi.protocol.Protocol;
import com.sissi.protocol.Protocol.Type;
import com.sissi.protocol.iq.vcard.VCard;
import com.sissi.ucenter.VCardContext;

/**
 * @author kim 2013年12月10日
 */
abstract class VCardGetProcessor extends ProxyProcessor {

	protected VCardContext vcardContext;

	public VCardGetProcessor(VCardContext vcardContext) {
		super();
		this.vcardContext = vcardContext;
	}

	@Override
	public Boolean input(JIDContext context, Protocol protocol) {
		context.write(this.fill(context, protocol).getParent().reply().setTo(context.getJid().getBare()).setType(Type.RESULT));
		return true;
	}

	abstract protected VCard fill(JIDContext context, Protocol protocol);
}
