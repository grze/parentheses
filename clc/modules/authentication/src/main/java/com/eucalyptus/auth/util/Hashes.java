/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    Software License Agreement (BSD License)
* 
*    Copyright (c) 2008, Regents of the University of California
*    All rights reserved.
* 
*    Redistribution and use of this software in source and binary forms, with
*    or without modification, are permitted provided that the following
*    conditions are met:
* 
*      Redistributions of source code must retain the above copyright notice,
*      this list of conditions and the following disclaimer.
* 
*      Redistributions in binary form must reproduce the above copyright
*      notice, this list of conditions and the following disclaimer in the
*      documentation and/or other materials provided with the distribution.
* 
*    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
*    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
*    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
*    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
*    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
*    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
*    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
*    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
*    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
*    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
*    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
*    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
*    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
*******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.auth.util;

import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.util.encoders.UrlBase64;

import com.eucalyptus.auth.SystemCredentialProvider;
import com.eucalyptus.auth.crypto.Digest;
import com.eucalyptus.bootstrap.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.zip.Adler32;

public class Hashes {
  private static Logger LOG = Logger.getLogger( Hashes.class );

  public static byte[] getPemBytes( final Object o ) {
    PEMWriter pemOut;
    ByteArrayOutputStream pemByteOut = new ByteArrayOutputStream( );
    try {
      pemOut = new PEMWriter( new OutputStreamWriter( pemByteOut ) );
      pemOut.writeObject( o );
      pemOut.close( );
    } catch ( IOException e ) {
      LOG.error( e, e );//this can never happen
    }
    return pemByteOut.toByteArray( );
  }

  public static X509Certificate getPemCert( final byte[] o ) {
    X509Certificate x509 = null;
    PEMReader in = null;
    ByteArrayInputStream pemByteIn = new ByteArrayInputStream( o );
    in = new PEMReader( new InputStreamReader( pemByteIn ) );
    try {
      x509 = ( X509Certificate ) in.readObject( );
    } catch ( IOException e ) {
      LOG.error( e, e );//this can never happen
    }
    return x509;
  }

  public static String hashPassword( String password ) throws NoSuchAlgorithmException {
    byte[] fp = Digest.MD5.get( ).digest( password.getBytes( ) );
    return getHexString( fp );
  }

  public static String getDigestBase64( String input, Digest hash, boolean randomize ) {
    byte[] inputBytes = input.getBytes( );
    byte[] digestBytes = null;
    MessageDigest digest = hash.get( );
    digest.update( inputBytes );
    if ( randomize ) {
      SecureRandom random = new SecureRandom( );
      random.setSeed( System.currentTimeMillis( ) );
      byte[] randomBytes = random.generateSeed( inputBytes.length );
      digest.update( randomBytes );
    }
    digestBytes = digest.digest( );
    return new String( UrlBase64.encode( digestBytes ) );
  }

  public static String base64encode( String input ) {
    return new String( UrlBase64.encode( input.getBytes( ) ) );
  }

  public static String base64decode( String input ) {
    return new String( UrlBase64.decode( input.getBytes( ) ) );
  }

  public static String getFingerPrint( Key privKey ) {
    try {
      byte[] fp = Digest.SHA1.get( ).digest( privKey.getEncoded( ) );
      StringBuffer sb = new StringBuffer( );
      for ( byte b : fp )
        sb.append( String.format( "%02X:", b ) );
      return sb.substring( 0, sb.length( ) - 1 ).toLowerCase( );
    } catch ( Exception e ) {
      LOG.error( e, e );
      return null;
    }
  }

  public static String getHexString( byte[] data ) {
    StringBuffer buf = new StringBuffer( );
    for ( int i = 0; i < data.length; i++ ) {
      int halfbyte = ( data[i] >>> 4 ) & 0x0F;
      int two_halfs = 0;
      do {
        if ( ( 0 <= halfbyte ) && ( halfbyte <= 9 ) ) buf.append( ( char ) ( '0' + halfbyte ) );
        else buf.append( ( char ) ( 'a' + ( halfbyte - 10 ) ) );
        halfbyte = data[i] & 0x0F;
      } while ( two_halfs++ < 1 );
    }
    return buf.toString( ).toLowerCase( );
  }

  public static String getRandom( int size ) {
    SecureRandom random = new SecureRandom( );
    random.setSeed( System.nanoTime( ) );
    byte[] randomBytes = new byte[size];
    random.nextBytes( randomBytes );
    return new String( UrlBase64.encode( randomBytes ) );
  }

  public static String generateId( final String userId, final String prefix ) {
    Adler32 hash = new Adler32( );
    String key = userId + (System.currentTimeMillis( ) * Math.random( ));
    hash.update( key.getBytes( ) );
    String imageId = String.format( "%s-%08X", prefix, hash.getValue( ) );
    return imageId;
  }

  public static byte[] hexToBytes( String data ) {
    int k = 0;
    byte[] results = new byte[data.length( ) / 2];
    for ( int i = 0; i < data.length( ); ) {
      results[k] = ( byte ) ( Character.digit( data.charAt( i++ ), 16 ) << 4 );
      results[k] += ( byte ) ( Character.digit( data.charAt( i++ ), 16 ) );
      k++;
    }

    return results;
  }

  public static String bytesToHex( byte[] data ) {
    StringBuffer buffer = new StringBuffer( );
    for ( int i = 0; i < data.length; i++ ) {
      buffer.append( byteToHex( data[i] ) );
    }
    return ( buffer.toString( ) );
  }

  public static String byteToHex( byte data ) {
    StringBuffer hexString = new StringBuffer( );
    hexString.append( toHex( ( data >>> 4 ) & 0x0F ) );
    hexString.append( toHex( data & 0x0F ) );
    return hexString.toString( );
  }

  public static char toHex( int value ) {
    if ( ( 0 <= value ) && ( value <= 9 ) ) return ( char ) ( '0' + value );
    else return ( char ) ( 'a' + ( value - 10 ) );
  }

  public static String getHexSignature( ) {
    try {
      Signature signer = Signature.getInstance( "SHA256withRSA" );
      signer.initSign( SystemCredentialProvider.getCredentialProvider( Component.eucalyptus ).getPrivateKey( ) );
      signer.update( "eucalyptus".getBytes( ) );
      byte[] sig = signer.sign( );
      String hexSig = bytesToHex( sig );
      return hexSig;
    } catch ( Exception e ) {
      LOG.fatal( e, e );
      System.exit( -5 );
      return null;
    }
  }
}
