package dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.support.SimpleTransactionStatus

@SpringJUnitConfig(TransactionalPersistenceHelperTest.TestConfig::class)
class TransactionalPersistenceHelperTest {
    @Autowired
    private lateinit var helper: TransactionalPersistenceHelper

    @Autowired
    private lateinit var transactionManager: RecordingTransactionManager

    @BeforeEach
    fun resetTransactionManager() {
        transactionManager.reset()
    }

    @Test
    fun `helper bean is proxied for transactional interception`() {
        AopUtils.isAopProxy(helper) shouldBe true
    }

    @Test
    fun `executeInTransaction commits and returns block result`() {
        val result = helper.executeInTransaction { 42 }

        result shouldBe 42
        transactionManager.beginCalls shouldBe 1
        transactionManager.commitCalls shouldBe 1
        transactionManager.rollbackCalls shouldBe 0
    }

    @Test
    fun `executeInTransaction rolls back when block throws`() {
        val error = shouldThrow<IllegalStateException> {
            helper.executeInTransaction<Int> { throw IllegalStateException("boom") }
        }

        error.message shouldBe "boom"
        transactionManager.beginCalls shouldBe 1
        transactionManager.commitCalls shouldBe 0
        transactionManager.rollbackCalls shouldBe 1
    }

    @Configuration
    @EnableTransactionManagement
    class TestConfig {
        @Bean
        fun transactionalPersistenceHelper(): TransactionalPersistenceHelper = TransactionalPersistenceHelper()

        @Bean
        fun transactionManager(): RecordingTransactionManager = RecordingTransactionManager()
    }

    class RecordingTransactionManager : PlatformTransactionManager {
        var beginCalls: Int = 0
            private set
        var commitCalls: Int = 0
            private set
        var rollbackCalls: Int = 0
            private set

        override fun getTransaction(definition: TransactionDefinition?): TransactionStatus {
            beginCalls++
            return SimpleTransactionStatus()
        }

        override fun commit(status: TransactionStatus) {
            commitCalls++
        }

        override fun rollback(status: TransactionStatus) {
            rollbackCalls++
        }

        fun reset() {
            beginCalls = 0
            commitCalls = 0
            rollbackCalls = 0
        }
    }
}
