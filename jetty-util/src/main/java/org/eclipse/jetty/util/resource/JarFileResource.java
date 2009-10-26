// ========================================================================
// Copyright (c) 1996-2009 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// All rights reserved. This program and the accompanying materials
// are made available under the terms of the Eclipse Public License v1.0
// and Apache License v2.0 which accompanies this distribution.
// The Eclipse Public License is available at 
// http://www.eclipse.org/legal/epl-v10.html
// The Apache License v2.0 is available at
// http://www.opensource.org/licenses/apache2.0.php
// You may elect to redistribute this code under either of these licenses. 
// ========================================================================
package org.eclipse.jetty.util.resource;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.eclipse.jetty.util.log.Log;

/* ------------------------------------------------------------ */
class JarFileResource extends JarResource
{
    
    transient JarFile _jarFile;
    transient File _file;
    transient String[] _list;
    transient JarEntry _entry;
    transient boolean _directory;
    transient String _jarUrl;
    transient String _path;
    transient boolean _exists;

    
    /* -------------------------------------------------------- */
    JarFileResource(URL url)
    {
        super(url);
    }
    
    JarFileResource(URL url, boolean useCaches)
    {
        super(url, useCaches);
    }
   

    /* ------------------------------------------------------------ */
    @Override
    public synchronized void release()
    {
        _list=null;
        _entry=null;
        _file=null;
        _jarFile=null;
        super.release();
    }
    
    /* ------------------------------------------------------------ */
    @Override
    protected boolean checkConnection()
    {
        try{
            super.checkConnection();
        }
        finally
        {
            if (_jarConnection==null)
            {
                _entry=null;
                _file=null;
                _jarFile=null;
                _list=null;
            }
        }
        return _jarFile!=null;
    }


