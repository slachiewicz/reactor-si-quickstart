package org.projectreactor.qs.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.Assert;
import org.springframework.util.StopWatch;
import reactor.core.Environment;
import reactor.event.dispatch.SynchronousDispatcher;
import reactor.function.Consumer;
import reactor.io.Buffer;
import reactor.io.encoding.Codec;
import reactor.net.NetChannel;
import reactor.net.config.ServerSocketOptions;
import reactor.net.config.SslOptions;
import reactor.net.netty.tcp.NettyTcpServer;
import reactor.net.tcp.TcpServer;
import reactor.net.tcp.spec.TcpServerSpec;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A Spring Integration {@literal InboundChannelAdapter} that ingest incoming TCP data using Reactor's Netty-based TCP
 * support.
 *
 * @author Jon Brisbin
 */
public class ReactorTcpInboundChannelAdapter<IN, OUT>
		extends MessageProducerSupport
		implements ApplicationEventPublisherAware {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final MessageHandler            messageHandler;
	private final TcpServerSpec<IN, OUT>    spec;
	private       TcpServer<IN, OUT>        server;
	private       ApplicationEventPublisher eventPublisher;

	public ReactorTcpInboundChannelAdapter(Environment env,
	                                       int listenPort,
	                                       String dispatcher,
	                                       final MessageHandler messageHandler) {
		this.messageHandler = messageHandler;
		Assert.notNull(env, "Environment cannot be null.");

		this.spec = new TcpServerSpec<IN, OUT>(NettyTcpServer.class)
				.env(env)
				.dispatcher(new SynchronousDispatcher())
				.listen(listenPort)
				.consume(new Consumer<NetChannel<IN, OUT>>() {
					@Override
					public void accept(NetChannel<IN, OUT> conn) {
						final AtomicLong msgCnt = new AtomicLong();
						final StopWatch stopWatch = new StopWatch();
						stopWatch.start();
						conn
								.when(Throwable.class, new Consumer<Throwable>() {
									@Override
									public void accept(Throwable t) {
										if(null != eventPublisher) {
											eventPublisher.publishEvent(new NetChannelExceptionEvent(t));
										}
									}
								})
								.consume(new Consumer<IN>() {
									@Override
									public void accept(IN in) {
										messageHandler.handleMessage(new GenericMessage<>(in));
										msgCnt.incrementAndGet();
									}
								})
								.on().close(new Runnable() {
							@Override
							public void run() {
								long cnt = msgCnt.get();
								stopWatch.stop();

								log.info("throughput this session: {}/sec in {}ms",
								         (int)(cnt / stopWatch.getTotalTimeSeconds()),
								         stopWatch.getTotalTimeMillis());
							}
						});
					}
				});
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	/**
	 * Fluent alias for {@link #setOutputChannel(org.springframework.messaging.MessageChannel)}.
	 *
	 * @param outputChannel
	 * 		the output channel to use
	 *
	 * @return {@literal this}
	 */
	public ReactorTcpInboundChannelAdapter<IN, OUT> setOutput(MessageChannel outputChannel) {
		super.setOutputChannel(outputChannel);
		return this;
	}

	/**
	 * Set the {@link ServerSocketOptions} to use.
	 *
	 * @param options
	 * 		the options to use
	 *
	 * @return {@literal this}
	 */
	public ReactorTcpInboundChannelAdapter<IN, OUT> setServerSocketOptions(ServerSocketOptions options) {
		spec.options(options);
		return this;
	}

	/**
	 * Set the {@link SslOptions} to use.
	 *
	 * @param sslOptions
	 * 		the options to use
	 *
	 * @return {@literal this}
	 */
	public ReactorTcpInboundChannelAdapter<IN, OUT> setSslOptions(SslOptions sslOptions) {
		spec.ssl(sslOptions);
		return this;
	}

	/**
	 * Set the {@link reactor.io.encoding.Codec} to use.
	 *
	 * @param codec
	 * 		the codec to use
	 *
	 * @return {@literal this}
	 */
	public ReactorTcpInboundChannelAdapter<IN, OUT> setCodec(Codec<Buffer, IN, OUT> codec) {
		spec.codec(codec);
		return this;
	}

	@Override
	protected void doStart() {
		server = spec.get().start(null);
	}

	@Override
	protected void doStop() {
		try {
			server.shutdown().await();
		} catch(InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

}
