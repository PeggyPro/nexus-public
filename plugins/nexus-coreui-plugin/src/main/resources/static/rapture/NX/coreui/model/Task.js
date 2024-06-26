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
/*global Ext, NX*/

/**
 * Task model.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.model.Task', {
  extend: 'Ext.data.Model',
  fields: [
    {name: 'id', type: 'string', sortType: 'asUCText'},
    {name: 'enabled', type: 'boolean'},
    {name: 'name', type: 'string', sortType: 'asUCText'},
    {name: 'typeId', type: 'string', sortType: 'asUCText'},
    {name: 'typeName', type: 'string', sortType: 'asUCText'},
    {name: 'status', type: 'string', sortType: 'asUCText'},
    {name: 'statusDescription', type: 'string', sortType: 'asUCText'},
    {name: 'schedule', type: 'string', sortType: 'asUCText'},
    {name: 'nextRun', type: 'date', dateFormat: 'c' },
    {name: 'lastRun', type: 'date', dateFormat: 'c' },
    {name: 'lastRunResult', type: 'string'},
    {name: 'runnable', type: 'boolean'},
    {name: 'stoppable', type: 'boolean'},
    {name: 'alertEmail', type: 'string'},
    {name: 'notificationCondition', type: 'string', defaultValue: 'FAILURE' },
    {name: 'properties', type: 'auto' /*object*/, defaultValue: null },
    {
      name: 'statusProgress',
      type: 'string',
      calculate: function (data) {
        return (data.properties && data.properties['.progress']) || data.statusDescription;
      },
    },
    {name: 'startDate', type: 'date', dateFormat: 'c' },
    {name: 'recurringDays', type: 'auto' /*array*/},
    {name: 'cronExpression', type: 'string'}
  ]
});
