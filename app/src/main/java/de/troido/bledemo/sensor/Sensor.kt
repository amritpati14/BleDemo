package de.troido.bledemo.sensor

import android.support.annotation.DrawableRes
import android.util.Log
import de.troido.bleacon.data.BleDeserializer
import de.troido.bleacon.data.Primitive
import de.troido.bleacon.data.VecDeserializer
import de.troido.bleacon.data.mapping
import de.troido.bleacon.data.then
import de.troido.bledemo.R
import java.io.Serializable
import java.util.*

sealed class Sensor<out V>(val value: V?, @DrawableRes val img: Int, val name: String)
    : Serializable {

    companion object {
        const val noValue: String = "N/A"
    }

    object Deserializer : BleDeserializer<List<Sensor<*>>> {
        override val length = 19

        override fun deserialize(data: ByteArray): List<Sensor<*>>? =
                deserializers[data[0]]?.deserialize(data.copyOfRange(1, data.size))
    }

    class Accelerometer(v: Vec3<Float>?) : Sensor<Vec3<Float>>(
            v?.map { if (it > -DEAD_ZONE && it < DEAD_ZONE) 0.0f else it },
            R.drawable.ic_acc,
            "Accelerometer"
    ) {
        companion object {
            private const val DEAD_ZONE = 0.1f

            private fun convert(x: Short): Float = x * 2.0f / 32768.0f

            fun fromShorts(x: Short, y: Short, z: Short): Accelerometer =
                    Accelerometer(Vec3(convert(x), convert(y), convert(z)))
        }
    }

    class Gyroscope(v: Vec3<Float>?) : Sensor<Vec3<Float>>(
            v?.map { if (it > -DEAD_ZONE && it < DEAD_ZONE) 0.0f else it },
            R.drawable.ic_gyro,
            "Gyroscope"
    ) {
        companion object {
            private const val DEAD_ZONE = 2.0f

            private fun convert(x: Short): Float = x * 245.0f / 32768.0f

            fun fromShorts(x: Short, y: Short, z: Short): Gyroscope =
                    Gyroscope(Vec3(convert(x), convert(y), convert(z)))
        }
    }

    class Compass(v: Vec3<Float>?) : Sensor<Vec3<Float>>(v, R.drawable.ic_compass, "Compass") {
        companion object {
            private fun convert(x: Short): Float = x * 0.00014f

            fun fromShorts(x: Short, y: Short, z: Short): Compass =
                    Compass(Vec3(convert(x), convert(y), convert(z)))
        }
    }

    class Temperature(v: Float?) : Sensor<Float>(v, R.drawable.ic_temperature, "Temperature")

    class Humidity(v: Float?) : Sensor<Float>(v, R.drawable.ic_humidity, "Humidity") {
        companion object {
            private const val HUMIDITY_MOCK_MIN = 55.0f
            private const val HUMIDITY_MOCK_MAX = 56.0f
            private val rng = Random()

            fun mockHumidity(): Humidity = Humidity(
                    HUMIDITY_MOCK_MIN + (HUMIDITY_MOCK_MAX - HUMIDITY_MOCK_MIN) * rng.nextFloat()
            )
        }
    }

    class Pressure(v: Float?) : Sensor<Float>(v, R.drawable.ic_pressure, "Pressure")

    class Light(v: Float?) : Sensor<Float>(v, R.drawable.ic_light, "Light")

    sealed class Controller(val ix: Int, @DrawableRes img: Int) :
            Sensor<Unit>(Unit, img, "Controller") {

        companion object {
            private val controllers = arrayOf(JoystickUp, JoystickDown, JoystickLeft,
                                              JoystickRight, LeftButton, RightButton)

            fun fromIx(ix: Int): Controller? = controllers.firstOrNull { it.ix == ix }
        }

        object JoystickUp : Controller(0, R.drawable.board_joystick_up)
        object JoystickDown : Controller(1, R.drawable.board_joystick_down)
        object JoystickLeft : Controller(2, R.drawable.board_joystick_left)
        object JoystickRight : Controller(3, R.drawable.board_joystick_right)
        object None : Controller(4, R.drawable.board)
        object RightButton : Controller(6, R.drawable.board_right_button)
        object LeftButton : Controller(7, R.drawable.board_left_button)

        object Deserializer : BleDeserializer<Controller> {

            override val length = 1
            override fun deserialize(data: ByteArray): Controller? {
                Log.d("BT", data.map { it.toInt() and 0x00ff }.toString())
                return controllers.firstOrNull { data[0].toInt() and (1 shl 7 shr it.ix) != 0 }
                        ?: None
            }
        }
    }
}

private val deserializer1 =
        VecDeserializer(9, Primitive.Int16.Deserializer).mapping {
            it.map(Primitive.Int16::data).apply { Log.d("vals", "$this") }.let {
                listOf(
                        Sensor.Gyroscope.fromShorts(it[0], it[1], it[2]),
                        Sensor.Accelerometer.fromShorts(it[3], it[4], it[5]),
                        Sensor.Compass.fromShorts(it[6], it[7], it[8])
                )
            }
        }

private val deserializer2 =
        VecDeserializer(2, Primitive.Float32.Deserializer)
                .mapping {
                    val (t, l) = it.map(Primitive.Float32::data)
                    listOf(Sensor.Temperature(t), Sensor.Light(l * 100))
                }
                .then(Sensor.Controller.Deserializer) { vec, ctrl -> vec + ctrl }

private val deserializers = mapOf(
        0x00.toByte() to deserializer1,
        0x01.toByte() to deserializer2
)