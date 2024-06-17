# Spring-RewardReferenceDomain

Every time a user dines at a restaurant participating in the network, a contribution is made to his account.

## Spring Framework:

Spring Framework es un marco de trabajo integral para el desarrollo de aplicaciones empresariales en Java. Proporciona una amplia gama de funcionalidades y módulos para abordar diferentes aspectos del desarrollo de aplicaciones, como:

- Gestión de la capa de persistencia (Spring Data).
- Inyección de dependencias (Spring Core).
- Desarrollo web (Spring MVC).
- Seguridad (Spring Security).
- Integración de servicios (Spring Integration), entre otros.

Spring Framework se centra en la modularidad y la flexibilidad, permitiendo a los desarrolladores elegir los componentes que necesitan para sus aplicaciones.

## Spring Boot:

Spring Boot es una extensión del Spring Framework que simplifica significativamente el proceso de configuración y desarrollo de aplicaciones Spring. Proporciona un enfoque "opinado" o preconfigurado para el desarrollo de aplicaciones, lo que significa que viene con una serie de configuraciones predeterminadas y dependencias incorporadas que permiten iniciar rápidamente un proyecto sin necesidad de realizar configuraciones manuales extensas.

Spring Boot utiliza la "convención sobre configuración", proporcionando configuraciones predeterminadas sensatas basadas en las convenciones y patrones comunes de desarrollo. Sin embargo, también permite la personalización cuando sea necesario.

Spring Boot incluye características como un servidor integrado de aplicaciones web (por ejemplo, Tomcat, Jetty), gestión automática de dependencias (a través de Maven o Gradle), inicio rápido (con `@SpringBootApplication`), y una amplia gama de complementos y módulos que facilitan el desarrollo de aplicaciones.

__Este proyecto estará basado simplemente en Spring Framework.__

## Spring Application Context

It represents the Spring Dependency Injection Container. Beans are managed inside it. Context are based in one or more than one configuration class.
There are two ways to configure the application context. One is __using external config files__ and another one is __using anottations__.

### External Config Files to configure Application context.

Usually configuration files are splitted:

- Web Config : Contains all the configuration related to the web part.
- Application Config: Contains the general configuration of the application.
- Infrastructure Config: Contains all the infrastucture related config. For example Db conections. Usually inside this config you import the other config and then in the other configs (app or web), using autowire, you can use beans from infra config.

```java {"id":"01HXGYTVKEKW4N4PGB5GQQMJXV"}
class RewardNetworkTest {

    private RewardNetwork rewardNetwork;

    @BeforeEach 
    void setUp() {
        ApplicationContext context = SpringApplication.run(TestInfrastructureConfig.class);
        rewardNetwork= context.getBean(RewardNetwork.class);
    }
    
}
```

```java {"id":"01HXGYTVKEKW4N4PGB5MC37RVF"}
@Configuration
// Import the beans from the application config.
@Import(RewardsConfig.class)
public class TestInfrastructureConfig {

    /**
     * Creates an in-memory "rewards" database populated
     * with test data for fast testing
     */
    @Bean
    public DataSource dataSource() {
        return (new EmbeddedDatabaseBuilder()) //
                .addScript("classpath:rewards/testdb/schema.sql") //
                .addScript("classpath:rewards/testdb/data.sql") //
                .build();
    }
}
```

```java {"id":"01HXGYTVKEKW4N4PGB5R0MN1B1"}
@Configuration
public class RewardsConfig {

	// Set this by adding a constructor.
	private DataSource dataSource;

	//Autowires gets the datasource from Infra Config
	@Autowired
	public RewardsConfig(DataSource ds){
		this.dataSource=ds;
	}
	// Each bean of application config represent a different component of application.
	@Bean
	 public AccountRepository accountRepository(){
		JdbcAccountRepository repo = new JdbcAccountRepository();
		repo.setDataSource(this.dataSource);
		return repo;
	}
	@Bean
	public RestaurantRepository restaurantRepository(){
		JdbcRestaurantRepository repo = new JdbcRestaurantRepository();
		repo.setDataSource(this.dataSource);
		return repo;
	}
	@Bean
	public RewardRepository rewardRepository(){
		JdbcRewardRepository repo = new JdbcRewardRepository();
		repo.setDataSource(this.dataSource);
		return repo;
	}
	@Bean
	public RewardNetwork rewardNetwork(AccountRepository account, RestaurantRepository restaurant ,RewardRepository reward){
		return new RewardNetworkImpl(account,restaurant,reward);
	}

}
```
### Anotations for configuring App Context.

The config file will be empty and you will use the following anotation to scann all the components/beans on the corresponding configured classes.
Components can be:

- @Component
- @Service
- @Repository
- @Controller

```java {"id":"01HYFQZSJA955AWS4SNEV1ESPB"}
// It will scann all the cllasses configured as components in the package rewards.internal
@ComponentScan("rewards.internal")
@Configuration
public class RewardsConfig {	
}

```

This will be an example of a class configured as component/bean. We use @Repository to identfy it as a component and @Autowired to
resolve the dependency inyection. In this case Datasource will be taken from another config file which defines it.

```java {"id":"01HYFQZSJBD01N5FD05BXCHTD7"}
@Repository
public class JdbcRewardRepository implements RewardRepository {

    private DataSource dataSource;

    /**
     * Sets the data source this repository will use to insert rewards.
     * @param dataSource the data source
     */
    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
}
```

## Container Lifecycle

The difference between annotation config and external config is:

| Type of Config | Bean Detection | BeanFactoryPP | Bean instantiation and DI | Bean PP |
|-----------|-----------|-----------|-----------|-----------|
| Java config| Read @Bean| ' | Call @Bean method impl| ' |
| Annotation config | @Component Scanning| ' | 1. Instantiation & @Autowired on Constructor. <br> 2. Injection of@Autowired methods and fields| ' |

### Initialization

There are two main steps and each of those have also there own steps:

1. Load and Process Bean Definitions

- Load Bean Definitions ti the BeanFactory. Just the names ofthe Beans.
- Post Process Bean Definitions using the BeanFactoryPostProcessor. An example is PropertySourcesPlaceholderConfigurer (it implemnts BeanFactoryPostProcessor) responsable for inserting external properties:
#### External properties to control Configuration

Property Source

It tells Spring from where to get those evironment variables or Java System property.
Available resource prefixes: classpath: file: http:

```java {"id":"01HXH47WVTGS3NPAYHK4B2Z3J9"}
@Configuration
@PropertySource ( "classpath:/com/organization/config/app.properties" )
@PropertySource ( "file:config/local.properties" )
public class ApplicationConfig {
｝
```

