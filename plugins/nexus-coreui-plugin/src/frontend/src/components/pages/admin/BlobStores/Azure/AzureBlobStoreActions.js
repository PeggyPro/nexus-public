/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import UIStrings, {ValidationUtils} from '@sonatype/nexus-ui-plugin';

export default {
  init: () => ({
    bucketConfiguration: {
      authentication: {
        authenticationMethod: 'MANAGEDIDENTITY'
      }
    }
  }),

  validation: (data) => {
    const validationErrors = {
      bucketConfiguration: {
        accountName: ValidationUtils.validateNotBlank(data.bucketConfiguration?.accountName),
        containerName: ValidationUtils.validateNotBlank(data.bucketConfiguration?.containerName)
      }
    };

    if (data.bucketConfiguration?.authentication?.authenticationMethod === 'ACCOUNTKEY') {
      validationErrors.bucketConfiguration.authentication = {
        accountKey: ValidationUtils.validateNotBlank(data.bucketConfiguration.authentication.accountKey)
      }
    }

    return validationErrors;
  }
};
