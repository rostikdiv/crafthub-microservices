package com.milhub.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ApiGatewayApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void testApplicationInstantiation() {
		ApiGatewayApplication app = new ApiGatewayApplication();
		org.junit.jupiter.api.Assertions.assertNotNull(app);
	}
}
