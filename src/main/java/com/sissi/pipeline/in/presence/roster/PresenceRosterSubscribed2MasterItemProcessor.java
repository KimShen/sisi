package com.sissi.pipeline.in.presence.roster;

import com.sissi.context.JID;
import com.sissi.context.JIDContext;
import com.sissi.pipeline.in.ProxyProcessor;
import com.sissi.protocol.Protocol;
import com.sissi.protocol.Protocol.Type;
import com.sissi.protocol.iq.IQ;
import com.sissi.protocol.iq.roster.Item;
import com.sissi.protocol.iq.roster.Roster;
import com.sissi.ucenter.RelationContext.RelationRoster;

/**
 * @author kim 2013-11-18
 */
public class PresenceRosterSubscribed2MasterItemProcessor extends ProxyProcessor {

	@Override
	public Boolean input(JIDContext context, Protocol protocol) {
		JID master = super.build(protocol.getTo());
		super.offer(master, this.prepare(context, master));
		return true;
	}

	private Protocol prepare(JIDContext context, JID jid) {
		RelationRoster relation = RelationRoster.class.cast(super.ourRelation(jid, context.getJid()));
		return new IQ().add(new Roster(new Item(context.getJid().asStringWithBare(), relation.getName(), Roster.Subscription.TO.toString(), relation.asGroup()))).setTo(jid).setType(Type.SET);
	}
}
