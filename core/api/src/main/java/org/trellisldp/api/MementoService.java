/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.trellisldp.api;

import java.time.Instant;
import java.util.SortedSet;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;

/**
 * An interface for a Memento subsystem. Mementos of {@link Resource}s may be made and retrieved using this service.
 * Mementos may also be recorded by other means, including by the persistence layer independently of Trellis, but unless
 * they are retrieved via this service, Trellis will not publish them as HTTP resources. Mementos of NonRDFSources (like
 * any other {@code Resource}) may also be made and retrieved here, but the associated {@link Binary}s are made (like
 * all {@code Binary}s) via a {@link BinaryService} implementation.
 */
public interface MementoService {

    /**
     * Create a new Memento for a resource.
     * @param identifier the resource identifier
     * @param time the time of the Memento
     * @param data the data to save
     * @return a new completion stage that, when the stage completes normally, indicates that Memento resource was
     * successfully created in the corresponding persistence layer. In the case of an unsuccessful write operation,
     * the {@link CompletableFuture} will complete exceptionally and can be handled with
     * {@link CompletableFuture#handle}, {@link CompletableFuture#exceptionally} or similar methods.
     */
    CompletableFuture<Void> put(IRI identifier, Instant time, Stream<? extends Quad> data);

    /**
     * Create a new Memento for a resource.
     * @param resource the resource
     * @return a new completion stage that, when the stage completes normally, indicates that Memento resource was
     * successfully created in the corresponding persistence layer. In the case of an unsuccessful write operation,
     * the {@link CompletableFuture} will complete exceptionally and can be handled with
     * {@link CompletableFuture#handle}, {@link CompletableFuture#exceptionally} or similar methods.
     */
    default CompletableFuture<Void> put(Resource resource) {
        return put(resource.getIdentifier(), resource.getModified(), resource.stream());
    }

    /**
     * Fetch a Memento resource for the given time.
     * @param identifier the resource identifier
     * @param time the requested time
     * @return the new completion stage, containing the fetched resource
     */
    CompletableFuture<Resource> get(IRI identifier, Instant time);

    /**
     * Get the times for all of the Mementos of the given resource.
     * @param identifier the resource identifier
     * @return the new completion stage containing a collection of Memento dateTimes
     */
    CompletableFuture<SortedSet<Instant>> mementos(IRI identifier);

    /**
     * Delete a Memento resource.
     * @param identifier the resource identifier
     * @param time the version at the given time
     * @return a new completion stage that, when the stage completes normally, indicates that Memento resource was
     * successfully deleted from the corresponding persistence layer. In the case of an unsuccessful write operation,
     * the {@link CompletableFuture} will complete exceptionally and can be handled with
     * {@link CompletableFuture#handle}, {@link CompletableFuture#exceptionally} or similar methods.
     */
    CompletableFuture<Void> delete(IRI identifier, Instant time);
}
