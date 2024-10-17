package shared.di

import kotlin.reflect.KClass
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ObjCClass
import kotlinx.cinterop.ObjCObject
import kotlinx.cinterop.ObjCProtocol
import kotlinx.cinterop.getOriginalKotlinClass

@OptIn(BetaInteropApi::class)
data class SwiftType(
    val type: ObjCObject,
    val swiftClazz: KClass<*>,
)

@OptIn(BetaInteropApi::class)
fun SwiftType.getClazz(): KClass<*> =
    when (type) {
        is ObjCClass -> getOriginalKotlinClass(type)
        is ObjCProtocol -> getOriginalKotlinClass(type)
        else -> null
    }
        ?: swiftClazz