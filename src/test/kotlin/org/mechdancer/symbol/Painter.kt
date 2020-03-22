package org.mechdancer.symbol

import org.mechdancer.algebra.implement.vector.Vector3D
import org.mechdancer.dependency.must
import org.mechdancer.remote.presets.RemoteHub
import org.mechdancer.remote.protocol.writeEnd
import org.mechdancer.remote.resources.Command
import org.mechdancer.remote.resources.MulticastSockets
import org.mechdancer.remote.resources.Name
import org.mechdancer.remote.resources.Networks
import org.mechdancer.symbol.core.Constant
import org.mechdancer.symbol.linear.NamedExpressionVector
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

/** 生成网络连接信息字符串 */
fun RemoteHub.networksInfo() =
    with(components) {
        "${must<Name>().field} opened ${must<Networks>().view.size} networks on ${must<MulticastSockets>().address}"
    }

private const val DIR_MASK = 0b0100
private const val FRAME_MASK = 0b1000

private object PaintCommand : Command {
    override val id = 6.toByte()
}

// 画任意内容
private fun RemoteHub.paint(
    topic: String,
    byte: Int,
    block: ByteArrayOutputStream.() -> Unit
) {
    ByteArrayOutputStream()
        .also { stream ->
            stream.writeEnd(topic)
            stream.write(byte)
            stream.block()
        }
        .toByteArray()
        .let { broadcast(PaintCommand, it) }
}

/**
 * 画一维信号
 */
fun RemoteHub.paint(
    topic: String,
    value: Number
) = paint(topic, 1) {
    DataOutputStream(this).apply {
        writeFloat(value.toFloat())
    }
}

/**
 * 画二维信号
 */
fun RemoteHub.paint(
    topic: String,
    x: Number,
    y: Number
) = paint(topic, 2) {
    DataOutputStream(this).apply {
        writeFloat(x.toFloat())
        writeFloat(y.toFloat())
    }
}

/**
 * 画三维信号
 */
fun RemoteHub.paint(
    topic: String,
    x: Number,
    y: Number,
    z: Number
) = paint(topic, 3) {
    DataOutputStream(this).apply {
        writeFloat(x.toFloat())
        writeFloat(y.toFloat())
        writeFloat(z.toFloat())
    }
}

/**
 * 画三维信号
 */
fun RemoteHub.paint(
    topic: String,
    p: Vector3D
) = paint(topic, 3) {
    DataOutputStream(this).apply {
        writeFloat(p.x.toFloat())
        writeFloat(p.y.toFloat())
        writeFloat(p.z.toFloat())
    }
}

/**
 * 画三维信号
 */
fun RemoteHub.paintFrame3(
    topic: String,
    list: Iterable<Iterable<Vector3D>>
) = paint(topic, 3 or FRAME_MASK) {
    DataOutputStream(this).apply {
        for (group in list) {
            for ((x, y, z) in group) {
                writeFloat(x.toFloat())
                writeFloat(y.toFloat())
                writeFloat(z.toFloat())
            }
            writeFloat(Float.NaN)
            writeFloat(Float.NaN)
            writeFloat(Float.NaN)
        }
    }
}

/**
 * 场画成一维信号的集合
 */
fun RemoteHub.paint(expressionVector: NamedExpressionVector) {
    for ((v, e) in expressionVector.expressions)
        if (e is Constant)
            paint(v.toString(), e.value)
}
