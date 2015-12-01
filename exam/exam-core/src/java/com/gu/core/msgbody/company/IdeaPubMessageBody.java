package com.gu.core.msgbody.company;

import com.gu.core.interfaces.MessageBody;

public class IdeaPubMessageBody extends MessageBody {

	private long ideaId;

	private long caseId;

	public long getIdeaId() {
		return ideaId;
	}

	public void setIdeaId(long ideaId) {
		this.ideaId = ideaId;
	}

	public long getCaseId() {
		return caseId;
	}

	public void setCaseId(long caseId) {
		this.caseId = caseId;
	}
}
