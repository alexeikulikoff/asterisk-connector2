package com.mibs.asterisk.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mibs.asterisk.web.controller.CurrentQueue;
import com.mibs.asterisk.web.controller.InitQueue;
import com.mibs.asterisk.web.controller.QueueContents;
import com.mibs.asterisk.web.events.AgentCalledEvent;
import com.mibs.asterisk.web.events.AsteriskEvent;
import com.mibs.asterisk.web.events.QueueCallerAbandonEvent;
import com.mibs.asterisk.web.events.QueueMemberAddedEvent;
import com.mibs.asterisk.web.events.QueueMemberRemovedEvent;
import com.mibs.asterisk.web.events.QueueMemberStatusEvent;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@RestController
public class AsteriskListener {

	private static final Logger logger = LogManager.getLogger(AsteriskListener.class.getName());

//	Members:(.*?(\n))+.*?Callers:

	private AppConfig config;

	private String clientid;
	private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd-MM-yyyy");
	private final Pattern pt = Pattern.compile("SIP/\\d+");

	private Lock lock = new ReentrantLock();
	private Condition condition = lock.newCondition();

	private AtomicBoolean noAuth = new AtomicBoolean(true);
	private Map<String, Class<? extends AsteriskEvent>> registeredEventClasses = new HashMap<>();
	public SocketConnector connector;

	private QueueContents content;

	public AsteriskListener(AppConfig co) {
		this.config = co;
		registerEventClasses();
		runListener();
	}

	@GetMapping("/init")
	public InitQueue init() {

		content = new QueueContents();

		String connURL = "jdbc:mysql://" + config.getDbHost() + ":3306/" + config.getDbName()
				+ "?useUnicode=yes&characterEncoding=UTF-8";
		String sql = "select name from queues";
		try (Connection connect = DriverManager.getConnection(connURL, config.getDbUser(), config.getDbPassword());
				Statement statement = connect.createStatement();
				ResultSet rs = statement.executeQuery(sql)) {

			while (rs.next()) {
				String queue = rs.getString("name");
				doCommand(queue);
				synchronized (content) {
					try {
						content.wait();

					} catch (InterruptedException e) {
						logger.error("Error wait is interrupted with message: " + e.getMessage());
					}
				}

			}
		} catch (Exception e) {
			logger.error("Error in getQueueContents " + e.getMessage());

		}

		return new InitQueue("hello: " + content);

	}

	private void doCommand(String queue) throws IOException {
		connector.getPrintWriter()
				.write("Action: COMMAND\r\nActionID:12345\r\ncommand: queue show " + queue + "\r\n\r\n");
		connector.getPrintWriter().flush();
	}

	Optional<CurrentQueue> buildCurrentQueue(List<String> lines) {
		Optional<String> opt = getQueueName(lines);
		if (opt.isPresent()) {
			String queue = opt.get();
			CurrentQueue currentQueue = new CurrentQueue(queue);
			boolean fl = false;
			for (String s : lines) {
				if (s.contains("Members:")) {
					fl = true;
					continue;
				}
				if (s.contains("Callers:")) {
					break;
				}
				if (fl) {
					Matcher m = pt.matcher(s);
					if (m.find())

						currentQueue.addMember(m.group(0));
				}
			}
			return Optional.ofNullable(currentQueue);

		} else {
			return Optional.empty();
		}
	}

	Optional<String> getQueueName(List<String> lines) {
		Pattern pattern = Pattern.compile("Output:(.*?)has");
		if (lines != null && lines.size() > 0) {

			Matcher matcher = pattern.matcher(lines.get(0));
			if (matcher.find()) {
				return Optional.ofNullable(matcher.group().replace("Output: ", "").replace(" has", ""));
			}
		}
		return Optional.empty();

	}

