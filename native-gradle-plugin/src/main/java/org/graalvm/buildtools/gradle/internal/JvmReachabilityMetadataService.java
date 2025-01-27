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
package org.graalvm.buildtools.gradle.internal;

import org.graalvm.reachability.JvmReachabilityMetadataRepository;
import org.graalvm.reachability.Query;
import org.graalvm.reachability.internal.FileSystemRepository;
import org.gradle.api.file.ArchiveOperations;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Property;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;

import javax.inject.Inject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class JvmReachabilityMetadataService implements BuildService<JvmReachabilityMetadataService.Params>, JvmReachabilityMetadataRepository {
    private static final Logger LOGGER = Logging.getLogger(JvmReachabilityMetadataService.class);

    private final JvmReachabilityMetadataRepository repository;

    @Inject
    protected abstract ArchiveOperations getArchiveOperations();

    @Inject
    protected abstract FileSystemOperations getFileOperations();

    public interface Params extends BuildServiceParameters {
        Property<LogLevel> getLogLevel();

        Property<URI> getUri();

        DirectoryProperty getCacheDir();
    }

    public JvmReachabilityMetadataService() throws URISyntaxException {
        URI uri = getParameters().getUri().get();
        this.repository = newRepository(uri);
    }

    private static String hashFor(URI uri) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] messageDigest = md.digest(md.digest(uri.toString().getBytes("utf-8")));
            BigInteger no = new BigInteger(1, messageDigest);
            StringBuilder digest = new StringBuilder(no.toString(16));
            while (digest.length() < 32) {
                digest.insert(0, "0");
            }
            return digest.toString();
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    private JvmReachabilityMetadataRepository newRepository(URI uri) throws URISyntaxException {
        String cacheKey = hashFor(uri);
        String path = uri.getPath();
        LogLevel logLevel = getParameters().getLogLevel().get();
        if (uri.getScheme().equals("file")) {
            File localFile = new File(uri);
            if (isSupportedZipFormat(path)) {
                return newRepositoryFromZipFile(cacheKey, localFile, logLevel);
            }
            return newRepositoryFromDirectory(localFile.toPath(), logLevel);
        }
        if (isSupportedZipFormat(path)) {
            File zipped = getParameters().getCacheDir().file(cacheKey + "/archive").get().getAsFile();
            if (!zipped.exists()) {
                try (ReadableByteChannel readableByteChannel = Channels.newChannel(uri.toURL().openStream())) {
                    try (FileOutputStream fileOutputStream = new FileOutputStream(zipped)) {
                        fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return newRepositoryFromZipFile(cacheKey, zipped, logLevel);
        }
        throw new UnsupportedOperationException("Remote URI must point to a zip, a tar.gz or tar.bz2 file");
    }

    private static boolean isSupportedZipFormat(String path) {
        return path.endsWith(".zip") || path.endsWith(".tar.gz") || path.endsWith(".tar.bz2");
    }

    private FileSystemRepository newRepositoryFromZipFile(String cacheKey, File localFile, LogLevel logLevel) {
        File explodedEntry = getParameters().getCacheDir().file(cacheKey + "/exploded").get().getAsFile();
        if (!explodedEntry.exists()) {
            if (explodedEntry.getParentFile().isDirectory() || explodedEntry.getParentFile().mkdirs()) {
                LOGGER.info("Extracting {} to {}", localFile, explodedEntry);
                getFileOperations().copy(spec -> {
                    if (localFile.getName().endsWith(".zip")) {
                        spec.from(getArchiveOperations().zipTree(localFile));
                    } else if (localFile.getName().endsWith(".tar.gz")) {
                        spec.from(getArchiveOperations().tarTree(localFile));
                    } else if (localFile.getName().endsWith(".tar.bz2")) {
                        spec.from(getArchiveOperations().tarTree(localFile));
                    }
                    spec.into(explodedEntry);
                });
            }
        }
        return newRepositoryFromDirectory(explodedEntry.toPath(), logLevel);
    }

    private FileSystemRepository newRepositoryFromDirectory(Path path, LogLevel logLevel) {
        if (Files.isDirectory(path)) {
            return new FileSystemRepository(path, new FileSystemRepository.Logger() {
                @Override
                public void log(String groupId, String artifactId, String version, Supplier<String> message) {
                    LOGGER.log(logLevel, "[jvm reachability metadata repository for {}:{}:{}]: {}", groupId, artifactId, version, message.get());
                }
            });
        } else {
            throw new IllegalArgumentException("JVM reachability metadata repository URI must point to a directory");
        }
    }

    /**
     * Performs a generic query on the repository, returning a list of
     * configuration directories. The query may be parameterized with
     * a number of artifacts, and can be used to refine behavior, for
     * example if a configuration directory isn't available for a
     * particular artifact version.
     *
     * @param queryBuilder the query builder
     * @return the set of configuration directories matching the query
     */
    @Override
    public Set<Path> findConfigurationDirectoriesFor(Consumer<? super Query> queryBuilder) {
        return repository.findConfigurationDirectoriesFor(queryBuilder);
    }

    /**
     * Returns a list of configuration directories for the specified artifact.
     * There may be more than one configuration directory for a given artifact,
     * but the list may also be empty if the repository doesn't contain any.
     * Never null.
     *
     * @param gavCoordinates the artifact GAV coordinates (group:artifact:version)
     * @return a list of configuration directories
     */
    @Override
    public Set<Path> findConfigurationDirectoriesFor(String gavCoordinates) {
        return repository.findConfigurationDirectoriesFor(gavCoordinates);
    }

    /**
     * Returns the set of configuration directories for all the modules supplied
     * as an argument.
     *
     * @param modules the list of modules
     * @return the set of configuration directories
     */
    @Override
    public Set<Path> findConfigurationDirectoriesFor(Collection<String> modules) {
        return repository.findConfigurationDirectoriesFor(modules);
    }
}
