package org.projectreactor.qs.integration.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Uses the efficient sendfile system call to send data to the server. This is the most efficient way to transmit data
 * to the server and results in lowering the client's use of the system and keeps it from interfering with throughput
 * measurements of the server-side components.
 *
 * @author Jon Brisbin
 */
public class LoadTestClient {

	/**
	 * Run a test that transmits the pre-generated data in "src/main/resources/data.bin" to the server.
	 *
	 * @param args
	 * 		arg0: port, arg1: threads
	 *
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws ExecutionException
	 * @throws TimeoutException
	 */
	public static void main(String... args) throws InterruptedException,
	                                               IOException,
	                                               ExecutionException,
	                                               TimeoutException {
		int port = Integer.parseInt(args[0]);
		int threads = Integer.parseInt(args[1]);

		List<Client> clients = new ArrayList<>();
		for(int i = 0; i < threads; i++) {
			clients.add(new Client(port));
		}

		ExecutorService threadPool = Executors.newFixedThreadPool(threads);

		for(Future<Void> f : threadPool.invokeAll(clients)) {
			f.get();
		}
		threadPool.shutdown();
	}

	private static class Client implements Callable<Void> {
		private final int port;

		private Client(int port) {
			this.port = port;
		}

		@Override
		public Void call() {
			try {
				SocketChannel client = SocketChannel.open();
				client.configureBlocking(true);
				client.connect(new InetSocketAddress(port));

				FileChannel data = FileChannel.open(Paths.get("src/main/resources/data.bin"));
				long size = data.size();

				long start = System.currentTimeMillis();
				data.transferTo(0, size, client);
				long end = System.currentTimeMillis();
				System.out.println("data sent to localhost:" + port + " in " + (end - start) + "ms");

				client.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
}
