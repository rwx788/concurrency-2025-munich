import org.jetbrains.lincheck.datastructures.*
import org.junit.*
import org.junit.runners.*
import kotlin.reflect.*

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
abstract class TestBase(
    val sequentialSpecification: KClass<*>,
    val checkObstructionFreedom: Boolean = true,
    val scenarios: Int = 150,
    val threads: Int = 3,
    val actorsBefore: Int = 1
) {
    @Test
    open fun modelCheckingTest() = ModelCheckingOptions()
        .iterations(scenarios)
        .invocationsPerIteration(1_000)
        .actorsBefore(actorsBefore)
        .threads(threads)
        .actorsPerThread(2)
        .actorsAfter(0)
        .sequentialSpecification(sequentialSpecification.java)
        .checkObstructionFreedom(checkObstructionFreedom)
        .apply { customConfiguration() }
        .check(this::class.java)

    @Test
    open fun stressTest() = StressOptions()
        .iterations(scenarios)
        .invocationsPerIteration(2_000)
        .actorsBefore(actorsBefore)
        .threads(threads)
        .actorsPerThread(2)
        .actorsAfter(0)
        .sequentialSpecification(sequentialSpecification.java)
        .apply { customConfiguration() }
        .check(this::class.java)

    protected open fun Options<*, *>.customConfiguration() {}
}