Environment variables.

```java {"id":"01HXH47WVTGS3NPAYHK7QQZK3J"}
@Bean 
public DataSource dataSource(Environment env) {
BasicDataSource ds = new BasicDataSource();
ds.setDriverClassName( env.getProperty( "db.driver" ));
ds.setUrl( env.getProperty("db.url"));
ds.setUser( env.getProperty( "db.user"));
ds.setPassword( env.getProperty( "db.password"));
return ds;
}
```

Using @Value

```java {"id":"01HXH47WVTGS3NPAYHK88N5JHG"}
@Configuration 
public class DbConfig (
@Bean
public DataSource data Source(
@Value("${db.driver}") String driver,
@Value("${db.urf}") String url,
@Value("${db.user}") String user,
@Value("${db.password}") String pwd) {
BasicDataSource ds = new BasicDataSource():
ds.setDriverClassName( driver):
ds.setUri( urf):
ds.setUser( user):
ds.setPassword( pwd )):
return ds;
}
```
We can also implement our own BeanFactoryPostProcessor. It has to be declared as a Bean.

``` java
@Bean
public static BeanFactoryPostProcessor myConfigurer() {
return new MyConfigurationCustomizer();
}
```

``` java
public class MyConfigurationCustomizer implements BeanFactoryPostProcessor {
// Perform customization of the configuration such as the placeholder syntax
}
```

2. Create and initialize Beans

- Find/Create its dependencies.
- Instantiate Beans (Dependency Injection)
- Perform Setter Injection (Dependency Injection)
- BeanPostProcessor (Before Init->Initiallizers call->After Init)

    Un ejemplo de esto es `@PostConstruct`: se utiliza para definir el/los método/s que queremos ejecutar después de construir el componente o bean. Puedes crear tu propio BeanPostProcessor implementándolo.

    ```java
    public interface BeanPostProcessor {
        public Object postProcessBeforeInitialization(Object bean, String beanName); 
        public Object postProcessAfterInitialization(Object bean, String beanName);
    }
    ```

    A veces, en lugar de devolver el bean, el BeanPostProcessor devuelve un proxy. Ese proxy puede ser: JDK Proxy y CGLib Proxy (Spring Boot). El primero se utiliza si tu clase implementa una interfaz y el segundo si no.


- Bean ready to use.

### Usage

Beans are available for use.

### Destruction

Before this phase is when you can run __@PreDestroy__:  is used to define the method/s that we want to execute before destroying the component or bean.
Beans are release for Garbage Collector


### Beans Scopes

- Singleton(default) One instance for every object.
- Prototype: New instance created everytime a bean is refered.
- Session: In web apps you have one instance per user session.
- Request: In web apps you have one instance per request. LifeCycle is very short

### Spring Profiles

Contains a group of Beans and usually represents each environment. Profiles can be defined at class level or bean/method level.

```java {"id":"01HXH47WVTGS3NPAYHK88N5JHG"}
@Configuration 
@Profile( "dev" )
public class DevConfig{

}

@Configuration 
@Profile( "!dev" )
public class ProdConfig{

}
```

Profiles must be activated at run-time:

- System property via command-line

```sh {"id":"01HYFQZSJA955AWS4SNB298HZF"}
Dspring.profiles.active=embedded,jpa
```

- System property programmatically

```sh {"id":"01HYFQZSJA955AWS4SNEA4F3Q1"}
System.setProperty("spring profiles.active", "embedded,jpa");
SpringApplication.run(AppConfig.class);
```

- Integration Test only: @ActiveProfiles

## Spring Aspect Oriented Programing (AOP)

Enable modularization of cross-cutting concerns. For example: Logs, security config etc. It is use to manage something common for all the app in a single point.
The leading AOP Technologies are:
- AspectJ
- Spring AOP

The core AOP Concepts are:
- Joint Point: method call or exception thrown.
- Pointcut: Expresion that selectes one or more Join Point
- Advice: Code to be executed at each selected Join Point.
	- Before
	```java
	//
	@Before(“execution(void set*(*))”)
	```
	- AfterReturning
	```java
	//	Audit all operations in the service package that return a Reward object
	@AfterReturning(value=“execution(* service..*.*(..))”,returning=“reward”)
	```
	- AfterThrowing
	```java
	//Send an email every time a Repository class throws an exception of type DataAccessException
	@AfterThrowing(value="execution(* rewards.internal.*.*Repository.*(..))", throwing="e")
	```
	- After
	```java
	// Track calls to all update methods
	@After(*execution(void update*(..))”)
	```
	- Arround
	```java
	// Cache values returned by cacheable services
	@Around(“execution(@example.Cacheable * rewards.service..”.*(..))")
	```
- Aspect: A module that encapsulates pointcuts and advice.
- Weaving: Technique to combine aspects and main code.
- AOP Proxy: An "enhanced" class that stands in place of your original.

An example could be to implement a log message before a setter method.

```java
public interface Cache {
	public void setCacheSize(int size);
	public void setTimeout(int ms);
}

public class SimpleCache Implements Cache { 
	private int cacheSize, ms; 
	private String name;
	public SimpleCache(String beanName) { name = beanName; }
	public void setCacheSize(int size) { cacheSize = size; }
	public void setTimeout(int ms) { ms = ms; }
	...
	public String toString() { return name; } //For convenience later
}
```

```java
@Aspect
@Component
public class PropertyChangeTracker {
	private Logger logger = Logger.getLogger(getClass());

	/*[Modifiers] ReturnType [ClassType] MethodName (Arguments) [throws ExceptionType]
	* matches one and only one
	.. matches zero or more arguments or packages
	*/
	@Before("execution(void set*(*)/*Join Point*/)")//Pointcut
	//Advice
	public void trackChange(JoinPoint point) {
		String methodName = point.getSignature().getName();
		Object newValue = point.getArgs()[0]; |
		logger.info(methodName + " about to change to " +
		newValue + "on" +
		point.getTarget());
	}
}
```
```java
@Configuration
@Aspect
@Import(AspectConfig.class)
public class MainConfig {
@Bean
public Cache cacheA() { return new SimpleCache(“cacheA”); }
@Bean
public Cache cacheB() { return new SimpleCache(“cacheB”); }
@Bean
public Cache cacheC() { return new SimpleCache(“cacheC”); }
}
```

```java
ApplicationContext context = SpringApplication.run(MainConfig.class);
@Autowired
@Qualifier("cacheA");
private Cache cache;
cache.setCacheSize(2500);

```

