package voodoo.data.curse

import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.Serializer
import kotlinx.serialization.internal.IntDescriptor

enum class PackageType {
    FOLDER,
    CTOP,
    SINGLEFILE,
    CMOD2,
    MODPACK,
    MOD,
    ANY;


    @Serializer(forClass = PackageType::class)
    companion object {
        override val descriptor: SerialDescriptor = IntDescriptor

        override fun deserialize(decoder: Decoder): PackageType {
            return values()[decoder.decodeInt()-1]
        }

        override fun serialize(encoder: Encoder, obj: PackageType) {
            encoder.encodeInt(obj.ordinal + 1)
        }
    }
}