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
import java.sql.SQLException;
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
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PreDestroy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mibs.asterisk.web.config.Utils;
import com.mibs.asterisk.web.controller.Agent;
import com.mibs.asterisk.web.controller.CurrentQueue;
import com.mibs.asterisk.web.controller.QueueContents;
import com.mibs.asterisk.web.events.AgentCalledEvent;
import com.mibs.asterisk.web.events.AgentConnectEvent;
import com.mibs.asterisk.web.events.AsteriskEvent;
import com.mibs.asterisk.web.events.QueueCallerAbandonEvent;
import com.mibs.asterisk.web.events.QueueMemberAddedEvent;
import com.mibs.asterisk.web.events.QueueMemberRemovedEvent;
import com.mibs.asterisk.web.events.QueueMemberStatusEvent;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;

import lombok.Getter;
import lombok.Setter;
import redis.clients.jedis.Jedis;

@Getter
@Setter
@RestController
public class AsteriskListener {

	private static final Logger logger = LogManager.getLogger(AsteriskListener.class.getName());

	private AppConfig config;
	private final SimpMessagingTemplate template;
	private String clientid;
	private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd-MM-yyyy");
	private final Pattern pt = Pattern.compile("SIP/\\d+");

	private static Channel channel;

	private Lock lock = new ReentrantLock();
	private Condition condition = lock.newCondition();

	private AtomicBoolean noAuth = new AtomicBoolean(true);
	private Map<String, Class<? extends AsteriskEvent>> registeredEventClasses = new HashMap<>();
	public SocketConnector connector;

	private QueueContents content;
	private static Jedis jedis;

	@PreDestroy
	public void onDestroy() throws Exception {

		jedis.close();
		System.out.println("Spring Container is destroyed!");
	}

	public AsteriskListener(AppConfig co, SimpMessagingTemplate template) {
		this.config = co;
		this.template = template;
		Utils.init(this.config);
		registerEventClasses();
		initRabbitMQConnection();
		runListener();

		jedis = new Jedis(this.config.getRedisHost());
		jedis.auth(this.config.getRedisPssword());

	}

	public static void publish(AgentCalledEvent event) {

		byte[] res = jedis.get(event.getCallerIdnum().trim().getBytes());
		if ((res != null) && (res.length > 0)) {

			String queue = "SIP_" + event.getDestcallerIdnum();
			try {
				channel.basicPublish("", queue, null, res);
			} catch (Exception e) {
				logger.error("Error while publishing message to RabbitMQ queue: " + queue + " for callid: "
						+ event.getDestcallerIdnum());
			}

		}
	}

	private void initRabbitMQConnection() {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(config.getRabbitmqHost());
		factory.setUsername(config.getRabbitmqUsername());
		factory.setPassword(config.getRabbitmqPassword());
		factory.setPort(5672);
		com.rabbitmq.client.Connection connection;

		try {
			connection = factory.newConnection();
			channel = connection.createChannel();
			String sql = "select name from  peers";
			String connURL = "jdbc:mysql://" + config.getDbHost() + ":3306/" + config.getDbName()
					+ "?useUnicode=yes&characterEncoding=UTF-8";
			try (Connection connect = DriverManager.getConnection(connURL, config.getDbUser(), config.getDbPassword());
					Statement statement = connect.createStatement();
					ResultSet rs = statement.executeQuery(sql)) {
				while (rs.next()) {
					if ((rs.getString("name") != null) && (rs.getString("name").length() > 0)) {
						String s = rs.getString("name").replace("/", "_");
						try {
							channel.queueDeclare(s, false, false, false, null);
						} catch (IOException e) {
							logger.error("Error declearing queue with message: " + e.getMessage());
						}
					}
				}
			} catch (SQLException e1) {
				logger.error("Error connecting to database with message: " + e1.getMessage());
			}

		} catch (IOException | TimeoutException e) {

			logger.error("Error rabbitMQ connection with message: " + e.getMessage());
		}
	}

	@CrossOrigin(origins = "*")
	@GetMapping("/init")
	public QueueContents init() {

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

		return content;

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
					if (m.find()) {
						String phone = m.group(0);
						Agent agent = new Agent();
						agent.setNumber(phone);
						agent.setState("0");
						Optional<String> optName = Utils.getAgentName(queue, phone);
						if (optName.isPresent()) {
							agent.setName(optName.get());
						} else {
							agent.setName("Unknown");
						}
						currentQueue.addMember(agent);
						currentQueue.setCallers(getCallers(lines));
					}

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

	int getCallers(List<String> lines) {
		int rs = 0;
		for (String ln : lines) {
			if (ln.contains("Callers:")) {
				break;
			}
			rs++;
			;
		}
		return lines.size() > 0 ? lines.size() - rs - 1 : 0;
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
		registerEventClasses(AgentConnectEvent.class);
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
			// socket.setSoTimeout(3000);
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
								event.execute(template);
							}
							sb = null;
						}

						if (line.startsWith("Output:")) {
							if (line.length() > "Output: ".length()) {
								qLines.add(line);

							} else {
								Optional<CurrentQueue> optQueue = buildCurrentQueue(qLines);
								if (optQueue.isPresent()) {

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
