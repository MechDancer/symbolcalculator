package org.mechdancer.symbol

import org.mechdancer.dependency.must
import org.mechdancer.remote.presets.RemoteHub
import org.mechdancer.remote.protocol.writeEnd
import org.mechdancer.remote.resources.Command
import org.mechdancer.remote.resources.MulticastSockets
import org.mechdancer.remote.resources.Name
import org.mechdancer.remote.resources.Networks
import org.mechdancer.symbol.core.Constant
import org.mechdancer.symbol.linear.ExpressionVector
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
 * 场画成一维信号的集合
 */
fun RemoteHub.paint(expressionVector: ExpressionVector) {
    for ((v, e) in expressionVector.expressions)
        if (e is Constant)
            paint(v.toString(), e.value)
}
