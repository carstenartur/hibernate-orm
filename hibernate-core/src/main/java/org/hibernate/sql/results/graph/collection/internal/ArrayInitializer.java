/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.collection.spi.PersistentArrayHolder;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @author Chris Cranford
 */
public class ArrayInitializer extends AbstractImmediateCollectionInitializer {
	private static final String CONCRETE_NAME = ArrayInitializer.class.getSimpleName();

	private final DomainResultAssembler<Integer> listIndexAssembler;
	private final DomainResultAssembler<?> elementAssembler;

	private final int indexBase;

	public ArrayInitializer(
			NavigablePath navigablePath,
			PluralAttributeMapping arrayDescriptor,
			FetchParentAccess parentAccess,
			LockMode lockMode,
			DomainResult<?> collectionKeyResult,
			DomainResult<?> collectionValueKeyResult,
			Fetch listIndexFetch,
			Fetch elementFetch,
			boolean isResultInitializer,
			AssemblerCreationState creationState) {
		super(
				navigablePath,
				arrayDescriptor,
				parentAccess,
				lockMode,
				collectionKeyResult,
				collectionValueKeyResult,
				isResultInitializer,
				creationState
		);
		//noinspection unchecked
		this.listIndexAssembler = (DomainResultAssembler<Integer>) listIndexFetch.createAssembler( this, creationState );
		this.elementAssembler = elementFetch.createAssembler( this, creationState );
		this.indexBase = getCollectionAttributeMapping().getIndexMetadata().getListIndexBase();
	}

	@Override
	protected String getSimpleConcreteImplName() {
		return CONCRETE_NAME;
	}

	@Override
	protected void forEachAssembler(Consumer<DomainResultAssembler<?>> consumer) {
		consumer.accept( listIndexAssembler );
		consumer.accept( elementAssembler );
	}

	@Override
	public @Nullable PersistentArrayHolder<?> getCollectionInstance() {
		return (PersistentArrayHolder<?>) super.getCollectionInstance();
	}

	@Override
	protected void readCollectionRow(
			CollectionKey collectionKey,
			List<Object> loadingState,
			RowProcessingState rowProcessingState) {
		final Integer indexValue = listIndexAssembler.assemble( rowProcessingState );
		if ( indexValue == null ) {
			throw new HibernateException( "Illegal null value for array index encountered while reading: "
					+ getCollectionAttributeMapping().getNavigableRole() );
		}
		final Object element = elementAssembler.assemble( rowProcessingState );
		if ( element == null ) {
			// If element is null, then NotFoundAction must be IGNORE
			return;
		}
		int index = indexValue;

		if ( indexBase != 0 ) {
			index -= indexBase;
		}

		for ( int i = loadingState.size(); i <= index; ++i ) {
			loadingState.add( i, null );
		}

		loadingState.set( index, element );
	}

	@Override
	public void initializeInstanceFromParent(Object parentInstance, RowProcessingState rowProcessingState) {
		final Object[] array = (Object[]) getInitializedPart().getValue( parentInstance );
		assert array != null;
		collectionInstance = new PersistentArrayHolder<>( rowProcessingState.getSession(), array );
		state = State.INITIALIZED;
		initializeSubInstancesFromParent( rowProcessingState );
	}

	@Override
	protected void initializeSubInstancesFromParent(RowProcessingState rowProcessingState) {
		final Initializer initializer = elementAssembler.getInitializer();
		if ( initializer != null ) {
			final Iterator iter = getCollectionInstance().elements();
			while ( iter.hasNext() ) {
				initializer.initializeInstanceFromParent( iter.next(), rowProcessingState );
			}
		}
	}

	@Override
	public String toString() {
		return "ArrayInitializer{" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}
}
