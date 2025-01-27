/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.graalvm.reachability;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Interface for accessing a reachability metadata repository.
 * The goal of this repository is to answer questions like:
 * "give me the configuration files for this artifact", where
 * an artifact is represented by its GAV coordinates.
 *
 * The repository query may be configured for multiple artifacts
 * and provide overrides for cases where configuration files
 * are missing.
 */
public interface JvmReachabilityMetadataRepository {
    /**
     * Performs a generic query on the repository, returning a list of
     * configuration directories. The query may be parameterized with
     * a number of artifacts, and can be used to refine behavior, for
     * example if a configuration directory isn't available for a
     * particular artifact version.
     * @param queryBuilder the query builder
     * @return the set of configuration directories matching the query
     */
    Set<Path> findConfigurationDirectoriesFor(Consumer<? super Query> queryBuilder);

    /**
     * Returns a list of configuration directories for the specified artifact.
     * There may be more than one configuration directory for a given artifact,
     * but the list may also be empty if the repository doesn't contain any.
     * Never null.
     * @param gavCoordinates the artifact GAV coordinates (group:artifact:version)
     * @return a list of configuration directories
     */
    default Set<Path> findConfigurationDirectoriesFor(String gavCoordinates) {
        return findConfigurationDirectoriesFor(q -> q.forArtifacts(gavCoordinates));
    }

    /**
     * Returns the set of configuration directories for all the modules supplied
     * as an argument.
     * @param modules the list of modules
     * @return the set of configuration directories
     */
    default Set<Path> findConfigurationDirectoriesFor(Collection<String> modules) {
        return findConfigurationDirectoriesFor(q -> q.forArtifacts(modules));
    }
}
