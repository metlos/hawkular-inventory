/*
 * Copyright 2015-2017 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hawkular.inventory.impl.tinkerpop.provider;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

/**
 * @author Lukas Krejci
 * @since 1.2.0
 */
@MessageLogger(projectCode = "HAWKINV")
@ValidIdRange(min = 100100, max = 100199)
public interface Log extends BasicLogger {
    Log LOG = Logger.getMessageLogger(Log.class, "org.hawkular.inventory.impl.tinkerpop.provider");

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 100100, value = "Compacting the commit log of %d transactions.")
    void compactingCommitLog(long nofTransactions);

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 100101, value = "Commit compaction interrupted with message '%s'. No further commit compactions" +
            " will run until the server is restarted.")
    void periodicFlushFailed(String message, @Cause Throwable cause);

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 100102, value = "Running the periodic flush-to-disk of the graph")
    void runningPeriodicFlushToDisk();

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 100103, value = "Finished the periodic flush-to-disk in %d milliseconds.")
    void finishedPeriodicFlush(long duration);
}
