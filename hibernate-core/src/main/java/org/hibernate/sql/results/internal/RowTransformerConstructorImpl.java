/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.results.internal;

import jakarta.persistence.TupleElement;
import org.hibernate.InstantiationException;
import org.hibernate.sql.results.spi.RowTransformer;

import java.lang.reflect.Constructor;
import java.util.List;

/**
 * {@link RowTransformer} instantiating an arbitrary class
 *
 * @author Gavin King
 */
public class RowTransformerConstructorImpl<T> implements RowTransformer<T> {
	private final Class<T> type;
	private final TupleMetadata tupleMetadata;
	private final Constructor<T> constructor;

	public RowTransformerConstructorImpl(Class<T> type, TupleMetadata tupleMetadata) {
		this.type = type;
		this.tupleMetadata = tupleMetadata;
		final List<TupleElement<?>> elements = tupleMetadata.getList();
		final Class<?>[] sig = new Class[elements.size()];
		for (int i = 0; i < elements.size(); i++) {
			sig[i] = elements.get(i).getJavaType();
		}
		try {
			constructor = type.getDeclaredConstructor( sig );
			constructor.setAccessible( true );
		}
		catch (Exception e) {
			throw new InstantiationException( "Cannot instantiate query result type ", type, e );
		}
	}

	@Override
	public T transformRow(Object[] row) {
		try {
			return constructor.newInstance( row );
		}
		catch (Exception e) {
			throw new InstantiationException( "Cannot instantiate query result type", type, e );
		}
	}

	@Override
	public int determineNumberOfResultElements(int rawElementCount) {
		return 1;
	}
}