## Integration Testing
In unit testing there is no need of spring, the components will be tested in isolation.
We are going to use JUnit 5 framework. 

In the case of integration testing in specific you need Spring to wire components dependencies.

| Production Mode | Integration test |
|-----------------|------------------|
| Application     | ServiceTest      | 
| ServiceImpl     | ServiceImpl      | 
| Repo            | Repo             | 
| Production DB   | Test DB          | 

As of now, we were creating the application context in the test class in the @BeforeEach. This was not really efficient because it crated the application context before each method:
```java
    @BeforeEach 
    void setUp() {
        ApplicationContext context = SpringApplication.run(TestInfrastructureConfig.class);
        rewardNetwork= context.getBean(RewardNetwork.class);
    }
```
In Spring exists the TestContext framework which allows you to define an ApplicationContext for testing. If we create the application context this way, the application context won't be created for each test. it will be cached.

__@ContextConfiguration__ is used to define the configuration tu use.

__@ExtendWith__ is an annotation from the JUnit 5 (JUnit Jupiter) testing framework. It is used to register extensions that provide additional functionality to tests, such as lifecycle callbacks, dependency injection, and more.

```java
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes={SystemTestConfig.class})
public class TransferServiceTests {
    @Autowired
    private TransferService transferService;

    @Test
    public void shouldTransferMoneySuccessfully() {
        TransferConfirmation conf = transferService.transfer(...);
		...
    }
}
```

Usually those two annotations go together and instead of using both we can just use: __@SpringJUnitConfig__

```java
@SpringJUnitConfig(SystemTestConfig.class)
public class TransferServiceTests {
	// @Autowired
	//private TransferService transferService;

	@Test
	public void shouldTransferMoneySuccessfully(@Autowired TransferService transferService) {
		TransferConfirmation conf = transferService.transfer(...);
		...
	}
}
```

As the application context is cached. If you don't want one component change to impact in another test we can use __@DirtiesContext__. This will errase the app context after the execution of the method.
```java
	@Test
	@DirtiesContext
	public void shouldTransferMoneySuccessfully(@Autowired TransferService transferService) {
		...
	}
```
Also we can include properties in a test class.

```java
@SpringJUnitConfig(SystemTestConfig.class)
@TestPropertySource(properties = { "username=foo", "password=bar" },
		locations = "classpath:/transfer-test.properties")
public class TransferServiceTests {
}
```

## Testing with Profiles

Before we used to enable or disable beans at runtime by including the profiles in the configuration class. However that wont work in a test environment. To activate profiles in tes environments we can do it by using __@ActiveProfiles__

Only beans matching an active profile or with no profile are loaded.

For Annotation Cofiguration:

```java
// Component class
@Repository
@Profile("jdbc")
public class
JdbcAccountRepository {
}
```

```java
//Test Class
@SpringJUnitConfig(DevConfig.class)
@ActiveProfiles("jdbc")
public class TransferServiceTests
{..}
```

For Java Configuration:
```java
//Configuration Class
@Configuration
@Profile("jdbc")
public class DevConfig {
@Bean
public ... {...}
}

//OR
@Configuration
public class DevConfig {
	@Bean
	@Profile("jdbc")
	public ... {...}
}
```

```java
//Test Class
@SpringJUnitConfig(DevConfig.class)
@ActiveProfiles("jdbc")
public class TransferServiceTests
{..}
```

## Testing with Databases
@Sql is an annotation that will let you execute scripts against your datasource.

```java
@SpringJUnitConfig(...)
@Sql( { "Itestfiles/schema.sql", "ItestfiIeslload-data.sql"})
public class MainTests {
// schema.sql and load-data.sql only run before this test
@Test 
public void success(){ ... }
	
@Test // Overrides class sqls 
@Sql ( scripts="/testfiles/setupBadTransfer.sql" ) // Excutes before executing method
@Sql ( scripts="/testfiIeslcleanup.sql“,
		executionPhase=Sql.ExecutionPhase.AFTER_TEST_METHOD) // Excutes when defined in executionPhase
public void transferError() { ... }
}
```

## Database Access Layer

JDBC is a Java API that simplifies the way to interact with DBs. Spring simplifies the usage of that API by using JdbcTemplate.

To create the JDBC Template we just need the datasource:

```java
JdbcTemplate template = new JdbcTemplate(dataSource); 
```

To implement JDBC-Based Repository:
```java
public class JabcCustomerRrepository implements CusiomerRepository {
    private JdbcTemplate jdbcTemplate;

    public JdbcCustomerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
// For selects
    public int getCustomerCount() {
        String sql = "select count(*) from customer”;
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
	public int getCountOfNationalsOver(Nationality nationality, int age) {
        String sql = "select count(*) from PERSON " +
                "where age > ? and nationality = ?";
        return jdbcTemplate.queryForObject
                (sql, Integer.class, age, nationality.toString());
    }
//For Updates, inserts or deletes. It will return the number of affected rows.
	public int updateAge(Person person) {
    	return jdbcTemplate.update(
            "update PERSON set age = ? where id =?",
    		person.getAge(),
            person.getld());
	}
}
```
To retrive 1 row multiple columns queryForMap(..), queryForObject():
```java
//Returns Map { ID=1, FIRST_NAME="John", LAST_NAME="Doe" }
public Map<String,Object> getPersoninfo(int id) {
	String sql = "select * from PERSON where id=?";
	return jdbcTemplate.queryForMap(sql, id);
}
public Person getFerson(int id) {
    return jdbcTemplate.queryForObject(
            "select first_name, last_name from PERSON where id=?",
    (rs, rowNum) -> new Person(rs.getString("first_name"),
            rs.getString("last_name"))
            , id);
}
```

To retrive multiple rows and columns queryForList(..), query():
```java
//Returns 0- Map { ID=1, FIRST_NAME="John", LAST_NAME="Doe" }
// 0- Map { ID=2, FIRST_NAME="Alejandro", LAST_NAME="Ramos" }
public List<Map<String,Object>> getAllPersoninfo() {
	String sql = "select * from PERSON";
	return jdbcTemplate.queryForList(sql);
}

public List<Person> getAllPersons() { 
	return jdbcTemplate.query(
			"select first_name, last_name from PERSON", 
					(rs, rowNum) -> new Person(rs.getString("first_name"),
			rs.getString("last_name"))
);
```

Spring provides a ResultSetExtractor interface for processing an entire ResultSet at once

This is usefull when you join more than one table.

