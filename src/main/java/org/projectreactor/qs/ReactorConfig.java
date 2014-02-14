package org.projectreactor.qs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.util.IdGenerator;
import reactor.core.Environment;
import reactor.spring.context.config.EnableReactor;
import reactor.spring.core.task.RingBufferAsyncTaskExecutor;
import reactor.spring.core.task.WorkQueueAsyncTaskExecutor;
import reactor.util.UUIDUtils;

import java.util.UUID;

/**
 * Reactor configuration to provide components required by Spring and Spring Integration.
 *
 * @author Jon Brisbin
 */
@Configuration
@EnableReactor
public class ReactorConfig {

	/**
	 * Provide a Spring {@link org.springframework.util.IdGenerator} based on Reactor's optimized {@link java.util.UUID}
	 * generator {@link reactor.util.UUIDUtils#random()}.
	 *
	 * @return a randomly-generated {@link java.util.UUID}
	 */
	@Bean
	public IdGenerator randomUUIDGenerator() {
		return new IdGenerator() {
			@Override
			public UUID generateId() {
				return UUIDUtils.random();
			}
		};
	}

	/**
	 * Provide a Spring {@link org.springframework.core.task.AsyncTaskExecutor} implementation based on the high-speed,
	 * multi-threaded, {@code RingBuffer}-backed {@link reactor.event.dispatch.WorkQueueDispatcher}.
	 *
	 * @param env
	 * 		the Reactor {@link reactor.core.Environment}
	 *
	 * @return an {@link org.springframework.core.task.AsyncTaskExecutor} based on a multi-threaded {@code RingBuffer}
	 */
	@Bean
	public AsyncTaskExecutor workQueueTaskExecutor(Environment env) {
		return new WorkQueueAsyncTaskExecutor(env)
				.setBacklog(2048)
				.setThreads(8);
	}

	/**
	 * Provide a Spring {@link org.springframework.core.task.AsyncTaskExecutor} implementation based on the high-speed,
	 * single-threaded, {@code RingBuffer}-backed {@link reactor.event.dispatch.RingBufferDispatcher}.
	 *
	 * @param env
	 * 		the Reactor {@link reactor.core.Environment}
	 *
	 * @return an {@link org.springframework.core.task.AsyncTaskExecutor} based on a single-threaded {@code RingBuffer}
	 */
	@Bean
	public AsyncTaskExecutor ringBufferTaskExecutor(Environment env) {
		return new RingBufferAsyncTaskExecutor(env)
				.setBacklog(4096);
	}

}
