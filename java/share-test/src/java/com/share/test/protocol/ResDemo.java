package com.share.test.protocol;

import io.netty.buffer.ByteBuf;

import com.share.core.annotation.Protocol;
import com.share.core.protocol.protocol.ProtocolResponse;
import com.share.core.util.StringUtil;

@Protocol
public class ResDemo extends ProtocolResponse {
	private byte[] email = new byte[10];

	public String getEmail() {
		return new String(email).trim();
	}

	public void setEmail(String email) {
		//StringUtil.copyToBytes(email, this.email);
	}

	public void loadFromBuffer(ByteBuf buffer) {
		buffer.readBytes(email);
	}

	public void convert2Buffer(ByteBuf buffer) {
		buffer.writeBytes(email);
	}
}
