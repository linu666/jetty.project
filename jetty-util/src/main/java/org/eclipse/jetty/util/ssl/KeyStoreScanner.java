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

package org.eclipse.jetty.util.ssl;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.function.Consumer;

import org.eclipse.jetty.util.Scanner;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedOperation;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

/**
 * <p>The {@link KeyStoreScanner} is used to monitor the KeyStore file used by the {@link SslContextFactory}.
 * It will reload the {@link SslContextFactory} if it detects that the KeyStore file has been modified.</p>
 * <p>If the TrustStore file needs to be changed, then this should be done before touching the KeyStore file,
 * the {@link SslContextFactory#reload(Consumer)} will only occur after the KeyStore file has been modified.</p>
 */
public class KeyStoreScanner extends ContainerLifeCycle implements Scanner.DiscreteListener
{
    private static final Logger LOG = Log.getLogger(KeyStoreScanner.class);

    private final SslContextFactory sslContextFactory;
    private final File keystoreFile;
    private final Scanner _scanner;

    public KeyStoreScanner(SslContextFactory sslContextFactory)
    {
        this.sslContextFactory = sslContextFactory;
        try
        {
            keystoreFile = sslContextFactory.getKeyStoreResource().getFile();
            if (keystoreFile == null || !keystoreFile.exists())
                throw new IllegalArgumentException("keystore file does not exist");
            if (keystoreFile.isDirectory())
                throw new IllegalArgumentException("expected keystore file not directory");
        }
        catch (IOException e)
        {
            throw new IllegalArgumentException("could not obtain keystore file", e);
        }

        File parentFile = keystoreFile.getParentFile();
        if (!parentFile.exists() || !parentFile.isDirectory())
            throw new IllegalArgumentException("error obtaining keystore dir");

        _scanner = new Scanner();
        _scanner.setScanDirs(Collections.singletonList(parentFile));
        _scanner.setScanInterval(1);
        _scanner.setReportDirs(false);
        _scanner.setReportExistingFilesOnStartup(false);
        _scanner.setScanDepth(1);
        _scanner.addListener(this);
        addBean(_scanner);
    }

    @Override
    public void fileAdded(String filename)
    {
        if (LOG.isDebugEnabled())
            LOG.debug("added {}", filename);

        if (keystoreFile.toPath().toString().equals(filename))
            reload();
    }

    @Override
    public void fileChanged(String filename)
    {
        if (LOG.isDebugEnabled())
            LOG.debug("changed {}", filename);

        if (keystoreFile.toPath().toString().equals(filename))
            reload();
    }

    @Override
    public void fileRemoved(String filename)
    {
        if (LOG.isDebugEnabled())
            LOG.debug("removed {}", filename);

        if (keystoreFile.toPath().toString().equals(filename))
            reload();
    }

    @ManagedOperation(value = "Reload the SSL Keystore", impact = "ACTION")
    public void reload()
    {
        if (LOG.isDebugEnabled())
            LOG.debug("reloading keystore file {}", keystoreFile);

        try
        {
            sslContextFactory.reload(scf -> {});
        }
        catch (Throwable t)
        {
            LOG.warn("Keystore Reload Failed", t);
        }
    }

    @ManagedAttribute("scanning interval to detect changes which need reloaded")
    public int getScanInterval()
    {
        return _scanner.getScanInterval();
    }

    public void setScanInterval(int scanInterval)
    {
        _scanner.setScanInterval(scanInterval);
    }
}
