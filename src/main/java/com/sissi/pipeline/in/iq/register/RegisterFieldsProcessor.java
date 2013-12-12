package com.sissi.pipeline.in.iq.register;

import java.util.List;

import com.sissi.context.JIDContext;
import com.sissi.pipeline.Input;
import com.sissi.protocol.Protocol;
import com.sissi.protocol.Protocol.Type;
import com.sissi.protocol.iq.register.Register;
import com.sissi.ucenter.field.Field;

/**
 * 
 * @author kim 2013年12月3日
 */
public class RegisterFieldsProcessor implements Input {

	private final List<Field<?>> fields;

	public RegisterFieldsProcessor(List<Field<?>> fields) {
		super();
		this.fields = fields;
	}

	@Override
	public Boolean input(JIDContext context, Protocol protocol) {
		context.write(Register.class.cast(protocol).add(fields).getParent().setType(Type.RESULT));
		return true;
	}
}
