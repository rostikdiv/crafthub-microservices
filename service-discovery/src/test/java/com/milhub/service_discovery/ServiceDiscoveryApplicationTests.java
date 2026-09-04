package com.milhub.service_discovery;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ServiceDiscoveryApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void mainMethodRuns() {
		ServiceDiscoveryApplication.main(new String[]{"--spring.profiles.active=test", "--server.port=0"});
	}

}

