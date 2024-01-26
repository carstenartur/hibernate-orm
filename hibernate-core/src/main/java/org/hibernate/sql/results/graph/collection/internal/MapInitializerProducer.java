/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import org.hibernate.LockMode;
import org.hibernate.collection.spi.CollectionInitializerProducer;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;

/**
 * @author Steve Ebersole
 */
public class MapInitializerProducer implements CollectionInitializerProducer {
	private final PluralAttributeMapping mapDescriptor;
	private final Fetch mapKeyFetch;
	private final Fetch mapValueFetch;

	public MapInitializerProducer(
			PluralAttributeMapping mapDescriptor,
			Fetch mapKeyFetch,
			Fetch mapValueFetch) {
		this.mapDescriptor = mapDescriptor;
		this.mapKeyFetch = mapKeyFetch;
		this.mapValueFetch = mapValueFetch;
	}

	@Override
	public CollectionInitializer produceInitializer(
			NavigablePath navigablePath,
			PluralAttributeMapping attributeMapping,
			FetchParentAccess parentAccess,
			LockMode lockMode,
			DomainResultAssembler<?> collectionKeyAssembler,
			DomainResultAssembler<?> collectionValueKeyAssembler,
			AssemblerCreationState creationState) {
		throw new UnsupportedOperationException( "Use the non-deprecated method variant instead." );
	}

	@Override
	public CollectionInitializer produceInitializer(
			NavigablePath navigablePath,
			PluralAttributeMapping attribute,
			FetchParentAccess parentAccess,
			LockMode lockMode,
			DomainResult<?> collectionKeyResult,
			DomainResult<?> collectionValueKeyResult,
			boolean isResultInitializer,
			AssemblerCreationState creationState) {
		return new MapInitializer(
				navigablePath,
				mapDescriptor,
				parentAccess,
				lockMode,
				collectionKeyResult,
				collectionValueKeyResult,
				mapKeyFetch,
				mapValueFetch,
				isResultInitializer,
				creationState
		);
	}
}