```java
public class JdbcOrderRepository {
	public Order findByConfirmationNumber(String number) {
		// Execute an outer join between order and item tables
		return jdbcTemplate.query(
				"select...from order o, item i...confirmation_id = ?",
				(ResultSetExtractor<Order>)(rs) -> {
                    Order order = null;
                    while (rs.next()) {
                        if (order == null)
                            g order = new Order(rs.getLong("ID"), rs.getString("NAME"), ...);
                        order.addltem(mapltem(rs));
                    }
                    return order;
                },
				number);
    }
}
```

## Transactions

### Management
PlatformTransactionManager is de main component to manage transactions.
First we need to define the PlatformTransactionManager in the configuration file:
```java
@Configuration
@EnableTransactionManagement
public class TxnConfig {
	@Bean
	public PlatformTransactionManager transactionManager(Datasource ds) {
		return new DataSourceTransactionManager(ds);
	}
}
```
Add the @Transactional annotation in the classes or methods.
```java
//@Transactional
public class RewardNetworklmpl implements RewardNetwork {
// To change any parameter you can update the properties in parenthesis.   
@Transactional (timeout=60)
public RewardConfirmation rewardAccountFor(Dining d) {
// atomic unit-of-work
}
}
```

### Propagation
When a transactional method calls another transactional method they both will be consider as a single transaction. However there are ways to split them in to separate transactions.
```java
//If the transaction was not yet started it creates one. If it was created it is considered on the same.
@Transactional(propagation=Propagation.REQUIRED)
//It doesn't matter whether or not transaction was created it will generate a new one.
@Transactional(propagation=Propagation.REQUIRES_NEW)
```
If two methods are on the same class they will be considered as same transaction independently of the propagation config.
As they are not in different proxies they will not go through the proxy.

### Rollbacks
Rollback is only executed if there is a RuntimeException. There are some DB Libraries that doesn't throw Runtime exception. In order to rollback exceptions different than RuntimeException you can configure them in the annotation.

```java
public class RewardNetworklmpl implements RewardNetwork {
@Transactional(rollbackFor=MyCheckedException.class,
noRollbackFor={JmxException.class, MailException.class})
public RewardConfirmation rewardAccountFor(Dining d) throws Exception {
...
}
}
```

### Usage in test
You can configure a test class or a method on a test class as transactional just by using: @Transactional.
In that case all involved method will be executed as a transaction that will always by rollback at the end. 
If you want to commit any of the operations you can put @Commit in the method that you want to commit.
```java
@SpringJUnitConfig(RewardsConfig.class)
@Transactional
public class RewardNetworkTest {
@Test
@Commit 
public void testRewardAccountFor() {
... // Whatever happens here will be committed
}
}
```

# SpringBoot

Springboot will take care of all the dependencies of your project so you don't have to include them in your POM.xml file.
For that you can use springboot parent or starter.

This is the configuration springboot parent:
```xml
<parent>
	<groupld>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-parent</artifactId>
	<version>2.7.5</version>
</parent>
```
This would be the configuration for Springboot starter Dependencies.
```xml
<dependencies>
	<dependency>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter</artifactId>
	</dependency>
</dependencies> 
```

You have different starters depending on the kind of things you need:
- spring-boot-starter-jdbc(Database libraries)
- spring-boot-starter-data-jpa (ORM API)
- spring-boot-starter-web (web apps)
- spring-boot-starter-batch

Also Springboot will handle the Dependency injection by autoconfiguring the application.
For that we use : @EnableAutoConfiguration.

```java
@SpringBootConfiguration
@ComponentScan
@EnableAutoConfiguration
public class Application {
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
```
All those annotation can be conbined as:
```java
@SpringBootApplication
(scanBasePackages="example.config")
public class Application {
}
```

SpringBoot allows you to package your application as Jar file or a container for Kubernetes. 

For this you will need the plugin inside of your pom file as well:
```xml
<build>
	<plugins>
		<plugin>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-maven-plugin</artifactId>
		</plugin>
	</plugins>
</build>
```

The command will be:gradle assemble Or mvn package

## Integration Testing with SpringBoot.

__@SpringBootTest__ alternative to @SpringJUnitConfig:

```java
//You define your application entry point. You can commit application entry point.
// It uses the one you set as @SpringBootConfiguration / @SpringBootApplication
@SpringBootTest(classes=Application.class)
public class TransferServiceTests {
    @Autowired
    private TransferService transferService;

    @Test
    public void successfulTransfer() {
        TransferConfirmation conf = transferService.trapsfer(...);
		...
    }
}
```
## Hello World example

Just three files to get a running Spring application:
- pom.xml: Setup Spring Boot (and any other) dependencies
```xml
<parent>
	<groupId>org.springframework.boot</groipId>
	<artifactId>spring-boot-starter-parent</artifactId>
	<version>2.7.5</version>
</parent>
<dependencies>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-jdbc</artifactId>
		</dependency>
		<dependency>
			<groupId>org.hsqldb</groupId>
			<artifactId>hsqldb</artifactId>
		</dependency>
</dependencies>
```

- application.properties: General configuration

```properties
# Set the log level for all modules to ‘ERROR’
logging. level. root=ERROR
# Tell Spring JDBC Embedded DB Factory where
# to obtain DDM and DML files
spring.sql.init.schema-locations=classpath:rewards/schema.sql
spring.sql.init.data-locations=classpath:rewards/data.sql
```

- Application class: Application launcher

```java
// Config class, doing component scanning and enabling auto configuration
@SpringBootApplication
public class Application {
	public static final String QUERY = "SELECT count(*) FROM T_ACCOUNT
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
	@Bean
	CommandLineRunner commandLineRunner(JdbcTemplate jdbcTemplate){
		return args -> System.out.println("Hello, there are "
				+ jdbcTemplate.queryForObject( SQL, Long.class)
				+" accounts");
	}
}
```

## Properties

### application.properties
You can create one porperties file per profile (dev, uat, cloud)
```properties
db.driver=org.postgresql.Driver
db.url=jdbc:postgresql://localhost/transfer
db.user=transfer-app
db.password=secret45
```

### application.yml

Don't use tabs, not allowed. You can put more than one profile in the same file.

```yaml
spring.datasource:
driver: org.postgresql.Driver
username: transfer-app
## Profile separator ---
---
spring: 
  profiles: local
  datasource:
    url: jdbc:postgresql://localhost/xfer
    password: secret45
---
spring:
  profiles: cloud
  datasource:
   url: jdbc:postgresql://prod/xfer
   password: secret45
```

## Mapping properties into classes

