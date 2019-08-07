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

package Applications.jetty.websocket.api.io;

import Applications.jetty.websocket.api.Session;

import java.io.IOException;
import java.io.OutputStream;

public class WebSocketOutputStream extends OutputStream {
    private final Session session;

    public WebSocketOutputStream(Session session) {
        this.session = session;
    }

    @Override
    public void write(int b) throws IOException {
        // TODO Auto-generated method stub
    }
}
