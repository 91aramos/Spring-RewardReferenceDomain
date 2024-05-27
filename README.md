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

Contains a group of Beans and usually represents each environment. Ptofiles can be defined at class level or bean/method level.

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
	@AfterThrowing(value=“execution(* *..Repository.*(..))”, throwing=‘“e”)
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
	@Before(“execution(void set*(*)/*Join Point*/)”)//Pointcut
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

