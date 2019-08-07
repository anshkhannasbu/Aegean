//
//  ========================================================================
//  Copyright (c) 1995-2013 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package Applications.jetty.websocket.common;

import Applications.jetty.util.BufferUtil;
import Applications.jetty.websocket.api.extensions.Frame;
import Applications.jetty.websocket.common.frames.*;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * A Base Frame as seen in <a href="https://tools.ietf.org/html/rfc6455#section-5.2">RFC 6455. Sec 5.2</a>
 * <p>
 * <pre>
 *    0                   1                   2                   3
 *    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *   +-+-+-+-+-------+-+-------------+-------------------------------+
 *   |F|R|R|R| opcode|M| Payload len |    Extended payload length    |
 *   |I|S|S|S|  (4)  |A|     (7)     |             (16/64)           |
 *   |N|V|V|V|       |S|             |   (if payload len==126/127)   |
 *   | |1|2|3|       |K|             |                               |
 *   +-+-+-+-+-------+-+-------------+ - - - - - - - - - - - - - - - +
 *   |     Extended payload length continued, if payload len == 127  |
 *   + - - - - - - - - - - - - - - - +-------------------------------+
 *   |                               |Masking-key, if MASK set to 1  |
 *   +-------------------------------+-------------------------------+
 *   | Masking-key (continued)       |          Payload Data         |
 *   +-------------------------------- - - - - - - - - - - - - - - - +
 *   :                     Payload Data continued ...                :
 *   + - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - +
 *   |                     Payload Data continued ...                |
 *   +---------------------------------------------------------------+
 * </pre>
 */
public abstract class WebSocketFrame implements Frame {
    public static WebSocketFrame copy(Frame copy) {
        WebSocketFrame frame = null;
        switch (copy.getOpCode()) {
            case OpCode.BINARY:
                frame = new BinaryFrame();
                break;
            case OpCode.TEXT:
                frame = new TextFrame();
                break;
            case OpCode.CLOSE:
                frame = new CloseFrame();
                break;
            case OpCode.CONTINUATION:
                frame = new ContinuationFrame();
                break;
            case OpCode.PING:
                frame = new PingFrame();
                break;
            case OpCode.PONG:
                frame = new PongFrame();
                break;
            default:
                throw new IllegalArgumentException("Cannot copy frame with opcode " + copy.getOpCode() + " - " + copy);
        }

        frame.copyHeaders(copy);
        frame.setPayload(copy.getPayload());

        return frame;
    }

    /**
     * Combined FIN + RSV1 + RSV2 + RSV3 + OpCode byte.
     * <p>
     * <p>
     * <pre>
     *   1000_0000 (0x80) = fin
     *   0100_0000 (0x40) = rsv1
     *   0010_0000 (0x20) = rsv2
     *   0001_0000 (0x10) = rsv3
     *   0000_1111 (0x0F) = opcode
     * </pre>
     */
    protected byte finRsvOp;
    protected boolean masked = false;

    protected byte mask[];
    /**
     * The payload data.
     * <p>
     * It is assumed to always be in FLUSH mode (ready to read) in this object.
     */
    protected ByteBuffer data;

    protected int payloadLength = 0;

    /**
     * Construct form opcode
     */
    protected WebSocketFrame(byte opcode) {
        reset();
        setOpCode(opcode);
    }

    public abstract void assertValid();

    protected void copy(WebSocketFrame copy, ByteBuffer payload) {
        copyHeaders(copy);
        setPayload(payload);
    }

    protected void copyHeaders(Frame frame) {
        finRsvOp = 0x00;
        finRsvOp |= frame.isFin() ? 0x80 : 0x00;
        finRsvOp |= frame.isRsv1() ? 0x40 : 0x00;
        finRsvOp |= frame.isRsv2() ? 0x20 : 0x00;
        finRsvOp |= frame.isRsv3() ? 0x10 : 0x00;
        finRsvOp |= frame.getOpCode() & 0x0F;

        masked = frame.isMasked();
        if (masked) {
            mask = frame.getMask();
        } else {
            mask = null;
        }
    }

