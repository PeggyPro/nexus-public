/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

package org.sonatype.nexus.crypto.secrets.internal;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.crypto.PbeCipherFactory;
import org.sonatype.nexus.crypto.internal.CryptoHelperImpl;
import org.sonatype.nexus.crypto.internal.PbeCipherFactoryImpl;
import org.sonatype.nexus.crypto.secrets.Secret;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class SecretsServiceImplTest
    extends TestSupport
{
  private PbeCipherFactory cipherFactory = new PbeCipherFactoryImpl(new CryptoHelperImpl());

  private SecretsServiceImpl underTest;

  @Before
  public void setup() throws Exception {
    underTest = new SecretsServiceImpl(cipherFactory);
  }

  @Test
  public void testLegacyEncryptDecrypt() {
    char[] secret = "my-secret".toCharArray();

    Secret encrypted = underTest.encrypt("testing", secret, null);

    assertThat(encrypted.getId(), not("my-secret"));

    assertThat(encrypted.decrypt(), is(secret));
  }

  @Test
  public void testFromLegacy() {
    char[] secret = "my-secret".toCharArray();

    Secret encrypted = underTest.encrypt("testing", secret, null);

    // Simulate reading an old value
    Secret fromEncrypted = underTest.from(encrypted.getId());

    assertThat(fromEncrypted.decrypt(), is(secret));
  }
}