First you can map the different properties into a class:
Assuming this is you application.properties
```properties
rewards.client.host=192.168.1.42
rewards.client.port=8080
rewards.client.logdir=/logs
rewards.client. timeout=2000
```

```java
@ConfigurationProperties(prefix="rewards.client")
public class ConnectionSettings {
	private String host;
	private int port;
	private String logdir;
	private int timeout;
	... // getters/setters
}
```

There are three options to ingest those properties:

```java
@SpringBootApplication
@EnableConfigurationProperties(ConnectionSettings.class)
public class RewardsApplication { .. }
```
```java
@SpringBootApplication
//This is like component scanning.
@ConfigurationPropertiesScan
public class RewardsApplication { .. }
```
```java
@Component
@ConfigurationProperties(prefix="rewards.client")
public class ConnectionSettings { .. }
```

## Autoconfiguration

It is enabled by using: @SpringBootApplication or @EnableAutoConfiguration.

How it works is that Spring provides @Configuration classes with conditions.

Conditions include
- Do classpath contents include specific classes?
- Are some properties set?
- Are some beans already configured (or not configured)?

AutoConfiguration classes are specified inside the external libraries:

 - spring-boot-autoconfigure/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports

### Override Configuration

There different ways to override the configuration:

#### Set SpringBoot properties
```properties
## application.properties
##This override the default properties of springboot for a datasource 
spring.datasource.url=jdbc:mysql://localhost/test
spring.datasource.username=dbuser
spring.datasource.password=dbpass
spring.datasource.driver-class-name=com.mysql. jdbc.Driver

spring.sql.init.schema-locations=classpath:/testdb/schema.sql
spring.sql.init.data-locations=classpath:/testdb/data.sql
```

```properties
## application.properties
##This override the default properties of springboot for log level
logging.level.org.springframework=DEBUG
logging.level.com.acme.your.code=INFO

```

#### Explicitly define beans yourself so Spring Boot won't

```java
@Bean
public DataSource dataSource() {
    return new EmbeddedDatabaseBuilder().
            setName("RewardsDb").build();
}
```
#### Explicitly disable some auto-configuration
There are two ways:
- In the @EnableAutoConfiguration annotation
```java
// The class can be found in the file mentiones in Autoconfiguraion section
@EnableAutoConfiguration(exclude=DataSourceAutoConfiguration.class)
```
- In the application.properties
```properties
spring.autoconfigure.exclude=\ 
org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
```
#### Change dependencies or their versions

- Override dependency version in your pom.xml or build.gradle:
```xml
<properties>
<spring-framework.version>5.3.22</spring-framework.version>
</properties>
```

- Explicitly Substitute Dependencies:
```xml
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-web</artifactId>
	// Exclude Tomcat
	<exclusions>
		<exclusion>
			<groupIld>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-tomcat</artifactId>
		</exclusion>
	</exclusions>
</dependency>
// Use Jetty
<dependency>
<groupId>org.springframework.boot</groupId>
<artifactId>spring-boot-starter-jetty</artifactId>
</dependency>
```

## Running an Application
These are commands that are run right after creating the application context.
- CommandLineRunner: Offers run () method, handling arguments as an array
- ApplicationRunner: Offers run () method, handling arguments as ApplicationArguments

## Introduction to JPA

JPA is Java Persistent API. JPA comunicates our application with the DB. JPA makes use of different providers: Hibernate,
EclipseLink, Toplink. JPA makes different mappings using annotations.

 - @Entity - Marks class as a JPA persistent class.
 - @Table - Specifies the exact table name to use on the DB (would be "Account" if unspecified).
 - @Id - Indicates the field to use as the primary key on the database.
 - @Column - Identifies column-level customization, such as the exact name of the column on the table.
 - @OneToMany - Identifies the field on the 'one' side of a one to many relationship.
 - @JoinColumn - Identifies the column on the 'many' table containing the column to be used when joining. Usually a foreign key.

### Persistence Unit
A persistence unit defines a set of all entity classes managed by EntityManager instances within an application.
These entity classes represent the data contained within a single data store (usually a database).
In other words, a persistence unit groups together related entities that share the same database connection and configuration.
The configuration for a persistence unit is typically specified in the persistence.xml configuration file.
 - __Purpose of Persistence Units__:
   - Organizing Entities: By grouping related entities, a persistence unit simplifies the management of their lifecycle and interactions with the database.
   - Configuration: It allows you to define properties such as the database connection details, JPA provider, and other settings. 
   - Scoping: Different persistence units can have different configurations, allowing you to manage multiple data sources or databases within a single application.

### EntityManagerFactory
In the context of Java Persistence API (JPA), the EntityManagerFactory is an interface used to interact with the factory of EntityManager instances associated with a persistence unit. Here are the key points:

 - Creating an EntityManagerFactory:
 	- The factory is created by invoking the method createEntityManagerFactory(String persistenceUnitName).
    - The persistenceUnitName argument corresponds to the name of the persistence unit defined in the persistence.xml file.
    - The factory manages all entities defined in the file.
    - Each JEE application can have multiple EntityManagerFactory instances, each tied to a different PersistenceUnit.
 - Closing the Factory:
    Properly closing the factory releases resources.
    When closed, all associated EntityManager instances are also considered closed.

### Repositories
A repository in JPA provides a consistent way to interact with your data, abstracting away the complexities of database operations.
For Creating a repository:
Creating a Repository:
 - To define a repository interface, you typically create an interface that extends one of the Spring Data repository interfaces.
 - For example, if you want basic CRUD methods, extend CrudRepository. There are other repositories in Spring data such as: PagingAndSortingRepository,JpaRepository, ReactiveCrudRepository 
 - You can also selectively expose methods by copying them from the CRUD repository into your domain-specific repository.
 - If many repositories share the same set of methods, create a custom base interface annotated with @NoRepositoryBean.

## Spring Data JPA

1. Define the necesary dependency:
```xml
<dependencies>
	<dependency>
		<groupld>org.springframework.boot</groupld>
		<artifactld>spring-boot-starter-data-jpa</artifactid>
	</dependency>
</dependencies>
```
Springboot will autoconfigure a Datasource, EntityManagerFactoryBean (can be customized) and a JpaTransactionManager(can be customized).