    protected void copyHeaders(WebSocketFrame copy) {
        finRsvOp = copy.finRsvOp;
        masked = copy.masked;
        mask = null;
        if (copy.mask != null) {
            mask = new byte[copy.mask.length];
            System.arraycopy(copy.mask, 0, mask, 0, mask.length);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        WebSocketFrame other = (WebSocketFrame) obj;
        if (data == null) {
            if (other.data != null) {
                return false;
            }
        } else if (!data.equals(other.data)) {
            return false;
        }
        if (finRsvOp != other.finRsvOp) {
            return false;
        }
        if (!Arrays.equals(mask, other.mask)) {
            return false;
        }
        if (masked != other.masked) {
            return false;
        }
        return true;
    }

    @Override
    public byte[] getMask() {
        return mask;
    }

    @Override
    public final byte getOpCode() {
        return (byte) (finRsvOp & 0x0F);
    }

    /**
     * Get the payload ByteBuffer. possible null.
     * <p>
     *
     * @return A {@link ByteBuffer#slice()} of the payload buffer (to prevent modification of the buffer state). Possibly null if no payload present.
     * <p>
     * Note: this method is exposed via the immutable {@link Frame#getPayload()} method.
     */
    @Override
    public ByteBuffer getPayload() {
        return data;
    }

    public String getPayloadAsUTF8() {
        return BufferUtil.toUTF8String(getPayload());
    }

    @Override
    public int getPayloadLength() {
        if (data == null) {
            return 0;
        }
        return payloadLength;
    }

    @Override
    public Type getType() {
        return Type.from(getOpCode());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((data == null) ? 0 : data.hashCode());
        result = (prime * result) + finRsvOp;
        result = (prime * result) + Arrays.hashCode(mask);
        return result;
    }

    @Override
    public boolean hasPayload() {
        return ((data != null) && (payloadLength > 0));
    }

    public abstract boolean isControlFrame();

    public abstract boolean isDataFrame();

    @Override
    public boolean isFin() {
        return (byte) (finRsvOp & 0x80) != 0;
    }

    @Override
    public boolean isLast() {
        return isFin();
    }

    @Override
    public boolean isMasked() {
        return masked;
    }

    @Override
    public boolean isRsv1() {
        return (byte) (finRsvOp & 0x40) != 0;
    }

    @Override
    public boolean isRsv2() {
        return (byte) (finRsvOp & 0x20) != 0;
    }

    @Override
    public boolean isRsv3() {
        return (byte) (finRsvOp & 0x10) != 0;
    }

    /**
     * Get the position currently within the payload data.
     * <p>
     * Used by flow control, generator and window sizing.
     *
     * @return the number of bytes remaining in the payload data that has not yet been written out to Network ByteBuffers.
     */
    public int position() {
        if (data == null) {
            return -1;
        }
        return data.position();
    }

    /**
     * Get the number of bytes remaining to write out to the Network ByteBuffer.
     * <p>
     * Used by flow control, generator and window sizing.
     *
     * @return the number of bytes remaining in the payload data that has not yet been written out to Network ByteBuffers.
     */
    @Override
    public int remaining() {
        if (data == null) {
            return 0;
        }
        return data.remaining();
    }

    public void reset() {
        finRsvOp = (byte) 0x80; // FIN (!RSV, opcode 0)
        masked = false;
        data = null;
        payloadLength = 0;
        mask = null;
    }

    public WebSocketFrame setFin(boolean fin) {
        // set bit 1
        this.finRsvOp = (byte) ((finRsvOp & 0x7F) | (fin ? 0x80 : 0x00));
        return this;
    }

    public Frame setMask(byte[] maskingKey) {
        this.mask = maskingKey;
        this.masked = (mask != null);
        return this;
    }

    public Frame setMasked(boolean mask) {
        this.masked = mask;
        return this;
    }

    protected WebSocketFrame setOpCode(byte op) {
        this.finRsvOp = (byte) ((finRsvOp & 0xF0) | (op & 0x0F));
        return this;
    }

    /**
     * Set the data payload.
     * <p>
     * The provided buffer will be used as is, no copying of bytes performed.
     * <p>
     * The provided buffer should be flipped and ready to READ from.
     *
     * @param buf the bytebuffer to set
     */
    public WebSocketFrame setPayload(ByteBuffer buf) {
        if (buf == null) {
            data = null;
            return this;
        }

        data = buf.slice();
        payloadLength = data.limit();
        return this;
    }

    public WebSocketFrame setRsv1(boolean rsv1) {
        // set bit 2
        this.finRsvOp = (byte) ((finRsvOp & 0xBF) | (rsv1 ? 0x40 : 0x00));
        return this;
    }

    public WebSocketFrame setRsv2(boolean rsv2) {
        // set bit 3
        this.finRsvOp = (byte) ((finRsvOp & 0xDF) | (rsv2 ? 0x20 : 0x00));
        return this;
    }

    public WebSocketFrame setRsv3(boolean rsv3) {
        // set bit 4
        this.finRsvOp = (byte) ((finRsvOp & 0xEF) | (rsv3 ? 0x10 : 0x00));
        return this;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(OpCode.name((byte) (finRsvOp & 0x0F)));
        b.append('[');
        b.append("len=").append(payloadLength);
        b.append(",fin=").append((finRsvOp & 0x80) != 0);
        b.append(",rsv=");
        b.append(((finRsvOp & 0x40) != 0) ? '1' : '.');
        b.append(((finRsvOp & 0x20) != 0) ? '1' : '.');
        b.append(((finRsvOp & 0x10) != 0) ? '1' : '.');
        b.append(",masked=").append(masked);
        b.append(",remaining=").append(remaining());
        b.append(",position=").append(position());
        b.append(']');
        return b.toString();
    }
}
