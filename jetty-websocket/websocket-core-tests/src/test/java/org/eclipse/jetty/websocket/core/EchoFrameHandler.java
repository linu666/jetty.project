//
// ========================================================================
// Copyright (c) 1995-2020 Mort Bay Consulting Pty Ltd and others.
//
// This program and the accompanying materials are made available under
// the terms of the Eclipse Public License 2.0 which is available at
// https://www.eclipse.org/legal/epl-2.0
//
// This Source Code may also be made available under the following
// Secondary Licenses when the conditions for such availability set
// forth in the Eclipse Public License, v. 2.0 are satisfied:
// the Apache License v2.0 which is available at
// https://www.apache.org/licenses/LICENSE-2.0
//
// SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
// ========================================================================
//

package org.eclipse.jetty.websocket.core;

import org.eclipse.jetty.util.Callback;

public class EchoFrameHandler extends TestAsyncFrameHandler
{
    private boolean throwOnFrame;

    public void throwOnFrame()
    {
        throwOnFrame = true;
    }

    public EchoFrameHandler(String name)
    {
        super(name);
    }

    @Override
    public void onFrame(Frame frame, Callback callback)
    {
        if (LOG.isDebugEnabled())
            LOG.debug("[{}] onFrame {}", name, frame);
        receivedFrames.offer(Frame.copy(frame));

        if (throwOnFrame)
            throw new RuntimeException("intentionally throwing in server onFrame()");

        if (frame.isDataFrame())
        {
            if (LOG.isDebugEnabled())
                LOG.debug("[{}] echoDataFrame {}", name, frame);
            Frame echo = Frame.copy(frame).setMask(null);
            coreSession.sendFrame(echo, callback, false);
        }
        else
        {
            callback.succeeded();
        }
    }
}
