package org.projectreactor.qs.integration;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

/**
 * Channel implementation that optimizes for the single, fixed MessageHandler case by using a
 * final instance variable and invoking that handler directly whenever a Message is sent to it.
 *
 * @author Mark Fisher
 */
public class SimpleChannel implements MessageChannel {

	private final MessageHandler handler;

	public SimpleChannel(MessageHandler handler) {
		this.handler = handler;
	}

	@Override
	public boolean send(Message<?> message) {
		handler.handleMessage(message);
		return true;
	}

	@Override
	public boolean send(Message<?> message, long timeout) {
		handler.handleMessage(message);
		return true;
	}

}
