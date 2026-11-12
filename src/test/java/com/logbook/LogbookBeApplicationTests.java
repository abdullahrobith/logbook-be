package com.logbook;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "logbook.jadwal.aktif=false")
class LogbookBeApplicationTests {

	@Test
	void contextLoads() {
	}

}
