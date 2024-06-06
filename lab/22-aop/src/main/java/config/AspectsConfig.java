package config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import org.springframework.context.annotation.EnableAspectJAutoProxy;
import rewards.internal.monitor.MonitorFactory;
import rewards.internal.monitor.jamon.JamonMonitorFactory;
@EnableAspectJAutoProxy
@ComponentScan("rewards.internal.aspects")
@Configuration
public class AspectsConfig {

	@Bean
	public MonitorFactory monitorFactory(){
		return new JamonMonitorFactory();
	}
	
}