	Optional<AsteriskEvent> buildEvent(StringBuilder line) {

		Class<? extends AsteriskEvent> eventClass = null;
		Constructor<?> constructor = null;
		AsteriskEvent event = null;
		Map<String, Method> methodMap = new HashMap<String, Method>();
		Map<String, String> cmdMap = new HashMap<String, String>();
		Optional<String> optEventName = eventName(line);
		if (!optEventName.isPresent())
			return Optional.empty();
		eventClass = registeredEventClasses.get(optEventName.get());
		if (eventClass == null)
			return Optional.empty();

		try {
			constructor = eventClass.getConstructor();
			event = (AsteriskEvent) constructor.newInstance();
		} catch (Exception e1) {
			return Optional.empty();
		}

		Class<? extends AsteriskEvent> buildedClass = event.getClass();

		for (Method m : buildedClass.getMethods()) {
			if (m.getName().startsWith("set")) {
				methodMap.put(m.getName().toLowerCase(Locale.ENGLISH), m);
			}
		}
		String attr, value, s;
		Method method;
		String[] lines = line.toString().split("\n");
		for (int i = 0; i < lines.length; i++) {
			s = lines[i].toLowerCase();
			if (s.contains(":")) {
				String[] arr = s.split(":");
				attr = arr[0].replaceAll("\\s", "");
				attr = attr.indexOf("-") > 0 ? attr.replace("-", "") : attr;
				value = arr[1].replaceAll("\\s", "");
				cmdMap.put("set" + attr, value);
			}
		}
		for (Map.Entry<String, String> bmap : cmdMap.entrySet()) {
			if ((method = methodMap.get(bmap.getKey())) != null) {
				try {
					method.invoke(event, bmap.getValue());
				} catch (IllegalAccessException e) {
					return Optional.empty();
				} catch (IllegalArgumentException e) {
					return Optional.empty();
				} catch (InvocationTargetException e) {
					return Optional.empty();
				}
			}
		}

		return Optional.ofNullable(event);

	}

	private void registerEventClasses() {
		registerEventClasses(QueueMemberAddedEvent.class);
		registerEventClasses(QueueMemberRemovedEvent.class);
		registerEventClasses(QueueMemberStatusEvent.class);
		registerEventClasses(AgentCalledEvent.class);
		registerEventClasses(QueueCallerAbandonEvent.class);
	}

	private void registerEventClasses(Class<? extends AsteriskEvent> clazz) {
		String className;
		String eventType;
		className = clazz.getName();
		eventType = className.substring(className.lastIndexOf('.') + 1).toLowerCase(Locale.ENGLISH);
		if (!eventType.endsWith("event")) {
			throw new IllegalArgumentException(clazz + " is not a AsteriskEvent");
		}
		eventType = eventType.substring(0, eventType.length() - "event".length());
		registeredEventClasses.put(eventType.toLowerCase(Locale.US), clazz);
	}

	private Optional<String> eventName(StringBuilder line) {
		String eventName = null;
		if (line != null && line.length() > 0) {
			eventName = line.toString().split("\n")[0].split(":")[1].toLowerCase().trim();
		}
		return (eventName != null) ? Optional.of(eventName) : Optional.empty();
	}

	public class SocketConnector {
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

		public PrintWriter getPrintWriter() {
			return this.out;
		}

		public BufferedReader getReader() {
			return this.in;
		}

		public boolean doLogin() throws IOException {
			out.write("Action: Login\r\nActionID:12345\r\nUsername: " + config.getUser() + "\r\nSecret: "
					+ config.getPassword() + "\r\n\r\n");
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

				logger.info("Autherization has been started for user: " + config.getUser() + " and host: "
						+ config.getHost() + ":" + config.getPort());

				lock.lock();
				try {

					if (connector != null && connector.getSocket() != null) {

						connector.getSocket().close();
					}

					connector = new SocketConnector(config.getHost(), config.getPort());

					if (connector.doLogin()) {
						logger.info("Logged in SUCCESS! Time: " + formatter.format(LocalDateTime.now()));
						try {
							Thread.sleep(3000);
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
					StringBuilder sb = null;
					List<String> qLines = new ArrayList<>();
					for (String line = connector.getReader().readLine(); line != null; line = connector.getReader()
							.readLine()) {
						if (line.startsWith("Event")) {
							sb = new StringBuilder();
						}
						if (sb != null) {
							sb.append(line);
							sb.append("\n");
						}
						if (line.length() == 0) {
							Optional<AsteriskEvent> opt = buildEvent(sb);
							if (opt.isPresent()) {

								AsteriskEvent event = opt.get();
								// System.out.println(event);
								event.execute();
							}
							sb = null;
						}

						if (line.startsWith("Output:")) {
							if (line.length() > "Output: ".length()) {
								qLines.add(line);

							} else {
								Optional<CurrentQueue> optQueue = buildCurrentQueue(qLines);
								if (optQueue.isPresent()) {
									System.out.println(optQueue.get());
									synchronized (content) {
										content.addQueueResponce(optQueue.get());
										content.notify();
									}

								}
								qLines.clear();
							}

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

	public void runListener() {
		ExecutorService executor = Executors.newCachedThreadPool();
		executor.execute(new Auther());
		executor.execute(new Listener());
		executor.shutdown();

	}

}