    /* ------------------------------------------------------------ */
    @Override
    protected void newConnection()
        throws IOException
    {
        super.newConnection();
        
        _entry=null;
        _file=null;
        _jarFile=null;
        _list=null;
        
        int sep = _urlString.indexOf("!/");
        _jarUrl=_urlString.substring(0,sep+2);
        _path=_urlString.substring(sep+2);
        if (_path.length()==0)
            _path=null;   
        _jarFile=_jarConnection.getJarFile();
        _file=new File(_jarFile.getName());
    }
    
    
    /* ------------------------------------------------------------ */
    /**
     * Returns true if the respresenetd resource exists.
     */
    @Override
    public boolean exists()
    {
        if (_exists)
            return true;

        if (_urlString.endsWith("!/"))
        {
            
            String file_url=_urlString.substring(4,_urlString.length()-2);
            try{return newResource(file_url).exists();}
            catch(Exception e) {Log.ignore(e); return false;}
        }
        
        boolean check=checkConnection();
        
        // Is this a root URL?
        if (_jarUrl!=null && _path==null)
        {
            // Then if it exists it is a directory
            _directory=check;
            return true;
        }
        else 
        {
            // Can we find a file for it?
            JarFile jarFile=null;
            if (check)
                // Yes
                jarFile=_jarFile;
            else
            {
                // No - so lets look if the root entry exists.
                try
                {
                    JarURLConnection c=(JarURLConnection)((new URL(_jarUrl)).openConnection());
                    c.setUseCaches(getUseCaches());
                    jarFile=c.getJarFile();
                }
                catch(Exception e)
                {
                       Log.ignore(e);
                }
            }

            // Do we need to look more closely?
            if (jarFile!=null && _entry==null && !_directory)
            {
                // OK - we have a JarFile, lets look at the entries for our path
                Enumeration e=jarFile.entries();
                while(e.hasMoreElements())
                {
                    JarEntry entry = (JarEntry) e.nextElement();
                    String name=entry.getName().replace('\\','/');
                    
                    // Do we have a match
                    if (name.equals(_path))
                    {
                        _entry=entry;
                        // Is the match a directory
                        _directory=_path.endsWith("/");
                        break;
                    }
                    else if (_path.endsWith("/"))
                    {
                        if (name.startsWith(_path))
                        {
                            _directory=true;
                            break;
                        }
                    }
                    else if (name.startsWith(_path) && name.length()>_path.length() && name.charAt(_path.length())=='/')
                    {
                        _directory=true;
                        break;
                    }
                }
            }
        }    
        
        _exists= ( _directory || _entry!=null);
        return _exists;
    }

    
    /* ------------------------------------------------------------ */
    /**
     * Returns true if the represented resource is a container/directory.
     * If the resource is not a file, resources ending with "/" are
     * considered directories.
     */
    @Override
    public boolean isDirectory()
    {
        return _urlString.endsWith("/") || exists() && _directory;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Returns the last modified time
     */
    @Override
    public long lastModified()
    {
        if (checkConnection() && _file!=null)
            return _file.lastModified();
        return -1;
    }

    /* ------------------------------------------------------------ */
    @Override
    public synchronized String[] list()
    {
        
        if(isDirectory() && _list==null)
        {
            ArrayList list = new ArrayList(32);

            checkConnection();
            
            JarFile jarFile=_jarFile;
            if(jarFile==null)
            {
                try
                {
                    JarURLConnection jc=(JarURLConnection)((new URL(_jarUrl)).openConnection());
                    jc.setUseCaches(getUseCaches());
                    jarFile=jc.getJarFile();
                }
                catch(Exception e)
                {
                     Log.ignore(e);
                }
            }
            
            Enumeration e=jarFile.entries();
            String dir=_urlString.substring(_urlString.indexOf("!/")+2);
            while(e.hasMoreElements())
            {
                
                JarEntry entry = (JarEntry) e.nextElement();               
                String name=entry.getName().replace('\\','/');               
                if(!name.startsWith(dir) || name.length()==dir.length())
                {
                    continue;
                }
                String listName=name.substring(dir.length());               
                int dash=listName.indexOf('/');
                if (dash>=0)
                {
                    //when listing jar:file urls, you get back one
                    //entry for the dir itself, which we ignore
                    if (dash==0 && listName.length()==1)
                        continue;
                    //when listing jar:file urls, all files and
                    //subdirs have a leading /, which we remove
                    if (dash==0)
                        listName=listName.substring(dash+1, listName.length());
                    else
                        listName=listName.substring(0,dash+1);
                    
                    if (list.contains(listName))
                        continue;
                }
                
                list.add(listName);
            }
            
            _list=new String[list.size()];
            list.toArray(_list);
        }
        return _list;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Return the length of the resource
     */
    @Override
    public long length()
    {
        if (isDirectory())
            return -1;

        if (_entry!=null)
            return _entry.getSize();
        
        return -1;
    }
    
    /* ------------------------------------------------------------ */
    /** Encode according to this resource type.
     * File URIs are not encoded.
     * @param uri URI to encode.
     * @return The uri unchanged.
     */
    @Override
    public String encode(String uri)
    {
        return uri;
    }

    
    /**
     * Take a Resource that possibly might use URLConnection caching
     * and turn it into one that doesn't.
     * @param resource
     * @return
     */
    public static Resource getNonCachingResource (Resource resource)
    {
        if (!(resource instanceof JarFileResource))
            return resource;
        
        JarFileResource oldResource = (JarFileResource)resource;
        
        JarFileResource newResource = new JarFileResource(oldResource.getURL(), false);
        return newResource;
        
    }
    
    /**
     * Check if this jar:file: resource is contained in the
     * named resource. Eg jar:file:///a/b/c/foo.jar!/x.html isContainedIn file:///a/b/c/foo.jar
     * @param resource
     * @return
     * @throws MalformedURLException
     */
    @Override
    public boolean isContainedIn (Resource resource) 
    throws MalformedURLException
    {
        String string = _urlString;
        int index = string.indexOf("!/");
        if (index > 0)
            string = string.substring(0,index);
        if (string.startsWith("jar:"))
            string = string.substring(4);
        URL url = new URL(string);
        return url.sameFile(resource.getURL());     
    }
}