2. Customize EntityManagerFactoryBean 
   1. Entity Locations
   
   By default entities are searched in the same package where there is @EnableAutoConfiguration annotation.
	If we want to edit where to look for the entities you can use @EntityScan and the name of the package:
	```Java
	@SpringBootApplication
	@EntityScan("rewards.internal")
	public class Application {
		//..
	}
	```
   2. Configuration Properties

	You can set some properties that were usually set in the persistence.xml configuration file.
	```properties
	# Leave blank - Spring Boot will try to select dialect for you
	# Set to 'default' - Hibernate will try to determine it
	spring. jpa.database=default
	# Create tables automatically? Default is:
	# Embedded database: create-drop
	# Any other database: none (do nothing)
	# Options: validate | update | create | create-drop
	spring. jpa.hibernate.ddl-auto=update
	# Show SQL being run (nicely formatted)
	spring. jpa.show-sql=true
	spring. jpa.properties.hibernate.format_sql=true
	# Any hibernate property 'xxx'
	spring. jpa.properties.hibernate.xxx=????
   ```

3. Annotate JPA domain class (Entity)
	
	```Java
	@Entity
	@Table(...)
	public class Customer {
	    @ld
	    @GeneratedValue(strategy = GenerationType.AUTO)
	    private Long id;
	    private Date orderDate;
	    private String email;
 		
 		// You are associating the beneficiaries to the Customer. The Account_id is a column of the beneficiaries entity	
 		@OneToMany
		@JoinColumn(name = "ACCOUNT_ID")
		private Set<Beneficiary> beneficiaries = new HashSet<Beneficiary>();
 		//Here you are associating the column ALLOCATION_PERCENTAGE to the property value of the class Percentage
 		@AttributeOverride(name="value",column=@Column(name="ALLOCATION_PERCENTAGE"))
		private Percentage allocationPercentage;
		// this annotation is for fields you dont want to link with the DB
		@Transient
		private String personalValue;
	    
	// Other data-members and getters and setters omitted
	}
	```
	
4. Choose a repository to extend

	For example the CrudRepository interface is like: 
	```Java
	public interface CrudRepository<T, ID extends Serializable>
			extends Repository<T, ID>{
		public long count();
		public <S extends T> S save(S entity);
		public <S extends T> Iterable<S> save(lterable<S> entities);
		public Optional<T> findByld(ID id);
		public Iterable<T> findAll();
		public Iterable<T> findAlIByld(lterable<ID> ids);
		public void deleteAll(Iterable<? extends T> entities);
		public void delete( T entity);
		public void deleteByld(ID id);
		public void deleteAll();
	}
	```
	PagingAndSortingRepository<T, K>
	- adds Iterable<T> findAll (Sort)
	- adds Page<T> findAll (Pageable)

	```Java
	public interface CustomerRepository extends CrudRepository<Customer, Long> {
		// find(First)By<DataMemberOfClass><Op>
 		// <Op> can be GreaterThan, NotEquals, Between, Like ...
 		public Customer findFirstByEmail(String someEmail); // No <Op> for Equals
		public List<Customer> findByOrderDateLessThan(Date someDate);
		public List<Customer> findByOrderDateBetween(Date d1, Date d2);
 	
 		// Custom query uses query language
		@Query("SELECT c FROM Customer c WHERE c.email NOT LIKE '%@%")
		public List<Customer> findInvalidEmails(); 
	}
	```

5. Finding your repositories 

	```Java
	@Configuration
	@EnableJpaRepositories(basePackages="com.acme.repository")
	public class CustomerConfig {
		@Bean 
		public CustomerService customerService(CustomerRepository repo) {
		return new CustomerService( repo );
		}
	}
	```
## Web Application with Springboot
1. Define the necesary dependency:
```xml
<dependencies>
	<dependency>
		<groupld>org.springframework.boot</groupld>
		<artifactld>spring-boot-starter-web</artifactld>
	</dependency>
</dependencies>
```
2. Controller implementation

	@Controller is an @Component so will be found by component-scanner
	```java
	@Controller
	public class AccountController {
	    // http://localhost:8080/accounts
	    @GetMapping("/accounts")
	    public @ResponseBody List<Account> list() {...}
	}
	```
	
	An equivalent to the previous:
	
	```java
	// Incorporates @Controller and @ResponseBody
	@RestController
	public class AccountController {
	    // http://localhost:8080/accounts/userid=1234
	    @GetMapping("/accounts")
		// @RequestParam extracts request parameters from the request URL 
		// and performs type conversion
	    public List<Account> list(@RequestParam("userid") int userid) {...}
	}
	```
	
	```java
	// Incorporates @Controller and @ResponseBody
	@RestController
	public class AccountController {
	    // http://localhost:8080/accounts/98765
		@GetMapping("/accounts/{accountid}")
		public Account find(@PathVariable("accountld") long id) {...}

 		//http://.../orders/1234/item/2
		@GetMapping("/orders/{id}/items/{itemld}")
		public Orderltem item(@PathVariable("id") long orderId, 
		@PathVariable int itemld, 
 		// the following will be taken from header.
		Locale locale, 
		@RequestHeader("user-agent") String agent) {...}

 		//http://.../suppliers?location=12345
		@GetMapping("/suppliers")
		public List<Supplier> getSuppliers(
 		// using Integer (the primitive) in case we don't receive any value
 		// to be able to get null values.
 		@RequestParam(required=false) Integer location,
		Principal user,
		HttpSession session ){...}
	}
	```
 
3. Message Converters

	By default, when you use @ResponseBody, remember it is included on the @RestController the message controller will be 
	the one responsable for converting the response to whichever format is requested by the client.
	
	In case we wanted to edit the response headers we could use ResponseEntity. it's body will also be handled by the
	MessageController.
	
	```java
	@GetMapping ("/store/orders/{id}")
	public ResponseEntity<Order> getOrder (@PathVariable long id) {
		Order order = orderService.find (id);
		return ResponseEntity 
				.ok()
				.lastModified (order.lastUpdated())
				.body (order);
	}
	```
 
4. Setting up the properties.

	In application.properties you can edit some properties of web server.
	
	```properties
	server.port=8080
	server.servlet.session.timeout=5m
	```
 
## RESTful Application with Springboot

Before we used @GetMapping now we will use @RequestMapping.

```java
@RequestMapping (path="/store/orders/{id}",
		method=RequestMethod.GET)
public ResponseEntity<Order> getOrder (@PathVariable long id) {
	Order order = orderService.find (id);
	return ResponseEntity
			.ok()
			.lastModified (order.lastUpdated())
			.body (order);
}
```

### GET Requests

```java
@GetMapping ("/store/orders/{id}")
public ResponseEntity<Order> getOrder (@PathVariable long id) {
	Order order = orderService.find (id);
	return ResponseEntity 
			.ok()
			.lastModified (order.lastUpdated())
			.body (order);
}
```

