package com.gu.core.msgbody.user;

import com.gu.core.interfaces.MessageBody;

/**
 * 删除点子消息
 * @author ruan
 */
public class DelIdeaMessageBody extends MessageBody {
	private long caseId;

	public long getCaseId() {
		return caseId;
	}

	public void setCaseId(long caseId) {
		this.caseId = caseId;
	}
}