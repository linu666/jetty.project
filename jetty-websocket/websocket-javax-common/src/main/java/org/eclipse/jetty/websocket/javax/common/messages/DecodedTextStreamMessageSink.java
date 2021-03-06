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

package org.eclipse.jetty.websocket.javax.common.messages;

import java.io.IOException;
import java.io.Reader;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.List;
import javax.websocket.CloseReason;
import javax.websocket.DecodeException;
import javax.websocket.Decoder;

import org.eclipse.jetty.websocket.core.CoreSession;
import org.eclipse.jetty.websocket.core.exception.CloseException;
import org.eclipse.jetty.websocket.javax.common.JavaxWebSocketFrameHandlerFactory;
import org.eclipse.jetty.websocket.javax.common.decoders.RegisteredDecoder;
import org.eclipse.jetty.websocket.util.messages.MessageSink;
import org.eclipse.jetty.websocket.util.messages.ReaderMessageSink;

public class DecodedTextStreamMessageSink<T> extends AbstractDecodedMessageSink.Stream<Decoder.TextStream<T>>
{
    public DecodedTextStreamMessageSink(CoreSession session, MethodHandle methodHandle, List<RegisteredDecoder> decoders)
    {
        super(session, methodHandle, decoders);
    }

    @Override
    MessageSink newMessageSink(CoreSession coreSession) throws Exception
    {
        MethodHandle methodHandle = JavaxWebSocketFrameHandlerFactory.getServerMethodHandleLookup()
            .findVirtual(DecodedTextStreamMessageSink.class, "onStreamStart", MethodType.methodType(void.class, Reader.class))
            .bindTo(this);
        return new ReaderMessageSink(coreSession, methodHandle);
    }

    public void onStreamStart(Reader reader)
    {
        try
        {
            T obj = _decoder.decode(reader);
            invoke(obj);
        }
        catch (DecodeException | IOException e)
        {
            throw new CloseException(CloseReason.CloseCodes.CANNOT_ACCEPT.getCode(), "Unable to decode", e);
        }
    }
}
