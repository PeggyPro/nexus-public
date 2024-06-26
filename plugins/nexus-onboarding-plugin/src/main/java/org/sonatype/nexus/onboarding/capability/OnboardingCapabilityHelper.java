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
package org.sonatype.nexus.onboarding.capability;

import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.capability.CapabilityReference;
import org.sonatype.nexus.capability.CapabilityRegistry;

import static com.google.common.base.Preconditions.checkNotNull;

@Named
@Singleton
public class OnboardingCapabilityHelper
{
  private final CapabilityRegistry capabilityRegistry;

  @Inject
  public OnboardingCapabilityHelper(final CapabilityRegistry capabilityRegistry) {
    this.capabilityRegistry = checkNotNull(capabilityRegistry);
  }

  public OnboardingCapability getOnboardingCapability() {
    Optional<? extends CapabilityReference> optionalCapabilityReference = capabilityRegistry.getAll().stream()
        .filter(reference -> reference.context().type().toString().equals(OnboardingCapability.TYPE_ID)).findFirst();
    return (OnboardingCapability) optionalCapabilityReference
        .orElseThrow(() -> new IllegalStateException("OnboardingCapability not found"));
  }
}