### PUT Requests (Update)

```java
@PutMapping("/store/orders/{orderld}/items/{itemId}")
@ResponseStatus(HttpStatus.NO_CONTENT) // 204
public void updateltem(@PathVariable long orderld,
					   @PathVariable String itemld,
					   @RequestBody Item item) {
	orderService.findOrderByld(orderld).updateltem(itemld, item);
}
```

### POST Requests (Create)

```java
// Example: POST http://server/store/orders/12345/items
@PostMapping("/store/orders/{id}/items")
public ResponseEntity<Void> createltem
		(@PathVariable long id, @RequestBody Item newItem) {
	// Add the new item to the order
	orderService.findOrderByld(id).addItem(newItem);

	// Build the location URI of the newly added item
	URI location = ServletUriComponentsBuilder
			.fromCurrentRequestUri()
			.path("{itemid}")
			.buildAndExpand(newltem.getId())
			.toUri();
	//Explicitly create a 201 Created response with Location harder set
	return ResponseEntity.created(location).build();
	// Return http://server/store/orders/12345/items/item%20A
}
```

### DELETE Requests

```java
@DeleteMapping("/store/orders/{orderld}/items/{itemld}")
@ResponseStatus(HttpStatus. NO_CONTENT) // 204 
public void deleteltem(@PathVariable long orderId, 
					   @PathVariable String itemId) { 
	orderService.findOrderByld(orderId).deleteltem(itemlId); 
} 
```

### RestTemplate
| HTTP Method | RestTemplate Method |
|-------------|---------------------|
| GET         | `getForObject(String url, Class<T> responseType)` |
| DELETE      | `delete(String url, Object... uriVariables)` |
| HEAD        | `headForHeaders(String url, Object... uriVariables)` |
| OPTIONS     | `optionsForAllow(String url, Object... uriVariables)` |
| POST        | `postForObject(String url, Object request, Class<T> responseType)` |
| PUT         | `put(String url,Object request,Object... uriVariables)` |
| PATCH       | `patchForObject(String url,Object request,Object... uriVariables)` |


```java
RestTemplate template = new RestTemplate();
String uri = "http://example.com/store/orders/{id}/items";

// GET all order items for an existing order with ID 1:
Orderltem[] items =
		template.getForObject(uri, OrderItem[].class, "1");

// POST to create a new item
OrderItem item = // create item object
		URI itemLocation = template.postForLocation(uri, item, "1");

// PUT to update the item
item.setAmount(2);
template.put(itemLocation, item);

// DELETE to remove that item again
template.delete(itemLocation);
```

### ResponseEntity

```java
String uri = "http://example.com/store/orders/{id}";

ResponseEntity<Order> response =
		restTemplate.getForEntity(uri, Order.class, "1");

assert(response.getStatusCode().equals(HttpStatus.OK));

long modified = response.getHeaders().getLastModified();

Order order = response.getBody();
```

### RequestEntity

```java
// Create RequestEntity - POST with HTTP BASIC authentication
RequestEntity<Orderltem> request = RequestEntity
		.post(new URI(itemUrl))
		.getHeaders().add(HttpHeaders.AUTHORIZATION,
				"Basic "+ getBase64EncodedLoginData())
		.contentType(MediaType.APPLICATION_JSON)
		.body(newltem);

// Send the request and receive the response
ResponseEntity<Void> response =
		restTemplate.exchange(request, Void.class);

assert(response.getStatusCode().equals(HttpStatus.CREATED));

```

## SpringBoot Testing

1. Dependency

```xml
<dependency>
<groupId>org.springframework.boot</groupId>
<artifactId>spring-boot-starter-test</artifactId>
<scope>test</scope>
</dependency>
```

### Springboot testing

```java
// Automatically searches for classes with @SpringBootConfiguration
// Auto-configures TestRestTemplate
// Provides support for different webEnvironment modes:
// RANDOM PORT, DEFINED_ PORT, MOCK, NONE
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class AccountClientBootTests {

    @Autowired
    private TestRestTemplate restTemplate;

	@Test
	public void addAndDeleteBeneficiary() {
        String addUrl = "/accounts/{accountld}/beneficiaries";
        URI newBeneficiaryLocation = restTemplate.postForLocation(addUrl, "David", 1);
        Beneficiary newBeneficiary
                = restTemplate.getForObject(newBeneficiaryLocation, Beneficiary.class);
        assertThat(newBeneficiary.getName()).isEqualTo("David");

        restTemplate.delete(newBeneficiaryLocation);

        ResponseEntity<Beneficiary> response
                = restTemplate.getForEntity(newBeneficiaryLocation, Beneficiary.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
```

### Spring MVC Testing

Before we were testing using integration testing and starting the Server conatiner. With Spring MVC the server
container is not started.
Here we wont have the restTemplate

```java
// Automatically searches for classes with @SpringBootConfiguration
// Auto-configures TestRestTemplate
// Provides support for different webEnvironment modes:
// RANDOM PORT, DEFINED_ PORT, MOCK, NONE
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders .*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers .*;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
public final class AccountControllerTests {

    @Autowired
    MockMvc mockMvc;

    @Test
    public void testBasicGet() {
        mockMvc.perform(
                // MockHttpServletRequestBuilder
                get("/accounts"))
				.andDo(print())
				// MockMVCResultMatchers
                .andExpect(status().isOk());
    }
	@Test
	public void testBasicPut() {
		mockMvc.perform(put("/accounts/{acctld}","123456001")
				.content("{...}")
				.contentType( "application/json"));
	}
}
```

#### MockHttpServletRequestBuilder

| Method       | Description |
|--------------|-------------|
| param        | Add a request parameter – such as param("myParam", 123) |
| requestAttr  | Add an object as a request attribute. Also, sessionAttr does the same for session scoped objects |
| header       | Add a header variable to the request. Also see headers, which adds multiple headers |
| content      | Request body |
| contentType  | Set the content type (Mime type) for the body of the expected response |
| locale       | Set the location for making requests |


#### MockMVCResultMatchers

| Method  | Matcher Returned     | Description                                  |
|---------|----------------------|----------------------------------------------|
| content | ContentResultMatchers | Assertions relating to the HTTP response body |
| header  | HeaderResultMatchers  | Assertions on the HTTP headers               |
| status  | StatusResultMatchers  | Assertions on the HTTP status                |
| xpath   |                      | Search returned XML using XPath expression   |
| jsonPath|                      | Search returned JSON using JsonPath          |


