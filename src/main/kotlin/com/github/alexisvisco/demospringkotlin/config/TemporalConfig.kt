package com.github.alexisvisco.demospringkotlin.config

import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowClientOptions
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.serviceclient.WorkflowServiceStubsOptions
import com.github.alexisvisco.demospringkotlin.temporal.email.EmailSenderActivityImpl
import com.github.alexisvisco.demospringkotlin.temporal.email.EmailSenderWorkflowImpl
import com.github.alexisvisco.demospringkotlin.temporal.imagevariant.ImageProcessorActivityImpl
import com.github.alexisvisco.demospringkotlin.temporal.imagevariant.ImageVariantWorkflowImpl
import io.temporal.worker.Worker
import io.temporal.worker.WorkerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.slf4j.LoggerFactory

@Configuration
class TemporalConfig(
    @Lazy private val emailVerificationActivityImpl: EmailSenderActivityImpl,
    @Lazy private val imageProcessorActivityImpl: ImageProcessorActivityImpl
) {

    companion object {
        private val logger = LoggerFactory.getLogger(TemporalConfig::class.java)
    }

    @Value("\${spring.temporal.connection.target}")
    private lateinit var connectionTarget: String

    @Value("\${spring.temporal.namespace}")
    private lateinit var temporalNamespace: String

    @Value("\${spring.temporal.task-queue}")
    private lateinit var taskQueue: String

    @Bean
    fun workflowServiceStubs(): WorkflowServiceStubs {
        val options = WorkflowServiceStubsOptions.newBuilder()
            .setTarget(connectionTarget)
            .build()

        return WorkflowServiceStubs.newServiceStubs(options)
    }

    @Bean
    fun workflowClient(workflowServiceStubs: WorkflowServiceStubs): WorkflowClient {
        val options = WorkflowClientOptions.newBuilder()
            .setNamespace(temporalNamespace)
            .build()

        return WorkflowClient.newInstance(workflowServiceStubs, options)
    }

    @Bean
    fun workerFactory(workflowClient: WorkflowClient): WorkerFactory {
        return WorkerFactory.newInstance(workflowClient)
    }

    @Bean
    fun worker(workerFactory: WorkerFactory): Worker {
        val worker = workerFactory.newWorker(taskQueue)

        worker.registerWorkflowImplementationTypes(EmailSenderWorkflowImpl::class.java)
        worker.registerActivitiesImplementations(emailVerificationActivityImpl)

        worker.registerWorkflowImplementationTypes(ImageVariantWorkflowImpl::class.java)
        worker.registerActivitiesImplementations(imageProcessorActivityImpl)
        
        return worker
    }

    @Bean
    fun temporalWorkerStarter(workerFactory: WorkerFactory): org.springframework.boot.ApplicationRunner {
        return org.springframework.boot.ApplicationRunner {
            workerFactory.start()
        }
    }
}
