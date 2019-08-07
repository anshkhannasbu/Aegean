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

package Applications.jetty.websocket.jsr356.encoders;

import Applications.jetty.util.BufferUtil;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class DefaultBinaryStreamEncoder extends AbstractEncoder implements Encoder.BinaryStream<ByteBuffer> {
    @Override
    public void encode(ByteBuffer message, OutputStream out) throws EncodeException, IOException {
        BufferUtil.writeTo(message, out);
    }
}