### Slice Testing

Performs isolated testing within a slice of an application
 - Web slice
 - Repository slice
 - Caching slice

Dependencies need to be mocked

For this we will use @WebMVCTest and get just the configuration that will be tested. In the case of testing the web
layer we will just get the controller.

```java
// we get the controller because we just want to test that web layer.
@WebMvcTest(AccountController.class)
public class AccountControllerBootTests {

	@Autowired
	private MockMvc mockMvc;

    //This mock the account manager into the application context.
	@MockBean
	private AccountManager accountManager;

	@Test
	public void testHandleDetailsRequest() throws Exception {

		// Define the behavior of our mock
		given(accountManager.getAccount(OL))
				.willReturn(new Account("1234567890", "John Doe"));

		// Make the test
		mockMvc.perform(get("/accounts/0"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON)
						.andExpect(jsonPath("name").value("John Doe"))
						.andExpect(jsonPath("number").value("1234567890"));

		// verify that our mock has been called somewhere in the code
		verify(accountManager).getAccount(0L);
	}

}
```

## Securing REST Application with Spring Security

### Security concepts

 - __Principal__: User, device or system that performs an action
 - __Authentication__: Establishing that a principal's credentials are valid. Examples: Basic, Digest, Form, X.509, OAuth 2.0 / OIDC
 - __Authorization__: Deciding if a principal is allowed to access a resource
 - __Authority__: Permission or credential enabling access (such as a role)
 - __Secured Resource__: Resource that is being secured

Dependency
```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
```
```java
@SpringBootApplication
@Import(RestSecurityConfig.class)
@EntityScan("rewards.internal")
public class RestWsApplication {..}
```
### Setup and Configuration

1. Setup Filter chain

```java

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests((authz) -> authz
				// - Allow DELETE on the /accounts resource (or any sub-resource)
				//   for "SUPERADMIN" role only
				.requestMatchers(HttpMethod.DELETE,"/accounts/**")
						.hasRole("SUPERADMIN")
				// - Allow POST or PUT on the /accounts resource (or any sub-resource)
				//   for "ADMIN" or "SUPERADMIN" role only
				.requestMatchers(HttpMethod.POST,"/accounts/**")
						.hasAnyRole("SUPERADMIN","ADMIN")
				.requestMatchers(HttpMethod.PUT,"/accounts/**")
						.hasAnyRole("SUPERADMIN","ADMIN")
				// - Allow GET on the /accounts resource (or any sub-resource)
				//   for all roles - "USER", "ADMIN", "SUPERADMIN"
				.requestMatchers(HttpMethod.GET,"/accounts/**")
						.hasAnyRole("SUPERADMIN","ADMIN","USER")
				// - Allow GET on the /authorities resource
				//   for all roles - "USER", "ADMIN", "SUPERADMIN"
				.requestMatchers(HttpMethod.GET,"/authorities")
						.hasAnyRole("SUPERADMIN","ADMIN","USER")
				// Deny any request that doesn't match any authorization rule
				.anyRequest().denyAll())
			.httpBasic(withDefaults())
			.csrf(CsrfConfigurer::disable);


		return http.build();
	}
	@Bean
	public InMemoryUserDetailsManager userDetailsService() {
		//- "user"/"user" with "USER" role
		UserDetails user =
				User.withUsername("user").password(passwordEncoder.encode("user"))
						.roles("USER").build();
		// - "admin"/"admin" with "USER" and "ADMIN" roles
		UserDetails admin =
				User.withUsername("admin").password(passwordEncoder.encode("admin"))
						.roles("ADMIN", "USER")
						.build();
		// - "superadmin"/"superadmin" with "USER", "ADMIN", and "SUPERADMIN" roles
		UserDetails superadmin =
				User.withUsername("superadmin").password(passwordEncoder.encode("superadmin"))
						.roles("SUPERADMIN", "ADMIN", "USER")
						.build();

		return new InMemoryUserDetailsManager(user, admin, superadmin);
    }
}
```
Perform security testing against MVC layer
```java
@WebMvcTest(AccountController.class)
@ContextConfiguration(classes = {RestWsApplication.class, RestSecurityConfig.class})
public class AccountControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountManager accountManager;

    @MockBean
    private AccountService accountService;

    @Test
    @WithMockUser(roles = {"INVALID"})
    void accountSummary_with_invalid_role_should_return_403() throws Exception {

        mockMvc.perform(get("/accounts"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "superadmin", password = "superadmin")
    public void accountDetails_with_superadmin_credentials_should_return_200() throws Exception {

        // arrange
        given(accountManager.getAccount(0L)).willReturn(new Account("1234567890", "John Doe"));

        // act and assert
        mockMvc.perform(get("/accounts/0"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("name").value("John Doe"))
                .andExpect(jsonPath("number").value("1234567890"));

        // verify
        verify(accountManager).getAccount(0L);

    }
}
```

Perform security testing against a running server
```java
@SpringBootTest(classes = {RestWsApplication.class},
		webEnvironment = WebEnvironment.RANDOM_PORT)
public class AccountClientTests {

    @Autowired
    private TestRestTemplate restTemplate;

    private Random random = new Random();

    @Test
    public void listAccounts_using_invalid_user_should_return_401() throws Exception {
        ResponseEntity<String> responseEntity
                = restTemplate.withBasicAuth("invalid", "invalid")
                .getForEntity("/accounts", String.class);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void createAccount_using_user_should_return_403() throws Exception {

        String url = "/accounts";
        // use a unique number to avoid conflicts
        String number = String.format("12345%4d", random.nextInt(10000));
        Account account = new Account(number, "John Doe");
        account.addBeneficiary("Jane Doe");
        ResponseEntity<Void> responseEntity
                = restTemplate.withBasicAuth("user", "user")
                .postForEntity(url, account, Void.class);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    }
}
    
```

Secure a Method.

```java
@Service
public class AccountService {
   //logged-in user belongs to "ADMIN" role and the username == principal.username =authentication.name
	@PreAuthorize("hasRole('ADMIN') && #username == principal.username")
	public List<String> getAuthoritiesForUser(String username) {

		//Retrieve authorities (roles) for the logged-in user
		Collection<? extends GrantedAuthority> grantedAuthorities
				= SecurityContextHolder.getContext()
				.getAuthentication().getAuthorities(); // Modify this line

		return grantedAuthorities.stream()
				.map(GrantedAuthority::getAuthority)
				.collect(Collectors.toList());
	}

}
    
```

2. Configure security (authorization) rules
3. Setup Web Authentication