package dev.remsely.f1goatdeterminer.datasync.app

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@Import(TestcontainersConfig::class)
@ActiveProfiles("test")
class DataSyncApplicationTests {
    @Test
    fun contextLoads() {
        // Application is not crushed
    }
}
