package com.mibs.asterisk.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Service
public class AsteriskListener {

	private static final Logger logger = LogManager.getLogger(AsteriskListener.class.getName());

	@Autowired
	private AppConfig conf;

	private String clientid;

	private Lock lock = new ReentrantLock();
	private Condition condition = lock.newCondition();
	AtomicBoolean noAuth = new AtomicBoolean(true);

	SocketConnector connector;

	class SocketConnector {
		private PrintWriter out;
		private BufferedReader in;
		private Socket socket;

		public SocketConnector(String ip, int port) throws UnknownHostException, IOException {

			SocketAddress endpoint = new InetSocketAddress(ip, port);
			socket = new Socket();
			socket.setSoTimeout(3000);
			socket.connect(endpoint, 3000);
			socket.setKeepAlive(true);
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

		}

		public Socket getSocket() {
			return this.socket;
		}

		public BufferedReader getReader() {
			return this.in;
		}

		public boolean doLogin() throws IOException {
			out.write("Action: Login\r\nActionID:12345\r\nUsername: " + conf.getUser() + "\r\nSecret: "
					+ conf.getPassword() + "\r\n\r\n");
			out.flush();

			boolean result = false;

			for (String line = in.readLine(); line != null; line = in.readLine()) {

				if (line.contains("Authentication accepted")) {
					result = true;
					line = null;
					break;
				}

			}
			return result;
		}

	}

	class Auther implements Runnable {

		@Override
		public void run() {
			while (true) {
				logger.info("Autherization has been started for user: " + conf.getUser() + " and host: "
						+ conf.getHost() + ":" + conf.getPort());
				lock.lock();
				try {

					if (connector != null && connector.getSocket() != null) {

						connector.getSocket().close();
					}

					connector = new SocketConnector(conf.getHost(), conf.getPort());

					if (connector.doLogin()) {
						logger.info("Logged in SUCCESS! Time: " + LocalDateTime.now());
						try {
							Thread.sleep(1000);
							noAuth.set(false);
							condition.signalAll();
							condition.await();
						} catch (InterruptedException e1) {
							logger.error("Error Interrupted Exception with message: " + e1.getMessage());
						}

					}

				} catch (UnknownHostException e) {
					logger.error("Error Unknown Host with message: " + e.getMessage());
					System.exit(0);
				} catch (IOException e) {
					logger.info("Try to create new connection....");
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e1) {
						logger.info("Interrupted Exception with message: " + e.getMessage());
					}
				} finally {
					lock.unlock();
				}
			}
		}
	}

	class Listener implements Runnable {
		@Override
		public void run() {
			while (true) {
				lock.lock();
				try {
					while (noAuth.get()) {
						logger.info("Wait for connection...");
						condition.await();
					}
					int i = 0;
					for (String line = connector.getReader().readLine(); line != null; line = connector.getReader()
							.readLine()) {
						if (line.contains("MusicOnHoldStop")) {
							System.out.println(line + ":  " + i++);
						}

					}
					noAuth.set(true);
					condition.signalAll();
				} catch (SocketTimeoutException ex) {

					logger.info("Socket Timeout Exception with message: " + ex.getMessage());

					if (connector.getSocket() != null) {
						try {
							logger.info("Try to close socket...");
							connector.getSocket().close();
						} catch (IOException e) {
							logger.error("Error has occured while closing socket with message: " + e.getMessage());
						} finally {
							noAuth.set(true);
							condition.signalAll();
						}
					}

				} catch (InterruptedException | IOException e) {
					logger.error("Error has occured while listening socket with message: " + e.getMessage());
				} finally {
					lock.unlock();
				}
			}
		}

	}

	public void runMe() {
		ExecutorService executor = Executors.newCachedThreadPool();
		executor.execute(new Auther());
		executor.execute(new Listener());
		executor.shutdown();

	}

	public AsteriskListener() {
		logger.info("Start Applocation");
	}

}
