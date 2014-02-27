package com.sissi.persistent.impl;

import java.util.Map;

import com.mongodb.BasicDBObjectBuilder;
import com.sissi.context.JIDBuilder;
import com.sissi.persistent.PersistentElementBox;
import com.sissi.protocol.Element;
import com.sissi.protocol.ProtocolType;
import com.sissi.protocol.iq.IQ;
import com.sissi.protocol.iq.bytestreams.Bytestreams;
import com.sissi.protocol.iq.data.XData;
import com.sissi.protocol.iq.data.XDataType;
import com.sissi.protocol.iq.data.XField;
import com.sissi.protocol.iq.data.XFieldType;
import com.sissi.protocol.iq.data.XOption;
import com.sissi.protocol.iq.si.Feature;
import com.sissi.protocol.iq.si.File;
import com.sissi.protocol.iq.si.Si;

/**
 * @author kim 2014年2月25日
 */
public class PersistentSi extends PersistentProtocol {

	private final String fieldName = "name";

	private final String fieldSize = "size";

	private final String profile = "http://jabber.org/protocol/si/profile/file-transfer";

	private final Feature feature = new Feature().setX(new XData().setType(XDataType.FORM).add(new XField().setType(XFieldType.LIST_SINGLE).setVar("stream-method").add(new XOption(Bytestreams.XMLNS))));

	private final String delegation;

	public PersistentSi(JIDBuilder jidBuilder, String title, String delegation) {
		super(Si.class, jidBuilder, title);
		this.delegation = delegation;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> query(Element element) {
		return BasicDBObjectBuilder.start(PersistentElementBox.fieldId, element.getId()).get().toMap();
	}

	@Override
	public Map<String, Object> write(Element element) {
		Si si = Si.class.cast(element);
		Map<String, Object> entity = super.write(si.parent().reply().setType(ProtocolType.SET.toString()));
		entity.put(PersistentElementBox.fieldHost, new String[] { si.host(this.delegation, super.jidBuilder.build(si.parent().getTo()).asStringWithBare()) });
		entity.put(PersistentElementBox.fieldClass, element.getClass().getSimpleName());
		entity.put(PersistentElementBox.fieldSid, si.getId());
		entity.put(this.fieldName, si.getFile().getName());
		entity.put(this.fieldSize, si.getFile().getSize());
		return entity;
	}

	@Override
	public Element read(Map<String, Object> element) {
		return IQ.class.cast(super.read(element, new IQ())).setId(element.get(PersistentElementBox.fieldSid).toString()).setFrom(this.delegation).add(new Si().setId(element.get(PersistentElementBox.fieldSid).toString()).setSource(PersistentElementBox.fieldFrom).setProfile(this.profile).setFeature(this.feature).setFile(new File().setName(element.get(this.fieldName).toString()).setSize(element.get(this.fieldSize).toString())));
	}
}
