package com.gu.core.msgbody.user;

import com.gu.core.interfaces.MessageBody;

public class IdeaPraiseUpMessageBody extends MessageBody {
	
	private long ideaId;

	public long getIdeaId() {
		return ideaId;
	}

	public void setIdeaId(long ideaId) {
		this.ideaId = ideaId;
	}

}
