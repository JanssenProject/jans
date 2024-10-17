package shared.di

import kotlin.reflect.KClass
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import org.koin.mp.KoinPlatformTools

/** Helper that allow to get Koin dependency in iOS */
fun <T> koinGet(
    clazz: KClass<*>,
    qualifier: Qualifier? = null,
    parameters: ParametersDefinition? = null
): T {
    val koin = KoinPlatformTools.defaultContext().get()

    return koin.get(clazz, qualifier, parameters)
}
