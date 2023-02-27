/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java.spi;

import java.lang.reflect.ParameterizedType;
import java.util.Objects;

import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.MutableMutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Extension of the general JavaType for "collection types"
 *
 * @apiNote "Collection types" are defined loosely here to cover mapping
 * collection types other than those from the "Java Collection Framework".
 *
 * @see CollectionSemantics
 *
 * @author Steve Ebersole
 */
public class CollectionJavaType<C> extends AbstractClassJavaType<C> {
	private final CollectionSemantics<C,?> semantics;

	public CollectionJavaType(Class<? extends C> type, CollectionSemantics<C,?> semantics) {
		super( type );
		this.semantics = semantics;
	}

	public CollectionSemantics<C,?> getSemantics() {
		return semantics;
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
		// none
		return null;
	}

	@Override
	public JavaType<C> createJavaType(
			ParameterizedType parameterizedType,
			TypeConfiguration typeConfiguration) {
		switch ( semantics.getCollectionClassification() ) {
			case ARRAY:
			case BAG:
			case ID_BAG:
			case LIST:
			case SET:
			case SORTED_SET:
			case ORDERED_SET:
				//noinspection unchecked,rawtypes
				return new BasicCollectionJavaType(
						parameterizedType,
						typeConfiguration.getJavaTypeRegistry()
								.resolveDescriptor( parameterizedType.getActualTypeArguments()[0] ),
						semantics
				);
		}
		// Construct a basic java type that knows its parametrization
		//noinspection unchecked
		return new UnknownBasicJavaType<>( parameterizedType, (MutabilityPlan<C>) MutableMutabilityPlan.INSTANCE );
	}

	@Override
	public C fromString(CharSequence string) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <X> X unwrap(C value, Class<X> type, WrapperOptions options) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public <X> C wrap(X value, WrapperOptions options) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public boolean areEqual(C one, C another) {
//		return one == another ||
//				(
//						one instanceof PersistentCollection &&
//								( (PersistentCollection<?>) one ).wasInitialized() &&
//								( (PersistentCollection<?>) one ).isWrapper( another )
//				) ||
//				(
//						another instanceof PersistentCollection &&
//								( (PersistentCollection<?>) another ).wasInitialized() &&
//								( (PersistentCollection<?>) another ).isWrapper( one )
//				);


		if ( one == another ) {
			return true;
		}

		if ( one instanceof PersistentCollection ) {
			final PersistentCollection pc = (PersistentCollection) one;
			return pc.wasInitialized() && ( pc.isWrapper( another ) || pc.isDirectlyProvidedCollection( another ) );
		}

		if ( another instanceof PersistentCollection ) {
			final PersistentCollection pc = (PersistentCollection) another;
			return pc.wasInitialized() && ( pc.isWrapper( one ) || pc.isDirectlyProvidedCollection( one ) );
		}

		return Objects.equals( one, another );
	}

	@Override
	public int extractHashCode(C x) {
		throw new UnsupportedOperationException();
	}
}
