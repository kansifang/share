package com.gu.core.msgbody.user;

import com.gu.core.interfaces.MessageBody;

/**
 * 邀请回答专案消息
 * @author ruan
 */
public class UserInvitePubIdeaMessageBody extends MessageBody {
	/**
	 * 专案id
	 */
	private long caseId;

	public long getCaseId() {
		return caseId;
	}

	public void setCaseId(long caseId) {
		this.caseId = caseId;
	}
}